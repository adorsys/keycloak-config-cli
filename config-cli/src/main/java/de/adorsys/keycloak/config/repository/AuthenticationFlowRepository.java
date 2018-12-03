package de.adorsys.keycloak.config.repository;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationFlowRepository {

    private final RealmRepository realmRepository;

    @Autowired
    public AuthenticationFlowRepository(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    public AuthenticationManagementResource get(String realm) {
        RealmResource realmResource = realmRepository.loadRealm(realm);
        return realmResource.flows();
    }
}
