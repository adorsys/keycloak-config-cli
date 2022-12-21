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
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
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
public class RequiredActionNormalizationService {

    private static final Logger logger = LoggerFactory.getLogger(RequiredActionNormalizationService.class);

    private final Javers javers;
    private final JaversUtil javersUtil;

    public RequiredActionNormalizationService(Javers javers, JaversUtil javersUtil) {
        this.javers = javers;
        this.javersUtil = javersUtil;
    }

    public List<RequiredActionProviderRepresentation> normalizeRequiredActions(List<RequiredActionProviderRepresentation> exportedActions,
                                                                               List<RequiredActionProviderRepresentation> baselineActions) {
        List<RequiredActionProviderRepresentation> exportedOrEmpty = exportedActions == null ? List.of() : exportedActions;
        List<RequiredActionProviderRepresentation> baselineOrEmpty = baselineActions == null ? List.of() : baselineActions;

        var exportedMap = exportedOrEmpty.stream()
                .collect(Collectors.toMap(RequiredActionProviderRepresentation::getAlias, Function.identity()));
        var baselineMap = baselineOrEmpty.stream()
                .collect(Collectors.toMap(RequiredActionProviderRepresentation::getAlias, Function.identity()));

        var normalizedActions = new ArrayList<RequiredActionProviderRepresentation>();
        for (var entry : baselineMap.entrySet()) {
            var alias = entry.getKey();
            var exportedAction = exportedMap.remove(alias);
            if (exportedAction == null) {
                logger.warn("Default realm requiredAction '{}' was deleted in exported realm. It may be reintroduced during import", alias);
                continue;
            }
            var baselineAction = entry.getValue();

            var diff = javers.compare(baselineAction, exportedAction);
            if (diff.hasChanges()) {
                var normalizedAction = new RequiredActionProviderRepresentation();
                normalizedAction.setAlias(alias);
                for (var change : diff.getChangesByType(PropertyChange.class)) {
                    javersUtil.applyChange(normalizedAction, change);
                }
                normalizedAction.setEnabled(exportedAction.isEnabled());
                normalizedAction.setDefaultAction(exportedAction.isDefaultAction());
                normalizedActions.add(normalizedAction);
            }
        }
        normalizedActions.addAll(exportedMap.values());
        return normalizedActions.isEmpty() ? null : normalizedActions;
    }
}
