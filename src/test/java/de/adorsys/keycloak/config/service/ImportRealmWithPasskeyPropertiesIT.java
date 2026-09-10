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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import de.adorsys.keycloak.config.AbstractImportIT;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;

class ImportRealmWithPasskeyPropertiesIT extends AbstractImportIT {
    private static final String REALM_NAME = "passkeys-realm";

    ImportRealmWithPasskeyPropertiesIT() {
        this.resourcePath = "import-files/realm-passkeys/";
    }

    @Test
    @Order(0)
     void shouldImportRealmWithPasskeysEnabled() throws Exception {
        doImport("00_update-realm_with_passkeys-enabled.json");

        RealmRepresentation updatedRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();
        assertThat(updatedRealm.getWebAuthnPolicyPasswordlessPasskeysEnabled(), is(true));
        assertThat(updatedRealm.getWebAuthnPolicyPasswordlessRpEntityName(), is("Keycloak"));
    }

    @Test
    @Order(1)
     void shouldImportRealmWithPasskeysDisabled() throws Exception {
        doImport("01_update-realm_passkeys-disabled.json");

        RealmRepresentation updatedRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();
        assertThat(updatedRealm.getWebAuthnPolicyPasswordlessPasskeysEnabled(), is(false));
        assertThat(updatedRealm.getWebAuthnPolicyPasswordlessRpEntityName(), is("Keycloak"));
    }

    @Test
    @Order(2)
     void shouldPreservePasskeySettingsWhenUpdatingRealm() throws Exception {
        doImport("00_update-realm_with_passkeys-enabled.json");

        RealmRepresentation updatedRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();
        assertThat(updatedRealm.getWebAuthnPolicyPasswordlessPasskeysEnabled(), is(true));
        assertThat(updatedRealm.getWebAuthnPolicyPasswordlessRpEntityName(), is("Keycloak"));

        doImport("02_update-realm_without_passkeys-settings.json");

        // Assert that previous Config not affected
        updatedRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();
        assertThat(updatedRealm.getWebAuthnPolicyPasswordlessPasskeysEnabled(), is(true));
        assertThat(updatedRealm.getWebAuthnPolicyPasswordlessRpEntityName(), is("Keycloak"));
    }

}
