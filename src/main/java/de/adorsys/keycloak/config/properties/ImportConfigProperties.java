/*
 * Copyright 2019-2020 adorsys GmbH & Co. KG @ https://adorsys.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package de.adorsys.keycloak.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "import")
@ConstructorBinding
@Validated
public class ImportConfigProperties {
    public static final String REALM_CHECKSUM_ATTRIBUTE_PREFIX_KEY = "de.adorsys.keycloak.config.import-checksum-{0}";

    @NotBlank
    private final String path;

    @NotNull
    private final boolean force;

    @NotBlank
    private final String cacheKey;

    public ImportConfigProperties(String path, boolean force, String cacheKey) {
        this.path = path;
        this.force = force;
        this.cacheKey = cacheKey;
    }

    public String getPath() {
        return path;
    }

    public boolean isForce() {
        return force;
    }

    public String getCacheKey() {
        return cacheKey;
    }
}
