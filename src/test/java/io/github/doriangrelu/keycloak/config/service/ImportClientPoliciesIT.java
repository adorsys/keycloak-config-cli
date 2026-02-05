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
package io.github.doriangrelu.keycloak.config.service;

import io.github.doriangrelu.keycloak.config.AbstractImportIT;
import io.github.doriangrelu.keycloak.config.util.VersionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ImportClientPoliciesIT extends AbstractImportIT {

    private static final String REALM_NAME = "realmWithClientPolicies";

    ImportClientPoliciesIT() {
        this.resourcePath = "import-files/client-policies";
    }

    @BeforeEach
    void init() {
        assumeTrue(VersionUtil.ge(KEYCLOAK_VERSION, "20"));
    }

    @Test
    @Order(1)
    void shouldCreateRealm() throws IOException {
        doImport("00_create_realm_with_no_client_policies.json");

        assertRealm();
    }

    @Test
    @Order(2)
    void shouldCreateRealmWithClientPolicies() throws IOException {
        doImport("01_create_realm_with_client_policies.json");

        var realm = assertRealm();

        var parsedClientProfiles = realm.getParsedClientProfiles();
        assertThat(parsedClientProfiles.getProfiles()).hasSize(1);
        assertThat(parsedClientProfiles.getProfiles()).first().extracting(ClientProfileRepresentation::getName).isEqualTo("acme-client-profile");

        var parsedClientPolicies = realm.getParsedClientPolicies();
        assertThat(parsedClientPolicies.getPolicies()).hasSize(1);
        assertThat(parsedClientPolicies.getPolicies()).first().extracting(ClientPolicyRepresentation::getName).isEqualTo("acme-client-policy");
    }

    @Test
    @Order(3)
    void shouldCreateRealmWithClientPoliciesWithOnlyOneProfile() throws IOException {
        doImport("02_create_realm_with_client_policies_only_1_profile.json");

        var realm = assertRealm();

        var parsedClientProfiles = realm.getParsedClientProfiles();
        assertThat(parsedClientProfiles.getProfiles()).hasSize(1);
        assertThat(parsedClientProfiles.getProfiles()).first().extracting(ClientProfileRepresentation::getName).isEqualTo("acme-client-profile");
    }

    @Test
    @Order(4)
    void shouldCreateRealmWithClientPoliciesWithOnlyTwoProfiles() throws IOException {
        doImport("03_create_realm_with_client_policies_only_2_profiles.json");

        var realm = assertRealm();

        var parsedClientProfiles = realm.getParsedClientProfiles();
        assertThat(parsedClientProfiles.getProfiles()).hasSize(2);
        assertThat(parsedClientProfiles.getProfiles()).first().extracting(ClientProfileRepresentation::getName).isEqualTo("acme-client-profile-1");
        assertThat(parsedClientProfiles.getProfiles()).element(1).extracting(ClientProfileRepresentation::getName).isEqualTo("acme-client-profile-2");
    }

    @Test
    @Order(5)
    void shouldCreateRealmWithClientPoliciesWithOnlyOnePolicy() throws IOException {
        doImport("04_create_realm_with_client_policies_only_1_policies.json");

        var realm = assertRealm();

        var parsedClientPolicies = realm.getParsedClientPolicies();
        assertThat(parsedClientPolicies.getPolicies()).hasSize(1);
        assertThat(parsedClientPolicies.getPolicies()).hasSize(1);
        assertThat(parsedClientPolicies.getPolicies()).first().extracting(ClientPolicyRepresentation::getName).isEqualTo("acme-client-policy");
    }

    private RealmRepresentation assertRealm() {
        var realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();
        assertThat(realm.getRealm()).isEqualTo(REALM_NAME);
        return realm;
    }
}
