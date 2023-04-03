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

package de.adorsys.keycloak.config.service.normalize;

import de.adorsys.keycloak.config.provider.BaselineProvider;
import de.adorsys.keycloak.config.util.JaversUtil;
import org.javers.core.Javers;
import org.javers.core.diff.changetype.PropertyChange;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.adorsys.keycloak.config.service.normalize.RealmNormalizationService.getNonNull;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "NORMALIZE")
public class ClientNormalizationService {

    private static final Logger logger = LoggerFactory.getLogger(ClientNormalizationService.class);
    private final Javers unOrderedJavers;
    private final BaselineProvider baselineProvider;
    private final JaversUtil javersUtil;

    public ClientNormalizationService(Javers unOrderedJavers,
                                      BaselineProvider baselineProvider,
                                      JaversUtil javersUtil) {
        this.unOrderedJavers = unOrderedJavers;
        this.baselineProvider = baselineProvider;
        this.javersUtil = javersUtil;
    }

    public List<ClientRepresentation> normalizeClients(RealmRepresentation exportedRealm, RealmRepresentation baselineRealm) {
        var exportedOrEmpty = getNonNull(exportedRealm.getClients());
        var baselineOrEmpty = getNonNull(baselineRealm.getClients());
        var exportedClientMap = new HashMap<String, ClientRepresentation>();
        for (var exportedClient : exportedOrEmpty) {
            exportedClientMap.put(exportedClient.getClientId(), exportedClient);
        }

        var baselineClientMap = new HashMap<String, ClientRepresentation>();
        var clients = new ArrayList<ClientRepresentation>();
        for (var baselineRealmClient : baselineOrEmpty) {
            var clientId = baselineRealmClient.getClientId();
            baselineClientMap.put(clientId, baselineRealmClient);
            var exportedClient = exportedClientMap.get(clientId);
            if (exportedClient == null) {
                logger.warn("Default realm client '{}' was deleted in exported realm. It may be reintroduced during import!", clientId);
                /*
                 * Here we need to define a configuration parameter: If we want the import *not* to reintroduce default clients that were
                 * deleted, we need to add *all* clients, not just default clients to the dump. Then during import, set the mode that
                 * makes clients fully managed, so that *only* clients that are in the dump end up in the realm
                 */
                continue;
            }
            if (clientChanged(exportedClient, baselineRealmClient)) {
                // We know the client has changed in some way. Now, compare it to a default client to minimize it
                clients.add(normalizeClient(exportedClient, exportedRealm.getKeycloakVersion()));
            }
        }

        // Now iterate over all the clients that are *not* default clients
        for (Map.Entry<String, ClientRepresentation> e : exportedClientMap.entrySet()) {
            if (!baselineClientMap.containsKey(e.getKey())) {
                clients.add(normalizeClient(e.getValue(), exportedRealm.getKeycloakVersion()));
            }
        }
        return clients;
    }

    public ClientRepresentation normalizeClient(ClientRepresentation client, String keycloakVersion) {
        var clientId = client.getClientId();
        var baselineClient = baselineProvider.getClient(keycloakVersion, clientId);
        var diff = unOrderedJavers.compare(baselineClient, client);
        var normalizedClient = new ClientRepresentation();
        for (var change : diff.getChangesByType(PropertyChange.class)) {
            javersUtil.applyChange(normalizedClient, change);
        }
        var mappers = client.getProtocolMappers();
        normalizedClient.setProtocolMappers(mappers);
        if (mappers != null) {
            for (var mapper : mappers) {
                mapper.setId(null);
            }
        }
        normalizedClient.setAuthorizationSettings(client.getAuthorizationSettings());
        normalizedClient.setClientId(clientId);
        return normalizedClient;
    }

    public boolean clientChanged(ClientRepresentation exportedClient, ClientRepresentation baselineClient) {
        var diff = unOrderedJavers.compare(baselineClient, exportedClient);
        if (diff.hasChanges()) {
            return true;
        }
        if (protocolMappersChanged(exportedClient.getProtocolMappers(), baselineClient.getProtocolMappers())) {
            return true;
        }
        return authorizationSettingsChanged(exportedClient.getAuthorizationSettings(), baselineClient.getAuthorizationSettings());
    }

    public boolean protocolMappersChanged(List<ProtocolMapperRepresentation> exportedMappers, List<ProtocolMapperRepresentation> baselineMappers) {
        // CompareCollections doesn't handle nulls gracefully
        return unOrderedJavers.compareCollections(getNonNull(baselineMappers), getNonNull(exportedMappers), ProtocolMapperRepresentation.class).hasChanges();
    }

    public boolean authorizationSettingsChanged(ResourceServerRepresentation exportedSettings, ResourceServerRepresentation baselineSettings) {
        return unOrderedJavers.compare(baselineSettings, exportedSettings).hasChanges();
    }

}
