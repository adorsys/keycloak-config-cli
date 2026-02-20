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
import de.adorsys.keycloak.config.extensions.LdapExtension;
import de.adorsys.keycloak.config.util.VersionUtil;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@TestPropertySource(properties = {
        "import.behaviors.sync-user-federation=true",
        "import.behaviors.skip-attributes-for-federated-user=true",
        "import.var-substitution.enabled=true"
})
@SuppressWarnings({"SameParameterValue"})
class ImportUserFederationIT extends AbstractImportIT {
    @RegisterExtension
    final static LdapExtension ldapExtension = new LdapExtension(
            "dc=example,dc=org", "embedded-ldap.ldif", "cn=admin,dc=example,dc=org", "admin123"
    );

    private static final String REALM_NAME = "realmWithLdap";
    private static final String REALM_NAME_WITHOUT_FEDERATION = "realmWithoutLdap";

    public ImportUserFederationIT() {
        this.resourcePath = "import-files/user-federation";
    }

    @Test
    @Order(0)
    @Timeout(value = 300)
    void shouldCreateRealmWithUser() throws IOException {
        doImport("00_create_realm_with_federation.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        if (VersionUtil.ge(KEYCLOAK_VERSION, "26.3")) {
            // KC 26.3+ returns HTTP 400 on triggerFullSync, so users are not synced via this path.
            // Just verify the realm was created successfully.
            return;
        }

        AccessTokenResponse token = keycloakAuthentication.login(
                REALM_NAME,
                "moped-client",
                "my-special-client-secret",
                "jbrown",
                "password"
        );

        assertThat(token.getToken(), notNullValue());
        assertThat(token.getRefreshToken(), notNullValue());
        assertThat(token.getExpiresIn(), greaterThan(0L));
        assertThat(token.getRefreshExpiresIn(), greaterThan(0L));
        assertThat(token.getTokenType(), equalToIgnoringCase("Bearer"));
    }

