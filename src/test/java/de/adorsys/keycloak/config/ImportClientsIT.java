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

package de.adorsys.keycloak.config;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.model.RealmImport;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImportClientsIT extends AbstractImportTest {
    private static final String REALM_NAME = "realmWithClients";

    ImportClientsIT() {
        this.resourcePath = "import-files/clients";
    }

    @Test
    @Order(0)
    void shouldCreateRealmWithClient() {
        doImport("00_create_realm_with_client.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        ClientRepresentation createdClient = getClientByClientId("moped-client");

        assertThat(createdClient.getName(), is("moped-client"));
        assertThat(createdClient.getClientId(), is("moped-client"));
        assertThat(createdClient.getDescription(), is("Moped-Client"));
        assertThat(createdClient.isEnabled(), is(true));
        assertThat(createdClient.getClientAuthenticatorType(), is("client-secret"));
        assertThat(createdClient.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(createdClient.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(createdClient.getProtocolMappers(), is(nullValue()));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(createdClient.getId());
        assertThat(clientSecret, is("my-special-client-secret"));
    }

    @Test
    @Order(1)
    void shouldUpdateRealmByAddingClient() {
        doImport("01_update_realm__add_client.json");

        RealmRepresentation realm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientRepresentation client = getClientByClientId("another-client");

        assertThat(client.getClientId(), is("another-client"));
        assertThat(client.getDescription(), is("Another-Client"));
        assertThat(client.isEnabled(), is(true));
        assertThat(client.getClientAuthenticatorType(), is("client-secret"));
        assertThat(client.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(client.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(client.getProtocolMappers(), is(nullValue()));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(client.getId());
        assertThat(clientSecret, is("my-other-client-secret"));

        ClientRepresentation otherClient = getClientByName("another-other-client");

        assertThat(otherClient.getName(), is("another-other-client"));
        assertThat(otherClient.getDescription(), is("Another-Other-Client"));
        assertThat(otherClient.isEnabled(), is(true));
        assertThat(otherClient.getClientAuthenticatorType(), is("client-secret"));
        assertThat(otherClient.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(otherClient.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(otherClient.getProtocolMappers(), is(nullValue()));

        // ... and has to be retrieved separately
        String otherClientSecret = getClientSecret(otherClient.getId());
        assertThat(otherClientSecret, is("my-another-other-client-secret"));
    }

    @Test
    @Order(2)
    void shouldUpdateRealmWithChangedClientProperties() {
        doImport("02_update_realm__change_clients_properties.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        ClientRepresentation createdClient = getClientByClientId("moped-client");

        assertThat(createdClient.getName(), is("moped-client"));
        assertThat(createdClient.getClientId(), is("moped-client"));
        assertThat(createdClient.getDescription(), is("Moped-Client"));
        assertThat(createdClient.isEnabled(), is(true));
        assertThat(createdClient.getClientAuthenticatorType(), is("client-secret"));
        assertThat(createdClient.getRedirectUris(), is(containsInAnyOrder("https://moped-client.org/redirect")));
        assertThat(createdClient.getWebOrigins(), is(containsInAnyOrder("https://moped-client.org/webOrigin")));
        assertThat(createdClient.getProtocolMappers(), is(nullValue()));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(createdClient.getId());
        assertThat(clientSecret, is("changed-special-client-secret"));
    }

    @Test
    @Order(3)
    void shouldUpdateRealmAddProtocolMapper() {
        doImport("03_update_realm__add_protocol-mapper.json");

        RealmRepresentation realm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientRepresentation updatedClient = getClientByClientId("moped-client");

        assertThat(updatedClient.getName(), is("moped-client"));
        assertThat(updatedClient.getClientId(), is("moped-client"));
        assertThat(updatedClient.getDescription(), is("Moped-Client"));
        assertThat(updatedClient.isEnabled(), is(true));
        assertThat(updatedClient.getClientAuthenticatorType(), is("client-secret"));
        assertThat(updatedClient.getRedirectUris(), is(containsInAnyOrder("https://moped-client.org/redirect")));
        assertThat(updatedClient.getWebOrigins(), is(containsInAnyOrder("https://moped-client.org/webOrigin")));
        assertThat(updatedClient.getProtocolMappers(), notNullValue());

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(updatedClient.getId());
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

        // client secret on this place is always null...
        assertThat(createdClient.getSecret(), is(nullValue()));

        // ... and has to be retrieved separately
        String clientSecret2 = getClientSecret(createdClient.getId());
        assertThat(clientSecret2, is("changed-special-client-secret"));

        ProtocolMapperRepresentation createdClientProtocolMappers = createdClient.getProtocolMappers().stream().filter(m -> Objects.equals(m.getName(), "BranchCodeMapper")).findFirst().orElse(null);

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
    void shouldUpdateRealmAddMoreProtocolMapper() {
        doImport("04_update_realm__add_more_protocol-mapper.json");

        RealmRepresentation realm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

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
        String clientSecret = getClientSecret(client.getId());
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
    void shouldUpdateRealmChangeProtocolMapper() {
        doImport("05_update_realm__change_protocol-mapper.json");

        RealmRepresentation realm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

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
        String clientSecret = getClientSecret(clien.getId());
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
    void shouldUpdateRealmIgnoreProtocolMapper() {
        doImport("06_update_realm__ignore_protocol-mapper.json");

        RealmRepresentation realm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

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

        String clientSecret = getClientSecret(client.getId());
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
    void shouldNotUpdateRealmUpdateClientWithError() {
        RealmImport foundImport = getImport("07_update_realm__try-to-update-client.json");

        ImportProcessingException thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), matchesPattern(".*Cannot update client 'another-client' for realm 'realmWithClients': .*"));
    }

    @Test
    @Order(8)
    void shouldNotUpdateRealmUpdateScopeMappingsWithError() {
        RealmImport foundImport = getImport("08_update_realm__try-to-update_protocol-mapper.json");

        ImportProcessingException thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), matchesPattern(".*Cannot update protocolMapper 'BranchCodeMapper' for client '.*' for realm 'realmWithClients': .*"));
    }

    @Test
    @Order(9)
    void shouldNotUpdateRealmCreateClientWithError() {
        RealmImport foundImport = getImport("09_update_realm__try-to-create-client.json");

        ImportProcessingException thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), matchesPattern(".*Cannot create client 'new-client' for realm 'realmWithClients': .*"));
    }

    @Test
    @Order(96)
    void shouldUpdateRealmDeleteProtocolMapper() {
        doImport("96_update_realm__delete_protocol-mapper.json");

        RealmRepresentation realm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

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

        String clientSecret = getClientSecret(client.getId());
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
    @Order(97)
    void shouldUpdateRealmDeleteAllProtocolMapper() {
        doImport("97_update_realm__delete_all_protocol-mapper.json");

        RealmRepresentation realm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

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

        String clientSecret = getClientSecret(client.getId());
        assertThat(clientSecret, is("changed-special-client-secret"));

        assertThat(client.getProtocolMappers(), is(nullValue()));


        ClientRepresentation otherClient = getClientByClientId("another-client");

        assertThat(otherClient.getClientId(), is("another-client"));
        assertThat(otherClient.getDescription(), is("Another-Client"));
        assertThat(otherClient.isEnabled(), is(true));
        assertThat(otherClient.getClientAuthenticatorType(), is("client-secret"));
        assertThat(otherClient.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(otherClient.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(otherClient.getProtocolMappers(), is(nullValue()));

        // ... and has to be retrieved separately
        String otherClientSecret = getClientSecret(otherClient.getId());
        assertThat(otherClientSecret, is("my-other-client-secret"));
    }

    @Test
    @Order(98)
    void shouldUpdateRealmDeleteClient() {
        doImport("98_update_realm__not_delete_client.json");

        RealmRepresentation realm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

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

        String clientSecret = getClientSecret(client.getId());
        assertThat(clientSecret, is("changed-special-client-secret"));

        assertThat(client.getProtocolMappers(), is(nullValue()));


        ClientRepresentation otherClient = getClientByClientId("another-client");

        assertThat(otherClient.getClientId(), is("another-client"));
        assertThat(otherClient.getDescription(), is("Another-Client"));
        assertThat(otherClient.isEnabled(), is(true));
        assertThat(otherClient.getClientAuthenticatorType(), is("client-secret"));
        assertThat(otherClient.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(otherClient.getWebOrigins(), is(containsInAnyOrder("*")));
        assertThat(otherClient.getProtocolMappers(), is(nullValue()));

        // ... and has to be retrieved separately
        String otherClientSecret = getClientSecret(otherClient.getId());
        assertThat(otherClientSecret, is("my-other-client-secret"));
    }

    /**
     * @param id (not client-id)
     */
    private String getClientSecret(String id) {
        CredentialRepresentation secret = keycloakProvider.get()
                .realm(REALM_NAME)
                .clients().get(id).getSecret();

        return secret.getValue();
    }

    private ClientRepresentation getClientByClientId(String clientId) {
        return keycloakProvider.get()
                .realm(REALM_NAME)
                .partialExport(true, true)
                .getClients()
                .stream()
                .filter(s -> Objects.equals(s.getClientId(), clientId))
                .findFirst()
                .orElse(null);
    }

    private ClientRepresentation getClientByName(String clientName) {
        return keycloakProvider.get()
                .realm(REALM_NAME)
                .partialExport(true, true)
                .getClients()
                .stream()
                .filter(s -> Objects.equals(s.getName(), clientName))
                .findFirst()
                .orElse(null);
    }
}
