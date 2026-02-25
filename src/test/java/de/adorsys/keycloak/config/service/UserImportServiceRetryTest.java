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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserImportServiceRetryTest {

    private final RealmRepository realmRepository = mock(RealmRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final ClientRepository clientRepository = mock(ClientRepository.class);
    private final ImportConfigProperties importConfigProperties = mock(ImportConfigProperties.class);
    private final ImportBehaviorsProperties importBehaviorsProperties = mock(ImportBehaviorsProperties.class);

    private final UserImportService userImportService = new UserImportService(
            realmRepository, userRepository, roleRepository, groupRepository, clientRepository, importConfigProperties
    );

    @BeforeEach
    void setUp() {
        reset(realmRepository, userRepository, roleRepository, groupRepository, clientRepository, importConfigProperties, importBehaviorsProperties);
        when(importConfigProperties.getBehaviors()).thenReturn(importBehaviorsProperties);
        when(importBehaviorsProperties.isSkipAttributesForFederatedUser()).thenReturn(false);
    }

    @Test
    void shouldRetryUpdateWithoutPasswordWhenPasswordHistoryViolationOccurs() throws IOException {
        // Given
        UserRepresentation userToImport = loadUserFromJson("import-files/users/61_create_realm_with_password_history_policy.json");
        String realm = "testPasswordHistory";

        UserRepresentation existingUser = loadUserFromJson("import-files/users/61_create_realm_with_password_history_policy.json");
        existingUser.setId("user1");
        
        userToImport.setEmail("new@example.com");

        when(userRepository.search(realm, userToImport.getUsername())).thenReturn(Optional.of(existingUser));

        RealmRepresentation realmRepresentation = new RealmRepresentation();
        realmRepresentation.setRegistrationEmailAsUsername(false);
        when(realmRepository.get(realm)).thenReturn(realmRepresentation);

        Response response = mock(Response.class);
        when(response.readEntity(String.class)).thenReturn("InvalidPasswordHistoryMessage");
        BadRequestException passwordHistoryException = mock(BadRequestException.class);
        when(passwordHistoryException.getResponse()).thenReturn(response);
        when(passwordHistoryException.getMessage()).thenReturn("HTTP 400 Bad Request");

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

        assertDoesNotThrow(() -> userImportService.doImport(createRealmImport(realm, userToImport)));

        verify(userRepository, times(2)).updateUser(eq(realm), any(UserRepresentation.class));
    }

    @Test
    void shouldUpdateFieldsWithoutChangingPassword() throws IOException {
        // Given
        UserRepresentation userToImport = loadUserFromJson("import-files/users/61_create_realm_with_password_history_policy.json");
        String realm = "testPasswordHistory";
        
        userToImport.setLastName("UpdatedLastName");

        UserRepresentation existingUser = loadUserFromJson("import-files/users/61_create_realm_with_password_history_policy.json");
        existingUser.setId("user1");

        when(userRepository.search(realm, userToImport.getUsername())).thenReturn(Optional.of(existingUser));

        RealmRepresentation realmRepresentation = new RealmRepresentation();
        realmRepresentation.setRegistrationEmailAsUsername(false);
        when(realmRepository.get(realm)).thenReturn(realmRepresentation);

        assertDoesNotThrow(() -> userImportService.doImport(createRealmImport(realm, userToImport)));

        verify(userRepository, times(1)).updateUser(eq(realm), any(UserRepresentation.class));
    }

    @Test
    void shouldIgnoreConfiguredUserUpdateProperties() throws IOException {
        UserRepresentation userToImport = loadUserFromJson("import-files/users/61_create_realm_with_password_history_policy.json");
        String realm = "testIgnoredUserProperties";

        UserRepresentation existingUser = loadUserFromJson("import-files/users/61_create_realm_with_password_history_policy.json");
        existingUser.setId("user1");

        existingUser.setEmail("ldap@example.com");
        userToImport.setEmail("new@example.com");
        userToImport.setLastName("UpdatedLastName");

        when(userRepository.search(realm, userToImport.getUsername())).thenReturn(Optional.of(existingUser));

        RealmRepresentation realmRepresentation = new RealmRepresentation();
        realmRepresentation.setRegistrationEmailAsUsername(false);
        when(realmRepository.get(realm)).thenReturn(realmRepresentation);

        when(importBehaviorsProperties.getUserUpdateIgnoredProperties()).thenReturn(List.of("attributes", "email"));

        assertDoesNotThrow(() -> userImportService.doImport(createRealmImport(realm, userToImport)));

        verify(userRepository).updateUser(eq(realm), any(UserRepresentation.class));
        verify(userRepository).updateUser(eq(realm), argThat(u -> "ldap@example.com".equals(u.getEmail())));
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
