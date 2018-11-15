package de.adorsys.keycloak.config.repository;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RealmRepository {

    private final Keycloak keycloak;

    @Autowired
    public RealmRepository(Keycloak keycloak) {
        this.keycloak = keycloak;
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
        return keycloak.realms().realm(realm);
    }

}
