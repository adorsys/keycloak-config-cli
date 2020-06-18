/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2020 adorsys GmbH & Co. KG @ https://adorsys.de
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package de.adorsys.keycloak.config.provider;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.keycloak.config.exception.InvalidImportException;
import de.adorsys.keycloak.config.model.KeycloakImport;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.util.ChecksumUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class KeycloakImportProvider {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakImportProvider.class);

    private final ObjectMapper objectMapper;
    private final ImportConfigProperties importConfigProperties;

    public KeycloakImportProvider(
            ObjectMapper objectMapper,
            ImportConfigProperties importConfigProperties
    ) {
        this.objectMapper = objectMapper;
        this.importConfigProperties = importConfigProperties;

        this.objectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public KeycloakImport get() {
        KeycloakImport keycloakImport;

        String importFilePath = importConfigProperties.getPath();
        keycloakImport = readFromPath(importFilePath);

        return keycloakImport;
    }

    private KeycloakImport readFromPath(String path) {
        File configPath = new File(path);

        if (!configPath.exists() || !configPath.canRead()) {
            throw new InvalidImportException("import.path does not exists: " + configPath.getAbsolutePath());
        }

        if (configPath.isDirectory()) {
            return readRealmImportsFromDirectory(configPath);
        }

        return readRealmImportFromFile(configPath);
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

        RealmImport realmImport = readRealmImport(importFile);
        realmImports.put(importFile.getName(), realmImport);

        return new KeycloakImport(realmImports);
    }

    private RealmImport readRealmImport(File importFile) {
        logger.info("Importing file '{}'", importFile.getAbsoluteFile());

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
        return ChecksumUtil.checksum(importFileInBytes);
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
