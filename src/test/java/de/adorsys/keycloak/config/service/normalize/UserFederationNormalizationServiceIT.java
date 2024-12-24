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
import org.keycloak.representations.idm.UserFederationMapperRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class UserFederationNormalizationServiceIT {

    private UserFederationNormalizationService service;
    private Javers javers;

    @BeforeEach
    public void setUp() {
        javers = mock(Javers.class);
        service = new UserFederationNormalizationService(javers);

    }

    @Test
    public void testNormalizeProviders() {
        List<UserFederationProviderRepresentation> exportedProviders = new ArrayList<>();
        List<UserFederationProviderRepresentation> baselineProviders = new ArrayList<>();

        UserFederationProviderRepresentation exportedProvider = new UserFederationProviderRepresentation();
        exportedProvider.setDisplayName("provider1");
        exportedProviders.add(exportedProvider);

        UserFederationProviderRepresentation baselineProvider = new UserFederationProviderRepresentation();
        baselineProvider.setDisplayName("provider1");
        baselineProviders.add(baselineProvider);

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compare(any(), any())).thenReturn(diff);

        List<UserFederationProviderRepresentation> result = service.normalizeProviders(exportedProviders, baselineProviders);

        assertThat(result).isNotEmpty();
    }

    @Test
    public void testNormalizeMappers() {
        List<UserFederationMapperRepresentation> exportedMappers = new ArrayList<>();
        List<UserFederationMapperRepresentation> baselineMappers = new ArrayList<>();

        UserFederationMapperRepresentation exportedMapper = new UserFederationMapperRepresentation();
        exportedMapper.setName("mapper1");
        exportedMapper.setFederationProviderDisplayName("provider1");
        exportedMappers.add(exportedMapper);

        UserFederationMapperRepresentation baselineMapper = new UserFederationMapperRepresentation();
        baselineMapper.setName("mapper1");
        baselineMapper.setFederationProviderDisplayName("provider1");
        baselineMappers.add(baselineMapper);

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compare(any(), any())).thenReturn(diff);

        List<UserFederationMapperRepresentation> result = service.normalizeMappers(exportedMappers, baselineMappers);

        assertThat(result).isNotEmpty();
    }
}
