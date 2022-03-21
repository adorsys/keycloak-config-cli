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
import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.util.VersionUtil;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("java:S5961")
class ImportRolesIT extends AbstractImportIT {
    private static final String REALM_NAME = "realmWithRoles";

    ImportRolesIT() {
        this.resourcePath = "import-files/roles";
    }

    @Test
    @Order(0)
    void shouldCreateRealmWithRoles() throws IOException {
        doImport("00_create_realm_with_roles.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                realm, "my_realm_role"
        );

        assertThat(realmRole.getName(), is("my_realm_role"));
        assertThat(realmRole.isComposite(), is(false));
        assertThat(realmRole.getClientRole(), is(false));
        assertThat(realmRole.getDescription(), is("My realm role"));

        assertThat(realmRole.getAttributes(), aMapWithSize(1));
        assertThat(realmRole.getAttributes(), hasEntry(is("my added attribute"), containsInAnyOrder("my added attribute value", "my added attribute second value")));

        RoleRepresentation clientRole = keycloakRepository.getClientRole(
                realm, "moped-client", "my_client_role"
        );

        assertThat(clientRole.getName(), is("my_client_role"));
        assertThat(clientRole.isComposite(), is(false));
        assertThat(clientRole.getClientRole(), is(true));
        assertThat(clientRole.getDescription(), is("My moped-client role"));
        assertThat(clientRole.getAttributes(), aMapWithSize(1));
        assertThat(clientRole.getAttributes(), hasEntry(is("my added client attribute"), containsInAnyOrder("my added client attribute value", "my added client attribute second value")));
    }

