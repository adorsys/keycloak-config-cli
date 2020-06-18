/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2020 adorsys GmbH & Co. KG @ https://adorsys.de
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

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import org.apache.http.client.utils.URIBuilder;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;

/**
 * This class exists cause we need to create a single keycloak instance or to close the keycloak before using a new one
 * to avoid a deadlock.
 */
@Component
public class KeycloakProvider {

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

    private String buildUri(String baseUri) {
        try {
            return new URIBuilder(baseUri)
                    .setPath("/auth")
                    .build()
                    .toString();
        } catch (URISyntaxException e) {
            throw new ImportProcessingException(e);
        }
    }
}
