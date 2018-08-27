package de.adorsys.keycloak.config.user;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
			UserResource proxy = keycloak.proxy(UserResource.class, response.getLocation());
			addRealmRolesToUser(realmId,userRepresentation,proxy);
        } else {
            throw new RuntimeException("Could not create user: " + userRepresentation.getUsername());
        }

        response.close();
    }

	private void addRealmRolesToUser(String realmId, UserRepresentation userRepresentation,UserResource proxy){
		LOG.info("Adding realm roles '{}'",userRepresentation.getRealmRoles());
		List<RoleRepresentation> transformedRoles = transformRoles(keycloak.realm(realmId),
				userRepresentation.getRealmRoles());
		proxy.roles().realmLevel().add(transformedRoles);
		LOG.info("Added {} roles to '{}'",transformedRoles.size(), userRepresentation.getUsername());

	}

	private List<RoleRepresentation> transformRoles(RealmResource realmResource, List<String> roles){
		return roles.stream().map(role -> realmResource.roles()
				.get(role).toRepresentation()).collect(Collectors.toCollection(ArrayList::new));
	}
}
