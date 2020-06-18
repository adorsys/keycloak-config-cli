/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2020 adorsys GmbH & Co. KG @ https://adorsys.de
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

package de.adorsys.keycloak.config;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

class ImportAuthenticatorConfigIT extends AbstractImportTest {
    private static final String REALM_NAME = "realmWithAuthConfig";

    ImportAuthenticatorConfigIT() {
        this.resourcePath = "import-files/auth-config";
    }

    @Test
    @Order(0)
    void shouldCreateRealmWithFlows() {
        doImport("0_create_realm_with_flow_auth_config.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        Optional<AuthenticatorConfigRepresentation> authConfig = getAuthenticatorConfig(createdRealm, "test auth config");
        assertThat(authConfig.isPresent(), is(true));
        assertThat(authConfig.get().getConfig().get("require.password.update.after.registration"), is("false"));
    }

    @Test
    @Order(1)
    void shouldAddExecutionToFlow() {
        doImport("1_update_realm_auth_config.json");

        RealmRepresentation updatedRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));

        Optional<AuthenticatorConfigRepresentation> changedAuthConfig = getAuthenticatorConfig(updatedRealm, "test auth config");
        assertThat(changedAuthConfig.isPresent(), is(true));
        assertThat(changedAuthConfig.get().getConfig().get("require.password.update.after.registration"), is("true"));
    }

    @Test
    @Order(2)
    void shouldChangeExecutionRequirement() {
        doImport("2_remove_realm_auth_config.json");

        RealmRepresentation updatedRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));

        Optional<AuthenticatorConfigRepresentation> deletedAuthConfig = getAuthenticatorConfig(updatedRealm, "test auth config");
        assertThat(deletedAuthConfig.isPresent(), is(false));
    }

    private Optional<AuthenticatorConfigRepresentation> getAuthenticatorConfig(RealmRepresentation updatedRealm, String configAlias) {
        return updatedRealm
                .getAuthenticatorConfig()
                .stream()
                .filter(x -> x.getAlias().equals(configAlias)).findAny();
    }
}
