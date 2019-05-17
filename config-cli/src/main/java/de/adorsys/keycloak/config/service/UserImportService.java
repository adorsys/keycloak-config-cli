package de.adorsys.keycloak.config.service;

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
import java.util.StringJoiner;
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

    public void importUser(String realm, UserRepresentation user) {
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

                debugLogAddedRealmRoles(rolesToAdd);

                roleRepository.addRealmRolesToUser(realm, username, realmRoles);
            }
        }

        private void handleRolesToBeRemoved(List<String> usersRealmLevelRolesToUpdate, List<String> existingUsersRealmLevelRoles) {
            List<String> rolesToDelete = searchForMissingRoles(existingUsersRealmLevelRoles, usersRealmLevelRolesToUpdate);

            if (!rolesToDelete.isEmpty()) {
                List<RoleRepresentation> realmRoles = roleRepository.searchRealmRoles(realm, rolesToDelete);

                debugLogRemovedRealmRoles(rolesToDelete);

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

                    debugLogAddedClientRoles(clientId, clientRolesToAdd);

                    roleRepository.addClientRolesToUser(realm, username, clientId, foundClientRoles);
                }
            }

            private void handleClientRolesToBeRemoved() {
                List<String> clientRolesToRemove = searchForMissingRoles(existingClientLevelRoles, clientRolesToImport);

                if (!clientRolesToRemove.isEmpty()) {
                    List<RoleRepresentation> foundClientRoles = roleRepository.searchClientRoles(realm, clientId, clientRolesToRemove);

                    debugLogRemovedClientRoles(clientId, clientRolesToRemove);

                    roleRepository.removeClientRolesForUser(realm, username, clientId, foundClientRoles);
                }
            }

            private void debugLogAddedClientRoles(String clientId, List<String> clientRolesToAdd) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Add client-level roles [{}] for client '{}' to user '{}' in realm '{}'", joinRoles(clientRolesToAdd), clientId, username, realm);
                }
            }

            private void debugLogRemovedClientRoles(String clientId, List<String> clientRolesToRemove) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Remove client-level roles [{}] for client '{}' from user '{}' in realm '{}'", joinRoles(clientRolesToRemove), clientId, username, realm);
                }
            }
        }

        private List<String> searchForMissingRoles(List<String> rolesToBeSearchedFor, List<String> rolesToBeTrawled) {
            return rolesToBeSearchedFor.stream()
                    .filter(role -> !rolesToBeTrawled.contains(role))
                    .collect(Collectors.toList());
        }

        private void debugLogAddedRealmRoles(List<String> realmRolesToAdd) {
            if (logger.isDebugEnabled()) {
                StringJoiner rolesJoiner = joinRoles(realmRolesToAdd);

                logger.debug("Add realm-level roles [{}] to user '{}' in realm '{}'", rolesJoiner, username, realm);
            }
        }

        private void debugLogRemovedRealmRoles(List<String> realmRolesToRemove) {
            if (logger.isDebugEnabled()) {
                StringJoiner rolesJoiner = joinRoles(realmRolesToRemove);

                logger.debug("Remove realm-level roles [{}] from user '{}' in realm '{}'", rolesJoiner, username, realm);
            }
        }

        private StringJoiner joinRoles(List<String> clientRolesToRemove) {
            StringJoiner rolesJoiner = new StringJoiner(",");

            for (String clientRole : clientRolesToRemove) {
                rolesJoiner.add(clientRole);
            }

            return rolesJoiner;
        }
    }
}
