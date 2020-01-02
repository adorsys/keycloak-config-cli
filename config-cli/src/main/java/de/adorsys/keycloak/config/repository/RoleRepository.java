package de.adorsys.keycloak.config.repository;

import com.google.common.collect.Sets;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.util.ToStringUtils;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoleRepository {
    private static final Logger logger = LoggerFactory.getLogger(RoleRepository.class);

    private final RealmRepository realmRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    @Autowired
    public RoleRepository(
            RealmRepository realmRepository,
            ClientRepository clientRepository,
            UserRepository userRepository
    ) {
        this.realmRepository = realmRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
    }

    public Optional<RoleRepresentation> tryToFindRealmRole(String realm, String name) {
        Optional<RoleRepresentation> maybeRole;

        RolesResource rolesResource = realmRepository.loadRealm(realm).roles();
        RoleResource roleResource = rolesResource.get(name);

        try {
            maybeRole = Optional.of(roleResource.toRepresentation());
        } catch (NotFoundException e) {
            maybeRole = Optional.empty();
        }

        return maybeRole;
    }

    public void createRealmRole(String realm, RoleRepresentation role) {
        logger.debug("create realm {} role: {}", realm, ToStringUtils.jsonToString(role));
        RolesResource rolesResource = realmRepository.loadRealm(realm).roles();
        rolesResource.create(role);
        updateRoleComposites(realm, role);
    }

    public void updateRealmRole(String realm, RoleRepresentation roleToUpdate) {
        logger.debug("update realm {} role: {}", realm, ToStringUtils.jsonToString(roleToUpdate));
        RoleResource roleResource = realmRepository.loadRealm(realm)
                .roles()
                .get(roleToUpdate.getName());

        roleResource.update(roleToUpdate);
        updateRoleComposites(realm, roleToUpdate);
    }

    private void updateRoleComposites(String realm, RoleRepresentation role) {
        RoleResource roleResource = realmRepository.loadRealm(realm).roles().get(role.getName());
        Set<String> existingRoleComposites = roleResource.getRoleComposites().stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
        Set<String> newlyAddedRoleComposites = new HashSet<>();
        if (role.getComposites() != null && role.getComposites().getRealm() != null) {
            newlyAddedRoleComposites = role.getComposites().getRealm();
        }

        // find newly added roles that not in existing roles
        Set<String> toAdd = Sets.difference(newlyAddedRoleComposites, existingRoleComposites);
        logger.debug("updating composite role: {}, composite roles to add: {}", role.getName(), Arrays.toString(toAdd.toArray()));
        // add to composites
        if (!toAdd.isEmpty()) {
            roleResource.addComposites(findRealmRoles(realm,toAdd));
        }

        // find existing roles that not in newly added roles
        Set<String> toRemove = Sets.difference(existingRoleComposites, newlyAddedRoleComposites);
        logger.debug("updating composite role: {}, composite roles to remove: {}", role.getName(), Arrays.toString(toRemove.toArray()));
        // remove from composites
        if (!toRemove.isEmpty()) {
            roleResource.deleteComposites(findRealmRoles(realm, toRemove));
        }
    }

    public RoleRepresentation findRealmRole(String realm, String roleName) {
        return tryToFindRealmRole(realm, roleName)
                .orElseThrow(
                        () -> new KeycloakRepositoryException(
                                "Cannot find realm role '" + roleName + "' within realm '" + realm + "'"
                        )
                );
    }

    public List<RoleRepresentation> findRealmRoles(String realm, Collection<String> roles) {
        return roles.stream()
                .map(role -> findRealmRole(realm, role))
                .collect(Collectors.toList());
    }

    public Optional<RoleRepresentation> tryToFindClientRole(String realm, String clientId, String roleName) {
        ClientRepresentation client = clientRepository.getClient(realm, clientId);
        RealmResource realmResource = realmRepository.loadRealm(realm);

        List<RoleRepresentation> clientRoles = realmResource.clients()
                .get(client.getId())
                .roles()
                .list();

        return clientRoles.stream()
                .filter(r -> r.getName().equals(roleName))
                .findFirst();
    }

    public List<RoleRepresentation> searchClientRoles(String realm, String clientId, List<String> roles) {
        ClientRepresentation foundClient = clientRepository.getClient(realm, clientId);

        ClientResource clientResource = realmRepository.loadRealm(realm)
                .clients()
                .get(foundClient.getId());

        return roles.stream()
                .map(role -> clientResource.roles()
                        .get(role)
                        .toRepresentation()
                ).collect(Collectors.toList());
    }

    public void createClientRole(String realm, String clientId, RoleRepresentation role) {
        ClientRepresentation client = clientRepository.getClient(realm, clientId);
        RolesResource rolesResource = realmRepository.loadRealm(realm)
                .clients()
                .get(client.getId())
                .roles();

        rolesResource.create(role);
    }

    public void updateClientRole(String realm, String clientId, RoleRepresentation roleToUpdate) {
        ClientRepresentation client = clientRepository.getClient(realm, clientId);

        RoleResource roleResource = realmRepository.loadRealm(realm)
                .clients()
                .get(client.getId())
                .roles()
                .get(roleToUpdate.getName());

        roleResource.update(roleToUpdate);
    }

    public List<RoleRepresentation> searchRealmRoles(String realm, List<String> roles) {
        return roles.stream()
                .map(role -> realmRepository.loadRealm(realm)
                        .roles()
                        .get(role)
                        .toRepresentation()
                )
                .collect(Collectors.toList());
    }

    public List<String> getUserRealmLevelRoles(String realm, String username) {
        UserRepresentation user = userRepository.findUser(realm, username);
        UserResource userResource = realmRepository.loadRealm(realm)
                .users()
                .get(user.getId());

        List<RoleRepresentation> roles = userResource.roles()
                .realmLevel()
                .listEffective();

        return roles.stream().map(RoleRepresentation::getName).collect(Collectors.toList());
    }

    public void addRealmRolesToUser(String realm, String username, List<RoleRepresentation> realmRoles) {
        UserResource userResource = userRepository.getUserResource(realm, username);
        userResource.roles().realmLevel().add(realmRoles);
    }

    public void removeRealmRolesForUser(String realm, String username, List<RoleRepresentation> realmRoles) {
        UserResource userResource = userRepository.getUserResource(realm, username);
        userResource.roles().realmLevel().remove(realmRoles);
    }

    public void addClientRolesToUser(String realm, String username, String clientId, List<RoleRepresentation> clientRoles) {
        ClientRepresentation client = clientRepository.getClient(realm, clientId);
        UserResource userResource = userRepository.getUserResource(realm, username);

        RoleScopeResource userClientRoles = userResource.roles()
                .clientLevel(client.getId());

        userClientRoles.add(clientRoles);
    }

    public void removeClientRolesForUser(String realm, String username, String clientId, List<RoleRepresentation> clientRoles) {
        ClientRepresentation client = clientRepository.getClient(realm, clientId);
        UserResource userResource = userRepository.getUserResource(realm, username);

        RoleScopeResource userClientRoles = userResource.roles()
                .clientLevel(client.getId());

        userClientRoles.remove(clientRoles);
    }

    public List<String> getUserClientLevelRoles(String realm, String username, String clientId) {
        ClientRepresentation client = clientRepository.getClient(realm, clientId);
        UserResource userResource = userRepository.getUserResource(realm, username);

        List<RoleRepresentation> roles = userResource.roles()
                .clientLevel(client.getId())
                .listEffective();

        return roles.stream().map(RoleRepresentation::getName).collect(Collectors.toList());
    }
}
