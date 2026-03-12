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
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;

@TestPropertySource(properties = {
        "import.users.merge-roles=true",
        "import.users.merge-groups=true"
})
class ImportUsersMergeRolesAndGroupsIT extends AbstractImportIT {
    private static final String REALM_NAME = "realmWithUsersMerge";

    ImportUsersMergeRolesAndGroupsIT() {
        this.resourcePath = "import-files/users-merge";
    }

    @Test
    @Order(0)
    void shouldCreateRealmWithUserWithInitialRolesAndGroups() throws IOException {
        doImport("00_create_realm_with_user_roles_groups.json");

        List<String> realmRoles = keycloakRepository.getUserRealmLevelRoles(REALM_NAME, "alice");
        assertThat(realmRoles, hasItem("user"));

        UserRepresentation user = keycloakRepository.getUser(REALM_NAME, "alice");
        List<GroupRepresentation> groups = keycloakProvider.getInstance().realm(REALM_NAME)
                .users()
                .get(user.getId())
                .groups();

        assertThat(groups.stream().map(GroupRepresentation::getPath).toList(), contains("/employees"));
    }

    @Test
    @Order(1)
    void shouldMergeUserRolesAndGroupsWithoutRemovingExistingAssignments() throws IOException {
        doImport("01_update_realm_merge_user_roles_groups.json");

        List<String> realmRoles = keycloakRepository.getUserRealmLevelRoles(REALM_NAME, "alice");
        assertThat(realmRoles, hasItems("user", "admin"));

        UserRepresentation user = keycloakRepository.getUser(REALM_NAME, "alice");
        List<GroupRepresentation> groups = keycloakProvider.getInstance().realm(REALM_NAME)
                .users()
                .get(user.getId())
                .groups();

        assertThat(groups.stream().map(GroupRepresentation::getPath).toList(), containsInAnyOrder("/employees", "/developers"));
    }

    @Test
    @Order(2)
    void shouldBeIdempotentWhenReimportingInMergeMode() throws IOException {
        doImport("01_update_realm_merge_user_roles_groups.json");

        List<String> realmRoles = keycloakRepository.getUserRealmLevelRoles(REALM_NAME, "alice");
        assertThat(realmRoles, hasItems("user", "admin"));

        UserRepresentation user = keycloakRepository.getUser(REALM_NAME, "alice");
        List<GroupRepresentation> groups = keycloakProvider.getInstance().realm(REALM_NAME)
                .users()
                .get(user.getId())
                .groups();

        assertThat(groups.stream().map(GroupRepresentation::getPath).toList(), containsInAnyOrder("/employees", "/developers"));
    }
}
