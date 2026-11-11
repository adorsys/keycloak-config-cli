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

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityProviderConfigMergeTest {

    @Test
    void shouldKeepExistingConfigWhenImportHasNoConfig() {
        Map<String, String> existing = Map.of("clientId", "id", "clientSecret", "**********");

        assertThat(IdentityProviderImportService.mergeConfig(existing, null)).isEqualTo(existing);
        assertThat(IdentityProviderImportService.mergeConfig(existing, Map.of())).isEqualTo(existing);
    }

    @Test
    void shouldOverrideAndAddImportedKeys() {
        Map<String, String> existing = Map.of("clientId", "id", "tokenUrl", "https://old/token");
        Map<String, String> imported = Map.of("tokenUrl", "https://new/token", "syncMode", "FORCE");

        assertThat(IdentityProviderImportService.mergeConfig(existing, imported)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "clientId", "id",
                "tokenUrl", "https://new/token",
                "syncMode", "FORCE"
        ));
    }

    @Test
    void shouldHandleMissingExistingConfig() {
        assertThat(IdentityProviderImportService.mergeConfig(null, Map.of("a", "b"))).containsExactlyEntriesOf(Map.of("a", "b"));
        assertThat(IdentityProviderImportService.mergeConfig(null, null)).isEmpty();
    }
}
