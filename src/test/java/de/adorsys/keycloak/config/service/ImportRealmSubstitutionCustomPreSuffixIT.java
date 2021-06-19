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
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@TestPropertySource(properties = {
        "import.var-substitution=true",
        "import.var-substitution-prefix=${",
        "import.var-substitution-suffix=}"
})

class ImportRealmSubstitutionCustomPreSuffixIT extends AbstractImportTest {
    private static final String REALM_NAME = "realm-substitution-custom-pre-suffix";

    ImportRealmSubstitutionCustomPreSuffixIT() {
        this.resourcePath = "import-files/realm-substitution-custom-pre-suffix";
    }

    @Test
    @Order(0)
    @SetSystemProperty(key = "kcc.junit.display-name", value = "<div class=\\\"kc-logo-text\\\"><span>Keycloak</span></div>")
    @SetSystemProperty(key = "kcc.junit.verify-email", value = "true")
    @SetSystemProperty(key = "kcc.junit.not-before", value = "1200")
    @SetSystemProperty(key = "kcc.junit.browser-security-headers", value = "{\"xRobotsTag\":\"noindex\"}")
    void shouldCreateRealm() throws IOException {
        assertThat(System.getProperty("kcc.junit.display-name"), is("<div class=\\\"kc-logo-text\\\"><span>Keycloak</span></div>"));

        doImport("0_create_realm.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getDisplayName(), is("<div class=\"kc-logo-text\"><span>Keycloak</span></div>"));
        assertThat(realm.getDisplayNameHtml(), is("<div class=\"kc-logo-text\"><span>Keycloak - ${role_uma_authorization}</span></div>"));
        assertThat(realm.isVerifyEmail(), is(Boolean.valueOf(System.getProperty("kcc.junit.verify-email"))));
        assertThat(realm.getNotBefore(), is(Integer.valueOf(System.getProperty("kcc.junit.not-before"))));
        assertThat(realm.getBrowserSecurityHeaders().get("xRobotsTag"), is("noindex"));
    }
}
