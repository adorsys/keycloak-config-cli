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

import de.adorsys.keycloak.config.util.JaversUtil;
import org.javers.core.Javers;
import org.javers.core.diff.changetype.PropertyChange;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "NORMALIZE")
public class ClientScopeNormalizationService {

    private static final Logger logger = LoggerFactory.getLogger(ClientScopeNormalizationService.class);

    private final Javers unOrderedJavers;
    private final JaversUtil javersUtil;

    public ClientScopeNormalizationService(Javers unOrderedJavers, JaversUtil javersUtil) {
        this.unOrderedJavers = unOrderedJavers;
        this.javersUtil = javersUtil;
    }

    public List<ClientScopeRepresentation> normalizeClientScopes(List<ClientScopeRepresentation> exportedScopes,
                                                                 List<ClientScopeRepresentation> baselineScopes) {
        List<ClientScopeRepresentation> exportedOrEmpty = exportedScopes == null ? List.of() : exportedScopes;
        List<ClientScopeRepresentation> baselineOrEmpty = baselineScopes == null ? List.of() : baselineScopes;

        var exportedMap = exportedOrEmpty.stream().collect(Collectors.toMap(ClientScopeRepresentation::getName,
                Function.identity()));
        var baselineMap = baselineOrEmpty.stream().collect(Collectors.toMap(ClientScopeRepresentation::getName,
                Function.identity()));

        var normalizedScopes = new ArrayList<ClientScopeRepresentation>();
        for (var entry : baselineMap.entrySet()) {
            var scopeName = entry.getKey();
            var baselineScope = entry.getValue();
            var exportedScope = exportedMap.remove(scopeName);

            if (exportedScope == null) {
                logger.warn("Default realm clientScope '{}' was deleted in exported realm. It may be reintroduced during import!", scopeName);
                continue;
            }

            if (clientScopeChanged(exportedScope, baselineScope)) {
                var normalizedScope = new ClientScopeRepresentation();
                var diff = unOrderedJavers.compare(baselineScope, exportedScope);
                normalizedScope.setName(exportedScope.getName());
                // set protocol
                for (var change : diff.getChangesByType(PropertyChange.class)) {
                    javersUtil.applyChange(normalizedScope, change);
                }
                var mappers = exportedScope.getProtocolMappers();
                normalizedScope.setProtocolMappers(mappers);
                if (mappers != null) {
                    for (var mapper : mappers) {
                        mapper.setId(null);
                    }
                }
                normalizedScopes.add(normalizedScope);
            }
        }

        for (var scope : exportedMap.values()) {
            scope.setId(null);
            normalizedScopes.add(scope);
        }
        return normalizedScopes.isEmpty() ? null : normalizedScopes;
    }

    public boolean clientScopeChanged(ClientScopeRepresentation exportedScope, ClientScopeRepresentation baselineScope) {
        if (unOrderedJavers.compare(baselineScope, exportedScope).hasChanges()) {
            return true;
        }

        return protocolMappersChanged(exportedScope.getProtocolMappers(), baselineScope.getProtocolMappers());
    }

    public boolean protocolMappersChanged(List<ProtocolMapperRepresentation> exportedMappers, List<ProtocolMapperRepresentation> baselineMappers) {
        return unOrderedJavers.compareCollections(baselineMappers == null ? List.of() : baselineMappers,
                exportedMappers == null ? List.of() : exportedMappers, ProtocolMapperRepresentation.class).hasChanges();
    }
}
