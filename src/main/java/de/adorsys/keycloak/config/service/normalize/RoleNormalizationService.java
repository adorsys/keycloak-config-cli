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
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "NORMALIZE")
public class RoleNormalizationService {

    private static final Logger logger = LoggerFactory.getLogger(RoleNormalizationService.class);

    private final Javers unOrderedJavers;
    private final JaversUtil javersUtil;

    private final AttributeNormalizationService attributeNormalizationService;

    @Autowired
    public RoleNormalizationService(Javers unOrderedJavers, JaversUtil javersUtil, AttributeNormalizationService attributeNormalizationService) {
        this.unOrderedJavers = unOrderedJavers;
        this.javersUtil = javersUtil;
        this.attributeNormalizationService = attributeNormalizationService;
    }

    public RolesRepresentation normalizeRoles(RolesRepresentation exportedRoles, RolesRepresentation baselineRoles) {
        var exportedOrEmpty = exportedRoles == null ? new RolesRepresentation() : exportedRoles;
        var baselineOrEmpty = baselineRoles == null ? new RolesRepresentation() : baselineRoles;
        var clientRoles = normalizeClientRoles(exportedOrEmpty.getClient(), baselineOrEmpty.getClient());
        var realmRoles = normalizeRealmRoles(exportedOrEmpty.getRealm(), baselineOrEmpty.getRealm());
        if (clientRoles == null && realmRoles == null) {
            return null;
        }
        var normalizedRoles = new RolesRepresentation();
        normalizedRoles.setClient(clientRoles);
        normalizedRoles.setRealm(realmRoles);
        return normalizedRoles;
    }

    public List<RoleRepresentation> normalizeRealmRoles(List<RoleRepresentation> exportedRealmRoles, List<RoleRepresentation> baselineRealmRoles) {
        List<RoleRepresentation> exportedOrEmpty = exportedRealmRoles == null ? List.of() : exportedRealmRoles;
        List<RoleRepresentation> baselineOrEmpty = baselineRealmRoles == null ? List.of() : baselineRealmRoles;

        var exportedMap = exportedOrEmpty.stream().collect(Collectors.toMap(RoleRepresentation::getName, Function.identity()));
        var baselineMap = baselineOrEmpty.stream().collect(Collectors.toMap(RoleRepresentation::getName, Function.identity()));

        var normalizedRoles = new ArrayList<RoleRepresentation>();
        for (var entry : baselineMap.entrySet()) {
            var roleName = entry.getKey();
            var baselineRole = entry.getValue();
            var exportedRole = exportedMap.remove(roleName);
            if (exportedRole == null) {
                logger.warn("Default realm role '{}' was deleted in exported realm. It may be reintroduced during import", roleName);
                continue;
            }
            var diff = unOrderedJavers.compare(baselineRole, exportedRole);
            if (diff.hasChanges()
                    || attributesChanged(baselineRole.getAttributes(), exportedRole.getAttributes())
                    || compositesChanged(exportedRole.getComposites(), baselineRole.getComposites())) {
                var normalizedRole = new RoleRepresentation();
                normalizedRole.setName(roleName);
                for (var change : diff.getChangesByType(PropertyChange.class)) {
                    javersUtil.applyChange(normalizedRole, change);
                }
                normalizedRole.setAttributes(attributeNormalizationService.normalizeAttributes(exportedRole.getAttributes(), baselineRole.getAttributes()));
                normalizedRoles.add(normalizedRole);
                normalizedRole.setComposites(exportedRole.getComposites());
            }
        }
        return normalizedRoles.isEmpty() ? null : normalizedRoles;
    }

    private boolean compositesChanged(RoleRepresentation.Composites exportedComposites, RoleRepresentation.Composites baselineComposites) {
        return unOrderedJavers.compare(baselineComposites, exportedComposites).hasChanges();
    }

    private boolean attributesChanged(Map<String, List<String>> exportedAttributes, Map<String, List<String>> baselineAttributes) {
        var exportedOrEmpty = exportedAttributes == null ? Map.of() : exportedAttributes;
        var baselineOrEmpty = baselineAttributes == null ? Map.of() : baselineAttributes;

        return !Objects.equals(exportedOrEmpty, baselineOrEmpty);
    }

    public Map<String, List<RoleRepresentation>> normalizeClientRoles(Map<String, List<RoleRepresentation>> exportedClientRoles,
                                                                      Map<String, List<RoleRepresentation>> baselineClientRoles) {
        Map<String, List<RoleRepresentation>> exportedOrEmpty = exportedClientRoles == null ? Map.of() : exportedClientRoles;
        Map<String, List<RoleRepresentation>> baselineOrEmpty = baselineClientRoles == null ? Map.of() : baselineClientRoles;

        Map<String, List<RoleRepresentation>> normalizedClientRoles = new HashMap<>();
        for (var entry : baselineOrEmpty.entrySet()) {
            var clientName = entry.getKey();
            var baselineRoles = entry.getValue();
            var exportedRoles = exportedOrEmpty.remove(clientName);

            var normalizedRoles = normalizeRealmRoles(exportedRoles, baselineRoles);
            if (normalizedRoles != null) {
                normalizedClientRoles.put(clientName, normalizedRoles);
            }
        }
        return normalizedClientRoles.isEmpty() ? null : normalizedClientRoles;
    }
}
