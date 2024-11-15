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

package de.adorsys.keycloak.config.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.keycloak.config.AbstractImportIT;
import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import de.adorsys.keycloak.config.repository.IdentityProviderRepository;
import de.adorsys.keycloak.config.test.util.SubGroupUtil;
import de.adorsys.keycloak.config.util.VersionUtil;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"java:S5961", "java:S5976"})
class ImportClientsIT extends AbstractImportIT {
    private static final String REALM_NAME = "realmWithClients";
    private static final String REALM_AUTH_FLOW_NAME = "realmWithClientsForAuthFlowOverrides";

    @Autowired
    private KeycloakConfigProperties properties;

    @Autowired
    private IdentityProviderRepository identityProviderRepository;

    ImportClientsIT() {
        this.resourcePath = "import-files/clients";
    }

    @Test
    @Order(0)
    void shouldCreateRealmWithClient() throws IOException {
        doImport("00_create_realm_with_client.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(false, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        // ... initial version of a custom attribute is in place
        assertThat(realm.getAttributes(), not(anEmptyMap()));
        assertThat(realm.getAttributes().get("custom"), is("test-step00"));

        ClientRepresentation createdClient = getClientByClientId(realm, "moped-client");

        assertThat(createdClient, notNullValue());
        assertThat(createdClient.getName(), is("moped-client"));
        assertThat(createdClient.getClientId(), is("moped-client"));
        assertThat(createdClient.getDescription(), is("Moped-Client"));
        assertThat(createdClient.isEnabled(), is(true));
        assertThat(createdClient.getClientAuthenticatorType(), is("client-secret"));
        assertThat(createdClient.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(createdClient.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(createdClient.getProtocolMappers(), is(nullValue()));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(REALM_NAME, createdClient.getId());
        assertThat(clientSecret, is("my-special-client-secret"));

        ClientRepresentation clientToDelete = getClientByClientId(realm, "client-to-be-deleted");

        assertThat(clientToDelete, notNullValue());
        assertThat(clientToDelete.getName(), is("client-to-be-deleted"));
        assertThat(clientToDelete.getClientId(), is("client-to-be-deleted"));
        assertThat(clientToDelete.isEnabled(), is(true));
        assertThat(clientToDelete.getClientAuthenticatorType(), is("client-secret"));
        assertThat(clientToDelete.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(clientToDelete.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(clientToDelete.getProtocolMappers(), is(nullValue()));

    }

    @Test
    @Order(1)
    void shouldUpdateRealmByAddingClient() throws IOException {
        doImport("01_update_realm__add_client.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(false, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        // ... updated version of a custom attribute is in place
        assertThat(realm.getAttributes(), not(anEmptyMap()));
        assertThat(realm.getAttributes().get("custom"), is("test-step01"));

        ClientRepresentation client;
        String clientSecret;

        client = getClientByClientId(realm, "another-client");
        assertThat(client, notNullValue());
        assertThat(client.getClientId(), is("another-client"));
        assertThat(client.getDescription(), is("Another-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(client.getProtocolMappers(), is(nullValue()));

        // ... and has to be retrieved separately
        clientSecret = getClientSecret(REALM_NAME, client.getId());
        assertThat(clientSecret, is("my-other-client-secret"));

        client = getClientByName(realm, "another-other-client");
        assertThat(client.getName(), is("another-other-client"));
        assertThat(client.getDescription(), is("Another-Other-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(client.getProtocolMappers(), is(nullValue()));

        // ... and has to be retrieved separately
        clientSecret = getClientSecret(REALM_NAME, client.getId());
        assertThat(clientSecret, is("my-another-other-client-secret"));

        client = getClientByName(realm, "another-other-client-without-client-id");
        assertThat(client.getName(), is("another-other-client-without-client-id"));
        assertThat(client.getDescription(), is("Another-Other-Client-Without-Client-Id"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(client.getProtocolMappers(), is(nullValue()));

        // ... and has to be retrieved separately
        clientSecret = getClientSecret(REALM_NAME, client.getId());
        assertThat(clientSecret, is("my-another-other-client-secret-without-client-id"));

        // ... a client which shall be removed

        ClientRepresentation clientToDelete = getClientByClientId(realm, "client-to-be-deleted");
        assertThat(clientToDelete, nullValue());

    }

    @Test
    @Order(2)
    void shouldUpdateRealmWithChangedClientProperties() throws IOException {
        doImport("02_update_realm__change_clients_properties.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(false, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientRepresentation createdClient = getClientByClientId(realm, "moped-client");

        assertThat(createdClient.getName(), is("moped-client"));
        assertThat(createdClient.getClientId(), is("moped-client"));
        assertThat(createdClient.getDescription(), is("Moped-Client"));
        assertThat(createdClient.isEnabled(), is(true));
        assertThat(createdClient.getClientAuthenticatorType(), is("client-secret"));
        assertThat(createdClient.getRedirectUris(), is(containsInAnyOrder("https://moped-client.org/redirect")));
        assertThat(createdClient.getWebOrigins(), is(containsInAnyOrder("https://moped-client.org/webOrigin")));
        assertThat(createdClient.getProtocolMappers(), is(nullValue()));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(REALM_NAME, createdClient.getId());
        assertThat(clientSecret, is("changed-special-client-secret"));
    }

    @Test
    @Order(3)
    void shouldUpdateRealmAddProtocolMapper() throws IOException {
        doImport("03_update_realm__add_protocol-mapper.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(false, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientRepresentation updatedClient = getClientByClientId(realm, "moped-client");

        assertThat(updatedClient.getName(), is("moped-client"));
        assertThat(updatedClient.getClientId(), is("moped-client"));
        assertThat(updatedClient.getDescription(), is("Moped-Client"));
        assertThat(updatedClient.isEnabled(), is(true));
        assertThat(updatedClient.getClientAuthenticatorType(), is("client-secret"));
        assertThat(updatedClient.getRedirectUris(), is(containsInAnyOrder("https://moped-client.org/redirect")));
        assertThat(updatedClient.getWebOrigins(), is(containsInAnyOrder("https://moped-client.org/webOrigin")));
        assertThat(updatedClient.getProtocolMappers(), notNullValue());

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(REALM_NAME, updatedClient.getId());
        assertThat(clientSecret, is("changed-special-client-secret"));

        ProtocolMapperRepresentation updatedClientProtocolMappers = updatedClient.getProtocolMappers().stream().filter(m -> Objects.equals(m.getName(), "BranchCodeMapper")).findFirst().orElse(null);

        assertThat(updatedClientProtocolMappers, notNullValue());
        assertThat(updatedClientProtocolMappers.getProtocol(), is("openid-connect"));
        assertThat(updatedClientProtocolMappers.getProtocolMapper(), is("oidc-usermodel-attribute-mapper"));
        assertThat(updatedClientProtocolMappers.getConfig().get("aggregate.attrs"), is("false"));
        assertThat(updatedClientProtocolMappers.getConfig().get("userinfo.token.claim"), is("true"));
        assertThat(updatedClientProtocolMappers.getConfig().get("user.attribute"), is("branch"));
        assertThat(updatedClientProtocolMappers.getConfig().get("multivalued"), is("false"));
        assertThat(updatedClientProtocolMappers.getConfig().get("id.token.claim"), is("false"));
        assertThat(updatedClientProtocolMappers.getConfig().get("access.token.claim"), is("true"));
        assertThat(updatedClientProtocolMappers.getConfig().get("claim.name"), is("branch"));
        assertThat(updatedClientProtocolMappers.getConfig().get("jsonType.label"), is("String"));

        ClientRepresentation createdClient = keycloakRepository.getClient(
                REALM_NAME,
                "moped-mapper-client"
        );

        assertThat(createdClient.getName(), is("moped-mapper-client"));
        assertThat(createdClient.getClientId(), is("moped-mapper-client"));
        assertThat(createdClient.getDescription(), is("Moped-Client"));
        assertThat(createdClient.isEnabled(), is(true));
        assertThat(createdClient.getClientAuthenticatorType(), is("client-secret"));
        assertThat(createdClient.getRedirectUris(), is(containsInAnyOrder("https://moped-client.org/redirect")));
        assertThat(createdClient.getWebOrigins(), is(containsInAnyOrder("https://moped-client.org/webOrigin")));

        // client secret on this place is always null for keycloak versions lower than 19...
        if (VersionUtil.ge(KEYCLOAK_VERSION, "19")) {
            assertThat(createdClient.getSecret(), is("changed-special-client-secret"));
        } else {
            assertThat(createdClient.getSecret(), is(nullValue()));
        }

        // ... and has to be retrieved separately
        String clientSecret2 = getClientSecret(REALM_NAME, createdClient.getId());
        assertThat(clientSecret2, is("changed-special-client-secret"));

        ProtocolMapperRepresentation createdClientProtocolMappers = createdClient.getProtocolMappers().stream()
                .filter(m -> Objects.equals(m.getName(), "BranchCodeMapper")).findFirst().orElse(null);

        assertThat(createdClientProtocolMappers, notNullValue());
        assertThat(createdClientProtocolMappers.getProtocol(), is("openid-connect"));
        assertThat(createdClientProtocolMappers.getProtocolMapper(), is("oidc-usermodel-attribute-mapper"));
        assertThat(createdClientProtocolMappers.getConfig().get("aggregate.attrs"), is("false"));
        assertThat(createdClientProtocolMappers.getConfig().get("userinfo.token.claim"), is("true"));
        assertThat(createdClientProtocolMappers.getConfig().get("user.attribute"), is("branch"));
        assertThat(createdClientProtocolMappers.getConfig().get("multivalued"), is("false"));
        assertThat(createdClientProtocolMappers.getConfig().get("id.token.claim"), is("false"));
        assertThat(createdClientProtocolMappers.getConfig().get("access.token.claim"), is("true"));
        assertThat(createdClientProtocolMappers.getConfig().get("claim.name"), is("branch"));
        assertThat(createdClientProtocolMappers.getConfig().get("jsonType.label"), is("String"));
    }

    @Test
    @Order(4)
    void shouldUpdateRealmAddMoreProtocolMapper() throws IOException {
        doImport("04_update_realm__add_more_protocol-mapper.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(false, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientRepresentation client = keycloakRepository.getClient(
                REALM_NAME,
                "moped-client"
        );

        assertThat(client.getName(), is("moped-client"));
        assertThat(client.getClientId(), is("moped-client"));
        assertThat(client.getDescription(), is("Moped-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("https://moped-client.org/redirect")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("https://moped-client.org/webOrigin")));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(REALM_NAME, client.getId());
        assertThat(clientSecret, is("changed-special-client-secret"));

        ProtocolMapperRepresentation protocolMapper = client.getProtocolMappers().stream().filter(m -> Objects.equals(m.getName(), "BranchCodeMapper")).findFirst().orElse(null);

        assertThat(protocolMapper, notNullValue());
        assertThat(protocolMapper.getProtocol(), is("openid-connect"));
        assertThat(protocolMapper.getProtocolMapper(), is("oidc-usermodel-attribute-mapper"));
        assertThat(protocolMapper.getConfig().get("aggregate.attrs"), is("false"));
        assertThat(protocolMapper.getConfig().get("userinfo.token.claim"), is("true"));
        assertThat(protocolMapper.getConfig().get("user.attribute"), is("branch"));
        assertThat(protocolMapper.getConfig().get("multivalued"), is("false"));
        assertThat(protocolMapper.getConfig().get("id.token.claim"), is("false"));
        assertThat(protocolMapper.getConfig().get("access.token.claim"), is("true"));
        assertThat(protocolMapper.getConfig().get("claim.name"), is("branch"));
        assertThat(protocolMapper.getConfig().get("jsonType.label"), is("String"));

        ProtocolMapperRepresentation protocolMapper2 = client.getProtocolMappers().stream().filter(m -> Objects.equals(m.getName(), "full name")).findFirst().orElse(null);

        assertThat(protocolMapper2, notNullValue());
        assertThat(protocolMapper2.getProtocol(), is("openid-connect"));
        assertThat(protocolMapper2.getProtocolMapper(), is("oidc-full-name-mapper"));
        assertThat(protocolMapper2.getConfig().get("id.token.claim"), is("true"));
        assertThat(protocolMapper2.getConfig().get("access.token.claim"), is("true"));
    }

    @Test
    @Order(5)
    void shouldUpdateRealmChangeProtocolMapper() throws IOException {
        doImport("05_update_realm__change_protocol-mapper.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(false, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientRepresentation clien = keycloakRepository.getClient(
                REALM_NAME,
                "moped-client"
        );

        assertThat(clien.getName(), is("moped-client"));
        assertThat(clien.getClientId(), is("moped-client"));
        assertThat(clien.getDescription(), is("Moped-Client"));
        assertThat(clien.isEnabled(), is(true));
        assertThat(clien.getClientAuthenticatorType(), is("client-secret"));
        assertThat(clien.getRedirectUris(), is(containsInAnyOrder("https://moped-client.org/redirect")));
        assertThat(clien.getWebOrigins(), is(containsInAnyOrder("https://moped-client.org/webOrigin")));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(REALM_NAME, clien.getId());
        assertThat(clientSecret, is("changed-special-client-secret"));

        ProtocolMapperRepresentation protocolMapper = clien.getProtocolMappers().stream().filter(m -> Objects.equals(m.getName(), "BranchCodeMapper")).findFirst().orElse(null);

        assertThat(protocolMapper, notNullValue());
        assertThat(protocolMapper.getProtocol(), is("openid-connect"));
        assertThat(protocolMapper.getProtocolMapper(), is("oidc-usermodel-attribute-mapper"));
        assertThat(protocolMapper.getConfig().get("aggregate.attrs"), is("false"));
        assertThat(protocolMapper.getConfig().get("userinfo.token.claim"), is("true"));
        assertThat(protocolMapper.getConfig().get("user.attribute"), is("branch"));
        assertThat(protocolMapper.getConfig().get("multivalued"), is("true"));
        assertThat(protocolMapper.getConfig().get("id.token.claim"), is("false"));
        assertThat(protocolMapper.getConfig().get("access.token.claim"), is("true"));
        assertThat(protocolMapper.getConfig().get("claim.name"), is("branch"));
        assertThat(protocolMapper.getConfig().get("jsonType.label"), is("String"));

        ProtocolMapperRepresentation protocolMapper2 = clien.getProtocolMappers().stream().filter(m -> Objects.equals(m.getName(), "full name")).findFirst().orElse(null);

        assertThat(protocolMapper2, notNullValue());
        assertThat(protocolMapper2.getProtocol(), is("openid-connect"));
        assertThat(protocolMapper2.getProtocolMapper(), is("oidc-full-name-mapper"));
        assertThat(protocolMapper2.getConfig().get("id.token.claim"), is("true"));
        assertThat(protocolMapper2.getConfig().get("access.token.claim"), is("false"));
    }

    @Test
    @Order(6)
    void shouldUpdateRealmIgnoreProtocolMapper() throws IOException {
        doImport("06_update_realm__ignore_protocol-mapper.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(false, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientRepresentation client = keycloakRepository.getClient(
                REALM_NAME,
                "moped-client"
        );

        assertThat(client.getName(), is("moped-client"));
        assertThat(client.getClientId(), is("moped-client"));
        assertThat(client.getDescription(), is("Moped-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("https://moped-client.org/redirect")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("https://moped-client.org/webOrigin")));

        String clientSecret = getClientSecret(REALM_NAME, client.getId());
        assertThat(clientSecret, is("changed-special-client-secret"));

        ProtocolMapperRepresentation protocolMapper = client.getProtocolMappers().stream().filter(m -> Objects.equals(m.getName(), "BranchCodeMapper")).findFirst().orElse(null);

        assertThat(protocolMapper, notNullValue());
        assertThat(protocolMapper.getProtocol(), is("openid-connect"));
        assertThat(protocolMapper.getProtocolMapper(), is("oidc-usermodel-attribute-mapper"));
        assertThat(protocolMapper.getConfig().get("aggregate.attrs"), is("false"));
        assertThat(protocolMapper.getConfig().get("userinfo.token.claim"), is("true"));
        assertThat(protocolMapper.getConfig().get("user.attribute"), is("branch"));
        assertThat(protocolMapper.getConfig().get("multivalued"), is("true"));
        assertThat(protocolMapper.getConfig().get("id.token.claim"), is("false"));
        assertThat(protocolMapper.getConfig().get("access.token.claim"), is("true"));
        assertThat(protocolMapper.getConfig().get("claim.name"), is("branch"));
        assertThat(protocolMapper.getConfig().get("jsonType.label"), is("String"));

        ProtocolMapperRepresentation protocolMapper2 = client.getProtocolMappers().stream().filter(m -> Objects.equals(m.getName(), "full name")).findFirst().orElse(null);

        assertThat(protocolMapper2, notNullValue());
        assertThat(protocolMapper2.getProtocol(), is("openid-connect"));
        assertThat(protocolMapper2.getProtocolMapper(), is("oidc-full-name-mapper"));
        assertThat(protocolMapper2.getConfig().get("id.token.claim"), is("true"));
        assertThat(protocolMapper2.getConfig().get("access.token.claim"), is("false"));
    }

    @Test
    @Order(7)
    void shouldNotUpdateRealmUpdateScopeMappingsWithError() throws IOException {
        RealmImport foundImport = getFirstImport("07_update_realm__try-to-update_protocol-mapper.json");
        realmImportService.doImport(foundImport);

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(false, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));
    }

    @Test
    @Order(8)
    void shouldUpdateRealmDeleteProtocolMapper() throws IOException {
        doImport("08_update_realm__delete_protocol-mapper.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(false, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientRepresentation client = keycloakRepository.getClient(
                REALM_NAME,
                "moped-client"
        );

        assertThat(client.getName(), is("moped-client"));
        assertThat(client.getClientId(), is("moped-client"));
        assertThat(client.getDescription(), is("Moped-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("https://moped-client.org/redirect")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("https://moped-client.org/webOrigin")));

        String clientSecret = getClientSecret(REALM_NAME, client.getId());
        assertThat(clientSecret, is("changed-special-client-secret"));

        ProtocolMapperRepresentation protocolMapper = client.getProtocolMappers().stream().filter(m -> Objects.equals(m.getName(), "BranchCodeMapper")).findFirst().orElse(null);

        assertThat(protocolMapper, is(nullValue()));

        ProtocolMapperRepresentation protocolMapper2 = client.getProtocolMappers().stream().filter(m -> Objects.equals(m.getName(), "full name")).findFirst().orElse(null);

        assertThat(protocolMapper2, notNullValue());
        assertThat(protocolMapper2.getProtocol(), is("openid-connect"));
        assertThat(protocolMapper2.getProtocolMapper(), is("oidc-full-name-mapper"));
        assertThat(protocolMapper2.getConfig().get("id.token.claim"), is("true"));
        assertThat(protocolMapper2.getConfig().get("access.token.claim"), is("false"));
    }

    @Test
    @Order(9)
    void shouldUpdateRealmDeleteAllProtocolMapper() throws IOException {
        doImport("09_update_realm__delete_all_protocol-mapper.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(false, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientRepresentation client = keycloakRepository.getClient(
                REALM_NAME,
                "moped-client"
        );

        assertThat(client.getName(), is("moped-client"));
        assertThat(client.getClientId(), is("moped-client"));
        assertThat(client.getDescription(), is("Moped-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("https://moped-client.org/redirect")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("https://moped-client.org/webOrigin")));

        String clientSecret = getClientSecret(REALM_NAME, client.getId());
        assertThat(clientSecret, is("changed-special-client-secret"));

        assertThat(client.getProtocolMappers(), is(nullValue()));


        ClientRepresentation otherClient = getClientByClientId(realm, "another-client");

        assertThat(otherClient.getClientId(), is("another-client"));
        assertThat(otherClient.getDescription(), is("Another-Client"));
        assertThat(otherClient.isEnabled(), is(true));
        assertThat(otherClient.getClientAuthenticatorType(), is("client-secret"));
        assertThat(otherClient.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(otherClient.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(otherClient.getProtocolMappers(), is(nullValue()));

        // ... and has to be retrieved separately
        String otherClientSecret = getClientSecret(REALM_NAME, otherClient.getId());
        assertThat(otherClientSecret, is("my-other-client-secret"));
    }

    @Test
    @Order(10)
    void shouldUpdateRealmRaiseErrorAddAuthorizationInvalidClient() throws IOException {
        ImportProcessingException thrown;

        RealmImport foundImport0 = getFirstImport("10.0_update_realm__raise_error_add_authorization_client_bearer_only.json");
        thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport0));
        assertThat(thrown.getMessage(), is("Unsupported authorization settings for client 'auth-moped-client' in realm 'realmWithClients': client must be confidential."));

        RealmImport foundImport1 = getFirstImport("10.1_update_realm__raise_error_add_authorization_client_public.json");
        thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport1));
        assertThat(thrown.getMessage(), is("Unsupported authorization settings for client 'auth-moped-client' in realm 'realmWithClients': client must be confidential."));

        RealmImport foundImport2 = getFirstImport("10.2_update_realm__raise_error_add_authorization_without_service_account_enabled.json");
        thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport2));
        assertThat(thrown.getMessage(), is("Unsupported authorization settings for client 'auth-moped-client' in realm 'realmWithClients': serviceAccountsEnabled must be 'true'."));

        /*
        RealmImport foundImport3 = getFirstImport("10.3_update_realm__raise_error_update_authorization_client_bearer_only.json");
        thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport3));
        assertThat(thrown.getMessage(), is("Unsupported authorization settings for client 'auth-moped-client' in realm 'realmWithClients': client must be confidential."));

        doImport("10.4.1_update_realm__raise_error_update_authorization_client_public.json");
        RealmImport foundImport4 = getFirstImport("10.4.2_update_realm__raise_error_update_authorization_client_public.json");
        thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport4));
        assertThat(thrown.getMessage(), is("Unsupported authorization settings for client '${client_realm-management}' in realm 'realmWithClients': client must be confidential."));

        RealmImport foundImport5 = getFirstImport("10.5_update_realm__raise_error_update_authorization_without_service_account_enabled.json");
        thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport5));
        assertThat(thrown.getMessage(), is("Unsupported authorization settings for client '${client_realm-management}' in realm 'realmWithClients': serviceAccountsEnabled must be 'true'."));
        */
    }

    @Test
    @Order(11)
    void shouldUpdateRealmAddAuthorization() throws IOException {
        doImport("11_update_realm__add_authorization.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(false, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientRepresentation client = getClientByName(realm, "auth-moped-client");
        assertThat(client.getName(), is("auth-moped-client"));
        assertThat(client.getClientId(), is("auth-moped-client"));
        assertThat(client.getDescription(), is("Auth-Moped-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("https://moped-client.org/redirect")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("https://moped-client.org/webOrigin")));
        assertThat(client.isServiceAccountsEnabled(), is(true));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(REALM_NAME, client.getId());
        assertThat(clientSecret, is("changed-special-client-secret"));

        ProtocolMapperRepresentation protocolMapper = client.getProtocolMappers().stream().filter(m -> Objects.equals(m.getName(), "BranchCodeMapper")).findFirst().orElse(null);
        assertThat(protocolMapper, notNullValue());
        assertThat(protocolMapper.getProtocol(), is("openid-connect"));
        assertThat(protocolMapper.getProtocolMapper(), is("oidc-usermodel-attribute-mapper"));
        assertThat(protocolMapper.getConfig().get("aggregate.attrs"), is("false"));
        assertThat(protocolMapper.getConfig().get("userinfo.token.claim"), is("true"));
        assertThat(protocolMapper.getConfig().get("user.attribute"), is("branch"));
        assertThat(protocolMapper.getConfig().get("multivalued"), is("false"));
        assertThat(protocolMapper.getConfig().get("id.token.claim"), is("false"));
        assertThat(protocolMapper.getConfig().get("access.token.claim"), is("true"));
        assertThat(protocolMapper.getConfig().get("claim.name"), is("branch"));
        assertThat(protocolMapper.getConfig().get("jsonType.label"), is("String"));

        ResourceServerRepresentation authorizationSettings = client.getAuthorizationSettings();
        assertThat(authorizationSettings.getPolicyEnforcementMode(), is(PolicyEnforcementMode.ENFORCING));
        assertThat(authorizationSettings.isAllowRemoteResourceManagement(), is(true));
        assertThat(authorizationSettings.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));

        List<ResourceRepresentation> authorizationSettingsResources = authorizationSettings.getResources();
        assertThat(authorizationSettingsResources, hasSize(3));

        ResourceRepresentation authorizationSettingsResource;
        authorizationSettingsResource = getAuthorizationSettingsResource(authorizationSettingsResources, "Admin Resource");
        assertThat(authorizationSettingsResource.getUris(), containsInAnyOrder("/protected/admin/*"));
        assertThat(authorizationSettingsResource.getType(), is("http://servlet-authz/protected/admin"));
        assertThat(authorizationSettingsResource.getScopes(), containsInAnyOrder(new ScopeRepresentation("urn:servlet-authz:protected:admin:access")));

        authorizationSettingsResource = getAuthorizationSettingsResource(authorizationSettingsResources, "Protected Resource");
        assertThat(authorizationSettingsResource.getUris(), containsInAnyOrder("/*"));
        assertThat(authorizationSettingsResource.getType(), is("http://servlet-authz/protected/resource"));
        assertThat(authorizationSettingsResource.getScopes(), containsInAnyOrder(new ScopeRepresentation("urn:servlet-authz:protected:resource:access")));
        assertThat(authorizationSettingsResource.getOwner().getName(), is("service-account-auth-moped-client"));
        assertThat(authorizationSettingsResource.getAttributes(), aMapWithSize(1));
        assertThat(authorizationSettingsResource.getAttributes(), hasEntry(is("key"), contains("value")));

        authorizationSettingsResource = getAuthorizationSettingsResource(authorizationSettingsResources, "Main Page");
        assertThat(authorizationSettingsResource.getUris(), empty());
        assertThat(authorizationSettingsResource.getType(), is("urn:servlet-authz:protected:resource"));
        assertThat(authorizationSettingsResource.getScopes(), containsInAnyOrder(
                new ScopeRepresentation("urn:servlet-authz:page:main:actionForAdmin"),
                new ScopeRepresentation("urn:servlet-authz:page:main:actionForUser")
        ));

        List<PolicyRepresentation> authorizationSettingsPolicies = authorizationSettings.getPolicies();
        PolicyRepresentation authorizationSettingsPolicy;

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "Any Admin Policy");
        assertThat(authorizationSettingsPolicy.getDescription(), is("Defines that adminsitrators can do something"));
        assertThat(authorizationSettingsPolicy.getType(), is("role"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(1));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("roles"), equalTo("[{\"id\":\"admin\",\"required\":false}]")));

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "Any User Policy");
        assertThat(authorizationSettingsPolicy.getDescription(), is("Defines that any user can do something"));
        assertThat(authorizationSettingsPolicy.getType(), is("role"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(1));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("roles"), equalTo("[{\"id\":\"user\",\"required\":false}]")));

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "All Users Policy");
        assertThat(authorizationSettingsPolicy.getDescription(), is("Defines that all users can do something"));
        assertThat(authorizationSettingsPolicy.getType(), is("aggregate"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.AFFIRMATIVE));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(1));
        assertThat(readJson(authorizationSettingsPolicy.getConfig().get("applyPolicies")), containsInAnyOrder("Any Admin Policy", "Any User Policy"));

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "Administrative Resource Permission");
        assertThat(authorizationSettingsPolicy.getDescription(), is("A policy that defines access to administrative resources"));
        assertThat(authorizationSettingsPolicy.getType(), is("resource"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(2));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("resources"), equalTo("[\"Admin Resource\"]")));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("applyPolicies"), equalTo("[\"Any Admin Policy\"]")));

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "User Action Scope Permission");
        assertThat(authorizationSettingsPolicy.getDescription(), is("A policy that defines access to a user scope"));
        assertThat(authorizationSettingsPolicy.getType(), is("scope"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(2));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("scopes"), equalTo("[\"urn:servlet-authz:page:main:actionForUser\"]")));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("applyPolicies"), equalTo("[\"Any User Policy\"]")));

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "Administrator Action Scope Permission");
        assertThat(authorizationSettingsPolicy.getDescription(), is("A policy that defines access to an administrator scope"));
        assertThat(authorizationSettingsPolicy.getType(), is("scope"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(2));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("scopes"), equalTo("[\"urn:servlet-authz:page:main:actionForAdmin\"]")));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("applyPolicies"), equalTo("[\"Any Admin Policy\"]")));

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "Protected Resource Permission");
        assertThat(authorizationSettingsPolicy.getDescription(), is("A policy that defines access to any protected resource"));
        assertThat(authorizationSettingsPolicy.getType(), is("resource"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(1));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("applyPolicies"), equalTo("[\"All Users Policy\"]")));

