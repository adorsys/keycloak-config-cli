/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2020 adorsys GmbH & Co. KG @ https://adorsys.de
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

// From: https://tuhrig.de/testing-configurationproperties-in-spring-boot/
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {KeycloakConfigPropertiesTest.TestConfiguration.class})
@TestPropertySource(properties = {
        "spring.main.log-startup-info=false",
        "keycloak.ssl-verify=false",
        "keycloak.url=https://localhost:8443",
        "keycloak.login-realm=moped",
        "keycloak.client-id=moped",
        "keycloak.client-id=moped-client",
        "keycloak.user=otherUser",
        "keycloak.password=otherPassword",
})
class KeycloakConfigPropertiesTest {

    @Autowired
    private KeycloakConfigProperties properties;

    @Test
    void shouldPopulateConfigurationProperties() {
        assertThat(properties.getLoginRealm(), is("moped"));
        assertThat(properties.getClientId(), is("moped-client"));
        assertThat(properties.getUser(), is("otherUser"));
        assertThat(properties.getPassword(), is("otherPassword"));
        assertThat(properties.getUrl(), is("https://localhost:8443"));
        assertThat(properties.isSslVerify(), is(false));
    }

    @EnableConfigurationProperties(KeycloakConfigProperties.class)
    public static class TestConfiguration {
        // nothing
    }
}
