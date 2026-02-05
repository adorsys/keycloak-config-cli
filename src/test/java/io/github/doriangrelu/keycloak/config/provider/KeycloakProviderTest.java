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

package io.github.doriangrelu.keycloak.config.provider;

import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.info.ProfileInfoRepresentation;
import org.keycloak.representations.info.ServerInfoRepresentation;
import io.github.doriangrelu.keycloak.config.properties.KeycloakConfigProperties;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
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
        when(props.getAvailabilityCheck()).thenReturn(new KeycloakConfigProperties.KeycloakAvailabilityCheck(false,
                java.time.Duration.ofSeconds(1), java.time.Duration.ofSeconds(1)));

        Constructor<KeycloakProvider> ctor = KeycloakProvider.class
                .getDeclaredConstructor(KeycloakConfigProperties.class);
        ctor.setAccessible(true);
        return ctor.newInstance(props);
    }

    @Test
    void testIsFgapV2Active_WhenV2IsDefault() throws Exception {
        KeycloakProvider provider = createProvider();

        Keycloak kc = mock(Keycloak.class);
        org.keycloak.admin.client.resource.ServerInfoResource serverInfoResource = mock(
                org.keycloak.admin.client.resource.ServerInfoResource.class);
        ServerInfoRepresentation serverInfo = mock(ServerInfoRepresentation.class);
        ProfileInfoRepresentation profile = mock(ProfileInfoRepresentation.class);
        org.keycloak.representations.info.SystemInfoRepresentation systemInfo = mock(
                org.keycloak.representations.info.SystemInfoRepresentation.class);

        when(systemInfo.getVersion()).thenReturn("26.2.0");
        when(profile.getDisabledFeatures()).thenReturn(null);
        when(profile.getPreviewFeatures()).thenReturn(null);
        when(profile.getExperimentalFeatures()).thenReturn(null);

        when(serverInfo.getSystemInfo()).thenReturn(systemInfo);
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
        org.keycloak.admin.client.resource.ServerInfoResource serverInfoResource2 = mock(
                org.keycloak.admin.client.resource.ServerInfoResource.class);
        ServerInfoRepresentation serverInfo2 = mock(ServerInfoRepresentation.class);
        ProfileInfoRepresentation profile2 = mock(ProfileInfoRepresentation.class);
        org.keycloak.representations.info.SystemInfoRepresentation systemInfo2 = mock(
                org.keycloak.representations.info.SystemInfoRepresentation.class);

        when(systemInfo2.getVersion()).thenReturn("26.2.0");
        when(profile2.getDisabledFeatures()).thenReturn(java.util.List.of("admin-fine-grained-authz:v2"));
        when(serverInfo2.getSystemInfo()).thenReturn(systemInfo2);
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
        org.keycloak.admin.client.resource.ServerInfoResource serverInfoResource3 = mock(
                org.keycloak.admin.client.resource.ServerInfoResource.class);
        ServerInfoRepresentation serverInfo3 = mock(ServerInfoRepresentation.class);
        ProfileInfoRepresentation profile3 = mock(ProfileInfoRepresentation.class);
        org.keycloak.representations.info.SystemInfoRepresentation systemInfo3 = mock(
                org.keycloak.representations.info.SystemInfoRepresentation.class);

        when(systemInfo3.getVersion()).thenReturn("26.2.0");
        when(profile3.getPreviewFeatures()).thenReturn(java.util.List.of("admin-fine-grained-authz:v1"));
        when(serverInfo3.getSystemInfo()).thenReturn(systemInfo3);
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
        org.keycloak.admin.client.resource.ServerInfoResource serverInfoResource4 = mock(
                org.keycloak.admin.client.resource.ServerInfoResource.class);
        ServerInfoRepresentation serverInfo4 = mock(ServerInfoRepresentation.class);
        ProfileInfoRepresentation profile4 = mock(ProfileInfoRepresentation.class);
        org.keycloak.representations.info.SystemInfoRepresentation systemInfo4 = mock(
                org.keycloak.representations.info.SystemInfoRepresentation.class);

        when(systemInfo4.getVersion()).thenReturn("26.2.0");
        when(profile4.getDisabledFeatures()).thenReturn(null);
        when(profile4.getPreviewFeatures()).thenReturn(null);
        when(profile4.getExperimentalFeatures()).thenReturn(null);

        when(serverInfo4.getSystemInfo()).thenReturn(systemInfo4);
        when(serverInfo4.getProfileInfo()).thenReturn(profile4);
        when(serverInfoResource4.getInfo()).thenReturn(serverInfo4);
        when(kc4.serverInfo()).thenReturn(serverInfoResource4);

        KeycloakProvider spy4 = Mockito.spy(provider);
        doReturn(kc4).when(spy4).getInstance();

        assertTrue(spy4.isFgapV2Active());
        assertTrue(spy4.isFgapV2Active());

        // serverInfo() is called once: version and profile check happen in same call
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

    @Test
    void testIsFgapV2Active_WhenVersionIsBefore26_2() throws Exception {
        KeycloakProvider provider = createProvider();

        Keycloak kc = mock(Keycloak.class);
        org.keycloak.admin.client.resource.ServerInfoResource serverInfoResource = mock(
                org.keycloak.admin.client.resource.ServerInfoResource.class);
        ServerInfoRepresentation serverInfo = mock(ServerInfoRepresentation.class);
        org.keycloak.representations.info.SystemInfoRepresentation systemInfo = mock(
                org.keycloak.representations.info.SystemInfoRepresentation.class);

        when(systemInfo.getVersion()).thenReturn("23.0.7");
        when(serverInfo.getSystemInfo()).thenReturn(systemInfo);
        when(serverInfoResource.getInfo()).thenReturn(serverInfo);
        when(kc.serverInfo()).thenReturn(serverInfoResource);

        KeycloakProvider spy = Mockito.spy(provider);
        doReturn(kc).when(spy).getInstance();

        assertFalse(spy.isFgapV2Active());
    }

    @Test
    void testIsFgapV2Active_WhenVersionIs26_1() throws Exception {
        KeycloakProvider provider = createProvider();

        Keycloak kc = mock(Keycloak.class);
        org.keycloak.admin.client.resource.ServerInfoResource serverInfoResource = mock(
                org.keycloak.admin.client.resource.ServerInfoResource.class);
        ServerInfoRepresentation serverInfo = mock(ServerInfoRepresentation.class);
        org.keycloak.representations.info.SystemInfoRepresentation systemInfo = mock(
                org.keycloak.representations.info.SystemInfoRepresentation.class);

        when(systemInfo.getVersion()).thenReturn("26.1.0");
        when(serverInfo.getSystemInfo()).thenReturn(systemInfo);
        when(serverInfoResource.getInfo()).thenReturn(serverInfo);
        when(kc.serverInfo()).thenReturn(serverInfoResource);

        KeycloakProvider spy = Mockito.spy(provider);
        doReturn(kc).when(spy).getInstance();

        assertFalse(spy.isFgapV2Active());
    }

    @Test
    void testGetKeycloakVersion_WhenMasterRealm_ReturnsVersion() throws Exception {
        KeycloakProvider provider = createProvider();
        Keycloak kc = mock(Keycloak.class);
        org.keycloak.admin.client.resource.ServerInfoResource serverInfoResource = mock(
                org.keycloak.admin.client.resource.ServerInfoResource.class);
        ServerInfoRepresentation serverInfo = mock(ServerInfoRepresentation.class);
        org.keycloak.representations.info.SystemInfoRepresentation systemInfo = mock(
                org.keycloak.representations.info.SystemInfoRepresentation.class);

        when(systemInfo.getVersion()).thenReturn("26.2.0");
        when(serverInfo.getSystemInfo()).thenReturn(systemInfo);
        when(serverInfoResource.getInfo()).thenReturn(serverInfo);
        when(kc.serverInfo()).thenReturn(serverInfoResource);

        KeycloakProvider spy = Mockito.spy(provider);
        doReturn(kc).when(spy).getInstance();

        assertEquals("26.2.0", spy.getKeycloakVersion());
    }

    @Test
    void testGetKeycloakVersion_WhenSkipServerInfoIsTrue_ReturnsUnknown() throws Exception {
        KeycloakConfigProperties props = mock(KeycloakConfigProperties.class);
        when(props.isSkipServerInfo()).thenReturn(true);
        when(props.getVersion()).thenReturn("@keycloak.version@");

        Constructor<KeycloakProvider> ctor = KeycloakProvider.class
                .getDeclaredConstructor(KeycloakConfigProperties.class);
        ctor.setAccessible(true);
        KeycloakProvider provider = ctor.newInstance(props);

        assertEquals("unknown", provider.getKeycloakVersion());
    }

    @Test
    void testGetKeycloakVersion_WhenServerInfoFailsWith403_ReturnsUnknown() throws Exception {
        KeycloakConfigProperties props = mock(KeycloakConfigProperties.class);
        when(props.isSkipServerInfo()).thenReturn(false);
        when(props.getVersion()).thenReturn("@keycloak.version@");

        Constructor<KeycloakProvider> ctor = KeycloakProvider.class
                .getDeclaredConstructor(KeycloakConfigProperties.class);
        ctor.setAccessible(true);
        KeycloakProvider provider = ctor.newInstance(props);

        Keycloak kc = mock(Keycloak.class);
        org.keycloak.admin.client.resource.ServerInfoResource serverInfoResource = mock(
                org.keycloak.admin.client.resource.ServerInfoResource.class);

        Response response = Response.status(403).build();
        when(serverInfoResource.getInfo()).thenThrow(new WebApplicationException(response));
        when(kc.serverInfo()).thenReturn(serverInfoResource);

        KeycloakProvider spy = Mockito.spy(provider);
        doReturn(kc).when(spy).getInstance();

        assertEquals("unknown", spy.getKeycloakVersion());
    }

    @Test
    void testGetKeycloakVersion_WhenExplicitVersionProvided_ReturnsExplicitVersion() throws Exception {
        KeycloakConfigProperties props = mock(KeycloakConfigProperties.class);
        when(props.isSkipServerInfo()).thenReturn(true);
        when(props.getVersion()).thenReturn("24.0.0");

        Constructor<KeycloakProvider> ctor = KeycloakProvider.class
                .getDeclaredConstructor(KeycloakConfigProperties.class);
        ctor.setAccessible(true);
        KeycloakProvider provider = ctor.newInstance(props);

        assertEquals("24.0.0", provider.getKeycloakVersion());
    }

    @Test
    void testIsKeycloakAvailableAlternative_CallsRealm() throws Exception {
        KeycloakConfigProperties props = mock(KeycloakConfigProperties.class);
        when(props.getLoginRealm()).thenReturn("master");

        Constructor<KeycloakProvider> ctor = KeycloakProvider.class
                .getDeclaredConstructor(KeycloakConfigProperties.class);
        ctor.setAccessible(true);
        KeycloakProvider provider = ctor.newInstance(props);

        Keycloak kc = mock(Keycloak.class);
        org.keycloak.admin.client.resource.RealmResource realmResource = mock(
                org.keycloak.admin.client.resource.RealmResource.class);
        when(kc.realm("master")).thenReturn(realmResource);

        java.lang.reflect.Method method = KeycloakProvider.class.getDeclaredMethod("isKeycloakAvailableAlternative",
                Keycloak.class);
        method.setAccessible(true);
        method.invoke(provider, kc);

        verify(kc, times(1)).realm("master");
        verify(realmResource, times(1)).toRepresentation();
    }
}
