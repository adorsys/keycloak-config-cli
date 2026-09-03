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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.adorsys.keycloak.config.AbstractImportIT;
import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.model.RealmImport;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.List;

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
        assertThat(userRealmLevelRoles, hasItem("default-roles-" + REALM_NAME.toLowerCase()));
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
                        .toList(),
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

    @Nested
    @Order(65)
    @TestPropertySource(properties = {
            "import.remote-state.enabled=false"
    })
    class ImportRemoteStateDisabled {

        @Test
        @Order(0)
        void shouldNotDeleteUnmentionedDefaultRoles() throws IOException {
            doImport("65_import_realm_without_mentioned_default_roles.json");

            String REALM_NAME = "realmWithoutMentionedDefaultRoles";

            RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

            assertThat(realm.getRealm(), is(REALM_NAME));

            RoleRepresentation realmRole = keycloakRepository.getRealmRole(realm, "default-roles-realmwithoutmentioneddefaultroles");

            assertThat(realmRole.getName(), is("default-roles-realmwithoutmentioneddefaultroles"));
            assertThat(realmRole.isComposite(), is(true));
            assertThat(realmRole.getClientRole(), is(false));
            assertThat(realmRole.getDescription(), is("${role_default-roles}"));
        }

        @Test
        @Order(1)
        void shouldContainFakeDefaultRoles() throws IOException {
            doImport("66_import_realm_with_fake_default_roles.json");

            String REALM_NAME = "realmWithFakeDefaultRoles";

            RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

            assertThat(realm.getRealm(), is(REALM_NAME));

            RoleRepresentation realmRole = keycloakRepository.getRealmRole(realm, "default-roles-fake");

            assertThat(realmRole.getName(), is("default-roles-fake"));
            assertThat(realmRole.isComposite(), is(false));
            assertThat(realmRole.getClientRole(), is(false));
            assertThat(realmRole.getDescription(), is("no default roles description"));
        }

        @Test
        @Order(2)
        void shouldHaveDeletedFakeDefaultRoles() throws IOException {
            doImport("67_import_realm_without_fake_default_roles.json");

            String REALM_NAME = "realmWithFakeDefaultRoles";

            RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

            assertThat(realm.getRealm(), is(REALM_NAME));

            RoleRepresentation realmRole = keycloakRepository.getRealmRole(realm, "default-roles-fake");

            assertThat(realmRole, nullValue());
        }
    }

    // Requirement C.9: with ZERO import.protected-roles configuration, the protected set is
    // realm roles "admin" and "create-realm", plus all roles of client "realm-management".
    // Requirement A.1/A.2: a protected role that differs is not updated, and a protected role
    // missing from the import is not deleted even under FULL managed mode.
    @Nested
    @Order(80)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestPropertySource(properties = {
            "import.remote-state.enabled=false"
    })
    class DefaultProtectionTest {

        @Autowired
        public RealmImportService realmImportService;

        private static final String REALM = "realmWithDefaultProtectedRoles";

        @Test
        @Order(0)
        void shouldCreateRealmWithDefaultProtectedRoles() throws IOException {
            doImport("80.1_create_realm_with_default_protected_roles.json", realmImportService);

            RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM).partialExport(true, true);

            assertThat(keycloakRepository.getRealmRole(realm, "admin").getDescription(), is("Original admin description"));
            assertThat(keycloakRepository.getRealmRole(realm, "create-realm").getDescription(), is("Original create-realm description"));
            assertThat(keycloakRepository.getRealmRole(realm, "custom_role").getDescription(), is("Original custom description"));
        }

        @Test
        @Order(1)
        void shouldNotUpdateDefaultProtectedRolesWhenTheyDiffer() throws IOException {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger roleImportLogger = loggerContext.getLogger("de.adorsys.keycloak.config.service.RoleImportService");
            roleImportLogger.setLevel(Level.DEBUG);
            ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
            listAppender.start();
            roleImportLogger.addAppender(listAppender);

            try {
                doImport("80.2_update_realm__change_default_protected_roles.json", realmImportService);
            } finally {
                roleImportLogger.detachAppender(listAppender);
            }

            RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM).partialExport(true, true);

            // protected realm roles: unchanged
            assertThat(keycloakRepository.getRealmRole(realm, "admin").getDescription(), is("Original admin description"));
            assertThat(keycloakRepository.getRealmRole(realm, "create-realm").getDescription(), is("Original create-realm description"));

            // protected client role of realm-management: unchanged
            RoleRepresentation manageUsers = keycloakRepository.getClientRole(realm, "realm-management", "manage-users");
            assertThat(manageUsers.getDescription(), is(not("Changed manage-users description")));

            // unprotected role: updated normally
            assertThat(keycloakRepository.getRealmRole(realm, "custom_role").getDescription(), is("Changed custom description"));

            boolean loggedSkip = listAppender.list.stream()
                    .anyMatch(event -> event.getLevel() == Level.INFO && event.getFormattedMessage().contains("admin"));
            assertThat("expected an info-level log entry naming skipped protected role 'admin'", loggedSkip, is(true));
        }

        @Test
        @Order(2)
        void shouldNotDeleteDefaultProtectedRolesMissingFromImportUnderFullManagedMode() throws IOException {
            doImport("80.3_update_realm__omit_default_protected_roles.json", realmImportService);

            RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM).partialExport(true, true);

            assertThat(keycloakRepository.getRealmRole(realm, "admin"), notNullValue());
            assertThat(keycloakRepository.getRealmRole(realm, "create-realm"), notNullValue());
            assertThat(keycloakRepository.getClientRole(realm, "realm-management", "manage-users"), notNullValue());
            assertThat(keycloakRepository.getRealmRole(realm, "custom_role").getDescription(), is("Changed custom description"));
        }
    }

    // Requirement B.6: realm roles are identified by name alone, client roles by (client, name) pair.
    // Requirement A.3: creation of a not-yet-existing protected role name is unaffected.
    @Nested
    @Order(81)
    @TestPropertySource(properties = {
            "import.remote-state.enabled=false"
    })
    class NamespaceSeparationTest {

        @Autowired
        public RealmImportService realmImportService;

        private static final String REALM = "realmWithNamespaceProtection";

        @Test
        @Order(0)
        void shouldCreateRealmWithRealmRoleNamedLikeProtectedClientRole() throws IOException {
            doImport("81.1_create_realm_with_namespace_collision.json", realmImportService);

            RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM).partialExport(true, true);

            assertThat(keycloakRepository.getRealmRole(realm, "manage-users").getDescription(),
                    is("Original manage-users realm role description"));
        }

        @Test
        @Order(1)
        void shouldUpdateRealmRoleNamedLikeProtectedClientRoleAndCreateNewlyDefinedProtectedRole() throws IOException {
            doImport("81.2_update_realm__change_realm_role_named_like_protected_client_role.json", realmImportService);

            RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM).partialExport(true, true);

            // "manage-users" as a REALM role must update normally - it must never be confused
            // with the protected CLIENT role of the same name belonging to realm-management.
            assertThat(keycloakRepository.getRealmRole(realm, "manage-users").getDescription(),
                    is("Changed manage-users realm role description"));

            // "admin" did not exist yet as a realm role; creation of a protected role name is unaffected.
            assertThat(keycloakRepository.getRealmRole(realm, "admin").getDescription(), is("Newly created admin realm role"));
        }
    }

    // Requirement C.10: additive mode - effective protected set is the UNION of built-in
    // defaults and configured roles.
    @Nested
    @Order(82)
    @TestPropertySource(properties = {
            "import.remote-state.enabled=false",
            "import.protected-roles.mode=add",
            "import.protected-roles.realm-roles=custom_protected_role"
    })
    class AdditiveProtectionTest {

        @Autowired
        public RealmImportService realmImportService;

        private static final String REALM = "realmWithAdditiveProtection";

        @Test
        @Order(0)
        void shouldCreateRealmWithAdditiveProtection() throws IOException {
            doImport("82.1_create_realm_with_additive_protection.json", realmImportService);

            RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM).partialExport(true, true);

            assertThat(keycloakRepository.getRealmRole(realm, "custom_protected_role").getDescription(),
                    is("Original custom protected description"));
            assertThat(keycloakRepository.getRealmRole(realm, "admin").getDescription(), is("Original admin description"));
        }

        @Test
        @Order(1)
        void shouldNotUpdateEitherConfiguredOrBuiltInProtectedRoleWhenTheyDiffer() throws IOException {
            doImport("82.2_update_realm__change_additive_protected_roles.json", realmImportService);

            RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM).partialExport(true, true);

            // configured protected role: unchanged
            assertThat(keycloakRepository.getRealmRole(realm, "custom_protected_role").getDescription(),
                    is("Original custom protected description"));
            // built-in default still applies in additive mode: unchanged
            assertThat(keycloakRepository.getRealmRole(realm, "admin").getDescription(), is("Original admin description"));
        }
    }

    // Requirement C.11: replace mode with an explicit role list - EXACTLY the configured
    // roles are protected, built-in defaults do not apply.
    @Nested
    @Order(83)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @TestPropertySource(properties = {
            "import.remote-state.enabled=false",
            "import.protected-roles.mode=replace",
            "import.protected-roles.realm-roles=custom_only_protected_role"
    })
    class ReplaceProtectionTest {

        @Autowired
        public RealmImportService realmImportService;

        private static final String REALM = "realmWithReplaceProtection";

        @Test
        @Order(0)
        void shouldCreateRealmWithReplaceProtection() throws IOException {
            doImport("83.1_create_realm_with_replace_protection.json", realmImportService);

            RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM).partialExport(true, true);

            assertThat(keycloakRepository.getRealmRole(realm, "custom_only_protected_role").getDescription(),
                    is("Original custom only protected description"));
            assertThat(keycloakRepository.getRealmRole(realm, "admin").getDescription(), is("Original admin description"));
        }

        @Test
        @Order(1)
        void shouldOnlyProtectConfiguredRoleAndUpdateAdminNormally() throws IOException {
            doImport("83.2_update_realm__change_replace_protected_roles.json", realmImportService);

            RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM).partialExport(true, true);

            // configured protected role: unchanged
            assertThat(keycloakRepository.getRealmRole(realm, "custom_only_protected_role").getDescription(),
                    is("Original custom only protected description"));
            // built-in defaults do NOT apply in replace mode: "admin" updates normally
            assertThat(keycloakRepository.getRealmRole(realm, "admin").getDescription(), is("Changed admin description"));
        }
    }

    // Requirement C.12: replace mode with empty lists is a full opt-out - no role is protected,
    // the tool behaves exactly as before this feature existed.
    @Nested
    @Order(84)
    @TestPropertySource(properties = {
            "import.remote-state.enabled=false",
            "import.protected-roles.mode=replace"
    })
    class FullOptOutProtectionTest {

        @Autowired
        public RealmImportService realmImportService;

        private static final String REALM = "realmWithFullOptOutProtection";

        @Test
        @Order(0)
        void shouldCreateRealmWithFullOptOutProtection() throws IOException {
            doImport("84.1_create_realm_with_full_opt_out.json", realmImportService);

            RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM).partialExport(true, true);

            assertThat(keycloakRepository.getRealmRole(realm, "admin").getDescription(), is("Original admin description"));
        }

        @Test
        @Order(1)
        void shouldUpdateNormallyDefinedProtectedRolesWhenOptedOut() throws IOException {
            doImport("84.2_update_realm__change_roles_with_full_opt_out.json", realmImportService);

            RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM).partialExport(true, true);

            assertThat(keycloakRepository.getRealmRole(realm, "admin").getDescription(), is("Changed admin description"));
            assertThat(keycloakRepository.getClientRole(realm, "realm-management", "manage-users").getDescription(),
                    is("Changed manage-users description"));
        }
    }

    // Requirement A.4/A.5: composite membership of a protected role is frozen (both add and
    // remove suppressed), independently on the realm-owned and client-owned composite paths;
    // protection is NOT transitive - an unprotected role referencing a protected role in its
    // composites is synced normally.
    @Nested
    @Order(85)
    @TestPropertySource(properties = {
            "import.remote-state.enabled=false",
            "import.managed.role=no-delete",
            "import.protected-roles.mode=replace",
            "import.protected-roles.realm-roles=protected_realm_role",
            "import.protected-roles.client-roles.moped-client=protected_client_role"
    })
    class CompositeProtectionTest {

        @Autowired
        public RealmImportService realmImportService;

        private static final String REALM = "realmWithProtectedComposites";

        @Test
        @Order(0)
        void shouldFreezeProtectedRoleCompositesButSyncUnprotectedRoleComposites() throws IOException {
            doImport("85.1_create_realm_with_protected_composites.json", realmImportService);

            // seed an initial composite baseline directly through the admin client, bypassing
            // this tool's own (protection-aware) composite import entirely
            RoleRepresentation helperRealmRole = keycloakProvider.getInstance().realm(REALM).roles().get("helper_realm_role").toRepresentation();
            keycloakProvider.getInstance().realm(REALM).roles().get("protected_realm_role").addComposites(List.of(helperRealmRole));

            RoleRepresentation helperClientRole = keycloakProvider.getInstance().realm(REALM)
                    .clients().get(getClientId(REALM, "moped-client"))
                    .roles().get("helper_client_role").toRepresentation();
            keycloakProvider.getInstance().realm(REALM)
                    .clients().get(getClientId(REALM, "moped-client"))
                    .roles().get("protected_client_role")
                    .addComposites(List.of(helperClientRole));

            doImport("85.2_update_realm__attempt_change_protected_composites.json", realmImportService);

            RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM).partialExport(true, true);

            RoleRepresentation protectedRealmRole = keycloakRepository.getRealmRole(realm, "protected_realm_role");
            assertThat("protected realm role's realm-composites must not be removed",
                    protectedRealmRole.getComposites().getRealm(), hasItem("helper_realm_role"));

            RoleRepresentation protectedClientRole = keycloakRepository.getClientRole(realm, "moped-client", "protected_client_role");
            assertThat("protected client role's client-composites must not be removed",
                    protectedClientRole.getComposites().getClient(), hasEntry(is("moped-client"), hasItem("helper_client_role")));

            RoleRepresentation unprotectedRealmRole = keycloakRepository.getRealmRole(realm, "unprotected_realm_role");
            assertThat("unprotected realm role referencing a protected client role must sync normally",
                    unprotectedRealmRole.getComposites().getClient(), hasEntry(is("moped-client"), hasItem("protected_client_role")));

            RoleRepresentation unprotectedClientRole = keycloakRepository.getClientRole(realm, "moped-client", "unprotected_client_role");
            assertThat("unprotected client role referencing a protected realm role must sync normally",
                    unprotectedClientRole.getComposites().getRealm(), hasItem("protected_realm_role"));
        }

        private String getClientId(String realmName, String clientId) {
            return keycloakProvider.getInstance().realm(realmName).clients().findByClientId(clientId).get(0).getId();
        }
    }

    // Requirement B.8: protection configured for a client absent from the target realm causes
    // no error and no warning noise - the import proceeds normally.
    @Nested
    @Order(86)
    @TestPropertySource(properties = {
            "import.remote-state.enabled=false",
            "import.protected-roles.mode=replace",
            "import.protected-roles.client-roles.non-existent-client=*"
    })
    class AbsentClientProtectionTest {

        @Autowired
        public RealmImportService realmImportService;

        private static final String REALM = "realmWithAbsentClientProtection";

        @Test
        @Order(0)
        void shouldImportWithoutErrorWhenProtectedClientIsAbsent() throws IOException {
            assertDoesNotThrow(() -> doImport("86.1_create_realm_with_absent_client_protection.json", realmImportService));

            RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM).partialExport(true, true);

            assertThat(keycloakRepository.getRealmRole(realm, "my_realm_role"), notNullValue());
        }
    }
}
