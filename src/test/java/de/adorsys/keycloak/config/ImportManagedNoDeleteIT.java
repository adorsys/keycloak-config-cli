/*
 * Copyright 2019-2020 adorsys GmbH & Co. KG @ https://adorsys.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package de.adorsys.keycloak.config;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.*;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

@TestPropertySource(properties = {
        "import.managed.group=no-delete",
        "import.managed.required-action=no-delete",
        "import.managed.client-scope=no-delete",
        "import.managed.scope-mapping=no-delete",
})
public class ImportManagedNoDeleteIT extends AbstractImportTest {
    private static final String REALM_NAME = "realmWithNoDelete";

    ImportManagedNoDeleteIT() {
        this.resourcePath = "import-files/managed-no-delete";
    }

    @Test
    @Order(0)
    public void shouldCreateSimpleRealm() {
        doImport("0_create_simple-realm.json");
        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        List<GroupRepresentation> createdGroup = createdRealm.getGroups();
        assertThat(createdGroup, hasSize(2));

        List<RequiredActionProviderRepresentation> createdRequiredActions = createdRealm.getRequiredActions()
                .stream()
                .filter((action) -> action.getAlias().equals("MY_CONFIGURE_TOTP") || action.getAlias().equals("my_terms_and_conditions"))
                .collect(Collectors.toList());
        assertThat(createdRequiredActions, hasSize(2));

        List<ClientScopeRepresentation> createdClientScopes = createdRealm.getClientScopes()
                .stream()
                .filter((clientScope) -> clientScope.getName().equals("my_clientScope") || clientScope.getName().equals("my_other_clientScope"))
                .collect(Collectors.toList());
        assertThat(createdClientScopes, hasSize(2));

        List<ScopeMappingRepresentation> createdScopeMappings = createdRealm.getScopeMappings()
                .stream()
                .filter((scopeMapping) -> scopeMapping.getClientScope().equals("offline_access"))
                .collect(Collectors.toList());
        assertThat(createdScopeMappings, hasSize(1));
    }

    @Test
    @Order(1)
    public void shouldUpdateRealmNotDeleteOne() {
        doImport("1_update-realm_not-delete-one.json");
        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        List<GroupRepresentation> createdGroup = createdRealm.getGroups();
        assertThat(createdGroup, hasSize(2));

        List<RequiredActionProviderRepresentation> createdRequiredActions = createdRealm.getRequiredActions()
                .stream()
                .filter((action) -> action.getAlias().equals("MY_CONFIGURE_TOTP") || action.getAlias().equals("my_terms_and_conditions"))
                .collect(Collectors.toList());
        assertThat(createdRequiredActions, hasSize(2));

        List<ClientScopeRepresentation> createdClientScopes = createdRealm.getClientScopes()
                .stream()
                .filter((clientScope) -> clientScope.getName().equals("my_clientScope") || clientScope.getName().equals("my_other_clientScope"))
                .collect(Collectors.toList());
        assertThat(createdClientScopes, hasSize(2));

        List<ScopeMappingRepresentation> createdScopeMappings = createdRealm.getScopeMappings()
                .stream()
                .filter((scopeMapping) -> scopeMapping.getClientScope().equals("offline_access"))
                .collect(Collectors.toList());
        assertThat(createdScopeMappings, hasSize(1));
    }

    @Test
    @Order(2)
    public void shouldUpdateRealmNotDeleteAll() {
        doImport("2_update-realm_not-delete-all.json");
        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        List<GroupRepresentation> createdGroup = createdRealm.getGroups();
        assertThat(createdGroup, hasSize(2));

        List<RequiredActionProviderRepresentation> createdRequiredActions = createdRealm.getRequiredActions()
                .stream()
                .filter((action) -> action.getAlias().equals("MY_CONFIGURE_TOTP") || action.getAlias().equals("my_terms_and_conditions"))
                .collect(Collectors.toList());
        assertThat(createdRequiredActions, hasSize(2));

        List<ClientScopeRepresentation> createdClientScopes = createdRealm.getClientScopes()
                .stream()
                .filter((clientScope) -> clientScope.getName().equals("my_clientScope") || clientScope.getName().equals("my_other_clientScope"))
                .collect(Collectors.toList());
        assertThat(createdClientScopes, hasSize(2));

        List<ScopeMappingRepresentation> createdScopeMappings = createdRealm.getScopeMappings()
                .stream()
                .filter((scopeMapping) -> scopeMapping.getClientScope().equals("offline_access"))
                .collect(Collectors.toList());
        assertThat(createdScopeMappings, hasSize(1));
    }
}
