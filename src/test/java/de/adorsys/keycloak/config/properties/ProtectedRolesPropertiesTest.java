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

import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportProtectedRolesProperties.ProtectedRolesMode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Requirement C.13: an unknown/invalid `import.protected-roles.mode` value must fail
 * application STARTUP with a clear message naming the offending property.
 * Requirement C.9: with zero configuration, no explicit protected-roles properties are
 * bound, and the defaults are resolved elsewhere (ProtectedRoleResolver) from empty lists.
 */
class ProtectedRolesPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues("import.files.locations=classpath:import-files");

    @Test
    void shouldBindEmptyDefaultsWhenNotConfigured() {
        contextRunner.run(context -> {
            ImportConfigProperties properties = context.getBean(ImportConfigProperties.class);

            assertThat(properties.getProtectedRoles().getMode(), is(ProtectedRolesMode.ADD));
            assertThat(properties.getProtectedRoles().getRealmRoles(), is(empty()));
            assertThat(properties.getProtectedRoles().getClientRoles(), is(anEmptyMap()));
        });
    }

    @Test
    void shouldFailStartupOnInvalidMode() {
        contextRunner
                .withPropertyValues("import.protected-roles.mode=bogus-mode")
                .run(context -> {
                    Throwable startupFailure = context.getStartupFailure();
                    assertThat(startupFailure, is(notNullValue()));

                    StringBuilder causeChainMessages = new StringBuilder();
                    for (Throwable cause = startupFailure; cause != null; cause = cause.getCause()) {
                        causeChainMessages.append(cause.getMessage()).append('\n');
                    }

                    // Spring Boot 3.4.5's top-level ConfigurationPropertiesBindException.getMessage()
                    // is a generic hardcoded string; the offending property path only appears in a
                    // nested cause (e.g. BindException / ConversionFailedException), so the whole
                    // cause chain must be inspected.
                    assertThat(causeChainMessages.toString(), containsString("import.protected-roles.mode"));
                });
    }

    @EnableConfigurationProperties(ImportConfigProperties.class)
    public static class TestConfiguration {
        // nothing
    }
}
