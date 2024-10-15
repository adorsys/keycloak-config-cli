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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.keycloak.config.AbstractImportIT;
import de.adorsys.keycloak.config.util.JsonUtil;
import de.adorsys.keycloak.config.util.VersionUtil;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class ImportUserProfileIT extends AbstractImportIT {

    private static final String REALM_NAME = "realmWithProfile";
    private static final String REALM_NAME_UNMANAGED_ATTRIBUTES = "realmWithUnmanagedAttributes";
    public static final String USER_PROFILE_ENABLED = "userProfileEnabled";

    ImportUserProfileIT() {
        this.resourcePath = "import-files/user-profile";
    }

    @Test
    @Order(0)
    @DisabledIfSystemProperty(named = "keycloak.version", matches = "16.1.1", disabledReason = "Not working")
    void shouldCreateRealmButNoUserProfileEnabled() throws IOException {
        assumeTrue(VersionUtil.lt(KEYCLOAK_VERSION,"23")); // this behaviour changed in Keycloak 23

        doImport("00_ignore_realm_with_user_profile.json");

        assertRealm(REALM_NAME, false);

        assertRealmHasUserProfileConfigurationStringWith(REALM_NAME, is(nullValue()));
    }

    @Test
    @Order(1)
    @DisabledIfSystemProperty(named = "keycloak.version", matches = "16.1.1", disabledReason = "Not working")
    void shouldCreateRealm() throws IOException {
        doImport("01_create_realm_with_user_profile.json");

        assertRealm(REALM_NAME, true);

        var configurationString = assertRealmHasUserProfileConfigurationStringWith(REALM_NAME, not(nullValue()));

        var mapper = new ObjectMapper();

        var configurationNode = mapper.readTree(configurationString);

        assertThat(configurationNode.at("/attributes/0/name").asText(), is("username"));
        assertThat(configurationNode.at("/attributes/0/validations/length/min").asInt(), is(1));
    }

    @Test
    @Order(2)
    @DisabledIfSystemProperty(named = "keycloak.version", matches = "16.1.1", disabledReason = "Not working")
    void shouldUpdateRealm() throws IOException {
        doImport("02_update_realm_with_user_profile.json");

        assertRealm(REALM_NAME, true);

        var configurationString = assertRealmHasUserProfileConfigurationStringWith(REALM_NAME, not(nullValue()));

        var mapper = new ObjectMapper();

        var configurationNode = mapper.readTree(configurationString);

        assertThat(configurationNode.at("/attributes/0/name").asText(), is("username"));
        assertThat(configurationNode.at("/attributes/0/validations/length/min").asInt(), is(5));
        assertThat(configurationNode.at("/attributes/1/name").asText(), is("email"));
        assertThat(configurationNode.at("/attributes/1/group").asText(), is("user_informations"));
        assertThat(configurationNode.at("/groups/0/name").asText(), is("user_informations"));
    }

    @Test
    @Order(3)
    @DisabledIfSystemProperty(named = "keycloak.version", matches = "16.1.1", disabledReason = "Not working")
    void shouldNotUpdateRealmByRemoveProfileWhenNothingSet() throws IOException {
        doImport("03_ignore_realm_without_user_profile.json");

        assertRealm(REALM_NAME, true);

        assertRealmHasUserProfileConfigurationStringWith(REALM_NAME, not(nullValue()));
        //check untouched?
    }

    @Test
    @Order(4)
    @DisabledIfSystemProperty(named = "keycloak.version", matches = "16.1.1", disabledReason = "Not working")
    void shouldUpdateRealmByRemoveProfileWhenSwitchedOff() throws IOException {
        assumeTrue(VersionUtil.lt(KEYCLOAK_VERSION,"23")); // this behaviour changed in Keycloak 23

        doImport("04_update_realm_with_user_profile_switched_off.json");

        assertRealm(REALM_NAME, false);

        assertRealmHasUserProfileConfigurationStringWith(REALM_NAME, is(nullValue()));
    }

    @Test
    @Order(5)
    @DisabledIfSystemProperty(named = "keycloak.version", matches = "16.1.1", disabledReason = "Not working")
    void shouldCreateRealmWithUnmanagedAttributes() throws IOException {
        assumeTrue(VersionUtil.ge(KEYCLOAK_VERSION,"24")); // was introduced with KC 24

        doImport("05_create_realm_with_unmanaged_attributes.json");

        assertRealm(REALM_NAME_UNMANAGED_ATTRIBUTES, true);

        var configurationString = assertRealmHasUserProfileConfigurationStringWith(REALM_NAME_UNMANAGED_ATTRIBUTES, not(nullValue()));

        var mapper = new ObjectMapper();

        var configurationNode = mapper.readTree(configurationString);

        assertThat(configurationNode.at("/unmanagedAttributePolicy").asText(), is("ENABLED"));
        assertThat(configurationNode.at("/attributes/0/name").asText(), is("username"));
        assertThat(configurationNode.at("/attributes/0/validations/length/min").asInt(), is(1));
    }

    @Test
    @Order(6)
    @DisabledIfSystemProperty(named = "keycloak.version", matches = "16.1.1", disabledReason = "Not working")
    void shouldUpdateRealmWithUnmanagedAttributes() throws IOException {
        assumeTrue(VersionUtil.ge(KEYCLOAK_VERSION,"24")); // was introduced with KC 24

        doImport("06_update_realm_with_unmanaged_attributes.json");

        assertRealm(REALM_NAME_UNMANAGED_ATTRIBUTES, true);

        var configurationString = assertRealmHasUserProfileConfigurationStringWith(REALM_NAME_UNMANAGED_ATTRIBUTES, not(nullValue()));

        var mapper = new ObjectMapper();

        var configurationNode = mapper.readTree(configurationString);

        assertThat(configurationNode.at("/unmanagedAttributePolicy").asText(), is("ADMIN_EDIT"));
        assertThat(configurationNode.at("/attributes/0/name").asText(), is("username"));
        assertThat(configurationNode.at("/attributes/0/validations/length/min").asInt(), is(1));
    }

    @Test
    @Order(7)
    @DisabledIfSystemProperty(named = "keycloak.version", matches = "16.1.1", disabledReason = "Not working")
    void shouldUpdateRealmWithNoUnmanagedAttributes() throws IOException {
        assumeTrue(VersionUtil.ge(KEYCLOAK_VERSION,"24")); // was introduced with KC 24

        doImport("07_update_realm_with_no_unmanaged_attributes.json");

        assertRealm(REALM_NAME_UNMANAGED_ATTRIBUTES, true);

        var configurationString = assertRealmHasUserProfileConfigurationStringWith(REALM_NAME_UNMANAGED_ATTRIBUTES, not(nullValue()));

        var mapper = new ObjectMapper();

        var configurationNode = mapper.readTree(configurationString);

        assertThat(configurationNode.hasNonNull("unmanagedAttributePolicy"), is(false));
        assertThat(configurationNode.at("/attributes/0/name").asText(), is("username"));
        assertThat(configurationNode.at("/attributes/0/validations/length/min").asInt(), is(1));
    }

    private void assertRealm(String realmName, boolean profileEnabled) {
        var realm = keycloakProvider.getInstance().realm(realmName).toRepresentation();

        assertThat(realm.getRealm(), is(realmName));
        assertThat(realm.isEnabled(), is(true));
        assertThat(Boolean.parseBoolean(realm.getAttributesOrEmpty().getOrDefault(USER_PROFILE_ENABLED, "false")), is(profileEnabled));
    }

    private String assertRealmHasUserProfileConfigurationStringWith(String realmName, Matcher<Object> matcher) {
        var userProfileResource = keycloakProvider.getInstance().realm(realmName).users().userProfile();
        var userProfileResourceConfiguration = JsonUtil.toJson(userProfileResource.getConfiguration());

        assertThat(userProfileResourceConfiguration, matcher);

        return userProfileResourceConfiguration;
    }

}
