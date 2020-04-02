/*
 * Copyright 2019-2020 adorsys GmbH & Co. KG @ https://adorsys.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.RoleRepository;
import de.adorsys.keycloak.config.repository.UserRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserImportService {
    private static final Logger logger = LoggerFactory.getLogger(UserImportService.class);

    private static final String[] IGNORED_PROPERTIES_FOR_UPDATE = {"realmRoles", "clientRoles"};

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public UserImportService(
            UserRepository userRepository,
            RoleRepository roleRepository
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public void doImport(RealmImport realmImport) {
        List<UserRepresentation> users = realmImport.getUsers();

        if (users != null) {
            for (UserRepresentation user : users) {
                importUser(realmImport.getRealm(), user);
            }
        }
    }

    private void importUser(String realm, UserRepresentation user) {
        UserImport userImport = new UserImport(realm, user);
        userImport.importUser();
    }

    private class UserImport {
        private final String realm;
        private final UserRepresentation userToImport;
        private final String username;

        private UserImport(String realm, UserRepresentation userToImport) {
            this.realm = realm;
            this.userToImport = userToImport;
            this.username = userToImport.getUsername();
        }

        public void importUser() {
            Optional<UserRepresentation> maybeUser = userRepository.tryToFindUser(realm, username);

            if (maybeUser.isPresent()) {
                updateUser(maybeUser.get());
            } else {
                logger.debug("Create user '{}' in realm '{}'", username, realm);
                userRepository.create(realm, userToImport);
            }

            handleRealmRoles();
            handleClientRoles();
        }

        private void updateUser(UserRepresentation existingUser) {
            UserRepresentation patchedUser = CloneUtils.deepPatch(existingUser, userToImport, IGNORED_PROPERTIES_FOR_UPDATE);

            if (!CloneUtils.deepEquals(existingUser, patchedUser)) {
                logger.debug("Update user '{}' in realm '{}'", username, realm);
                userRepository.updateUser(realm, patchedUser);
            } else {
                logger.debug("No need to update user '{}' in realm '{}'", username, realm);
            }
        }

        private void handleRealmRoles() {
            List<String> usersRealmLevelRolesToUpdate = userToImport.getRealmRoles();
            List<String> existingUsersRealmLevelRoles = roleRepository.getUserRealmLevelRoles(realm, username);

            handleRolesToBeAdded(usersRealmLevelRolesToUpdate, existingUsersRealmLevelRoles);
            handleRolesToBeRemoved(usersRealmLevelRolesToUpdate, existingUsersRealmLevelRoles);
        }

        private void handleRolesToBeAdded(List<String> usersRealmLevelRolesToUpdate, List<String> existingUsersRealmLevelRoles) {
            List<String> rolesToAdd = searchForMissingRoles(usersRealmLevelRolesToUpdate, existingUsersRealmLevelRoles);

            if (!rolesToAdd.isEmpty()) {
                List<RoleRepresentation> realmRoles = roleRepository.searchRealmRoles(realm, rolesToAdd);

                logger.debug("Add realm-level roles [{}] to user '{}' in realm '{}'", String.join(",", rolesToAdd), username, realm);

                roleRepository.addRealmRolesToUser(realm, username, realmRoles);
            }
        }

        private void handleRolesToBeRemoved(List<String> usersRealmLevelRolesToUpdate, List<String> existingUsersRealmLevelRoles) {
            List<String> rolesToDelete = searchForMissingRoles(existingUsersRealmLevelRoles, usersRealmLevelRolesToUpdate);

            if (!rolesToDelete.isEmpty()) {
                List<RoleRepresentation> realmRoles = roleRepository.searchRealmRoles(realm, rolesToDelete);

                logger.debug("Remove realm-level roles [{}] from user '{}' in realm '{}'", String.join(",", rolesToDelete), username, realm);

                roleRepository.removeRealmRolesForUser(realm, username, realmRoles);
            }
        }

        private void handleClientRoles() {
            Map<String, List<String>> clientRolesToImport = userToImport.getClientRoles();

            for (Map.Entry<String, List<String>> clientRoles : clientRolesToImport.entrySet()) {
                setupClientRoles(clientRoles);
            }
        }

        private void setupClientRoles(Map.Entry<String, List<String>> clientRoles) {
            String clientId = clientRoles.getKey();

            ClientRoleImport clientRoleImport = new ClientRoleImport(clientId);
            clientRoleImport.importClientRoles();
        }

        private List<String> searchForMissingRoles(List<String> rolesToBeSearchedFor, List<String> rolesToBeTrawled) {
            return rolesToBeSearchedFor.stream()
                    .filter(role -> !rolesToBeTrawled.contains(role))
                    .collect(Collectors.toList());
        }

        private class ClientRoleImport {
            private final String clientId;
            private final List<String> existingClientLevelRoles;
            private final List<String> clientRolesToImport;

            private ClientRoleImport(String clientId) {
                this.clientId = clientId;
                this.existingClientLevelRoles = roleRepository.getUserClientLevelRoles(realm, username, clientId);

                Map<String, List<String>> clientsRolesToImport = userToImport.getClientRoles();
                this.clientRolesToImport = clientsRolesToImport.get(clientId);
            }

            public void importClientRoles() {
                handleClientRolesToBeAdded();
                handleClientRolesToBeRemoved();
            }

            private void handleClientRolesToBeAdded() {
                List<String> clientRolesToAdd = searchForMissingRoles(clientRolesToImport, existingClientLevelRoles);

                if (!clientRolesToAdd.isEmpty()) {
                    List<RoleRepresentation> foundClientRoles = roleRepository.searchClientRoles(realm, clientId, clientRolesToAdd);

                    logger.debug("Add client-level roles [{}] for client '{}' to user '{}' in realm '{}'", String.join(",", clientRolesToAdd), clientId, username, realm);

                    roleRepository.addClientRolesToUser(realm, username, clientId, foundClientRoles);
                }
            }

            private void handleClientRolesToBeRemoved() {
                List<String> clientRolesToRemove = searchForMissingRoles(existingClientLevelRoles, clientRolesToImport);

                if (!clientRolesToRemove.isEmpty()) {
                    List<RoleRepresentation> foundClientRoles = roleRepository.searchClientRoles(realm, clientId, clientRolesToRemove);

                    logger.debug("Remove client-level roles [{}] for client '{}' from user '{}' in realm '{}'", String.join(",", clientRolesToRemove), clientId, username, realm);

                    roleRepository.removeClientRolesForUser(realm, username, clientId, foundClientRoles);
                }
            }
        }
    }
}
