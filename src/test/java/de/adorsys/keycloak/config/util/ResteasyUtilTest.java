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

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearSystemProperty;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

    @Test
    @ClearSystemProperty(key = "http.proxyHost")
    @ClearSystemProperty(key = "http.proxyPort")
    void shouldApplyReadTimeoutToRequests() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        CountDownLatch requestReceived = new CountDownLatch(1);
        server.createContext("/", exchange -> {
            requestReceived.countDown();
            try {
                Thread.sleep(500);
                exchange.sendResponseHeaders(200, -1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();

        ResteasyClient client = ResteasyUtil.getClient(false, null,
                Duration.ofSeconds(2), Duration.ofMillis(100), null);
        try {
            var target = client.target("http://127.0.0.1:" + server.getAddress().getPort());

            assertThrows(Exception.class, () -> target.request().get());
            assertTrue(requestReceived.await(1, TimeUnit.SECONDS));
        } finally {
            client.close();
            server.stop(0);
        }
    }

    @Test
    @ClearSystemProperty(key = "http.proxyHost")
    @ClearSystemProperty(key = "http.proxyPort")
    void shouldAllowSelfSignedServerWhenSslVerificationIsDisabled() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (var input = ResteasyUtilTest.class.getClassLoader().getResourceAsStream("tls/test-keystore.p12")) {
            keyStore.load(input, "testpass".toCharArray());
        }
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "testpass".toCharArray());
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

        HttpsServer server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setHttpsConfigurator(new com.sun.net.httpserver.HttpsConfigurator(sslContext));
        server.createContext("/", exchange -> {
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        String url = "https://127.0.0.1:" + server.getAddress().getPort();
        try {
            ResteasyClient verifiedClient = ResteasyUtil.getClient(false, null,
                    Duration.ofSeconds(2), Duration.ofSeconds(2), null);
            try {
                assertThrows(Exception.class, () -> verifiedClient.target(url).request().get());
            } finally {
                verifiedClient.close();
            }

            ResteasyClient unverifiedClient = ResteasyUtil.getClient(true, null,
                    Duration.ofSeconds(2), Duration.ofSeconds(2), null);
            try {
                var response = unverifiedClient.target(url).request().get();
                try {
                    assertEquals(200, response.getStatus());
                } finally {
                    response.close();
                }
            } finally {
                unverifiedClient.close();
            }
        } finally {
            server.stop(0);
        }
    }

}
