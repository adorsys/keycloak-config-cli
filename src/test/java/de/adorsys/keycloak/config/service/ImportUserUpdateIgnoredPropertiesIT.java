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
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;

@TestPropertySource(properties = {
        "import.behaviors.user-update-ignored-properties=attributes,email"
})
@SuppressWarnings({"java:S5961", "java:S5976"})
class ImportUserUpdateIgnoredPropertiesIT extends AbstractImportIT {

    private static final String REALM_NAME = "realmUserUpdateIgnoredProps";

    ImportUserUpdateIgnoredPropertiesIT() {
        this.resourcePath = "import-files/user-update-ignored-properties";
    }

    @Test
    @Order(0)
    void shouldCreateRealmWithUser() throws IOException {
        doImport("00_create_realm_with_user.json");

        UserRepresentation user = keycloakRepository.getUser(REALM_NAME, "ldapuser");
        assertThat(user.getEmail(), is("ldapuser@old.example"));
    }

    @Test
    @Order(1)
    void shouldUpdateRolesButNotOverwriteEmailWhenConfigured() throws IOException {
        UserRepresentation userBefore = keycloakRepository.getUser(REALM_NAME, "ldapuser");
        assertThat(userBefore.getEmail(), is("ldapuser@old.example"));

        doImport("01_update_user_roles_try_change_email.json");

        UserRepresentation userAfter = keycloakRepository.getUser(REALM_NAME, "ldapuser");
        assertThat(userAfter.getEmail(), is("ldapuser@old.example"));

        List<String> realmLevelRoles = keycloakRepository.getUserRealmLevelRoles(REALM_NAME, "ldapuser");
        assertThat(realmLevelRoles, hasItems("role_a", "role_b"));

        List<String> clientLevelRoles = keycloakRepository.getUserClientLevelRoles(REALM_NAME, "ldapuser", "test-client");
        assertThat(clientLevelRoles, contains("client_role_a"));
    }
}
