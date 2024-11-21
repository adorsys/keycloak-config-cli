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

package de.adorsys.keycloak.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

/*
 * Duplicated prefix keycloak. Since only one of the two classes is loaded (depending on configuration) this is fine.
 * This saves us from having to define a keycloak address for the normalization usage, since we don't actually need to
 * talk to a keycloak instance, and we only need to know the version.
 */
@ConfigurationProperties(prefix = "normalize", ignoreUnknownFields = false)
@Validated
public class NormalizationKeycloakConfigProperties {

    @NotNull
    private final String version;

    public NormalizationKeycloakConfigProperties(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
