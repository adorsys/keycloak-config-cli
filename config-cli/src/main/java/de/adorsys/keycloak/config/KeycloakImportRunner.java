package de.adorsys.keycloak.config;

import de.adorsys.keycloak.config.model.KeycloakImport;
import de.adorsys.keycloak.config.service.KeycloakImportProvider;
import de.adorsys.keycloak.config.service.KeycloakImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class KeycloakImportRunner implements CommandLineRunner {

    private final KeycloakImportProvider keycloakImportProvider;
    private final KeycloakImportService keycloakImportService;

    @Autowired
    public KeycloakImportRunner(
            KeycloakImportProvider keycloakImportProvider,
            KeycloakImportService keycloakImportService
    ) {
        this.keycloakImportProvider = keycloakImportProvider;
        this.keycloakImportService = keycloakImportService;
    }

    @Override
    public void run(String... args) throws Exception {
        KeycloakImport keycloakImport = keycloakImportProvider.get();

        keycloakImportService.doImport(keycloakImport);

        return;
    }
}
