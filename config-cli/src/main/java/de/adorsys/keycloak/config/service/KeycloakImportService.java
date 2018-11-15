package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.KeycloakImport;
import de.adorsys.keycloak.config.model.RealmImport;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class KeycloakImportService {

    private final Keycloak keycloak;


    @Autowired
    public KeycloakImportService(
            Keycloak keycloak
    ) {
        this.keycloak = keycloak;
    }

    public void doImport(KeycloakImport keycloakImport) {
        Map<String, RealmImport> realmImports = keycloakImport.getRealmImports();

        for(Map.Entry<String, RealmImport> realmImport : realmImports.entrySet()) {
            RealmImportService realmImportService = new RealmImportService(keycloak, realmImport.getValue());
            realmImportService.doImport();
        }
    }
}
