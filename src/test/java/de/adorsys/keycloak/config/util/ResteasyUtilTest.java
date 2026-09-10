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

package de.adorsys.keycloak.config.util;

import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class ResteasyUtilTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private static String testKeystorePath() {
        return Path.of(Objects.requireNonNull(
                ResteasyUtilTest.class.getClassLoader().getResource("tls/test-keystore.p12")).getPath()).toString();
    }

    @Test
    void shouldCreateClientWithoutTlsConfig() {
        ResteasyClient client = ResteasyUtil.getClient(false, null, TIMEOUT, TIMEOUT, null);
        assertNotNull(client);
        assertFalse(client.isClosed());
        client.close();
    }

    @Test
    void shouldCreateClientWithValidKeystore() {
        KeycloakConfigProperties.TlsConfig tlsConfig = new KeycloakConfigProperties.TlsConfig(
                testKeystorePath(), "testpass", "PKCS12",
                null, null, "PKCS12");

        ResteasyClient client = ResteasyUtil.getClient(false, null, TIMEOUT, TIMEOUT, tlsConfig);
        assertNotNull(client);
        assertFalse(client.isClosed());
        client.close();
    }

    @Test
    void shouldCreateClientWithValidTruststore() {
        KeycloakConfigProperties.TlsConfig tlsConfig = new KeycloakConfigProperties.TlsConfig(
                null, null, "PKCS12",
                testKeystorePath(), "testpass", "PKCS12");

        ResteasyClient client = ResteasyUtil.getClient(false, null, TIMEOUT, TIMEOUT, tlsConfig);
        assertNotNull(client);
        assertFalse(client.isClosed());
        client.close();
    }

    @Test
    void shouldCreateClientWithBothKeystoreAndTruststore() {
        String path = testKeystorePath();

        KeycloakConfigProperties.TlsConfig tlsConfig = new KeycloakConfigProperties.TlsConfig(
                path, "testpass", "PKCS12",
                path, "testpass", "PKCS12");

        ResteasyClient client = ResteasyUtil.getClient(false, null, TIMEOUT, TIMEOUT, tlsConfig);
        assertNotNull(client);
        assertFalse(client.isClosed());
        client.close();
    }

    @Test
    void shouldFailWithNonExistentKeystorePath() {
        KeycloakConfigProperties.TlsConfig tlsConfig = new KeycloakConfigProperties.TlsConfig(
                "/nonexistent/path/keystore.p12", "password", "PKCS12",
                null, null, "PKCS12");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> ResteasyUtil.getClient(false, null, TIMEOUT, TIMEOUT, tlsConfig));
        assertTrue(thrown.getMessage().contains("/nonexistent/path/keystore.p12"));
    }

    @Test
    void shouldFailWithNonExistentTruststorePath() {
        KeycloakConfigProperties.TlsConfig tlsConfig = new KeycloakConfigProperties.TlsConfig(
                null, null, "PKCS12",
                "/nonexistent/path/truststore.p12", "password", "PKCS12");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> ResteasyUtil.getClient(false, null, TIMEOUT, TIMEOUT, tlsConfig));
        assertTrue(thrown.getMessage().contains("/nonexistent/path/truststore.p12"));
    }

    @Test
    void shouldFailWithWrongKeystorePassword() {
        KeycloakConfigProperties.TlsConfig tlsConfig = new KeycloakConfigProperties.TlsConfig(
                testKeystorePath(), "wrongpass", "PKCS12",
                null, null, "PKCS12");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> ResteasyUtil.getClient(false, null, TIMEOUT, TIMEOUT, tlsConfig));
        assertTrue(thrown.getMessage().contains("test-keystore.p12"));
    }

    @Test
    void shouldFailWithInvalidKeystoreType() {
        KeycloakConfigProperties.TlsConfig tlsConfig = new KeycloakConfigProperties.TlsConfig(
                testKeystorePath(), "testpass", "INVALID_TYPE",
                null, null, "PKCS12");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> ResteasyUtil.getClient(false, null, TIMEOUT, TIMEOUT, tlsConfig));
        assertTrue(thrown.getMessage().contains("test-keystore.p12"));
    }
}
