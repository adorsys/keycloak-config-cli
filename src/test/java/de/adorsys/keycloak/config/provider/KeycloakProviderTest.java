/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2025 adorsys GmbH & Co. KG @ https://adorsys.com
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

package de.adorsys.keycloak.config.provider;

import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.info.ProfileInfoRepresentation;
import org.keycloak.representations.info.ServerInfoRepresentation;
import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import org.mockito.Mockito;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KeycloakProviderTest {

    private KeycloakProvider createProvider() throws Exception {
    KeycloakConfigProperties props = mock(KeycloakConfigProperties.class);
    when(props.getUrl()).thenReturn("http://localhost:8080/");
    when(props.getLoginRealm()).thenReturn("master");
    when(props.getClientId()).thenReturn("");
    when(props.getGrantType()).thenReturn("");
    when(props.getClientSecret()).thenReturn("");
    when(props.getUser()).thenReturn("");
    when(props.getPassword()).thenReturn("");
    when(props.isSslVerify()).thenReturn(true);
    when(props.getHttpProxy()).thenReturn(null);
    when(props.getConnectTimeout()).thenReturn(java.time.Duration.ofSeconds(1));
    when(props.getReadTimeout()).thenReturn(java.time.Duration.ofSeconds(1));
    when(props.getAvailabilityCheck()).thenReturn(new KeycloakConfigProperties.KeycloakAvailabilityCheck(false, java.time.Duration.ofSeconds(1), java.time.Duration.ofSeconds(1)));
        
        Constructor<KeycloakProvider> ctor = KeycloakProvider.class.getDeclaredConstructor(KeycloakConfigProperties.class);
        ctor.setAccessible(true);
        return ctor.newInstance(props);
    }

    @Test
    void testIsFgapV2Active_WhenV2IsDefault() throws Exception {
        KeycloakProvider provider = createProvider();

    Keycloak kc = mock(Keycloak.class);
    org.keycloak.admin.client.resource.ServerInfoResource serverInfoResource = mock(org.keycloak.admin.client.resource.ServerInfoResource.class);
    ServerInfoRepresentation serverInfo = mock(ServerInfoRepresentation.class);
    ProfileInfoRepresentation profile = mock(ProfileInfoRepresentation.class);

    when(profile.getDisabledFeatures()).thenReturn(null);
    when(profile.getPreviewFeatures()).thenReturn(null);
    when(profile.getExperimentalFeatures()).thenReturn(null);

    when(serverInfo.getProfileInfo()).thenReturn(profile);
    when(serverInfoResource.getInfo()).thenReturn(serverInfo);
    when(kc.serverInfo()).thenReturn(serverInfoResource);

    KeycloakProvider spy = Mockito.spy(provider);
    doReturn(kc).when(spy).getInstance();

    assertTrue(spy.isFgapV2Active());
    }

    @Test
    void testIsFgapV2Active_WhenV2IsExplicitlyDisabled() throws Exception {
        KeycloakProvider provider = createProvider();

    Keycloak kc2 = mock(Keycloak.class);
    org.keycloak.admin.client.resource.ServerInfoResource serverInfoResource2 = mock(org.keycloak.admin.client.resource.ServerInfoResource.class);
    ServerInfoRepresentation serverInfo2 = mock(ServerInfoRepresentation.class);
    ProfileInfoRepresentation profile2 = mock(ProfileInfoRepresentation.class);

    when(profile2.getDisabledFeatures()).thenReturn(java.util.List.of("admin-fine-grained-authz:v2"));
    when(serverInfo2.getProfileInfo()).thenReturn(profile2);
    when(serverInfoResource2.getInfo()).thenReturn(serverInfo2);
    when(kc2.serverInfo()).thenReturn(serverInfoResource2);

    KeycloakProvider spy2 = Mockito.spy(provider);
    doReturn(kc2).when(spy2).getInstance();

    assertFalse(spy2.isFgapV2Active());
    }

    @Test
    void testIsFgapV2Active_WhenV1IsEnabled() throws Exception {
        KeycloakProvider provider = createProvider();

    Keycloak kc3 = mock(Keycloak.class);
    org.keycloak.admin.client.resource.ServerInfoResource serverInfoResource3 = mock(org.keycloak.admin.client.resource.ServerInfoResource.class);
    ServerInfoRepresentation serverInfo3 = mock(ServerInfoRepresentation.class);
    ProfileInfoRepresentation profile3 = mock(ProfileInfoRepresentation.class);

    when(profile3.getPreviewFeatures()).thenReturn(java.util.List.of("admin-fine-grained-authz:v1"));
    when(serverInfo3.getProfileInfo()).thenReturn(profile3);
    when(serverInfoResource3.getInfo()).thenReturn(serverInfo3);
    when(kc3.serverInfo()).thenReturn(serverInfoResource3);

    KeycloakProvider spy3 = Mockito.spy(provider);
    doReturn(kc3).when(spy3).getInstance();

    assertFalse(spy3.isFgapV2Active());
    }

    @Test
    void testIsFgapV2Active_CachesResult() throws Exception {
        KeycloakProvider provider = createProvider();

    Keycloak kc4 = mock(Keycloak.class);
    org.keycloak.admin.client.resource.ServerInfoResource serverInfoResource4 = mock(org.keycloak.admin.client.resource.ServerInfoResource.class);
    ServerInfoRepresentation serverInfo4 = mock(ServerInfoRepresentation.class);
    ProfileInfoRepresentation profile4 = mock(ProfileInfoRepresentation.class);

    when(profile4.getDisabledFeatures()).thenReturn(null);
    when(profile4.getPreviewFeatures()).thenReturn(null);
    when(profile4.getExperimentalFeatures()).thenReturn(null);

    when(serverInfo4.getProfileInfo()).thenReturn(profile4);
    when(serverInfoResource4.getInfo()).thenReturn(serverInfo4);
    when(kc4.serverInfo()).thenReturn(serverInfoResource4);

    KeycloakProvider spy4 = Mockito.spy(provider);
    doReturn(kc4).when(spy4).getInstance();

    assertTrue(spy4.isFgapV2Active());
    assertTrue(spy4.isFgapV2Active());

    verify(kc4, times(1)).serverInfo();
    }

    @Test
    void testIsFgapV2Active_HandlesExceptionGracefully() throws Exception {
        KeycloakProvider provider = createProvider();

    Keycloak kc5 = mock(Keycloak.class);
    when(kc5.serverInfo()).thenThrow(new RuntimeException("boom"));

    KeycloakProvider spy5 = Mockito.spy(provider);
    doReturn(kc5).when(spy5).getInstance();

    assertFalse(spy5.isFgapV2Active());
    }
}
