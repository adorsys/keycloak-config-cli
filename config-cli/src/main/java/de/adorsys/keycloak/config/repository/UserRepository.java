package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.util.ResponseUtil;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@Service
public class UserRepository {

    private final RealmRepository realmRepository;

    @Autowired
    public UserRepository(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    public Optional<UserRepresentation> tryToFindUser(String realm, String username) {
        Optional<UserRepresentation> maybeUser;
        List<UserRepresentation> foundUsers = realmRepository.loadRealm(realm).users().search(username);

        if(foundUsers.isEmpty()) {
            maybeUser = Optional.empty();
        } else {
            maybeUser = Optional.of(foundUsers.get(0));
        }

        return maybeUser;
    }

    public UserResource getUserResource(String realm, String username) {
        UserRepresentation foundUser = findUser(realm, username);
        return realmRepository.loadRealm(realm).users().get(foundUser.getId());
    }

    public UserRepresentation findUser(String realm, String username) throws KeycloakRepositoryException {
        List<UserRepresentation> foundUsers = realmRepository.loadRealm(realm).users().search(username);

        if(foundUsers.isEmpty()) {
            throw new KeycloakRepositoryException("Cannot find user '" + username + "' in realm '" + realm + "'");
        }

        return foundUsers.get(0);
    }

    public void create(String realm, UserRepresentation userToCreate) throws KeycloakRepositoryException {
        RealmResource realmResource = realmRepository.loadRealm(realm);
        UsersResource usersResource = realmResource.users();

        Response response = usersResource.create(userToCreate);

        ResponseUtil.throwOnError(response);
    }

    public void updateUser(String realm, UserRepresentation user) {
        UserResource userResource = getUserResource(realm, user.getUsername());
        userResource.update(user);
    }
}
