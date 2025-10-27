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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.keycloak.config.configuration.RestClientX509Config;
import de.adorsys.keycloak.config.exception.KeycloakProviderException;
import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import de.adorsys.keycloak.config.token.RestClientX509TokenManager;
import de.adorsys.keycloak.config.util.ResteasyUtil;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.BasicAuthentication;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.function.Supplier;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * This class exists because we need to create a single keycloak instance or to close the keycloak before using a new one
 * to avoid a deadlock.
 */
@Component
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class KeycloakProvider implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakProvider.class);

    private final KeycloakConfigProperties properties;
    private final RestClientX509Config restClientX509Config;
    private final RestClientX509TokenManager customTokenManager;
    private final Supplier<ResteasyClient> resteasyClientSupplier;

    private Keycloak keycloak;
    private ResteasyClient resteasyClient;

    private String version;

    @Autowired
    private KeycloakProvider(KeycloakConfigProperties properties, RestClientX509Config restClientX509Config) {
        this.properties = properties;
        this.restClientX509Config = restClientX509Config;
        this.resteasyClientSupplier = () -> {
            try {
                return ResteasyUtil.getClient(
                        !this.properties.isSslVerify(),
                        this.properties.getHttpProxy(),
                        this.properties.getConnectTimeout(),
                        this.properties.getReadTimeout(),
                        this.restClientX509Config
                );
            } catch (Exception e) {
                throw new KeycloakProviderException(e);
            }
        };
        this.customTokenManager = new RestClientX509TokenManager(properties, resteasyClientSupplier);
    }

    public Keycloak getInstance() {
        if (keycloak == null || resteasyClient == null || keycloak.isClosed() || resteasyClient.isClosed()) {
            resteasyClient = resteasyClientSupplier.get();
            resteasyClient.register(JacksonProvider.class);
            keycloak = createKeycloak();

            checkServerVersion();
        }

        return keycloak;
    }

    public String getKeycloakVersion() {
        if (version == null) {
            version = getInstance().serverInfo().getInfo().getSystemInfo().getVersion();
        }

        return version;
    }

    public void refreshToken() {
        if (restClientX509Config.isX509Configured()) {
            customTokenManager.refreshToken();
        } else {
            getInstance().tokenManager().refreshToken();
        }
    }

    public <T> T getCustomApiProxy(Class<T> proxyClass) {
        try {
            URI uri = new URI(properties.getUrl());
            return getInstance().proxy(proxyClass, uri);
        } catch (URISyntaxException e) {
            throw new KeycloakProviderException(e);
        }
    }

    private Keycloak createKeycloak() {
        Keycloak result;
        if (properties.getAvailabilityCheck().isEnabled()) {
            result = getKeycloakWithRetry();
        } else {
            result = getKeycloak();
        }

        return result;
    }

    private Keycloak getKeycloakWithRetry() {
        Duration timeout = properties.getAvailabilityCheck().getTimeout();
        Duration retryDelay = properties.getAvailabilityCheck().getRetryDelay();

        RetryPolicy<Object> retryPolicy = RetryPolicy.builder()
                .withDelay(retryDelay)
                .withMaxDuration(timeout)
                .withMaxRetries(-1)
                .onRetry(e -> logger.debug("Attempt failure #{}: {}", e.getAttemptCount(), e.getLastException().getMessage()))
                .build();

        logger.info("Wait {} seconds until {} is available ...", timeout.getSeconds(), properties.getUrl());

        try {
            return Failsafe.with(retryPolicy).get(() -> {
                Keycloak obj = getKeycloak();
                obj.serverInfo().getInfo();
                return obj;
            });
        } catch (Exception e) {
            String message = MessageFormat.format("Could not connect to keycloak in {0} seconds: {1}", timeout.getSeconds(), e.getMessage());
            throw new KeycloakProviderException(message);
        }
    }

    private Keycloak getKeycloak() {
        Keycloak keycloakInstance = getKeycloakInstance(properties.getUrl());
        if (!restClientX509Config.isX509Configured()) {
            keycloakInstance.tokenManager().getAccessToken();
        }

        return keycloakInstance;
    }

    private Keycloak getKeycloakInstance(String serverUrl) {
        if (restClientX509Config.isX509Configured()) {
            return getKeycloakInstanceWithX509(serverUrl);
        } else {
            return KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(properties.getLoginRealm())
                    .clientId(properties.getClientId())
                    .grantType(properties.getGrantType())
                    .clientSecret(properties.getClientSecret())
                    .username(properties.getUser())
                    .password(properties.getPassword())
                    .resteasyClient(resteasyClient)
                    .build();
        }
    }

    private Keycloak getKeycloakInstanceWithX509(String serverUrl) {
        try {
            String accessToken = customTokenManager.getAccessTokenString();
            logger.debug("Building Keycloak client with X.509 authentication for server: {}", serverUrl);
            return KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(properties.getLoginRealm())
                    .clientId(properties.getClientId())
                    .authorization("Bearer " + accessToken)
                    .resteasyClient(resteasyClient)
                    .build();
        } catch (Exception e) {
            logger.error("Failed to build Keycloak client with X.509 authentication", e);
            throw new KeycloakProviderException("Could not build Keycloak client with X.509 authentication: " + e.getMessage(), e);
        }
    }

    private void checkServerVersion() {
        if (properties.getVersion().equals("@keycloak.version@")) return;

        String kccKeycloakMajorVersion = properties.getVersion().split("\\.")[0];

        if (!getKeycloakVersion().startsWith(kccKeycloakMajorVersion)) {
            logger.warn(
                    "Local keycloak-config-cli ({}-{}) and remote Keycloak ({}) may not compatible.",
                    getClass().getPackage().getImplementationVersion(),
                    properties.getVersion(),
                    getKeycloakVersion()
            );
        }
    }

    @Override
    public void close() {
        if (!isClosed()) {
            logout();
            keycloak.close();
        }
    }

    // see: https://github.com/keycloak/keycloak/blob/8ea09d38168c22937363cf77a07f9de5dc7b48b0/services/src/main/java/org/keycloak/protocol/oidc/endpoints/LogoutEndpoint.java#L207-L220

    /**
     * Logout a session via a non-browser invocation.  Similar signature to refresh token except there is no grant_type.
     * You must pass in the refresh token and
     * authenticate the client if it is not public.
     * <p>
     * If the client is a confidential client
     * you must include the client-id and secret in a Basic Auth Authorization header.
     * <p>
     * If the client is a public client, then you must include a "client_id" form parameter.
     * <p>
     * returns 204 if successful, 400 if not with a json error response.
     */
    private void logout() {

        String refreshToken = this.keycloak.tokenManager().getAccessToken().getRefreshToken();
        // if we do not have a refreshToken, we are not able ot logout (grant_type=client_credentials)
        // or authenticating with a x509 certificate does not use a refresh token
        if (refreshToken == null || restClientX509Config.isX509Configured()) {
            return;
        }

        ResteasyWebTarget resteasyWebTarget = resteasyClient
                .target(properties.getUrl())
                .path("/realms/" + properties.getLoginRealm() + "/protocol/openid-connect/logout");

        Form form = new Form();
        form.param("refresh_token", refreshToken);

        if (!properties.getClientId().isEmpty() && properties.getClientSecret().isEmpty()) {
            form.param("client_id", properties.getClientId());
        }

        if (!properties.getClientId().isEmpty() && !properties.getClientSecret().isEmpty()) {
            resteasyWebTarget.register(new BasicAuthentication(properties.getClientId(), properties.getClientSecret()));
        }

        Response response = resteasyWebTarget.request().post(Entity.form(form));
        // if debugging is enabled, care about error on logout.
        if (!response.getStatusInfo().equals(Response.Status.NO_CONTENT)) {
            logger.warn("Unable to logout. HTTP Status: {}", response.getStatus());
            if (logger.isDebugEnabled()) {
                throw new WebApplicationException(response);
            }
        }
    }

    public boolean isClosed() {
        return keycloak == null || keycloak.isClosed();
    }

    /*
    Similar to
    https://github.com/keycloak/keycloak-client/blob/0ca751f23022c9295f2e7dc9fa72725e4380f4ed/admin-client/src/main/java/org/keycloak/admin/client/JacksonProvider.java
    but without objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL).
    JsonInclude.Include.NON_NULL will cause errors with some unit tests
    in ImportClientsIT.
     */
    public static class JacksonProvider extends ResteasyJackson2Provider {

        public ObjectMapper locateMapper(Class<?> type, MediaType mediaType) {
            ObjectMapper objectMapper = super.locateMapper(type, mediaType);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return objectMapper;
        }
    }
}
