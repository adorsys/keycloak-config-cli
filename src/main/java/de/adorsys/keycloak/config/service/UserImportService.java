/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2020 adorsys GmbH & Co. KG @ https://adorsys.com
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
import de.adorsys.keycloak.config.repository.GroupRepository;
import de.adorsys.keycloak.config.repository.RoleRepository;
import de.adorsys.keycloak.config.repository.UserRepository;
import de.adorsys.keycloak.config.util.CloneUtil;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class UserImportService {
    private static final Logger logger = LoggerFactory.getLogger(UserImportService.class);

    private static final String[] IGNORED_PROPERTIES_FOR_UPDATE = {"realmRoles", "clientRoles"};

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;

    private final ImportConfigProperties importConfigProperties;

    @Autowired
    public UserImportService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            GroupRepository groupRepository, ImportConfigProperties importConfigProperties
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.groupRepository = groupRepository;
        this.importConfigProperties = importConfigProperties;
    }

    public void doImport(RealmImport realmImport) {
        List<UserRepresentation> users = realmImport.getUsers();

        if (users == null) {
            return;
        }

        if (users.isEmpty()) {
            logger.warn("Purging users isn't supported in keycloak-config-cli!");
            return;
        }

        Consumer<UserRepresentation> loop = user -> importUser(realmImport.getRealm(), user);
        if (importConfigProperties.isParallel()) {
            users.parallelStream().forEach(loop);
        } else {
            users.forEach(loop);
        }
    }

    private void importUser(String realmName, UserRepresentation user) {
        UserImport userImport = new UserImport(realmName, user);
        userImport.importUser();
    }

    private class UserImport {
        private final String realmName;
        private final UserRepresentation userToImport;
        private final String username;

        private UserImport(String realmName, UserRepresentation userToImport) {
            this.realmName = realmName;
            this.userToImport = userToImport;
            this.username = userToImport.getUsername();
        }

        public void importUser() {
            Optional<UserRepresentation> maybeUser = userRepository.search(realmName, username);

            if (maybeUser.isPresent()) {
                updateUser(maybeUser.get());
            } else {
                logger.debug("Create user '{}' in realm '{}'", username, realmName);
                userRepository.create(realmName, userToImport);
            }

            handleRealmRoles();
            handleClientRoles();
            handleGroups();
        }

        private void updateUser(UserRepresentation existingUser) {
            UserRepresentation patchedUser = CloneUtil.deepPatch(existingUser, userToImport, IGNORED_PROPERTIES_FOR_UPDATE);
            if (userToImport.getAttributes() != null) {
                patchedUser.setAttributes(userToImport.getAttributes());
            }

            if (!CloneUtil.deepEquals(existingUser, patchedUser, "access")) {
                logger.debug("Update user '{}' in realm '{}'", username, realmName);
                userRepository.updateUser(realmName, patchedUser);
            } else {
                logger.debug("No need to update user '{}' in realm '{}'", username, realmName);
            }
        }

        private void handleGroups() {
            List<String> userGroupsToUpdate = userToImport.getGroups();
            if (userGroupsToUpdate == null) {
                userGroupsToUpdate = Collections.emptyList();
            }

            List<String> existingUserGroups = userRepository.getGroups(realmName, userToImport)
                    .stream().map(GroupRepresentation::getName).collect(Collectors.toList());

            handleGroupsToBeAdded(userGroupsToUpdate, existingUserGroups);
            handleGroupsToBeRemoved(userGroupsToUpdate, existingUserGroups);
        }

        private void handleGroupsToBeAdded(List<String> userGroupsToUpdate, List<String> existingUserGroupsToUpdate) {
            List<String> groupsToAdd = searchForMissing(userGroupsToUpdate, existingUserGroupsToUpdate);
            if (groupsToAdd.isEmpty()) return;

            List<GroupRepresentation> groups = groupRepository.findGroups(realmName, groupsToAdd);

            logger.debug("Add groups {} to user '{}' in realm '{}'", groupsToAdd, username, realmName);

            groupRepository.addGroupsToUser(realmName, username, groups);
        }

        private void handleGroupsToBeRemoved(List<String> userGroupsToUpdate, List<String> existingUserGroupsToUpdate) {
            List<String> groupsToDelete = searchForMissing(existingUserGroupsToUpdate, userGroupsToUpdate);
            if (groupsToDelete.isEmpty()) return;

            List<GroupRepresentation> groups = groupRepository.findGroups(realmName, groupsToDelete);

            logger.debug("Remove groups {} from user '{}' in realm '{}'", groupsToDelete, username, realmName);

            groupRepository.removeGroupsFromUser(realmName, username, groups);
        }

        private void handleRealmRoles() {
            List<String> usersRealmLevelRolesToUpdate = userToImport.getRealmRoles();
            if (usersRealmLevelRolesToUpdate == null) {
                usersRealmLevelRolesToUpdate = Collections.emptyList();
            }

            List<String> existingUsersRealmLevelRoles = roleRepository.getUserRealmLevelRoles(realmName, username);

            handleRolesToBeAdded(usersRealmLevelRolesToUpdate, existingUsersRealmLevelRoles);
            handleRolesToBeRemoved(usersRealmLevelRolesToUpdate, existingUsersRealmLevelRoles);
        }

        private void handleRolesToBeAdded(List<String> usersRealmLevelRolesToUpdate, List<String> existingUsersRealmLevelRoles) {
            List<String> rolesToAdd = searchForMissing(usersRealmLevelRolesToUpdate, existingUsersRealmLevelRoles);
            if (rolesToAdd.isEmpty()) return;

            List<RoleRepresentation> realmRoles = roleRepository.searchRealmRoles(realmName, rolesToAdd);

            logger.debug("Add realm-level roles {} to user '{}' in realm '{}'", rolesToAdd, username, realmName);

            roleRepository.addRealmRolesToUser(realmName, username, realmRoles);
        }

        private void handleRolesToBeRemoved(List<String> usersRealmLevelRolesToUpdate, List<String> existingUsersRealmLevelRoles) {
            List<String> rolesToDelete = searchForMissing(existingUsersRealmLevelRoles, usersRealmLevelRolesToUpdate);
            if (rolesToDelete.isEmpty()) return;

            List<RoleRepresentation> realmRoles = roleRepository.searchRealmRoles(realmName, rolesToDelete);

            logger.debug("Remove realm-level roles {} from user '{}' in realm '{}'", rolesToDelete, username, realmName);

            roleRepository.removeRealmRolesForUser(realmName, username, realmRoles);
        }

        private void handleClientRoles() {
            Map<String, List<String>> clientRolesToImport = userToImport.getClientRoles();
            if (clientRolesToImport == null) return;

            for (Map.Entry<String, List<String>> clientRoles : clientRolesToImport.entrySet()) {
                setupClientRoles(clientRoles);
            }
        }

        private void setupClientRoles(Map.Entry<String, List<String>> clientRoles) {
            String clientId = clientRoles.getKey();

            ClientRoleImport clientRoleImport = new ClientRoleImport(clientId);
            clientRoleImport.importClientRoles();
        }

        private List<String> searchForMissing(List<String> searchedFor, List<String> trawled) {
            return searchedFor.stream().filter(role -> !trawled.contains(role)).collect(Collectors.toList());
        }

        private class ClientRoleImport {
            private final String clientId;
            private final List<String> existingClientLevelRoles;
            private final List<String> clientRolesToImport;

            private ClientRoleImport(String clientId) {
                this.clientId = clientId;
                this.existingClientLevelRoles = roleRepository.getUserClientLevelRoles(realmName, username, clientId);

                Map<String, List<String>> clientsRolesToImport = userToImport.getClientRoles();
                this.clientRolesToImport = clientsRolesToImport.get(clientId);
            }

            public void importClientRoles() {
                handleClientRolesToBeAdded();
                handleClientRolesToBeRemoved();
            }

            private void handleClientRolesToBeAdded() {
                List<String> clientRolesToAdd = searchForMissing(clientRolesToImport, existingClientLevelRoles);
                if (clientRolesToAdd.isEmpty()) return;

                List<RoleRepresentation> foundClientRoles = roleRepository.getClientRoles(realmName, clientId, clientRolesToAdd);

                logger.debug("Add client-level roles {} for client '{}' to user '{}' in realm '{}'", clientRolesToAdd, clientId, username, realmName);

                roleRepository.addClientRolesToUser(realmName, username, clientId, foundClientRoles);
            }

            private void handleClientRolesToBeRemoved() {
                List<String> clientRolesToRemove = searchForMissing(existingClientLevelRoles, clientRolesToImport);
                if (clientRolesToRemove.isEmpty()) return;

                List<RoleRepresentation> foundClientRoles = roleRepository.getClientRoles(realmName, clientId, clientRolesToRemove);

                logger.debug("Remove client-level roles {} for client '{}' from user '{}' in realm '{}'", clientRolesToRemove, clientId, username, realmName);

                roleRepository.removeClientRolesForUser(realmName, username, clientId, foundClientRoles);
            }
        }
    }
}
