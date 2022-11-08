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

package de.adorsys.keycloak.config.service.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import de.adorsys.keycloak.config.KeycloakConfigRunner;
import de.adorsys.keycloak.config.properties.ExportConfigProperties;
import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.ListCompareAlgorithm;
import org.javers.core.diff.changetype.PropertyChange;
import org.javers.core.metamodel.clazz.EntityDefinition;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RealmExportService {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakConfigRunner.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ObjectMapper YAML_MAPPER = new YAMLMapper();

    private static final String PLACEHOLDER = "REALM_NAME_PLACEHOLDER";
    private static final Javers JAVERS;

    static {
        YAML_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        var realmIgnoredProperties = new ArrayList<String>();
        realmIgnoredProperties.add("groups");
        realmIgnoredProperties.add("roles");
        realmIgnoredProperties.add("defaultRole");
        realmIgnoredProperties.add("clientProfiles");
        realmIgnoredProperties.add("clientPolicies");
        realmIgnoredProperties.add("users");
        realmIgnoredProperties.add("federatedUsers");
        realmIgnoredProperties.add("scopeMappings");
        realmIgnoredProperties.add("clientScopeMappings");
        realmIgnoredProperties.add("clients");
        realmIgnoredProperties.add("clientScopes");
        realmIgnoredProperties.add("userFederationProviders");
        realmIgnoredProperties.add("userFederationMappers");
        realmIgnoredProperties.add("identityProviders");
        realmIgnoredProperties.add("identityProviderMappers");
        realmIgnoredProperties.add("protocolMappers");
        realmIgnoredProperties.add("components");
        realmIgnoredProperties.add("authenticationFlows");
        realmIgnoredProperties.add("authenticatorConfig");
        realmIgnoredProperties.add("requiredActions");
        realmIgnoredProperties.add("applicationScopeMappings");
        realmIgnoredProperties.add("applications");
        realmIgnoredProperties.add("oauthClients");
        realmIgnoredProperties.add("clientTemplates");

        JAVERS = JaversBuilder.javers()
                .registerEntity(new EntityDefinition(RealmRepresentation.class, "id", realmIgnoredProperties))
                .registerEntity(new EntityDefinition(ClientRepresentation.class, "clientId", List.of("id", "protocolMappers")))
                .registerEntity(new EntityDefinition(ProtocolMapperRepresentation.class, "name", List.of("id")))
                .withListCompareAlgorithm(ListCompareAlgorithm.LEVENSHTEIN_DISTANCE)
                .build();
    }

    private final ExportConfigProperties exportConfigProperties;
    private final KeycloakConfigProperties keycloakConfigProperties;

    @Autowired
    public RealmExportService(ExportConfigProperties exportConfigProperties,
                              KeycloakConfigProperties keycloakConfigProperties) {
        this.exportConfigProperties = exportConfigProperties;
        this.keycloakConfigProperties = keycloakConfigProperties;
    }

    public void doExports() throws Exception {
        var outputLocation = Paths.get(exportConfigProperties.getLocation());
        if (!Files.exists(outputLocation)) {
            Files.createDirectories(outputLocation);
        }
        if (!Files.isDirectory(outputLocation)) {
            logger.error("Output location '{}' is not a directory. Aborting.", exportConfigProperties.getLocation());
        }
        var keycloakConfigVersion = keycloakConfigProperties.getVersion();
        var exportVersion = exportConfigProperties.getKeycloakVersion();
        if (!exportVersion.equals(keycloakConfigVersion)) {
            logger.warn("Keycloak-Config-CLI keycloak version {} and export keycloak version {} are not equal."
                            + " This may cause problems if the API changed."
                            + " Please compile keycloak-config-cli with a matching keycloak version!",
                    keycloakConfigVersion, exportVersion);
        }
        var inputFile = Paths.get(exportConfigProperties.getLocation(), "in", "realm.json");
        try (var is = Files.newInputStream(inputFile)) {
            var realm = OBJECT_MAPPER.readValue(is, RealmRepresentation.class);
            var realmName = realm.getRealm();
            RealmRepresentation defaultRealm;
            try (var defaultRealmIs = getClass().getResourceAsStream(String.format("/reference-realms/%s/realm.json", exportConfigProperties.getKeycloakVersion()))) {
                if (defaultRealmIs == null) {
                    logger.error("Reference realm for version {} does not exist", exportConfigProperties.getKeycloakVersion());
                    return;
                }
                /*
                 * Replace the placeholder with the realm name to import. This sets some internal values like role names,
                 * baseUrls and redirectUrls so that they don't get picked up as "changes"
                 */
                var realmString = new String(defaultRealmIs.readAllBytes(), StandardCharsets.UTF_8).replace(PLACEHOLDER, realmName);
                defaultRealm = OBJECT_MAPPER.readValue(realmString, RealmRepresentation.class);
            }

            /*
             * Trick javers into thinking this is the "same" object, by setting the ID on the reference realm
             * to the ID of the current realm. That way we only get actual changes, not a full list of changes
             * including the "object removed" and "object added" changes
             */
            logger.info("Exporting realm {}", realmName);
            defaultRealm.setId(realm.getId());
            var strippedRealm = new RealmRepresentation();
            handleBaseRealm(realm, defaultRealm, strippedRealm);

            // Realm is complete, now do clients
            var allClientsDiff = JAVERS.compareCollections(defaultRealm.getClients(),realm.getClients(), ClientRepresentation.class);

            if (allClientsDiff.hasChanges()) {
                ClientRepresentation defaultEmptyClient;
                try (var clientIs = getClass().getResourceAsStream(String.format("/reference-realms/%s/client.json", exportConfigProperties.getKeycloakVersion()))) {
                    defaultEmptyClient = OBJECT_MAPPER.readValue(clientIs, ClientRepresentation.class);
                }
                // Clients aren't all default. Enumerate the "default" clients first and check if they changed at all
                var defaultReferenceClients = defaultRealm.getClients();

                for (var defaultReferenceClient : defaultReferenceClients) {
                    var defaultRealmClient = realm.getClients().stream().filter(c -> c.getClientId().equals(defaultReferenceClient.getClientId())).findAny();
                    if (defaultRealmClient.isPresent()) {
                        var client = defaultRealmClient.get();
                        // First, compare it to the actual default client:
                        var clientDiff = JAVERS.compare(defaultReferenceClient, client);
                        if (clientDiff.hasChanges()) {
                            var strippedClient = createMinimizedClient(strippedRealm, defaultEmptyClient, client);
                            // Add protocol mappers here
                            // Maybe add authorizationSettings here
                        }
                    }
                }
                // Now that we're done with the default realm clients, handle clients that are *not* default
                var defaultClientIds = defaultReferenceClients.stream().map(ClientRepresentation::getClientId).collect(Collectors.toList());
                var nonDefaultClients = realm.getClients().stream()
                        .filter(c -> !defaultClientIds.contains(c.getClientId())).collect(Collectors.toList());
                for (var client : nonDefaultClients) {
                    var strippedClient = createMinimizedClient(strippedRealm, defaultEmptyClient, client);
                    // Add protocol mappers here
                    // Maybe add authorizationSettings here
                }
            }
            var outputFile = Paths.get(exportConfigProperties.getLocation(), "out", String.format("%s.yaml", realmName));
            try (var os = new FileOutputStream(outputFile.toFile())) {
                YAML_MAPPER.writeValue(os, strippedRealm);
            }
        }
    }

    private ClientRepresentation createMinimizedClient(RealmRepresentation strippedRealm, ClientRepresentation defaultEmptyClient, ClientRepresentation client) throws NoSuchFieldException, IllegalAccessException {
        // Trick javers again
        defaultEmptyClient.setClientId(client.getClientId());
        /*
         * We have changes compared to the keycloak default client.
         * Now compare to a 'naked' client to generate a minimal client representation to put into yaml
         */
        var strippedClient = new ClientRepresentation();
        var minimalDiff = JAVERS.compare(defaultEmptyClient, client);
        for (var change : minimalDiff.getChangesByType(PropertyChange.class)) {
            applyClientChange(strippedClient, client, (PropertyChange<?>) change);
        }
        strippedClient.setClientId(client.getClientId());
        strippedClient.setEnabled(client.isEnabled());
        if (strippedRealm.getClients() == null) {
            strippedRealm.setClients(new ArrayList<>());
        }
        strippedRealm.getClients().add(strippedClient);
        return strippedClient;
    }

    private void handleBaseRealm(RealmRepresentation realm, RealmRepresentation defaultRealm, RealmRepresentation strippedRealm) throws NoSuchFieldException, IllegalAccessException {
        var diff = JAVERS.compare(defaultRealm, realm);
        for (var change : diff.getChangesByType(PropertyChange.class)) {
            applyRealmChange(strippedRealm, change);
        }

        // Now that Javers is done, clean up a bit afterwards. We always need to set the realm and enabled fields
        strippedRealm.setRealm(realm.getRealm());
        strippedRealm.setEnabled(realm.isEnabled());

        // If the realm ID diverges from the name, include it in the dump, otherwise remove it
        if (Objects.equals(realm.getRealm(), realm.getId())) {
            strippedRealm.setId(null);
        } else {
            strippedRealm.setId(realm.getId());
        }
    }

    private void applyRealmChange(RealmRepresentation strippedRealm, PropertyChange<?> change) throws NoSuchFieldException, IllegalAccessException {
        var field = RealmRepresentation.class.getDeclaredField(change.getPropertyName());
        field.setAccessible(true);
        field.set(strippedRealm, change.getRight());
    }

    private void applyClientChange(ClientRepresentation strippedClient, ClientRepresentation client, PropertyChange<?> change) throws NoSuchFieldException, IllegalAccessException {
        var field = ClientRepresentation.class.getDeclaredField(change.getPropertyName());
        field.setAccessible(true);
        field.set(strippedClient, field.get(client));
    }
}
