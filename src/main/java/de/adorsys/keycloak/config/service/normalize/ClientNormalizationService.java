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
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
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
import java.util.Set;
import java.util.stream.Collectors;

import static de.adorsys.keycloak.config.service.normalize.RealmNormalizationService.getNonNull;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "NORMALIZE")
public class ClientNormalizationService {

    private static final Set<String> SAML_ATTRIBUTES = Set.of("saml_signature_canonicalization_method", "saml.onetimeuse.condition",
            "saml_name_id_format", "saml.authnstatement", "saml.server.signature.keyinfo$xmlSigKeyInfoKeyNameTransformer",
            "saml_force_name_id_format", "saml.artifact.binding", "saml.artifact.binding.identifier", "saml.server.signature", "saml.encrypt",
            "saml.assertion.signature", "saml.allow.ecp.flow", "saml.signing.private.key", "saml.force.name.id.format", "saml.client.signature",
            "saml.signature.algorithm", "saml.signing.certificate", "saml.server.signature.keyinfo.ext", "saml.multivalued.roles",
            "saml.force.post.binding");

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
                clients.add(normalizeClient(exportedClient, exportedRealm.getKeycloakVersion(), exportedRealm));
            }
        }

        // Now iterate over all the clients that are *not* default clients
        for (Map.Entry<String, ClientRepresentation> e : exportedClientMap.entrySet()) {
            if (!baselineClientMap.containsKey(e.getKey())) {
                clients.add(normalizeClient(e.getValue(), exportedRealm.getKeycloakVersion(), exportedRealm));
            }
        }
        return clients;
    }

    public ClientRepresentation normalizeClient(ClientRepresentation client, String keycloakVersion, RealmRepresentation exportedRealm) {
        var clientId = client.getClientId();
        var baselineClient = baselineProvider.getClient(keycloakVersion, clientId);
        var diff = unOrderedJavers.compare(baselineClient, client);
        var normalizedClient = new ClientRepresentation();
        for (var change : diff.getChangesByType(PropertyChange.class)) {
            javersUtil.applyChange(normalizedClient, change);
        }

        // Always include protocol, even if it's the default "openid-connect"
        normalizedClient.setProtocol(client.getProtocol());
        var mappers = client.getProtocolMappers();
        normalizedClient.setProtocolMappers(mappers);
        if (mappers != null) {
            for (var mapper : mappers) {
                mapper.setId(null);
            }
        }
        normalizedClient.setAuthorizationSettings(client.getAuthorizationSettings());
        normalizedClient.setClientId(clientId);

        // Older versions of keycloak include SAML attributes even in OIDC clients. Ignore these.
        if (normalizedClient.getProtocol().equals("openid-connect") && normalizedClient.getAttributes() != null) {
            normalizedClient.getAttributes().keySet().removeIf(SAML_ATTRIBUTES::contains);
        }

        if (normalizedClient.getAuthenticationFlowBindingOverrides() != null) {
            var overrides = new HashMap<String, String>();
            var flows = exportedRealm.getAuthenticationFlows().stream()
                    .collect(Collectors.toMap(AuthenticationFlowRepresentation::getId, AuthenticationFlowRepresentation::getAlias));
            for (var entry : normalizedClient.getAuthenticationFlowBindingOverrides().entrySet()) {
                var id = entry.getValue();
                overrides.put(entry.getKey(), flows.get(id));
            }
            normalizedClient.setAuthenticationFlowBindingOverrides(overrides);
        }
        normalizedClient.setPublicClient(client.isPublicClient());
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
        return unOrderedJavers.compareCollections(getNonNull(baselineMappers), getNonNull(exportedMappers), ProtocolMapperRepresentation.class)
                .hasChanges();
    }

    public boolean authorizationSettingsChanged(ResourceServerRepresentation exportedSettings, ResourceServerRepresentation baselineSettings) {
        return unOrderedJavers.compare(baselineSettings, exportedSettings).hasChanges();
    }

}
