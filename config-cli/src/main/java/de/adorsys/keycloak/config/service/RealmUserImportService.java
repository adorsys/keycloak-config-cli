package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.repository.ClientRepository;
import de.adorsys.keycloak.config.repository.RealmRepository;
import de.adorsys.keycloak.config.repository.UserRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RealmUserImportService {
    private static final Logger logger = LoggerFactory.getLogger(RealmUserImportService.class);

    private final RealmRepository realmRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;

    @Autowired
    public RealmUserImportService(
            RealmRepository realmRepository,
            UserRepository userRepository,
            ClientRepository clientRepository
    ) {
        this.realmRepository = realmRepository;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
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
            logger.debug("Creating user '{}' in realm '{}'.", userToCreate.getUsername(), realm);
        } else {
            logger.error("Cannot create user '{}' in realm '{}'.", userToCreate.getUsername(), realm);
        }

        response.close();
    }

    private void updateUser(String realm, UserRepresentation existingUser, UserRepresentation userToUpdate) {
        UserResource userResource = getUserResource(realm, userToUpdate.getUsername());
        UserRepresentation patchedUser = CloneUtils.deepPatch(existingUser, userToUpdate);

        userResource.update(patchedUser);
    }

    private void handleRealmRoles(String realm, UserRepresentation userToUpdate) {
        List<String> realmRolesToUpdate = userToUpdate.getRealmRoles();
        List<RoleRepresentation> realmRoles = searchRealmRoles(realm, realmRolesToUpdate);

        UserResource userResource = getUserResource(realm, userToUpdate.getUsername());
        userResource.roles().realmLevel().add(realmRoles);
    }

    private List<RoleRepresentation> searchRealmRoles(String realm, List<String> roles){
        return roles.stream()
                .map(role -> realmRepository.loadRealm(realm).roles()
                        .get(role).toRepresentation()
                ).collect(Collectors.toList());
    }

    private void handleClientRoles(String realm, UserRepresentation userToCreate) {
        Map<String, List<String>> clientRolesToImport = userToCreate.getClientRoles();

        for (Map.Entry<String, List<String>> clientRoles : clientRolesToImport.entrySet()) {
            setupClientRole(realm, userToCreate, clientRoles);
        }
    }

    private void setupClientRole(String realm, UserRepresentation userToCreate, Map.Entry<String, List<String>> clientRoles) {
        String clientId = clientRoles.getKey();

        UserResource userResource = getUserResource(realm, userToCreate.getUsername());
        List<RoleRepresentation> foundClientRoles = searchClientRoles(realm, clientId, clientRoles.getValue());

        RoleMappingResource userRoles = userResource.roles();
        ClientRepresentation client = clientRepository.getClient(realm, clientId);
        RoleScopeResource userClientRoles = userRoles.clientLevel(client.getId());

        userClientRoles.add(foundClientRoles);
    }

    private UserResource getUserResource(String realm, String username) {
        return userRepository.getUserResource(realm, username);
    }

    private List<RoleRepresentation> searchClientRoles(String realm, String clientId, List<String> roles){
        return roles.stream()
                .map(role -> clientRepository.getClientResource(realm, clientId)
                        .roles()
                        .get(role)
                        .toRepresentation()
                ).collect(Collectors.toList());
    }
}
