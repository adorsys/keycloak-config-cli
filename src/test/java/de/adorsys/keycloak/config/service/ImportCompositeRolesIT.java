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

import de.adorsys.keycloak.config.AbstractImportIT;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;

@SuppressWarnings("java:S5961")
class ImportCompositeRolesIT extends AbstractImportIT {
    private static final String REALM_NAME = "realmwithcompositeroles";

    ImportCompositeRolesIT() {
        this.resourcePath = "import-files/roles";
    }

    @Test
    @Order(0)
    void shouldCreateRealmWithCompositeRoles() throws IOException {
        doImport("72.1_update_composite_realm_roles_with_roles.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                realm, "my_composite_realm_role");

        assertThat(realmRole.getName(), is("my_composite_realm_role"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(false));
        assertThat(realmRole.getDescription(), is("My composite realm role"));
        assertThat(realmRole.getComposites().getRealm(), hasSize(2));
        assertThat(realmRole.getComposites().getRealm(), containsInAnyOrder("my_realm_role", "my_other_realm_role"));
        assertThat(realmRole.getComposites().getClient().get("moped-client"), hasSize(1));

    }

    @Test
    @Order(1)
    void shouldRemoveOneRealmRoleFromCompositeRealmRole() throws IOException {
        doImport("72.2_remove_role_from_composite_realm_roles.json");
        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                realm, "my_composite_realm_role");

        assertThat(realmRole.getName(), is("my_composite_realm_role"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(false));
        assertThat(realmRole.getDescription(), is("My composite realm role"));
        assertThat(realmRole.getComposites().getRealm(), hasSize(1));
        assertThat(realmRole.getComposites().getRealm(), contains("my_realm_role"));
        assertThat(realmRole.getComposites().getClient().get("moped-client"), hasSize(1));
    }

    @Test
    @Order(2)
    void shouldRemoveAllRealmRoleFromCompositeRealmRole() throws IOException {
        doImport("72.3_remove_all_roles_from_composite_realm_roles.json");
        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                realm, "my_composite_realm_role");

        assertThat(realmRole.getName(), is("my_composite_realm_role"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(false));
        assertThat(realmRole.getDescription(), is("My composite realm role"));
        assertThat(realmRole.getComposites().getRealm(), is(nullValue()));
    }

}