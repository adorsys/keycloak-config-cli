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
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ImportUserUpdateIgnoredPropertiesIT extends AbstractImportIT {
    private static final String REALM_NAME = "realmUserUpdateIgnoredProps";

    ImportUserUpdateIgnoredPropertiesIT() {
        this.resourcePath = "import-files/user-update-ignored-properties";
    }

    @Test
    @Order(0)
    void shouldReproduceIssue() throws IOException {
        // 1. Initial import: create user with old email
        doImport("00_create_realm_with_user.json");

        UserRepresentation user = keycloakRepository.getUser(REALM_NAME, "ldapuser");
        assertThat(user.getEmail(), is("ldapuser@old.example"));

        // 2. Second import: ignore email, and change email in JSON
        // We simulate the CLI flag by modifying the bean
        ImportConfigProperties importProperties = (ImportConfigProperties) ReflectionTestUtils.getField(realmImportService, "importProperties");
        ReflectionTestUtils.setField(importProperties.getBehaviors(), "userUpdateIgnoredProperties", List.of("email"));

        String previousSystemProperty = System.getProperty("import.behaviors.user-update-ignored-properties");
        System.setProperty("import.behaviors.user-update-ignored-properties", "email");
        try {
            doImport("01_update_user_roles_try_change_email.json");
        } finally {
            if (previousSystemProperty == null) {
                System.clearProperty("import.behaviors.user-update-ignored-properties");
            } else {
                System.setProperty("import.behaviors.user-update-ignored-properties", previousSystemProperty);
            }
        }

        user = keycloakRepository.getUser(REALM_NAME, "ldapuser");
        assertThat(user.getEmail(), is("ldapuser@old.example")); // Should still be old email because it's ignored

        // 3. Third import: remove email from ignore list, but same JSON
        ReflectionTestUtils.setField(importProperties.getBehaviors(), "userUpdateIgnoredProperties", List.of());

        doImport("01_update_user_roles_try_change_email.json");

        user = keycloakRepository.getUser(REALM_NAME, "ldapuser");
        // THIS IS WHERE IT IS EXPECTED TO FAIL:
        // If the checksum matches, it will skip the import, and the email will STILL be "ldapuser@old.example"
        assertThat(user.getEmail(), is("ldapuser@new.example"));
    }
}
