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

package de.adorsys.keycloak.config.token;

import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.AccessTokenResponse;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RestClientX509TokenManagerTest {

    @Mock
    private KeycloakConfigProperties properties;

    @Mock
    private Supplier<ResteasyClient> resteasyClientSupplier;

    @Mock
    private ResteasyClient resteasyClient;

    @Mock
    private ResteasyWebTarget target;

    @Mock
    private Invocation.Builder builder;

    @Mock
    private Response response;

    private RestClientX509TokenManager tokenManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(properties.getUrl()).thenReturn("https://localhost:8443");
        when(properties.getLoginRealm()).thenReturn("master");
        when(properties.getClientId()).thenReturn("config-cli-x509");

        when(resteasyClientSupplier.get()).thenReturn(resteasyClient);

        tokenManager = new RestClientX509TokenManager(properties, resteasyClientSupplier);
    }

    @Test
    void shouldFetchTokenSuccessfully() {
        String tokenEndpoint = "https://localhost:8443/realms/master/protocol/openid-connect/token";
        String tokenJson = "{\"access_token\":\"test-token\",\"expires_in\":60,\"refresh_expires_in\":0,\"token_type\":\"Bearer\"}";

        when(resteasyClient.target(tokenEndpoint)).thenReturn(target);
        when(target.request()).thenReturn(builder);
        when(builder.post(any(Entity.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(200);
        when(response.readEntity(String.class)).thenReturn(tokenJson);

        AccessTokenResponse token = tokenManager.getAccessToken();

        assertThat(token, is(notNullValue()));
        assertThat(token.getToken(), is("test-token"));
        assertThat(token.getExpiresIn(), is(60L));
        assertThat(token.getTokenType(), is("Bearer"));

        verify(resteasyClient).target(tokenEndpoint);
        verify(builder).post(any(Entity.class));
        verify(resteasyClient).close();
    }

    @Test
    void shouldCacheToken() {
        String tokenEndpoint = "https://localhost:8443/realms/master/protocol/openid-connect/token";
        String tokenJson = "{\"access_token\":\"test-token\",\"expires_in\":3600,\"refresh_expires_in\":0,\"token_type\":\"Bearer\"}";

        when(resteasyClient.target(tokenEndpoint)).thenReturn(target);
        when(target.request()).thenReturn(builder);
        when(builder.post(any(Entity.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(200);
        when(response.readEntity(String.class)).thenReturn(tokenJson);

        AccessTokenResponse token1 = tokenManager.getAccessToken();
        AccessTokenResponse token2 = tokenManager.getAccessToken();

        assertThat(token1, is(sameInstance(token2)));
        verify(builder, times(1)).post(any(Entity.class));
    }

    @Test
    void shouldRefreshExpiredToken() throws Exception {
        String tokenEndpoint = "https://localhost:8443/realms/master/protocol/openid-connect/token";
        String tokenJson1 = "{\"access_token\":\"token1\",\"expires_in\":1,\"refresh_expires_in\":0,\"token_type\":\"Bearer\"}";
        String tokenJson2 = "{\"access_token\":\"token2\",\"expires_in\":3600,\"refresh_expires_in\":0,\"token_type\":\"Bearer\"}";

        when(resteasyClient.target(tokenEndpoint)).thenReturn(target);
        when(target.request()).thenReturn(builder);
        when(builder.post(any(Entity.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(200);
        when(response.readEntity(String.class)).thenReturn(tokenJson1, tokenJson2);

        AccessTokenResponse token1 = tokenManager.getAccessToken();
        Thread.sleep(2000); // Wait for token to expire
        AccessTokenResponse token2 = tokenManager.getAccessToken();

        assertThat(token1.getToken(), is("token1"));
        assertThat(token2.getToken(), is("token2"));
        verify(builder, times(2)).post(any(Entity.class));
    }

    @Test
    void shouldThrowExceptionOn401() {
        String tokenEndpoint = "https://localhost:8443/realms/master/protocol/openid-connect/token";

        when(resteasyClient.target(tokenEndpoint)).thenReturn(target);
        when(target.request()).thenReturn(builder);
        when(builder.post(any(Entity.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(401);
        when(response.readEntity(String.class)).thenReturn("{\"error\":\"invalid_client\"}");

        assertThrows(Exception.class, () -> tokenManager.getAccessToken());
        verify(resteasyClient).close();
    }

    @Test
    void shouldGetAccessTokenString() {
        String tokenEndpoint = "https://localhost:8443/realms/master/protocol/openid-connect/token";
        String tokenJson = "{\"access_token\":\"test-token-string\",\"expires_in\":60,\"refresh_expires_in\":0,\"token_type\":\"Bearer\"}";

        when(resteasyClient.target(tokenEndpoint)).thenReturn(target);
        when(target.request()).thenReturn(builder);
        when(builder.post(any(Entity.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(200);
        when(response.readEntity(String.class)).thenReturn(tokenJson);

        String tokenString = tokenManager.getAccessTokenString();

        assertThat(tokenString, is("test-token-string"));
    }
}
