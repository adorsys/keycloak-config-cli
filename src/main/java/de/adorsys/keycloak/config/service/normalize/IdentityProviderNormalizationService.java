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
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
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
public class IdentityProviderNormalizationService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityProviderNormalizationService.class);

    private final Javers unOrderedJavers;
    private final JaversUtil javersUtil;

    public IdentityProviderNormalizationService(Javers unOrderedJavers, JaversUtil javersUtil) {
        this.unOrderedJavers = unOrderedJavers;
        this.javersUtil = javersUtil;
    }

    public List<IdentityProviderRepresentation> normalizeProviders(List<IdentityProviderRepresentation> exportedProviders,
                                                                   List<IdentityProviderRepresentation> baselineProviders) {
        List<IdentityProviderRepresentation> exportedOrEmpty = exportedProviders == null ? List.of() : exportedProviders;
        List<IdentityProviderRepresentation> baselineOrEmpty = baselineProviders == null ? List.of() : baselineProviders;

        var exportedMap = exportedOrEmpty.stream()
                .collect(Collectors.toMap(IdentityProviderRepresentation::getAlias, Function.identity()));
        var baselineMap = baselineOrEmpty.stream()
                .collect(Collectors.toMap(IdentityProviderRepresentation::getAlias, Function.identity()));

        var normalizedProviders = new ArrayList<IdentityProviderRepresentation>();
        for (var entry : baselineMap.entrySet()) {
            var alias = entry.getKey();
            var exportedProvider = exportedMap.remove(alias);
            if (exportedProvider == null) {
                logger.warn("Default realm identityProvider '{}' was deleted in exported realm. It may be reintroduced during import!", alias);
                continue;
            }
            var baselineProvider = entry.getValue();

            var diff = unOrderedJavers.compare(baselineProvider, exportedProvider);
            if (diff.hasChanges()) {
                var normalizedProvider = new IdentityProviderRepresentation();
                normalizedProvider.setAlias(exportedProvider.getAlias());
                // Need to set manually because it's default true on the class, but it might not be on the baseline
                normalizedProvider.setEnabled(exportedProvider.isEnabled());
                for (var change : diff.getChangesByType(PropertyChange.class)) {
                    javersUtil.applyChange(normalizedProvider, change);
                }
                normalizedProviders.add(normalizedProvider);
            }
        }
        normalizedProviders.addAll(exportedMap.values());
        return normalizedProviders.isEmpty() ? null : normalizedProviders;
    }

    public List<IdentityProviderMapperRepresentation> normalizeMappers(List<IdentityProviderMapperRepresentation> exportedMappers,
                                                                       List<IdentityProviderMapperRepresentation> baselineMappers) {
        List<IdentityProviderMapperRepresentation> exportedOrEmpty = exportedMappers == null ? List.of() : exportedMappers;
        List<IdentityProviderMapperRepresentation> baselineOrEmpty = baselineMappers == null ? List.of() : baselineMappers;

        var exportedMap = exportedOrEmpty.stream()
                .collect(Collectors.toMap(IdentityProviderMapperRepresentation::getName, Function.identity()));
        var baselineMap = baselineOrEmpty.stream()
                .collect(Collectors.toMap(IdentityProviderMapperRepresentation::getName, Function.identity()));

        var normalizedMappers = new ArrayList<IdentityProviderMapperRepresentation>();
        for (var entry : baselineMap.entrySet()) {
            var name = entry.getKey();
            var exportedMapper = exportedMap.remove(name);
            if (exportedMapper == null) {
                logger.warn("Default realm identityProviderMapper '{}' was deleted in exported realm. It may be reintroduced during import!", name);
                continue;
            }
            var baselineMapper = entry.getValue();
            var normalizedMapper = new IdentityProviderMapperRepresentation();
            normalizedMapper.setName(name);

            var diff = unOrderedJavers.compare(baselineMapper, exportedMapper);
            if (diff.hasChanges()) {
                for (var change : diff.getChangesByType(PropertyChange.class)) {
                    javersUtil.applyChange(normalizedMapper, change);
                }
            }
            normalizedMappers.add(normalizedMapper);
        }
        normalizedMappers.addAll(exportedMap.values());
        return normalizedMappers.isEmpty() ? null : normalizedMappers;
    }
}
