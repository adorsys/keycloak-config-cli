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
import de.adorsys.keycloak.config.exception.InvalidImportException;
import de.adorsys.keycloak.config.model.RealmImport;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"java:S5961", "java:S5976"})
class ImportDefaultGroupsIT extends AbstractImportTest {
    private static final String REALM_NAME = "realmWithDefaultGroups";

    ImportDefaultGroupsIT() {
        this.resourcePath = "import-files/default-groups";
    }

    @Test
    @Order(0)
    void shouldCreateRealmWithDefaultGroups() throws IOException {
        doImport("00_create_realm_with_default_groups.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));
        assertThat(realm.getDefaultGroups(), containsInAnyOrder("/My Group"));
    }

    @Test
    @Order(1)
    void shouldUpdateRealmRemoveAndAddDefaultGroup() throws IOException {
        doImport("01_update_realm_remove_and_add_default_group.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));
        assertThat(realm.getDefaultGroups(), containsInAnyOrder("/My Added Group"));
    }

    @Test
    @Order(2)
    void shouldUpdateRealmAddDefaultSubGroup() throws IOException {
        doImport("02_update_realm_add_default_subgroup.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));
        assertThat(realm.getDefaultGroups(), containsInAnyOrder("/My Group", "/My Added Group", "/Group with subgroup/My SubGroup"));
    }

    @Test
    @Order(96)
    void shouldUpdateRealmAddNonExistSubGroup() throws IOException {
        RealmImport foundImport = getImport("96_update_realm_add_non_exists_default_subgroup.json");

        InvalidImportException thrown = assertThrows(InvalidImportException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), is("Unable to add default group '/not-exist'. Does group exists?"));
    }

    @Test
    @Order(97)
    void shouldUpdateRealmRemoveDefaultGroup() throws IOException {
        doImport("97_update_realm_remove_default_group.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));
        assertThat(realm.getDefaultGroups(), containsInAnyOrder("/My Group", "/Group with subgroup/My SubGroup"));
    }

    @Test
    @Order(98)
    void shouldUpdateRealmSkipDefaultGroup() throws IOException {
        doImport("98_update_realm_skip_default_group.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));
        assertThat(realm.getDefaultGroups(), containsInAnyOrder("/My Group", "/Group with subgroup/My SubGroup"));
    }

    @Test
    @Order(99)
    void shouldUpdateRealmRemoveAllDefaultGroup() throws IOException {
        doImport("99_update_realm_remove_all_default_group.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));
        assertThat(realm.getGroups(), is(nullValue()));
        assertThat(realm.getDefaultGroups(), is(nullValue()));
    }
}
