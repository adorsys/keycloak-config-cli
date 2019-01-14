package de.adorsys.keycloak.config.util;

import de.adorsys.keycloak.config.service.KeycloakProvider;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

@Component
public class KeycloakRepository {

    private final KeycloakProvider keycloakProvider;

    @Autowired
    public KeycloakRepository(KeycloakProvider keycloakProvider) {
        this.keycloakProvider = keycloakProvider;
    }

    public UserRepresentation getUser(String realmName, String username) {
        List<UserRepresentation> foundUsers = keycloakProvider.get().realm(realmName)
                .users()
                .list()
                .stream()
                .filter(u -> u.getUsername().equals(username))
                .collect(Collectors.toList());

        assertThat(foundUsers, hasSize(1));

        return foundUsers.get(0);
    }

    public List<String> getUserRealmLevelRoles(String realmName, String username) {
        UserRepresentation user = getUser(realmName, username);
        UserResource userResource = keycloakProvider.get()
                .realm(realmName)
                .users()
                .get(user.getId());

        List<RoleRepresentation> roles = userResource.roles()
                .realmLevel()
                .listEffective();

        return roles.stream().map(RoleRepresentation::getName).collect(Collectors.toList());
    }

    public ClientRepresentation getClient(String realmName, String clientId) {
        List<ClientRepresentation> foundClients = keycloakProvider.get()
                .realm(realmName)
                .clients()
                .findByClientId(clientId);

        assertThat(foundClients, hasSize(1));

        return foundClients.get(0);
    }

    public List<String> getUserClientLevelRoles(String realmName, String username, String clientId) {
        UserRepresentation user = getUser(realmName, username);
        ClientRepresentation client = getClient(realmName, clientId);

        UserResource userResource = keycloakProvider.get()
                .realm(realmName)
                .users()
                .get(user.getId());

        List<RoleRepresentation> roles = userResource.roles()
                .clientLevel(client.getId())
                .listEffective();

        return roles.stream().map(RoleRepresentation::getName).collect(Collectors.toList());
    }

    public boolean isClientRoleExisting(String realm, String clientId, String role) {
        ClientRepresentation client = getClient(realm, clientId);

        List<RoleRepresentation> clientRoles = keycloakProvider.get()
                .realm(realm)
                .clients().get(client.getId())
                .roles()
                .list();

        long count = clientRoles.stream()
                .filter(r -> Objects.equals(r.getName(), role))
                .count();

        return count > 0;
    }
}
