package de.adorsys.keycloak.config.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.keycloak.config.model.KeycloakImport;
import de.adorsys.keycloak.config.model.RealmImport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class KeycloakImportProvider {

    @Value("${import.path}")
    private String importDirectoryPath;

    private final ObjectMapper objectMapper;

    public KeycloakImportProvider(@Qualifier("json") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public KeycloakImport get() {
        try {
            File configDirectory = new File(importDirectoryPath);
            if(!configDirectory.exists()) {
                throw new RuntimeException("Is not existing: " + importDirectoryPath);
            }
            if(!configDirectory.isDirectory()) {
                throw new RuntimeException("Is no directory: " + importDirectoryPath);
            }

            return readRealmImports(configDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private KeycloakImport readRealmImports(File importFilesDirectory) throws IOException {
        Map<String, RealmImport> realmImports = new HashMap<>();

        File[] files = importFilesDirectory.listFiles();
        if(files != null) {
            for (File importFile : files) {
                if(!importFile.isDirectory()) {
                    RealmImport realmImport = objectMapper.readValue(importFile, RealmImport.class);
                    realmImports.put(importFile.getName(), realmImport);
                }
            }
        }

        return new KeycloakImport(realmImports);
    }
}
