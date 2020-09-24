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

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

class ImportRolesIT extends AbstractImportTest {
    private static final String REALM_NAME = "realmWithRoles";

    ImportRolesIT() {
        this.resourcePath = "import-files/roles";
    }

    @Test
    @Order(0)
    void shouldCreateRealmWithRoles() {
        doImport("00_create_realm_with_roles.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RoleRepresentation createdRealmRole = keycloakRepository.getRealmRole(
                REALM_NAME,
                "my_realm_role"
        );

        assertThat(createdRealmRole.getName(), is("my_realm_role"));
        assertThat(createdRealmRole.isComposite(), is(false));
        assertThat(createdRealmRole.getClientRole(), is(false));
        assertThat(createdRealmRole.getDescription(), is("My realm role"));

        RoleRepresentation createdClientRole = keycloakRepository.getClientRole(
                REALM_NAME,
                "moped-client",
                "my_client_role"
        );

        assertThat(createdClientRole.getName(), is("my_client_role"));
        assertThat(createdClientRole.isComposite(), is(false));
        assertThat(createdClientRole.getClientRole(), is(true));
        assertThat(createdClientRole.getDescription(), is("My moped-client role"));
    }

    @Test
    @Order(1)
    void shouldAddRealmRole() {
        doImport("01_update_realm__add_realm_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RoleRepresentation createdRealmRole = keycloakRepository.getRealmRole(
                REALM_NAME,
                "my_other_realm_role"
        );

        assertThat(createdRealmRole.getName(), is("my_other_realm_role"));
        assertThat(createdRealmRole.isComposite(), is(false));
        assertThat(createdRealmRole.getClientRole(), is(false));
        assertThat(createdRealmRole.getDescription(), is("My other realm role"));
    }

    @Test
    @Order(2)
    void shouldAddClientRole() {
        doImport("02_update_realm__add_client_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RoleRepresentation createdRealmRole = keycloakRepository.getClientRole(
                REALM_NAME,
                "moped-client", "my_other_client_role"
        );

        assertThat(createdRealmRole.getName(), is("my_other_client_role"));
        assertThat(createdRealmRole.isComposite(), is(false));
        assertThat(createdRealmRole.getClientRole(), is(true));
        assertThat(createdRealmRole.getDescription(), is("My other moped-client role"));
    }

    @Test
    @Order(3)
    void shouldChangeRealmRole() {
        doImport("03_update_realm__change_realm_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RoleRepresentation createdRealmRole = keycloakRepository.getRealmRole(
                REALM_NAME,
                "my_other_realm_role"
        );

        assertThat(createdRealmRole.getName(), is("my_other_realm_role"));
        assertThat(createdRealmRole.isComposite(), is(false));
        assertThat(createdRealmRole.getClientRole(), is(false));
        assertThat(createdRealmRole.getDescription(), is("My changed other realm role"));
    }

    @Test
    @Order(4)
    void shouldChangeClientRole() {
        doImport("04_update_realm__change_client_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RoleRepresentation createdRealmRole = keycloakRepository.getClientRole(
                REALM_NAME,
                "moped-client", "my_other_client_role"
        );

        assertThat(createdRealmRole.getName(), is("my_other_client_role"));
        assertThat(createdRealmRole.isComposite(), is(false));
        assertThat(createdRealmRole.getClientRole(), is(true));
        assertThat(createdRealmRole.getDescription(), is("My changed other moped-client role"));
    }

    @Test
    @Order(5)
    void shouldAddUserWithRealmRole() {
        doImport("05_update_realm__add_user_with_realm_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        List<String> userRealmLevelRoles = keycloakRepository.getUserRealmLevelRoles(
                REALM_NAME,
                "myuser"
        );

        assertThat(userRealmLevelRoles, hasItem("my_realm_role"));
    }

    @Test
    @Order(6)
    void shouldAddUserWithClientRole() {
        doImport("06_update_realm__add_user_with_client_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        List<String> userClientLevelRoles = keycloakRepository.getUserClientLevelRoles(
                REALM_NAME,
                "myotheruser",
                "moped-client"
        );

        assertThat(userClientLevelRoles, hasItem("my_client_role"));
    }

