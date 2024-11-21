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
import de.adorsys.keycloak.config.model.ImportResource;
import de.adorsys.keycloak.config.model.KeycloakImport;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.PathMatcher;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class KeycloakImportProvider {
    private final PathMatchingResourcePatternResolver patternResolver;
    private final ImportConfigProperties importConfigProperties;

    private StringSubstitutor interpolator = null;

    private static final Logger logger = LoggerFactory.getLogger(KeycloakImportProvider.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Autowired
    public KeycloakImportProvider(
            Environment environment,
            PathMatchingResourcePatternResolver patternResolver,
            ImportConfigProperties importConfigProperties
    ) {
        this.patternResolver = patternResolver;
        this.importConfigProperties = importConfigProperties;

        if (importConfigProperties.getVarSubstitution().isEnabled()) {
            setupVariableSubstitution(environment);
        }
    }

    private void setupVariableSubstitution(Environment environment) {
        StringLookup variableResolver = StringLookupFactory.INSTANCE.interpolatorStringLookup(
                StringLookupFactory.INSTANCE.functionStringLookup(environment::getProperty)
        );

        this.interpolator = StringSubstitutor.createInterpolator()
                .setVariableResolver(variableResolver)
                .setVariablePrefix(importConfigProperties.getVarSubstitution().getPrefix())
                .setVariableSuffix(importConfigProperties.getVarSubstitution().getSuffix())
                .setEnableSubstitutionInVariables(importConfigProperties.getVarSubstitution().isNested())
                .setEnableUndefinedVariableException(importConfigProperties.getVarSubstitution().isUndefinedIsError());
    }

    public KeycloakImport readFromLocations(String... locations) {
        return readFromLocations(Arrays.asList(locations));
    }

    public KeycloakImport readFromLocations(Collection<String> locations) {
        Map<String, Map<String, List<RealmImport>>> realmImports = new LinkedHashMap<>();

        for (String location : locations) {
            logger.debug("Loading file location '{}'", location);
            String resourceLocation = prepareResourceLocation(location);

            Resource[] resources;
            try {
                resources = this.patternResolver.getResources(resourceLocation);
            } catch (IOException e) {
                throw new InvalidImportException("Unable to proceed location '" + location + "': " + e.getMessage(), e);
            }

            resources = Arrays.stream(resources).filter(this::filterExcludedResources).toArray(Resource[]::new);

            if (resources.length == 0) {
                throw new InvalidImportException("No files matching '" + location + "'!");
            }

            // Import Pipe
            Map<String, List<RealmImport>> realmImport = Arrays.stream(resources)
                    .map(this::readResource)
                    .filter(this::filterEmptyResources)
                    .sorted(Map.Entry.comparingByKey())
                    .map(this::substituteImportResource)
                    .map(this::readRealmImportFromImportResource)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue, LinkedHashMap::new));

            realmImports.put(location, realmImport);
        }

        return new KeycloakImport(realmImports);
    }

    private boolean filterExcludedResources(Resource resource) {
        if (!resource.isFile()) {
            return true;
        }

        File file;

        try {
            file = resource.getFile();
        } catch (IOException ignored) {
            return true;
        }

        if (file.isDirectory()) {
            return false;
        }

        if (!this.importConfigProperties.getFiles().isIncludeHiddenFiles() && (file.isHidden() || FileUtils.hasHiddenAncestorDirectory(file))) {
            return false;
        }

        PathMatcher pathMatcher = patternResolver.getPathMatcher();
        return importConfigProperties.getFiles().getExcludes()
                .stream()
                .map(pattern -> pattern.startsWith("**") ? "/" + pattern : pattern)
                .map(pattern -> !pattern.startsWith("/**") ? "/**" + pattern : pattern)
                .map(pattern -> !pattern.startsWith("/") ? "/" + pattern : pattern)
                .noneMatch(pattern -> {
                    boolean match = pathMatcher.match(pattern, file.getPath());
                    if (match) {
                        logger.debug("Excluding resource file '{}' (match {})", file.getPath(), pattern);
                        return true;
                    }
                    return false;
                });
    }

    private ImportResource readResource(Resource resource) {
        logger.debug("Loading file '{}'", resource.getFilename());

        try {
            resource = setupAuthentication(resource);
            try (InputStream inputStream = resource.getInputStream()) {
                return new ImportResource(resource.getURI().toString(), new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new InvalidImportException("Unable to proceed resource '" + resource + "': " + e.getMessage(), e);
        } finally {
            Authenticator.setDefault(null);
        }
    }

    private boolean filterEmptyResources(ImportResource resource) {
        return !resource.getValue().isEmpty();
    }

    private ImportResource substituteImportResource(ImportResource importResource) {
        if (importConfigProperties.getVarSubstitution().isEnabled()) {
            importResource.setValue(interpolator.replace(importResource.getValue()));
        }

        return importResource;
    }

    private Pair<String, List<RealmImport>> readRealmImportFromImportResource(ImportResource resource) {
        String location = resource.getFilename();
        String content = resource.getValue();
        String contentChecksum = DigestUtils.sha256Hex(content);

        if (logger.isTraceEnabled()) {
            logger.trace(content);
        }

        List<RealmImport> realmImports;
        try {
            realmImports = readContent(content);
        } catch (Exception e) {
            throw new InvalidImportException("Unable to parse file '" + location + "': " + e.getMessage(), e);
        }
        realmImports.forEach(realmImport -> {
            realmImport.setChecksum(contentChecksum);
            realmImport.setSource(location);
        });

        return new ImmutablePair<>(location, realmImports);
    }

    private List<RealmImport> readContent(String content) {
        List<RealmImport> realmImports = new ArrayList<>();

        Yaml yaml = new Yaml();
        Iterable<Object> yamlDocuments = yaml.loadAll(content);

        for (Object yamlDocument : yamlDocuments) {
            realmImports.add(OBJECT_MAPPER.convertValue(yamlDocument, RealmImport.class));
        }

        return realmImports;
    }

    private String prepareResourceLocation(String location) {
        String importLocation = location;

        importLocation = importLocation.replaceFirst("^zip:", "jar:");

        // backward compatibility to correct a possible missing prefix "file:" in path
        if (!importLocation.contains(":")) {
            importLocation = "file:" + importLocation;
        }
        return importLocation;
    }

    private Resource setupAuthentication(Resource resource) throws IOException {
        String userInfo;

        try {
            userInfo = resource.getURL().getUserInfo();
        } catch (IOException e) {
            return resource;
        }

        if (userInfo == null) return resource;

        String[] userInfoSplit = userInfo.split(":");

        if (userInfoSplit.length != 2) return resource;

        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userInfoSplit[0], userInfoSplit[1].toCharArray());
            }
        });

        // Mask AuthInfo
        String location = resource.getURI().toString().replace(userInfo + "@", "***@");
        return new UrlResource(location);
    }
}
