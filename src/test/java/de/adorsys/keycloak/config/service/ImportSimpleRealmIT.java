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
import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"java:S5961", "java:S5976"})
class ImportSimpleRealmIT extends AbstractImportTest {
    private static final String REALM_NAME = "simple";

    ImportSimpleRealmIT() {
        this.resourcePath = "import-files/simple-realm";
    }

    @Test
    @Order(0)
    void shouldCreateSimpleRealm() throws IOException {
        doImport("00_create_simple-realm.json");

        RealmRepresentation createdRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));
        assertThat(createdRealm.getLoginTheme(), is(nullValue()));
        assertThat(
                createdRealm.getAttributes().get("de.adorsys.keycloak.config.import-checksum-default"),
                is("6292be0628c50ff8fc02bd4092f48a731133e4802e158e7bc2ba174524b4ccf1")
        );
    }

    @Test
    @Order(1)
    void shouldNotUpdateSimpleRealm() throws IOException {
        doImport("00.1_update_simple-realm_with_same_config.json");

        RealmRepresentation createdRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));
        assertThat(createdRealm.getLoginTheme(), is(nullValue()));
        assertThat(
                createdRealm.getAttributes().get("de.adorsys.keycloak.config.import-checksum-default"),
                is("6292be0628c50ff8fc02bd4092f48a731133e4802e158e7bc2ba174524b4ccf1")
        );
    }

    @Test
    @Order(2)
    void shouldUpdateSimpleRealm() throws IOException {
        doImport("01_update_login-theme_to_simple-realm.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));
        assertThat(realm.getLoginTheme(), is("moped"));
        assertThat(
                realm.getAttributes().get("de.adorsys.keycloak.config.import-checksum-default"),
                is("4ac94d3adb91122979e80816a8a355a01f9c7c90a25b6b529bf2a572e1158b1c")
        );
    }

    @Test
    @Order(3)
    void shouldCreateSimpleRealmWithLoginTheme() throws IOException {
        doImport("02_create_simple-realm_with_login-theme.json");

        RealmRepresentation createdRealm = keycloakProvider.getInstance().realm("simpleWithLoginTheme").toRepresentation();

        assertThat(createdRealm.getRealm(), is("simpleWithLoginTheme"));
        assertThat(createdRealm.isEnabled(), is(true));
        assertThat(createdRealm.getLoginTheme(), is("moped"));
        assertThat(
                createdRealm.getAttributes().get("de.adorsys.keycloak.config.import-checksum-default"),
                is("9362cc7d2e91e9b9eee39d0b9306de0f7857f9d6326133335fc2d5cf767f7018")
        );
    }

    @Test
    @Order(4)
    void shouldNotCreateSimpleRealmWithInvalidName() {
        KeycloakRepositoryException thrown = assertThrows(
                KeycloakRepositoryException.class,
                () -> doImport("04_create_simple-realm_with_invalid_name.json")
        );

        assertThat(thrown.getMessage(), matchesPattern("^Cannot create realm '.+': .+$"));
    }

    @Test
    @Order(5)
    void shouldUpdateBruteForceProtection() throws IOException {
        doImport("05_update_simple-realm_with_brute-force-protected.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm("simple").toRepresentation();

        assertThat(realm.getRealm(), is("simple"));
        assertThat(realm.isEnabled(), is(true));
        assertThat(realm.isBruteForceProtected(), is(true));
        assertThat(realm.isPermanentLockout(), is(false));
        assertThat(realm.getMaxFailureWaitSeconds(), is(900));
        assertThat(realm.getMinimumQuickLoginWaitSeconds(), is(60));
        assertThat(realm.getWaitIncrementSeconds(), is(3600));
        assertThat(realm.getQuickLoginCheckMilliSeconds(), is(1000L));
        assertThat(realm.getMaxDeltaTimeSeconds(), is(43200));
        assertThat(realm.getFailureFactor(), is(5));
    }

    @Test
    @Order(6)
    void shouldUpdateSmtpSettings() throws IOException {
        doImport("06_update_simple-realm_with_smtp-settings.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm("simple").toRepresentation();

        assertThat(realm.getRealm(), is("simple"));
        assertThat(realm.isEnabled(), is(true));

        Map<String, String> config = realm.getSmtpServer();

        assertThat(config.get("from"), is("keycloak-config-cli@example.com"));
        assertThat(config.get("fromDisplayName"), is("keycloak-config-cli"));
        assertThat(config.get("host"), is("mta"));
        assertThat(config.get("auth"), is("true"));
        assertThat(config.get("envelopeFrom"), is("keycloak-config-cli@example.com"));
        assertThat(config.get("user"), is("username"));
        assertThat(config.get("password"), is("**********"));
    }

    @Test
    @Order(7)
    void shouldUpdateWebAuthnSettings() throws IOException {
        doImport("07_update_simple-realm_with_web-authn-settings.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm("simple").toRepresentation();

        assertThat(realm.getRealm(), is("simple"));
        assertThat(realm.isEnabled(), is(true));
        assertThat(realm.getWebAuthnPolicyPasswordlessUserVerificationRequirement(), is("required"));
    }

    @Test
    @Order(8)
    void shouldUpdateEventsEnabledAndNotReset() throws IOException {
        doImport("08.1_update_simple-realm_with_events_enabled.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm("simple").toRepresentation();
        assertThat(realm.getRealm(), is("simple"));
        assertThat(realm.isEnabled(), is(true));
        assertThat(realm.isEventsEnabled(), is(true));
        assertThat(realm.isAdminEventsEnabled(), is(true));
        assertThat(realm.isAdminEventsDetailsEnabled(), is(true));

        doImport("08.2_update_simple-realm_with_events_enabled.json");

        realm = keycloakProvider.getInstance().realm("simple").toRepresentation();
        assertThat(realm.getRealm(), is("simple"));
        assertThat(realm.isEnabled(), is(true));
        assertThat(realm.isEventsEnabled(), is(true));
        assertThat(realm.isAdminEventsEnabled(), is(true));
        assertThat(realm.isAdminEventsDetailsEnabled(), is(true));
    }

    @Test
    @Order(9)
    void shouldNotUpdateSimpleRealmWithInvalidName() {
        KeycloakRepositoryException thrown = assertThrows(
                KeycloakRepositoryException.class,
                () -> doImport("09_update_simple-realm_with_invalid_property.json")
        );

        assertThat(thrown.getMessage(), matchesPattern("^Cannot update realm '.+': .+$"));
    }

    @Test
    @Order(10)
    void shouldUpdateSimpleRealmWithDefaultScopes() throws IOException {
        doImport("10.1_update_simple-realm_add_defaultScopes.json");

        RealmRepresentation realm;
        realm = keycloakProvider.getInstance().realm("simple").partialExport(false, false);
        assertThat(realm.getRealm(), is("simple"));
        assertThat(realm.isEnabled(), is(true));
        assertThat(realm.getDefaultDefaultClientScopes(), notNullValue());
        assertThat(realm.getDefaultOptionalClientScopes(), notNullValue());
        assertThat(realm.getDefaultDefaultClientScopes(), contains("email"));
        assertThat(realm.getDefaultOptionalClientScopes(), contains("address"));

        doImport("10.2_update_simple-realm_change_defaultScopes.json");

        realm = keycloakProvider.getInstance().realm("simple").partialExport(false, false);
        assertThat(realm.getRealm(), is("simple"));
        assertThat(realm.isEnabled(), is(true));
        assertThat(realm.getDefaultDefaultClientScopes(), notNullValue());
        assertThat(realm.getDefaultOptionalClientScopes(), notNullValue());
        assertThat(realm.getDefaultDefaultClientScopes(), contains("address"));
        assertThat(realm.getDefaultOptionalClientScopes(), contains("email"));

        doImport("10.3_update_simple-realm_skip_defaultScopes.json");

        realm = keycloakProvider.getInstance().realm("simple").partialExport(false, false);
        assertThat(realm.getRealm(), is("simple"));
        assertThat(realm.isEnabled(), is(true));
        assertThat(realm.getDefaultDefaultClientScopes(), notNullValue());
        assertThat(realm.getDefaultOptionalClientScopes(), notNullValue());
        assertThat(realm.getDefaultDefaultClientScopes(), contains("address"));
        assertThat(realm.getDefaultOptionalClientScopes(), contains("email"));

        doImport("10.4_update_simple-realm_remove_defaultScopes.json");

        realm = keycloakProvider.getInstance().realm("simple").partialExport(false, false);
        assertThat(realm.getRealm(), is("simple"));
        assertThat(realm.isEnabled(), is(true));
        assertThat(realm.getDefaultDefaultClientScopes(), notNullValue());
        assertThat(realm.getDefaultOptionalClientScopes(), notNullValue());
        assertThat(realm.getDefaultDefaultClientScopes(), empty());
        assertThat(realm.getDefaultOptionalClientScopes(), empty());

        doImport("10.4_update_simple-realm_remove_defaultScopes.json");

        realm = keycloakProvider.getInstance().realm("simple").partialExport(false, false);
        assertThat(realm.getRealm(), is("simple"));
        assertThat(realm.isEnabled(), is(true));
        assertThat(realm.getDefaultDefaultClientScopes(), notNullValue());
        assertThat(realm.getDefaultOptionalClientScopes(), notNullValue());
        assertThat(realm.getDefaultDefaultClientScopes(), empty());
        assertThat(realm.getDefaultOptionalClientScopes(), empty());

        ImportProcessingException thrown;
        thrown = assertThrows(
                ImportProcessingException.class,
                () -> doImport("10.5_update_simple-realm_invalid-default_defaultScopes.json")
        );

        assertThat(thrown.getMessage(), is("Could not find client scope 'non-exist' in realm 'simple'!"));

        thrown = assertThrows(
                ImportProcessingException.class,
                () -> doImport("10.6_update_simple-realm_invalid-optional_defaultScopes.json")
        );

        assertThat(thrown.getMessage(), is("Could not find client scope 'non-exist' in realm 'simple'!"));
    }
}
