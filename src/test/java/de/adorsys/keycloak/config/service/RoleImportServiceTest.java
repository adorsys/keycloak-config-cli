/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2025 adorsys GmbH & Co. KG @ https://adorsys.com
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package de.adorsys.keycloak.config.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportProtectedRolesProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportProtectedRolesProperties.ProtectedRolesMode;
import de.adorsys.keycloak.config.repository.RoleRepository;
import de.adorsys.keycloak.config.service.role.ProtectedRoleResolver;
import de.adorsys.keycloak.config.service.rolecomposites.client.ClientRoleCompositeImportService;
import de.adorsys.keycloak.config.service.rolecomposites.realm.RealmRoleCompositeImportService;
import de.adorsys.keycloak.config.service.state.StateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RoleImportService}, covering two concerns:
 *
 * <ul>
 *   <li>Change-detection: server-generated identifiers ({@code id} of the role itself and {@code containerId}
 *       of its owning realm/client) must never be interpreted as a content change, so an import file that
 *       differs only in identifiers does not push an update to Keycloak.</li>
 *   <li>Protected-role guard: roles flagged in {@code import.protected-roles} are skipped for
 *       update/delete/composite-sync (with an explanatory skip log), while unprotected roles, role creations
 *       and cross-namespace names still proceed.</li>
 * </ul>
 */
class RoleImportServiceTest {

    private static final String REALM_NAME = "realmWithRoles";
    private static final String CLIENT_ID = "moped-client";
    private static final String REALM_ROLE_NAME = "my_realm_role";
    private static final String CLIENT_ROLE_NAME = "my_client_role";

    private static final String EXISTING_ROLE_ID = "1eb9dd3b-0000-0000-0000-0000000000aa";
    private static final String EXISTING_CONTAINER_ID = "1eb9dd3b-0000-0000-0000-0000000000bb";
    private static final String IMPORTED_ROLE_ID = "9999ffff-0000-0000-0000-0000000000aa";
    private static final String IMPORTED_CONTAINER_ID = "9999ffff-0000-0000-0000-0000000000bb";

    private static final String DESCRIPTION = "My role";
    private static final String CHANGED_DESCRIPTION = "My changed role";
    private static final Map<String, List<String>> ATTRIBUTES = Map.of("my attribute", List.of("my value"));
    private static final Map<String, List<String>> CHANGED_ATTRIBUTES = Map.of("my attribute", List.of("my changed value"));

    private RoleRepository roleRepository;
    private ImportConfigProperties importConfigProperties;
    private ImportConfigProperties.ImportManagedProperties managedProperties;
    private StateService stateService;
    private RealmRoleCompositeImportService realmRoleCompositeImport;
    private ClientRoleCompositeImportService clientRoleCompositeImport;
    private RoleImportService roleImportService;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        roleRepository = mock(RoleRepository.class);
        importConfigProperties = mock(ImportConfigProperties.class);
        managedProperties = mock(ImportConfigProperties.ImportManagedProperties.class);
        stateService = mock(StateService.class);
        realmRoleCompositeImport = mock(RealmRoleCompositeImportService.class);
        clientRoleCompositeImport = mock(ClientRoleCompositeImportService.class);

        when(importConfigProperties.getManaged()).thenReturn(managedProperties);
        when(managedProperties.getRole()).thenReturn(ImportManagedPropertiesValues.NO_DELETE);
        when(importConfigProperties.isParallel()).thenReturn(false);
        when(importConfigProperties.getRemoteState())
                .thenReturn(mock(ImportConfigProperties.ImportRemoteStateProperties.class));
        when(importConfigProperties.getProtectedRoles())
                .thenReturn(new ImportProtectedRolesProperties(ProtectedRolesMode.ADD, List.of(), Map.of()));

        roleImportService = newService();

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger(RoleImportService.class);
        logger.setLevel(Level.DEBUG);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    private RoleImportService newService() {
        return new RoleImportService(
                realmRoleCompositeImport,
                clientRoleCompositeImport,
                roleRepository,
                importConfigProperties,
                stateService,
                new ProtectedRoleResolver(importConfigProperties)
        );
    }

    private void givenProtectedRoles(ProtectedRolesMode mode, List<String> realmRoles, Map<String, List<String>> clientRoles) {
        Map<String, Collection<String>> clientRolesAsCollections = new HashMap<>(clientRoles);
        when(importConfigProperties.getProtectedRoles())
                .thenReturn(new ImportProtectedRolesProperties(mode, realmRoles, clientRolesAsCollections));
        roleImportService = newService();
    }

