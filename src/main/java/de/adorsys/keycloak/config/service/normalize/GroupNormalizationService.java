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
import org.keycloak.representations.idm.GroupRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.adorsys.keycloak.config.service.normalize.RealmNormalizationService.getNonNull;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "NORMALIZE")
public class GroupNormalizationService {

    private static final Logger logger = LoggerFactory.getLogger(GroupNormalizationService.class);

    private final Javers unOrderedJavers;
    private final AttributeNormalizationService attributeNormalizationService;

    public GroupNormalizationService(Javers unOrderedJavers,
                                     AttributeNormalizationService attributeNormalizationService) {
        this.unOrderedJavers = unOrderedJavers;
        this.attributeNormalizationService = attributeNormalizationService;
    }

    public List<GroupRepresentation> normalizeGroups(List<GroupRepresentation> exportedGroups, List<GroupRepresentation> baselineGroups) {
        var exportedOrEmpty = getNonNull(exportedGroups);
        var baselineOrEmpty = getNonNull(baselineGroups);
        var exportedGroupsMap = exportedOrEmpty.stream()
                .collect(Collectors.toMap(GroupRepresentation::getPath, Function.identity()));
        var baselineGroupsMap = baselineOrEmpty.stream()
                .collect(Collectors.toMap(GroupRepresentation::getPath, Function.identity()));

        var normalizedGroups = new ArrayList<GroupRepresentation>();
        for (var entry : baselineGroupsMap.entrySet()) {
            var groupPath = entry.getKey();
            var exportedGroup = exportedGroupsMap.remove(groupPath);
            if (exportedGroup == null) {
                logger.warn("Default realm group '{}' was deleted in exported realm. It may be reintroduced during import", groupPath);
                continue;
            }
            var baselineGroup = entry.getValue();
            var diff = unOrderedJavers.compare(baselineGroup, exportedGroup);

            if (diff.hasChanges() || subGroupsChanged(exportedGroup, baselineGroup)
                    || attributeNormalizationService.listAttributesChanged(exportedGroup.getAttributes(), baselineGroup.getAttributes())
                    || attributeNormalizationService.listAttributesChanged(exportedGroup.getClientRoles(), baselineGroup.getClientRoles())) {
                normalizedGroups.add(exportedGroup);
            }
        }
        normalizedGroups.addAll(exportedGroupsMap.values());
        normalizeGroupList(normalizedGroups);
        return normalizedGroups.isEmpty() ? null : normalizedGroups;
    }

    public boolean subGroupsChanged(GroupRepresentation exportedGroup, GroupRepresentation baselineGroup) {
        if (exportedGroup.getSubGroups() == null && baselineGroup.getSubGroups() != null) {
            return true;
        }
        if (exportedGroup.getSubGroups() != null && baselineGroup.getSubGroups() == null) {
            return true;
        }
        if (exportedGroup.getSubGroups() == null && baselineGroup.getSubGroups() == null) {
            return false;
        }

        Map<String, GroupRepresentation> exportedSubGroups = exportedGroup.getSubGroups().stream()
                .collect(Collectors.toMap(GroupRepresentation::getPath, Function.identity()));
        Map<String, GroupRepresentation> baselineSubGroups = baselineGroup.getSubGroups().stream()
                .collect(Collectors.toMap(GroupRepresentation::getPath, Function.identity()));

        for (var entry : baselineSubGroups.entrySet()) {
            var groupPath = entry.getKey();
            var exportedSubGroup = exportedSubGroups.remove(groupPath);

            if (exportedSubGroup == null) {
                // There's a subgroup in the baseline that's gone in the export. This counts as a change.
                return true;
            }
            var baselineSubGroup = entry.getValue();
            if (unOrderedJavers.compare(baselineSubGroup, exportedSubGroup).hasChanges()) {
                return true;
            }
            if (subGroupsChanged(exportedSubGroup, baselineSubGroup)) {
                return true;
            }
        }

        // There are subgroups in the export that are not in the baseline. This is a change.
        return !exportedSubGroups.isEmpty();
    }

    public void normalizeGroupList(List<GroupRepresentation> groups) {
        for (var group : groups) {
            if (group.getAttributes() != null && group.getAttributes().isEmpty()) {
                group.setAttributes(null);
            }
            if (group.getRealmRoles() != null && group.getRealmRoles().isEmpty()) {
                group.setRealmRoles(null);
            }
            if (group.getClientRoles() != null && group.getClientRoles().isEmpty()) {
                group.setClientRoles(null);
            }
            if (group.getSubGroups() != null) {
                if (group.getSubGroups().isEmpty()) {
                    group.setSubGroups(null);
                } else {
                    normalizeGroupList(group.getSubGroups());
                }
            }
            group.setId(null);
        }
    }
}
