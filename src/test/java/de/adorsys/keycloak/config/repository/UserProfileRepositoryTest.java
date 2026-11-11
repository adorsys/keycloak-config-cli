/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2025 adorsys GmbH & Co. KG @ https://adorsys.com
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

package de.adorsys.keycloak.config.repository;

import com.sun.net.httpserver.HttpServer;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RealmRepresentation;

import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserProfileRepositoryTest {

    private static final String REALM = "test-realm";
    private static final String PROFILE = "{\"attributes\":[{\"name\":\"email\",\"defaultValue\":\"x\"}]}";

    private HttpServer proxy;
    private KeycloakProvider provider;
    private UserProfileRepository repository;
    private final AtomicReference<String> requestPath = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicInteger responseStatus = new AtomicInteger(204);

    @BeforeEach
    void setUp() throws Exception {
        proxy = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        proxy.createContext("/", exchange -> {
            requestPath.set(exchange.getRequestURI().toString());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] error = "invalid profile".getBytes(StandardCharsets.UTF_8);
            int status = responseStatus.get();
            exchange.sendResponseHeaders(status, status == 204 ? -1 : error.length);
            if (status != 204) {
                exchange.getResponseBody().write(error);
            }
            exchange.close();
        });
        proxy.start();

        KeycloakConfigProperties properties = mock(KeycloakConfigProperties.class);
        when(properties.getUrl()).thenReturn("http://keycloak.invalid:9999");
        when(properties.getHttpProxy()).thenReturn(URI.create("http://127.0.0.1:" + proxy.getAddress().getPort()).toURL());
        when(properties.getConnectTimeout()).thenReturn(Duration.ofSeconds(2));
        when(properties.getReadTimeout()).thenReturn(Duration.ofSeconds(2));
        when(properties.isSslVerify()).thenReturn(true);
        when(properties.hasAuthorization()).thenReturn(true);
        when(properties.getAuthorization()).thenReturn("test-token");
        when(properties.getLoginRealm()).thenReturn("master");
        when(properties.getVersion()).thenReturn("26.8.1");
        when(properties.isSkipServerInfo()).thenReturn(true);
        when(properties.getAvailabilityCheck()).thenReturn(
                new KeycloakConfigProperties.KeycloakAvailabilityCheck(false, Duration.ofSeconds(1), Duration.ofSeconds(1)));
        Constructor<KeycloakProvider> constructor = KeycloakProvider.class.getDeclaredConstructor(KeycloakConfigProperties.class);
        constructor.setAccessible(true);
        provider = constructor.newInstance(properties);

        RealmRepository realms = mock(RealmRepository.class);
        RealmResource realm = mock(RealmResource.class);
        UsersResource users = mock(UsersResource.class);
        UserProfileResource profile = mock(UserProfileResource.class);
        when(realms.getResource(REALM)).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(users.userProfile()).thenReturn(profile);
        RealmRepresentation representation = new RealmRepresentation();
        when(realms.get(REALM)).thenReturn(representation);
        repository = new UserProfileRepository(realms, provider);
    }

    @AfterEach
    void tearDown() {
        if (provider != null) {
            provider.close();
        }
        if (proxy != null) {
            proxy.stop(0);
        }
    }

    @Test
    void updatesProfileThroughConfiguredProxyWithoutLosingJsonFields() {
        repository.updateUserProfile(REALM, true, PROFILE);

        assertThat(requestPath.get()).isEqualTo("http://keycloak.invalid:9999/admin/realms/test-realm/users/profile");
        assertThat(authorization.get()).isEqualTo("Bearer test-token");
        assertThat(requestBody.get()).isEqualTo(PROFILE);
    }

    @Test
    void includesResponseStatusAndBodyOnFailure() {
        responseStatus.set(400);

        assertThatThrownBy(() -> repository.updateUserProfile(REALM, true, PROFILE))
                .isInstanceOf(KeycloakRepositoryException.class)
                .hasMessage("Could not update UserProfile Definition")
                .hasCauseInstanceOf(KeycloakRepositoryException.class)
                .cause()
                .hasMessageContaining("Status: 400, Error: invalid profile");
    }
}
