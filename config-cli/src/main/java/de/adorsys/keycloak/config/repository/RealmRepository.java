package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.service.KeycloakProvider;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RealmRepository {

    private final KeycloakProvider keycloakProvider;

    @Autowired
    public RealmRepository(KeycloakProvider keycloakProvider) {
        this.keycloakProvider = keycloakProvider;
    }

    public Optional<RealmResource> tryToLoadRealm(String realm) {
        Optional<RealmResource> loadedRealm;

        try {
            RealmResource foundRealm = loadRealm(realm);

            // check here if realm is present, otherwise this method throws an NotFoundException
            foundRealm.toRepresentation();

            loadedRealm = Optional.of(foundRealm);
        } catch (javax.ws.rs.NotFoundException e) {
            loadedRealm = Optional.empty();
        }

        return loadedRealm;
    }

    public RealmResource loadRealm(String realm) {
        return keycloakProvider.get().realms().realm(realm);
    }

    public RealmRepresentation getRealm(String realm) {
        return loadRealm(realm).toRepresentation();
    }

    public void updateRealm(RealmRepresentation realmToUpdate) {
        loadRealm(realmToUpdate.getRealm()).update(realmToUpdate);
    }
}
