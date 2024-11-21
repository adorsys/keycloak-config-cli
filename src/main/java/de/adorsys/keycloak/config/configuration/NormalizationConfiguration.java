/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2022 adorsys GmbH & Co. KG @ https://adorsys.com
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

package de.adorsys.keycloak.config.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.ListCompareAlgorithm;
import org.javers.core.metamodel.clazz.EntityDefinition;
import org.javers.core.metamodel.clazz.EntityDefinitionBuilder;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserFederationMapperRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "NORMALIZE")
public class NormalizationConfiguration {

    @Bean
    public Javers javers() {
        return commonJavers()
                .withListCompareAlgorithm(ListCompareAlgorithm.LEVENSHTEIN_DISTANCE)
                .build();
    }

    @Bean
    public Javers unOrderedJavers() {
        return commonJavers()
                .withListCompareAlgorithm(ListCompareAlgorithm.AS_SET)
                .build();
    }

    @Bean
    public YAMLMapper yamlMapper() {
        var ym = new YAMLMapper();
        ym.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        ym.enable(SerializationFeature.INDENT_OUTPUT);
        ym.enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR);
        return ym;
    }

    @Bean
    public ObjectMapper objectMapper() {
        var om = new ObjectMapper();
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        om.enable(SerializationFeature.INDENT_OUTPUT);
        return om;
    }

    private JaversBuilder commonJavers() {
        var realmIgnoredProperties = new ArrayList<String>();
        realmIgnoredProperties.add("id");
        realmIgnoredProperties.add("groups");
        realmIgnoredProperties.add("roles");
        realmIgnoredProperties.add("defaultRole");
        realmIgnoredProperties.add("clientProfiles"); //
        realmIgnoredProperties.add("clientPolicies"); //
        realmIgnoredProperties.add("users");
        realmIgnoredProperties.add("federatedUsers");
        realmIgnoredProperties.add("scopeMappings"); //
        realmIgnoredProperties.add("clientScopeMappings"); //
        realmIgnoredProperties.add("clients"); //
        realmIgnoredProperties.add("clientScopes"); //
        realmIgnoredProperties.add("userFederationProviders");
        realmIgnoredProperties.add("userFederationMappers");
        realmIgnoredProperties.add("identityProviders");
        realmIgnoredProperties.add("identityProviderMappers");
        realmIgnoredProperties.add("protocolMappers"); //
        realmIgnoredProperties.add("components");
        realmIgnoredProperties.add("authenticationFlows");
        realmIgnoredProperties.add("authenticatorConfig");
        realmIgnoredProperties.add("requiredActions");
        realmIgnoredProperties.add("applicationScopeMappings");
        realmIgnoredProperties.add("applications");
        realmIgnoredProperties.add("oauthClients");
        realmIgnoredProperties.add("clientTemplates");
        realmIgnoredProperties.add("attributes");

        return JaversBuilder.javers()
                .registerEntity(new EntityDefinition(RealmRepresentation.class, "realm", realmIgnoredProperties))
                .registerEntity(new EntityDefinition(ClientRepresentation.class, "clientId",
                        List.of("id", "authorizationSettings", "protocolMappers")))
                .registerEntity(new EntityDefinition(ProtocolMapperRepresentation.class, "name", List.of("id")))
                .registerEntity(new EntityDefinition(ClientScopeRepresentation.class, "name", List.of("id", "protocolMappers")))
                .registerEntity(new EntityDefinition(RoleRepresentation.class, "name", List.of("id", "containerId", "composites", "attributes")))
                .registerEntity(new EntityDefinition(GroupRepresentation.class, "path", List.of("id", "subGroups", "attributes", "clientRoles")))
                .registerEntity(new EntityDefinition(AuthenticationFlowRepresentation.class, "alias", List.of("id", "authenticationExecutions")))
                .registerEntity(new EntityDefinition(IdentityProviderRepresentation.class, "alias", List.of("internalId")))
                .registerEntity(EntityDefinitionBuilder.entityDefinition(IdentityProviderMapperRepresentation.class)
                        .withIdPropertyNames("name", "identityProviderAlias")
                        .withIgnoredProperties("id").build())
                .registerEntity(new EntityDefinition(RequiredActionProviderRepresentation.class, "alias"))
                .registerEntity(new EntityDefinition(UserFederationProviderRepresentation.class, "displayName", List.of("id")))
                .registerEntity(EntityDefinitionBuilder.entityDefinition(UserFederationMapperRepresentation.class)
                        .withIdPropertyNames("name", "federationProviderDisplayName")
                        .withIgnoredProperties("id").build())
                .registerEntity(new EntityDefinition(ComponentExportRepresentation.class, "name", List.of("id", "subComponents", "config")));
    }
}
