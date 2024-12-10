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
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ClientScopeNormalizationServiceIT {

    private ClientScopeNormalizationService service;
    private Javers javers;

    @BeforeEach
    public void setUp() {
        javers = mock(Javers.class);
        service = new ClientScopeNormalizationService(javers);
    }

    @Test
    public void testNormalizeClientScopes() {
        List<ClientScopeRepresentation> exportedScopes = new ArrayList<>();
        List<ClientScopeRepresentation> baselineScopes = new ArrayList<>();

        ClientScopeRepresentation exportedScope = new ClientScopeRepresentation();
        exportedScope.setName("scope1");
        exportedScope.setProtocolMappers(Collections.singletonList(new ProtocolMapperRepresentation()));
        exportedScopes.add(exportedScope);

        ClientScopeRepresentation baselineScope = new ClientScopeRepresentation();
        baselineScope.setName("scope1");
        baselineScope.setProtocolMappers(Collections.singletonList(new ProtocolMapperRepresentation()));
        baselineScopes.add(baselineScope);

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compare(any(), any())).thenReturn(diff);

        List<ClientScopeRepresentation> result = service.normalizeClientScopes(exportedScopes, baselineScopes);

        assertThat(result).isNotEmpty();
    }

    @Test
    public void testClientScopeChanged() {
        ClientScopeRepresentation exportedScope = new ClientScopeRepresentation();
        ClientScopeRepresentation baselineScope = new ClientScopeRepresentation();

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compare(any(), any())).thenReturn(diff);

        boolean result = service.clientScopeChanged(exportedScope, baselineScope);

        assertThat(result).isTrue();
    }

    @Test
    public void testProtocolMappersChanged() {
        List<ProtocolMapperRepresentation> exportedMappers = Collections.singletonList(new ProtocolMapperRepresentation());
        List<ProtocolMapperRepresentation> baselineMappers = Collections.singletonList(new ProtocolMapperRepresentation());

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compareCollections(anyList(), anyList(), eq(ProtocolMapperRepresentation.class))).thenReturn(diff);

        boolean result = service.protocolMappersChanged(exportedMappers, baselineMappers);

        assertThat(result).isTrue();
    }
}
