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
import de.adorsys.keycloak.config.util.JsonUtil;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

public class ImportUserProfileIT extends AbstractImportIT {

    private static final String REALM_NAME = "realmWithProfile";
    public static final String USER_PROFILE_ENABLED = "userProfileEnabled";

    ImportUserProfileIT() {
        this.resourcePath = "import-files/user-profile";
    }

    @Test
    @Order(0)
    @DisabledIfSystemProperty(named = "keycloak.version", matches = "16.1.1", disabledReason = "Not working")
    void shouldCreateRealmButNoUserProfileEnabled() throws IOException {
        doImport("00_ignore_realm_with_user_profile.json");

        assertRealm(false);

        assertRealmHasUserProfileConfigurationStringWith(is(nullValue()));
    }

    @Test
    @Order(1)
    @DisabledIfSystemProperty(named = "keycloak.version", matches = "16.1.1", disabledReason = "Not working")
    void shouldCreateRealm() throws IOException {
        doImport("01_create_realm_with_user_profile.json");

        assertRealm(true);

        var configurationString = assertRealmHasUserProfileConfigurationStringWith(not(nullValue()));

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

        assertRealm(true);

        var configurationString = assertRealmHasUserProfileConfigurationStringWith(not(nullValue()));

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

        assertRealm(true);

        assertRealmHasUserProfileConfigurationStringWith(not(nullValue()));
        //check untouched?
    }

    @Test
    @Order(4)
    @DisabledIfSystemProperty(named = "keycloak.version", matches = "16.1.1", disabledReason = "Not working")
    void shouldUpdateRealmByRemoveProfileWhenSwitchedOff() throws IOException {
        doImport("04_update_realm_with_user_profile_switched_off.json");

        assertRealm(false);

        assertRealmHasUserProfileConfigurationStringWith(is(nullValue()));
    }

    private void assertRealm(boolean profileEnabled) {
        var realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));
        assertThat(Boolean.parseBoolean(realm.getAttributesOrEmpty().getOrDefault(USER_PROFILE_ENABLED, "false")), is(profileEnabled));
    }

    private String assertRealmHasUserProfileConfigurationStringWith(Matcher<Object> matcher) {
        var userProfileResource = keycloakProvider.getInstance().realm(REALM_NAME).users().userProfile();
        var userProfileResourceConfiguration = JsonUtil.toJson(userProfileResource.getConfiguration());

        assertThat(userProfileResourceConfiguration, matcher);

        return userProfileResourceConfiguration;
    }

}
