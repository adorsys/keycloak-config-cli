package de.adorsys.keycloak.config.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.keycloak.config.model.KeycloakImport;
import de.adorsys.keycloak.config.model.RealmImport;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class KeycloakImportProvider {

    @Value("${import.path:#{null}}")
    private String importDirectoryPath;

    @Value("${import.file:#{null}}")
    private String importFilePath;

    private final ObjectMapper objectMapper;

    public KeycloakImportProvider(@Qualifier("json") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public KeycloakImport get() {
        KeycloakImport keycloakImport;

        if(Strings.isNotBlank(importFilePath)) {
            keycloakImport = readFromFile(importFilePath);
        } else if(Strings.isNotBlank(importDirectoryPath)) {
            keycloakImport = readFromDirectory(importDirectoryPath);
        } else {
            throw new RuntimeException("Either 'import.path' or 'import.file' has to be defined");
        }

        return keycloakImport;
    }

    private KeycloakImport readFromFile(String filename) {
        try {
            File configFile = new File(filename);
            if(!configFile.exists()) {
                throw new RuntimeException("Is not existing: " + filename);
            }
            if(configFile.isDirectory()) {
                throw new RuntimeException("Is a directory: " + filename);
            }

            return readRealmImportFromFile(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private KeycloakImport readFromDirectory(String filename) {
        try {
            File configDirectory = new File(filename);
            if(!configDirectory.exists()) {
                throw new RuntimeException("Is not existing: " + filename);
            }
            if(!configDirectory.isDirectory()) {
                throw new RuntimeException("Is not a directory: " + filename);
            }

            return readRealmImportsFromDirectory(configDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private KeycloakImport readRealmImportsFromDirectory(File importFilesDirectory) throws IOException {
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

    private KeycloakImport readRealmImportFromFile(File importFile) throws IOException {
        Map<String, RealmImport> realmImports = new HashMap<>();

        if(!importFile.isDirectory()) {
            RealmImport realmImport = objectMapper.readValue(importFile, RealmImport.class);
            realmImports.put(importFile.getName(), realmImport);
        }

        return new KeycloakImport(realmImports);
    }
}
