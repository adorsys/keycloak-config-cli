package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import org.keycloak.admin.client.Keycloak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RealmsImportService {
    private static final Logger logger = LoggerFactory.getLogger(RealmsImportService.class);

    private final Keycloak keycloak;

    @Autowired
    public RealmsImportService(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    public void doImport(Map<String, RealmImport> realmImports) {
        for(Map.Entry<String, RealmImport> realmImport : realmImports.entrySet()) {
            RealmImportService realmImportService = new RealmImportService(keycloak, realmImport.getValue());
            realmImportService.doImport();
        }
    }
}
