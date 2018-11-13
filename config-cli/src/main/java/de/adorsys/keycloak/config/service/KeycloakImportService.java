package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.KeycloakImport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KeycloakImportService {

    private final RealmsImportService realmsImportService;

    @Autowired
    public KeycloakImportService(RealmsImportService realmsImportService) {
        this.realmsImportService = realmsImportService;
    }

    public void doImport(KeycloakImport keycloakImport) {
        realmsImportService.doImport(keycloakImport.getRealmImports());
    }
}
