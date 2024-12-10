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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AttributeNormalizationServiceIT {

    private AttributeNormalizationService service;
    private Javers javers;

    @BeforeEach
    public void setUp() {
        javers = mock(Javers.class);
        service = new AttributeNormalizationService(javers);
    }

    @Test
    public void testNormalizeStringAttributes() {
        Map<String, String> exportedAttributes = new HashMap<>();
        Map<String, String> baselineAttributes = new HashMap<>();

        exportedAttributes.put("attr1", "value1");
        baselineAttributes.put("attr1", "value2");

        Map<String, String> result = service.normalizeStringAttributes(exportedAttributes, baselineAttributes);

        assertThat(result).isNotEmpty();
        assertThat(result.get("attr1")).isEqualTo("value1");
    }

    @Test
    public void testNormalizeListAttributes() {
        Map<String, List<String>> exportedAttributes = new HashMap<>();
        Map<String, List<String>> baselineAttributes = new HashMap<>();

        exportedAttributes.put("attr1", Collections.singletonList("value1"));
        baselineAttributes.put("attr1", Collections.singletonList("value2"));

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compareCollections(anyList(), anyList(), eq(String.class))).thenReturn(diff);

        Map<String, List<String>> result = service.normalizeListAttributes(exportedAttributes, baselineAttributes);

        assertThat(result).isNotEmpty();
        assertThat(result.get("attr1")).contains("value1");
    }

    @Test
    public void testListAttributesChanged() {
        Map<String, List<String>> exportedAttributes = new HashMap<>();
        Map<String, List<String>> baselineAttributes = new HashMap<>();

        exportedAttributes.put("attr1", Collections.singletonList("value1"));
        baselineAttributes.put("attr1", Collections.singletonList("value2"));

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compareCollections(anyList(), anyList(), eq(String.class))).thenReturn(diff);

        boolean result = service.listAttributesChanged(exportedAttributes, baselineAttributes);

        assertThat(result).isTrue();
    }
}