    @Test
    @Order(1)
    void shouldAddRealmRole() throws IOException {
        doImport("01_update_realm__add_realm_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole;
        realmRole = keycloakRepository.getRealmRole(
                realm, "my_realm_role"
        );

        assertThat(realmRole.getName(), is("my_realm_role"));
        assertThat(realmRole.isComposite(), is(false));
        assertThat(realmRole.getClientRole(), is(false));
        assertThat(realmRole.getDescription(), is("My realm role"));
        assertThat(realmRole.getAttributes(), aMapWithSize(1));
        assertThat(realmRole.getAttributes(), hasEntry(is("my second added attribute"), containsInAnyOrder("my second added attribute value", "my second added attribute second value")));

        realmRole = keycloakRepository.getRealmRole(
                realm, "my_other_realm_role"
        );

        assertThat(realmRole.getName(), is("my_other_realm_role"));
        assertThat(realmRole.isComposite(), is(false));
        assertThat(realmRole.getClientRole(), is(false));
        assertThat(realmRole.getDescription(), is("My other realm role"));
        assertThat(realmRole.getAttributes(), aMapWithSize(1));
        assertThat(realmRole.getAttributes(), hasEntry(is("my added attribute"), containsInAnyOrder("my added attribute value", "my added attribute second value")));

        RoleRepresentation clientRole = keycloakRepository.getClientRole(
                realm, "moped-client", "my_client_role"
        );

        assertThat(clientRole.getName(), is("my_client_role"));
        assertThat(clientRole.isComposite(), is(false));
        assertThat(clientRole.getClientRole(), is(true));
        assertThat(clientRole.getDescription(), is("My moped-client role"));
        assertThat(clientRole.getAttributes(), aMapWithSize(1));
        assertThat(clientRole.getAttributes(), hasEntry(is("my second added client attribute"), containsInAnyOrder("my second added client attribute value", "my second added client attribute second value")));
    }

    @Test
    @Order(2)
    void shouldAddClientRole() throws IOException {
        doImport("02_update_realm__add_client_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole;
        realmRole = keycloakRepository.getRealmRole(
                realm, "my_realm_role"
        );

        assertThat(realmRole.getName(), is("my_realm_role"));
        assertThat(realmRole.isComposite(), is(false));
        assertThat(realmRole.getClientRole(), is(false));
        assertThat(realmRole.getDescription(), is("My realm role"));
        assertThat(realmRole.getAttributes(), anEmptyMap());

        realmRole = keycloakRepository.getRealmRole(
                realm, "my_other_realm_role"
        );

        assertThat(realmRole.getName(), is("my_other_realm_role"));
        assertThat(realmRole.isComposite(), is(false));
        assertThat(realmRole.getClientRole(), is(false));
        assertThat(realmRole.getDescription(), is("My other realm role"));
        assertThat(realmRole.getAttributes(), anEmptyMap());

        RoleRepresentation clientRole = keycloakRepository.getClientRole(
                realm, "moped-client", "my_client_role"
        );

        assertThat(clientRole.getName(), is("my_client_role"));
        assertThat(clientRole.isComposite(), is(false));
        assertThat(clientRole.getClientRole(), is(true));
        assertThat(clientRole.getDescription(), is("My moped-client role"));
        assertThat(clientRole.getAttributes(), anEmptyMap());
    }

    @Test
    @Order(3)
    void shouldChangeRealmRole() throws IOException {
        doImport("03_update_realm__change_realm_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                realm,
                "my_other_realm_role"
        );

        assertThat(realmRole.getName(), is("my_other_realm_role"));
        assertThat(realmRole.isComposite(), is(false));
        assertThat(realmRole.getClientRole(), is(false));
        assertThat(realmRole.getDescription(), is("My changed other realm role"));
    }

    @Test
    @Order(4)
    void shouldChangeClientRole() throws IOException {
        doImport("04_update_realm__change_client_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getClientRole(
                realm,
                "moped-client", "my_other_client_role"
        );

        assertThat(realmRole.getName(), is("my_other_client_role"));
        assertThat(realmRole.isComposite(), is(false));
        assertThat(realmRole.getClientRole(), is(true));
        assertThat(realmRole.getDescription(), is("My changed other moped-client role"));
    }

    @Test
    @Order(5)
    void shouldAddUserWithRealmRole() throws IOException {
        doImport("05_update_realm__add_user_with_realm_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        List<String> userRealmLevelRoles = keycloakRepository.getUserRealmLevelRoles(
                REALM_NAME,
                "myuser"
        );

        assertThat(userRealmLevelRoles, hasItem("my_realm_role"));

        if (VersionUtil.ge(KEYCLOAK_VERSION, "13")) {
            assertThat(userRealmLevelRoles, hasItem("default-roles-" + REALM_NAME.toLowerCase()));
        }
    }

    @Test
    @Order(6)
    void shouldAddUserWithClientRole() throws IOException {
        doImport("06_update_realm__add_user_with_client_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        List<String> userClientLevelRoles = keycloakRepository.getUserClientLevelRoles(
                REALM_NAME,
                "myotheruser",
                "moped-client"
        );

        assertThat(userClientLevelRoles, hasItem("my_client_role"));
    }

    @Test
    @Order(7)
    void shouldChangeUserAddRealmRole() throws IOException {
        doImport("07_update_realm__change_user_add_realm_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        List<String> userRealmLevelRoles = keycloakRepository.getUserRealmLevelRoles(
                REALM_NAME,
                "myotheruser"
        );

        assertThat(userRealmLevelRoles, hasItem("my_realm_role"));
    }

    @Test
    @Order(8)
    void shouldChangeUserAddClientRole() throws IOException {
        doImport("08_update_realm__change_user_add_client_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        List<String> userClientLevelRoles = keycloakRepository.getUserClientLevelRoles(
                REALM_NAME,
                "myuser",
                "moped-client"
        );

        assertThat(userClientLevelRoles, contains("my_client_role"));
    }

    @Test
    @Order(9)
    void shouldChangeUserRemoveRealmRole() throws IOException {
        doImport("09_update_realm__change_user_remove_realm_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        List<String> userRealmLevelRoles = keycloakRepository.getUserRealmLevelRoles(
                REALM_NAME,
                "myuser"
        );

        assertThat(userRealmLevelRoles, not(hasItem("my_realm_role")));
    }

    @Test
    @Order(10)
    void shouldChangeUserRemoveClientRole() throws IOException {
        doImport("10_update_realm__change_user_remove_client_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        List<String> userClientLevelRoles = keycloakRepository.getUserClientLevelRoles(
                REALM_NAME,
                "myotheruser",
                "moped-client"
        );

        assertThat(userClientLevelRoles, not(hasItem("my_client_role")));
    }

    @Test
    @Order(11)
    void shouldAddRealmRoleWithRealmComposite() throws IOException {
        doImport("11_update_realm__add_realm_role_with_realm_composite.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                realm,
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
    void shouldAddRealmRoleWithClientComposite() throws IOException {
        doImport("12_update_realm__add_realm_role_with_client_composite.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                realm,
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
    void shouldAddRealmCompositeToRealmRole() throws IOException {
        doImport("13_update_realm__add_realm_composite_to_realm_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                realm,
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
    void shouldAddClientCompositeToRealmRole() throws IOException {
        doImport("14_update_realm__add_client_composite_to_realm_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                realm,
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
    void shouldAddCompositeClientToRealmRole() throws IOException {
        doImport("15.1_update_realm__add_composite_client_to_realm_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                realm,
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

        RealmImport foundImport = getFirstImport("15.2_update_realm__add_non_existing_composite_client_to_realm_role.json");
        KeycloakRepositoryException thrown = assertThrows(KeycloakRepositoryException.class, () -> realmImportService.doImport(foundImport));
        assertThat(thrown.getMessage(), is("Error adding composite roles to realm role 'my_composite_client_role': Cannot find client role 'non_exists' within realm 'realmWithRoles'"));
    }

    @Test
    @Order(16)
    void shouldAddClientRoleWithRealmRoleComposite() throws IOException {
        doImport("16_update_realm__add_client_role_with_realm_role_composite.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getClientRole(
                realm,
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
    void shouldAddClientRoleWithClientRoleComposite() throws IOException {
        doImport("17_update_realm__add_client_role_with_client_role_composite.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getClientRole(
                realm,
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
    void shouldAddRealmRoleCompositeToClientRole() throws IOException {
        doImport("18_update_realm__add_realm_role_composite_to_client_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getClientRole(
                realm,
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
    void shouldAddClientRoleCompositeToClientRole() throws IOException {
        doImport("19.1_update_realm__add_client_role_composite_to_client_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getClientRole(
                realm,
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


        RealmImport foundImport = getFirstImport("19.2_update_realm__add_non_existing_client_role_composite_to_client_role.json");
        KeycloakRepositoryException thrown = assertThrows(KeycloakRepositoryException.class, () -> realmImportService.doImport(foundImport));
        assertThat(thrown.getMessage(), is("Error adding composite roles to client role 'my_other_composite_moped_client_role': Cannot find client role 'non_exists' within realm 'realmWithRoles'"));
    }

    @Test
    @Order(20)
    void shouldAddClientRoleCompositesToClientRole() throws IOException {
        doImport("20_update_realm__add_client_role_composites_to_client_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getClientRole(
                realm,
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
    void shouldRemoveRealmCompositeFromRealmRole() throws IOException {
        doImport("21_update_realm__remove_realm_role_composite_from_realm_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                realm,
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
    void shouldRemoveCompositeClientFromRealmRole() throws IOException {
        doImport("22_update_realm__remove_client_role_composite_from_realm_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                realm,
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
    void shouldRemoveClientCompositesFromRealmRole() throws IOException {
        doImport("23_update_realm__remove_client_role_composites_from_realm_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                realm,
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
    void shouldRemoveRealmCompositeFromClientRole() throws IOException {
        doImport("24_update_realm__remove_realm_role_composite_from_client_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getClientRole(
                realm,
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
    void shouldRemoveClientCompositeFromClientRole() throws IOException {
        doImport("25_update_realm__remove_client_role_composite_from_client_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getClientRole(
                realm,
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
    void shouldRemoveClientCompositesFromClientRole() throws IOException {
        doImport("26_update_realm__remove_client_role_composites_from_client_role.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getClientRole(
                realm,
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

    @Test
    @Order(27)
    void shouldCreateRolesWithAttributes() throws IOException {
        doImport("27_update_realm__create_role_with_attributes.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation realmRole = keycloakRepository.getRealmRole(
                realm,
                "my_composite_attribute_client_role"
        );

        assertThat(realmRole.getName(), is("my_composite_attribute_client_role"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(false));
        assertThat(realmRole.getDescription(), is("My composite client role with attributes"));

        assertThat(realmRole.getAttributes(), aMapWithSize(2));
        assertThat(realmRole.getAttributes(), hasEntry(is("my added attribute"), containsInAnyOrder("my added attribute value", "my added attribute second value")));
        assertThat(realmRole.getAttributes(), hasEntry(is("my second added attribute"), containsInAnyOrder("my second added attribute value", "my second added attribute second value")));

        RoleRepresentation.Composites composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getRealm(), is(nullValue()));

        assertThat(composites.getClient(), aMapWithSize(1));
        assertThat(composites.getClient(), hasEntry(is("moped-client"), containsInAnyOrder("my_other_client_role")));

        RoleRepresentation clientRole = keycloakRepository.getClientRole(
                realm,
                "moped-client",
                "my_other_composite_attribute_moped_client_role"
        );

        assertThat(clientRole.getName(), is("my_other_composite_attribute_moped_client_role"));
        assertThat(clientRole.isComposite(), is(true));
        assertThat(clientRole.getClientRole(), is(true));
        assertThat(clientRole.getDescription(), is("My other composite moped-client role with attributes"));

        assertThat(clientRole.getAttributes(), aMapWithSize(2));
        assertThat(clientRole.getAttributes(), hasEntry(is("my added attribute"), containsInAnyOrder("my added attribute value", "my added attribute second value")));
        assertThat(clientRole.getAttributes(), hasEntry(is("my second added attribute"), containsInAnyOrder("my second added attribute value", "my second added attribute second value")));

        RoleRepresentation.Composites clientRoleComposites = clientRole.getComposites();
        assertThat(clientRoleComposites, notNullValue());
        assertThat(clientRoleComposites.getRealm(), is(nullValue()));

        assertThat(clientRoleComposites.getClient(), aMapWithSize(1));
        assertThat(clientRoleComposites.getClient(), hasEntry(is("second-moped-client"), containsInAnyOrder("my_other_second_client_role")));
    }

    @Test
    @Order(28)
    void shouldThrowUpdateRealmAddReferNonExistClientRole() throws IOException {
        RealmImport foundImport = getFirstImport("28_try-to_update_realm__refer-non-exist-role.json");

        KeycloakRepositoryException thrown = assertThrows(KeycloakRepositoryException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), is("Cannot find client role 'my_non_exist_client_role' for client 'moped-client' within realm 'realmWithRoles'"));
    }

    @Test
    @Order(29)
    void shouldThrowUpdateRealmAddClientRoleWithoutClient() throws IOException {
        RealmImport foundImport = getFirstImport("29_try-to_update_realm__add-client-role-without-client.json");

        ImportProcessingException thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), is("Can't create role 'my_second_client_role' for non existing client 'non-exists-client' in realm 'realmWithRoles'!"));
    }

    @Test
    @Order(70)
    @SuppressWarnings("deprecation")
    void shouldNotThrowImportingClientRoleThatAlreadyExists() throws IOException {
        RealmImport foundImport = getFirstImport("70_import_realm_with_duplicated_client_role.json");

        assertThat(
                foundImport
                        .getClients()
                        .get(0)
                        .getDefaultRoles(),
                hasItemInArray("USER")
        );

        assertThat(
                foundImport.getRoles()
                        .getClient()
                        .get("my-app")
                        .stream().map(RoleRepresentation::getName)
                        .collect(Collectors.toList()),
                hasItem("USER")
        );

        // client role 'USER' has been already created during client import
        // but client roles import should not fail on importing role with the same name
        assertDoesNotThrow(() -> realmImportService.doImport(foundImport));
    }

    @Test
    @Order(71)
    void shouldImportRealmWithNestedComposites() throws IOException {
        doImport("71.1_import_realm_with_nested_composites.json");

        String REALM_NAME = "realmWithRoles71";

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));

        RoleRepresentation realmRole;
        RoleRepresentation.Composites composites;

        realmRole = keycloakRepository.getRealmRole(
                realm, "subscription_user"
        );

        assertThat(realmRole.getName(), is("subscription_user"));
        assertThat(realmRole.getDescription(), is("subscription"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(false));

        composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getClient(), hasEntry(is("fe"), containsInAnyOrder("subscription_user")));
        assertThat(composites.getClient(), hasEntry(is("be"), containsInAnyOrder("subscription_user")));
        assertThat(composites.getClient(), aMapWithSize(2));
        assertThat(composites.getRealm(), is(nullValue()));


        realmRole = keycloakRepository.getRealmRole(
                realm, "procurement_user"
        );

        assertThat(realmRole.getName(), is("procurement_user"));
        assertThat(realmRole.getDescription(), is("procurement"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(false));

        composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getClient(), hasEntry(is("fe"), containsInAnyOrder("procurement_user")));
        assertThat(composites.getClient(), hasEntry(is("be"), containsInAnyOrder("procurement_user")));
        assertThat(composites.getClient(), aMapWithSize(2));
        assertThat(composites.getRealm(), is(nullValue()));


        realmRole = keycloakRepository.getRealmRole(
                realm, "vendor_user"
        );

        assertThat(realmRole.getName(), is("vendor_user"));
        assertThat(realmRole.getDescription(), is("vendor"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(false));

        composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getClient(), hasEntry(is("fe"), containsInAnyOrder("vendor_user")));
        assertThat(composites.getClient(), hasEntry(is("be"), containsInAnyOrder("vendor_user")));
        assertThat(composites.getClient(), aMapWithSize(2));
        assertThat(composites.getRealm(), is(nullValue()));


        realmRole = keycloakRepository.getRealmRole(
                realm, "default-roles-realmWithRoles71"
        );

        assertThat(realmRole.getName(), is("default-roles-realmWithRoles71"));
        assertThat(realmRole.getDescription(), is("${role_default-roles}"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(false));

        composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getClient(), hasEntry(is("account"), containsInAnyOrder("view-profile", "manage-account")));
        assertThat(composites.getClient(), aMapWithSize(1));
        assertThat(composites.getRealm(), containsInAnyOrder("offline_access", "subscription_user", "uma_authorization"));


        List<String> userRealmLevelRoles = keycloakRepository.getUserRealmLevelRoles(
                REALM_NAME, "user@test.com"
        );

        assertThat(userRealmLevelRoles, hasItem("subscription_user"));

        doImport("71.2_import_realm_with_nested_composites.json");

        realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        realmRole = keycloakRepository.getRealmRole(
                realm, "subscription_user"
        );

        assertThat(realmRole.getName(), is("subscription_user"));
        assertThat(realmRole.getDescription(), is("subscription"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(false));

        composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getClient(), hasEntry(is("fe"), containsInAnyOrder("procurement_user")));
        assertThat(composites.getClient(), hasEntry(is("fe2"), containsInAnyOrder("subscription_user")));
        assertThat(composites.getClient(), aMapWithSize(2));
        assertThat(composites.getRealm(), is(nullValue()));


        realmRole = keycloakRepository.getRealmRole(
                realm, "procurement_user"
        );

        assertThat(realmRole.getName(), is("procurement_user"));
        assertThat(realmRole.getDescription(), is("procurement"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(false));

        composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getClient(), hasEntry(is("be2"), containsInAnyOrder("subscription_user")));
        assertThat(composites.getClient(), hasEntry(is("be3"), containsInAnyOrder("procurement_user")));
        assertThat(composites.getClient(), aMapWithSize(2));
        assertThat(composites.getRealm(), is(nullValue()));


        realmRole = keycloakRepository.getRealmRole(
                realm, "vendor_user"
        );

        assertThat(realmRole.getName(), is("vendor_user"));
        assertThat(realmRole.getDescription(), is("vendor"));
        assertThat(realmRole.isComposite(), is(true));
        assertThat(realmRole.getClientRole(), is(false));

        composites = realmRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getClient(), hasEntry(is("fe"), containsInAnyOrder("vendor_user")));
        assertThat(composites.getClient(), hasEntry(is("be2"), containsInAnyOrder("vendor_user")));
        assertThat(composites.getClient(), aMapWithSize(2));
        assertThat(composites.getRealm(), is(nullValue()));


        doImport("71.3_import_realm_with_nested_composites.json");

        realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        RoleRepresentation ClientRole = keycloakRepository.getClientRole(
                realm, "fe", "composite_role_user"
        );

        assertThat(ClientRole.getName(), is("composite_role_user"));
        assertThat(ClientRole.getDescription(), is("composite role created BEFORE dependent roles have been created."));
        assertThat(ClientRole.isComposite(), is(true));
        assertThat(ClientRole.getClientRole(), is(true));

        composites = ClientRole.getComposites();
        assertThat(composites, notNullValue());
        assertThat(composites.getClient(), aMapWithSize(2));
        assertThat(composites.getClient(), hasEntry(is("be"), containsInAnyOrder("subscription_user", "procurement_user", "vendor_user")));
        assertThat(composites.getClient(), hasEntry(is("fe"), containsInAnyOrder("subscription_user", "procurement_user", "vendor_user")));
        assertThat(composites.getRealm(), is(nullValue()));
    }

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Nested
    @Order(60)
    @TestPropertySource(properties = {
            "import.behaviors.remove-default-role-from-user=true"
    })
    class RemoveDefaultRoleTest {
        @Autowired
        public RealmImportService realmImportService;

        @Test
        @Order(0)
        void shouldCreateUserAndRemoveDefaultRole() throws IOException {
            doImport("60_update_realm__add_user_with_realm_role.json", realmImportService);

            RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

            assertThat(realm.getRealm(), is(REALM_NAME));
            assertThat(realm.isEnabled(), is(true));

            List<String> userRealmLevelRoles = keycloakRepository.getUserRealmLevelRoles(
                    REALM_NAME,
                    "myuser6"
            );

            assertThat(userRealmLevelRoles, hasItem("my_realm_role"));
            assertThat(userRealmLevelRoles, not(hasItem("default-roles-" + REALM_NAME.toLowerCase())));
        }
    }
}
