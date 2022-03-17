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

package de.adorsys.keycloak.config.provider;

import de.adorsys.keycloak.config.AbstractImportIT;
import de.adorsys.keycloak.config.exception.KeycloakProviderException;
import de.adorsys.keycloak.config.resource.ManagementPermissions;
import org.apache.http.conn.ConnectTimeoutException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.*;

class KeycloakProviderIT {
    @Nested
    @TestPropertySource(properties = {
            "keycloak.read-timeout=PT0.01S"
    })
    class ResteasyReadTimeout extends AbstractImportIT {
        @Test
        void run() {
            // very low read timeout leads inevitably to a read timeout, which in turn shows that the configuration is applied
            ProcessingException thrown = assertThrows(ProcessingException.class, keycloakProvider::getInstance);
            assertNotNull(thrown.getCause());
            assertTrue(thrown.getCause() instanceof SocketTimeoutException);
            assertThat(thrown.getCause().getMessage(), matchesPattern(".*[Rr]ead timed out.*"));
        }
    }

    @Nested
    @TestPropertySource(properties = {
            "keycloak.url=https://10.255.255.1",
            "keycloak.connect-timeout=PT0.01S"
    })
    class ResteasyConnectTimeout extends AbstractImportIT {
        @Test
        @Timeout(value = 1L)
        void run() {
            // connect timeout since IP is not reachable - test fails if it exceeds one second which in turn shows that
            // the configuration is applied
            ProcessingException thrown = assertThrows(ProcessingException.class, keycloakProvider::getInstance);
            assertNotNull(thrown.getCause());
            assertTrue(thrown.getCause() instanceof ConnectTimeoutException);
            assertThat(thrown.getCause().getMessage(), matchesPattern(".*[Cc]onnect timed out.*"));
        }
    }

    @Nested
    @TestPropertySource(properties = {
            "keycloak.url=https://localhost:1",
            "keycloak.availability-check.enabled=true",
            "keycloak.availability-check.timeout=300ms",
            "keycloak.availability-check.retry-delay=100ms",
    })
    class RaiseTimeout extends AbstractImportIT {
        @Autowired
        public KeycloakProvider keycloakProvider;

        @Test
        void run() {
            KeycloakProviderException thrown = assertThrows(KeycloakProviderException.class, keycloakProvider::getInstance);

            assertThat(thrown.getMessage(), matchesPattern("Could not connect to keycloak in 0 seconds: .*$"));
        }
    }

    @Nested
    @TestPropertySource(properties = {
            "keycloak.url=${keycloak.baseUrl}/z/"
    })
    class InvalidServerUrl extends AbstractImportIT {
        @Autowired
        public KeycloakProvider keycloakProvider;

        @Test
        void run() {
            assertThrows(NotFoundException.class, keycloakProvider::getKeycloakVersion);
        }
    }

    @Nested
    @SetSystemProperty(key = "http.proxyHost", value = "localhost")
    @SetSystemProperty(key = "http.proxyPort", value = "2")
    @TestPropertySource(properties = {
            "keycloak.url=https://keycloak:8080/auth/",
    })
    class HttpProxySystemProperties extends AbstractImportIT {
        @Autowired
        public KeycloakProvider keycloakProvider;

        @Test
        void testHttpProxy() {
            ProcessingException thrown = assertThrows(ProcessingException.class, keycloakProvider::getKeycloakVersion);

            assertThat(thrown.getMessage(), matchesPattern(".+ Connect to localhost:2 .+ failed: .+"));
        }
    }

    @Nested
    @TestPropertySource(properties = {
            "keycloak.url=https://keycloak:8080/auth/",
            "keycloak.http-proxy=http://localhost:2",
    })
    class HttpProxySpringProperties extends AbstractImportIT {
        @Test
        void run() {
            ProcessingException thrown = assertThrows(ProcessingException.class, keycloakProvider::getKeycloakVersion);

            assertThat(thrown.getMessage(), matchesPattern(".+ Connect to localhost:2 .+ failed: .+"));
        }
    }

    @Nested
    class GetCustomApiProxy extends AbstractImportIT {
        @Test
        void run() {
            ManagementPermissions proxy = keycloakProvider.getCustomApiProxy(ManagementPermissions.class);
            assertNotNull(proxy);
        }
    }

    @Nested
    @TestPropertySource(properties = {
            "keycloak.url=http://crappy|url"
    })
    class GetCustomApiProxyInvalidUri extends AbstractImportIT {
        @Test
        void run() {
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> keycloakProvider.getCustomApiProxy(ManagementPermissions.class));
            assertNotNull(thrown.getCause());
            assertTrue(thrown.getCause() instanceof URISyntaxException);
        }
    }
}
