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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "NORMALIZE")
@SuppressFBWarnings(value = {"NP_LOAD_OF_KNOWN_NULL_VALUE", "RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE"},
        justification = "Bug in Spotbugs, see https://github.com/spotbugs/spotbugs/issues/1338")
public class BaselineProvider {

    private static final String PLACEHOLDER = "REALM_NAME_PLACEHOLDER";

    private final ObjectMapper objectMapper;

    @Autowired
    public BaselineProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RealmRepresentation getRealm(String version, String realmName) {
        try (var inputStream = getClass()
                .getResourceAsStream(String.format("/baseline/%s/realm/realm.json", version))) {
            if (inputStream == null) {
                throw new NormalizationException(String.format("Reference realm for version %s does not exist", version));
            }
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
        try (var is = getClass()
                .getResourceAsStream(String.format("/baseline/%s/client/client.json", version))) {
            var client = objectMapper.readValue(is, ClientRepresentation.class);
            client.setClientId(clientId);
            return client;
        } catch (IOException ex) {
            throw new NormalizationException(String.format("Failed to load baseline client for version %s", version), ex);
        }
    }
}
