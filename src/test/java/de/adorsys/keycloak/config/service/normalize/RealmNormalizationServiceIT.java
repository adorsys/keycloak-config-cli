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

import de.adorsys.keycloak.config.properties.NormalizationKeycloakConfigProperties;
import de.adorsys.keycloak.config.provider.BaselineProvider;
import de.adorsys.keycloak.config.util.JaversUtil;
import org.javers.core.Javers;
import org.javers.core.diff.Diff;
import org.javers.core.diff.changetype.PropertyChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class RealmNormalizationServiceIT {

    private RealmNormalizationService service;
    private Javers javers;
    private BaselineProvider baselineProvider;
    private NormalizationKeycloakConfigProperties keycloakConfigProperties;

    @BeforeEach
    public void setUp() {
        javers = mock(Javers.class);
        baselineProvider = mock(BaselineProvider.class);
        ClientNormalizationService clientNormalizationService = mock(ClientNormalizationService.class);
        ScopeMappingNormalizationService scopeMappingNormalizationService = mock(ScopeMappingNormalizationService.class);
        ProtocolMapperNormalizationService protocolMapperNormalizationService = mock(ProtocolMapperNormalizationService.class);
        ClientScopeNormalizationService clientScopeNormalizationService = mock(ClientScopeNormalizationService.class);
        RoleNormalizationService roleNormalizationService = mock(RoleNormalizationService.class);
        AttributeNormalizationService attributeNormalizationService = mock(AttributeNormalizationService.class);
        GroupNormalizationService groupNormalizationService = mock(GroupNormalizationService.class);
        AuthFlowNormalizationService authFlowNormalizationService = mock(AuthFlowNormalizationService.class);
        IdentityProviderNormalizationService identityProviderNormalizationService = mock(IdentityProviderNormalizationService.class);
        RequiredActionNormalizationService requiredActionNormalizationService = mock(RequiredActionNormalizationService.class);
        UserFederationNormalizationService userFederationNormalizationService = mock(UserFederationNormalizationService.class);
        ClientPolicyNormalizationService clientPolicyNormalizationService = mock(ClientPolicyNormalizationService.class);
        JaversUtil javersUtil = mock(JaversUtil.class);
        keycloakConfigProperties = mock(NormalizationKeycloakConfigProperties.class);

        service = new RealmNormalizationService(
                keycloakConfigProperties,
                javers,
                baselineProvider,
                clientNormalizationService,
                scopeMappingNormalizationService,
                protocolMapperNormalizationService,
                clientScopeNormalizationService,
                roleNormalizationService,
                attributeNormalizationService,
                groupNormalizationService,
                authFlowNormalizationService,
                identityProviderNormalizationService,
                requiredActionNormalizationService,
                userFederationNormalizationService,
                clientPolicyNormalizationService,
                javersUtil
        );
    }

    @Test
    public void testNormalizeRealm() {
        RealmRepresentation exportedRealm = new RealmRepresentation();
        exportedRealm.setKeycloakVersion("1.0");
        exportedRealm.setRealm("test-realm");

        RealmRepresentation baselineRealm = new RealmRepresentation();
        baselineRealm.setRealm("test-realm");

        when(keycloakConfigProperties.getVersion()).thenReturn("1.0");
        when(baselineProvider.getRealm("1.0", "test-realm")).thenReturn(baselineRealm);

        Diff diff = mock(Diff.class);
        when(javers.compare(any(), any())).thenReturn(diff);
        when(diff.getChangesByType(PropertyChange.class)).thenReturn(Collections.emptyList());

        RealmRepresentation result = service.normalizeRealm(exportedRealm);

        assertThat(result).isNotNull();
        assertThat(result.getRealm()).isEqualTo("test-realm");
    }

    @Test
    public void testHandleBaseRealm() {
        RealmRepresentation exportedRealm = new RealmRepresentation();
        exportedRealm.setRealm("test-realm");
        exportedRealm.setEnabled(true);

        RealmRepresentation baselineRealm = new RealmRepresentation();
        baselineRealm.setRealm("test-realm");

        RealmRepresentation minimizedRealm = new RealmRepresentation();

        Diff diff = mock(Diff.class);
        when(javers.compare(any(), any())).thenReturn(diff);
        when(diff.getChangesByType(PropertyChange.class)).thenReturn(Collections.emptyList());

        service.handleBaseRealm(exportedRealm, baselineRealm, minimizedRealm);

        assertThat(minimizedRealm.getRealm()).isEqualTo("test-realm");
        assertThat(minimizedRealm.isEnabled()).isTrue();
    }
}
