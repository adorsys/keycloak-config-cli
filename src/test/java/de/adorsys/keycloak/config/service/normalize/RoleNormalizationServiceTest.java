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
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class RoleNormalizationServiceTest {

    private RoleNormalizationService service;
    private Javers javers;

    @BeforeEach
    public void setUp() {
        javers = mock(Javers.class);
        AttributeNormalizationService attributeNormalizationService = mock(AttributeNormalizationService.class);
        service = new RoleNormalizationService(javers, attributeNormalizationService);
    }

    @Test
    public void testNormalizeRoles() {
        RolesRepresentation exportedRoles = new RolesRepresentation();
        RolesRepresentation baselineRoles = new RolesRepresentation();

        RoleRepresentation exportedRole = new RoleRepresentation();
        exportedRole.setName("role1");

        RoleRepresentation baselineRole = new RoleRepresentation();
        baselineRole.setName("role1");

        exportedRoles.setRealm(Collections.singletonList(exportedRole));
        baselineRoles.setRealm(Collections.singletonList(baselineRole));

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compare(any(), any())).thenReturn(diff);

        RolesRepresentation result = service.normalizeRoles(exportedRoles, baselineRoles);

        assertThat(result).isNotNull();
        assertThat(result.getRealm()).isNotEmpty();
    }

    @Test
    public void testNormalizeRealmRoles() {
        List<RoleRepresentation> exportedRoles = new ArrayList<>();
        List<RoleRepresentation> baselineRoles = new ArrayList<>();

        RoleRepresentation exportedRole = new RoleRepresentation();
        exportedRole.setName("role1");
        exportedRoles.add(exportedRole);

        RoleRepresentation baselineRole = new RoleRepresentation();
        baselineRole.setName("role1");
        baselineRoles.add(baselineRole);

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compare(any(), any())).thenReturn(diff);

        List<RoleRepresentation> result = service.normalizeRealmRoles(exportedRoles, baselineRoles);

        assertThat(result).isNotEmpty();
    }

    @Test
    public void testNormalizeClientRoles() {
        Map<String, List<RoleRepresentation>> exportedRoles = new HashMap<>();
        Map<String, List<RoleRepresentation>> baselineRoles = new HashMap<>();

        RoleRepresentation exportedRole = new RoleRepresentation();
        exportedRole.setName("role1");
        exportedRoles.put("client1", Collections.singletonList(exportedRole));

        RoleRepresentation baselineRole = new RoleRepresentation();
        baselineRole.setName("role1");
        baselineRoles.put("client1", Collections.singletonList(baselineRole));

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compare(any(), any())).thenReturn(diff);

        Map<String, List<RoleRepresentation>> result = service.normalizeClientRoles(exportedRoles, baselineRoles);

        assertThat(result).isNotEmpty();
    }

    @Test
    public void testNormalizeRoleList() {
        List<RoleRepresentation> exportedRoles = new ArrayList<>();
        List<RoleRepresentation> baselineRoles = new ArrayList<>();

        RoleRepresentation exportedRole = new RoleRepresentation();
        exportedRole.setName("role1");
        exportedRoles.add(exportedRole);

        RoleRepresentation baselineRole = new RoleRepresentation();
        baselineRole.setName("role1");
        baselineRoles.add(baselineRole);

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compare(any(), any())).thenReturn(diff);

        List<RoleRepresentation> result = service.normalizeRoleList(exportedRoles, baselineRoles, null);

        assertThat(result).isNotEmpty();
    }

    @Test
    public void testNormalizeList() {
        List<RoleRepresentation> roles = new ArrayList<>();

        RoleRepresentation role = new RoleRepresentation();
        role.setName("role1");
        roles.add(role);

        List<RoleRepresentation> result = service.normalizeList(roles);

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getId()).isNull();
    }

    @Test
    public void testCompositesChanged() {
        RoleRepresentation.Composites exportedComposites = new RoleRepresentation.Composites();
        RoleRepresentation.Composites baselineComposites = new RoleRepresentation.Composites();

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compare(any(), any())).thenReturn(diff);

        boolean result = service.compositesChanged(exportedComposites, baselineComposites);

        assertThat(result).isTrue();
    }
}
