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

import org.javers.core.Javers;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.adorsys.keycloak.config.service.normalize.RealmNormalizationService.getNonNull;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "NORMALIZE")
public class ProtocolMapperNormalizationService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityProviderNormalizationService.class);

    private final Javers unOrderedJavers;

    public ProtocolMapperNormalizationService(Javers unOrderedJavers) {
        this.unOrderedJavers = unOrderedJavers;
    }

    public List<ProtocolMapperRepresentation> normalizeProtocolMappers(List<ProtocolMapperRepresentation> exportedMappers,
                                                                       List<ProtocolMapperRepresentation> baselineMappers) {
        var exportedOrEmpty = getNonNull(exportedMappers);
        var baselineOrEmpty = getNonNull(baselineMappers);

        var exportedMap = exportedOrEmpty.stream()
                .collect(Collectors.toMap(ProtocolMapperRepresentation::getName, Function.identity()));
        var baselineMap = baselineOrEmpty.stream()
                .collect(Collectors.toMap(ProtocolMapperRepresentation::getName, Function.identity()));
        var normalizedMappers = new ArrayList<ProtocolMapperRepresentation>();

        for (var entry : baselineMap.entrySet()) {
            var name = entry.getKey();
            var exportedMapper = exportedMap.remove(name);
            if (exportedMapper == null) {
                logger.warn("Default realm protocolMapper '{}' was deleted in exported realm. It may be reintroduced during import!", name);
                continue;
            }

            var baselineMapper = entry.getValue();
            if (unOrderedJavers.compare(baselineMapper, exportedMapper).hasChanges()) {
                normalizedMappers.add(exportedMapper);
            }
        }
        normalizedMappers.addAll(exportedMap.values());
        for (var mapper : normalizedMappers) {
            mapper.setId(null);
            if (mapper.getConfig() != null && mapper.getConfig().isEmpty()) {
                mapper.setConfig(null);
            }
        }
        return normalizedMappers.isEmpty() ? null : normalizedMappers;
    }
}