        assertThat(authorizationSettings.getScopes(), hasSize(4));
        assertThat(authorizationSettings.getScopes(), containsInAnyOrder(
                new ScopeRepresentation("urn:servlet-authz:protected:admin:access"),
                new ScopeRepresentation("urn:servlet-authz:protected:resource:access"),
                new ScopeRepresentation("urn:servlet-authz:page:main:actionForAdmin"),
                new ScopeRepresentation("urn:servlet-authz:page:main:actionForUser")
        ));

        client = getClientByName(realm, "missing-id-client");
        assertThat(client.getName(), is("missing-id-client"));
        assertThat(client.getClientId(), not(emptyString()));
        assertThat(client.getDescription(), is("Missing-Id-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.isServiceAccountsEnabled(), is(true));
        assertThat(client.getAuthorizationServicesEnabled(), is(true));

        authorizationSettings = client.getAuthorizationSettings();
        assertThat(authorizationSettings.getPolicyEnforcementMode(), is(PolicyEnforcementMode.ENFORCING));
        assertThat(authorizationSettings.isAllowRemoteResourceManagement(), is(false));
        assertThat(authorizationSettings.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));

        authorizationSettingsResources = authorizationSettings.getResources();
        assertThat(authorizationSettingsResources, hasSize(1));

        authorizationSettingsResource = getAuthorizationSettingsResource(authorizationSettingsResources, "Admin Resource");
        assertThat(authorizationSettingsResource.getUris(), containsInAnyOrder("/protected/admin/*"));
        assertThat(authorizationSettingsResource.getType(), is("http://servlet-authz/protected/admin"));
        assertThat(authorizationSettingsResource.getScopes(), containsInAnyOrder(new ScopeRepresentation("urn:servlet-authz:protected:admin:access")));

        authorizationSettingsPolicies = authorizationSettings.getPolicies();
        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "Any Admin Policy");
        assertThat(authorizationSettingsPolicy.getDescription(), is("Defines that adminsitrators can do something"));
        assertThat(authorizationSettingsPolicy.getType(), is("role"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(1));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("roles"), equalTo("[{\"id\":\"admin\",\"required\":false}]")));

        assertThat(authorizationSettings.getScopes(), hasSize(1));
        assertThat(authorizationSettings.getScopes(), containsInAnyOrder(
                new ScopeRepresentation("urn:servlet-authz:protected:admin:access")
        ));
    }

    @Test
    @Order(12)
    void shouldUpdateRealmUpdateAuthorization() throws IOException {
        // https://github.com/adorsys/keycloak-config-cli/issues/641
        ResourceRepresentation resource = new ResourceRepresentation();
        resource.setName("Tweedl Social Service");
        resource.setType("http://www.example.com/rsrcs/socialstream/140-compatible");
        resource.setIconUri("http://www.example.com/icons/sharesocial.png");
        createRemoteManagedClientResource(REALM_NAME, "auth-moped-client", "changed-special-client-secret", resource);

        doImport("12_update_realm__update_authorization.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(false, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientRepresentation client = getClientByName(realm, "auth-moped-client");
        assertThat(client.getName(), is("auth-moped-client"));
        assertThat(client.getClientId(), is("auth-moped-client"));
        assertThat(client.getDescription(), is("Auth-Moped-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("https://moped-client.org/redirect")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("https://moped-client.org/webOrigin")));
        assertThat(client.isServiceAccountsEnabled(), is(true));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(REALM_NAME, client.getId());
        assertThat(clientSecret, is("changed-special-client-secret"));

        ProtocolMapperRepresentation protocolMapper = client.getProtocolMappers().stream().filter(m -> Objects.equals(m.getName(), "BranchCodeMapper")).findFirst().orElse(null);
        assertThat(protocolMapper, notNullValue());
        assertThat(protocolMapper.getProtocol(), is("openid-connect"));
        assertThat(protocolMapper.getProtocolMapper(), is("oidc-usermodel-attribute-mapper"));
        assertThat(protocolMapper.getConfig().get("aggregate.attrs"), is("false"));
        assertThat(protocolMapper.getConfig().get("userinfo.token.claim"), is("true"));
        assertThat(protocolMapper.getConfig().get("user.attribute"), is("branch"));
        assertThat(protocolMapper.getConfig().get("multivalued"), is("false"));
        assertThat(protocolMapper.getConfig().get("id.token.claim"), is("false"));
        assertThat(protocolMapper.getConfig().get("access.token.claim"), is("true"));
        assertThat(protocolMapper.getConfig().get("claim.name"), is("branch"));
        assertThat(protocolMapper.getConfig().get("jsonType.label"), is("String"));

        ResourceServerRepresentation authorizationSettings = client.getAuthorizationSettings();
        assertThat(authorizationSettings.getPolicyEnforcementMode(), is(PolicyEnforcementMode.PERMISSIVE));
        assertThat(authorizationSettings.isAllowRemoteResourceManagement(), is(true));
        assertThat(authorizationSettings.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));

        List<ResourceRepresentation> authorizationSettingsResources = authorizationSettings.getResources();
        assertThat(authorizationSettingsResources, hasSize(5));

        ResourceRepresentation authorizationSettingsResource;
        authorizationSettingsResource = getAuthorizationSettingsResource(authorizationSettingsResources, "Admin Resource");
        assertThat(authorizationSettingsResource.getUris(), containsInAnyOrder("/protected/admin/*"));
        assertThat(authorizationSettingsResource.getType(), is("http://servlet-authz/protected/admin"));
        assertThat(authorizationSettingsResource.getScopes(), containsInAnyOrder(new ScopeRepresentation("urn:servlet-authz:protected:admin:access", "https://www.keycloak.org/resources/favicon.ico")));

        authorizationSettingsResource = getAuthorizationSettingsResource(authorizationSettingsResources, "Protected Resource");
        assertThat(authorizationSettingsResource.getUris(), containsInAnyOrder("/*"));
        assertThat(authorizationSettingsResource.getType(), is("http://servlet-authz/protected/resource"));
        assertThat(authorizationSettingsResource.getScopes(), containsInAnyOrder(new ScopeRepresentation("urn:servlet-authz:protected:resource:access")));
        assertThat(authorizationSettingsResource.getOwner().getName(), is("service-account-auth-moped-client"));
        assertThat(authorizationSettingsResource.getAttributes(), aMapWithSize(2));
        assertThat(authorizationSettingsResource.getAttributes(), hasEntry(is("key"), contains("value")));
        assertThat(authorizationSettingsResource.getAttributes(), hasEntry(is("key2"), contains("value2")));

        authorizationSettingsResource = getAuthorizationSettingsResource(authorizationSettingsResources, "Premium Resource");
        assertThat(authorizationSettingsResource.getUris(), containsInAnyOrder("/protected/premium/*"));
        assertThat(authorizationSettingsResource.getType(), is("urn:servlet-authz:protected:resource"));
        assertThat(authorizationSettingsResource.getScopes(), containsInAnyOrder(new ScopeRepresentation("urn:servlet-authz:protected:premium:access")));

        authorizationSettingsResource = getAuthorizationSettingsResource(authorizationSettingsResources, "Main Page");
        assertThat(authorizationSettingsResource.getUris(), empty());
        assertThat(authorizationSettingsResource.getType(), is("urn:servlet-authz:protected:resource"));
        assertThat(authorizationSettingsResource.getScopes(), containsInAnyOrder(
                new ScopeRepresentation("urn:servlet-authz:page:main:actionForPremiumUser"),
                new ScopeRepresentation("urn:servlet-authz:page:main:actionForAdmin"),
                new ScopeRepresentation("urn:servlet-authz:page:main:actionForUser")
        ));

        authorizationSettingsResource = getAuthorizationSettingsResource(authorizationSettingsResources, "Tweedl Social Service");
        assertThat(authorizationSettingsResource.getUris(), empty());
        assertThat(authorizationSettingsResource.getType(), is("http://www.example.com/rsrcs/socialstream/140-compatible"));
        assertThat(authorizationSettingsResource.getIconUri(), is("http://www.example.com/icons/sharesocial.png"));
        assertThat(authorizationSettingsResource.getScopes(), empty());

        List<PolicyRepresentation> authorizationSettingsPolicies = authorizationSettings.getPolicies();
        PolicyRepresentation authorizationSettingsPolicy;

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "Any Admin Policy");
        assertThat(authorizationSettingsPolicy.getDescription(), is("Defines that adminsitrators can do something"));
        assertThat(authorizationSettingsPolicy.getType(), is("role"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(1));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("roles"), equalTo("[{\"id\":\"admin\",\"required\":false}]")));

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "Any User Policy");
        assertThat(authorizationSettingsPolicy.getDescription(), is("Defines that any user can do something"));
        assertThat(authorizationSettingsPolicy.getType(), is("role"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(1));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("roles"), equalTo("[{\"id\":\"user\",\"required\":false}]")));

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "Only Premium User Policy");
        assertThat(authorizationSettingsPolicy.getDescription(), is("Defines that only premium users can do something"));
        assertThat(authorizationSettingsPolicy.getType(), is("role"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(1));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("roles"), equalTo("[{\"id\":\"user_premium\",\"required\":false}]")));

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "All Users Policy");
        assertThat(authorizationSettingsPolicy.getDescription(), is("Defines that all users can do something"));
        assertThat(authorizationSettingsPolicy.getType(), is("aggregate"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.AFFIRMATIVE));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(1));
        assertThat(readJson(authorizationSettingsPolicy.getConfig().get("applyPolicies")), containsInAnyOrder("Only Premium User Policy", "Any Admin Policy", "Any User Policy"));

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "Administrative Resource Permission");
        assertThat(authorizationSettingsPolicy.getDescription(), is("A policy that defines access to administrative resources"));
        assertThat(authorizationSettingsPolicy.getType(), is("resource"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(2));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("resources"), equalTo("[\"Admin Resource\"]")));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("applyPolicies"), equalTo("[\"Any Admin Policy\"]")));

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "Premium User Scope Permission");
        assertThat(authorizationSettingsPolicy.getDescription(), is("A policy that defines access to a premium scope"));
        assertThat(authorizationSettingsPolicy.getType(), is("scope"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(2));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("scopes"), equalTo("[\"urn:servlet-authz:page:main:actionForPremiumUser\"]")));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("applyPolicies"), equalTo("[\"Only Premium User Policy\"]")));

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "User Action Scope Permission");
        assertThat(authorizationSettingsPolicy.getDescription(), is("A policy that defines access to a user scope"));
        assertThat(authorizationSettingsPolicy.getType(), is("scope"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(2));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("scopes"), equalTo("[\"urn:servlet-authz:page:main:actionForUser\"]")));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("applyPolicies"), equalTo("[\"Any User Policy\"]")));

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "Administrator Action Scope Permission");
        assertThat(authorizationSettingsPolicy.getDescription(), is("A policy that defines access to an administrator scope"));
        assertThat(authorizationSettingsPolicy.getType(), is("scope"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(2));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("scopes"), equalTo("[\"urn:servlet-authz:page:main:actionForAdmin\"]")));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("applyPolicies"), equalTo("[\"Any Admin Policy\"]")));

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "Protected Resource Permission");
        assertThat(authorizationSettingsPolicy.getDescription(), is("A policy that defines access to any protected resource"));
        assertThat(authorizationSettingsPolicy.getType(), is("resource"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(1));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("applyPolicies"), equalTo("[\"All Users Policy\"]")));

        assertThat(authorizationSettings.getScopes(), hasSize(6));
        assertThat(authorizationSettings.getScopes(), containsInAnyOrder(
                new ScopeRepresentation("urn:servlet-authz:protected:admin:access"),
                new ScopeRepresentation("urn:servlet-authz:protected:resource:access"),
                new ScopeRepresentation("urn:servlet-authz:protected:premium:access"),
                new ScopeRepresentation("urn:servlet-authz:page:main:actionForPremiumUser"),
                new ScopeRepresentation("urn:servlet-authz:page:main:actionForAdmin"),
                new ScopeRepresentation("urn:servlet-authz:page:main:actionForUser", "https://www.keycloak.org/resources/favicon.ico")
        ));

        ClientRepresentation mopedClient = getClientByName(realm, "moped-client");
        assertThat(mopedClient.isServiceAccountsEnabled(), is(true));

        ResourceServerRepresentation mopedAuthorizationSettings = mopedClient.getAuthorizationSettings();
        assertThat(mopedAuthorizationSettings.getPolicyEnforcementMode(), is(PolicyEnforcementMode.PERMISSIVE));
        assertThat(mopedAuthorizationSettings.isAllowRemoteResourceManagement(), is(true));
        assertThat(mopedAuthorizationSettings.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));

        client = getClientByName(realm, "missing-id-client");
        assertThat(client.getName(), is("missing-id-client"));
        assertThat(client.getClientId(), not(emptyString()));
        assertThat(client.getDescription(), is("Missing-Id-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.isServiceAccountsEnabled(), is(true));
        assertThat(client.getAuthorizationServicesEnabled(), is(true));

        authorizationSettings = client.getAuthorizationSettings();
        assertThat(authorizationSettings.getPolicyEnforcementMode(), is(PolicyEnforcementMode.ENFORCING));
        assertThat(authorizationSettings.isAllowRemoteResourceManagement(), is(true));
        assertThat(authorizationSettings.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));

        authorizationSettingsResources = authorizationSettings.getResources();
        assertThat(authorizationSettingsResources, hasSize(1));

        authorizationSettingsResource = getAuthorizationSettingsResource(authorizationSettingsResources, "Admin Resource");
        assertThat(authorizationSettingsResource.getUris(), containsInAnyOrder("/protected/admin/*"));
        assertThat(authorizationSettingsResource.getType(), is("http://servlet-authz/protected/admin"));
        assertThat(authorizationSettingsResource.getScopes(), containsInAnyOrder(new ScopeRepresentation("urn:servlet-authz:protected:user:access")));

        authorizationSettingsPolicies = authorizationSettings.getPolicies();
        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "Any Admin Policy");
        assertThat(authorizationSettingsPolicy.getDescription(), is("Defines that adminsitrators can do something"));
        assertThat(authorizationSettingsPolicy.getType(), is("role"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(1));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("roles"), equalTo("[{\"id\":\"user\",\"required\":false}]")));

        assertThat(authorizationSettings.getScopes(), hasSize(1));
        assertThat(authorizationSettings.getScopes(), containsInAnyOrder(
                new ScopeRepresentation("urn:servlet-authz:protected:user:access")
        ));
    }

    @Test
    @Order(13)
    void shouldUpdateRealmRemoveAuthorization() throws IOException {
        doImport("13_update_realm__remove_authorization.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(false, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientRepresentation client = getClientByName(realm, "auth-moped-client");
        assertThat(client.getName(), is("auth-moped-client"));
        assertThat(client.getClientId(), is("auth-moped-client"));
        assertThat(client.getDescription(), is("Auth-Moped-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("https://moped-client.org/redirect")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("https://moped-client.org/webOrigin")));
        assertThat(client.isServiceAccountsEnabled(), is(true));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(REALM_NAME, client.getId());
        assertThat(clientSecret, is("changed-special-client-secret"));

        ProtocolMapperRepresentation protocolMapper = client.getProtocolMappers().stream().filter(m -> Objects.equals(m.getName(), "BranchCodeMapper")).findFirst().orElse(null);
        assertThat(protocolMapper, notNullValue());
        assertThat(protocolMapper.getProtocol(), is("openid-connect"));
        assertThat(protocolMapper.getProtocolMapper(), is("oidc-usermodel-attribute-mapper"));
        assertThat(protocolMapper.getConfig().get("aggregate.attrs"), is("false"));
        assertThat(protocolMapper.getConfig().get("userinfo.token.claim"), is("true"));
        assertThat(protocolMapper.getConfig().get("user.attribute"), is("branch"));
        assertThat(protocolMapper.getConfig().get("multivalued"), is("false"));
        assertThat(protocolMapper.getConfig().get("id.token.claim"), is("false"));
        assertThat(protocolMapper.getConfig().get("access.token.claim"), is("true"));
        assertThat(protocolMapper.getConfig().get("claim.name"), is("branch"));
        assertThat(protocolMapper.getConfig().get("jsonType.label"), is("String"));

        ResourceServerRepresentation authorizationSettings = client.getAuthorizationSettings();
        assertThat(authorizationSettings.getPolicyEnforcementMode(), is(PolicyEnforcementMode.PERMISSIVE));
        assertThat(authorizationSettings.isAllowRemoteResourceManagement(), is(true));
        assertThat(authorizationSettings.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));

        List<ResourceRepresentation> authorizationSettingsResources = authorizationSettings.getResources();
        assertThat(authorizationSettingsResources, hasSize(4));

        ResourceRepresentation authorizationSettingsResource;
        authorizationSettingsResource = getAuthorizationSettingsResource(authorizationSettingsResources, "Admin Resource");
        assertThat(authorizationSettingsResource.getUris(), containsInAnyOrder("/protected/admin/*"));
        assertThat(authorizationSettingsResource.getType(), is("http://servlet-authz/protected/admin"));
        assertThat(authorizationSettingsResource.getScopes(), containsInAnyOrder(new ScopeRepresentation("urn:servlet-authz:protected:admin:access")));

        authorizationSettingsResource = getAuthorizationSettingsResource(authorizationSettingsResources, "Premium Resource");
        assertThat(authorizationSettingsResource.getUris(), containsInAnyOrder("/protected/premium/*"));
        assertThat(authorizationSettingsResource.getType(), is("urn:servlet-authz:protected:resource"));
        assertThat(authorizationSettingsResource.getScopes(), containsInAnyOrder(new ScopeRepresentation("urn:servlet-authz:protected:premium:access")));

        authorizationSettingsResource = getAuthorizationSettingsResource(authorizationSettingsResources, "Main Page");
        assertThat(authorizationSettingsResource.getUris(), empty());
        assertThat(authorizationSettingsResource.getType(), is("urn:servlet-authz:protected:resource"));
        assertThat(authorizationSettingsResource.getScopes(), containsInAnyOrder(
                new ScopeRepresentation("urn:servlet-authz:page:main:actionForPremiumUser"),
                new ScopeRepresentation("urn:servlet-authz:page:main:actionForAdmin")
        ));

        authorizationSettingsResource = getAuthorizationSettingsResource(authorizationSettingsResources, "Tweedl Social Service");
        assertThat(authorizationSettingsResource.getUris(), empty());
        assertThat(authorizationSettingsResource.getType(), is("http://www.example.com/rsrcs/socialstream/140-compatible"));
        assertThat(authorizationSettingsResource.getIconUri(), is("http://www.example.com/icons/sharesocial.png"));
        assertThat(authorizationSettingsResource.getScopes(), empty());

        List<PolicyRepresentation> authorizationSettingsPolicies = authorizationSettings.getPolicies();
        PolicyRepresentation authorizationSettingsPolicy;

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "Any Admin Policy");
        assertThat(authorizationSettingsPolicy.getDescription(), is("Defines that adminsitrators can do something"));
        assertThat(authorizationSettingsPolicy.getType(), is("role"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(1));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("roles"), equalTo("[{\"id\":\"admin\",\"required\":false}]")));

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "Only Premium User Policy");
        assertThat(authorizationSettingsPolicy.getDescription(), is("Defines that only premium users can do something"));
        assertThat(authorizationSettingsPolicy.getType(), is("role"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(1));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("roles"), equalTo("[{\"id\":\"user_premium\",\"required\":false}]")));

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "All Users Policy");
        assertThat(authorizationSettingsPolicy.getDescription(), is("Defines that all users can do something"));
        assertThat(authorizationSettingsPolicy.getType(), is("aggregate"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.AFFIRMATIVE));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(1));
        assertThat(readJson(authorizationSettingsPolicy.getConfig().get("applyPolicies")), containsInAnyOrder("Only Premium User Policy", "Any Admin Policy"));

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "Administrative Resource Permission");
        assertThat(authorizationSettingsPolicy.getDescription(), is("A policy that defines access to administrative resources"));
        assertThat(authorizationSettingsPolicy.getType(), is("resource"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(2));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("resources"), equalTo("[\"Admin Resource\"]")));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("applyPolicies"), equalTo("[\"Any Admin Policy\"]")));

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "Premium User Scope Permission");
        assertThat(authorizationSettingsPolicy.getDescription(), is("A policy that defines access to a premium scope"));
        assertThat(authorizationSettingsPolicy.getType(), is("scope"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(2));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("scopes"), equalTo("[\"urn:servlet-authz:page:main:actionForPremiumUser\"]")));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("applyPolicies"), equalTo("[\"Only Premium User Policy\"]")));

        authorizationSettingsPolicy = getAuthorizationPolicy(authorizationSettingsPolicies, "Administrator Action Scope Permission");
        assertThat(authorizationSettingsPolicy.getDescription(), is("A policy that defines access to an administrator scope"));
        assertThat(authorizationSettingsPolicy.getType(), is("scope"));
        assertThat(authorizationSettingsPolicy.getLogic(), is(Logic.POSITIVE));
        assertThat(authorizationSettingsPolicy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(authorizationSettingsPolicy.getConfig(), aMapWithSize(2));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("scopes"), equalTo("[\"urn:servlet-authz:page:main:actionForAdmin\"]")));
        assertThat(authorizationSettingsPolicy.getConfig(), hasEntry(equalTo("applyPolicies"), equalTo("[\"Any Admin Policy\"]")));

        assertThat(authorizationSettings.getScopes(), hasSize(4));
        assertThat(authorizationSettings.getScopes(), containsInAnyOrder(
                new ScopeRepresentation("urn:servlet-authz:protected:admin:access"),
                new ScopeRepresentation("urn:servlet-authz:protected:premium:access"),
                new ScopeRepresentation("urn:servlet-authz:page:main:actionForPremiumUser"),
                new ScopeRepresentation("urn:servlet-authz:page:main:actionForAdmin")
        ));

        ClientRepresentation mopedClient = getClientByName(realm, "moped-client");
        assertThat(mopedClient.isServiceAccountsEnabled(), is(false));
        assertThat(mopedClient.getAuthorizationSettings(), nullValue());

        client = getClientByName(realm, "missing-id-client");
        assertThat(client.getName(), is("missing-id-client"));
        assertThat(client.getClientId(), not(emptyString()));
        assertThat(client.getDescription(), is("Missing-Id-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.isServiceAccountsEnabled(), is(true));
        assertThat(client.getAuthorizationServicesEnabled(), is(true));

        authorizationSettings = client.getAuthorizationSettings();
        assertThat(authorizationSettings.getPolicyEnforcementMode(), is(PolicyEnforcementMode.ENFORCING));
        assertThat(authorizationSettings.isAllowRemoteResourceManagement(), is(true));
        assertThat(authorizationSettings.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));

        assertThat(authorizationSettings.getResources(), hasSize(0));
        assertThat(authorizationSettings.getPolicies(), hasSize(0));
        assertThat(authorizationSettings.getScopes(), hasSize(0));
    }

    @Test
    @Order(14)
    void shouldSetAuthenticationFlowBindingOverrides() throws IOException {
        doImport("14_update_realm__set_auth-flow-overrides.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        // Check is flow are imported, only check existence since there is many tests' case on AuthFlow
        assertThat(getAuthenticationFlow(realm, "custom flow"), notNullValue());
        assertThat(getAuthenticationFlow(realm, "custom flow 2"), notNullValue());

        assertThat(getClientByName(realm, "auth-moped-client"), notNullValue());
        assertThat(getClientByName(realm, "moped-client"), notNullValue());

        ClientRepresentation client = getClientByName(realm, "another-client");
        assertThat(client.getName(), is("another-client"));
        assertThat(client.getClientId(), is("another-client"));
        assertThat(client.getDescription(), is("Another-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(client.isServiceAccountsEnabled(), is(false));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(REALM_NAME, client.getId());
        assertThat(clientSecret, is("my-other-client-secret"));

        // ... and finally assert that we really want
        assertThat(client.getAuthenticationFlowBindingOverrides().entrySet(), hasSize(1));
        assertThat(client.getAuthenticationFlowBindingOverrides(), allOf(hasEntry("browser", getAuthenticationFlow(realm, "custom flow").getId())));
    }

    @Test
    @Order(15)
    void shouldUpdateAuthenticationFlowBindingOverrides() throws IOException {
        doImport("15_update_realm__change_auth-flow-overrides.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        // Check is flow are imported, only check existence since there is many tests' case on AuthFlow
        assertThat(getAuthenticationFlow(realm, "custom flow"), notNullValue());
        assertThat(getAuthenticationFlow(realm, "custom flow 2"), notNullValue());

        assertThat(getClientByName(realm, "auth-moped-client"), notNullValue());
        assertThat(getClientByName(realm, "moped-client"), notNullValue());

        ClientRepresentation client = getClientByName(realm, "another-client");
        assertThat(client.getName(), is("another-client"));
        assertThat(client.getClientId(), is("another-client"));
        assertThat(client.getDescription(), is("Another-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(client.isServiceAccountsEnabled(), is(false));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(REALM_NAME, client.getId());
        assertThat(clientSecret, is("my-other-client-secret"));

        // ... and finally assert that we really want
        assertThat(client.getAuthenticationFlowBindingOverrides().entrySet(), hasSize(1));
        assertThat(client.getAuthenticationFlowBindingOverrides(), allOf(hasEntry("direct_grant", getAuthenticationFlow(realm, "custom flow 2").getId())));
    }

    @Test
    @Order(16)
    void shouldNotChangeAuthenticationFlowBindingOverrides() throws IOException {
        doImport("16_update_realm__ignore_auth-flow-overrides.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        // Check is flow are imported, only check existence since there is many tests' case on AuthFlow
        assertThat(getAuthenticationFlow(realm, "custom flow"), notNullValue());
        assertThat(getAuthenticationFlow(realm, "custom flow 2"), notNullValue());

        assertThat(getClientByName(realm, "auth-moped-client"), notNullValue());
        assertThat(getClientByName(realm, "moped-client"), notNullValue());

        ClientRepresentation client = getClientByName(realm, "another-client");
        assertThat(client.getName(), is("another-client"));
        assertThat(client.getClientId(), is("another-client"));
        assertThat(client.getDescription(), is("Another-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(client.isServiceAccountsEnabled(), is(false));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(REALM_NAME, client.getId());
        assertThat(clientSecret, is("my-other-client-secret"));

        // ... and finally assert that we really want
        assertThat(client.getAuthenticationFlowBindingOverrides().entrySet(), hasSize(1));
        assertThat(client.getAuthenticationFlowBindingOverrides(), allOf(hasEntry("direct_grant", getAuthenticationFlow(realm, "custom flow 2").getId())));
    }

    @Test
    @Order(17)
    void shouldUpdateAuthenticationFlowBindingOverridesIdWhenFlowChanged() throws IOException {
        doImport("17_update_realm__update_id_auth-flow-overrides_when_flow_changed.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        // Check is flow are imported, only check existence since there is many tests' case on AuthFlow
        assertThat(getAuthenticationFlow(realm, "custom flow"), notNullValue());
        assertThat(getAuthenticationFlow(realm, "custom flow 2"), notNullValue());

        assertThat(getClientByName(realm, "auth-moped-client"), notNullValue());
        assertThat(getClientByName(realm, "moped-client"), notNullValue());

        ClientRepresentation client = getClientByName(realm, "another-client");
        assertThat(client.getName(), is("another-client"));
        assertThat(client.getClientId(), is("another-client"));
        assertThat(client.getDescription(), is("Another-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(client.isServiceAccountsEnabled(), is(false));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(REALM_NAME, client.getId());
        assertThat(clientSecret, is("my-other-client-secret"));

        // ... and finally assert that we really want
        assertThat(client.getAuthenticationFlowBindingOverrides().entrySet(), hasSize(1));
        assertThat(client.getAuthenticationFlowBindingOverrides(), allOf(hasEntry("direct_grant", getAuthenticationFlow(realm, "custom flow 2").getId())));
    }

    @Test
    @Order(18)
    void shouldntUpdateWithAnInvalidAuthenticationFlowBindingOverrides() throws IOException {
        RealmImport foundImport = getFirstImport("18_cannot_update_realm__with_invalid_auth-flow-overrides.json");
        KeycloakRepositoryException thrown = assertThrows(KeycloakRepositoryException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), is("Cannot find top-level-flow 'bad value' in realm 'realmWithClients'."));

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        // Check is flow are imported, only check existence since there is many tests' case on AuthFlow
        assertThat(getAuthenticationFlow(realm, "custom flow"), notNullValue());
        assertThat(getAuthenticationFlow(realm, "custom flow 2"), notNullValue());

        assertThat(getClientByName(realm, "auth-moped-client"), notNullValue());
        assertThat(getClientByName(realm, "moped-client"), notNullValue());

        ClientRepresentation client = getClientByName(realm, "another-client");
        assertThat(client.getName(), is("another-client"));
        assertThat(client.getClientId(), is("another-client"));
        assertThat(client.getDescription(), is("Another-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(client.isServiceAccountsEnabled(), is(false));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(REALM_NAME, client.getId());
        assertThat(clientSecret, is("my-other-client-secret"));

        // ... and finally assert that we really want
        assertThat(client.getAuthenticationFlowBindingOverrides().entrySet(), hasSize(1));
        assertThat(client.getAuthenticationFlowBindingOverrides(), allOf(hasEntry("direct_grant", getAuthenticationFlow(realm, "custom flow 2").getId())));
    }

    @Test
    @Order(19)
    void shouldRemoveAuthenticationFlowBindingOverrides() throws IOException {
        doImport("19_update_realm__remove_auth-flow-overrides.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        // Check is flow are imported, only check existence since there is many tests' case on AuthFlow
        assertThat(getAuthenticationFlow(realm, "custom flow"), notNullValue());
        assertThat(getAuthenticationFlow(realm, "custom flow 2"), notNullValue());

        assertThat(getClientByName(realm, "auth-moped-client"), notNullValue());
        assertThat(getClientByName(realm, "moped-client"), notNullValue());

        ClientRepresentation client = getClientByName(realm, "another-client");
        assertThat(client.getName(), is("another-client"));
        assertThat(client.getClientId(), is("another-client"));
        assertThat(client.getDescription(), is("Another-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(client.isServiceAccountsEnabled(), is(false));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(REALM_NAME, client.getId());
        assertThat(clientSecret, is("my-other-client-secret"));

        // ... and finally assert that we really want
        assertThat(client.getAuthenticationFlowBindingOverrides(), equalTo(Collections.emptyMap()));
    }

    @Test
    @Order(20)
    @DisabledIfSystemProperty(named = "keycloak.version", matches = "1[23].0.*", disabledReason = "https://issues.redhat.com/browse/KEYCLOAK-18035")
    void shouldCreateClientAddDefaultClientScope() throws IOException {
        doImport("20_update_realm__create_client_add_default_scope.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        assertThat(getClientByName(realm, "auth-moped-client"), notNullValue());
        assertThat(getClientByName(realm, "moped-client"), notNullValue());

        ClientRepresentation client = getClientByName(realm, "default-client-scope-client");
        assertThat(client.getName(), is("default-client-scope-client"));
        assertThat(client.getClientId(), is("default-client-scope-client"));
        assertThat(client.getDescription(), is("Default-Client-Another-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(client.isServiceAccountsEnabled(), is(false));
        assertThat(client.getDefaultClientScopes(), is(containsInAnyOrder("custom-address", "address")));
        assertThat(client.getOptionalClientScopes(), is(containsInAnyOrder("custom-email", "email")));
    }

    @Test
    @Order(21)
    @DisabledIfSystemProperty(named = "keycloak.version", matches = "1[23].0.*", disabledReason = "https://issues.redhat.com/browse/KEYCLOAK-18035")
    void shouldUpdateClientUpdateDefaultClientScope() throws IOException {
        doImport("21_update_realm__update_client_update_default_scope.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        assertThat(getClientByName(realm, "auth-moped-client"), notNullValue());
        assertThat(getClientByName(realm, "moped-client"), notNullValue());

        ClientRepresentation client = getClientByName(realm, "default-client-scope-client");
        assertThat(client.getName(), is("default-client-scope-client"));
        assertThat(client.getClientId(), is("default-client-scope-client"));
        assertThat(client.getDescription(), is("Default-Client-Another-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(client.isServiceAccountsEnabled(), is(false));
        assertThat(client.getDefaultClientScopes(), is(containsInAnyOrder("address", "custom-email")));
        assertThat(client.getOptionalClientScopes(), is(containsInAnyOrder("email", "custom-address")));
    }

    @Test
    @Order(22)
    @DisabledIfSystemProperty(named = "keycloak.version", matches = "1[23].0.*", disabledReason = "https://issues.redhat.com/browse/KEYCLOAK-18035")
    void shouldUpdateClientSkipDefaultClientScope() throws IOException {
        doImport("22_update_realm__update_client_skip_default_scope.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        assertThat(getClientByName(realm, "auth-moped-client"), notNullValue());
        assertThat(getClientByName(realm, "moped-client"), notNullValue());

        ClientRepresentation client = getClientByName(realm, "default-client-scope-client");
        assertThat(client.getName(), is("default-client-scope-client"));
        assertThat(client.getClientId(), is("default-client-scope-client"));
        assertThat(client.getDescription(), is("Default-Client-Another-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(client.isServiceAccountsEnabled(), is(false));
        assertThat(client.getDefaultClientScopes(), is(containsInAnyOrder("address", "custom-email")));
        assertThat(client.getOptionalClientScopes(), is(containsInAnyOrder("email", "custom-address")));
    }

    @Test
    @Order(23)
    @DisabledIfSystemProperty(named = "keycloak.version", matches = "1[23].0.*", disabledReason = "https://issues.redhat.com/browse/KEYCLOAK-18035")
    void shouldUpdateClientRemoveDefaultClientScope() throws IOException {
        doImport("23_update_realm__update_client_remove_default_scope.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        assertThat(getClientByName(realm, "auth-moped-client"), notNullValue());
        assertThat(getClientByName(realm, "moped-client"), notNullValue());

        ClientRepresentation client = getClientByName(realm, "default-client-scope-client");
        assertThat(client.getName(), is("default-client-scope-client"));
        assertThat(client.getClientId(), is("default-client-scope-client"));
        assertThat(client.getDescription(), is("Default-Client-Another-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(client.isServiceAccountsEnabled(), is(false));
        assertThat(client.getDefaultClientScopes(), is(empty()));
        assertThat(client.getOptionalClientScopes(), is(empty()));
    }

    @Test
    @Order(24)
    @DisabledIfSystemProperty(named = "keycloak.version", matches = "1[23].0.*", disabledReason = "https://issues.redhat.com/browse/KEYCLOAK-18035")
    void shouldUpdateClientAddCustomDefaultClientScope() throws IOException {
        doImport("24_update_realm__update_client_add_custom_default_scope.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        assertThat(getClientByName(realm, "auth-moped-client"), notNullValue());
        assertThat(getClientByName(realm, "moped-client"), notNullValue());

        ClientRepresentation client = getClientByName(realm, "default-client-scope-client");
        assertThat(client.getName(), is("default-client-scope-client"));
        assertThat(client.getClientId(), is("default-client-scope-client"));
        assertThat(client.getDescription(), is("Default-Client-Another-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(client.isServiceAccountsEnabled(), is(false));
        assertThat(client.getDefaultClientScopes(), is(containsInAnyOrder("custom-scope")));
        assertThat(client.getOptionalClientScopes(), is(containsInAnyOrder("custom-email")));
    }

    @Test
    @Order(25)
    @DisabledIfSystemProperty(named = "keycloak.version", matches = "1[23].0.*", disabledReason = "https://issues.redhat.com/browse/KEYCLOAK-18035")
    void shouldUpdateClientUpdateCustomDefaultClientScope() throws IOException {
        doImport("25_update_realm__update_client_update_custom_default_scope.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        assertThat(getClientByName(realm, "auth-moped-client"), notNullValue());
        assertThat(getClientByName(realm, "moped-client"), notNullValue());

        ClientRepresentation client = getClientByName(realm, "default-client-scope-client");
        assertThat(client.getName(), is("default-client-scope-client"));
        assertThat(client.getClientId(), is("default-client-scope-client"));
        assertThat(client.getDescription(), is("Default-Client-Another-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(client.isServiceAccountsEnabled(), is(false));
        assertThat(client.getDefaultClientScopes(), is(containsInAnyOrder("custom-scope")));
        assertThat(client.getOptionalClientScopes(), is(containsInAnyOrder("custom-email")));
    }

    @Test
    @Order(26)
    @DisabledIfSystemProperty(named = "keycloak.version", matches = "1[23].0.*", disabledReason = "https://issues.redhat.com/browse/KEYCLOAK-18035")
    void shouldUpdateClientRemoveCustomDefaultClientScope() throws IOException {
        doImport("26_update_realm__update_client_remove_custom_default_scope.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        assertThat(getClientByName(realm, "auth-moped-client"), notNullValue());
        assertThat(getClientByName(realm, "moped-client"), notNullValue());

        ClientRepresentation client = getClientByName(realm, "default-client-scope-client");
        assertThat(client.getName(), is("default-client-scope-client"));
        assertThat(client.getClientId(), is("default-client-scope-client"));
        assertThat(client.getDescription(), is("Default-Client-Another-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(client.isServiceAccountsEnabled(), is(false));
        assertThat(client.getDefaultClientScopes(), is(empty()));
        assertThat(client.getOptionalClientScopes(), is(empty()));
    }

    @Test
    @Order(70)
    void shouldCreateRealmWithClientWithAuthenticationFlowBindingOverrides() throws IOException {
        doImport("70_create_realm__with_client_with_auth-flow-overrides.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_AUTH_FLOW_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_AUTH_FLOW_NAME));
        assertThat(realm.isEnabled(), is(true));

        // Check is flow are imported, only check existence since there is many tests' case on AuthFlow
        assertThat(getAuthenticationFlow(realm, "custom flow"), notNullValue());

        ClientRepresentation client = getClientByName(realm, "moped-client");
        assertThat(client.getName(), is("moped-client"));
        assertThat(client.getClientId(), is("moped-client"));
        assertThat(client.getDescription(), is("Moped-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(client.isServiceAccountsEnabled(), is(false));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(REALM_AUTH_FLOW_NAME, client.getId());
        assertThat(clientSecret, is("my-special-client-secret"));

        // ... and finally assert that we really want
        assertThat(client.getAuthenticationFlowBindingOverrides().entrySet(), hasSize(1));
        assertThat(client.getAuthenticationFlowBindingOverrides(), allOf(hasEntry("browser", getAuthenticationFlow(realm, "custom flow").getId())));
    }

    @Test
    @Order(41)
    void shouldAddAuthzPoliciesForRealmManagement() throws IOException {
        doImport("41_update_realm_add_authz_policy_realm-management.json");

        String REALM_NAME = "realmWithClientsForAuthzGrantedPolicies";

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientRepresentation client;
        client = getClientByClientId(realm, "fine-grained-permission-client-id");
        assertThat(client, notNullValue());
        assertThat(client.getId(), is("50eadf70-6e80-4f1d-ba0d-85cafa3c1dc7"));
        assertThat(client.getClientId(), is("fine-grained-permission-client-id"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.isBearerOnly(), is(false));
        assertThat(client.isConsentRequired(), is(false));
        assertThat(client.isStandardFlowEnabled(), is(true));
        assertThat(client.isImplicitFlowEnabled(), is(false));
        assertThat(client.isDirectAccessGrantsEnabled(), is(true));
        assertThat(client.isServiceAccountsEnabled(), is(false));
        assertThat(client.isPublicClient(), is(true));
        assertThat(client.getProtocol(), is("openid-connect"));

        String clientFineGrainedPermissionId = client.getId();
        assertThat(
                keycloakProvider.getInstance().realm(REALM_NAME).clients().get(clientFineGrainedPermissionId).getPermissions().isEnabled(),
                is(true)
        );

        client = getClientByClientId(realm, "realm-management");
        assertThat(client, notNullValue());
        assertThat(client.getClientId(), is("realm-management"));
        assertThat(client.getName(), is("${client_realm-management}"));
        assertThat(client.isSurrogateAuthRequired(), is(false));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.isAlwaysDisplayInConsole(), is(false));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), empty());
        assertThat(client.getWebOrigins(), empty());
        assertThat(client.getNotBefore(), is(0));
        assertThat(client.isBearerOnly(), is(true));
        assertThat(client.isConsentRequired(), is(false));
        assertThat(client.isStandardFlowEnabled(), is(true));
        assertThat(client.isImplicitFlowEnabled(), is(false));
        assertThat(client.isDirectAccessGrantsEnabled(), is(false));
        assertThat(client.isServiceAccountsEnabled(), is(false));
        assertThat(client.isServiceAccountsEnabled(), is(false));
        assertThat(client.getAuthorizationServicesEnabled(), is(true));
        assertThat(client.isFrontchannelLogout(), is(false));
        assertThat(client.getProtocol(), is("openid-connect"));
        assertThat(client.getAuthenticationFlowBindingOverrides(), anEmptyMap());
        assertThat(client.isFullScopeAllowed(), is(false));
        assertThat(client.getNodeReRegistrationTimeout(), is(0));
        assertThat(client.getDefaultClientScopes(), containsInAnyOrder("web-origins", "profile", "roles", "email"));
        assertThat(client.getOptionalClientScopes(), containsInAnyOrder("address", "phone", "offline_access", "microprofile-jwt"));

        checkClientAttributes(client);

        String[] clientsIds = new String[]{clientFineGrainedPermissionId};
        String[] scopeNames = new String[]{
                "manage",
                "view",
                "map-roles",
                "map-roles-client-scope",
                "map-roles-composite",
                "configure",
                "token-exchange",
                "keycloak-config-cli-1"
        };

        ResourceServerRepresentation authorizationSettings = client.getAuthorizationSettings();
        assertThat(authorizationSettings.isAllowRemoteResourceManagement(), is(false));
        assertThat(authorizationSettings.getPolicyEnforcementMode(), is(PolicyEnforcementMode.ENFORCING));
        assertThat(authorizationSettings.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));

        List<ResourceRepresentation> resources = authorizationSettings.getResources();
        assertThat(resources, hasSize(1));

        ResourceRepresentation resource;
        resource = getAuthorizationSettingsResource(resources, "client.resource." + clientFineGrainedPermissionId);
        assertThat(resource.getType(), is("Client"));
        assertThat(resource.getOwnerManagedAccess(), is(false));
        assertThat(resource.getScopes().stream().map(ScopeRepresentation::getName).toList(), containsInAnyOrder(scopeNames));

        List<PolicyRepresentation> policies = authorizationSettings.getPolicies();

        PolicyRepresentation policy;
        policy = getAuthorizationPolicy(policies, "clientadmin-policy");
        assertThat(policy.getType(), is("group"));
        assertThat(policy.getLogic(), is(Logic.POSITIVE));
        assertThat(policy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(policy.getConfig(), aMapWithSize(1));
        assertThat(policy.getConfig(), hasEntry(equalTo("groups"), equalTo("[{\"path\":\"/client-admin-group\",\"extendChildren\":false}]")));

        for (String clientsId : clientsIds) {
            for (String scope : scopeNames) {
                policy = getAuthorizationPolicy(policies, scope + ".permission.client." + clientsId);
                assertThat(scope + ".permission.client." + clientsId, policy, notNullValue());
                assertThat(policy.getType(), is("scope"));
                assertThat(policy.getLogic(), is(Logic.POSITIVE));
                assertThat(policy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
                assertThat(policy.getConfig(), hasEntry(equalTo("resources"), equalTo("[\"client.resource." + clientsId + "\"]")));
                assertThat(policy.getConfig(), hasEntry(equalTo("scopes"), equalTo("[\"" + scope + "\"]")));

                if (policy.getName().startsWith("configure.permission.client")) {
                    assertThat(policy.getConfig(), hasEntry(equalTo("applyPolicies"), equalTo("[\"clientadmin-policy\"]")));
                    assertThat(policy.getConfig(), aMapWithSize(3));
                } else {
                    assertThat(policy.getConfig(), aMapWithSize(2));
                }
            }
        }
        assertThat(policies, hasSize(1 + clientsIds.length * scopeNames.length));

        assertThat(authorizationSettings.getScopes().stream().map(ScopeRepresentation::getName).toList(), containsInAnyOrder(scopeNames));
    }

    @Test
    @Order(42)
    void shouldUpdateAuthzPoliciesForRealmManagement() throws IOException {
        doImport("42_update_realm_update_authz_policy_realm-management.json");

        String REALM_NAME = "realmWithClientsForAuthzGrantedPolicies";

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientRepresentation client;
        client = getClientByClientId(realm, "fine-grained-permission-client-id");
        assertThat(client, notNullValue());
        assertThat(client.getId(), is("50eadf70-6e80-4f1d-ba0d-85cafa3c1dc7"));
        assertThat(client.getClientId(), is("fine-grained-permission-client-id"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.isBearerOnly(), is(false));
        assertThat(client.isConsentRequired(), is(false));
        assertThat(client.isStandardFlowEnabled(), is(true));
        assertThat(client.isImplicitFlowEnabled(), is(false));
        assertThat(client.isDirectAccessGrantsEnabled(), is(true));
        assertThat(client.isServiceAccountsEnabled(), is(false));
        assertThat(client.isPublicClient(), is(true));
        assertThat(client.getProtocol(), is("openid-connect"));

        String clientFineGrainedPermissionId = client.getId();
        assertThat(
                keycloakProvider.getInstance().realm(REALM_NAME).clients().get(clientFineGrainedPermissionId).getPermissions().isEnabled(),
                is(true)
        );


        client = getClientByClientId(realm, "z-fine-grained-permission-client-without-id");
        assertThat(client, notNullValue());
        assertThat(client.getClientId(), is("z-fine-grained-permission-client-without-id"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.isBearerOnly(), is(false));
        assertThat(client.isConsentRequired(), is(false));
        assertThat(client.isStandardFlowEnabled(), is(true));
        assertThat(client.isImplicitFlowEnabled(), is(false));
        assertThat(client.isDirectAccessGrantsEnabled(), is(true));
        assertThat(client.isServiceAccountsEnabled(), is(false));
        assertThat(client.isPublicClient(), is(true));
        assertThat(client.getProtocol(), is("openid-connect"));

        String clientZFineGrainedPermissionWithoutIdId = client.getId();
        assertThat(
                keycloakProvider.getInstance().realm(REALM_NAME).clients().get(clientZFineGrainedPermissionWithoutIdId).getPermissions().isEnabled(),
                is(true)
        );


        client = getClientByClientId(realm, "realm-management");
        assertThat(client, notNullValue());
        assertThat(client.getClientId(), is("realm-management"));
        assertThat(client.getName(), is("${client_realm-management}"));
        assertThat(client.isSurrogateAuthRequired(), is(false));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.isAlwaysDisplayInConsole(), is(false));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), empty());
        assertThat(client.getWebOrigins(), empty());
        assertThat(client.getNotBefore(), is(0));
        assertThat(client.isBearerOnly(), is(true));
        assertThat(client.isConsentRequired(), is(false));
        assertThat(client.isStandardFlowEnabled(), is(true));
        assertThat(client.isImplicitFlowEnabled(), is(false));
        assertThat(client.isDirectAccessGrantsEnabled(), is(false));
        assertThat(client.isServiceAccountsEnabled(), is(false));
        assertThat(client.isServiceAccountsEnabled(), is(false));
        assertThat(client.getAuthorizationServicesEnabled(), is(true));
        assertThat(client.isFrontchannelLogout(), is(false));
        assertThat(client.getProtocol(), is("openid-connect"));
        assertThat(client.getAuthenticationFlowBindingOverrides(), anEmptyMap());
        assertThat(client.isFullScopeAllowed(), is(false));
        assertThat(client.getNodeReRegistrationTimeout(), is(0));
        assertThat(client.getDefaultClientScopes(), containsInAnyOrder("web-origins", "profile", "roles", "email"));
        assertThat(client.getOptionalClientScopes(), containsInAnyOrder("address", "phone", "offline_access", "microprofile-jwt"));

        checkClientAttributes(client);

        String[] clientsIds = new String[]{clientFineGrainedPermissionId, clientZFineGrainedPermissionWithoutIdId};
        String[] scopeNames = new String[]{
                "manage",
                "view",
                "map-roles",
                "map-roles-client-scope",
                "map-roles-composite",
                "configure",
                "token-exchange",
                "keycloak-config-cli-2"
        };

        ResourceServerRepresentation authorizationSettings = client.getAuthorizationSettings();
        assertThat(authorizationSettings.isAllowRemoteResourceManagement(), is(false));
        assertThat(authorizationSettings.getPolicyEnforcementMode(), is(PolicyEnforcementMode.ENFORCING));
        assertThat(authorizationSettings.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));

        List<ResourceRepresentation> resources = authorizationSettings.getResources();
        assertThat(resources, hasSize(2));

        ResourceRepresentation resource;
        resource = getAuthorizationSettingsResource(resources, "client.resource." + clientFineGrainedPermissionId);
        assertThat(resource.getType(), is("Client"));
        assertThat(resource.getOwnerManagedAccess(), is(false));
        assertThat(resource.getScopes().stream().map(ScopeRepresentation::getName).toList(), containsInAnyOrder(scopeNames));

        resource = getAuthorizationSettingsResource(resources, "client.resource." + clientZFineGrainedPermissionWithoutIdId);
        assertThat(resource.getType(), is("Client"));
        assertThat(resource.getOwnerManagedAccess(), is(false));
        assertThat(resource.getScopes().stream().map(ScopeRepresentation::getName).toList(), containsInAnyOrder(scopeNames));

        List<PolicyRepresentation> policies = authorizationSettings.getPolicies();

        PolicyRepresentation policy;
        policy = getAuthorizationPolicy(policies, "clientadmin-policy");
        assertThat(policy.getType(), is("group"));
        assertThat(policy.getLogic(), is(Logic.POSITIVE));
        assertThat(policy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(policy.getConfig(), aMapWithSize(1));
        assertThat(policy.getConfig(), hasEntry(equalTo("groups"), equalTo("[{\"path\":\"/client-admin-group\",\"extendChildren\":false}]")));

        for (String clientsId : clientsIds) {
            for (String scope : scopeNames) {
                policy = getAuthorizationPolicy(policies, scope + ".permission.client." + clientsId);
                assertThat(scope + ".permission.client." + clientsId, policy, notNullValue());
                assertThat(policy.getType(), is("scope"));
                assertThat(policy.getLogic(), is(Logic.POSITIVE));
                assertThat(policy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
                assertThat(policy.getConfig(), hasEntry(equalTo("resources"), equalTo("[\"client.resource." + clientsId + "\"]")));
                assertThat(policy.getConfig(), hasEntry(equalTo("scopes"), equalTo("[\"" + scope + "\"]")));

                if (policy.getName().startsWith("configure.permission.client")) {
                    assertThat(policy.getConfig(), hasEntry(equalTo("applyPolicies"), equalTo("[\"clientadmin-policy\"]")));
                    assertThat(policy.getConfig(), aMapWithSize(3));
                } else {
                    assertThat(policy.getConfig(), aMapWithSize(2));
                }
            }
        }
        assertThat(policies, hasSize(1 + clientsIds.length * scopeNames.length));

        List<String> scopes = authorizationSettings.getScopes().stream().map(ScopeRepresentation::getName).toList();
        assertThat(scopes, containsInAnyOrder(scopeNames));
    }

    @Test
    @Order(43)
    void shouldRemoveClientAndAuthzPoliciesForRealmManagement() throws IOException {
        doImport("43_update_realm_remove_client_and_authz_policy_realm-management.json");

        String REALM_NAME = "realmWithClientsForAuthzGrantedPolicies";

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientRepresentation client;
        client = getClientByClientId(realm, "z-fine-grained-permission-client-without-id");
        assertThat(client, notNullValue());
        assertThat(client.getClientId(), is("z-fine-grained-permission-client-without-id"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.isBearerOnly(), is(false));
        assertThat(client.isConsentRequired(), is(false));
        assertThat(client.isStandardFlowEnabled(), is(true));
        assertThat(client.isImplicitFlowEnabled(), is(false));
        assertThat(client.isDirectAccessGrantsEnabled(), is(true));
        assertThat(client.isServiceAccountsEnabled(), is(false));
        assertThat(client.isPublicClient(), is(true));
        assertThat(client.getProtocol(), is("openid-connect"));

        String clientZFineGrainedPermissionWithoutIdId = client.getId();
        assertThat(
                keycloakProvider.getInstance().realm(REALM_NAME).clients().get(clientZFineGrainedPermissionWithoutIdId).getPermissions().isEnabled(),
                is(true)
        );


        client = getClientByClientId(realm, "realm-management");
        assertThat(client, notNullValue());
        assertThat(client.getClientId(), is("realm-management"));
        assertThat(client.getName(), is("${client_realm-management}"));
        assertThat(client.isSurrogateAuthRequired(), is(false));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.isAlwaysDisplayInConsole(), is(false));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), empty());
        assertThat(client.getWebOrigins(), empty());
        assertThat(client.getNotBefore(), is(0));
        assertThat(client.isBearerOnly(), is(true));
        assertThat(client.isConsentRequired(), is(false));
        assertThat(client.isStandardFlowEnabled(), is(true));
        assertThat(client.isImplicitFlowEnabled(), is(false));
        assertThat(client.isDirectAccessGrantsEnabled(), is(false));
        assertThat(client.isServiceAccountsEnabled(), is(false));
        assertThat(client.isServiceAccountsEnabled(), is(false));
        assertThat(client.getAuthorizationServicesEnabled(), is(true));
        assertThat(client.isFrontchannelLogout(), is(false));
        assertThat(client.getProtocol(), is("openid-connect"));
        assertThat(client.getAuthenticationFlowBindingOverrides(), anEmptyMap());
        assertThat(client.isFullScopeAllowed(), is(false));
        assertThat(client.getNodeReRegistrationTimeout(), is(0));
        assertThat(client.getDefaultClientScopes(), containsInAnyOrder("web-origins", "profile", "roles", "email"));
        assertThat(client.getOptionalClientScopes(), containsInAnyOrder("address", "phone", "offline_access", "microprofile-jwt"));

        checkClientAttributes(client);

        String[] clientsIds = new String[]{clientZFineGrainedPermissionWithoutIdId};
        String[] scopeNames = new String[]{
                "manage",
                "view",
                "map-roles",
                "map-roles-client-scope",
                "map-roles-composite",
                "configure",
                "token-exchange",
        };

        ResourceServerRepresentation authorizationSettings = client.getAuthorizationSettings();
        assertThat(authorizationSettings.isAllowRemoteResourceManagement(), is(false));
        assertThat(authorizationSettings.getPolicyEnforcementMode(), is(PolicyEnforcementMode.ENFORCING));
        assertThat(authorizationSettings.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));

        List<ResourceRepresentation> resources = authorizationSettings.getResources();
        assertThat(resources, hasSize(1));

        ResourceRepresentation resource;
        resource = getAuthorizationSettingsResource(resources, "client.resource." + clientZFineGrainedPermissionWithoutIdId);
        assertThat(resource.getType(), is("Client"));
        assertThat(resource.getOwnerManagedAccess(), is(false));
        assertThat(resource.getScopes().stream().map(ScopeRepresentation::getName).toList(), containsInAnyOrder(scopeNames));

        List<PolicyRepresentation> policies = authorizationSettings.getPolicies();

        PolicyRepresentation policy;

        for (String clientsId : clientsIds) {
            for (String scope : scopeNames) {
                policy = getAuthorizationPolicy(policies, scope + ".permission.client." + clientsId);
                assertThat(scope + ".permission.client." + clientsId, policy, notNullValue());
                assertThat(policy.getType(), is("scope"));
                assertThat(policy.getLogic(), is(Logic.POSITIVE));
                assertThat(policy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
                assertThat(policy.getConfig(), hasEntry(equalTo("resources"), equalTo("[\"client.resource." + clientsId + "\"]")));
                assertThat(policy.getConfig(), hasEntry(equalTo("scopes"), equalTo("[\"" + scope + "\"]")));
                assertThat(policy.getConfig(), aMapWithSize(2));
            }
        }

        assertThat(policies, hasSize(clientsIds.length * scopeNames.length));

        List<String> scopes = authorizationSettings.getScopes().stream().map(ScopeRepresentation::getName).toList();
        assertThat(scopes, containsInAnyOrder(scopeNames));
    }

    @Test
    @Order(44)
    void shouldRemoveAuthzPoliciesForRealmManagement() throws IOException {
        doImport("44_update_realm_remove_authz_policy_realm-management.json");

        String REALM_NAME = "realmWithClientsForAuthzGrantedPolicies";

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientRepresentation client;
        client = getClientByClientId(realm, "z-fine-grained-permission-client-without-id");
        assertThat(client, notNullValue());
        assertThat(client.getClientId(), is("z-fine-grained-permission-client-without-id"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.isBearerOnly(), is(false));
        assertThat(client.isConsentRequired(), is(false));
        assertThat(client.isStandardFlowEnabled(), is(true));
        assertThat(client.isImplicitFlowEnabled(), is(false));
        assertThat(client.isDirectAccessGrantsEnabled(), is(true));
        assertThat(client.isServiceAccountsEnabled(), is(false));
        assertThat(client.isPublicClient(), is(true));
        assertThat(client.getProtocol(), is("openid-connect"));

        String clientZFineGrainedPermissionWithoutIdId = client.getId();
        assertThat(
                keycloakProvider.getInstance().realm(REALM_NAME).clients().get(clientZFineGrainedPermissionWithoutIdId).getPermissions().isEnabled(),
                is(false)
        );


        client = getClientByClientId(realm, "realm-management");
        assertThat(client, notNullValue());
        assertThat(client.getClientId(), is("realm-management"));
        assertThat(client.getName(), is("${client_realm-management}"));
        assertThat(client.isSurrogateAuthRequired(), is(false));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.isAlwaysDisplayInConsole(), is(false));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), empty());
        assertThat(client.getWebOrigins(), empty());
        assertThat(client.getNotBefore(), is(0));
        assertThat(client.isBearerOnly(), is(false));
        assertThat(client.isConsentRequired(), is(false));
        assertThat(client.isStandardFlowEnabled(), is(true));
        assertThat(client.isImplicitFlowEnabled(), is(false));
        assertThat(client.isDirectAccessGrantsEnabled(), is(false));
        assertThat(client.isServiceAccountsEnabled(), is(true));
        assertThat(client.getAuthorizationServicesEnabled(), is(true));
        assertThat(client.isFrontchannelLogout(), is(false));
        assertThat(client.getProtocol(), is("openid-connect"));
        assertThat(client.getAuthenticationFlowBindingOverrides(), anEmptyMap());
        assertThat(client.isFullScopeAllowed(), is(false));
        assertThat(client.getNodeReRegistrationTimeout(), is(0));
        assertThat(client.getDefaultClientScopes(), containsInAnyOrder("web-origins", "profile", "roles", "email"));
        assertThat(client.getOptionalClientScopes(), containsInAnyOrder("address", "phone", "offline_access", "microprofile-jwt"));

        if (VersionUtil.lt(KEYCLOAK_VERSION, "26")) {
            assertThat(client.getAttributes(), hasKey("client.secret.creation.time"));
        } else {
            // https://github.com/keycloak/keycloak/pull/30433 Added attribute to recognize realm client
            assertThat(client.getAttributes(), hasEntry("realm_client", "true"));
        }

        ResourceServerRepresentation authorizationSettings = client.getAuthorizationSettings();
        assertThat(authorizationSettings.isAllowRemoteResourceManagement(), is(false));
        assertThat(authorizationSettings.getPolicyEnforcementMode(), is(PolicyEnforcementMode.ENFORCING));
        assertThat(authorizationSettings.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));

        List<ResourceRepresentation> resources = authorizationSettings.getResources();
        assertThat(resources, empty());

        List<PolicyRepresentation> policies = authorizationSettings.getPolicies();
        assertThat(policies, empty());

        List<String> scopes = authorizationSettings.getScopes().stream().map(ScopeRepresentation::getName).toList();
        assertThat(scopes, empty());
    }

    @Test
    @Order(45)
    void shouldUpdateAuthzPoliciesPerIdentityProvidersForRealmManagement() throws IOException {
        doImport("45_update_realm_update_authz_policy_for_idp_realm-management.json");

        String REALM_NAME = "realmWithClientsForAuthzGrantedPolicies";

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        IdentityProviderRepresentation providerWithId = getIdentityProviderByAlias(realm, "provider-with-id");
        assertThat(providerWithId, notNullValue());
        assertThat(providerWithId.getInternalId(), is("1dcbfbe7-1cee-4d42-8c39-d8ed74b4cf22"));
        assertThat(providerWithId.getProviderId(), is("oidc"));


        ClientRepresentation client = getClientByClientId(realm, "realm-management");

        String[] idpIds = new String[]{providerWithId.getInternalId()};
        String[] scopeNames = new String[]{
                "token-exchange"
        };

        ResourceServerRepresentation authorizationSettings = client.getAuthorizationSettings();
        assertThat(authorizationSettings.isAllowRemoteResourceManagement(), is(false));
        assertThat(authorizationSettings.getPolicyEnforcementMode(), is(PolicyEnforcementMode.ENFORCING));
        assertThat(authorizationSettings.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));

        List<ResourceRepresentation> resources = authorizationSettings.getResources();
        assertThat(resources, hasSize(1));

        ResourceRepresentation resource;
        resource = getAuthorizationSettingsResource(resources, "idp.resource." + providerWithId.getInternalId());
        assertThat(resource.getType(), is("IdentityProvider"));
        assertThat(resource.getOwnerManagedAccess(), is(false));
        assertThat(resource.getScopes().stream().map(ScopeRepresentation::getName).toList(), containsInAnyOrder(scopeNames));

        List<PolicyRepresentation> policies = authorizationSettings.getPolicies();

        PolicyRepresentation policy;
        policy = getAuthorizationPolicy(policies, "clientadmin-policy");
        assertThat(policy.getType(), is("group"));
        assertThat(policy.getLogic(), is(Logic.POSITIVE));
        assertThat(policy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(policy.getConfig(), aMapWithSize(1));
        assertThat(policy.getConfig(), hasEntry(equalTo("groups"), equalTo("[{\"path\":\"/client-admin-group\",\"extendChildren\":false}]")));

        for (String id : idpIds) {
            for (String scope : scopeNames) {
                policy = getAuthorizationPolicy(policies, scope + ".permission.idp." + id);
                assertThat(scope + ".permission.idp." + id, policy, notNullValue());
                assertThat(policy.getType(), is("scope"));
                assertThat(policy.getLogic(), is(Logic.POSITIVE));
                assertThat(policy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
                assertThat(policy.getConfig(), hasEntry(equalTo("resources"), equalTo("[\"idp.resource." + id + "\"]")));
                assertThat(policy.getConfig(), hasEntry(equalTo("scopes"), equalTo("[\"" + scope + "\"]")));

                assertThat(policy.getConfig(), hasEntry(equalTo("applyPolicies"), equalTo("[\"clientadmin-policy\"]")));
                assertThat(policy.getConfig(), aMapWithSize(3));
            }
        }
        assertThat(policies, hasSize(1 + idpIds.length * scopeNames.length));
    }


    @Test
    @Order(46)
    void shouldUpdateAuthzPoliciesPerIdentityProvidersWithPlaceholdersForRealmManagement() throws IOException {
        doImport("46_update_realm_update_authz_policy_for_idp_with_placeholder_realm-management.json");

        String REALM_NAME = "realmWithClientsForAuthzGrantedPolicies";

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        IdentityProviderRepresentation providerWithId = getIdentityProviderByAlias(realm, "provider-with-id");
        assertThat(providerWithId, notNullValue());
        assertThat(providerWithId.getInternalId(), is("1dcbfbe7-1cee-4d42-8c39-d8ed74b4cf22"));
        assertThat(providerWithId.getProviderId(), is("oidc"));

        IdentityProviderRepresentation providerWithoutId = getIdentityProviderByAlias(realm, "provider-without-id");
        assertThat(providerWithId, notNullValue());
        assertThat(providerWithId.getInternalId(), is(notNullValue()));
        assertThat(providerWithId.getProviderId(), is("oidc"));


        ClientRepresentation client = getClientByClientId(realm, "realm-management");

        String[] idpIds = new String[]{providerWithId.getInternalId(), providerWithoutId.getInternalId()};
        String[] scopeNames = new String[]{
                "token-exchange"
        };

        ResourceServerRepresentation authorizationSettings = client.getAuthorizationSettings();
        assertThat(authorizationSettings.isAllowRemoteResourceManagement(), is(false));
        assertThat(authorizationSettings.getPolicyEnforcementMode(), is(PolicyEnforcementMode.ENFORCING));
        assertThat(authorizationSettings.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));

        List<ResourceRepresentation> resources = authorizationSettings.getResources();
        assertThat(resources, hasSize(2));

        for (String id : idpIds) {
            ResourceRepresentation resource;
            resource = getAuthorizationSettingsResource(resources, "idp.resource." + id);
            assertThat(resource.getType(), is("IdentityProvider"));
            assertThat(resource.getOwnerManagedAccess(), is(false));
            assertThat(resource.getScopes().stream().map(ScopeRepresentation::getName).toList(), containsInAnyOrder(scopeNames));
        }

        List<PolicyRepresentation> policies = authorizationSettings.getPolicies();

        PolicyRepresentation policy;
        policy = getAuthorizationPolicy(policies, "clientadmin-policy");
        assertThat(policy.getType(), is("group"));
        assertThat(policy.getLogic(), is(Logic.POSITIVE));
        assertThat(policy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(policy.getConfig(), aMapWithSize(1));
        assertThat(policy.getConfig(), hasEntry(equalTo("groups"), equalTo("[{\"path\":\"/client-admin-group\",\"extendChildren\":false}]")));

        for (String id : idpIds) {
            for (String scope : scopeNames) {
                policy = getAuthorizationPolicy(policies, scope + ".permission.idp." + id);
                assertThat(scope + ".permission.idp." + id, policy, notNullValue());
                assertThat(policy.getType(), is("scope"));
                assertThat(policy.getLogic(), is(Logic.POSITIVE));
                assertThat(policy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
                assertThat(policy.getConfig(), hasEntry(equalTo("resources"), equalTo("[\"idp.resource." + id + "\"]")));
                assertThat(policy.getConfig(), hasEntry(equalTo("scopes"), equalTo("[\"" + scope + "\"]")));

                assertThat(policy.getConfig(), hasEntry(equalTo("applyPolicies"), equalTo("[\"clientadmin-policy\"]")));
                assertThat(policy.getConfig(), aMapWithSize(3));
            }
        }
        assertThat(policies, hasSize(1 + idpIds.length * scopeNames.length));
    }

    @Test
    @Order(47)
    void shouldUpdateAuthzPoliciesPerRolesWithPlaceholdersForRealmManagement() throws IOException {
        doImport("47_update_realm_update_authz_policy_for_role_with_placeholder_realm-management.json");

        String REALM_NAME = "realmWithClientsForAuthzGrantedPolicies";

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        RoleRepresentation role = getRealmRoleByName(realm, "My test role");
        assertThat(role, notNullValue());
        assertThat(role.getId(), is(notNullValue()));
        assertThat(role.getName(), is("My test role"));

        ClientRepresentation client = getClientByClientId(realm, "realm-management");

        String[] roleIds = new String[]{role.getId()};
        String[] scopeNames = new String[]{
                "map-role-composite", "map-role-client-scope", "map-role"
        };

        ResourceServerRepresentation authorizationSettings = client.getAuthorizationSettings();
        assertThat(authorizationSettings.isAllowRemoteResourceManagement(), is(false));
        assertThat(authorizationSettings.getPolicyEnforcementMode(), is(PolicyEnforcementMode.ENFORCING));
        assertThat(authorizationSettings.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));

        List<ResourceRepresentation> resources = authorizationSettings.getResources();
        assertThat(resources, hasSize(1));

        for (String id : roleIds) {
            ResourceRepresentation resource;
            resource = getAuthorizationSettingsResource(resources, "role.resource." + id);
            assertThat(resource.getType(), is("Role"));
            assertThat(resource.getOwnerManagedAccess(), is(false));
            assertThat(resource.getScopes().stream().map(ScopeRepresentation::getName).toList(), containsInAnyOrder(scopeNames));
        }

        List<PolicyRepresentation> policies = authorizationSettings.getPolicies();

        PolicyRepresentation policy;
        policy = getAuthorizationPolicy(policies, "clientadmin-policy");
        assertThat(policy.getType(), is("group"));
        assertThat(policy.getLogic(), is(Logic.POSITIVE));
        assertThat(policy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(policy.getConfig(), aMapWithSize(1));
        assertThat(policy.getConfig(), hasEntry(equalTo("groups"), equalTo("[{\"path\":\"/client-admin-group\",\"extendChildren\":false}]")));

        for (String id : roleIds) {
            for (String scope : scopeNames) {
                policy = getAuthorizationPolicy(policies, scope + ".permission." + id);
                assertThat(scope + ".permission." + id, policy, notNullValue());
                assertThat(policy.getType(), is("scope"));
                assertThat(policy.getLogic(), is(Logic.POSITIVE));
                assertThat(policy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
                assertThat(policy.getConfig(), hasEntry(equalTo("resources"), equalTo("[\"role.resource." + id + "\"]")));
                assertThat(policy.getConfig(), hasEntry(equalTo("scopes"), equalTo("[\"" + scope + "\"]")));

                if (policy.getName().startsWith("map-role.permission")) {
                    assertThat(policy.getConfig(), hasEntry(equalTo("applyPolicies"), equalTo("[\"clientadmin-policy\"]")));
                    assertThat(policy.getConfig(), aMapWithSize(3));
                } else {
                    assertThat(policy.getConfig(), aMapWithSize(2));
                }
            }
        }
        assertThat(policies, hasSize(1 + roleIds.length * scopeNames.length));
    }

    @Test
    @Order(48)
    void shouldUpdateAuthzPoliciesPerGroupsWithPlaceholdersForRealmManagement() throws IOException {
        doImport("48_update_realm_update_authz_policy_for_group_with_placeholder_realm-management.json");

        String REALM_NAME = "realmWithClientsForAuthzGrantedPolicies";

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        GroupRepresentation group = getGroupByPath(realm, "My test group", "My test group2");
        assertThat(group, notNullValue());
        assertThat(group.getId(), is(notNullValue()));
        assertThat(group.getName(), is("My test group2"));
        assertThat(group.getPath(), is("/My test group/My test group2"));

        ClientRepresentation client = getClientByClientId(realm, "realm-management");

        String[] groupIds = new String[]{group.getId()};
        // scopes at the beginning of policy names are different from the actual scope names
        // actual scopes are delimited by minus sign ('manage-members'), but when used in policy name, dot is used ("manage.members")
        String[] scopeNames = new String[]{
                "manage-members",
                "view",
                "manage-membership",
                "view-members",
                "manage"
        };

        ResourceServerRepresentation authorizationSettings = client.getAuthorizationSettings();
        assertThat(authorizationSettings.isAllowRemoteResourceManagement(), is(false));
        assertThat(authorizationSettings.getPolicyEnforcementMode(), is(PolicyEnforcementMode.ENFORCING));
        assertThat(authorizationSettings.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));

        List<ResourceRepresentation> resources = authorizationSettings.getResources();
        assertThat(resources, hasSize(1));

        for (String id : groupIds) {
            ResourceRepresentation resource;
            resource = getAuthorizationSettingsResource(resources, "group.resource." + id);
            assertThat(resource.getType(), is("Group"));
            assertThat(resource.getOwnerManagedAccess(), is(false));
            assertThat(resource.getScopes().stream().map(ScopeRepresentation::getName).toList(), containsInAnyOrder(scopeNames));
        }

        List<PolicyRepresentation> policies = authorizationSettings.getPolicies();

        PolicyRepresentation policy;
        policy = getAuthorizationPolicy(policies, "clientadmin-policy");
        assertThat(policy.getType(), is("group"));
        assertThat(policy.getLogic(), is(Logic.POSITIVE));
        assertThat(policy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
        assertThat(policy.getConfig(), aMapWithSize(1));
        assertThat(policy.getConfig(), hasEntry(equalTo("groups"), equalTo("[{\"path\":\"/client-admin-group\",\"extendChildren\":false}]")));

        for (String id : groupIds) {
            for (String scope : scopeNames) {
                String scopeInPolicy = scope.replace("-", ".");
                policy = getAuthorizationPolicy(policies, scopeInPolicy + ".permission.group." + id);
                assertThat(scopeInPolicy + ".permission.group." + id, policy, notNullValue());
                assertThat(policy.getType(), is("scope"));
                assertThat(policy.getLogic(), is(Logic.POSITIVE));
                assertThat(policy.getDecisionStrategy(), is(DecisionStrategy.UNANIMOUS));
                assertThat(policy.getConfig(), hasEntry(equalTo("resources"), equalTo("[\"group.resource." + id + "\"]")));
                assertThat(policy.getConfig(), hasEntry(equalTo("scopes"), equalTo("[\"" + scope + "\"]")));

                if (policy.getName().startsWith("manage.members.permission.group")) {
                    assertThat(policy.getConfig(), hasEntry(equalTo("applyPolicies"), equalTo("[\"clientadmin-policy\"]")));
                    assertThat(policy.getConfig(), aMapWithSize(3));
                } else {
                    assertThat(policy.getConfig(), aMapWithSize(2));
                }
            }
        }
        assertThat(policies, hasSize(1 + groupIds.length * scopeNames.length));
    }

    @Test
    @Order(49)
    void shouldTriggerErrorWhenReferencingMissingObjectsByNameInFineGrainedAuthz() throws IOException {
        ImportProcessingException thrown;

        RealmImport foundImport0 = getFirstImport("49.0_update_realm_update_authz_policy_for_unknown_type_with_placeholder_realm-management.json");
        thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport0));
        assertThat(thrown.getMessage(), is("Cannot resolve 'unknowntype.resource.$unknown-id' in realm 'realmWithClientsForAuthzGrantedPolicies', the type 'unknowntype' is not supported by keycloak-config-cli."));

        RealmImport foundImport1 = getFirstImport("49.1_update_realm_update_authz_policy_for_client_with_error_placeholder_realm-management.json");
        thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport1));
        assertThat(thrown.getMessage(), is("Cannot find client 'missing-client' in realm 'realmWithClientsForAuthzGrantedPolicies' for 'client.resource.$missing-client'"));

        RealmImport foundImport2 = getFirstImport("49.2_update_realm_update_authz_policy_for_idp_with_error_placeholder_realm-management.json");
        thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport2));
        assertThat(thrown.getMessage(), is("Cannot find identity provider with alias 'missing-provider' in realm 'realmWithClientsForAuthzGrantedPolicies' for 'idp.resource.$missing-provider'"));

        RealmImport foundImport3 = getFirstImport("49.3_update_realm_update_authz_policy_for_role_with_error_placeholder_realm-management.json");
        thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport3));
        assertThat(thrown.getMessage(), is("Cannot find realm role 'Missing role' in realm 'realmWithClientsForAuthzGrantedPolicies' for 'role.resource.$Missing role'"));

        RealmImport foundImport4 = getFirstImport("49.4_update_realm_update_authz_policy_for_group_with_error_placeholder_realm-management.json");
        thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport4));
        assertThat(thrown.getMessage(), is("Cannot find group with path 'Missing group' in realm 'realmWithClientsForAuthzGrantedPolicies' for 'group.resource.$Missing group'"));
    }

    @Test
    @Order(50)
    void shouldNotTriggerErrorWhenReferencingInvalidUuidInFineGrainedAuthz() throws IOException {
        // These scenarios do not use placeholders and instead reference objects by UUID - which do not need to exist.
        // Keycloak accepts this, and it sometimes even works (for objects that allow specifying UUID in creation and are created after the import)
        // This is how to partially support fine-grained authz for types that are not supported yet by keycloak-config-cli
        // will log a warning, but otherwise the import succeeds

        RealmImport foundImport0 = getFirstImport("50.0_update_realm_update_authz_policy_for_unknown_type_with_id_realm-management.json");
        assertDoesNotThrow(() -> realmImportService.doImport(foundImport0));

        RealmImport foundImport1 = getFirstImport("50.1_update_realm_update_authz_policy_for_client_with_bad_id_realm-management.json");
        assertDoesNotThrow(() -> realmImportService.doImport(foundImport1));

        RealmImport foundImport2 = getFirstImport("50.2_update_realm_update_authz_policy_for_idp_with_bad_id_realm-management.json");
        assertDoesNotThrow(() -> realmImportService.doImport(foundImport2));

        RealmImport foundImport3 = getFirstImport("50.3_update_realm_update_authz_policy_for_role_with_bad_id_realm-management.json");
        assertDoesNotThrow(() -> realmImportService.doImport(foundImport3));

        RealmImport foundImport4 = getFirstImport("50.4_update_realm_update_authz_policy_for_group_with_bad_id_realm-management.json");
        assertDoesNotThrow(() -> realmImportService.doImport(foundImport4));
    }

    @Test
    @Order(51)
    void updateRealmWithClientWithMoreThan100RolesInRealmManagementAuthorization() throws IOException {
        doImport("51_update_realm_with_client_with_more_than_100_roles_in_realm_management_authorization.json");
    }

    @Test
    @Order(71)
    void shouldAddClientWithAuthenticationFlowBindingOverrides() throws IOException {
        doImport("71_update_realm__add_client_with_auth-flow-overrides.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_AUTH_FLOW_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_AUTH_FLOW_NAME));
        assertThat(realm.isEnabled(), is(true));

        // Check is flow are imported, only check existence since there is many tests' case on AuthFlow
        assertThat(getAuthenticationFlow(realm, "custom flow"), notNullValue());

        ClientRepresentation client = getClientByName(realm, "moped-client");
        assertThat(client.getName(), is("moped-client"));
        assertThat(client.getClientId(), is("moped-client"));
        assertThat(client.getDescription(), is("Moped-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(client.isServiceAccountsEnabled(), is(false));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(REALM_AUTH_FLOW_NAME, client.getId());
        assertThat(clientSecret, is("my-special-client-secret"));

        // ... and finally assert that we really want
        assertThat(client.getAuthenticationFlowBindingOverrides().entrySet(), hasSize(1));
        assertThat(client.getAuthenticationFlowBindingOverrides(), allOf(hasEntry("browser", getAuthenticationFlow(realm, "custom flow").getId())));

        ClientRepresentation anotherClient = getClientByName(realm, "another-client");
        assertThat(anotherClient.getName(), is("another-client"));
        assertThat(anotherClient.getClientId(), is("another-client"));
        assertThat(anotherClient.getDescription(), is("Another-Client"));
        assertThat(anotherClient.isEnabled(), is(true));
        assertThat(anotherClient.getClientAuthenticatorType(), is("client-secret"));
        assertThat(anotherClient.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(anotherClient.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(anotherClient.isServiceAccountsEnabled(), is(false));

        // ... and has to be retrieved separately
        String anotherClientSecret = getClientSecret(REALM_AUTH_FLOW_NAME, anotherClient.getId());
        assertThat(anotherClientSecret, is("my-special-client-secret"));

        // ... and finally assert that we really want
        assertThat(anotherClient.getAuthenticationFlowBindingOverrides().entrySet(), hasSize(1));
        assertThat(anotherClient.getAuthenticationFlowBindingOverrides(), allOf(hasEntry("browser", getAuthenticationFlow(realm, "custom flow").getId())));
    }

    @Test
    @Order(72)
    void shouldClearAuthenticationFlowBindingOverrides() throws IOException {
        doImport("72_update_realm__clear_auth-flow-overrides.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_AUTH_FLOW_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_AUTH_FLOW_NAME));
        assertThat(realm.isEnabled(), is(true));

        // Check is flow are imported, only check existence since there is many tests' case on AuthFlow
        assertThat(getAuthenticationFlow(realm, "custom flow"), notNullValue());

        ClientRepresentation client = getClientByName(realm, "moped-client");
        assertThat(client.getName(), is("moped-client"));
        assertThat(client.getClientId(), is("moped-client"));
        assertThat(client.getDescription(), is("Moped-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(client.isServiceAccountsEnabled(), is(false));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(REALM_AUTH_FLOW_NAME, client.getId());
        assertThat(clientSecret, is("my-special-client-secret"));

        // ... and finally assert that we really want
        assertThat(client.getAuthenticationFlowBindingOverrides(), equalTo(Collections.emptyMap()));

        ClientRepresentation anotherClient = getClientByName(realm, "another-client");
        assertThat(anotherClient.getName(), is("another-client"));
        assertThat(anotherClient.getClientId(), is("another-client"));
        assertThat(anotherClient.getDescription(), is("Another-Client"));
        assertThat(anotherClient.isEnabled(), is(true));
        assertThat(anotherClient.getClientAuthenticatorType(), is("client-secret"));
        assertThat(anotherClient.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(anotherClient.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(anotherClient.isServiceAccountsEnabled(), is(false));

        // ... and has to be retrieved separately
        String anotherClientSecret = getClientSecret(REALM_AUTH_FLOW_NAME, anotherClient.getId());
        assertThat(anotherClientSecret, is("my-special-client-secret"));

        // ... and finally assert that we really want
        assertThat(anotherClient.getAuthenticationFlowBindingOverrides(), equalTo(Collections.emptyMap()));
    }

    @Test
    @Order(73)
    void shouldSetAuthenticationFlowBindingOverridesByIds() throws IOException {
        doImport("73_update_realm__set_auth-flow-overrides-with-id.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_AUTH_FLOW_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_AUTH_FLOW_NAME));
        assertThat(realm.isEnabled(), is(true));

        // Check is flow are imported, only check existence since there is many tests' case on AuthFlow
        assertThat(getAuthenticationFlow(realm, "custom exported flow"), notNullValue());

        ClientRepresentation client = getClientByName(realm, "moped-client");
        assertThat(client.getName(), is("moped-client"));
        assertThat(client.getClientId(), is("moped-client"));
        assertThat(client.getDescription(), is("Moped-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(client.isServiceAccountsEnabled(), is(false));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(REALM_AUTH_FLOW_NAME, client.getId());
        assertThat(clientSecret, is("my-special-client-secret"));

        // ... and finally assert that we really want
        assertThat(client.getAuthenticationFlowBindingOverrides().entrySet(), hasSize(1));
        assertThat(client.getAuthenticationFlowBindingOverrides(), allOf(hasEntry("browser", "fbee9bfe-430a-48ac-8ef7-00dd17a1ab43")));

        ClientRepresentation anotherClient = getClientByName(realm, "another-client");
        assertThat(anotherClient.getName(), is("another-client"));
        assertThat(anotherClient.getClientId(), is("another-client"));
        assertThat(anotherClient.getDescription(), is("Another-Client"));
        assertThat(anotherClient.isEnabled(), is(true));
        assertThat(anotherClient.getClientAuthenticatorType(), is("client-secret"));
        assertThat(anotherClient.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(anotherClient.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(anotherClient.isServiceAccountsEnabled(), is(false));

        // ... and has to be retrieved separately
        String anotherClientSecret = getClientSecret(REALM_AUTH_FLOW_NAME, anotherClient.getId());
        assertThat(anotherClientSecret, is("my-special-client-secret"));

        // ... and finally assert that we really want
        assertThat(anotherClient.getAuthenticationFlowBindingOverrides(), equalTo(Collections.emptyMap()));
    }

    @Test
    @Order(90)
    void shouldNotUpdateRealmCreateClientWithError_And_ReamStateSurvivesAttributesUpdate() throws IOException {
        RealmImport foundImport = getFirstImport("90_update_realm__try-to-create-client.json");

        // Given the state of the realm already exists
        assertThat(getRealmState(foundImport.getRealm()), not(anEmptyMap()));
        // and found import contains custom attributes
        assertThat(foundImport.getAttributes().get("custom"), notNullValue());

        ImportProcessingException thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), matchesPattern(".*Cannot create client 'new-client' in realm 'realmWithClients': .*"));

        // Expect the experienced fatal exception has not led to the realm state erasure
        // when the realm configuration contains custom attributes
        assertThat(getRealmState(foundImport.getRealm()), not(anEmptyMap()));
    }

    @Test
    @Order(91)
    @DisabledIfSystemProperty(named = "keycloak.version", matches = "17.0.0", disabledReason = "https://github.com/keycloak/keycloak/issues/10176")
    void shouldNotUpdateRealmUpdateClientWithError() throws IOException {
        doImport("91.0_update_realm__try-to-update-client.json");
        RealmImport foundImport = getFirstImport("91.1_update_realm__try-to-update-client.json");

        ImportProcessingException thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), matchesPattern(".*Cannot update client 'another-client-with-long-description' in realm 'realmWithClients': .*"));
    }

    @Test
    @Order(92)
    void shouldNotUpdateRealmInvalidClient() throws IOException {
        RealmImport foundImport = getFirstImport("92_update_realm__invalid-client.json");

        ImportProcessingException thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), matchesPattern("clients require client id or name."));
    }

    @Test
    @Order(98)
    void shouldUpdateRealmDeleteClient() throws IOException {
        doImport("98_update_realm__delete_client.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(false, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientRepresentation client = keycloakRepository.getClient(
                REALM_NAME,
                "moped-client"
        );

        assertThat(client.getName(), is("moped-client"));
        assertThat(client.getClientId(), is("moped-client"));
        assertThat(client.getDescription(), is("Moped-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("https://moped-client.org/redirect")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("https://moped-client.org/webOrigin")));

        String clientSecret = getClientSecret(REALM_NAME, client.getId());
        assertThat(clientSecret, is("changed-special-client-secret"));

        assertThat(client.getProtocolMappers(), is(nullValue()));


        ClientRepresentation otherClient = getClientByClientId(realm, "another-client");

        assertThat(otherClient, nullValue());
    }

    /**
     * @param id (not client-id)
     */
    private String getClientSecret(String realm, String id) {
        CredentialRepresentation secret = keycloakProvider.getInstance()
                .realm(realm)
                .clients().get(id).getSecret();

        return secret.getValue();
    }

    private ClientRepresentation getClientByClientId(RealmRepresentation realm, String clientId) {
        return realm
                .getClients()
                .stream()
                .filter(s -> Objects.equals(s.getClientId(), clientId))
                .findFirst()
                .orElse(null);
    }

    private ClientRepresentation getClientByName(RealmRepresentation realm, String clientName) {
        return realm
                .getClients()
                .stream()
                .filter(s -> Objects.equals(s.getName(), clientName))
                .findFirst()
                .orElse(null);
    }

    private ResourceRepresentation getAuthorizationSettingsResource(List<ResourceRepresentation> authorizationSettings, String name) {
        return authorizationSettings
                .stream()
                .filter(s -> Objects.equals(s.getName(), name))
                .findFirst()
                .orElse(null);
    }

    private PolicyRepresentation getAuthorizationPolicy(List<PolicyRepresentation> authorizationSettings, String name) {
        return authorizationSettings
                .stream()
                .filter(s -> Objects.equals(s.getName(), name))
                .findFirst()
                .orElse(null);
    }

    private IdentityProviderRepresentation getIdentityProviderByAlias(RealmRepresentation realm, String alias) {
        return identityProviderRepository.getAll(realm.getRealm())
                .stream()
                .filter(s -> Objects.equals(s.getAlias(), alias))
                .findFirst()
                .orElse(null);
    }

    private RoleRepresentation getRealmRoleByName(RealmRepresentation realm, String name) {
        return realm.getRoles()
                .getRealm()
                .stream()
                .filter(s -> Objects.equals(s.getName(), name))
                .findFirst()
                .orElse(null);
    }

    private GroupRepresentation getGroupByPath(RealmRepresentation realm, String... path) {
        List<GroupRepresentation> groups = realm.getGroups();
        GroupRepresentation group = null;
        for (String p : path) {
            group = groups.stream()
                    .filter(g -> Objects.equals(g.getName(), p))
                    .findFirst()
                    .orElse(null);
            if (group == null) {
                break;
            }
            groups = SubGroupUtil.getSubGroups(group, keycloakProvider.getInstance().realm(realm.getRealm()));
        }

        return group;
    }

    private List<String> readJson(String jsonString) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readValue(jsonString, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }

    private AuthenticationFlowRepresentation getAuthenticationFlow(RealmRepresentation updatedRealm, String flowAlias) {
        List<AuthenticationFlowRepresentation> authenticationFlows = updatedRealm.getAuthenticationFlows();
        return authenticationFlows.stream()
                .filter(f -> f.getAlias().equals(flowAlias))
                .findFirst()
                .orElse(null);
    }

    private Map<String, String> getRealmState(String realmName) {
        return keycloakRepository.getRealmAttributes(realmName)
                .entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(ImportConfigProperties.REALM_STATE_ATTRIBUTE_COMMON_PREFIX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void createRemoteManagedClientResource(String realm, String clientId, String clientSecret, ResourceRepresentation resource) {
        Configuration configuration = new Configuration();
        configuration.setAuthServerUrl(properties.getUrl());
        configuration.setRealm(realm);
        configuration.setResource(clientId);
        configuration.setCredentials(Collections.singletonMap("secret", clientSecret));
        AuthzClient authzClient = AuthzClient.create(configuration);

        authzClient.protection().resource().create(resource);
    }

    private void checkClientAttributes(ClientRepresentation client) {
        if (VersionUtil.lt(KEYCLOAK_VERSION, "26")) {
            assertThat(client.getAttributes(), anEmptyMap());
        } else {
            // https://github.com/keycloak/keycloak/pull/30433 Added attribute to recognize realm client
            assertThat(client.getAttributes(), hasEntry("realm_client", "true"));
        }
    }
}
