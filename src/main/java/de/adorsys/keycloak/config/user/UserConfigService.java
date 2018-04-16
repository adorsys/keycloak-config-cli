package de.adorsys.keycloak.config.user;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.List;

@Service
public class UserConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(UserConfigService.class);
    private final Keycloak keycloak;

    @Autowired
    public UserConfigService(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    public void handleUsers(String realmId, List<UserRepresentation> userConfigurations) {
        UsersResource usersResource = keycloak.realms().realm(realmId).users();
        for (UserRepresentation userRepresentation : userConfigurations) {
            handleUser(usersResource, realmId, userRepresentation);
        }
    }

    private void handleUser(UsersResource usersResource, String realmId, UserRepresentation userRepresentation) {
        List<UserRepresentation> foundUsers = usersResource.search(userRepresentation.getUsername());
        for(UserRepresentation foundUser : foundUsers) {
            LOG.debug("Deleting user '{}' in realm '{}'.", foundUser.getId(), realmId);
            usersResource.delete(foundUser.getId());
        }

        Response response = usersResource.create(userRepresentation);

        if (response.getStatus() < 400) {
            LOG.debug("Creating user '{}' in realm '{}'.", userRepresentation.getUsername(), realmId);
            keycloak.proxy(UserResource.class, response.getLocation());
        } else {
            throw new RuntimeException("Could not create user: " + userRepresentation.getUsername());
        }

        response.close();
    }
}
