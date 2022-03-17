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
import de.adorsys.keycloak.config.exception.InvalidImportException;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImportRealmFileTypeIT extends AbstractImportIT {
    private static final String REALM_NAME = "realm-file-type-auto";

    ImportRealmFileTypeIT() {
        this.resourcePath = "import-files/realm-file-type/auto";
    }

    @Test
    @Order(0)
    void shouldCreateRealm() throws IOException {
        doImport("0_create_realm.yaml");
        doImport("1_update_realm.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));
        assertThat(realm.getLoginTheme(), is("moped"));
    }

    @Test
    @Order(1)
    void shouldCreateMultipleRealm() throws IOException {
        doImport("2_multi_document.yaml");

        for (int i = 0; i < 5; i++) {
            String realmName = REALM_NAME + "-" + i;

            RealmRepresentation realm = keycloakProvider.getInstance().realm(realmName).toRepresentation();

            assertThat(realm.getRealm(), is(realmName));
            assertThat(realm.isEnabled(), is(true));
        }
    }
}

class ImportRealmFileTypeInvalidIT extends AbstractImportIT {
    ImportRealmFileTypeInvalidIT() {
        this.resourcePath = "import-files/realm-file-type/invalid";
    }

    @Test
    @Order(0)
    void shouldThrow() {
        InvalidImportException thrown = assertThrows(InvalidImportException.class, () -> doImport("0_create_realm"));

        assertThat(thrown.getMessage(), is("Unknown file extension: "));
    }
}

class ImportRealmFileTypeSyntaxErrorIT extends AbstractImportIT {
    ImportRealmFileTypeSyntaxErrorIT() {
        this.resourcePath = "import-files/realm-file-type/syntax-error";
    }

    @Test
    @Order(0)
    void shouldThrow() {
        assertThrows(InvalidImportException.class, () -> doImport("0_create_realm"));
    }
}

@TestPropertySource(properties = {
        "import.file-type=yaml",
})
class ImportRealmYamlIT extends AbstractImportIT {
    private static final String REALM_NAME = "realm-file-type-yaml";

    ImportRealmYamlIT() {
        this.resourcePath = "import-files/realm-file-type/yaml";
    }

    @Test
    @Order(0)
    void shouldCreateRealm() throws IOException {
        doImport("0_create_realm.yaml");
        doImport("1_update_realm.yml");
        doImport("2_update_realm.json");
        doImport("3_update_realm_anchors.yaml");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));
        assertThat(realm.getLoginTheme(), is("moped"));
        assertThat(realm.getDisplayName(), is("Realm YAML"));

        UserRepresentation user;
        Map<String, List<String>> userAttributes;

        user = keycloakRepository.getUser(REALM_NAME, "user1");
        assertThat(user.getUsername(), is("user1"));
        assertThat(user.isEnabled(), is(true));

        userAttributes = user.getAttributes();
        assertThat(userAttributes, notNullValue());
        assertThat(userAttributes, hasEntry(is("attr1"), contains("val1")));
        assertThat(userAttributes, hasEntry(is("attr2"), contains("val2")));
        assertThat(userAttributes, hasEntry(is("attr3"), contains("val3")));

        user = keycloakRepository.getUser(REALM_NAME, "user2");
        assertThat(user.getUsername(), is("user2"));
        assertThat(user.isEnabled(), is(true));

        userAttributes = user.getAttributes();
        assertThat(userAttributes, notNullValue());
        assertThat(userAttributes, hasEntry(is("attr1"), contains("val1")));
        assertThat(userAttributes, hasEntry(is("attr2"), contains("val2")));
        assertThat(userAttributes, hasEntry(is("attr3"), contains("val3")));
        assertThat(userAttributes, hasEntry(is("attr4"), contains("val4")));
    }

    @Test
    @Order(99)
    void shouldThrowWithUnknownProperty() {
        InvalidImportException thrown = assertThrows(InvalidImportException.class, () -> doImport("99_invalid_realm.yaml"));

        assertThat(thrown.getMessage(), containsString("Unrecognized field \"unknown-property\" (class de.adorsys.keycloak.config.model.RealmImport), not marked as ignorable"));
    }
}

@TestPropertySource(properties = {
        "import.file-type=json",
})
class ImportRealmJsonIT extends AbstractImportIT {
    private static final String REALM_NAME = "realm-file-type-json";

    ImportRealmJsonIT() {
        this.resourcePath = "import-files/realm-file-type/json";
    }

    @Test
    @Order(0)
    void shouldCreateRealm() throws IOException {
        doImport("0_create_realm.json");
        doImport("1_update_realm.yml");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));
        assertThat(realm.getLoginTheme(), is("moped"));
    }

    @Test
    @Order(99)
    void shouldThrowWithUnknownProperty() {
        InvalidImportException thrown = assertThrows(InvalidImportException.class, () -> doImport("99_invalid_realm.json"));

        assertThat(thrown.getMessage(), containsString("Unrecognized field \"unknown-property\" (class de.adorsys.keycloak.config.model.RealmImport), not marked as ignorable"));
    }
}
