package de.adorsys.keycloak.config;

import de.adorsys.keycloak.config.model.KeycloakImport;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.service.KeycloakImportProvider;
import de.adorsys.keycloak.config.service.RealmImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class Application implements CommandLineRunner {
    private final KeycloakImportProvider keycloakImportProvider;
    private final RealmImportService realmImportService;

    @Autowired
    public Application(
            KeycloakImportProvider keycloakImportProvider,
            RealmImportService realmImportService
    ) {
        this.keycloakImportProvider = keycloakImportProvider;
        this.realmImportService = realmImportService;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) {
        KeycloakImport keycloakImport = keycloakImportProvider.get();

        Map<String, RealmImport> realmImports = keycloakImport.getRealmImports();

        for (Map.Entry<String, RealmImport> realmImport : realmImports.entrySet()) {
            realmImportService.doImport(realmImport.getValue());
        }
    }
}
