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

package de.adorsys.keycloak.config.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RoleCompositeRepositoryTest {

    private static final String REALM_NAME = "test-realm";
    private static final String ROLE_NAME = "test-role";
    private static final String CLIENT_ID = "moped-client";
    private static final String CLIENT_INTERNAL_ID = "internal-client-id";

    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final ClientRepository clientRepository = mock(ClientRepository.class);
    private final RoleResource roleResource = mock(RoleResource.class);
    private final ClientResource clientResource = mock(ClientResource.class);
    private final RoleCompositeRepository repository = new RoleCompositeRepository(roleRepository, clientRepository);

    @BeforeEach
    void setUp() {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);

        when(roleRepository.loadRealmRole(REALM_NAME, ROLE_NAME)).thenReturn(roleResource);
        when(clientRepository.getResourceById(REALM_NAME, CLIENT_INTERNAL_ID)).thenReturn(clientResource);
        when(clientResource.toRepresentation()).thenReturn(client);
    }

    @Test
    void shouldGroupExistingClientCompositesWithoutLoadingAllClients() {
        RoleRepresentation clientRole = role("client-role", true, CLIENT_INTERNAL_ID);
        RoleRepresentation otherClientRole = role("other-client-role", true, CLIENT_INTERNAL_ID);
        RoleRepresentation realmRole = role("realm-role", false, "realm");
        RoleRepresentation unknownRoleType = role("unknown-role-type", null, "realm");

        when(roleResource.getRoleComposites()).thenReturn(Set.of(
                clientRole,
                otherClientRole,
                realmRole,
                unknownRoleType
        ));

        Map<String, List<String>> result = repository.searchRealmRoleClientComposites(REALM_NAME, ROLE_NAME);

        assertThat(result).containsOnlyKeys(CLIENT_ID);
        assertThat(result.get(CLIENT_ID)).containsExactlyInAnyOrder("client-role", "other-client-role");
        verify(clientRepository, never()).getAll(REALM_NAME);
        verify(clientRepository).getResourceById(REALM_NAME, CLIENT_INTERNAL_ID);
    }

    private RoleRepresentation role(String name, Boolean clientRole, String containerId) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(name);
        role.setClientRole(clientRole);
        role.setContainerId(containerId);
        return role;
    }
}
