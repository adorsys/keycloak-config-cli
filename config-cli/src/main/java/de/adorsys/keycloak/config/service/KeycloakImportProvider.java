package de.adorsys.keycloak.config.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.keycloak.config.exception.InvalidImportException;
import de.adorsys.keycloak.config.model.KeycloakImport;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.service.checksum.ChecksumService;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class KeycloakImportProvider {

    @Value("${import.path:#{null}}")
    private String importDirectoryPath;

    @Value("${import.file:#{null}}")
    private String importFilePath;

    private final ObjectMapper objectMapper;

    private final ChecksumService checksumService;

    public KeycloakImportProvider(
            @Qualifier("json") ObjectMapper objectMapper,
            ChecksumService checksumService
    ) {
        this.objectMapper = objectMapper;
        this.checksumService = checksumService;
    }

    public KeycloakImport get() {
        KeycloakImport keycloakImport;

        if (Strings.isNotBlank(importFilePath)) {
            keycloakImport = readFromFile(importFilePath);
        } else if (Strings.isNotBlank(importDirectoryPath)) {
            keycloakImport = readFromDirectory(importDirectoryPath);
        } else {
            throw new InvalidImportException("Either 'import.path' or 'import.file' has to be defined");
        }

        return keycloakImport;
    }

    private KeycloakImport readFromFile(String filename) {
        File configFile = new File(filename);

        if (!configFile.exists()) {
            throw new InvalidImportException("Is not existing: " + filename);
        }
        if (configFile.isDirectory()) {
            throw new InvalidImportException("Is a directory: " + filename);
        }

        return readRealmImportFromFile(configFile);
    }

    private KeycloakImport readFromDirectory(String filename) {
        File configDirectory = new File(filename);

        if (!configDirectory.exists()) {
            throw new InvalidImportException("Is not existing: " + filename);
        }
        if (!configDirectory.isDirectory()) {
            throw new InvalidImportException("Is not a directory: " + filename);
        }

        return readRealmImportsFromDirectory(configDirectory);
    }

    public KeycloakImport readRealmImportsFromDirectory(File importFilesDirectory) {
        Map<String, RealmImport> realmImports = Optional.ofNullable(importFilesDirectory.listFiles())
                .map(Arrays::asList)
                .orElse(Collections.emptyList())
                .stream()
                .filter(File::isFile)
                .collect(Collectors.toMap(File::getName, this::readRealmImport));

        return new KeycloakImport(realmImports);
    }

    private KeycloakImport readRealmImportFromFile(File importFile) {
        Map<String, RealmImport> realmImports = new HashMap<>();

        if (!importFile.isDirectory()) {
            RealmImport realmImport = readRealmImport(importFile);
            realmImports.put(importFile.getName(), realmImport);
        }

        return new KeycloakImport(realmImports);
    }

    private RealmImport readRealmImport(File importFile) {
        RealmImport realmImport = readToRealmImport(importFile);

        String checksum = calculateChecksum(importFile);
        realmImport.setChecksum(checksum);

        return realmImport;
    }

    private RealmImport readToRealmImport(File importFile) {
        RealmImport realmImport;

        try {
            realmImport = objectMapper.readValue(importFile, RealmImport.class);
        } catch (IOException e) {
            throw new InvalidImportException(e);
        }

        return realmImport;
    }

    private String calculateChecksum(File importFile) {
        byte[] importFileInBytes = readRealmImportToBytes(importFile);
        return checksumService.checksum(importFileInBytes);
    }

    private byte[] readRealmImportToBytes(File importFile) {
        byte[] importFileInBytes;

        try {
            importFileInBytes = Files.readAllBytes(importFile.toPath());
        } catch (IOException e) {
            throw new InvalidImportException(e);
        }

        return importFileInBytes;
    }
}
