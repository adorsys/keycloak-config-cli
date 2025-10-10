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

package de.adorsys.keycloak.config.configuration;

import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;


public class RestClientX509ConfigTest {

    @Test
    void testIsX509ConfiguredWithAllProperties() {
        KeycloakConfigProperties.X509Config x509Config = new KeycloakConfigProperties.X509Config(
                "certs/client.p12",
                "changeit",
                "certs/truststore.jks",
                "changeit"
        );

        KeycloakConfigProperties properties = createMockProperties(x509Config);
        RestClientX509Config restClientX509Config = new RestClientX509Config(properties);

        Assertions.assertTrue(restClientX509Config.isX509Configured());
    }

    @Test
    void testIsX509ConfiguredWithPartialProperties() {
        KeycloakConfigProperties.X509Config x509Config = new KeycloakConfigProperties.X509Config(
                "certs/client.p12",
                "changeit",
                null,
                null
        );

        KeycloakConfigProperties properties = createMockProperties(x509Config);
        RestClientX509Config restClientX509Config = new RestClientX509Config(properties);

        Assertions.assertFalse(restClientX509Config.isX509Configured());
    }

    @Test
    void testIsX509ConfiguredWithEmptyStrings() {
        KeycloakConfigProperties.X509Config x509Config = new KeycloakConfigProperties.X509Config(
                "",
                "",
                "",
                ""
        );

        KeycloakConfigProperties properties = createMockProperties(x509Config);
        RestClientX509Config restClientX509Config = new RestClientX509Config(properties);

        Assertions.assertFalse(restClientX509Config.isX509Configured());
    }

    private KeycloakConfigProperties createMockProperties(KeycloakConfigProperties.X509Config x509Config) {
        return new KeycloakConfigProperties(
                "master",
                "admin-cli",
                "test-version",
                "http://localhost:8080",
                "admin",
                "admin123",
                "",
                "password",
                true,
                null,
                new KeycloakConfigProperties.KeycloakAvailabilityCheck(false, Duration.ofSeconds(120), Duration.ofSeconds(2)),
                Duration.ofSeconds(10),
                Duration.ofSeconds(10),
                x509Config
        );
    }
}
