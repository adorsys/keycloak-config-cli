package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.repository.RealmRepository;
import de.adorsys.keycloak.config.repository.RoleRepository;
import de.adorsys.keycloak.config.repository.UserRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.*;

@Service
public class UserImportService {
    private static final Logger logger = LoggerFactory.getLogger(UserImportService.class);

    private static final String[] IGNORED_PROPERTIES_FOR_UPDATE = {"realmRoles", "clientRoles"};

    private final RealmRepository realmRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public UserImportService(
            RealmRepository realmRepository,
            UserRepository userRepository,
            RoleRepository roleRepository
    ) {
        this.realmRepository = realmRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public void importUser(String realm, UserRepresentation user) {
        Optional<UserRepresentation> maybeUser = userRepository.tryToFindUser(realm, user.getUsername());

        if(maybeUser.isPresent()) {
            updateUser(realm, maybeUser.get(), user);
        } else {
            createUser(realm, user);
        }

        handleRealmRoles(realm, user);
        handleClientRoles(realm, user);
    }

    private void createUser(String realm, UserRepresentation userToCreate) {
        Response response = realmRepository.loadRealm(realm).users().create(userToCreate);

        if (response.getStatus() < 400) {
            if(logger.isDebugEnabled()) logger.debug("Creating user '{}' in realm '{}'.", userToCreate.getUsername(), realm);
        } else {
            if(logger.isDebugEnabled()) logger.error("Cannot create user '{}' in realm '{}'.", userToCreate.getUsername(), realm);
        }

        response.close();
    }

    private void updateUser(String realm, UserRepresentation existingUser, UserRepresentation userToUpdate) {
        UserRepresentation patchedUser = CloneUtils.deepPatch(existingUser, userToUpdate, IGNORED_PROPERTIES_FOR_UPDATE);

        if(!CloneUtils.deepEquals(existingUser, patchedUser)) {
            if(logger.isDebugEnabled()) logger.debug("Updating user '{}' in realm '{}'...", userToUpdate.getUsername(), realm);
            userRepository.updateUser(realm, patchedUser);
        } else {
            if(logger.isDebugEnabled()) logger.debug("No need to update user '{}' in realm '{}'.", userToUpdate.getUsername(), realm);
        }
    }

    private void handleRealmRoles(String realm, UserRepresentation userToUpdate) {
        List<String> usersRealmLevelRolesToUpdate = userToUpdate.getRealmRoles();
        List<String> existingUsersRealmLevelRoles = roleRepository.getUserRealmLevelRoles(realm, userToUpdate.getUsername());

        handleRolesToBeAdded(realm, userToUpdate.getUsername(), usersRealmLevelRolesToUpdate, existingUsersRealmLevelRoles);
        handleRolesToBeRemoved(realm, userToUpdate.getUsername(), usersRealmLevelRolesToUpdate, existingUsersRealmLevelRoles);
    }

    private void handleRolesToBeAdded(String realm, String username, List<String> usersRealmLevelRolesToUpdate, List<String> existingUsersRealmLevelRoles) {
        List<String> rolesToAdd = new ArrayList<>();

        for (String usersRealmLevelRoleToUpdate : usersRealmLevelRolesToUpdate) {
            if(!existingUsersRealmLevelRoles.contains(usersRealmLevelRoleToUpdate)) {
                rolesToAdd.add(usersRealmLevelRoleToUpdate);
            }
        }

        if(!rolesToAdd.isEmpty()) {
            List<RoleRepresentation> realmRoles = roleRepository.searchRealmRoles(realm, rolesToAdd);

            debugLogAddedRealmRoles(realm, username, rolesToAdd);

            roleRepository.addRealmRolesToUser(realm, username, realmRoles);
        }
    }

    private void handleRolesToBeRemoved(String realm, String username, List<String> usersRealmLevelRolesToUpdate, List<String> existingUsersRealmLevelRoles) {
        List<String> rolesToDelete = new ArrayList<>();

        for (String existingUsersRealmLevelRole : existingUsersRealmLevelRoles) {
            if(!usersRealmLevelRolesToUpdate.contains(existingUsersRealmLevelRole)) {
                rolesToDelete.add(existingUsersRealmLevelRole);
            }
        }

        if(!rolesToDelete.isEmpty()) {
            List<RoleRepresentation> realmRoles = roleRepository.searchRealmRoles(realm, rolesToDelete);

            debugLogRemovedRealmRoles(realm, username, rolesToDelete);

            roleRepository.removeRealmRolesForUser(realm, username, realmRoles);
        }
    }

    private void handleClientRoles(String realm, UserRepresentation userToCreate) {
        Map<String, List<String>> clientRolesToImport = userToCreate.getClientRoles();

        for (Map.Entry<String, List<String>> clientRoles : clientRolesToImport.entrySet()) {
            setupClientRoles(realm, userToCreate, clientRoles);
        }
    }

    private void setupClientRoles(String realm, UserRepresentation userToImport, Map.Entry<String, List<String>> clientRoles) {
        String clientId = clientRoles.getKey();
        String username = userToImport.getUsername();

        List<String> existingClientLevelRoles = roleRepository.getUserClientLevelRoles(realm, username, clientId);
        Map<String, List<String>> clientsRolesToImport = userToImport.getClientRoles();
        List<String> clientRolesToImport = clientsRolesToImport.get(clientId);

        handleClientRolesToBeAdded(realm, username, clientId, existingClientLevelRoles, clientRolesToImport);
        handleClientRolesToBeRemoved(realm, username, clientId, existingClientLevelRoles, clientRolesToImport);
    }

    private void handleClientRolesToBeAdded(String realm, String username, String clientId, List<String> existingClientLevelRoles, List<String> clientRolesToImport) {
        List<String> clientRolesToAdd = new ArrayList<>();

        for (String clientRoleToImport : clientRolesToImport) {
            if(!existingClientLevelRoles.contains(clientRoleToImport)) {
                clientRolesToAdd.add(clientRoleToImport);
            }
        }

        if(!clientRolesToAdd.isEmpty()) {
            List<RoleRepresentation> foundClientRoles = roleRepository.searchClientRoles(realm, clientId, clientRolesToAdd);

            debugLogAddedClientRoles(realm, username, clientId, clientRolesToAdd);

            roleRepository.addClientRolesToUser(realm, username, clientId, foundClientRoles);
        }
    }

    private void handleClientRolesToBeRemoved(String realm, String username, String clientId, List<String> existingClientLevelRoles, List<String> clientRolesToImport) {
        List<String> clientRolesToRemove = new ArrayList<>();

        for (String existingClientLevelRole : existingClientLevelRoles) {
            if(!clientRolesToImport.contains(existingClientLevelRole)) {
                clientRolesToRemove.add(existingClientLevelRole);
            }
        }

        if(!clientRolesToRemove.isEmpty()) {
            List<RoleRepresentation> foundClientRoles = roleRepository.searchClientRoles(realm, clientId, clientRolesToRemove);

            debugLogRemovedClientRoles(realm, username, clientId, clientRolesToRemove);

            roleRepository.removeClientRolesForUser(realm, username, clientId, foundClientRoles);
        }
    }

    private void debugLogAddedRealmRoles(String realm, String username, List<String> realmRolesToAdd) {
        if(logger.isDebugEnabled()) {
            StringJoiner rolesJoiner = joinRoles(realmRolesToAdd);

            logger.debug("Add realm-level roles [{}] to user '{}' in realm '{}'", rolesJoiner, username, realm);
        }
    }

    private void debugLogRemovedRealmRoles(String realm, String username, List<String> realmRolesToRemove) {
        if(logger.isDebugEnabled()) {
            StringJoiner rolesJoiner = joinRoles(realmRolesToRemove);

            logger.debug("Remove realm-level roles [{}] from user '{}' in realm '{}'", rolesJoiner, username, realm);
        }
    }

    private void debugLogAddedClientRoles(String realm, String username, String clientId, List<String> clientRolesToAdd) {
        if(logger.isDebugEnabled()) {
            logger.debug("Add client-level roles [{}] for client '{}' to user '{}' in realm '{}'", joinRoles(clientRolesToAdd), clientId, username, realm);
        }
    }

    private void debugLogRemovedClientRoles(String realm, String username, String clientId, List<String> clientRolesToRemove) {
        if(logger.isDebugEnabled()) {
            logger.debug("Remove client-level roles [{}] for client '{}' from user '{}' in realm '{}'", joinRoles(clientRolesToRemove), clientId, username, realm);
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
