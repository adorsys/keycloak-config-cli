/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2021 adorsys GmbH & Co. KG @ https://adorsys.com
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
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.adorsys.keycloak.config.exception.InvalidImportException;
import de.adorsys.keycloak.config.model.KeycloakImport;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.util.ChecksumUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class KeycloakImportProvider {
    private final ResourceLoader resourceLoader;
    private final Collection<ResourceExtractor> resourceExtractors;
    private final ImportConfigProperties importConfigProperties;

    private StringSubstitutor interpolator = null;
    private static final Logger logger = LoggerFactory.getLogger(KeycloakImportProvider.class);

    @Autowired
    public KeycloakImportProvider(
            ResourceLoader resourceLoader,
            Collection<ResourceExtractor> resourceExtractors,
            ImportConfigProperties importConfigProperties
    ) {
        this.resourceLoader = resourceLoader;
        this.resourceExtractors = resourceExtractors;
        this.importConfigProperties = importConfigProperties;

        if (importConfigProperties.isVarSubstitution()) {
            this.interpolator = StringSubstitutor.createInterpolator()
                    .setEnableSubstitutionInVariables(importConfigProperties.isVarSubstitutionInVariables())
                    .setEnableUndefinedVariableException(
                            importConfigProperties.isVarSubstitutionUndefinedThrowsExceptions());
        }
    }

    public KeycloakImport get() {
        KeycloakImport keycloakImport;

        String importFilePath = importConfigProperties.getPath();
        keycloakImport = readFromPath(importFilePath);

        return keycloakImport;
    }

    KeycloakImport readFromPath(String path) {
        // backward compatibility to correct a possible missing prefix "file:" in path
        if (!ResourceUtils.isUrl(path)) {
            path = "file:" + path;
        }

        Resource resource = resourceLoader.getResource(path);
        Optional<ResourceExtractor> maybeMatchingExtractor = resourceExtractors.stream()
                .filter(r -> {
                    try {
                        return r.canHandleResource(resource);
                    } catch (IOException e) {
                        return false;
                    }
                }).findFirst();
        if (!maybeMatchingExtractor.isPresent()) {
            logger.error("No resource extractor found to handle config property import.path! Check your settings.");
            return null;
        }

        try {
            return readRealmImportsFromResource(maybeMatchingExtractor.get().extract(resource));
        } catch (IOException ioex) {
            throw new InvalidImportException("import.path does not exists: " + path);
        }
    }

    KeycloakImport readRealmImportsFromResource(Collection<File> importResources) {
        Map<String, RealmImport> realmImports = importResources.stream()
                // https://stackoverflow.com/a/52130074/8087167
                .collect(Collectors.toMap(
                        File::getName,
                        this::readRealmImport,
                        (u, v) -> {
                            throw new IllegalStateException(String.format("Duplicate key %s", u));
                        },
                        TreeMap::new
                ));
        return new KeycloakImport(realmImports);
    }

    public KeycloakImport readRealmImportFromFile(File importFile) {
        Map<String, RealmImport> realmImports = new HashMap<>();

        RealmImport realmImport = readRealmImport(importFile);
        realmImports.put(importFile.getName(), realmImport);

        return new KeycloakImport(realmImports);
    }

    private RealmImport readRealmImport(File importFile) {
        logger.info("Importing file '{}'", importFile.getAbsoluteFile());
        return readToRealmImport(importFile);
    }

    private RealmImport readToRealmImport(File importFile) {
        ImportConfigProperties.ImportFileType fileType = importConfigProperties.getFileType();

        ObjectMapper objectMapper;

        switch (fileType) {
            case YAML:
                objectMapper = new ObjectMapper(new YAMLFactory());
                break;
            case JSON:
                objectMapper = new ObjectMapper();
                break;
            case AUTO:
                String fileExt = FilenameUtils.getExtension(importFile.getName());
                switch (fileExt) {
                    case "yaml":
                    case "yml":
                        objectMapper = new ObjectMapper(new YAMLFactory());
                        break;
                    case "json":
                        objectMapper = new ObjectMapper();
                        break;
                    default:
                        throw new InvalidImportException("Unknown file extension: " + fileExt);
                }
                break;
            default:
                throw new InvalidImportException("Unknown import file type: " + fileType.toString());
        }

        objectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        byte[] importFileInBytes = readRealmImportToBytes(importFile);
        String importConfig = new String(importFileInBytes, StandardCharsets.UTF_8);

        if (importConfigProperties.isVarSubstitution()) {
            importConfig = interpolator.replace(importConfig);
        }

        String checksum = ChecksumUtil.checksum(importConfig.getBytes(StandardCharsets.UTF_8));

        try {
            RealmImport realmImport = objectMapper.readValue(importConfig, RealmImport.class);
            realmImport.setChecksum(checksum);

            return realmImport;
        } catch (IOException e) {
            throw new InvalidImportException(e);
        }
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
