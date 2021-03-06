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

import de.adorsys.keycloak.config.AbstractImportTest;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;

class ImportClientScopeMappingsIT extends AbstractImportTest {
    private static final String REALM_NAME = "realmWithClientScopeMappings";

    ImportClientScopeMappingsIT() {
        this.resourcePath = "import-files/client-scope-mappings";
    }

    @Test
    @Order(0)
    void shouldCreateRealmWithClientScopeMapping() throws IOException {
        doImport("00_create_realm_with_client_scope_mapping.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME)
                .partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        Map<String, List<ScopeMappingRepresentation>> clientScopeMappings = realm.getClientScopeMappings();
        assertThat(clientScopeMappings, notNullValue());
        assertThat(clientScopeMappings, hasKey("moped-client"));

        ScopeMappingRepresentation clientScopeMapping = clientScopeMappings.get("moped-client").get(0);
        assertThat(clientScopeMapping.getClient(), is("other-moped-client"));
        assertThat(clientScopeMapping.getRoles(), contains("moped-role"));
    }

    @Test
    @Order(1)
    void shouldUpdateRealmAddRoleToClientScopeMapping() throws IOException {
        doImport("01_update_realm_add_role_to_client_scope_mapping.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME)
                .partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        Map<String, List<ScopeMappingRepresentation>> clientScopeMappings = realm.getClientScopeMappings();
        assertThat(clientScopeMappings, notNullValue());
        assertThat(clientScopeMappings, hasKey("moped-client"));

        ScopeMappingRepresentation clientScopeMapping = clientScopeMappings.get("moped-client").get(0);
        assertThat(clientScopeMapping.getClient(), is("other-moped-client"));
        assertThat(clientScopeMapping.getRoles(), containsInAnyOrder("moped-role", "2nd-moped-role", "3rd-moped-role"));
    }

    @Test
    @Order(2)
    void shouldUpdateRealmRemoveRoleToClientScopeMapping() throws IOException {
        doImport("02_update_realm_remove_role_to_client_scope_mapping.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME)
                .partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        Map<String, List<ScopeMappingRepresentation>> clientScopeMappings = realm.getClientScopeMappings();
        assertThat(clientScopeMappings, notNullValue());
        assertThat(clientScopeMappings, hasKey("moped-client"));

        ScopeMappingRepresentation clientScopeMapping = clientScopeMappings.get("moped-client").get(0);
        assertThat(clientScopeMapping.getClient(), is("other-moped-client"));
        assertThat(clientScopeMapping.getRoles(), containsInAnyOrder("2nd-moped-role", "3rd-moped-role"));
    }

    @Test
    @Order(3)
    void shouldUpdateRealmRemoveRoleByDeleteRole() throws IOException {
        doImport("03_update_realm_remove_role_by_delete_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME)
                .partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        Map<String, List<ScopeMappingRepresentation>> clientScopeMappings = realm.getClientScopeMappings();
        assertThat(clientScopeMappings, notNullValue());
        assertThat(clientScopeMappings, hasKey("moped-client"));

        ScopeMappingRepresentation clientScopeMapping;
        clientScopeMapping = clientScopeMappings.get("moped-client").get(0);
        assertThat(clientScopeMapping.getClient(), is("other-moped-client"));
        assertThat(clientScopeMapping.getRoles(), contains("2nd-moped-role"));
    }

    @Test
    @Order(4)
    void shouldUpdateRealmAddClientScopeMapping() throws IOException {
        doImport("04_update_realm_add_client_scope_mapping.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME)
                .partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        Map<String, List<ScopeMappingRepresentation>> clientScopeMappings = realm.getClientScopeMappings();
        assertThat(clientScopeMappings, notNullValue());
        assertThat(clientScopeMappings, allOf(hasKey("moped-client"), hasKey("other-moped-client")));

        ScopeMappingRepresentation clientScopeMapping;
        clientScopeMapping = clientScopeMappings.get("moped-client").get(0);
        assertThat(clientScopeMapping.getClient(), is("other-moped-client"));
        assertThat(clientScopeMapping.getRoles(), contains("2nd-moped-role"));

        clientScopeMapping = clientScopeMappings.get("other-moped-client").get(0);
        assertThat(clientScopeMapping.getClient(), is("moped-client"));
        assertThat(clientScopeMapping.getRoles(), contains("other-moped-role"));
    }

    @Test
    @Order(97)
    void shouldUpdateRealmSkipClientScopeMapping() throws IOException {
        doImport("97_update_realm_skip_client_scope_mapping.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME)
                .partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        Map<String, List<ScopeMappingRepresentation>> clientScopeMappings = realm.getClientScopeMappings();
        assertThat(clientScopeMappings, notNullValue());
        assertThat(clientScopeMappings, allOf(hasKey("moped-client"), hasKey("other-moped-client")));

        ScopeMappingRepresentation clientScopeMapping;
        clientScopeMapping = clientScopeMappings.get("moped-client").get(0);
        assertThat(clientScopeMapping.getClient(), is("other-moped-client"));
        assertThat(clientScopeMapping.getRoles(), contains("2nd-moped-role"));

        clientScopeMapping = clientScopeMappings.get("other-moped-client").get(0);
        assertThat(clientScopeMapping.getClient(), is("moped-client"));
        assertThat(clientScopeMapping.getRoles(), contains("other-moped-role"));
    }

    @Test
    @Order(98)
    void shouldUpdateRealmRemoveClientScopeMapping() throws IOException {
        doImport("98_update_realm_remove_client_scope_mapping.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME)
                .partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        Map<String, List<ScopeMappingRepresentation>> clientScopeMappings = realm.getClientScopeMappings();
        assertThat(clientScopeMappings, notNullValue());
        assertThat(clientScopeMappings, hasKey("other-moped-client"));

        ScopeMappingRepresentation clientScopeMapping = clientScopeMappings.get("other-moped-client").get(0);
        assertThat(clientScopeMapping.getClient(), is("moped-client"));
        assertThat(clientScopeMapping.getRoles(), contains("other-moped-role"));
    }

    @Test
    @Order(99)
    void shouldUpdateRealmRemoveAllClientScopeMapping() throws IOException {
        doImport("99_update_realm_remove_all_client_scope_mapping.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME)
                .partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        Map<String, List<ScopeMappingRepresentation>> clientScopeMappings = realm.getClientScopeMappings();
        assertThat(clientScopeMappings, nullValue());
    }
}
