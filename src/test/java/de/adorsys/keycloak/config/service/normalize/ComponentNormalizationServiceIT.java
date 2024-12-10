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
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentExportRepresentation;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ComponentNormalizationServiceIT {

    private ComponentNormalizationService service;
    private Javers javers;

    @BeforeEach
    public void setUp() {
        javers = mock(Javers.class);
        service = new ComponentNormalizationService(javers);
    }

    @Test
    public void testNormalizeComponents() {
        MultivaluedHashMap<String, ComponentExportRepresentation> exportedComponents = new MultivaluedHashMap<>();
        MultivaluedHashMap<String, ComponentExportRepresentation> baselineComponents = new MultivaluedHashMap<>();

        ComponentExportRepresentation exportedComponent = new ComponentExportRepresentation();
        exportedComponent.setName("component1");
        exportedComponents.add("class1", exportedComponent);

        ComponentExportRepresentation baselineComponent = new ComponentExportRepresentation();
        baselineComponent.setName("component1");
        baselineComponents.add("class1", baselineComponent);

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compare(any(), any())).thenReturn(diff);

        MultivaluedHashMap<String, ComponentExportRepresentation> result = service.normalizeComponents(exportedComponents, baselineComponents);

        assertThat(result).isNotEmpty();
    }

    @Test
    public void testNormalizeList() {
        List<ComponentExportRepresentation> exportedComponents = new ArrayList<>();
        List<ComponentExportRepresentation> baselineComponents = new ArrayList<>();

        ComponentExportRepresentation exportedComponent = new ComponentExportRepresentation();
        exportedComponent.setName("component1");
        exportedComponents.add(exportedComponent);

        ComponentExportRepresentation baselineComponent = new ComponentExportRepresentation();
        baselineComponent.setName("component1");
        baselineComponents.add(baselineComponent);

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compare(any(), any())).thenReturn(diff);

        List<ComponentExportRepresentation> result = service.normalizeList(exportedComponents, baselineComponents, "class1");

        assertThat(result).isNotEmpty();
    }

    @Test
    public void testNormalizeEntry() {
        ComponentExportRepresentation component = new ComponentExportRepresentation();
        component.setId("id1");
        component.setConfig(new MultivaluedHashMap<>());

        service.normalizeEntry(component);

        assertThat(component.getId()).isNull();
        assertThat(component.getConfig()).isNull();
    }
}
