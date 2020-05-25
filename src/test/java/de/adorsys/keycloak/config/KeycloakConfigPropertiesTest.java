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

package de.adorsys.keycloak.config;

import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

// From: https://tuhrig.de/testing-configurationproperties-in-spring-boot/
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {KeycloakConfigPropertiesTest.TestConfiguration.class})
@ActiveProfiles("IT")
public class KeycloakConfigPropertiesTest {

    @Autowired
    private KeycloakConfigProperties properties;

    @Test
    public void shouldPopulateConfigurationProperties() {
        assertEquals("master", properties.getRealm());
        assertEquals("admin-cli", properties.getClientId());
        assertEquals("admin", properties.getUser());
        assertEquals("admin123", properties.getPassword());
        assertEquals("http://localhost:8080", properties.getUrl());
        assertEquals("default", properties.getMigrationKey());
        assertEquals(true, properties.getSslVerify());
    }

    @EnableConfigurationProperties(KeycloakConfigProperties.class)
    public static class TestConfiguration {
        // nothing
    }
}
