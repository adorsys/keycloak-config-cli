package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

public class RealmUserImportService {
    private static final Logger logger = LoggerFactory.getLogger(RealmsImportService.class);

    private final RealmImport realmImport;
    private final RealmResource realmResource;

    public RealmUserImportService(RealmImport realmImport, RealmResource realmResource) {
        this.realmImport = realmImport;
        this.realmResource = realmResource;
    }

    public void doImport() {
        List<UserRepresentation> users = realmImport.getUsers();

        if(users != null) {
            importUsers(users);
        }
    }

    private void importUsers(List<UserRepresentation> users) {
        for(UserRepresentation user : users) {
            Optional<UserRepresentation> maybeUser = tryToFindUser(user.getUsername());

            if(maybeUser.isPresent()) {
                updateUser(maybeUser.get(), user);
            } else {
                createUser(user);
            }
        }
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
        UserResource userResource = realmResource.users().get(existingUser.getId());
        UserRepresentation patchedUser = CloneUtils.deepPatch(existingUser, userToUpdate);

        userResource.update(patchedUser);
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
}
