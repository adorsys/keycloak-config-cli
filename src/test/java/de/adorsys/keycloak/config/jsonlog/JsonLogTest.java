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

package de.adorsys.keycloak.config.jsonlog;

import de.adorsys.keycloak.config.extensions.GithubActionsExtension;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

// From: https://tuhrig.de/testing-configurationproperties-in-spring-boot/
@ExtendWith(SpringExtension.class)
@ExtendWith(GithubActionsExtension.class)
@SpringBootTest(classes = {JsonLogTest.TestConfiguration.class})
@TestPropertySource(properties = {
        "spring.main.log-startup-info=true",
        "spring.profiles.active=json-log",
        "import.files.locations=default",
})
class JsonLogTest {

    @Autowired
    ConfigurableApplicationContext applicationContext;

    @Test
    void startupUsingJsonLoggingShouldWork() {
        assertThat(applicationContext.isActive(), is(true));
    }

    @EnableConfigurationProperties(ImportConfigProperties.class)
    public static class TestConfiguration {
        // nothing
    }
}
