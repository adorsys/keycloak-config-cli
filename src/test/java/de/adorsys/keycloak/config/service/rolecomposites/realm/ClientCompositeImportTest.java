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

package de.adorsys.keycloak.config.service.rolecomposites.realm;

import de.adorsys.keycloak.config.repository.RoleCompositeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientCompositeImportTest {

    @Mock
    private RoleCompositeRepository repository;

    @Test
    void shouldRetainDeclaredClientsAndRemoveOnlyAbsentClientComposites() {
        RoleRepresentation retainedRole = new RoleRepresentation();
        retainedRole.setName("retained-role");
        when(repository.searchRealmRoleClientComposites(
                "test-realm", "source-role", "retained-client"))
                .thenReturn(Set.of(retainedRole));
        when(repository.searchRealmRoleClientComposites("test-realm", "source-role"))
                .thenReturn(Map.of(
                        "retained-client", List.of("retained-role"),
                        "removed-client", List.of("removed-role")
                ));

        new ClientCompositeImport(repository).update(
                "test-realm",
                "source-role",
                Map.of("retained-client", List.of("retained-role"))
        );

        verify(repository).removeRealmRoleClientComposites(
                "test-realm",
                "source-role",
                Map.of("removed-client", List.of("removed-role"))
        );
        verify(repository, never()).addRealmRoleClientComposites(
                "test-realm", "source-role", "retained-client", Set.of());
    }
}
