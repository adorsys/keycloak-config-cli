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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

class AuthorizeImportUsingServiceAccountIT extends AbstractImportTest {

    private static final String REALM_NAME = "service-account";

    AuthorizeImportUsingServiceAccountIT() {
        this.resourcePath = "import-files/service-account";
    }

    @Test
    @Order(1)
    void createClientWithServiceAccountEnabled() {
        doImport("01_create_realm_client_with_service_account_enabled.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientRepresentation client = keycloakRepository.getClient(
                REALM_NAME,
                "config-cli"
        );

        assertThat(client.isServiceAccountsEnabled(), is(true));
    }

    @Nested
    @TestPropertySource(properties = {
            "keycloak.password=invalid",
            "keycloak.grant-type=client_credentials",
            "keycloak.client-id=config-cli",
            "keycloak.login-realm=service-account",
            "keycloak.client-secret=config-cli-secret",
            "keycloak.client-secret=config-cli-secret",
    })
    class importRealmUsingServiceAccount {
        @Test
        @Order(2)
        void run() {
            doImport("02_update_realm_client_with_service_account_enabled.json");

            RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

            assertThat(realm.getRealm(), is(REALM_NAME));
            assertThat(realm.isEnabled(), is(true));
            assertThat(realm.getLoginTheme(), is("moped"));
        }
    }
}
