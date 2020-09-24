/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2020 adorsys GmbH & Co. KG @ https://adorsys.com
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

import de.adorsys.keycloak.config.exception.KeycloakProviderException;
import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.http.client.utils.URIBuilder;
import org.keycloak.admin.client.Keycloak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.time.Duration;

/**
 * This class exists cause we need to create a single keycloak instance or to close the keycloak before using a new one
 * to avoid a deadlock.
 */
@Component
public class KeycloakProvider {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakProvider.class);

    private final KeycloakConfigProperties properties;

    private Keycloak keycloak;
    private boolean isClosed = true;

    @Autowired
    public KeycloakProvider(KeycloakConfigProperties properties) {
        this.properties = properties;
    }

    public Keycloak get() {
        if (keycloak == null || isClosed) {
            keycloak = createKeycloak(properties);
            isClosed = false;
        }

        return keycloak;
    }

    public void close() {
        if (!isClosed && keycloak != null) {
            keycloak.close();
        }

        isClosed = true;
    }

    private Keycloak createKeycloak(
            KeycloakConfigProperties properties
    ) {
        Keycloak result;
        if (properties.getAvailabilityCheck().isEnabled()) {
            result = getKeycloakWithRetry();
        } else {
            result = getKeycloak();
        }

        return result;
    }

    private String buildUri(String baseUri) {
        try {
            return new URIBuilder(baseUri).setPath("/auth").build().toString();
        } catch (URISyntaxException e) {
            throw new KeycloakProviderException(e);
        }
    }

    private Keycloak getKeycloakWithRetry() {
        Duration timeout = properties.getAvailabilityCheck().getTimeout();
        Duration retryDelay = properties.getAvailabilityCheck().getRetryDelay();

        RetryPolicy<Keycloak> retryPolicy = new RetryPolicy<Keycloak>()
                .withDelay(retryDelay)
                .withMaxDuration(timeout)
                .withMaxRetries(-1)
                .onRetry(e -> logger.debug("Attempt failure #{}: {}", e.getAttemptCount(), e.getLastFailure().getMessage()));

        logger.info("Wait {} seconds until {} is available ...", timeout.getSeconds(), properties.getUrl());

        try {
            return Failsafe.with(retryPolicy).get(() -> {
                Keycloak obj = getKeycloak();
                obj.realm(properties.getLoginRealm()).toRepresentation();
                return obj;
            });
        } catch (Exception e) {
            String message = MessageFormat.format("Could not connect to keycloak in {0} seconds: {1}", timeout.getSeconds(), e.getMessage());
            throw new KeycloakProviderException(message);
        }
    }

    private Keycloak getKeycloak() {
        return Keycloak.getInstance(
                buildUri(properties.getUrl()),
                properties.getLoginRealm(),
                properties.getUser(),
                properties.getPassword(),
                properties.getClientId(),
                null,
                null,
                null,
                !properties.isSslVerify(),
                null
        );
    }
}
