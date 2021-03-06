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
import de.adorsys.keycloak.config.model.RealmImport;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import java.io.IOException;
import java.util.List;

import static de.adorsys.keycloak.config.test.util.KeycloakRepository.getAuthenticatorConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"java:S5961", "java:S5976"})
class ImportAuthenticatorConfigIT extends AbstractImportTest {
    private static final String REALM_NAME = "realmWithAuthConfig";

    ImportAuthenticatorConfigIT() {
        this.resourcePath = "import-files/auth-config";
    }

    @Test
    @Order(0)
    void shouldCreateRealmWithFlows() throws IOException {
        doImport("0_create_realm_with_flow_auth_config.json");

        RealmRepresentation createdRealm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        List<AuthenticatorConfigRepresentation> authConfig;
        authConfig = getAuthenticatorConfig(createdRealm, "test auth config");
        assertThat(authConfig, is(not(empty())));
        assertThat(authConfig, hasSize(1));
        assertThat(authConfig.get(0).getConfig().get("require.password.update.after.registration"), is("false"));

        authConfig = getAuthenticatorConfig(createdRealm, "create unique user config");
        assertThat(authConfig, is(not(empty())));
        assertThat(authConfig, hasSize(1));
        assertThat(authConfig.get(0).getConfig().get("require.password.update.after.registration"), is("false"));

        authConfig = getAuthenticatorConfig(createdRealm, "review profile config");
        assertThat(authConfig, is(not(empty())));
        assertThat(authConfig, hasSize(1));
        assertThat(authConfig.get(0).getConfig().get("update.profile.on.first.login"), is("missing"));
    }

    @Test
    @Order(1)
    void shouldAddExecutionToFlow() throws IOException {
        doImport("1_update_realm_auth_config.json");

        RealmRepresentation updatedRealm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));

        List<AuthenticatorConfigRepresentation> authConfig = getAuthenticatorConfig(updatedRealm, "test auth config");
        assertThat(authConfig, is(not(empty())));
        assertThat(authConfig, hasSize(1));
        assertThat(authConfig.get(0).getConfig().get("require.password.update.after.registration"), is("true"));
    }

    @Test
    @Order(2)
    void shouldChangeExecutionRequirement() throws IOException {
        doImport("2_remove_realm_auth_config.json");

        RealmRepresentation updatedRealm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));

        List<AuthenticatorConfigRepresentation> authConfig = getAuthenticatorConfig(updatedRealm, "test auth config");
        assertThat(authConfig, is(empty()));
    }

    @Test
    @Order(3)
    void shouldUpdateRealmCreateFlowAuthConfigInsideNonTopLevelFlow() throws IOException {
        doImport("3_update_realm__create_flow_auth_config_inside_non_top_level_flow.json");

        RealmRepresentation updatedRealm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));

        List<AuthenticatorConfigRepresentation> authConfig = getAuthenticatorConfig(updatedRealm, "other test auth config");
        assertThat(authConfig, is(not(empty())));
        assertThat(authConfig, hasSize(1));
        assertThat(authConfig.get(0).getConfig().get("require.password.update.after.registration"), is("false"));
    }

    @Test
    @Order(4)
    void shouldUpdateRealmUpdateFlowAuthConfigInsideNonTopLevelFlow() throws IOException {
        doImport("4_update_realm__update_flow_auth_config_inside_non_top_level_flow.json");

        RealmRepresentation updatedRealm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));

        List<AuthenticatorConfigRepresentation> authConfig = getAuthenticatorConfig(updatedRealm, "other test auth config");
        assertThat(authConfig, is(not(empty())));
        assertThat(authConfig, hasSize(1));
        assertThat(authConfig.get(0).getConfig().get("require.password.update.after.registration"), is("true"));
    }

    @Test
    @Order(5)
    void shouldUpdateRealmDeleteFlowAuthConfigInsideNonTopLevelFlow() throws IOException {
        doImport("5_update_realm__delete_flow_auth_config_inside_non_top_level_flow.json");

        RealmRepresentation updatedRealm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));

        List<AuthenticatorConfigRepresentation> authConfig = getAuthenticatorConfig(updatedRealm, "other test auth config");
        assertThat(authConfig, is(empty()));
    }

    @Test
    @Order(6)
    void shouldUpdateRealmCreateFlowAuthConfigInsideBuiltinNonTopLevelFlow() throws IOException {
        doImport("6_update_realm__create_flow_auth_config_inside_builtin_non_top_level_flow.json");

        RealmRepresentation updatedRealm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));

        List<AuthenticatorConfigRepresentation> authConfig = getAuthenticatorConfig(updatedRealm, "custom-recaptcha");
        assertThat(authConfig, is(not(empty())));
        assertThat(authConfig, hasSize(1));
        assertThat(authConfig.get(0).getConfig().get("useRecaptchaNet"), is("false"));
    }

    @Test
    @Order(7)
    void shouldUpdateRealmUpdateFlowAuthConfigInsideBuiltinNonTopLevelFlow() throws IOException {
        doImport("7_update_realm__update_flow_auth_config_inside_builtin_non_top_level_flow.json");

        RealmRepresentation updatedRealm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));

        List<AuthenticatorConfigRepresentation> authConfig = getAuthenticatorConfig(updatedRealm, "custom-recaptcha");
        assertThat(authConfig, is(not(empty())));
        assertThat(authConfig, hasSize(1));
        assertThat(authConfig.get(0).getConfig().get("useRecaptchaNet"), is("true"));
    }

    @Test
    @Order(8)
    void shouldUpdateRealmDeleteFlowAuthConfigInsideBuiltinNonTopLevelFlow() throws IOException {
        doImport("8_update_realm__delete_flow_auth_config_inside_builtin_non_top_level_flow.json");

        RealmRepresentation updatedRealm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));

        List<AuthenticatorConfigRepresentation> authConfig = getAuthenticatorConfig(updatedRealm, "custom-recaptcha");
        assertThat(authConfig, is(empty()));
    }

    @Test
    @Order(9)
    void shouldThrowInvalidAuthConfig() throws IOException {
        RealmImport foundImport = getImport("9_update_realm__invalid_auth_config.json");

        ImportProcessingException thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), is("Authenticator Config 'custom-recaptcha' not found. Config must be used in execution"));
    }
}