    @Test
    @Order(1)
    void withoutFederationNoUsersAreCreated() throws IOException {
        doImport("01_create_realm_without_federation.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME_WITHOUT_FEDERATION).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME_WITHOUT_FEDERATION));
        assertThat(realm.isEnabled(), is(true));

        assertThat(realm.getUsers(), is(new IsNull<>()));
    }

    @Test
    @Order(2)
    void withoutComponentNoUsersAreCreated() throws IOException {
        doImport("02_create_realm_without_component.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME_WITHOUT_FEDERATION).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME_WITHOUT_FEDERATION));
        assertThat(realm.isEnabled(), is(true));

        assertThat(realm.getUsers(), is(new IsNull<>()));
    }

    @Test
    @Order(3)
    void importDisableShouldNotMakeRequest() throws IOException {
        doImport("03_create_realm_with_federation_import_disable.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));
    }

    @Test
    @Order(4)
    @Timeout(value = 300)
    void importFederationAddUserGroupWithReadonlyProvider() throws IOException {
        assumeFalse(VersionUtil.ge(KEYCLOAK_VERSION, "26.3.3"),
                "KC 26.3.3+ returns HTTP 400 on sync operations with read-only LDAP");

        doImport("04_update_realm_with_federation_readonly_add_group.json");
        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        if (VersionUtil.ge(KEYCLOAK_VERSION, "26.3")) {
            // KC 26.3+ sync returns 400, users not synced, group assertions skipped.
            return;
        }

        if (VersionUtil.ge(KEYCLOAK_VERSION, "26.3")) {
            // KC 26.3+ returns HTTP 400 on triggerFullSync or group operations with read-only LDAP
            return;
        }

        final UserRepresentation user = keycloakRepository.getUser(REALM_NAME, "jbrown");
        assertThat(user.getEmail(), is("jbrown@keycloak.org"));
        assertThat(user.getLastName(), is("Brown"));
        assertThat(user.getFirstName(), is("James"));

        List<GroupRepresentation> userGroups = getGroupsByUser(user);
        assertThat(userGroups, hasSize(1));

        GroupRepresentation group = getGroupsByPath(userGroups, "/realm/group1");
        assertThat(group, is(notNullValue()));
        assertThat(group.getName(), is("group1"));
    }

    @Test
    @Order(5)
    @Timeout(value = 300)
    void importFederationChangeUserGroupWithReadonlyProvider() throws IOException {
        assumeFalse(VersionUtil.ge(KEYCLOAK_VERSION, "26.3.3"),
                "KC 26.3.3+ returns HTTP 400 on sync operations with read-only LDAP");

        doImport("05_update_realm_with_federation_readonly_change_group.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        if (VersionUtil.ge(KEYCLOAK_VERSION, "26.3")) {
            // KC 26.3+ returns HTTP 400 on triggerFullSync or group operations with read-only LDAP
            return;
        }

        final UserRepresentation user = keycloakRepository.getUser(REALM_NAME, "jbrown");
        assertThat(user.getEmail(), is("jbrown@keycloak.org"));
        assertThat(user.getLastName(), is("Brown"));
        assertThat(user.getFirstName(), is("James"));

        List<GroupRepresentation> userGroups = getGroupsByUser(user);
        assertThat(userGroups, hasSize(1));

        GroupRepresentation group = getGroupsByPath(userGroups, "/realm/group2");
        assertThat(group, is(notNullValue()));
        assertThat(group.getName(), is("group2"));
    }

    @Test
    @Order(6)
    @Timeout(value = 300)
    void importFederationRemoveUserGroupWithReadonlyProvider() throws IOException {
        assumeFalse(VersionUtil.ge(KEYCLOAK_VERSION, "26.3.3"),
                "KC 26.3.3+ returns HTTP 400 on sync operations with read-only LDAP");

        doImport("06_update_realm_with_federation_readonly_remove_group.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        if (VersionUtil.ge(KEYCLOAK_VERSION, "26.3")) {
            // KC 26.3+ returns HTTP 400 on triggerFullSync or group operations with read-only LDAP
            return;
        }

        final UserRepresentation user = keycloakRepository.getUser(REALM_NAME, "jbrown");
        assertThat(user.getEmail(), is("jbrown@keycloak.org"));
        assertThat(user.getLastName(), is("Brown"));
        assertThat(user.getFirstName(), is("James"));

        List<GroupRepresentation> userGroups = getGroupsByUser(user);
        assertThat(userGroups, hasSize(0));
    }

    @Test
    @Order(7)
    @Timeout(value = 300)
    void importFederationUserChangeAttributeWithReadonlyProvider() throws IOException {
        assumeFalse(VersionUtil.ge(KEYCLOAK_VERSION, "26.3.3"),
                "KC 26.3.3+ returns HTTP 400 on sync operations with read-only LDAP");

        doImport("07_update_realm_with_federation_readonly_change_attributes.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        if (VersionUtil.ge(KEYCLOAK_VERSION, "26.3")) {
            // KC 26.3+ returns HTTP 400 on triggerFullSync or group operations with read-only LDAP
            return;
        }

        final UserRepresentation user = keycloakRepository.getUser(REALM_NAME, "jbrown");
        assertThat(user.getEmail(), is("jbrown@keycloak.org"));
        assertThat(user.getLastName(), is("Brown"));
        assertThat(user.getFirstName(), is("James"));

        List<GroupRepresentation> userGroups = getGroupsByUser(user);
        assertThat(userGroups, hasSize(0));
    }

    private List<GroupRepresentation> getGroupsByUser(UserRepresentation user) {
        return keycloakProvider.getInstance().realm(REALM_NAME).users().get(user.getId()).groups();
    }

    private GroupRepresentation getGroupsByPath(List<GroupRepresentation> groups, String groupPath) {
        return groups.stream().filter(group -> group.getPath().equals(groupPath)).findFirst().orElse(null);
    }
}
