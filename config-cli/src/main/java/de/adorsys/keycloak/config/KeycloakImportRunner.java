package de.adorsys.keycloak.config;

import de.adorsys.keycloak.config.model.KeycloakImport;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.service.KeycloakImportProvider;
import de.adorsys.keycloak.config.service.RealmImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class KeycloakImportRunner implements CommandLineRunner {

    private final KeycloakImportProvider keycloakImportProvider;
    private final RealmImportService realmImportService;

    @Autowired
    public KeycloakImportRunner(
            KeycloakImportProvider keycloakImportProvider,
            RealmImportService realmImportService
    ) {
        this.keycloakImportProvider = keycloakImportProvider;
        this.realmImportService = realmImportService;
    }

    @Override
    public void run(String... args) throws Exception {
        KeycloakImport keycloakImport = keycloakImportProvider.get();

        Map<String, RealmImport> realmImports = keycloakImport.getRealmImports();

        for(Map.Entry<String, RealmImport> realmImport : realmImports.entrySet()) {
            realmImportService.doImport(realmImport.getValue());
        }
    }
}
