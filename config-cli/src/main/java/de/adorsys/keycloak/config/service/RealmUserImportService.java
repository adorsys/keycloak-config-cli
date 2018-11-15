package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RealmUserImportService {
    private static final Logger logger = LoggerFactory.getLogger(RealmUserImportService.class);

    private final RealmResource realmResource;
    private final RealmImport realmImport;

    private UserResource userResource;

    public RealmUserImportService(RealmResource realmResource, RealmImport realmImport) {
        this.realmResource = realmResource;
        this.realmImport = realmImport;
    }

    public void importUser(UserRepresentation user) {
        Optional<UserRepresentation> maybeUser = tryToFindUser(user.getUsername());

        if(maybeUser.isPresent()) {
            updateUser(maybeUser.get(), user);
        } else {
            createUser(user);
        }

        handleRealmRoles(user);
        handleClientRoles(user);
    }

    private Optional<UserRepresentation> tryToFindUser(String username) {
        Optional<UserRepresentation> maybeUser;
        List<UserRepresentation> foundUsers = realmResource.users().search(username);

        if(foundUsers.isEmpty()) {
            maybeUser = Optional.empty();
        } else {
            maybeUser = Optional.of(foundUsers.get(0));
        }

        return maybeUser;
    }

    private void createUser(UserRepresentation userToCreate) {
        Response response = realmResource.users().create(userToCreate);

        if (response.getStatus() < 400) {
            logger.debug("Creating user '{}' in realm '{}'.", userToCreate.getUsername(), realmImport.getRealm());
        } else {
            logger.error("Cannot create user '{}' in realm '{}'.", userToCreate.getUsername(), realmImport.getRealm());
        }

        response.close();
    }

    private void updateUser(UserRepresentation existingUser, UserRepresentation userToUpdate) {
        UserResource userResource = getUserResource(userToUpdate.getUsername());
        UserRepresentation patchedUser = CloneUtils.deepPatch(existingUser, userToUpdate);

        userResource.update(patchedUser);
    }

    private void handleRealmRoles(UserRepresentation userToUpdate) {
        List<String> realmRolesToUpdate = userToUpdate.getRealmRoles();
        List<RoleRepresentation> realmRoles = searchRealmRoles(realmRolesToUpdate);

        UserResource userResource = getUserResource(userToUpdate.getUsername());
        userResource.roles().realmLevel().add(realmRoles);
    }

    private List<RoleRepresentation> searchRealmRoles(List<String> roles){
        return roles.stream()
                .map(role -> realmResource.roles()
                        .get(role).toRepresentation()
                ).collect(Collectors.toList());
    }

    private void handleClientRoles(UserRepresentation userToCreate) {
        Map<String, List<String>> clientRolesToImport = userToCreate.getClientRoles();

        for (Map.Entry<String, List<String>> clientRoles : clientRolesToImport.entrySet()) {
            setupClientRole(userToCreate, clientRoles);
        }
    }

    private void setupClientRole(UserRepresentation userToCreate, Map.Entry<String, List<String>> clientRoles) {
        String clientId = clientRoles.getKey();

        UserResource userResource = getUserResource(userToCreate.getUsername());
        List<RoleRepresentation> foundClientRoles = searchClientRoles(clientId, clientRoles.getValue());

        RoleMappingResource userRoles = userResource.roles();
        ClientRepresentation client = getClient(clientId);
        RoleScopeResource userClientRoles = userRoles.clientLevel(client.getId());

        userClientRoles.add(foundClientRoles);
    }

    private List<RoleRepresentation> searchClientRoles(String clientId, List<String> roles){
        return roles.stream()
                .map(role -> getClientResource(clientId)
                        .roles()
                        .get(role)
                        .toRepresentation()
                ).collect(Collectors.toList());
    }

    private ClientResource getClientResource(String clientId) {
        ClientRepresentation foundClient = getClient(clientId);
        return realmResource.clients().get(foundClient.getId());
    }

    private ClientRepresentation getClient(String clientId) {
        List<ClientRepresentation> foundClients = realmResource.clients().findByClientId(clientId);

        if(foundClients.isEmpty()) {
            throw new RuntimeException("Cannot find client by clientId '" + clientId + "'");
        }

        return foundClients.get(0);
    }

    private UserResource getUserResource(String username) {
        if(userResource == null) {
            UserRepresentation foundUser = findUser(username);
            userResource = realmResource.users().get(foundUser.getId());
        }

        return userResource;
    }

    private UserRepresentation findUser(String username) {
        List<UserRepresentation> foundUsers = realmResource.users().search(username);
        return foundUsers.get(0);
    }
}
