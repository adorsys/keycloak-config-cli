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
import de.adorsys.keycloak.config.exception.KeycloakProviderException;
import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import de.adorsys.keycloak.config.util.ResteasyUtil;
import de.adorsys.keycloak.config.util.VersionUtil;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.BasicAuthentication;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.info.ProfileInfoRepresentation;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.representations.info.ProfileInfoRepresentation;
import org.keycloak.representations.info.ServerInfoRepresentation;
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
 * This class exists because we need to create a single keycloak instance or to
 * close the keycloak before using a new one
 * to avoid a deadlock.
 */
@Component
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class KeycloakProvider implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakProvider.class);

    private final KeycloakConfigProperties properties;
    private final Supplier<ResteasyClient> resteasyClientSupplier;

    private Keycloak keycloak;
    private ResteasyClient resteasyClient;

    private String version;
    private Boolean fgapV2Active;
    private Boolean fgapV2Active;

    @Autowired
    private KeycloakProvider(KeycloakConfigProperties properties) {
        this.properties = properties;
        this.resteasyClientSupplier = () -> ResteasyUtil.getClient(
                !this.properties.isSslVerify(),
                this.properties.getHttpProxy(),
                this.properties.getConnectTimeout(),
                this.properties.getReadTimeout());
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
            if (properties.isSkipServerInfo()) {
                // Skip server info - use explicit version or default
                version = (properties.getVersion() != null && !properties.getVersion().isEmpty()
                        && !properties.getVersion().equals("@keycloak.version@"))
                                ? properties.getVersion()
                                : "unknown";
                logger.info("Server info unavailable. Using version: {}", version);
            } else {
                try {
                    ServerInfoRepresentation info = getInstance().serverInfo().getInfo();
                    if (info != null && info.getSystemInfo() != null) {
                        version = info.getSystemInfo().getVersion();
                        logger.info("Server info available. Using version: {}", version);
                    } else {
                        // Fallback if systemInfo is null
                        version = (properties.getVersion() != null && !properties.getVersion().isEmpty()
                                && !properties.getVersion().equals("@keycloak.version@"))
                                        ? properties.getVersion()
                                        : "unknown";
                        logger.info("Server info unavailable. Using version: {}", version);
                    }
                } catch (WebApplicationException e) {
                    if (e.getResponse().getStatus() == 401 || e.getResponse().getStatus() == 403) {
                        // Fallback on 401 Unauthorized or 403 Forbidden (typical for non-master realm
                        // access)
                        version = (properties.getVersion() != null && !properties.getVersion().isEmpty()
                                && !properties.getVersion().equals("@keycloak.version@"))
                                        ? properties.getVersion()
                                        : "unknown";
                        logger.info("Server info unavailable ({}). Using version: {}", e.getResponse().getStatus(),
                                version);
                    } else {
                        throw e;
                    }
                }
            }
        }

        return version;
    }

    /**
     * Definitive detection for Keycloak FGAP V2 (admin-fine-grained-authz:v2).
     * Uses server info profile features to determine whether FGAP V2 is active.
     * Result is cached in {@link #fgapV2Active}.
     * Returns false if detection fails.
     */
    public boolean isFgapV2Active() {
        if (fgapV2Active != null) {
            return fgapV2Active;
        }

        try {
            ServerInfoRepresentation info = getInstance().serverInfo().getInfo();
            if (info == null) {
                fgapV2Active = false;
                logger.debug("Profile info not available from Keycloak serverInfo()");
                return fgapV2Active;
            }

            // Check Keycloak version - FGAP V2 only exists in 26.2+
            if (info.getSystemInfo() == null) {
                fgapV2Active = false;
                logger.debug("SystemInfo not available from Keycloak serverInfo()");
                return fgapV2Active;
            }
            String keycloakVersion = info.getSystemInfo().getVersion();
            if (!VersionUtil.ge(keycloakVersion, "26.2")) {
                fgapV2Active = false;
                logger.debug("Keycloak version {} is before 26.2 => FGAP V2 not available", keycloakVersion);
                return fgapV2Active;
            }

            ProfileInfoRepresentation profile = info.getProfileInfo();
            if (profile == null) {
                fgapV2Active = false;
                logger.debug("ProfileInfoRepresentation not available from serverInfo()");
                return fgapV2Active;
            }

            java.util.List<String> disabled = profile.getDisabledFeatures();
            java.util.List<String> preview = profile.getPreviewFeatures();
            java.util.List<String> experimental = profile.getExperimentalFeatures();

            // If v2 is explicitly disabled, it's not active
            if (disabled != null && disabled.contains("admin-fine-grained-authz:v2")) {
                fgapV2Active = false;
                logger.debug("Detected admin-fine-grained-authz:v2 in disabled features => FGAP V2 not active");
                return fgapV2Active;
            }

            // If v1 is present in preview/experimental, v1 is still in use -> V2 not active
            if ((preview != null && preview.contains("admin-fine-grained-authz:v1"))
                    || (experimental != null && experimental.contains("admin-fine-grained-authz:v1"))) {
                fgapV2Active = false;
                logger.debug("Detected admin-fine-grained-authz:v1 in preview/experimental features => FGAP V2 not active");
                return fgapV2Active;
            }

            // Otherwise, assume V2 is active (Keycloak 26.2+ defaults to V2)
            fgapV2Active = true;
            logger.debug("Keycloak version {} with no V1/disabled V2 detected - FGAP V2 is active", keycloakVersion);
            return fgapV2Active;
        } catch (Exception e) {
            logger.warn("Unable to detect FGAP V2 from server info: {}", e.getMessage());
            return false;
        }
    }

    public void refreshToken() {
        getInstance().tokenManager().refreshToken();
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
                .onRetry(e -> logger.debug("Attempt failure #{}: {}", e.getAttemptCount(),
                        e.getLastException().getMessage()))
                .build();

        logger.info("Wait {} seconds until {} is available ...", timeout.getSeconds(), properties.getUrl());

        try {
            return Failsafe.with(retryPolicy).get(() -> {
                Keycloak obj = getKeycloak();

                if (properties.isSkipServerInfo()) {
                    // Use alternative health check when server info is skipped
                    isKeycloakAvailableAlternative(obj);
                } else {
                    try {
                        obj.serverInfo().getInfo();
                    } catch (WebApplicationException e) {
                        if (e.getResponse().getStatus() == 401 || e.getResponse().getStatus() == 403) {
                            // Fallback to alternative check if server info fails with 401/403
                            logger.warn("Server info check failed with {}, using alternative health check",
                                    e.getResponse().getStatus());
                            isKeycloakAvailableAlternative(obj);
                        } else {
                            throw e;
                        }
                    }
                }

                return obj;
            });
        } catch (Exception e) {
            String message = MessageFormat.format("Could not connect to keycloak in {0} seconds: {1}",
                    timeout.getSeconds(), e.getMessage());
            throw new KeycloakProviderException(message);
        }
    }

    private Keycloak getKeycloak() {
        Keycloak keycloakInstance = getKeycloakInstance(properties.getUrl());
        keycloakInstance.tokenManager().getAccessToken();

        return keycloakInstance;
    }

    private Keycloak getKeycloakInstance(String serverUrl) {
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

    private void checkServerVersion() {
        String keycloakVersion = getKeycloakVersion();

        if (properties.getVersion().equals("@keycloak.version@")) {
            return;
        }

        // Skip version check if server info is unavailable and using default version
        if (keycloakVersion.equals("unknown") && properties.isSkipServerInfo()) {
            logger.info("Server version check skipped (server info unavailable)");
            return;
        }

        String kccKeycloakMajorVersion = properties.getVersion().split("\\.")[0];

        if (!keycloakVersion.startsWith(kccKeycloakMajorVersion)) {
            logger.warn(
                    "Local keycloak-config-cli ({}-{}) and remote Keycloak ({}) may not compatible.",
                    getClass().getPackage().getImplementationVersion(),
                    properties.getVersion(),
                    keycloakVersion);
        }
    }

    /**
     * Alternative health check for Keycloak availability when server info is not
     * accessible.
     * Attempts to fetch the login realm configuration as a lightweight check.
     *
     * @param keycloak the Keycloak instance to check
     * @throws Exception if the realm cannot be accessed (Keycloak unavailable)
     */
    private void isKeycloakAvailableAlternative(Keycloak keycloak) {
        // Attempt to fetch current realm configuration as health check
        keycloak.realm(properties.getLoginRealm()).toRepresentation();
    }

    @Override
    public void close() {
        if (!isClosed()) {
            logout();
            keycloak.close();
        }
    }

    // see:
    // https://github.com/keycloak/keycloak/blob/8ea09d38168c22937363cf77a07f9de5dc7b48b0/services/src/main/java/org/keycloak/protocol/oidc/endpoints/LogoutEndpoint.java#L207-L220

    /**
     * Logout a session via a non-browser invocation. Similar signature to refresh
     * token except there is no grant_type.
     * You must pass in the refresh token and
     * authenticate the client if it is not public.
     * <p>
     * If the client is a confidential client
     * you must include the client-id and secret in a Basic Auth Authorization
     * header.
     * <p>
     * If the client is a public client, then you must include a "client_id" form
     * parameter.
     * <p>
     * returns 204 if successful, 400 if not with a json error response.
     */
    private void logout() {
        String refreshToken = this.keycloak.tokenManager().getAccessToken().getRefreshToken();
        // if we do not have a refreshToken, we are not able ot logout
        // (grant_type=client_credentials)
        if (refreshToken == null) {
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
     * Similar to
     * https://github.com/keycloak/keycloak-client/blob/
     * 0ca751f23022c9295f2e7dc9fa72725e4380f4ed/admin-client/src/main/java/org/
     * keycloak/admin/client/JacksonProvider.java
     * but without
     * objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL).
     * JsonInclude.Include.NON_NULL will cause errors with some unit tests
     * in ImportClientsIT.
     */
    public static class JacksonProvider extends ResteasyJackson2Provider {

        public ObjectMapper locateMapper(Class<?> type, MediaType mediaType) {
            ObjectMapper objectMapper = super.locateMapper(type, mediaType);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return objectMapper;
        }
    }
}