    @Test
    @Order(7)
    void shouldChangeUserAddRealmRole() {
        doImport("07_update_realm__change_user_add_realm_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        List<String> userRealmLevelRoles = keycloakRepository.getUserRealmLevelRoles(
                REALM_NAME,
                "myotheruser"
        );

        assertThat(userRealmLevelRoles, hasItem("my_realm_role"));
    }

    @Test
    @Order(8)
    void shouldChangeUserAddClientRole() {
        doImport("08_update_realm__change_user_add_client_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        List<String> userClientLevelRoles = keycloakRepository.getUserClientLevelRoles(
                REALM_NAME,
                "myuser",
                "moped-client"
        );

        assertThat(userClientLevelRoles, contains("my_client_role"));
    }

    @Test
    @Order(9)
    void shouldChangeUserRemoveRealmRole() {
        doImport("09_update_realm__change_user_remove_realm_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        List<String> userRealmLevelRoles = keycloakRepository.getUserRealmLevelRoles(
                REALM_NAME,
                "myuser"
        );

        assertThat(userRealmLevelRoles, not(hasItem("my_realm_role")));
    }

    @Test
    @Order(10)
    void shouldChangeUserRemoveClientRole() {
        doImport("10_update_realm__change_user_remove_client_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        List<String> userClientLevelRoles = keycloakRepository.getUserClientLevelRoles(
                REALM_NAME,
                "myotheruser",
                "moped-client"
        );

        assertThat(userClientLevelRoles, not(hasItem("my_client_role")));
    }

    @Test
    @Order(11)
    void shouldAddRealmRoleWithRealmComposite() {
        doImport("11_update_realm__add_realm_role_with_realm_composite.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                REALM_NAME,
                "my_composite_realm_role"
        );

        assertThat(realmRole.getName(), is("my_composite_realm_role"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(false));
        assertThat(realmRole.getDescription(), is("My added composite realm role"));

        RoleRepresentation.Composites composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getRealm(), contains("my_realm_role"));
        assertThat(composites.getClient(), is(nullValue()));
    }

    @Test
    @Order(12)
    void shouldAddRealmRoleWithClientComposite() {
        doImport("12_update_realm__add_realm_role_with_client_composite.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                REALM_NAME,
                "my_composite_client_role"
        );

        assertThat(realmRole.getName(), is("my_composite_client_role"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(false));
        assertThat(realmRole.getDescription(), is("My added composite client role"));

        RoleRepresentation.Composites composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getRealm(), is(nullValue()));

        assertThat(composites.getClient(), aMapWithSize(1));
        assertThat(composites.getClient(), hasEntry(is("moped-client"), containsInAnyOrder("my_client_role")));
    }

    @Test
    @Order(13)
    void shouldAddRealmCompositeToRealmRole() {
        doImport("13_update_realm__add_realm_composite_to_realm_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                REALM_NAME,
                "my_composite_realm_role"
        );

        assertThat(realmRole.getName(), is("my_composite_realm_role"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(false));
        assertThat(realmRole.getDescription(), is("My added composite realm role"));

        RoleRepresentation.Composites composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getRealm(), containsInAnyOrder("my_realm_role", "my_other_realm_role"));
        assertThat(composites.getClient(), is(nullValue()));
    }

    @Test
    @Order(14)
    void shouldAddClientCompositeToRealmRole() {
        doImport("14_update_realm__add_client_composite_to_realm_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                REALM_NAME,
                "my_composite_client_role"
        );

        assertThat(realmRole.getName(), is("my_composite_client_role"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(false));
        assertThat(realmRole.getDescription(), is("My added composite client role"));

        RoleRepresentation.Composites composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getRealm(), is(nullValue()));

        assertThat(composites.getClient(), aMapWithSize(1));
        assertThat(composites.getClient(), hasEntry(is("moped-client"), containsInAnyOrder("my_client_role", "my_other_client_role")));
    }

