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

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.repository.AuthenticationFlowRepository;
import de.adorsys.keycloak.config.repository.ClientRepository;
import de.adorsys.keycloak.config.repository.ClientScopeRepository;
import de.adorsys.keycloak.config.service.state.StateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;

import java.util.List;
import java.util.Optional;

import static de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues.FULL;
import static de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues.NO_DELETE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ClientImportServiceTest {

    private ClientRepository clientRepository;
    private ClientScopeRepository clientScopeRepository;
    private AuthenticationFlowRepository authenticationFlowRepository;
    private ImportConfigProperties importConfigProperties;
    private ImportConfigProperties.ImportManagedProperties managedProperties;
    private ImportConfigProperties.ImportRemoteStateProperties remoteStateProperties;
    private StateService stateService;
    private KeycloakProvider keycloakProvider;

    private ClientImportService service;

    @BeforeEach
    void setUp() {
        clientRepository = mock(ClientRepository.class);
        clientScopeRepository = mock(ClientScopeRepository.class);
        authenticationFlowRepository = mock(AuthenticationFlowRepository.class);
        importConfigProperties = mock(ImportConfigProperties.class);
        managedProperties = mock(ImportConfigProperties.ImportManagedProperties.class);
        remoteStateProperties = mock(ImportConfigProperties.ImportRemoteStateProperties.class);
        stateService = mock(StateService.class);
        keycloakProvider = mock(KeycloakProvider.class);

        when(importConfigProperties.getManaged()).thenReturn(managedProperties);
        when(managedProperties.getClient()).thenReturn(NO_DELETE);
        when(importConfigProperties.getRemoteState()).thenReturn(remoteStateProperties);
        when(remoteStateProperties.isEnabled()).thenReturn(false);
        when(stateService.getClients()).thenReturn(List.of());
        when(importConfigProperties.isParallel()).thenReturn(false);
        when(importConfigProperties.isValidate()).thenReturn(false);
        when(keycloakProvider.isFgapV2Active()).thenReturn(false);

        service = new ClientImportService(
                clientRepository,
                clientScopeRepository,
                authenticationFlowRepository,
                importConfigProperties,
                stateService,
                keycloakProvider
        );
    }

    @Test
    void shouldSkipAdminPermissionsClientWhenFgapV2Active() {
        when(keycloakProvider.isFgapV2Active()).thenReturn(true);

        ClientRepresentation adminPermissions = new ClientRepresentation();
        adminPermissions.setClientId(ClientImportService.ADMIN_PERMISSIONS_CLIENT_ID);

        RealmImport realmImport = new RealmImport();
        realmImport.setRealm("test-realm");
        realmImport.setClients(List.of(adminPermissions));

        service.doImport(realmImport);

        verifyNoInteractions(clientRepository);
    }

    @Test
    void shouldCreateAdminPermissionsClientWhenFgapV2InactiveAndMissing() {
        when(keycloakProvider.isFgapV2Active()).thenReturn(false);
        when(clientRepository.searchByClientId("test-realm", "admin-permissions")).thenReturn(Optional.empty());

        ClientRepresentation adminPermissions = new ClientRepresentation();
        adminPermissions.setClientId("admin-permissions");

        RealmImport realmImport = new RealmImport();
        realmImport.setRealm("test-realm");
        realmImport.setClients(List.of(adminPermissions));

        assertThrows(ImportProcessingException.class, () -> service.doImport(realmImport));

        // Creating system clients is forbidden: ensure we exercised that branch
        verify(clientRepository, times(1)).searchByClientId("test-realm", "admin-permissions");
        verify(clientRepository, never()).create(anyString(), any());
    }

    @Test
    void shouldThrowWhenAuthorizationSettingsWithBearerOnlyAndValidationEnabled() {
        when(importConfigProperties.isValidate()).thenReturn(true);

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("my-client");
        client.setBearerOnly(true);
        client.setAuthorizationSettings(new ResourceServerRepresentation());

        RealmImport realmImport = new RealmImport();
        realmImport.setRealm("test-realm");
        realmImport.setClients(List.of(client));

        assertThrows(ImportProcessingException.class, () -> service.doImport(realmImport));
        verify(clientRepository, never()).create(anyString(), any());
    }

    @Test
    void shouldNotDeleteAdminPermissionsClientWhenFgapV2Active() {
        when(managedProperties.getClient()).thenReturn(FULL);
        when(keycloakProvider.isFgapV2Active()).thenReturn(true);

        ClientRepresentation imported = new ClientRepresentation();
        imported.setClientId("keep");
        imported.setDefaultClientScopes(List.of());
        imported.setOptionalClientScopes(List.of());

        ClientRepresentation existingAdminPermissions = new ClientRepresentation();
        existingAdminPermissions.setClientId("admin-permissions");

        ClientRepresentation existingOther = new ClientRepresentation();
        existingOther.setClientId("remove-me");

        ClientRepresentation existingKeep = new ClientRepresentation();
        existingKeep.setClientId("keep");
        existingKeep.setDefaultClientScopes(List.of());
        existingKeep.setOptionalClientScopes(List.of());

        when(clientRepository.getAll("test-realm")).thenReturn(List.of(existingAdminPermissions, existingOther));
        when(clientRepository.searchByClientId("test-realm", "keep")).thenReturn(Optional.of(existingKeep));

        RealmImport realmImport = new RealmImport();
        realmImport.setRealm("test-realm");
        realmImport.setClients(List.of(imported));

        service.doImport(realmImport);

        verify(clientRepository, times(1)).remove("test-realm", existingOther);
        verify(clientRepository, never()).remove("test-realm", existingAdminPermissions);
    }
}
