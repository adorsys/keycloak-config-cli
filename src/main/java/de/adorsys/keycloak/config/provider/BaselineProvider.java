/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2022 adorsys GmbH & Co. KG @ https://adorsys.com
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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.keycloak.config.exception.NormalizationException;
import de.adorsys.keycloak.config.properties.NormalizationConfigProperties;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "NORMALIZE")
public class BaselineProvider {

    private static final Logger logger = LoggerFactory.getLogger(BaselineProvider.class);
    private static final String PLACEHOLDER = "REALM_NAME_PLACEHOLDER";

    private final ObjectMapper objectMapper;

    private final String fallbackVersion;

    @Autowired
    public BaselineProvider(ObjectMapper objectMapper, NormalizationConfigProperties normalizationConfigProperties) {
        this.objectMapper = objectMapper;
        this.fallbackVersion = normalizationConfigProperties.getFallbackVersion();
    }

    public RealmRepresentation getRealm(String version, String realmName) {
        try (var inputStream = getRealmInputStream(version)) {
            /*
             * Replace the placeholder with the realm name to import. This sets some internal values like role names,
             * baseUrls and redirectUrls so that they don't get picked up as "changes"
             */
            var realmString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).replace(PLACEHOLDER, realmName);
            return objectMapper.readValue(realmString, RealmRepresentation.class);
        } catch (IOException ex) {
            throw new NormalizationException(String.format("Failed to load baseline realm for version %s", version), ex);
        }
    }

    public ClientRepresentation getClient(String version, String clientId) {
        try (var is = getClientInputStream(version)) {
            var client = objectMapper.readValue(is, ClientRepresentation.class);
            client.setClientId(clientId);
            return client;
        } catch (IOException ex) {
            throw new NormalizationException(String.format("Failed to load baseline client for version %s", version), ex);
        }
    }

    public InputStream getRealmInputStream(String version) {
        var inputStream = getClass().getResourceAsStream(String.format("/baseline/%s/realm/realm.json", version));
        if (inputStream == null) {
            if (fallbackVersion != null) {
                logger.warn("Reference realm not found for version {}. Using fallback version {}!", version, fallbackVersion);
                inputStream = getClass().getResourceAsStream(String.format("/baseline/%s/realm/realm.json", fallbackVersion));
                if (inputStream == null) {
                    throw new NormalizationException(String.format("Reference realm for version %s does not exist, "
                            + "and fallback version %s does not exist either. Aborting!", version, fallbackVersion));
                }
            } else {
                throw new NormalizationException(String.format("Reference realm for version %s does not exist. Aborting!", version));
            }
        }
        return inputStream;
    }

    public InputStream getClientInputStream(String version) {
        var inputStream = getClass().getResourceAsStream(String.format("/baseline/%s/client/client.json", version));
        if (inputStream == null) {
            if (fallbackVersion != null) {
                logger.debug("Reference client not found for version {}. Using fallback version {}!", version, fallbackVersion);
                inputStream = getClass().getResourceAsStream(String.format("/baseline/%s/client/client.json", fallbackVersion));
                if (inputStream == null) {
                    throw new NormalizationException(String.format("Reference client for version %s does not exist, "
                            + "and fallback version %s does not exist either. Aborting!", version, fallbackVersion));
                }
            } else {
                throw new NormalizationException(String.format("Reference client for version %s does not exist. Aborting!", version));
            }
        }
        return inputStream;
    }
}
