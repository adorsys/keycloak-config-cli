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
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.adorsys.keycloak.config.service.normalize.RealmNormalizationService.getNonNull;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "NORMALIZE")
public class RoleNormalizationService {

    private static final Logger logger = LoggerFactory.getLogger(RoleNormalizationService.class);

    private final Javers unOrderedJavers;
    private final AttributeNormalizationService attributeNormalizationService;

    @Autowired
    public RoleNormalizationService(Javers unOrderedJavers, AttributeNormalizationService attributeNormalizationService) {
        this.unOrderedJavers = unOrderedJavers;
        this.attributeNormalizationService = attributeNormalizationService;
    }

    public RolesRepresentation normalizeRoles(RolesRepresentation exportedRoles, RolesRepresentation baselineRoles) {
        var exportedOrEmpty = exportedRoles == null ? new RolesRepresentation() : exportedRoles;
        var baselineOrEmpty = baselineRoles == null ? new RolesRepresentation() : baselineRoles;
        var clientRoles = normalizeClientRoles(exportedOrEmpty.getClient(), baselineOrEmpty.getClient());
        var realmRoles = normalizeRealmRoles(exportedOrEmpty.getRealm(), baselineOrEmpty.getRealm());
        var normalizedRoles = new RolesRepresentation();
        if (!clientRoles.isEmpty()) {
            normalizedRoles.setClient(clientRoles);
        }
        if (!realmRoles.isEmpty()) {
            normalizedRoles.setRealm(realmRoles);
        }

        // avoid generating an empty roles element
        if (normalizedRoles.getRealm() == null || (normalizedRoles.getRealm().isEmpty() && normalizedRoles.getClient().isEmpty())) {
            return null;
        }

        return normalizedRoles;
    }

    public List<RoleRepresentation> normalizeRealmRoles(List<RoleRepresentation> exportedRoles, List<RoleRepresentation> baselineRoles) {
        return normalizeRoleList(exportedRoles, baselineRoles, null);
    }

    public Map<String, List<RoleRepresentation>> normalizeClientRoles(Map<String, List<RoleRepresentation>> exportedRoles,
                                                                      Map<String, List<RoleRepresentation>> baselineRoles) {
        var exportedOrEmpty = getNonNull(exportedRoles);
        var baselineOrEmpty = getNonNull(baselineRoles);

        var normalizedRoles = new HashMap<String, List<RoleRepresentation>>();
        for (var entry : baselineOrEmpty.entrySet()) {
            var clientId = entry.getKey();
            var baselineClientRoles = entry.getValue();
            var exportedClientRoles = exportedOrEmpty.remove(clientId);
            exportedClientRoles = getNonNull(exportedClientRoles);

            var normalizedClientRoles = normalizeRoleList(exportedClientRoles, baselineClientRoles, clientId);
            if (!normalizedClientRoles.isEmpty()) {
                normalizedRoles.put(clientId, normalizedClientRoles);
            }
        }

        for (var entry : exportedOrEmpty.entrySet()) {
            var clientId = entry.getKey();
            var roles = entry.getValue();

            if (!roles.isEmpty()) {
                normalizedRoles.put(clientId, normalizeList(roles));
            }
        }
        return normalizedRoles;
    }

    public List<RoleRepresentation> normalizeRoleList(List<RoleRepresentation> exportedRoles,
                                                      List<RoleRepresentation> baselineRoles, String clientId) {
        var exportedOrEmpty = getNonNull(exportedRoles);
        var baselineOrEmpty = getNonNull(baselineRoles);

        var exportedMap = exportedOrEmpty.stream()
                .collect(Collectors.toMap(RoleRepresentation::getName, Function.identity()));
        var baselineMap = baselineOrEmpty.stream()
                .collect(Collectors.toMap(RoleRepresentation::getName, Function.identity()));
        var normalizedRoles = new ArrayList<RoleRepresentation>();
        for (var entry : baselineMap.entrySet()) {
            var roleName = entry.getKey();
            var exportedRole = exportedMap.remove(roleName);
            if (exportedRole == null) {
                if (clientId == null) {
                    logger.warn("Default realm role '{}' was deleted in exported realm. It may be reintroduced during import!", roleName);
                } else {
                    logger.warn("Default realm client-role '{}' for client '{}' was deleted in the exported realm. "
                            + "It may be reintroduced during import!", roleName, clientId);
                }
                continue;
            }

            var baselineRole = entry.getValue();

            var diff = unOrderedJavers.compare(baselineRole, exportedRole);

            if (diff.hasChanges()
                    || compositesChanged(exportedRole.getComposites(), baselineRole.getComposites())
                    || attributeNormalizationService.listAttributesChanged(exportedRole.getAttributes(), baselineRole.getAttributes())) {
                normalizedRoles.add(exportedRole);
            }
        }
        normalizedRoles.addAll(exportedMap.values());
        return normalizeList(normalizedRoles);
    }

    public List<RoleRepresentation> normalizeList(List<RoleRepresentation> roles) {
        for (var role : roles) {
            role.setId(null);
            if (role.getAttributes() != null && role.getAttributes().isEmpty()) {
                role.setAttributes(null);
            }
        }
        return roles;
    }

    public boolean compositesChanged(RoleRepresentation.Composites exportedComposites, RoleRepresentation.Composites baselineComposites) {
        return unOrderedJavers.compare(baselineComposites, exportedComposites)
                .hasChanges();
    }
}
