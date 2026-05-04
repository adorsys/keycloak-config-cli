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

package de.adorsys.keycloak.config.condition;

import de.adorsys.keycloak.config.configuration.TestConfiguration;
import de.adorsys.keycloak.config.repository.OrganizationRepository;
import de.adorsys.keycloak.config.service.OrganizationImportService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class OrganizationVersionConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
                    "spring.main.log-startup-info=false",

                    "import.files.locations=classpath:import-files",
                    "import.behaviors.checksum-changed=fail",
                    "keycloak.url=http://localhost:8080",
                    "keycloak.login-realm=master",
                    "keycloak.client-id=admin-cli",
                    "keycloak.user=admin",
                    "keycloak.password=admin",
                    "keycloak.grant-type=password",
                    "keycloak.ssl-verify=false",
                    "keycloak.skip-server-info=true"
            );

    @Test
    void shouldNotCreateOrganizationBeansForKeycloak25() {
        contextRunner
                .withPropertyValues("keycloak.version=25.0.0")
                .run(context -> {
                    assertThat(context.getBeansOfType(OrganizationRepository.class).values(), empty());
                    assertThat(context.getBeansOfType(OrganizationImportService.class).values(), empty());
                });
    }

    @Test
    void shouldCreateOrganizationBeansForKeycloak26() {
        contextRunner
                .withPropertyValues("keycloak.version=26.1.3")
                .run(context -> {
                    assertThat(context.getBeansOfType(OrganizationRepository.class).values(), hasSize(1));
                    assertThat(context.getBeansOfType(OrganizationImportService.class).values(), hasSize(1));
                });
    }
}
