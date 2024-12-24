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

import de.adorsys.keycloak.config.provider.BaselineProvider;
import de.adorsys.keycloak.config.util.JaversUtil;
import org.javers.core.Javers;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.*;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ClientNormalizationServiceTest {

    private ClientNormalizationService service;
    private Javers javers;
    private BaselineProvider baselineProvider;

    @BeforeEach
    public void setUp() {
        javers = mock(Javers.class);
        baselineProvider = mock(BaselineProvider.class);
        JaversUtil javersUtil = mock(JaversUtil.class);
        service = new ClientNormalizationService(javers, baselineProvider, javersUtil);
    }

    @Test
    public void testNormalizeClients() {
        RealmRepresentation exportedRealm = new RealmRepresentation();
        RealmRepresentation baselineRealm = new RealmRepresentation();

        ClientRepresentation exportedClient = new ClientRepresentation();
        exportedClient.setClientId("client1");
        exportedClient.setProtocol("openid-connect");

        ClientRepresentation baselineClient = new ClientRepresentation();
        baselineClient.setClientId("client1");
        exportedClient.setProtocol("openid-connect");

        exportedRealm.setClients(Collections.singletonList(exportedClient));
        baselineRealm.setClients(Collections.singletonList(baselineClient));

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compare(any(), any())).thenReturn(diff);
        when(baselineProvider.getClient(anyString(), anyString())).thenReturn(baselineClient);

        List<ClientRepresentation> result = service.normalizeClients(exportedRealm, baselineRealm);

        assertThat(result).isNotEmpty();
    }

    @Test
    public void testNormalizeClient() {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("client1");
        client.setProtocol("openid-connect");

        RealmRepresentation exportedRealm = new RealmRepresentation();
        exportedRealm.setKeycloakVersion("1.0");

        Diff diff = mock(Diff.class);
        when(javers.compare(any(), any())).thenReturn(diff);
        when(baselineProvider.getClient(anyString(), anyString())).thenReturn(new ClientRepresentation());

        ClientRepresentation result = service.normalizeClient(client, "1.0", exportedRealm);

        assertThat(result).isNotNull();
        assertThat(result.getClientId()).isEqualTo("client1");
        assertThat(result.getProtocol()).isEqualTo("openid-connect");
    }

    @Test
    public void testClientChanged() {
        ClientRepresentation exportedClient = new ClientRepresentation();
        ClientRepresentation baselineClient = new ClientRepresentation();

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compare(any(), any())).thenReturn(diff);

        boolean result = service.clientChanged(exportedClient, baselineClient);

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

    @Test
    public void testAuthorizationSettingsChanged() {
        ResourceServerRepresentation exportedSettings = new ResourceServerRepresentation();
        ResourceServerRepresentation baselineSettings = new ResourceServerRepresentation();

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(javers.compare(any(), any())).thenReturn(diff);

        boolean result = service.authorizationSettingsChanged(exportedSettings, baselineSettings);

        assertThat(result).isTrue();
    }
}
