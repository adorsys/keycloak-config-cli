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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.keycloak.config.exception.KeycloakProviderException;
import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.keycloak.common.util.Time;
import org.keycloak.representations.AccessTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;

public class RestClientX509TokenManager {

    private static final long DEFAULT_MIN_VALIDITY = 30;
    private static final String GRANT_TYPE = "client_credentials";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final long MIN_TOKEN_VALIDITY = DEFAULT_MIN_VALIDITY;

    private final Supplier<ResteasyClient> resteasyClientSupplier;
    private final KeycloakConfigProperties properties;
    private AccessTokenResponse currentToken;
    private long expirationTime;

    private static final Logger logger = LoggerFactory.getLogger(RestClientX509TokenManager.class);

    public RestClientX509TokenManager(KeycloakConfigProperties keycloakConfigProperties, Supplier<ResteasyClient> resteasyClient) {
        this.resteasyClientSupplier = resteasyClient;
        this.properties = keycloakConfigProperties;
    }

    public String getAccessTokenString() {
        return getAccessToken().getToken();
    }

    public synchronized AccessTokenResponse getAccessToken() {
        if (currentToken == null || tokenExpired()) {
            currentToken = fetchNewToken();
        }
        return currentToken;
    }

    public synchronized void refreshToken() {
        currentToken = fetchNewToken();
    }

    private boolean tokenExpired() {
        return currentToken == null || (Time.currentTime() + MIN_TOKEN_VALIDITY) >= expirationTime;
    }

    private AccessTokenResponse fetchNewToken() {
        String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token",
                properties.getUrl(), properties.getLoginRealm());

        ResteasyClient resteasyClient = resteasyClientSupplier.get();
        try {
            ResteasyWebTarget target = resteasyClient.target(tokenEndpoint);

            Form form = new Form();
            form.param("grant_type", GRANT_TYPE);
            form.param("client_id", properties.getClientId());

            Response response = target.request().post(Entity.form(form));
            int requestTime = Time.currentTime();

            try {
                if (response.getStatus() != 200) {
                    String errorBody = response.readEntity(String.class);
                    logger.error("Failed to obtain token. Status: {}, Body: {}", response.getStatus(), errorBody);
                    throw new KeycloakProviderException(
                        String.format("Failed to obtain token from Keycloak. HTTPS %d: %s",
                            response.getStatus(), errorBody)
                    );
                }

                String responseBody = response.readEntity(String.class);
                AccessTokenResponse tokenResponse = parseTokenResponse(responseBody);
                expirationTime = requestTime + tokenResponse.getExpiresIn();

                logger.debug("Successfully obtained access token. Expires in {} seconds", tokenResponse.getExpiresIn());
                return tokenResponse;
            } finally {
                response.close();
            }
        } catch (KeycloakProviderException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error obtaining token from Keycloak", e);
            throw new KeycloakProviderException("Failed to obtain token from Keycloak server: " + e.getMessage(), e);
        } finally {
            resteasyClient.close();
        }
    }

    private AccessTokenResponse parseTokenResponse(String responseBody) {
        try {
            return OBJECT_MAPPER.readValue(responseBody, AccessTokenResponse.class);
        } catch (Exception e) {
            logger.error("Failed to parse token response: {}", responseBody, e);
            throw new KeycloakProviderException("Failed to parse token response from Keycloak", e);
        }
    }
}
