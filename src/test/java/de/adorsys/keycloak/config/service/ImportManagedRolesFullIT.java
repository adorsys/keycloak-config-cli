/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2020 adorsys GmbH & Co. KG @ https://adorsys.com
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
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

@TestPropertySource(properties = {
        "import.managed.role=full",
})
class ImportManagedRolesFullIT extends AbstractImportTest {
    private static final String REALM_NAME = "realmWithManagedFull";

    ImportManagedRolesFullIT() {
        this.resourcePath = "import-files/managed-roles-full";
    }

    @Test
    @Order(0)
    void shouldCreateSimpleRealm() {
        doImport("0_create_realm.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME)
                .partialExport(true, true);

        RoleRepresentation role;

        role = keycloakRepository.getRealmRole(
                realm,
                "my_realm_role"
        );
        assertThat(role.getName(), is("my_realm_role"));
        assertThat(role.isComposite(), is(false));
        assertThat(role.getClientRole(), is(false));
        assertThat(role.getDescription(), is("My realm role"));

        role = keycloakRepository.getRealmRole(
                realm,
                "my_other_realm_role"
        );
        assertThat(role.getName(), is("my_other_realm_role"));
        assertThat(role.isComposite(), is(false));
        assertThat(role.getClientRole(), is(false));
        assertThat(role.getDescription(), is("My other realm role"));

        role = keycloakRepository.getClientRole(
                realm,
                "moped-client",
                "my_client_role"
        );
        assertThat(role.getName(), is("my_client_role"));
        assertThat(role.isComposite(), is(false));
        assertThat(role.getClientRole(), is(true));
        assertThat(role.getDescription(), is("My moped-client role"));

        role = keycloakRepository.getClientRole(
                realm,
                "moped-client", "my_other_client_role"
        );
        assertThat(role.getName(), is("my_other_client_role"));
        assertThat(role.isComposite(), is(false));
        assertThat(role.getClientRole(), is(true));
        assertThat(role.getDescription(), is("My other moped-client role"));
    }

    @Test
    @Order(1)
    void shouldUpdateRealmDeleteOne() {
        doImport("1_update_realm_remove-roles.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME)
                .partialExport(true, true);

        RoleRepresentation role;

        role = keycloakRepository.getRealmRole(
                realm,
                "my_realm_role"
        );
        assertThat(role, nullValue());

        role = keycloakRepository.getRealmRole(
                realm,
                "my_other_realm_role"
        );
        assertThat(role.getName(), is("my_other_realm_role"));
        assertThat(role.isComposite(), is(false));
        assertThat(role.getClientRole(), is(false));
        assertThat(role.getDescription(), is("My other realm role"));

        role = keycloakRepository.getClientRole(
                realm,
                "moped-client",
                "my_client_role"
        );
        assertThat(role.getName(), is("my_client_role"));
        assertThat(role.isComposite(), is(false));
        assertThat(role.getClientRole(), is(true));
        assertThat(role.getDescription(), is("My moped-client role"));

        role = keycloakRepository.getClientRole(
                realm,
                "moped-client", "my_other_client_role"
        );
        assertThat(role, nullValue());
    }

    @Test
    @Order(2)
    void shouldUpdateRealmSkipRealmRole() {
        doImport("2_update_skip-realm-role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME)
                .partialExport(true, true);

        RoleRepresentation role;

        role = keycloakRepository.getRealmRole(
                realm,
                "my_realm_role"
        );
        assertThat(role, nullValue());

        role = keycloakRepository.getRealmRole(
                realm,
                "my_other_realm_role"
        );
        assertThat(role.getName(), is("my_other_realm_role"));
        assertThat(role.isComposite(), is(false));
        assertThat(role.getClientRole(), is(false));
        assertThat(role.getDescription(), is("My other realm role"));

        role = keycloakRepository.getClientRole(
                realm,
                "moped-client",
                "my_client_role"
        );
        assertThat(role.getName(), is("my_client_role"));
        assertThat(role.isComposite(), is(false));
        assertThat(role.getClientRole(), is(true));
        assertThat(role.getDescription(), is("My moped-client role"));

        role = keycloakRepository.getClientRole(
                realm,
                "moped-client", "my_other_client_role"
        );
        assertThat(role, nullValue());
    }


    @Test
    @Order(3)
    void shouldUpdateRealmSkipClientRole() {
        doImport("3_update_skip-client-role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME)
                .partialExport(true, true);

        RoleRepresentation role;

        role = keycloakRepository.getRealmRole(
                realm,
                "my_realm_role"
        );
        assertThat(role, nullValue());

        role = keycloakRepository.getRealmRole(
                realm,
                "my_other_realm_role"
        );
        assertThat(role.getName(), is("my_other_realm_role"));
        assertThat(role.isComposite(), is(false));
        assertThat(role.getClientRole(), is(false));
        assertThat(role.getDescription(), is("My other realm role"));

        role = keycloakRepository.getClientRole(
                realm,
                "moped-client",
                "my_client_role"
        );
        assertThat(role.getName(), is("my_client_role"));
        assertThat(role.isComposite(), is(false));
        assertThat(role.getClientRole(), is(true));
        assertThat(role.getDescription(), is("My moped-client role"));

        role = keycloakRepository.getClientRole(
                realm,
                "moped-client", "my_other_client_role"
        );
        assertThat(role, nullValue());
    }


    @Test
    @Order(4)
    void shouldUpdateRealmSkipAllRoles() {
        doImport("4.1_update_realm_skip-all-roles.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME)
                .partialExport(true, true);

        RoleRepresentation role;

        role = keycloakRepository.getRealmRole(
                realm,
                "my_realm_role"
        );
        assertThat(role, nullValue());

        role = keycloakRepository.getRealmRole(
                realm,
                "my_other_realm_role"
        );
        assertThat(role.getName(), is("my_other_realm_role"));
        assertThat(role.isComposite(), is(false));
        assertThat(role.getClientRole(), is(false));
        assertThat(role.getDescription(), is("My other realm role"));

        role = keycloakRepository.getClientRole(
                realm,
                "moped-client",
                "my_client_role"
        );
        assertThat(role.getName(), is("my_client_role"));
        assertThat(role.isComposite(), is(false));
        assertThat(role.getClientRole(), is(true));
        assertThat(role.getDescription(), is("My moped-client role"));

        role = keycloakRepository.getClientRole(
                realm,
                "moped-client", "my_other_client_role"
        );
        assertThat(role, nullValue());

        doImport("4.2_update_realm_skip-all-roles.json");

        realm = keycloakProvider.getInstance().realm(REALM_NAME)
                .partialExport(true, true);

        role = keycloakRepository.getRealmRole(
                realm,
                "my_realm_role"
        );
        assertThat(role, nullValue());

        role = keycloakRepository.getRealmRole(
                realm,
                "my_other_realm_role"
        );
        assertThat(role.getName(), is("my_other_realm_role"));
        assertThat(role.isComposite(), is(false));
        assertThat(role.getClientRole(), is(false));
        assertThat(role.getDescription(), is("My other realm role"));

        role = keycloakRepository.getClientRole(
                realm,
                "moped-client",
                "my_client_role"
        );
        assertThat(role.getName(), is("my_client_role"));
        assertThat(role.isComposite(), is(false));
        assertThat(role.getClientRole(), is(true));
        assertThat(role.getDescription(), is("My moped-client role"));

        role = keycloakRepository.getClientRole(
                realm,
                "moped-client", "my_other_client_role"
        );
        assertThat(role, nullValue());
    }


    @Test
    @Order(5)
    void shouldUpdateRealmDeleteAllRoles() {
        doImport("5_update_realm_delete-all-roles.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME)
                .partialExport(true, true);

        RoleRepresentation role;

        role = keycloakRepository.getRealmRole(
                realm,
                "my_realm_role"
        );
        assertThat(role, nullValue());

        role = keycloakRepository.getRealmRole(
                realm,
                "my_other_realm_role"
        );
        assertThat(role, nullValue());

        role = keycloakRepository.getClientRole(
                realm,
                "moped-client",
                "my_client_role"
        );
        assertThat(role, nullValue());

        role = keycloakRepository.getClientRole(
                realm,
                "moped-client", "my_other_client_role"
        );
        assertThat(role, nullValue());
    }
}


@TestPropertySource(properties = {
        "import.managed.role=full",
        "import.state=false",
})
class ImportManagedRolesFullWithoutStateIT extends AbstractImportTest {

    private static final String REALM_NAME = "realmWithManagedFullWithoutState";

    ImportManagedRolesFullWithoutStateIT() {
        this.resourcePath = "import-files/managed-roles-full/without-state/";
    }

    @Test
    @Order(0)
    void shouldCreateSimpleRealm() {
        doImport("0_create_realm.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME)
                .partialExport(true, true);

        RoleRepresentation role;

        role = keycloakRepository.getRealmRole(
                realm,
                "my_realm_role"
        );
        assertThat(role.getName(), is("my_realm_role"));
        assertThat(role.isComposite(), is(false));
        assertThat(role.getClientRole(), is(false));
        assertThat(role.getDescription(), is("My realm role"));

        role = keycloakRepository.getRealmRole(
                realm,
                "my_other_realm_role"
        );
        assertThat(role.getName(), is("my_other_realm_role"));
        assertThat(role.isComposite(), is(false));
        assertThat(role.getClientRole(), is(false));
        assertThat(role.getDescription(), is("My other realm role"));

        role = keycloakRepository.getClientRole(
                realm,
                "moped-client",
                "my_client_role"
        );
        assertThat(role.getName(), is("my_client_role"));
        assertThat(role.isComposite(), is(false));
        assertThat(role.getClientRole(), is(true));
        assertThat(role.getDescription(), is("My moped-client role"));

        role = keycloakRepository.getClientRole(
                realm,
                "moped-client", "my_other_client_role"
        );
        assertThat(role.getName(), is("my_other_client_role"));
        assertThat(role.isComposite(), is(false));
        assertThat(role.getClientRole(), is(true));
        assertThat(role.getDescription(), is("My other moped-client role"));

        role = keycloakRepository.getRealmRole(
                realm, "offline_access"
        );
        assertThat(role, notNullValue());

        role = keycloakRepository.getRealmRole(
                realm, "uma_authorization"
        );
        assertThat(role, notNullValue());

        role = keycloakRepository.getClientRole(
                realm,
                "broker", "read-token"
        );
        assertThat(role, notNullValue());
    }
}
