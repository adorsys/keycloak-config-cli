package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoleRepository {

    private final RealmRepository realmRepository;
    private final ClientRepository clientRepository;

    @Autowired
    public RoleRepository(RealmRepository realmRepository, ClientRepository clientRepository) {
        this.realmRepository = realmRepository;
        this.clientRepository = clientRepository;
    }

    public Optional<RoleRepresentation> tryToFindRealmRole(String realm, String name) {
        Optional<RoleRepresentation> maybeRole;

        RolesResource rolesResource = realmRepository.loadRealm(realm).roles();
        RoleResource roleResource = rolesResource.get(name);

        try {
            maybeRole = Optional.of(roleResource.toRepresentation());
        } catch(NotFoundException e) {
            maybeRole = Optional.empty();
        }

        return maybeRole;
    }

    public void createRealmRole(String realm, RoleRepresentation role) {
        RolesResource rolesResource = realmRepository.loadRealm(realm).roles();
        rolesResource.create(role);
    }

    public void updateRealmRole(String realm, RoleRepresentation roleToUpdate) {
        RoleResource roleResource = realmRepository.loadRealm(realm)
                .roles()
                .get(roleToUpdate.getName());

        roleResource.update(roleToUpdate);
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

    public List<RoleRepresentation> searchRealmRoles(String realm, List<String> roles){
        return roles.stream()
                .map(role -> realmRepository.loadRealm(realm)
                        .roles()
                        .get(role)
                        .toRepresentation()
                )
                .collect(Collectors.toList());
    }
}
