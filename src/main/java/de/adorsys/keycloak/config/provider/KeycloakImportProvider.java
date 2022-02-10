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
import de.adorsys.keycloak.config.exception.InvalidImportException;
import de.adorsys.keycloak.config.model.KeycloakImport;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.util.ChecksumUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class KeycloakImportProvider {
    private final ResourceLoader resourceLoader;
    private final Collection<ResourceExtractor> resourceExtractors;
    private final ImportConfigProperties importConfigProperties;

    private StringSubstitutor interpolator = null;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Autowired
    public KeycloakImportProvider(
            Environment environment,
            ResourceLoader resourceLoader,
            Collection<ResourceExtractor> resourceExtractors,
            ImportConfigProperties importConfigProperties
    ) {
        this.resourceLoader = resourceLoader;
        this.resourceExtractors = resourceExtractors;
        this.importConfigProperties = importConfigProperties;

        if (importConfigProperties.isVarSubstitution()) {
            String prefix = importConfigProperties.getVarSubstitutionPrefix();
            String suffix = importConfigProperties.getVarSubstitutionSuffix();

            StringLookup variableResolver = StringLookupFactory.INSTANCE.interpolatorStringLookup(
                    StringLookupFactory.INSTANCE.functionStringLookup(environment::getProperty)
            );

            this.interpolator = StringSubstitutor.createInterpolator()
                    .setVariableResolver(variableResolver)
                    .setVariablePrefix(prefix)
                    .setVariableSuffix(suffix)
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

    public KeycloakImport readFromPath(String path) {
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
            throw new InvalidImportException("No resource extractor found to handle config property import.path=" + path + "! Check your settings.");
        }

        try {
            return readRealmImportsFromResource(maybeMatchingExtractor.get().extract(resource));
        } catch (IOException e) {
            throw new InvalidImportException("import.path does not exists: " + path, e);
        }
    }

    private KeycloakImport readRealmImportsFromResource(Collection<File> importResources) {
        Map<String, List<RealmImport>> realmImports = importResources.stream()
                // https://stackoverflow.com/a/52130074/8087167
                .collect(Collectors.toMap(
                        File::getAbsolutePath,
                        this::readRealmImport,
                        (u, v) -> {
                            throw new IllegalStateException(String.format("Duplicate key %s", u));
                        },
                        TreeMap::new
                ));
        return new KeycloakImport(realmImports);
    }

    public KeycloakImport readRealmImportFromFile(File importFile) {
        Map<String, List<RealmImport>> realmImports = new HashMap<>();

        List<RealmImport> realmImport = readRealmImport(importFile);
        realmImports.put(importFile.getAbsolutePath(), realmImport);

        return new KeycloakImport(realmImports);
    }

    private List<RealmImport> readRealmImport(File importFile) {
        String importConfig;

        try {
            importConfig = FileUtils.readFileToString(importFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new InvalidImportException(e);
        }

        if (importConfigProperties.isVarSubstitution()) {
            importConfig = interpolator.replace(importConfig);
        }

        String checksum = ChecksumUtil.checksum(importConfig.getBytes(StandardCharsets.UTF_8));

        ImportConfigProperties.ImportFileType fileType = importConfigProperties.getFileType();

        List<RealmImport> realmImports;

        switch (fileType) {
            case YAML:
                realmImports = readYaml(importConfig);
                break;
            case JSON:
                realmImports = readJson(importConfig);
                break;
            case AUTO:
                String fileExt = FilenameUtils.getExtension(importFile.getName());
                switch (fileExt) {
                    case "yaml":
                    case "yml":
                        realmImports = readYaml(importConfig);
                        break;
                    case "json":
                        realmImports = readJson(importConfig);
                        break;
                    default:
                        throw new InvalidImportException("Unknown file extension: " + fileExt);
                }
                break;
            default:
                throw new InvalidImportException("Unknown import file type: " + fileType);
        }

        realmImports.forEach(realmImport -> realmImport.setChecksum(checksum));

        return realmImports;
    }

    private List<RealmImport> readJson(String data) {
        try {
            RealmImport realmImport = OBJECT_MAPPER.readValue(data, RealmImport.class);

            return Collections.singletonList(realmImport);
        } catch (IOException e) {
            throw new InvalidImportException(e);
        }
    }

    private List<RealmImport> readYaml(String data) {
        List<RealmImport> realmImports = new ArrayList<>();

        Yaml yaml = new Yaml();
        Iterable<Object> yamlDocuments = yaml.loadAll(data);

        try {
            for (Object yamlDocument : yamlDocuments) {
                realmImports.add(OBJECT_MAPPER.convertValue(yamlDocument, RealmImport.class));
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidImportException(e.getMessage());
        }

        return realmImports;
    }
}
