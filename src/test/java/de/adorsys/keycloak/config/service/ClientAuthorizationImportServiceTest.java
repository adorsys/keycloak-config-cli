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

import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.repository.ClientRepository;
import de.adorsys.keycloak.config.repository.GroupRepository;
import de.adorsys.keycloak.config.repository.IdentityProviderRepository;
import de.adorsys.keycloak.config.repository.RoleRepository;
import de.adorsys.keycloak.config.service.state.StateService;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * Unit tests for ClientAuthorizationImportService focusing on FGAP V2 error handling.
 * These tests ensure the error handling code paths are covered even on Keycloak versions < 26.2.
 */
class ClientAuthorizationImportServiceTest {

    private ClientRepository clientRepository;
    private ImportConfigProperties importConfigProperties;
    private ClientAuthorizationImportService service;

    @BeforeEach
    void setUp() {
        clientRepository = mock(ClientRepository.class);
        IdentityProviderRepository identityProviderRepository = mock(IdentityProviderRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        GroupRepository groupRepository = mock(GroupRepository.class);
        importConfigProperties = mock(ImportConfigProperties.class);
        StateService stateService = mock(StateService.class);

        // Mock the nested managed properties chain
        ImportConfigProperties.ImportManagedProperties managedProperties = mock(ImportConfigProperties.ImportManagedProperties.class);
        when(importConfigProperties.getManaged()).thenReturn(managedProperties);
        when(managedProperties.getClientAuthorizationResources()).thenReturn(ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues.NO_DELETE);
        when(managedProperties.getClientAuthorizationPolicies()).thenReturn(ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues.NO_DELETE);
        when(managedProperties.getClientAuthorizationScopes()).thenReturn(ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues.NO_DELETE);

        service = new ClientAuthorizationImportService(
                clientRepository,
                identityProviderRepository,
                roleRepository,
                groupRepository,
                importConfigProperties,
                stateService
        );
    }

    @Nested
    class FgapV2ErrorHandling {

        private RealmImport realmImport;
        private ClientRepresentation client;
        private ResourceServerRepresentation authorizationSettings;

        @BeforeEach
        void setUp() {
            realmImport = new RealmImport();
            realmImport.setRealm("test-realm");

            client = new ClientRepresentation();
            client.setId("client-id");
            client.setClientId("test-client");

            authorizationSettings = new ResourceServerRepresentation();
            authorizationSettings.setResources(new ArrayList<>());
            authorizationSettings.setScopes(new ArrayList<>());
            authorizationSettings.setPolicies(new ArrayList<>());
            // Set properties that will make authorization settings different from existing
            authorizationSettings.setPolicyEnforcementMode(PolicyEnforcementMode.ENFORCING);
            authorizationSettings.setAllowRemoteResourceManagement(true);
            authorizationSettings.setDecisionStrategy(DecisionStrategy.UNANIMOUS);

            client.setAuthorizationSettings(authorizationSettings);
            realmImport.setClients(List.of(client));

            // Mock client repository responses
            when(clientRepository.getByClientId(anyString(), anyString())).thenReturn(client);

            // Create a separate authorization for existing state with DIFFERENT settings
            ResourceServerRepresentation existingAuth = new ResourceServerRepresentation();
            existingAuth.setResources(new ArrayList<>());
            existingAuth.setScopes(new ArrayList<>());
            existingAuth.setPolicies(new ArrayList<>());
            // Make existing auth settings different to trigger updateAuthorizationSettings
            existingAuth.setPolicyEnforcementMode(PolicyEnforcementMode.PERMISSIVE);
            existingAuth.setAllowRemoteResourceManagement(false);
            existingAuth.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
            when(clientRepository.getAuthorizationConfigById(anyString(), anyString())).thenReturn(existingAuth);
        }

        private Response createMockResponse(int status) {
            Response response = mock(Response.class);
            Response.StatusType statusType = mock(Response.StatusType.class);
            when(response.getStatus()).thenReturn(status);
            when(response.getStatusInfo()).thenReturn(statusType);
            when(statusType.getStatusCode()).thenReturn(status);
            when(statusType.getFamily()).thenReturn(
                status >= 500 ? Response.Status.Family.SERVER_ERROR :
                status >= 400 ? Response.Status.Family.CLIENT_ERROR :
                Response.Status.Family.SUCCESSFUL
            );
            return response;
        }

        @Test
        void shouldHandleKeycloakRepositoryExceptionWithFgapV2Message() {
            // Given: A resource to import
            ResourceRepresentation resource = new ResourceRepresentation();
            resource.setName("test-resource");
            authorizationSettings.setResources(List.of(resource));

            // And: Repository throws KeycloakRepositoryException with FGAP V2 message
            doThrow(new KeycloakRepositoryException("Authorization API not supported (likely FGAP V2 active)"))
                    .when(clientRepository).createAuthorizationResource(anyString(), anyString(), any());

            // When: Import is executed
            service.doImport(realmImport);

            // Then: Exception is caught and logged, import continues
            verify(clientRepository, times(1)).createAuthorizationResource(eq("test-realm"), eq("client-id"), any());
        }

        @Test
        void shouldHandleServerErrorExceptionWith501Status() {
            // Given: A resource to import
            ResourceRepresentation resource = new ResourceRepresentation();
            resource.setName("test-resource");
            authorizationSettings.setResources(List.of(resource));

            // And: Repository throws ServerErrorException with 501 status (Not Implemented)
            ServerErrorException exception = new ServerErrorException(createMockResponse(501));

            doThrow(exception)
                    .when(clientRepository).createAuthorizationResource(anyString(), anyString(), any());

            // When: Import is executed
            service.doImport(realmImport);

            // Then: Exception is caught and logged as FGAP V2 warning
            verify(clientRepository, times(1)).createAuthorizationResource(eq("test-realm"), eq("client-id"), any());
        }

        @Test
        void shouldHandleNotFoundExceptionWith404Status() {
            // Given: A resource to import
            ResourceRepresentation resource = new ResourceRepresentation();
            resource.setName("test-resource");
            authorizationSettings.setResources(List.of(resource));

            // And: Repository throws NotFoundException (404)
            NotFoundException exception = new NotFoundException(createMockResponse(404));

            doThrow(exception)
                    .when(clientRepository).createAuthorizationResource(anyString(), anyString(), any());

            // When: Import is executed
            service.doImport(realmImport);

            // Then: Exception is caught and logged as FGAP V2 warning
            verify(clientRepository, times(1)).createAuthorizationResource(eq("test-realm"), eq("client-id"), any());
        }

        @Test
        void shouldHandleWebApplicationExceptionWith404Status() {
            // Given: A resource to import
            ResourceRepresentation resource = new ResourceRepresentation();
            resource.setName("test-resource");
            authorizationSettings.setResources(List.of(resource));

            // And: Repository throws WebApplicationException with 404 status
            WebApplicationException exception = new WebApplicationException(createMockResponse(404));

            doThrow(exception)
                    .when(clientRepository).createAuthorizationResource(anyString(), anyString(), any());

            // When: Import is executed
            service.doImport(realmImport);

            // Then: Exception is caught and logged as FGAP V2 warning
            verify(clientRepository, times(1)).createAuthorizationResource(eq("test-realm"), eq("client-id"), any());
        }

        @Test
        void shouldSkipV2ResourceTypeForAdminPermissionsClient() {
            // Given: admin-permissions client with V2 resource type (Groups)
            client.setClientId("admin-permissions");
            ResourceRepresentation groupsResource = new ResourceRepresentation();
            groupsResource.setName("Groups");
            authorizationSettings.setResources(List.of(groupsResource));

            // And: Repository throws NotFoundException (FGAP V2 active)
            NotFoundException exception = new NotFoundException(createMockResponse(404));

            doThrow(exception)
                    .when(clientRepository).createAuthorizationResource(anyString(), anyString(), any());

            // When: Import is executed
            service.doImport(realmImport);

            // Then: V2 resource type is skipped with debug log
            verify(clientRepository, times(1)).createAuthorizationResource(eq("test-realm"), eq("client-id"), any());
        }

        @Test
        void shouldSkipV2ResourceTypeUsers() {
            // Given: admin-permissions client with V2 resource type (Users)
            client.setClientId("admin-permissions");
            ResourceRepresentation usersResource = new ResourceRepresentation();
            usersResource.setName("Users");
            authorizationSettings.setResources(List.of(usersResource));

            // And: Repository throws ServerErrorException with 501
            ServerErrorException exception = new ServerErrorException(createMockResponse(501));

            doThrow(exception)
                    .when(clientRepository).createAuthorizationResource(anyString(), anyString(), any());

            // When: Import is executed
            service.doImport(realmImport);

            // Then: V2 resource type is skipped
            verify(clientRepository, times(1)).createAuthorizationResource(eq("test-realm"), eq("client-id"), any());
        }

        @Test
        void shouldSkipV2ResourceTypeClients() {
            // Given: admin-permissions client with V2 resource type (Clients)
            client.setClientId("admin-permissions");
            ResourceRepresentation clientsResource = new ResourceRepresentation();
            clientsResource.setName("Clients");
            authorizationSettings.setResources(List.of(clientsResource));

            // And: Repository throws KeycloakRepositoryException
            doThrow(new KeycloakRepositoryException("Authorization API not supported (likely FGAP V2 active)"))
                    .when(clientRepository).createAuthorizationResource(anyString(), anyString(), any());

            // When: Import is executed
            service.doImport(realmImport);

            // Then: V2 resource type is skipped
            verify(clientRepository, times(1)).createAuthorizationResource(eq("test-realm"), eq("client-id"), any());
        }

        @Test
        void shouldSkipV2ResourceTypeRoles() {
            // Given: admin-permissions client with V2 resource type (Roles)
            client.setClientId("admin-permissions");
            ResourceRepresentation rolesResource = new ResourceRepresentation();
            rolesResource.setName("Roles");
            authorizationSettings.setResources(List.of(rolesResource));

            // And: Repository throws ServerErrorException
            ServerErrorException exception = new ServerErrorException(createMockResponse(501));

            doThrow(exception)
                    .when(clientRepository).createAuthorizationResource(anyString(), anyString(), any());

            // When: Import is executed
            service.doImport(realmImport);

            // Then: V2 resource type is skipped
            verify(clientRepository, times(1)).createAuthorizationResource(eq("test-realm"), eq("client-id"), any());
        }

        @Test
        void shouldHandleFgapV2ErrorOnUpdateResource() {
            // Given: Existing resource to update
            ResourceRepresentation existingResource = new ResourceRepresentation();
            existingResource.setId("existing-id");
            existingResource.setName("test-resource");

            // Mock existing authorization with the resource
            ResourceServerRepresentation existingAuth = new ResourceServerRepresentation();
            existingAuth.setResources(List.of(existingResource));
            existingAuth.setScopes(new ArrayList<>());
            existingAuth.setPolicies(new ArrayList<>());
            when(clientRepository.getAuthorizationConfigById(anyString(), anyString())).thenReturn(existingAuth);

            ResourceRepresentation updatedResource = new ResourceRepresentation();
            updatedResource.setName("test-resource");
            updatedResource.setDisplayName("Updated Resource");

            realmImport.getClients().get(0).getAuthorizationSettings().setResources(List.of(updatedResource));

            // And: Repository throws NotFoundException on update
            NotFoundException exception = new NotFoundException(createMockResponse(404));

            doThrow(exception)
                    .when(clientRepository).updateAuthorizationResource(anyString(), anyString(), any());

            // When: Import is executed
            service.doImport(realmImport);

            // Then: Exception is caught and logged
            verify(clientRepository, times(1)).updateAuthorizationResource(eq("test-realm"), eq("client-id"), any());
        }

        @Test
        void shouldHandleFgapV2ErrorOnRefreshAuthorizationConfig() {
            // Given: A resource to import
            ResourceRepresentation resource = new ResourceRepresentation();
            resource.setName("test-resource");
            authorizationSettings.setResources(List.of(resource));

            // And: Repository throws 501 when refreshing authorization config
            ServerErrorException exception = new ServerErrorException(createMockResponse(501));

            when(clientRepository.getAuthorizationConfigById(anyString(), anyString()))
                    .thenReturn(authorizationSettings)  // First call succeeds
                    .thenThrow(exception);              // Second call (refresh) throws 501

            // When: Import is executed
            service.doImport(realmImport);

            // Then: Exception is caught and logged, uses existing authorization settings
            verify(clientRepository, times(2)).getAuthorizationConfigById(eq("test-realm"), eq("client-id"));
        }

        @Test
        void shouldHandleFgapV2ErrorWith400Status() {
            // Given: A resource to import
            ResourceRepresentation resource = new ResourceRepresentation();
            resource.setName("test-resource");
            authorizationSettings.setResources(List.of(resource));

            // And: Repository throws 400 (Bad Request) when refreshing authorization config
            BadRequestException exception = new BadRequestException(createMockResponse(400));

            when(clientRepository.getAuthorizationConfigById(anyString(), anyString()))
                    .thenReturn(authorizationSettings)
                    .thenThrow(exception);

            // When: Import is executed
            service.doImport(realmImport);

            // Then: Exception is caught and logged, uses existing authorization settings
            verify(clientRepository, times(2)).getAuthorizationConfigById(eq("test-realm"), eq("client-id"));
        }

        @Test
        void shouldHandleNonV2ResourceForAdminPermissionsClient() {
            // Given: admin-permissions client with non-V2 resource type
            client.setClientId("admin-permissions");
            ResourceRepresentation customResource = new ResourceRepresentation();
            customResource.setName("custom-resource");
            authorizationSettings.setResources(List.of(customResource));

            // And: Repository throws FGAP V2 error
            doThrow(new KeycloakRepositoryException("Authorization API not supported (likely FGAP V2 active)"))
                    .when(clientRepository).createAuthorizationResource(anyString(), anyString(), any());

            // When: Import is executed
            service.doImport(realmImport);

            // Then: Warning is logged (not skipped as debug)
            verify(clientRepository, times(1)).createAuthorizationResource(eq("test-realm"), eq("client-id"), any());
        }

        @Test
        void shouldHandleBadRequestExceptionForAdminPermissionsClient() {
            // Given: admin-permissions client
            client.setClientId("admin-permissions");

            // And: Repository throws BadRequestException when updating authorization settings
            BadRequestException exception = new BadRequestException(createMockResponse(400));
            doThrow(exception)
                    .when(clientRepository).updateAuthorizationSettings(anyString(), anyString(), any());

            // When: Import is executed
            service.doImport(realmImport);

            // Then: Exception is caught and logged for admin-permissions client
            verify(clientRepository, times(1)).updateAuthorizationSettings(eq("test-realm"), eq("client-id"), any());
        }

        @Test
        void shouldThrowBadRequestExceptionForNonAdminPermissionsClient() {
            // Given: Regular client (not admin-permissions)
            client.setClientId("regular-client");

            // And: Repository throws BadRequestException when updating authorization settings
            BadRequestException exception = new BadRequestException(createMockResponse(400));
            doThrow(exception)
                    .when(clientRepository).updateAuthorizationSettings(anyString(), anyString(), any());

            // When/Then: Exception should be re-thrown for non-admin-permissions clients
            try {
                service.doImport(realmImport);
            } catch (BadRequestException e) {
                // Expected - exception should be re-thrown
            }

            verify(clientRepository, times(1)).updateAuthorizationSettings(eq("test-realm"), eq("client-id"), any());
        }

        @Test
        void shouldHandleServerErrorExceptionWith501InHandleAuthorizationSettings() {
            // Given: Client with authorization settings
            // And: Repository throws ServerErrorException with 501 status
            ServerErrorException exception = new ServerErrorException(createMockResponse(501));
            doThrow(exception)
                    .when(clientRepository).updateAuthorizationSettings(anyString(), anyString(), any());

            // When: Import is executed
            service.doImport(realmImport);

            // Then: Exception is caught and logged
            verify(clientRepository, times(1)).updateAuthorizationSettings(eq("test-realm"), eq("client-id"), any());
        }

        @Test
        void shouldHandleNotFoundExceptionInHandleAuthorizationSettings() {
            // Given: Client with authorization settings
            // And: Repository throws NotFoundException with 404 status
            NotFoundException exception = new NotFoundException(createMockResponse(404));
            doThrow(exception)
                    .when(clientRepository).updateAuthorizationSettings(anyString(), anyString(), any());

            // When: Import is executed
            service.doImport(realmImport);

            // Then: Exception is caught and logged
            verify(clientRepository, times(1)).updateAuthorizationSettings(eq("test-realm"), eq("client-id"), any());
        }

        @Test
        void shouldHandleScopeCreationWithKeycloakRepositoryException() {
            // Given: Scope to import
            ScopeRepresentation scope = new ScopeRepresentation();
            scope.setName("test-scope");
            authorizationSettings.setScopes(List.of(scope));

            // And: Repository throws KeycloakRepositoryException with FGAP V2 message
            doThrow(new KeycloakRepositoryException("Authorization API not supported (likely FGAP V2 active)"))
                    .when(clientRepository).addAuthorizationScope(anyString(), anyString(), any());

            // When: Import is executed
            service.doImport(realmImport);

            // Then: Exception is caught and logged
            verify(clientRepository, times(1)).addAuthorizationScope(eq("test-realm"), eq("client-id"), any());
        }

        @Test
        void shouldHandleScopeCreationWithNotFoundException() {
            // Given: Scope to import
            ScopeRepresentation scope = new ScopeRepresentation();
            scope.setName("test-scope");
            authorizationSettings.setScopes(List.of(scope));

            // And: Repository throws NotFoundException
            NotFoundException exception = new NotFoundException(createMockResponse(404));
            doThrow(exception)
                    .when(clientRepository).addAuthorizationScope(anyString(), anyString(), any());

            // When: Import is executed
            service.doImport(realmImport);

            // Then: Exception is caught and logged as FGAP V2 warning
            verify(clientRepository, times(1)).addAuthorizationScope(eq("test-realm"), eq("client-id"), any());
        }

        @Test
        void shouldHandleScopeCreationWithServerErrorException() {
            // Given: Scope to import
            ScopeRepresentation scope = new ScopeRepresentation();
            scope.setName("test-scope");
            authorizationSettings.setScopes(List.of(scope));

            // And: Repository throws ServerErrorException with 501
            ServerErrorException exception = new ServerErrorException(createMockResponse(501));
            doThrow(exception)
                    .when(clientRepository).addAuthorizationScope(anyString(), anyString(), any());

            // When: Import is executed
            service.doImport(realmImport);

            // Then: Exception is caught and logged
            verify(clientRepository, times(1)).addAuthorizationScope(eq("test-realm"), eq("client-id"), any());
        }

        @Test
        void shouldHandleScopeUpdateWithFgapV2Error() {
            // Given: Existing scope to update
            ScopeRepresentation existingScope = new ScopeRepresentation();
            existingScope.setId("scope-id");
            existingScope.setName("test-scope");

            // Mock existing authorization with the scope
            ResourceServerRepresentation existingAuth = new ResourceServerRepresentation();
            existingAuth.setScopes(List.of(existingScope));
            existingAuth.setResources(new ArrayList<>());
            existingAuth.setPolicies(new ArrayList<>());
            when(clientRepository.getAuthorizationConfigById(anyString(), anyString())).thenReturn(existingAuth);

            ScopeRepresentation updatedScope = new ScopeRepresentation();
            updatedScope.setName("test-scope");
            updatedScope.setDisplayName("Updated Scope");

            realmImport.getClients().get(0).getAuthorizationSettings().setScopes(List.of(updatedScope));

            // And: Repository throws NotFoundException on update
            NotFoundException exception = new NotFoundException(createMockResponse(404));
            doThrow(exception)
                    .when(clientRepository).updateAuthorizationScope(anyString(), anyString(), any());

            // When: Import is executed
            service.doImport(realmImport);

            // Then: Exception is caught and logged
            verify(clientRepository, times(1)).updateAuthorizationScope(eq("test-realm"), eq("client-id"), any());
        }
    }
}
