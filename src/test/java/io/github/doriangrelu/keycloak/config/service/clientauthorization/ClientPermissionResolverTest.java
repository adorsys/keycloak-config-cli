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

package io.github.doriangrelu.keycloak.config.service.clientauthorization;

import io.github.doriangrelu.keycloak.config.exception.ImportProcessingException;
import io.github.doriangrelu.keycloak.config.exception.KeycloakRepositoryException;
import io.github.doriangrelu.keycloak.config.repository.ClientRepository;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.ClientRepresentation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

/**
 * Unit tests for ClientPermissionResolver to cover error handling paths.
 */
class ClientPermissionResolverTest {

    private ClientRepository clientRepository;
    private ClientPermissionResolver resolver;
    private static final String REALM_NAME = "test-realm";
    private static final String CLIENT_ID = "test-client";
    private static final String AUTHZ_NAME = "test-authz";

    @BeforeEach
    void setUp() {
        clientRepository = mock(ClientRepository.class);
        resolver = new ClientPermissionResolver(REALM_NAME, clientRepository);
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
    void shouldResolveObjectIdSuccessfully() {
        // Given: Client exists
        ClientRepresentation client = new ClientRepresentation();
        client.setId("client-uuid");
        client.setClientId(CLIENT_ID);
        when(clientRepository.getByClientId(REALM_NAME, CLIENT_ID)).thenReturn(client);

        // When: Resolve object ID
        String id = resolver.resolveObjectId(CLIENT_ID, AUTHZ_NAME);

        // Then: Returns client UUID
        assert id.equals("client-uuid");
        verify(clientRepository, times(1)).getByClientId(REALM_NAME, CLIENT_ID);
    }

    @Test
    void shouldThrowImportProcessingExceptionWhenClientNotFound() {
        // Given: Client not found
        when(clientRepository.getByClientId(REALM_NAME, CLIENT_ID))
                .thenThrow(NotFoundException.class);

        // When/Then: Should throw ImportProcessingException
        assertThrows(ImportProcessingException.class, () ->
            resolver.resolveObjectId(CLIENT_ID, AUTHZ_NAME)
        );
    }

    @Test
    void shouldThrowImportProcessingExceptionWhenKeycloakRepositoryException() {
        // Given: Repository throws KeycloakRepositoryException
        when(clientRepository.getByClientId(REALM_NAME, CLIENT_ID))
                .thenThrow(new KeycloakRepositoryException("Error"));

        // When/Then: Should throw ImportProcessingException
        assertThrows(ImportProcessingException.class, () ->
            resolver.resolveObjectId(CLIENT_ID, AUTHZ_NAME)
        );
    }

    @Test
    void shouldEnablePermissionsSuccessfully() {
        // Given: Permissions not enabled
        when(clientRepository.isPermissionEnabled(REALM_NAME, CLIENT_ID)).thenReturn(false);

        // When: Enable permissions
        resolver.enablePermissions(CLIENT_ID);

        // Then: Permissions should be enabled
        verify(clientRepository, times(1)).isPermissionEnabled(REALM_NAME, CLIENT_ID);
        verify(clientRepository, times(1)).enablePermission(REALM_NAME, CLIENT_ID);
    }

    @Test
    void shouldSkipEnablingWhenAlreadyEnabled() {
        // Given: Permissions already enabled
        when(clientRepository.isPermissionEnabled(REALM_NAME, CLIENT_ID)).thenReturn(true);

        // When: Enable permissions
        resolver.enablePermissions(CLIENT_ID);

        // Then: Should not call enablePermission
        verify(clientRepository, times(1)).isPermissionEnabled(REALM_NAME, CLIENT_ID);
        verify(clientRepository, never()).enablePermission(anyString(), anyString());
    }

    @Test
    void shouldThrowImportProcessingExceptionWhenClientNotFoundOnEnablePermissions() {
        // Given: Client not found when checking permissions
        when(clientRepository.isPermissionEnabled(REALM_NAME, CLIENT_ID))
                .thenThrow(NotFoundException.class);

        // When/Then: Should throw ImportProcessingException
        assertThrows(ImportProcessingException.class, () ->
            resolver.enablePermissions(CLIENT_ID)
        );
    }

    @Test
    void shouldHandleServerErrorExceptionWith501() {
        // Given: Permissions not enabled
        when(clientRepository.isPermissionEnabled(REALM_NAME, CLIENT_ID)).thenReturn(false);

        // And: ServerErrorException with 501 (Not Implemented) when enabling
        ServerErrorException exception = new ServerErrorException(createMockResponse(501));
        doThrow(exception)
                .when(clientRepository).enablePermission(REALM_NAME, CLIENT_ID);

        // When: Enable permissions
        resolver.enablePermissions(CLIENT_ID);

        // Then: Exception is caught and logged, continues gracefully
        verify(clientRepository, times(1)).isPermissionEnabled(REALM_NAME, CLIENT_ID);
        verify(clientRepository, times(1)).enablePermission(REALM_NAME, CLIENT_ID);
    }

    @Test
    void shouldHandleNotFoundExceptionWith404() {
        // Given: Permissions not enabled
        when(clientRepository.isPermissionEnabled(REALM_NAME, CLIENT_ID)).thenReturn(false);

        // And: NotFoundException with 404 when enabling
        NotFoundException exception = new NotFoundException(createMockResponse(404));
        doThrow(exception)
                .when(clientRepository).enablePermission(REALM_NAME, CLIENT_ID);

        // When: Enable permissions
        resolver.enablePermissions(CLIENT_ID);

        // Then: Exception is caught and logged, continues gracefully
        verify(clientRepository, times(1)).isPermissionEnabled(REALM_NAME, CLIENT_ID);
        verify(clientRepository, times(1)).enablePermission(REALM_NAME, CLIENT_ID);
    }

    @Test
    void shouldRethrowServerErrorExceptionWithOtherStatus() {
        // Given: Permissions not enabled
        when(clientRepository.isPermissionEnabled(REALM_NAME, CLIENT_ID)).thenReturn(false);

        // And: ServerErrorException with 500 (other error) when enabling
        ServerErrorException exception = new ServerErrorException(createMockResponse(500));
        doThrow(exception)
                .when(clientRepository).enablePermission(REALM_NAME, CLIENT_ID);

        // When/Then: Exception should be re-thrown
        assertThrows(ServerErrorException.class, () ->
            resolver.enablePermissions(CLIENT_ID)
        );

        verify(clientRepository, times(1)).isPermissionEnabled(REALM_NAME, CLIENT_ID);
        verify(clientRepository, times(1)).enablePermission(REALM_NAME, CLIENT_ID);
    }
}
