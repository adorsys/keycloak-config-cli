/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2021 adorsys GmbH & Co. KG @ https://adorsys.com
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
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.GroupRepresentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class GroupNormalizationServiceIT {

    private GroupNormalizationService service;
    private Javers javers;
    private AttributeNormalizationService attributeNormalizationService;

    @BeforeEach
    public void setUp() {
        javers = mock(Javers.class);
        attributeNormalizationService = mock(AttributeNormalizationService.class);
        service = new GroupNormalizationService(javers, attributeNormalizationService);
    }

    @Test
    public void testNormalizeGroups() {
        List<GroupRepresentation> exportedGroups = new ArrayList<>();
        List<GroupRepresentation> baselineGroups = new ArrayList<>();

        GroupRepresentation exportedGroup = new GroupRepresentation();
        exportedGroup.setPath("/group1");
        exportedGroups.add(exportedGroup);

        GroupRepresentation baselineGroup = new GroupRepresentation();
        baselineGroup.setPath("/group1");
        baselineGroups.add(baselineGroup);

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compare(any(), any())).thenReturn(diff);
        when(attributeNormalizationService.listAttributesChanged(any(), any())).thenReturn(false);

        List<GroupRepresentation> result = service.normalizeGroups(exportedGroups, baselineGroups);

        assertThat(result).isNotEmpty();
    }

    @Test
    public void testSubGroupsChanged() {
        GroupRepresentation exportedGroup = new GroupRepresentation();
        GroupRepresentation baselineGroup = new GroupRepresentation();

        exportedGroup.setSubGroups(Collections.singletonList(new GroupRepresentation()));
        baselineGroup.setSubGroups(Collections.singletonList(new GroupRepresentation()));

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compare(any(), any())).thenReturn(diff);

        boolean result = service.subGroupsChanged(exportedGroup, baselineGroup);

        assertThat(result).isTrue();
    }

    @Test
    public void testNormalizeGroupList() {
        List<GroupRepresentation> groups = new ArrayList<>();
        GroupRepresentation group = new GroupRepresentation();
        group.setAttributes(Collections.emptyMap());
        group.setRealmRoles(Collections.emptyList());
        group.setClientRoles(Collections.emptyMap());
        group.setSubGroups(Collections.emptyList());
        groups.add(group);

        service.normalizeGroupList(groups);

        assertThat(group.getAttributes()).isNull();
        assertThat(group.getRealmRoles()).isNull();
        assertThat(group.getClientRoles()).isNull();
        assertThat(group.getId()).isNull();
    }
}
