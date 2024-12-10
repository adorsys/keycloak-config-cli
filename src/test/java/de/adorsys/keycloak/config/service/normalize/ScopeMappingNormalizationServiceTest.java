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
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;

import java.lang.reflect.Field;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ScopeMappingNormalizationServiceTest {

    private ScopeMappingNormalizationService service;
    private Javers javers;

    @BeforeEach
    public void setUp() {
        javers = mock(Javers.class);
        service = new ScopeMappingNormalizationService(javers);
    }


    @Test
    public void testNormalizeScopeMappings() throws NoSuchFieldException, IllegalAccessException {
        RealmRepresentation exportedRealm = new RealmRepresentation();
        RealmRepresentation baselineRealm = new RealmRepresentation();

        ScopeMappingRepresentation exportedMapping = new ScopeMappingRepresentation();
        exportedMapping.setClientScope("clientScope1");
        exportedMapping.setRoles(Set.of("role1"));

        ScopeMappingRepresentation baselineMapping = new ScopeMappingRepresentation();
        baselineMapping.setClientScope("clientScope1");
        baselineMapping.setRoles(Set.of("role2"));

        Field exportedField = RealmRepresentation.class.getDeclaredField("scopeMappings");
        exportedField.setAccessible(true);
        exportedField.set(exportedRealm, new ArrayList<>(List.of(exportedMapping)));

        Field baselineField = RealmRepresentation.class.getDeclaredField("scopeMappings");
        baselineField.setAccessible(true);
        baselineField.set(baselineRealm, new ArrayList<>(List.of(baselineMapping)));

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compare(any(), any())).thenReturn(diff);

        List<ScopeMappingRepresentation> result = service.normalizeScopeMappings(exportedRealm, baselineRealm);

        assertThat(result).contains(exportedMapping);
    }

    @Test
    public void testNormalizeClientScopeMappings() {
        RealmRepresentation exportedRealm = new RealmRepresentation();
        RealmRepresentation baselineRealm = new RealmRepresentation();

        ScopeMappingRepresentation exportedMapping = new ScopeMappingRepresentation();
        exportedMapping.setClientScope("clientScope1");
        exportedMapping.setRoles(Set.of("role1"));

        ScopeMappingRepresentation baselineMapping = new ScopeMappingRepresentation();
        baselineMapping.setClientScope("clientScope1");
        baselineMapping.setRoles(Set.of("role2"));

        Map<String, List<ScopeMappingRepresentation>> exportedClientScopeMappings = new HashMap<>();
        exportedClientScopeMappings.put("clientScope1", List.of(exportedMapping));
        exportedRealm.setClientScopeMappings(exportedClientScopeMappings);

        Map<String, List<ScopeMappingRepresentation>> baselineClientScopeMappings = new HashMap<>();
        baselineClientScopeMappings.put("clientScope1", List.of(baselineMapping));
        baselineRealm.setClientScopeMappings(baselineClientScopeMappings);

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compareCollections(anyList(), anyList(), eq(ScopeMappingRepresentation.class))).thenReturn(diff);

        Map<String, List<ScopeMappingRepresentation>> result = service.normalizeClientScopeMappings(exportedRealm, baselineRealm);

        assertThat(result).containsKey("clientScope1");
        assertThat(result.get("clientScope1")).contains(exportedMapping);
    }

    @Test
    public void testScopeMappingChanged() {
        ScopeMappingRepresentation exportedMapping = new ScopeMappingRepresentation();
        ScopeMappingRepresentation baselineMapping = new ScopeMappingRepresentation();

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compare(any(), any())).thenReturn(diff);

        boolean result = service.scopeMappingChanged(exportedMapping, baselineMapping);

        assertThat(result).isTrue();
    }
}
