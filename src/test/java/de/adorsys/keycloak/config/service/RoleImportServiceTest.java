/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2021 adorsys GmbH & Co. KG @ https://adorsys.com
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
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportProtectedRolesProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportProtectedRolesProperties.ProtectedRolesMode;
import de.adorsys.keycloak.config.repository.RoleRepository;
import de.adorsys.keycloak.config.service.rolecomposites.client.ClientRoleCompositeImportService;
import de.adorsys.keycloak.config.service.rolecomposites.realm.RealmRoleCompositeImportService;
import de.adorsys.keycloak.config.service.state.StateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * STUB - no implementation, defines contract and should be deleted.
 * Exercises RoleImportService's "protected role" guard: production code is expected
 * to build/consult a ProtectedRoleResolver derived from
 * importConfigProperties.getProtectedRoles() before writing to any role.
 */
class RoleImportServiceTest {

    private static final String REALM_NAME = "test-realm";
    private static final String CLIENT_ID = "moped-client";

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

        roleImportService = new RoleImportService(
                realmRoleCompositeImport,
                clientRoleCompositeImport,
                roleRepository,
                importConfigProperties,
                stateService
        );

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger(RoleImportService.class);
        logger.setLevel(Level.DEBUG);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    private void givenProtectedRoles(ProtectedRolesMode mode, List<String> realmRoles, Map<String, List<String>> clientRoles) {
        Map<String, java.util.Collection<String>> clientRolesAsCollections = new java.util.HashMap<>(clientRoles);
        when(importConfigProperties.getProtectedRoles())
                .thenReturn(new ImportProtectedRolesProperties(mode, realmRoles, clientRolesAsCollections));
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
            org.junit.jupiter.api.Assertions.assertTrue(loggedSkip,
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
}
