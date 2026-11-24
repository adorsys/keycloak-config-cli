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
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleCompositeRepositoryTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private RoleResource roleResource;
    @Mock
    private ClientResource firstClientResource;
    @Mock
    private ClientResource secondClientResource;

    private RoleCompositeRepository repository;

    @BeforeEach
    void setUp() {
        repository = new RoleCompositeRepository(roleRepository, clientRepository);

        when(roleResource.getRoleComposites()).thenReturn(Set.of(
                clientRole("first-role", "first-container"),
                clientRole("second-role", "first-container"),
                clientRole("third-role", "second-container"),
                nonClientRole("realm-role")
        ));
        when(clientRepository.getResourceById("test-realm", "first-container"))
                .thenReturn(firstClientResource);
        when(clientRepository.getResourceById("test-realm", "second-container"))
                .thenReturn(secondClientResource);
        when(firstClientResource.toRepresentation()).thenReturn(client("first-client"));
        when(secondClientResource.toRepresentation()).thenReturn(client("second-client"));
    }

    @Test
    void shouldResolveOnlyClientsReferencedByRealmRoleComposites() {
        when(roleRepository.loadRealmRole("test-realm", "source-role")).thenReturn(roleResource);

        Map<String, java.util.List<String>> result = repository.searchRealmRoleClientComposites(
                "test-realm", "source-role");

        assertThat(result).containsOnlyKeys("first-client", "second-client");
        assertThat(result.get("first-client")).containsExactlyInAnyOrder("first-role", "second-role");
        assertThat(result.get("second-client")).containsExactly("third-role");
        verifyReferencedClientsOnly();
    }

    @Test
    void shouldResolveOnlyClientsReferencedByClientRoleComposites() {
        when(roleRepository.loadClientRole("test-realm", "source-client", "source-role"))
                .thenReturn(roleResource);

        Map<String, java.util.List<String>> result = repository.searchClientRoleClientComposites(
                "test-realm", "source-client", "source-role");

        assertThat(result).containsOnlyKeys("first-client", "second-client");
        assertThat(result.get("first-client")).containsExactlyInAnyOrder("first-role", "second-role");
        assertThat(result.get("second-client")).containsExactly("third-role");
        verifyReferencedClientsOnly();
    }

    private void verifyReferencedClientsOnly() {
        verify(clientRepository).getResourceById("test-realm", "first-container");
        verify(clientRepository).getResourceById("test-realm", "second-container");
        verify(clientRepository, never()).getAll(anyString());
        verify(clientRepository, never()).getAllIds(anyString());
    }

    private RoleRepresentation clientRole(String name, String containerId) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(name);
        role.setClientRole(true);
        role.setContainerId(containerId);
        return role;
    }

    private RoleRepresentation nonClientRole(String name) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(name);
        role.setClientRole(null);
        return role;
    }

    private ClientRepresentation client(String clientId) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        return client;
    }
}