    @Test
    @Order(15)
    void shouldAddCompositeClientToRealmRole() {
        doImport("15_update_realm__add_composite_client_to_realm_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                REALM_NAME,
                "my_composite_client_role"
        );

        assertThat(realmRole.getName(), is("my_composite_client_role"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(false));
        assertThat(realmRole.getDescription(), is("My added composite client role"));

        RoleRepresentation.Composites composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getRealm(), is(nullValue()));

        assertThat(composites.getClient(), aMapWithSize(2));
        assertThat(composites.getClient(), hasEntry(is("moped-client"), containsInAnyOrder("my_client_role", "my_other_client_role")));
        assertThat(composites.getClient(), hasEntry(is("second-moped-client"), containsInAnyOrder("my_other_second_client_role", "my_second_client_role")));
    }

    @Test
    @Order(16)
    void shouldAddClientRoleWithRealmRoleComposite() {
        doImport("16_update_realm__add_client_role_with_realm_role_composite.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getClientRole(
                REALM_NAME,
                "moped-client",
                "my_composite_moped_client_role"
        );

        assertThat(realmRole.getName(), is("my_composite_moped_client_role"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(true));
        assertThat(realmRole.getDescription(), is("My composite moped-client role"));

        RoleRepresentation.Composites composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getRealm(), contains("my_realm_role"));
        assertThat(composites.getClient(), is(nullValue()));
    }

    @Test
    @Order(17)
    void shouldAddClientRoleWithClientRoleComposite() {
        doImport("17_update_realm__add_client_role_with_client_role_composite.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getClientRole(
                REALM_NAME,
                "moped-client",
                "my_other_composite_moped_client_role"
        );

        assertThat(realmRole.getName(), is("my_other_composite_moped_client_role"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(true));
        assertThat(realmRole.getDescription(), is("My other composite moped-client role"));

        RoleRepresentation.Composites composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getRealm(), is(nullValue()));

        assertThat(composites.getClient(), aMapWithSize(1));
        assertThat(composites.getClient(), hasEntry(is("moped-client"), containsInAnyOrder("my_client_role")));
    }

    @Test
    @Order(18)
    void shouldAddRealmRoleCompositeToClientRole() {
        doImport("18_update_realm__add_realm_role_composite to_client_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getClientRole(
                REALM_NAME,
                "moped-client",
                "my_composite_moped_client_role"
        );

        assertThat(realmRole.getName(), is("my_composite_moped_client_role"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(true));
        assertThat(realmRole.getDescription(), is("My composite moped-client role"));

        RoleRepresentation.Composites composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getRealm(), containsInAnyOrder("my_realm_role", "my_other_realm_role"));
        assertThat(composites.getClient(), is(nullValue()));
    }

    @Test
    @Order(19)
    void shouldAddClientRoleCompositeToClientRole() {
        doImport("19_update_realm__add_client_role_composite to_client_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getClientRole(
                REALM_NAME,
                "moped-client",
                "my_other_composite_moped_client_role"
        );

        assertThat(realmRole.getName(), is("my_other_composite_moped_client_role"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(true));
        assertThat(realmRole.getDescription(), is("My other composite moped-client role"));

        RoleRepresentation.Composites composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getRealm(), is(nullValue()));

        assertThat(composites.getClient(), aMapWithSize(1));
        assertThat(composites.getClient(), hasEntry(is("moped-client"), containsInAnyOrder("my_client_role", "my_other_client_role")));
    }

    @Test
    @Order(20)
    void shouldAddClientRoleCompositesToClientRole() {
        doImport("20_update_realm__add_client_role_composites_to_client_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getClientRole(
                REALM_NAME,
                "moped-client",
                "my_other_composite_moped_client_role"
        );

        assertThat(realmRole.getName(), is("my_other_composite_moped_client_role"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(true));
        assertThat(realmRole.getDescription(), is("My other composite moped-client role"));

        RoleRepresentation.Composites composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getRealm(), is(nullValue()));

        assertThat(composites.getClient(), aMapWithSize(2));
        assertThat(composites.getClient(), hasEntry(is("moped-client"), containsInAnyOrder("my_client_role", "my_other_client_role")));
        assertThat(composites.getClient(), hasEntry(is("second-moped-client"), containsInAnyOrder("my_other_second_client_role", "my_second_client_role")));
    }

