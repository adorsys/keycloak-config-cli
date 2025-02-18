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

import de.adorsys.keycloak.config.exception.InvalidImportException;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.repository.ClientRepository;
import de.adorsys.keycloak.config.repository.GroupRepository;
import de.adorsys.keycloak.config.repository.RealmRepository;
import de.adorsys.keycloak.config.repository.RoleRepository;
import de.adorsys.keycloak.config.repository.UserRepository;
import de.adorsys.keycloak.config.util.CloneUtil;
import de.adorsys.keycloak.config.util.KeycloakUtil;
import org.keycloak.representations.idm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class UserImportService {
    private static final Logger logger = LoggerFactory.getLogger(UserImportService.class);

    private static final String[] IGNORED_PROPERTIES_FOR_UPDATE = {"realmRoles", "clientRoles", "serviceAccountClientId", "attributes"};
    private static final String USER_LABEL_FOR_INITIAL_CREDENTIAL = "initial";

    private final RealmRepository realmRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;
    private final ClientRepository clientRepository;

    private final ImportConfigProperties importConfigProperties;

    @Autowired
    public UserImportService(
            RealmRepository realmRepository, UserRepository userRepository,
            RoleRepository roleRepository,
            GroupRepository groupRepository,
            ClientRepository clientRepository, ImportConfigProperties importConfigProperties
    ) {
        this.realmRepository = realmRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.groupRepository = groupRepository;
        this.clientRepository = clientRepository;
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

        private UserImport(String realmName, UserRepresentation userToImport) {
            this.realmName = realmName;
            this.userToImport = userToImport;
        }

        public void importUser() {
            if (
                    // The service accounts shall not be taken into account
                    !StringUtils.hasLength(userToImport.getServiceAccountClientId())
                            && Boolean.TRUE.equals(realmRepository.get(realmName).isRegistrationEmailAsUsername())
            ) {
                if (
                        userToImport.getUsername() != null
                                && !Objects.equals(userToImport.getUsername(), userToImport.getEmail())
                ) {
                    String errorMessage = String.format(
                            "Invalid user '%s' in realm '%s': username (%s) and email (%s) "
                                    + "is different while 'email as username' is enabled on realm.",
                            userToImport.getUsername(), realmName, userToImport.getUsername(), userToImport.getEmail());
                    throw new InvalidImportException(errorMessage);
                }

                userToImport.setUsername(userToImport.getEmail());
            }

            Optional<UserRepresentation> maybeUser = userRepository.search(realmName, userToImport.getUsername());

            if (maybeUser.isEmpty() && userToImport.getEmail() != null) {
                maybeUser = userRepository.searchByAttributes(realmName, userToImport.getEmail(), userToImport.getFirstName(),
                        userToImport.getLastName());
            }

            if (maybeUser.isPresent()) {
                updateUser(maybeUser.get());
            } else {
                logger.debug("Create user '{}' in realm '{}'", userToImport.getUsername(), realmName);
                userRepository.create(realmName, userToImport);
            }

            handleRealmRoles();
            handleClientRoles();
            handleGroups();
        }

        private void updateUser(UserRepresentation existingUser) {
            UserRepresentation patchedUser = CloneUtil
                    .patch(existingUser, userToImport, IGNORED_PROPERTIES_FOR_UPDATE);

            if (importConfigProperties.getBehaviors().isSkipAttributesForFederatedUser() && patchedUser.getFederationLink() != null) {
                patchedUser.setAttributes(null);
            } else if (existingUser.getAttributes() != null && userToImport.getAttributes() != null) {
                patchedUser.setAttributes(userToImport.getAttributes());
            }

            if (patchedUser.getCredentials() != null) {
                // do not override password, if userLabel is set "initial"
                List<CredentialRepresentation> userCredentials = patchedUser.getCredentials().stream()
                        .filter(credentialRepresentation -> !Objects.equals(
                                credentialRepresentation.getUserLabel(), USER_LABEL_FOR_INITIAL_CREDENTIAL
                        ))
                        .toList();
                patchedUser.setCredentials(userCredentials.isEmpty() ? null : userCredentials);
            }

            if (!CloneUtil.deepEquals(existingUser, patchedUser, "access")) {
                logger.debug("Update user '{}' in realm '{}'", userToImport.getUsername(), realmName);
                userRepository.updateUser(realmName, patchedUser);
            } else {
                logger.debug("No need to update user '{}' in realm '{}'", userToImport.getUsername(), realmName);
            }
        }

        private void handleGroups() {
            List<String> userGroupsToUpdate = userToImport.getGroups();
            if (userGroupsToUpdate == null) {
                userGroupsToUpdate = Collections.emptyList();
            }

            // Unify group name & group path
            userGroupsToUpdate = userGroupsToUpdate
                    .stream().map(groupName -> groupName.startsWith("/") ? groupName : "/" + groupName)
                    .toList();

            List<String> existingUserGroups = userRepository.getGroups(realmName, userToImport)
                    .stream().map(GroupRepresentation::getPath)
                    .toList();

            handleGroupsToBeAdded(userGroupsToUpdate, existingUserGroups);
            handleGroupsToBeRemoved(userGroupsToUpdate, existingUserGroups);
        }

        private void handleGroupsToBeAdded(
                List<String> userGroupsToUpdate,
                List<String> existingUserGroupsToUpdate
        ) {
            List<String> groupsToAdd = searchForMissing(userGroupsToUpdate, existingUserGroupsToUpdate);
            if (groupsToAdd.isEmpty()) return;

            List<GroupRepresentation> groups = groupRepository.findGroupsByGroupPath(realmName, groupsToAdd);

            logger.debug("Add groups {} to user '{}' in realm '{}'",
                    groupsToAdd, userToImport.getUsername(), realmName);

            groupRepository.addGroupsToUser(realmName, userToImport.getUsername(), groups);
        }

        private void handleGroupsToBeRemoved(
                List<String> userGroupsToUpdate,
                List<String> existingUserGroupsToUpdate
        ) {
            List<String> groupsToDelete = searchForMissing(existingUserGroupsToUpdate, userGroupsToUpdate);
            if (groupsToDelete.isEmpty()) return;

            List<GroupRepresentation> groups = groupRepository.findGroupsByGroupPath(realmName, groupsToDelete);

            logger.debug("Remove groups {} from user '{}' in realm '{}'",
                    groupsToDelete, userToImport.getUsername(), realmName);

            groupRepository.removeGroupsFromUser(realmName, userToImport.getUsername(), groups);
        }

        private void handleRealmRoles() {
            List<String> usersRealmLevelRolesToUpdate = userToImport.getRealmRoles();
            if (usersRealmLevelRolesToUpdate == null) {
                usersRealmLevelRolesToUpdate = Collections.emptyList();
            }

            List<String> existingUsersRealmLevelRoles = roleRepository
                    .getUserRealmLevelRoles(realmName, userToImport.getUsername());

            handleRolesToBeAdded(usersRealmLevelRolesToUpdate, existingUsersRealmLevelRoles);
            handleRolesToBeRemoved(usersRealmLevelRolesToUpdate, existingUsersRealmLevelRoles);
        }

        private void handleRolesToBeAdded(List<String> usersRealmLevelRolesToUpdate, List<String> existingUsersRealmLevelRoles) {
            List<String> rolesToAdd = searchForMissing(usersRealmLevelRolesToUpdate, existingUsersRealmLevelRoles);
            if (rolesToAdd.isEmpty()) return;

            List<RoleRepresentation> realmRoles = roleRepository.searchRealmRoles(realmName, rolesToAdd);

            logger.debug("Add realm-level roles {} to user '{}' in realm '{}'",
                    rolesToAdd, userToImport.getUsername(), realmName);

            roleRepository.addRealmRolesToUser(realmName, userToImport.getUsername(), realmRoles);
        }

        private void handleRolesToBeRemoved(List<String> usersRealmLevelRolesToUpdate, List<String> existingUsersRealmLevelRoles) {
            List<String> rolesToDelete = searchForMissing(existingUsersRealmLevelRoles, usersRealmLevelRolesToUpdate);
            if (!importConfigProperties.getBehaviors().isRemoveDefaultRoleFromUser()) {
                rolesToDelete.remove("default-roles-" + realmName.toLowerCase());
            }

            if (rolesToDelete.isEmpty()) return;

            List<RoleRepresentation> realmRoles = roleRepository.searchRealmRoles(realmName, rolesToDelete);

            logger.debug("Remove realm-level roles {} from user '{}' in realm '{}'",
                    rolesToDelete, userToImport.getUsername(), realmName);

            roleRepository.removeRealmRolesForUser(realmName, userToImport.getUsername(), realmRoles);
        }

        private void handleClientRoles() {
            Map<String, List<String>> clientRolesToImport = Optional.ofNullable(userToImport.getClientRoles())
                    .orElseGet(Collections::emptyMap);
            Map<String, List<String>> existingClientsRoles = roleRepository
                    .getUserClientLevelRoles(realmName, userToImport.getUsername());

            for (Map.Entry<String, List<String>> existing : existingClientsRoles.entrySet()) {
                List<String> rolesToImport = clientRolesToImport.get(existing.getKey());

                if (rolesToImport == null) {
                    ClientRepresentation client = clientRepository.getByClientId(realmName, existing.getKey());
                    if (KeycloakUtil.isDefaultClient(client)) {
                        // Do not remove keycloak default client's roles when they are not in the configuration
                        continue;
                    }
                    rolesToImport = Collections.emptyList();
                }
                setupClientRoles(
                        existing.getKey(),
                        existing.getValue(),
                        rolesToImport);
            }
            for (Map.Entry<String, List<String>> toImport : clientRolesToImport.entrySet()) {
                if (!existingClientsRoles.containsKey(toImport.getKey())) {
                    setupClientRoles(
                            toImport.getKey(),
                            Collections.emptyList(),
                            toImport.getValue());
                }
            }
        }

        private void setupClientRoles(String clientId, List<String> existing, List<String> toImport) {
            ClientRoleImport clientRoleImport = new ClientRoleImport(clientId, existing, toImport);
            clientRoleImport.importClientRoles();
        }

        @SuppressWarnings("java:S6204")
        private List<String> searchForMissing(List<String> searchedFor, List<String> trawled) {
            return searchedFor.stream().filter(role -> !trawled.contains(role)).collect(Collectors.toList());
        }

        private class ClientRoleImport {
            private final String clientId;
            private final List<String> existingClientLevelRoles;
            private final List<String> clientRolesToImport;

            private ClientRoleImport(String clientId,
                                     List<String> existingClientLevelRoles,
                                     List<String> clientRolesToImport) {

                this.clientId = clientId;
                this.existingClientLevelRoles = existingClientLevelRoles;
                this.clientRolesToImport = clientRolesToImport;
            }

            public void importClientRoles() {
                handleClientRolesToBeAdded();
                handleClientRolesToBeRemoved();
            }

            private void handleClientRolesToBeAdded() {
                List<String> clientRolesToAdd = searchForMissing(clientRolesToImport, existingClientLevelRoles);
                if (clientRolesToAdd.isEmpty()) return;

                List<RoleRepresentation> clientRoles = roleRepository
                        .getClientRolesByName(realmName, clientId, clientRolesToAdd);

                logger.debug("Add client-level roles {} for client '{}' to user '{}' in realm '{}'",
                        clientRolesToAdd, clientId, userToImport.getUsername(), realmName);

                roleRepository.addClientRolesToUser(realmName, userToImport.getUsername(), clientId, clientRoles);
            }

            private void handleClientRolesToBeRemoved() {
                List<String> clientRolesToRemove = searchForMissing(existingClientLevelRoles, clientRolesToImport);
                if (clientRolesToRemove.isEmpty()) return;

                List<RoleRepresentation> clientRoles = roleRepository
                        .getClientRolesByName(realmName, clientId, clientRolesToRemove);

                logger.debug("Remove client-level roles {} for client '{}' from user '{}' in realm '{}'",
                        clientRolesToRemove, clientId, userToImport.getUsername(), realmName);

                roleRepository.removeClientRolesForUser(realmName, userToImport.getUsername(), clientId, clientRoles);
            }
        }
    }
}
