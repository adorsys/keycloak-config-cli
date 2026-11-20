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

package de.adorsys.keycloak.config.provider;

import de.adorsys.keycloak.config.AbstractImportTest;
import de.adorsys.keycloak.config.model.KeycloakImport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;

// Split out of KeycloakImportProviderIT: as a @Nested class there, @MockServerTest tried to inject its
// MockServerClient field into the nested test instance and failed before the test ran.
@TestPropertySource(properties = {
        "import.files.excludes=**/*create*,**/4_*,src"
})
class KeycloakImportProviderExcludeIT extends AbstractImportTest {
    @Autowired
    KeycloakImportProvider keycloakImportProvider;

    @Test
    void shouldReadLocalFilesFromDirectorySorted() {
        String location = "classpath:import-files/import/sorted/*";
        KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location);

        assertThat(keycloakImport.getRealmImports(), hasKey(is(location)));
        assertThat(keycloakImport.getRealmImports().get(location).keySet(), contains(
                matchesPattern(".+/1_update_realm\\.json"),
                matchesPattern(".+/2_update_realm\\.json"),
                matchesPattern(".+/5_update_realm\\.json"),
                matchesPattern(".+/6_update_realm\\.json"),
                matchesPattern(".+/7_update_realm\\.json"),
                matchesPattern(".+/9_update_realm\\.json")
        ));
    }
}
