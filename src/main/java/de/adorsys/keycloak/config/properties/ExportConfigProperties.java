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
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import javax.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "export", ignoreUnknownFields = false)
@ConstructorBinding
@Validated
public class ExportConfigProperties {

    private final boolean enabled;
    private final List<String> excludes;
    @NotNull
    private final String location;

    @NotNull
    private final String keycloakVersion;

    public ExportConfigProperties(boolean enabled, List<String> excludes, String keycloakVersion, String location) {
        this.enabled = enabled;
        this.excludes = excludes == null ? List.of() : excludes;
        this.keycloakVersion = keycloakVersion;
        this.location = location;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getExcludes() {
        return excludes;
    }

    public String getKeycloakVersion() {
        return keycloakVersion;
    }

    public String getLocation() {
        return location;
    }
}
