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
import de.adorsys.keycloak.config.service.script.JavaScriptEvaluator;
import de.adorsys.keycloak.config.service.script.ScriptEvaluator;
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
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class KeycloakImportProvider {
    private final PathMatchingResourcePatternResolver patternResolver;
    private final ImportConfigProperties importConfigProperties;
    private final Environment environment;

    private StringSubstitutor interpolator = null;
    private ScriptEvaluator scriptEvaluator = null;

    private static final Pattern JS_PATTERN = Pattern.compile("\\$\\$\\{javascript:(.*?)\\}", Pattern.DOTALL);

    // File lookups may intentionally provide YAML structure (for example, list items).
    // Expand these before parsing YAML; other substitutions remain scalar-based.
    private static final Pattern YAML_FILE_PATTERN = Pattern.compile(
            "\\$\\(file:UTF-8:[^()]*+(?:\\([^()]*+\\)[^()]*+)*+\\)"
    );

    private static final Logger logger = LoggerFactory.getLogger(KeycloakImportProvider.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Autowired
    public KeycloakImportProvider(
            Environment environment,
            PathMatchingResourcePatternResolver patternResolver,
            ImportConfigProperties importConfigProperties
    ) {
        this.environment = environment;
        this.patternResolver = patternResolver;
        this.importConfigProperties = importConfigProperties;

        if (importConfigProperties.getVarSubstitution().isEnabled()) {
            setupVariableSubstitution(environment);
        }

        if (importConfigProperties.getVarSubstitution().isScriptEvaluationEnabled()) {
            this.scriptEvaluator = new JavaScriptEvaluator();
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
        if (isYamlResource(importResource)) {
            importResource.setValue(substituteYamlFileIncludes(importResource.getValue()));
        } else {
            importResource.setValue(substituteContent(importResource.getValue()));
        }

        return importResource;
    }

    private String substituteYamlFileIncludes(String content) {
        if (!importConfigProperties.getVarSubstitution().isEnabled()) {
            return content;
        }

        Matcher matcher = YAML_FILE_PATTERN.matcher(content);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String replacement = expandYamlFileInclude(content, matcher.start(), matcher.group());
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String expandYamlFileInclude(String content, int placeholderStart, String placeholder) {
        String replacement = interpolator.replace(placeholder);
        // Only inline YAML list fragments. Expanding arbitrary file values here
        // would break block scalars (for example PEM keys) whose indentation is
        // significant and must be handled after YAML parsing.
        if (!replacement.stripLeading().startsWith("- ")) {
            return placeholder;
        }

        // The placeholder is usually indented as a YAML sequence item. The first
        // line inherits that indentation from the original document, but subsequent
        // lines from the included file need the same prefix or YAML sees them at
        // column zero and rejects the sequence.
        int lineStart = content.lastIndexOf('\n', placeholderStart - 1) + 1;
        String indentation = content.substring(lineStart, placeholderStart);
        if (!indentation.chars().allMatch(Character::isWhitespace)) {
            return replacement;
        }
        return indentContinuationLines(replacement, indentation);
    }

    private static String indentContinuationLines(String text, String indentation) {
        String[] lines = text.split("\\n", -1);
        StringBuilder indented = new StringBuilder(lines[0]);
        for (int i = 1; i < lines.length; i++) {
            indented.append('\n');
            if (!lines[i].isEmpty()) {
                indented.append(indentation).append(lines[i]);
            }
        }
        return indented.toString();
    }

    private boolean isYamlResource(ImportResource resource) {
        String filename = resource.getFilename().toLowerCase(Locale.ROOT);
        return filename.endsWith(".yaml") || filename.endsWith(".yml");
    }

    private String substituteContent(String content) {
        String substituted = content;

        if (importConfigProperties.getVarSubstitution().isEnabled()) {
            substituted = interpolator.replace(substituted);
        }

        if (JS_PATTERN.matcher(substituted).find()) {
            if (!importConfigProperties.getVarSubstitution().isScriptEvaluationEnabled()) {
                throw new IllegalStateException("Script evaluation used but --import.var-substitution.script-evaluation-enabled not set");
            }
            substituted = evaluateScripts(substituted);
        }

        return substituted;
    }

    private String evaluateScripts(String content) {
        Map<String, Object> context = new HashMap<>();
        Map<String, String> env = new HashMap<>();
        System.getenv().forEach(env::put);

        // Add system properties to env context for testing/flexibility
        System.getProperties().forEach((k, v) -> env.put(k.toString(), v.toString()));

        context.put("env", env);

        Matcher matcher = JS_PATTERN.matcher(content);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String expression = matcher.group(1);
            Object result = scriptEvaluator.evaluate(expression, context);
            String replacement;
            try {
                if (result instanceof String) {
                    replacement = (String) result;
                } else {
                    replacement = OBJECT_MAPPER.writeValueAsString(result);
                }
            } catch (Exception e) {
                throw new InvalidImportException("Failed to serialize script result: " + e.getMessage(), e);
            }
            logger.debug("Replaced script expression '{}' with '{}'", expression, replacement);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private Pair<String, List<RealmImport>> readRealmImportFromImportResource(ImportResource resource) {
        String location = resource.getFilename();
        String content = resource.getValue();
        String contentChecksum = DigestUtils.sha256Hex(content + getImportBehaviorChecksumSalt());

        if (logger.isTraceEnabled()) {
            logger.trace(content);
        }

        List<RealmImport> realmImports;
        try {
            realmImports = readContent(resource);
        } catch (Exception e) {
            throw new InvalidImportException("Unable to parse file '" + location + "': " + e.getMessage(), e);
        }
        realmImports.forEach(realmImport -> {
            realmImport.setChecksum(contentChecksum);
            realmImport.setSource(location);
        });

        return new ImmutablePair<>(location, realmImports);
    }

    private String getImportBehaviorChecksumSalt() {
        if (environment == null
                || !environment.containsProperty("import.behaviors.user-update-ignored-properties")
        ) {
            return "";
        }

        if (importConfigProperties == null || importConfigProperties.getBehaviors() == null) {
            return "";
        }

        Collection<String> ignored = importConfigProperties.getBehaviors().getUserUpdateIgnoredProperties();
        String ignoredNormalized = normalizeAndSortChecksumValues(ignored);

        return "\n#userUpdateIgnoredProperties=" + ignoredNormalized;
    }

    private String normalizeAndSortChecksumValues(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .sorted()
                .collect(Collectors.joining(","));
    }

    private List<RealmImport> readContent(ImportResource resource) {
        List<RealmImport> realmImports = new ArrayList<>();
        String content = resource.getValue();

        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setCodePointLimit(importConfigProperties.getFiles().getCodePointLimit());

        Yaml yaml = new Yaml(loaderOptions);
        // loadAll() is lazy. Materialize every document before parsing substituted
        // scalar values, because SnakeYAML instances are not reentrant.
        List<Object> yamlDocuments = new ArrayList<>();
        yaml.loadAll(content).forEach(yamlDocuments::add);

        for (Object yamlDocument : yamlDocuments) {
            Object substitutedDocument = isYamlResource(resource)
                    ? substituteYamlValue(yamlDocument, loaderOptions)
                    : yamlDocument;
            realmImports.add(OBJECT_MAPPER.convertValue(substitutedDocument, RealmImport.class));
        }

        return realmImports;
    }

    private Object substituteYamlValue(Object value, LoaderOptions loaderOptions) {
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> substituted = new LinkedHashMap<>();
            map.forEach((key, nestedValue) -> substituted.put(
                    key instanceof String stringKey ? substituteYamlScalar(stringKey, loaderOptions) : key,
                    substituteYamlValue(nestedValue, loaderOptions)
            ));
            return substituted;
        }

        if (value instanceof List<?> list) {
            return list.stream().map(item -> substituteYamlValue(item, loaderOptions)).toList();
        }

        return value instanceof String stringValue ? substituteYamlScalar(stringValue, loaderOptions) : value;
    }

    private Object substituteYamlScalar(String value, LoaderOptions loaderOptions) {
        String substituted = substituteContent(value);
        if (substituted.equals(value) || !looksLikeStructuredValue(substituted)) {
            return substituted;
        }

        try {
            // Use a fresh parser: nested load() on the parser currently iterating
            // loadAll() corrupts SnakeYAML's internal parser state.
            return new Yaml(loaderOptions).load(substituted);
        } catch (Exception ignored) {
            return substituted;
        }
    }

    private boolean looksLikeStructuredValue(String value) {
        String trimmed = value.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
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