    @Test
    @Order(21)
    void shouldRemoveRealmCompositeFromRealmRole() {
        doImport("21_update_realm__remove_realm_role_composite_from_realm_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                REALM_NAME,
                "my_composite_realm_role"
        );

        assertThat(realmRole.getName(), is("my_composite_realm_role"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(false));
        assertThat(realmRole.getDescription(), is("My added composite realm role"));

        RoleRepresentation.Composites composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getRealm(), contains("my_other_realm_role"));
        assertThat(composites.getClient(), is(nullValue()));
    }

    @Test
    @Order(22)
    void shouldRemoveCompositeClientFromRealmRole() {
        doImport("22_update_realm__remove_client_role_composite_from_realm_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                REALM_NAME,
                "my_composite_client_role"
        );

        assertThat(realmRole.getName(), is("my_composite_client_role"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(false));
        assertThat(realmRole.getDescription(), is("My added composite client role"));

        RoleRepresentation.Composites composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getRealm(), is(nullValue()));

        assertThat(composites.getClient(), aMapWithSize(2));
        assertThat(composites.getClient(), hasEntry(is("moped-client"), containsInAnyOrder("my_other_client_role")));
        assertThat(composites.getClient(), hasEntry(is("second-moped-client"), containsInAnyOrder("my_other_second_client_role", "my_second_client_role")));
    }

    @Test
    @Order(23)
    void shouldRemoveClientCompositesFromRealmRole() {
        doImport("23_update_realm__remove_client_role_composites_from_realm_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                REALM_NAME,
                "my_composite_client_role"
        );

        assertThat(realmRole.getName(), is("my_composite_client_role"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(false));
        assertThat(realmRole.getDescription(), is("My added composite client role"));

        RoleRepresentation.Composites composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getRealm(), is(nullValue()));

        assertThat(composites.getClient(), aMapWithSize(1));
        assertThat(composites.getClient(), hasEntry(is("moped-client"), containsInAnyOrder("my_other_client_role")));
    }

    @Test
    @Order(24)
    void shouldRemoveRealmCompositeFromClientRole() {
        doImport("24_update_realm__remove_realm_role_composite_from_client_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getClientRole(
                REALM_NAME,
                "moped-client",
                "my_composite_moped_client_role"
        );

        assertThat(realmRole.getName(), is("my_composite_moped_client_role"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(true));
        assertThat(realmRole.getDescription(), is("My composite moped-client role"));

        RoleRepresentation.Composites composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getRealm(), contains("my_other_realm_role"));
        assertThat(composites.getClient(), is(nullValue()));
    }

    @Test
    @Order(25)
    void shouldRemoveClientCompositeFromClientRole() {
        doImport("25_update_realm__remove_client_role_composite_from_client_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getClientRole(
                REALM_NAME,
                "moped-client",
                "my_other_composite_moped_client_role"
        );

        assertThat(realmRole.getName(), is("my_other_composite_moped_client_role"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(true));
        assertThat(realmRole.getDescription(), is("My other composite moped-client role"));

        RoleRepresentation.Composites composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getRealm(), is(nullValue()));

        assertThat(composites.getClient(), aMapWithSize(2));
        assertThat(composites.getClient(), hasEntry(is("moped-client"), containsInAnyOrder("my_client_role", "my_other_client_role")));
        assertThat(composites.getClient(), hasEntry(is("second-moped-client"), containsInAnyOrder("my_other_second_client_role")));
    }

    @Test
    @Order(26)
    void shouldRemoveClientCompositesFromClientRole() {
        doImport("26_update_realm__remove_client_role_composites_from_client_role.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getClientRole(
                REALM_NAME,
                "moped-client",
                "my_other_composite_moped_client_role"
        );

        assertThat(realmRole.getName(), is("my_other_composite_moped_client_role"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(true));
        assertThat(realmRole.getDescription(), is("My other composite moped-client role"));

        RoleRepresentation.Composites composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getRealm(), is(nullValue()));

        assertThat(composites.getClient(), aMapWithSize(1));
        assertThat(composites.getClient(), hasEntry(is("second-moped-client"), containsInAnyOrder("my_other_second_client_role")));
    }
}
