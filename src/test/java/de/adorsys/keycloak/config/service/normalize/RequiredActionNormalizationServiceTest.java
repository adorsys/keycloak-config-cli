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
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class RequiredActionNormalizationServiceTest {

    private RequiredActionNormalizationService service;
    private Javers javers;

    @BeforeEach
    public void setUp() {
        javers = mock(Javers.class);
        service = new RequiredActionNormalizationService(javers);
    }

    @Test
    public void testNormalizeRequiredActions() {
        List<RequiredActionProviderRepresentation> exportedActions = new ArrayList<>();
        List<RequiredActionProviderRepresentation> baselineActions = new ArrayList<>();

        RequiredActionProviderRepresentation exportedAction = new RequiredActionProviderRepresentation();
        exportedAction.setAlias("action1");
        exportedActions.add(exportedAction);

        RequiredActionProviderRepresentation baselineAction = new RequiredActionProviderRepresentation();
        baselineAction.setAlias("action1");
        baselineActions.add(baselineAction);

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compare(any(), any())).thenReturn(diff);

        List<RequiredActionProviderRepresentation> result = service.normalizeRequiredActions(exportedActions, baselineActions);

        assertThat(result).isNotEmpty();
    }
}
