package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.service.KeycloakProvider;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.WebApplicationException;
import java.util.Optional;

@Service
public class RealmRepository {

    private final KeycloakProvider keycloakProvider;

    @Autowired
    public RealmRepository(KeycloakProvider keycloakProvider) {
        this.keycloakProvider = keycloakProvider;
    }

    public boolean exists(String realm) {
        return tryToLoadRealm(realm).isPresent();
    }

    private Optional<RealmRepresentation> tryToLoadRealm(String realm) {
        Optional<RealmRepresentation> maybeRealm;

        try {
            RealmResource realmResource = loadRealm(realm);

            // check here if realm is present, otherwise this method throws an NotFoundException
            RealmRepresentation foundRealm = realmResource.toRepresentation();

            maybeRealm = Optional.of(foundRealm);
        } catch (javax.ws.rs.NotFoundException e) {
            maybeRealm = Optional.empty();
        }

        return maybeRealm;
    }

    public RealmResource loadRealm(String realm) {
        return keycloakProvider.get().realms().realm(realm);
    }

    public void create(RealmRepresentation realmToCreate) {
        Keycloak keycloak = keycloakProvider.get();
        RealmsResource realmsResource = keycloak.realms();

        try {
            realmsResource.create(realmToCreate);
        } catch (WebApplicationException error) {
            throw new KeycloakRepositoryException(
                    "Cannot create realm '" + realmToCreate.getRealm() + "'",
                    error
            );
        }
    }

    public RealmRepresentation get(String realm) {
        return loadRealm(realm).toRepresentation();
    }

    public void update(RealmRepresentation realmToUpdate) {
        loadRealm(realmToUpdate.getRealm()).update(realmToUpdate);
    }

    public RealmRepresentation partialExport(String realm) {
        return loadRealm(realm).partialExport(true, true);
    }
}
