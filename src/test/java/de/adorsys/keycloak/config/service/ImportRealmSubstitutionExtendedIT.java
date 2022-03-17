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
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@TestPropertySource(properties = {
        "import.var-substitution=true",
        "import.var-substitution-in-variables=false",
        "import.var-substitution-undefined-throws-exceptions=false",
        "spring.config.import=configtree:src/test/resources/import-files/realm-substitution-extended/configtree/",
        "kcc.junit.from.spring-boot.property=value from property"
})

@SetSystemProperty(key = "kcc.junit.display-name", value = "<div class=\\\"kc-logo-text\\\"><span>Keycloak</span></div>")
@SetSystemProperty(key = "kcc.junit.verify-email", value = "true")
@SetSystemProperty(key = "kcc.junit.not-before", value = "1200")
@SetSystemProperty(key = "kcc.junit.browser-security-headers", value = "{\"xRobotsTag\":\"noindex\"}")
class ImportRealmSubstitutionExtendedIT extends AbstractImportIT {

    @Autowired
    private Environment env;

    private static final String REALM_NAME = "realm-substitution-extended";

    ImportRealmSubstitutionExtendedIT() {
        this.resourcePath = "import-files/realm-substitution-extended";
    }

    @Test
    @Order(0)
    void shouldCreateRealm() throws IOException {
        assertThat(System.getProperty("kcc.junit.display-name"), is("<div class=\\\"kc-logo-text\\\"><span>Keycloak</span></div>"));

        assertThat(env.getProperty("kcc.junit.from.spring-boot.property"), is("value from property"));
        assertThat(env.getProperty("kcc.junit.from.spring-boot.configtree"), is("value from configtree"));

        doImport("0_update_realm.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getDisplayName(), is("<div class=\"kc-logo-text\"><span>Keycloak</span></div>"));
        assertThat(realm.getDisplayNameHtml(), is(System.getenv("JAVA_HOME") + " - value from property - value from configtree"));
        assertThat(realm.isVerifyEmail(), is(Boolean.valueOf(System.getProperty("kcc.junit.verify-email"))));
        assertThat(realm.getNotBefore(), is(Integer.valueOf(System.getProperty("kcc.junit.not-before"))));
        assertThat(realm.getBrowserSecurityHeaders().get("xRobotsTag"), is("noindex"));
    }
}