    private RoleRepresentation role(String name, String description) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(name);
        role.setDescription(description);
        return role;
    }

    private RealmImport realmImportWithRealmRoles(List<RoleRepresentation> roles) {
        RealmImport realmImport = new RealmImport();
        realmImport.setRealm(REALM_NAME);
        RolesRepresentation rolesRepresentation = new RolesRepresentation();
        rolesRepresentation.setRealm(roles);
        realmImport.setRoles(rolesRepresentation);
        return realmImport;
    }

    private RealmImport realmImportWithClientRoles(Map<String, List<RoleRepresentation>> clientRoles) {
        RealmImport realmImport = new RealmImport();
        realmImport.setRealm(REALM_NAME);
        RolesRepresentation rolesRepresentation = new RolesRepresentation();
        rolesRepresentation.setClient(clientRoles);
        realmImport.setRoles(rolesRepresentation);
        return realmImport;
    }

    @Nested
    class ProtectedRealmRoleUpdate {
        @BeforeEach
        void init() {
            givenProtectedRoles(ProtectedRolesMode.REPLACE, List.of("admin"), Map.of());
        }

        @Test
        void doImport_shouldNotUpdateProtectedRealmRoleWhenItDiffers() {
            RoleRepresentation existingAdmin = role("admin", "Original admin description");
            when(roleRepository.getRealmRoles(REALM_NAME)).thenReturn(List.of(existingAdmin));

            RealmImport realmImport = realmImportWithRealmRoles(
                    List.of(role("admin", "Changed admin description"))
            );

            roleImportService.doImport(realmImport);

            verify(roleRepository, never()).updateRealmRole(anyString(), argThat(r -> "admin".equals(r.getName())));
        }

        @Test
        void doImport_shouldLogSkipReasonForProtectedRealmRole() {
            RoleRepresentation existingAdmin = role("admin", "Original admin description");
            when(roleRepository.getRealmRoles(REALM_NAME)).thenReturn(List.of(existingAdmin));

            RealmImport realmImport = realmImportWithRealmRoles(
                    List.of(role("admin", "Changed admin description"))
            );

            roleImportService.doImport(realmImport);

            boolean loggedSkip = listAppender.list.stream()
                    .anyMatch(event -> event.getLevel() == Level.INFO
                            && event.getFormattedMessage().contains("admin"));
            assertTrue(loggedSkip,
                    "expected an info-level log entry naming the skipped protected role 'admin'");
        }

        @Test
        void doImport_shouldCreateProtectedRealmRoleWhenNotExisting() {
            when(roleRepository.getRealmRoles(REALM_NAME)).thenReturn(List.of());

            RealmImport realmImport = realmImportWithRealmRoles(
                    List.of(role("admin", "Newly created admin"))
            );

            roleImportService.doImport(realmImport);

            verify(roleRepository).createRealmRole(eq(REALM_NAME), argThat(r -> "admin".equals(r.getName())));
        }
    }

    @Nested
    class UnprotectedRealmRoleUpdate {
        @BeforeEach
        void init() {
            givenProtectedRoles(ProtectedRolesMode.REPLACE, List.of("admin"), Map.of());
        }

        @Test
        void doImport_shouldUpdateUnprotectedRealmRoleNormally() {
            RoleRepresentation existing = role("my_custom_role", "Original description");
            when(roleRepository.getRealmRoles(REALM_NAME)).thenReturn(List.of(existing));

            RealmImport realmImport = realmImportWithRealmRoles(
                    List.of(role("my_custom_role", "Changed description"))
            );

            roleImportService.doImport(realmImport);

            verify(roleRepository).updateRealmRole(eq(REALM_NAME), argThat(r -> "my_custom_role".equals(r.getName())));
        }
    }

    @Nested
    class ProtectedRealmRoleDeletion {
        @BeforeEach
        void init() {
            givenProtectedRoles(ProtectedRolesMode.REPLACE, List.of("admin"), Map.of());
            when(managedProperties.getRole()).thenReturn(ImportManagedPropertiesValues.FULL);
            when(importConfigProperties.getRemoteState().isEnabled()).thenReturn(false);
        }

        @Test
        void doImport_shouldNotDeleteProtectedRealmRoleMissingInImport() {
            RoleRepresentation existingAdmin = role("admin", "Original admin description");
            when(roleRepository.getRealmRoles(REALM_NAME)).thenReturn(List.of(existingAdmin));

            RealmImport realmImport = realmImportWithRealmRoles(List.of());

            roleImportService.doImport(realmImport);

            verify(roleRepository, never()).deleteRealmRole(eq(REALM_NAME), argThat(r -> "admin".equals(r.getName())));
        }
    }

    @Nested
    class ProtectedClientRoleUpdate {
        @BeforeEach
        void init() {
            givenProtectedRoles(ProtectedRolesMode.REPLACE, List.of(), Map.of(CLIENT_ID, List.of("manage-users")));
        }

        @Test
        void doImport_shouldNotUpdateProtectedClientRoleWhenItDiffers() {
            RoleRepresentation existing = role("manage-users", "Original description");
            when(roleRepository.getClientRoles(REALM_NAME)).thenReturn(Map.of(CLIENT_ID, List.of(existing)));

            RealmImport realmImport = realmImportWithClientRoles(
                    Map.of(CLIENT_ID, List.of(role("manage-users", "Changed description")))
            );

            roleImportService.doImport(realmImport);

            verify(roleRepository, never()).updateClientRole(anyString(), anyString(), argThat(r -> "manage-users".equals(r.getName())));
        }

        @Test
        void doImport_shouldCreateProtectedClientRoleWhenNotExisting() {
            when(roleRepository.getClientRoles(REALM_NAME)).thenReturn(Map.of(CLIENT_ID, List.of()));

            RealmImport realmImport = realmImportWithClientRoles(
                    Map.of(CLIENT_ID, List.of(role("manage-users", "Newly created")))
            );

            roleImportService.doImport(realmImport);

            verify(roleRepository).createClientRole(eq(REALM_NAME), eq(CLIENT_ID), argThat(r -> "manage-users".equals(r.getName())));
        }
    }

    @Nested
    class WildcardClientProtection {
        @BeforeEach
        void init() {
            givenProtectedRoles(ProtectedRolesMode.REPLACE, List.of(), Map.of("realm-management", List.of("*")));
        }

        @Test
        void doImport_shouldNotUpdateAnyRoleOfWildcardProtectedClient() {
            RoleRepresentation existing = role("some-future-role", "Original description");
            when(roleRepository.getClientRoles(REALM_NAME)).thenReturn(Map.of("realm-management", List.of(existing)));

            RealmImport realmImport = realmImportWithClientRoles(
                    Map.of("realm-management", List.of(role("some-future-role", "Changed description")))
            );

            roleImportService.doImport(realmImport);

            verify(roleRepository, never()).updateClientRole(anyString(), anyString(), argThat(r -> "some-future-role".equals(r.getName())));
        }
    }

    @Nested
    class NamespaceSeparation {
        @BeforeEach
        void init() {
            // "manage-users" protected only as a CLIENT role of realm-management
            givenProtectedRoles(ProtectedRolesMode.REPLACE, List.of(), Map.of("realm-management", List.of("manage-users")));
        }

        @Test
        void doImport_shouldUpdateRealmRoleWithSameNameAsProtectedClientRole() {
            RoleRepresentation existing = role("manage-users", "Original realm role description");
            when(roleRepository.getRealmRoles(REALM_NAME)).thenReturn(List.of(existing));

            RealmImport realmImport = realmImportWithRealmRoles(
                    List.of(role("manage-users", "Changed realm role description"))
            );

            roleImportService.doImport(realmImport);

            verify(roleRepository).updateRealmRole(eq(REALM_NAME), argThat(r -> "manage-users".equals(r.getName())));
        }
    }

    @Nested
    class RealmRoles {

        @Test
        void doImport_shouldNotUpdateRealmRoleWhenOnlyRoleIdDiffers() {
            // Given: an existing realm role and an identical imported role with a different role id
            final RoleRepresentation existingRole = aRealmRole(EXISTING_ROLE_ID, EXISTING_CONTAINER_ID, DESCRIPTION, ATTRIBUTES);
            final RoleRepresentation roleToImport = aRealmRole(IMPORTED_ROLE_ID, EXISTING_CONTAINER_ID, DESCRIPTION, ATTRIBUTES);
            when(roleRepository.getRealmRoles(REALM_NAME)).thenReturn(List.of(existingRole));

            // When: the import runs
            roleImportService.doImport(aRealmImportWithRealmRoles(roleToImport));

            // Then: the differing role id alone is not a change, so no update is sent to Keycloak
            verify(roleRepository, never()).updateRealmRole(anyString(), any());
        }

        @Test
        void doImport_shouldNotUpdateRealmRoleWhenOnlyContainerIdDiffers() {
            // Given: an existing realm role and an identical imported role with a different container id
            final RoleRepresentation existingRole = aRealmRole(EXISTING_ROLE_ID, EXISTING_CONTAINER_ID, DESCRIPTION, ATTRIBUTES);
            final RoleRepresentation roleToImport = aRealmRole(EXISTING_ROLE_ID, IMPORTED_CONTAINER_ID, DESCRIPTION, ATTRIBUTES);
            when(roleRepository.getRealmRoles(REALM_NAME)).thenReturn(List.of(existingRole));

            // When: the import runs
            roleImportService.doImport(aRealmImportWithRealmRoles(roleToImport));

            // Then: the differing container id alone is not a change, so no update is sent to Keycloak
            verify(roleRepository, never()).updateRealmRole(anyString(), any());
        }

        @Test
        void doImport_shouldNotUpdateRealmRoleWhenBothIdentifiersDifferAndContentIsIdentical() {
            // Given: an existing realm role and an imported role differing only in both identifiers
            final RoleRepresentation existingRole = aRealmRole(EXISTING_ROLE_ID, EXISTING_CONTAINER_ID, DESCRIPTION, ATTRIBUTES);
            final RoleRepresentation roleToImport = aRealmRole(IMPORTED_ROLE_ID, IMPORTED_CONTAINER_ID, DESCRIPTION, ATTRIBUTES);
            when(roleRepository.getRealmRoles(REALM_NAME)).thenReturn(List.of(existingRole));

            // When: the import runs
            roleImportService.doImport(aRealmImportWithRealmRoles(roleToImport));

            // Then: identifier-only differences never trigger an update
            verify(roleRepository, never()).updateRealmRole(anyString(), any());
        }

        @Test
        void doImport_shouldNotUpdateRealmRoleWhenImportCarriesNoIdentifiers() {
            // Given: an existing realm role and an imported role without any identifiers (config-as-code case)
            final RoleRepresentation existingRole = aRealmRole(EXISTING_ROLE_ID, EXISTING_CONTAINER_ID, DESCRIPTION, ATTRIBUTES);
            final RoleRepresentation roleToImport = aRealmRole(null, null, DESCRIPTION, ATTRIBUTES);
            when(roleRepository.getRealmRoles(REALM_NAME)).thenReturn(List.of(existingRole));

            // When: the import runs
            roleImportService.doImport(aRealmImportWithRealmRoles(roleToImport));

            // Then: absent identifiers are not a difference, so no update is sent to Keycloak
            verify(roleRepository, never()).updateRealmRole(anyString(), any());
        }

        @Test
        void doImport_shouldNotUpdateRealmRoleWhenExistingRoleCarriesNoIdentifiers() {
            // Given: an existing realm role without identifiers and an imported role carrying identifiers
            final RoleRepresentation existingRole = aRealmRole(null, null, DESCRIPTION, ATTRIBUTES);
            final RoleRepresentation roleToImport = aRealmRole(IMPORTED_ROLE_ID, IMPORTED_CONTAINER_ID, DESCRIPTION, ATTRIBUTES);
            when(roleRepository.getRealmRoles(REALM_NAME)).thenReturn(List.of(existingRole));

            // When: the import runs
            roleImportService.doImport(aRealmImportWithRealmRoles(roleToImport));

            // Then: identifiers present only on the import side are not a difference either
            verify(roleRepository, never()).updateRealmRole(anyString(), any());
        }

        @Test
        void doImport_shouldUpdateRealmRoleWhenIdentifiersAndContentDiffer() {
            // Given: an existing realm role and an imported role differing in identifiers AND content
            final RoleRepresentation existingRole = aRealmRole(EXISTING_ROLE_ID, EXISTING_CONTAINER_ID, DESCRIPTION, ATTRIBUTES);
            final RoleRepresentation roleToImport =
                    aRealmRole(IMPORTED_ROLE_ID, IMPORTED_CONTAINER_ID, CHANGED_DESCRIPTION, CHANGED_ATTRIBUTES);
            when(roleRepository.getRealmRoles(REALM_NAME)).thenReturn(List.of(existingRole));

            // When: the import runs
            roleImportService.doImport(aRealmImportWithRealmRoles(roleToImport));

            // Then: the genuine content change is applied to the existing role, keeping its own identifiers
            final ArgumentCaptor<RoleRepresentation> captor = ArgumentCaptor.forClass(RoleRepresentation.class);
            verify(roleRepository).updateRealmRole(anyString(), captor.capture());

            final RoleRepresentation updatedRole = captor.getValue();
            assertEquals(REALM_ROLE_NAME, updatedRole.getName(), "the existing role must be the update target");
            assertEquals(EXISTING_ROLE_ID, updatedRole.getId(), "the target instance role id must be preserved");
            assertEquals(EXISTING_CONTAINER_ID, updatedRole.getContainerId(), "the target instance container id must be preserved");
            assertEquals(CHANGED_DESCRIPTION, updatedRole.getDescription(), "the new description must be applied");
            assertEquals(CHANGED_ATTRIBUTES, updatedRole.getAttributes(), "the new attributes must be applied");
        }
    }

    @Nested
    class ClientRoles {

        @Test
        void doImport_shouldNotUpdateClientRoleWhenOnlyRoleIdDiffers() {
            // Given: an existing client role and an identical imported role with a different role id
            final RoleRepresentation existingRole = aClientRole(EXISTING_ROLE_ID, EXISTING_CONTAINER_ID, DESCRIPTION, ATTRIBUTES);
            final RoleRepresentation roleToImport = aClientRole(IMPORTED_ROLE_ID, EXISTING_CONTAINER_ID, DESCRIPTION, ATTRIBUTES);
            when(roleRepository.getClientRoles(REALM_NAME)).thenReturn(Map.of(CLIENT_ID, List.of(existingRole)));

            // When: the import runs
            roleImportService.doImport(aRealmImportWithClientRoles(roleToImport));

            // Then: the differing role id alone is not a change, so no update is sent to Keycloak
            verify(roleRepository, never()).updateClientRole(anyString(), anyString(), any());
        }

        @Test
        void doImport_shouldNotUpdateClientRoleWhenOnlyContainerIdDiffers() {
            // Given: an existing client role and an identical imported role with a different container id
            final RoleRepresentation existingRole = aClientRole(EXISTING_ROLE_ID, EXISTING_CONTAINER_ID, DESCRIPTION, ATTRIBUTES);
            final RoleRepresentation roleToImport = aClientRole(EXISTING_ROLE_ID, IMPORTED_CONTAINER_ID, DESCRIPTION, ATTRIBUTES);
            when(roleRepository.getClientRoles(REALM_NAME)).thenReturn(Map.of(CLIENT_ID, List.of(existingRole)));

            // When: the import runs
            roleImportService.doImport(aRealmImportWithClientRoles(roleToImport));

            // Then: the differing container id alone is not a change, so no update is sent to Keycloak
            verify(roleRepository, never()).updateClientRole(anyString(), anyString(), any());
        }

        @Test
        void doImport_shouldNotUpdateClientRoleWhenBothIdentifiersDifferAndContentIsIdentical() {
            // Given: an existing client role and an imported role differing only in both identifiers
            final RoleRepresentation existingRole = aClientRole(EXISTING_ROLE_ID, EXISTING_CONTAINER_ID, DESCRIPTION, ATTRIBUTES);
            final RoleRepresentation roleToImport = aClientRole(IMPORTED_ROLE_ID, IMPORTED_CONTAINER_ID, DESCRIPTION, ATTRIBUTES);
            when(roleRepository.getClientRoles(REALM_NAME)).thenReturn(Map.of(CLIENT_ID, List.of(existingRole)));

            // When: the import runs
            roleImportService.doImport(aRealmImportWithClientRoles(roleToImport));

            // Then: identifier-only differences never trigger an update
            verify(roleRepository, never()).updateClientRole(anyString(), anyString(), any());
        }

        @Test
        void doImport_shouldNotUpdateClientRoleWhenImportCarriesNoIdentifiers() {
            // Given: an existing client role and an imported role without any identifiers (config-as-code case)
            final RoleRepresentation existingRole = aClientRole(EXISTING_ROLE_ID, EXISTING_CONTAINER_ID, DESCRIPTION, ATTRIBUTES);
            final RoleRepresentation roleToImport = aClientRole(null, null, DESCRIPTION, ATTRIBUTES);
            when(roleRepository.getClientRoles(REALM_NAME)).thenReturn(Map.of(CLIENT_ID, List.of(existingRole)));

            // When: the import runs
            roleImportService.doImport(aRealmImportWithClientRoles(roleToImport));

            // Then: absent identifiers are not a difference, so no update is sent to Keycloak
            verify(roleRepository, never()).updateClientRole(anyString(), anyString(), any());
        }

        @Test
        void doImport_shouldNotUpdateClientRoleWhenExistingRoleCarriesNoIdentifiers() {
            // Given: an existing client role without identifiers and an imported role carrying identifiers
            final RoleRepresentation existingRole = aClientRole(null, null, DESCRIPTION, ATTRIBUTES);
            final RoleRepresentation roleToImport = aClientRole(IMPORTED_ROLE_ID, IMPORTED_CONTAINER_ID, DESCRIPTION, ATTRIBUTES);
            when(roleRepository.getClientRoles(REALM_NAME)).thenReturn(Map.of(CLIENT_ID, List.of(existingRole)));

            // When: the import runs
            roleImportService.doImport(aRealmImportWithClientRoles(roleToImport));

            // Then: identifiers present only on the import side are not a difference either
            verify(roleRepository, never()).updateClientRole(anyString(), anyString(), any());
        }

        @Test
        void doImport_shouldUpdateClientRoleWhenIdentifiersAndContentDiffer() {
            // Given: an existing client role and an imported role differing in identifiers AND content
            final RoleRepresentation existingRole = aClientRole(EXISTING_ROLE_ID, EXISTING_CONTAINER_ID, DESCRIPTION, ATTRIBUTES);
            final RoleRepresentation roleToImport =
                    aClientRole(IMPORTED_ROLE_ID, IMPORTED_CONTAINER_ID, CHANGED_DESCRIPTION, CHANGED_ATTRIBUTES);
            when(roleRepository.getClientRoles(REALM_NAME)).thenReturn(Map.of(CLIENT_ID, List.of(existingRole)));

            // When: the import runs
            roleImportService.doImport(aRealmImportWithClientRoles(roleToImport));

            // Then: the genuine content change is applied to the existing role of the intended client
            final ArgumentCaptor<RoleRepresentation> captor = ArgumentCaptor.forClass(RoleRepresentation.class);
            verify(roleRepository).updateClientRole(anyString(), anyString(), captor.capture());

            final RoleRepresentation updatedRole = captor.getValue();
            assertEquals(CLIENT_ROLE_NAME, updatedRole.getName(), "the existing role must be the update target");
            assertEquals(EXISTING_ROLE_ID, updatedRole.getId(), "the target instance role id must be preserved");
            assertEquals(EXISTING_CONTAINER_ID, updatedRole.getContainerId(), "the target instance container id must be preserved");
            assertEquals(CHANGED_DESCRIPTION, updatedRole.getDescription(), "the new description must be applied");
            assertEquals(CHANGED_ATTRIBUTES, updatedRole.getAttributes(), "the new attributes must be applied");
        }
    }

    private static RealmImport aRealmImportWithRealmRoles(RoleRepresentation... realmRoles) {
        final RolesRepresentation roles = new RolesRepresentation();
        roles.setRealm(List.of(realmRoles));

        return aRealmImport(roles);
    }

    private static RealmImport aRealmImportWithClientRoles(RoleRepresentation... clientRoles) {
        final RolesRepresentation roles = new RolesRepresentation();
        roles.setClient(Map.of(CLIENT_ID, List.of(clientRoles)));

        return aRealmImport(roles);
    }

    private static RealmImport aRealmImport(RolesRepresentation roles) {
        final RealmImport realmImport = new RealmImport();
        realmImport.setRealm(REALM_NAME);
        realmImport.setRoles(roles);

        return realmImport;
    }

    private static RoleRepresentation aRealmRole(String id, String containerId, String description,
                                                 Map<String, List<String>> attributes) {
        return aRole(REALM_ROLE_NAME, false, id, containerId, description, attributes);
    }

    private static RoleRepresentation aClientRole(String id, String containerId, String description,
                                                  Map<String, List<String>> attributes) {
        return aRole(CLIENT_ROLE_NAME, true, id, containerId, description, attributes);
    }

    private static RoleRepresentation aRole(String name, boolean clientRole, String id, String containerId,
                                            String description, Map<String, List<String>> attributes) {
        final RoleRepresentation role = new RoleRepresentation();
        role.setName(name);
        role.setId(id);
        role.setContainerId(containerId);
        role.setDescription(description);
        role.setComposite(false);
        role.setClientRole(clientRole);
        role.setAttributes(attributes);

        return role;
    }
}
