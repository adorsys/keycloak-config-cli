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

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportBehaviorsProperties;
import de.adorsys.keycloak.config.repository.ClientRepository;
import de.adorsys.keycloak.config.repository.GroupRepository;
import de.adorsys.keycloak.config.repository.RealmRepository;
import de.adorsys.keycloak.config.repository.RoleRepository;
import de.adorsys.keycloak.config.repository.UserRepository;
import de.adorsys.keycloak.config.util.JsonUtil;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserImportServiceRetryTest {

    @Mock
    private RealmRepository realmRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ImportConfigProperties importConfigProperties;

    @Mock
    private ImportBehaviorsProperties importBehaviorsProperties;

    @InjectMocks
    private UserImportService userImportService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(importConfigProperties.getBehaviors()).thenReturn(importBehaviorsProperties);
        when(importBehaviorsProperties.isSkipAttributesForFederatedUser()).thenReturn(false);
    }

    @Test
    void shouldRetryUpdateWithoutPasswordWhenPasswordHistoryViolationOccurs() throws IOException {
        // Given
        UserRepresentation userToImport = loadUserFromJson("import-files/users/61_create_realm_with_password_history_policy.json");
        String realm = "testPasswordHistory"; // Matched from JSON

        UserRepresentation existingUser = loadUserFromJson("import-files/users/61_create_realm_with_password_history_policy.json");
        existingUser.setId("user1");
        
        // Ensure email is different to simulate update intent (though not strictly required for this test logic)
        userToImport.setEmail("new@example.com");

        when(userRepository.search(realm, userToImport.getUsername())).thenReturn(Optional.of(existingUser));

        // Mock realmRepository.get(realmName) to return a RealmRepresentation
        RealmRepresentation realmRepresentation = new RealmRepresentation();
        realmRepresentation.setRegistrationEmailAsUsername(false);
        when(realmRepository.get(realm)).thenReturn(realmRepresentation);

        // Mock BadRequestException for password history
        Response response = mock(Response.class);
        when(response.readEntity(String.class)).thenReturn("InvalidPasswordHistoryMessage");
        BadRequestException passwordHistoryException = mock(BadRequestException.class);
        when(passwordHistoryException.getResponse()).thenReturn(response);
        when(passwordHistoryException.getMessage()).thenReturn("HTTP 400 Bad Request");

        // First call fails with exception, second call succeeds
        // We use doAnswer to inspect arguments at the time of call
        // and the object is mutated between calls.
        doAnswer(invocation -> {
            UserRepresentation userArg = invocation.getArgument(1);
            assertNotNull(userArg.getCredentials(), "First attempt should have credentials");
            assertFalse(userArg.getCredentials().isEmpty(), "First attempt should have credentials");
            throw passwordHistoryException;
        }).doAnswer(invocation -> {
            UserRepresentation userArg = invocation.getArgument(1);
            assertTrue(userArg.getCredentials() == null || userArg.getCredentials().isEmpty(), 
                    "Second attempt should not have password credentials");
            return null;
        }).when(userRepository).updateUser(eq(realm), any(UserRepresentation.class));

        // Whencombine this text
        assertDoesNotThrow(() -> userImportService.doImport(createRealmImport(realm, userToImport)));

        // Then
        verify(userRepository, times(2)).updateUser(eq(realm), any(UserRepresentation.class));
    }

    @Test
    void shouldUpdateFieldsWithoutChangingPassword() throws IOException {
        // Given
        UserRepresentation userToImport = loadUserFromJson("import-files/users/61_create_realm_with_password_history_policy.json");
        String realm = "testPasswordHistory";
        
        // Update lastName field without changing password
        userToImport.setLastName("UpdatedLastName");

        UserRepresentation existingUser = loadUserFromJson("import-files/users/61_create_realm_with_password_history_policy.json");
        existingUser.setId("user1");

        when(userRepository.search(realm, userToImport.getUsername())).thenReturn(Optional.of(existingUser));

        // Mock realmRepository.get(realmName)
        RealmRepresentation realmRepresentation = new RealmRepresentation();
        realmRepresentation.setRegistrationEmailAsUsername(false);
        when(realmRepository.get(realm)).thenReturn(realmRepresentation);

        // When
        assertDoesNotThrow(() -> userImportService.doImport(createRealmImport(realm, userToImport)));

        // Then - verify updateUser was called exactly once and succeeded
        verify(userRepository, times(1)).updateUser(eq(realm), any(UserRepresentation.class));
    }

    private RealmImport createRealmImport(String realm, UserRepresentation user) {
        RealmImport realmImport = new RealmImport();
        realmImport.setRealm(realm);
        realmImport.setUsers(Collections.singletonList(user));
        return realmImport;
    }

    private UserRepresentation loadUserFromJson(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            RealmImport realmImport = JsonUtil.readValue(jsonContent, RealmImport.class);
            if (realmImport.getUsers() == null || realmImport.getUsers().isEmpty()) {
                throw new IOException("No users found in JSON file: " + resourcePath);
            }
            return realmImport.getUsers().get(0);
        }
    }
}
