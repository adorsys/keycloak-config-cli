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

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.repository.RoleRepository;
import de.adorsys.keycloak.config.service.rolecomposites.client.ClientRoleCompositeImportService;
import de.adorsys.keycloak.config.service.rolecomposites.realm.RealmRoleCompositeImportService;
import de.adorsys.keycloak.config.service.state.StateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Change-detection tests for {@link RoleImportService}.
 *
 * <p>Server-generated identifiers ({@code id} of the role itself and {@code containerId} of its owning
 * realm/client) must never be interpreted as a content change: an import file typically carries the
 * identifiers of the instance it was exported from, which never match the target instance.
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

    private RoleImportService service;

    @BeforeEach
    void setUp() {
        roleRepository = mock(RoleRepository.class);
        importConfigProperties = mock(ImportConfigProperties.class);
        final ImportConfigProperties.ImportManagedProperties managed =
                mock(ImportConfigProperties.ImportManagedProperties.class);

        when(importConfigProperties.getManaged()).thenReturn(managed);
        when(managed.getRole()).thenReturn(ImportManagedPropertiesValues.NO_DELETE);
        when(importConfigProperties.isParallel()).thenReturn(false);

        service = new RoleImportService(
                mock(RealmRoleCompositeImportService.class),
                mock(ClientRoleCompositeImportService.class),
                roleRepository,
                importConfigProperties,
                mock(StateService.class)
        );
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
            service.doImport(aRealmImportWithRealmRoles(roleToImport));

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
            service.doImport(aRealmImportWithRealmRoles(roleToImport));

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
            service.doImport(aRealmImportWithRealmRoles(roleToImport));

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
            service.doImport(aRealmImportWithRealmRoles(roleToImport));

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
            service.doImport(aRealmImportWithRealmRoles(roleToImport));

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
            service.doImport(aRealmImportWithRealmRoles(roleToImport));

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
            service.doImport(aRealmImportWithClientRoles(roleToImport));

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
            service.doImport(aRealmImportWithClientRoles(roleToImport));

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
            service.doImport(aRealmImportWithClientRoles(roleToImport));

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
            service.doImport(aRealmImportWithClientRoles(roleToImport));

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
            service.doImport(aRealmImportWithClientRoles(roleToImport));

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
            service.doImport(aRealmImportWithClientRoles(roleToImport));

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
