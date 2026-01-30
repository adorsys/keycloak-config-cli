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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RealmRoleCompositeImportServiceTest {

    private RealmCompositeImport realmCompositeImport;
    private ClientCompositeImport clientCompositeImport;
    private RealmRoleCompositeImportService realmRoleCompositeImportService;

    @BeforeEach
    void setUp() {
        realmCompositeImport = mock(RealmCompositeImport.class);
        clientCompositeImport = mock(ClientCompositeImport.class);
        realmRoleCompositeImportService = new RealmRoleCompositeImportService(realmCompositeImport,
                clientCompositeImport);
    }

    @Test
    void shouldUpdateWithEmptySetWhenCompositesAreNull() {
        String realmName = "test-realm";
        RoleRepresentation role = new RoleRepresentation();
        role.setName("test-role");
        role.setComposites(null);

        realmRoleCompositeImportService.update(realmName, List.of(role));

        verify(realmCompositeImport).update(eq(realmName), eq(role), eq(Set.of()));
    }

    @Test
    void shouldUpdateWithEmptySetWhenRealmCompositesAreNull() {
        String realmName = "test-realm";
        RoleRepresentation role = new RoleRepresentation();
        role.setName("test-role");
        RoleRepresentation.Composites composites = new RoleRepresentation.Composites();
        composites.setRealm(null);
        role.setComposites(composites);

        realmRoleCompositeImportService.update(realmName, List.of(role));

        verify(realmCompositeImport).update(eq(realmName), eq(role), eq(Set.of()));
    }

    @Test
    void shouldUpdateWithActualSetWhenRealmCompositesArePresent() {
        String realmName = "test-realm";
        RoleRepresentation role = new RoleRepresentation();
        role.setName("test-role");
        RoleRepresentation.Composites composites = new RoleRepresentation.Composites();
        Set<String> realmComposites = Set.of("composite-1", "composite-2");
        composites.setRealm(realmComposites);
        role.setComposites(composites);

        realmRoleCompositeImportService.update(realmName, List.of(role));

        verify(realmCompositeImport).update(eq(realmName), eq(role), eq(realmComposites));
    }
}
