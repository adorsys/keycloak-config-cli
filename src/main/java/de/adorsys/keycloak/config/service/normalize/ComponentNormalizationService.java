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
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentExportRepresentation;
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
public class ComponentNormalizationService {

    private static final Logger logger = LoggerFactory.getLogger(ComponentNormalizationService.class);

    private final Javers unOrderedJavers;

    public ComponentNormalizationService(Javers unOrderedJavers) {
        this.unOrderedJavers = unOrderedJavers;
    }

    public MultivaluedHashMap<String, ComponentExportRepresentation>
            normalizeComponents(MultivaluedHashMap<String, ComponentExportRepresentation> exportedComponents,
                                MultivaluedHashMap<String, ComponentExportRepresentation> baselineComponents) {
        var exportedOrEmpty = getNonNull(exportedComponents);
        var baselineOrEmpty = getNonNull(baselineComponents);

        var normalizedMap = new MultivaluedHashMap<String, ComponentExportRepresentation>();
        for (var entry : baselineOrEmpty.entrySet()) {
            var componentClass = entry.getKey();

            var exportedList = exportedOrEmpty.remove(componentClass);

            if (exportedList == null) {
                logger.warn("Default realm component '{}' was deleted in exported realm. It may be reintroduced during import!", componentClass);
                continue;
            }
            var baselineList = entry.getValue();
            var normalizedList = normalizeList(exportedList, baselineList, componentClass);
            normalizedMap.put(componentClass, normalizedList);
        }
        normalizedMap.putAll(exportedOrEmpty);
        //var toRemove = new HashSet<String>();
        for (var entry : normalizedMap.entrySet()) {
            var componentList = entry.getValue();
            for (var component : componentList) {
                normalizeEntry(component);
            }
        }
        return normalizedMap;
    }

    public List<ComponentExportRepresentation> normalizeList(List<ComponentExportRepresentation> exportedComponents,
                                                             List<ComponentExportRepresentation> baselineComponents,
                                                             String componentClass) {
        var exportedOrEmpty = getNonNull(exportedComponents);
        var baselineOrEmpty = getNonNull(baselineComponents);

        var exportedMap = exportedOrEmpty.stream()
                .collect(Collectors.toMap(ComponentExportRepresentation::getName, Function.identity()));
        var baselineMap = baselineOrEmpty.stream()
                .collect(Collectors.toMap(ComponentExportRepresentation::getName, Function.identity()));
        var normalizedComponents = new ArrayList<ComponentExportRepresentation>();

        for (var entry : baselineMap.entrySet()) {
            var name = entry.getKey();
            var exportedComponent = exportedMap.remove(name);
            if (exportedComponent == null) {
                logger.warn("Default realm component '{}' was deleted in exported realm. It may be reintroduced during import!", name);
                continue;
            }

            var baselineComponent = entry.getValue();
            if (unOrderedJavers.compare(baselineComponent, exportedComponent).hasChanges()) {
                normalizedComponents.add(exportedComponent);
            }
        }
        normalizedComponents.addAll(exportedMap.values());
        return normalizedComponents;
    }

    public void normalizeEntry(ComponentExportRepresentation component) {
        component.setId(null);
        if (component.getConfig() != null && component.getConfig().isEmpty()) {
            component.setConfig(null);
        }
    }
}
