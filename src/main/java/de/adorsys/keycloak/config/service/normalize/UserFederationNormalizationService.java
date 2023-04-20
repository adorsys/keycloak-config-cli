/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2023 adorsys GmbH & Co. KG @ https://adorsys.com
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
import org.keycloak.representations.idm.UserFederationMapperRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.adorsys.keycloak.config.service.normalize.RealmNormalizationService.getNonNull;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "NORMALIZE")
public class UserFederationNormalizationService {

    private static final Logger logger = LoggerFactory.getLogger(UserFederationNormalizationService.class);

    private final Javers unOrderedJavers;

    @Autowired
    public UserFederationNormalizationService(Javers unOrderedJavers) {
        this.unOrderedJavers = unOrderedJavers;
    }

    public List<UserFederationProviderRepresentation> normalizeProviders(List<UserFederationProviderRepresentation> exportedProviders,
                                                                         List<UserFederationProviderRepresentation> baselineProviders) {
        var exportedOrEmpty = getNonNull(exportedProviders);
        var baselineOrEmpty = getNonNull(baselineProviders);

        var exportedMap = exportedOrEmpty.stream()
                .collect(Collectors.toMap(UserFederationProviderRepresentation::getDisplayName, Function.identity()));
        var baselineMap = baselineOrEmpty.stream()
                .collect(Collectors.toMap(UserFederationProviderRepresentation::getDisplayName, Function.identity()));

        var normalizedProviders = new ArrayList<UserFederationProviderRepresentation>();
        for (var entry : baselineMap.entrySet()) {
            var displayName = entry.getKey();
            var exportedProvider = exportedMap.remove(displayName);

            if (exportedProvider == null) {
                logger.warn("Default realm UserFederationProvider '{}' was deleted in exported realm. "
                        + "It may be reintroduced during import!", displayName);
                continue;
            }

            var baselineProvider = entry.getValue();
            if (unOrderedJavers.compare(baselineProvider, exportedProvider).hasChanges()) {
                normalizedProviders.add(exportedProvider);
            }
        }
        normalizedProviders.addAll(exportedMap.values());
        for (var provider : normalizedProviders) {
            provider.setId(null);
            if (provider.getConfig() != null && provider.getConfig().isEmpty()) {
                provider.setConfig(null);
            }
        }
        return normalizedProviders.isEmpty() ? null : normalizedProviders;
    }

    public List<UserFederationMapperRepresentation> normalizeMappers(List<UserFederationMapperRepresentation> exportedMappers,
                                                                     List<UserFederationMapperRepresentation> baselineMappers) {
        var exportedOrEmpty = getNonNull(exportedMappers);
        var baselineOrEmpty = getNonNull(baselineMappers);

        var exportedMap = exportedOrEmpty.stream()
                .collect(Collectors.toMap(m -> new MapperKey(m.getName(), m.getFederationProviderDisplayName()), Function.identity()));
        var baselineMap = baselineOrEmpty.stream()
                .collect(Collectors.toMap(m -> new MapperKey(m.getName(), m.getFederationProviderDisplayName()), Function.identity()));

        var normalizedMappers = new ArrayList<UserFederationMapperRepresentation>();
        for (var entry : baselineMap.entrySet()) {
            var key = entry.getKey();
            var exportedMapper = exportedMap.remove(key);
            if (exportedMapper == null) {
                logger.warn("Default realm UserFederationMapper '{}' for federation '{}' was deleted in exported realm. "
                        + "It may be reintroduced during import!", key.getName(), key.getFederationDisplayName());
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

    private static class MapperKey {
        private final String name;
        private final String federationDisplayName;

        public MapperKey(String name, String federationDisplayName) {
            this.name = name;
            this.federationDisplayName = federationDisplayName;
        }

        public String getName() {
            return name;
        }

        public String getFederationDisplayName() {
            return federationDisplayName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserFederationNormalizationService.MapperKey mapperKey = (UserFederationNormalizationService.MapperKey) o;
            return Objects.equals(name, mapperKey.name) && Objects.equals(federationDisplayName, mapperKey.federationDisplayName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, federationDisplayName);
        }
    }

}
