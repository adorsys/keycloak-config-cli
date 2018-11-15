package de.adorsys.keycloak.config.repository;

import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.util.Optional;

@Service
public class RoleRepository {

    private final RealmRepository realmRepository;

    @Autowired
    public RoleRepository(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    public Optional<RoleRepresentation> tryToFindRole(String realm, String name) {
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
}
