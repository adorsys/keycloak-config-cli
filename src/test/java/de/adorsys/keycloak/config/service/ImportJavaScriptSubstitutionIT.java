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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestPropertySource(properties = {
        "import.var-substitution.enabled=true",
        "import.var-substitution.script-evaluation-enabled=true"
})
class ImportJavaScriptSubstitutionIT extends AbstractImportIT {
    private static final String REALM_NAME = "js-substitution";

    ImportJavaScriptSubstitutionIT() {
        this.resourcePath = "import-files/js-substitution";
    }

    @Test
    @Order(0)
    @SetSystemProperty(key = "JS_TEST_ENABLED", value = "true")
    @SetSystemProperty(key = "APP_ENV", value = "DEV")
    void shouldCreateRealmWithJs() throws IOException {
        doImport("realm-with-js.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat("Realm name should be correct", realm.getRealm(), is(REALM_NAME));
        assertThat("Realm should be enabled", realm.isEnabled(), is(true));
        assertThat("DisplayName should be 'JS Substitution'", realm.getDisplayName(), is("JS Substitution"));
        assertThat("DisplayNameHtml should be 'Development'", realm.getDisplayNameHtml(), is("Development"));
        assertThat("NotBefore should be 600", realm.getNotBefore(), is(600));
        assertThat("RegistrationAllowed should be true", realm.isRegistrationAllowed(), is(true));
    }

    @Test
    @Order(1)
    @SetSystemProperty(key = "APP_ENV", value = "INT")
    void shouldHandleUserReportedUseCases() throws IOException {
        doImport("user-cases.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm("user-cases").toRepresentation();

        assertThat("Enabled should be true (INT === INT)", realm.isEnabled(), is(true));
        assertThat("NotBefore should be 172800 (2*24*60*60)", realm.getNotBefore(), is(172800));
    }

    @Nested
    @TestPropertySource(properties = {
            "import.var-substitution.enabled=true",
            "import.var-substitution.script-evaluation-enabled=false"
    })
    class JsDisabledIT extends AbstractImportIT {
        JsDisabledIT() {
            this.resourcePath = "import-files/js-substitution";
        }

        @Test
        @Order(1)
        void shouldFailWhenJsDisabled() {
            assertThrows(IllegalStateException.class, () -> doImport("realm-with-js.json"));
        }
    }
}
