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

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.AbstractImportTest;
import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.util.StreamUtil;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImportClientScopesIT extends AbstractImportTest {
    private static final String REALM_NAME = "realmWithClientScopes";

    ImportClientScopesIT() {
        this.resourcePath = "import-files/client-scopes";
    }

    @Test
    @Order(0)
    void shouldCreateRealmWithClientScope() {
        doImport("00_create_realm_with_clientScope.json");

        RealmRepresentation realm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientScopeRepresentation createdClientScope = getClientScope(
                "my_clientScope"
        );

        assertThat(createdClientScope.getName(), is("my_clientScope"));
        assertThat(createdClientScope.getDescription(), is("My clientScope"));
        assertThat(createdClientScope.getProtocol(), is("openid-connect"));
        assertThat(createdClientScope.getAttributes(), aMapWithSize(2));
        assertThat(createdClientScope.getAttributes().get("include.in.token.scope"), is("true"));
        assertThat(createdClientScope.getAttributes().get("display.on.consent.screen"), is("false"));
        assertThat(createdClientScope.getProtocolMappers(), is(nullValue()));
    }

    @Test
    @Order(1)
    void shouldAddClientScope() {
        doImport("01_update_realm__add_clientScope.json");

        RealmRepresentation realm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientScopeRepresentation createdClientScope = getClientScope(
                "my_other_clientScope"
        );

        assertThat(createdClientScope.getName(), is("my_other_clientScope"));
        assertThat(createdClientScope.getDescription(), is("My other clientScope"));
        assertThat(createdClientScope.getProtocol(), is("openid-connect"));
        assertThat(createdClientScope.getAttributes(), aMapWithSize(2));
        assertThat(createdClientScope.getAttributes().get("include.in.token.scope"), is("false"));
        assertThat(createdClientScope.getAttributes().get("display.on.consent.screen"), is("true"));
        assertThat(createdClientScope.getProtocolMappers(), is(nullValue()));
    }

    @Test
    @Order(2)
    void shouldChangeClientScope() {
        doImport("02_update_realm__change_clientScope.json");

        RealmRepresentation realm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientScopeRepresentation createdClientScope = getClientScope(
                "my_other_clientScope"
        );

        assertThat(createdClientScope.getName(), is("my_other_clientScope"));
        assertThat(createdClientScope.getDescription(), is("My changed other clientScope"));
        assertThat(createdClientScope.getProtocol(), is("openid-connect"));
        assertThat(createdClientScope.getAttributes(), aMapWithSize(2));
        assertThat(createdClientScope.getAttributes().get("include.in.token.scope"), is("false"));
        assertThat(createdClientScope.getAttributes().get("display.on.consent.screen"), is("false"));
        assertThat(createdClientScope.getProtocolMappers(), is(nullValue()));
    }

    @Test
    @Order(3)
    void shouldChangeClientScopeWithAddProtocolMapper() {
        doImport("03_update_realm__change_clientScope_add_protocolMapper.json");

        RealmRepresentation realm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientScopeRepresentation createdClientScope = getClientScope(
                "my_other_clientScope"
        );

        assertThat(createdClientScope.getName(), is("my_other_clientScope"));
        assertThat(createdClientScope.getDescription(), is("My changed other clientScope"));
        assertThat(createdClientScope.getProtocol(), is("openid-connect"));
        assertThat(createdClientScope.getAttributes(), aMapWithSize(2));
        assertThat(createdClientScope.getAttributes().get("include.in.token.scope"), is("false"));
        assertThat(createdClientScope.getAttributes().get("display.on.consent.screen"), is("false"));
        assertThat(createdClientScope.getProtocolMappers(), hasSize(1));

        ProtocolMapperRepresentation protocolMapper = createdClientScope.getProtocolMappers().stream().filter(m -> Objects.equals(m.getName(), "my_protocol_mapper")).findFirst().orElse(null);

        assertThat(protocolMapper, notNullValue());
        assertThat(protocolMapper.getProtocol(), is("openid-connect"));
        assertThat(protocolMapper.getProtocolMapper(), is("oidc-usermodel-attribute-mapper"));
        assertThat(protocolMapper.getConfig().get("user.attribute"), is("my-attribute"));
        assertThat(protocolMapper.getConfig().get("id.token.claim"), is("true"));
        assertThat(protocolMapper.getConfig().get("access.token.claim"), is("true"));
        assertThat(protocolMapper.getConfig().get("claim.name"), is("my-claim"));
        assertThat(protocolMapper.getConfig().get("userinfo.token.claim"), is("true"));
    }

    @Test
    @Order(4)
    void shouldChangeClientScopeWithChangeProtocolMapper() {
        doImport("04_update_realm__change_clientScope_change_protocolMapper.json");

        RealmRepresentation realm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientScopeRepresentation createdClientScope = getClientScope(
                "my_other_clientScope"
        );

        assertThat(createdClientScope.getName(), is("my_other_clientScope"));
        assertThat(createdClientScope.getDescription(), is("My changed other clientScope"));
        assertThat(createdClientScope.getProtocol(), is("openid-connect"));
        assertThat(createdClientScope.getAttributes(), aMapWithSize(2));
        assertThat(createdClientScope.getAttributes().get("include.in.token.scope"), is("false"));
        assertThat(createdClientScope.getAttributes().get("display.on.consent.screen"), is("false"));
        assertThat(createdClientScope.getProtocolMappers(), hasSize(1));

        ProtocolMapperRepresentation protocolMapper = createdClientScope.getProtocolMappers().stream().filter(m -> Objects.equals(m.getName(), "my_protocol_mapper")).findFirst().orElse(null);

        assertThat(protocolMapper, notNullValue());
        assertThat(protocolMapper.getProtocol(), is("openid-connect"));
        assertThat(protocolMapper.getProtocolMapper(), is("oidc-usermodel-attribute-mapper"));
        assertThat(protocolMapper.getConfig().get("user.attribute"), is("my-changed-attribute"));
        assertThat(protocolMapper.getConfig().get("id.token.claim"), is("true"));
        assertThat(protocolMapper.getConfig().get("access.token.claim"), is("true"));
        assertThat(protocolMapper.getConfig().get("claim.name"), is("my-changed-claim"));
        assertThat(protocolMapper.getConfig().get("userinfo.token.claim"), is("true"));
    }

    @Test
    @Order(5)
    void shouldChangeClientScopeWithReplaceProtocolMapper() {
        doImport("05_update_realm__change_clientScope_replace_protocolMapper.json");

        RealmRepresentation realm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientScopeRepresentation createdClientScope = getClientScope(
                "my_other_clientScope"
        );

        assertThat(createdClientScope.getName(), is("my_other_clientScope"));
        assertThat(createdClientScope.getDescription(), is("My changed other clientScope"));
        assertThat(createdClientScope.getProtocol(), is("openid-connect"));
        assertThat(createdClientScope.getAttributes(), aMapWithSize(2));
        assertThat(createdClientScope.getAttributes().get("include.in.token.scope"), is("false"));
        assertThat(createdClientScope.getAttributes().get("display.on.consent.screen"), is("false"));
        assertThat(createdClientScope.getProtocolMappers(), hasSize(1));

        ProtocolMapperRepresentation protocolMapper = createdClientScope.getProtocolMappers().stream().filter(m -> Objects.equals(m.getName(), "my_replaced_protocol_mapper")).findFirst().orElse(null);

        assertThat(protocolMapper, notNullValue());
        assertThat(protocolMapper.getProtocol(), is("openid-connect"));
        assertThat(protocolMapper.getProtocolMapper(), is("oidc-usermodel-attribute-mapper"));
        assertThat(protocolMapper.getConfig().get("user.attribute"), is("my-changed-attribute"));
        assertThat(protocolMapper.getConfig().get("id.token.claim"), is("true"));
        assertThat(protocolMapper.getConfig().get("access.token.claim"), is("true"));
        assertThat(protocolMapper.getConfig().get("claim.name"), is("my-changed-claim"));
        assertThat(protocolMapper.getConfig().get("userinfo.token.claim"), is("true"));
    }

    @Test
    @Order(6)
    void shouldNotUpdateRealmUpdateScopeMappingsWithError() {
        RealmImport foundImport = getImport("06_update_realm__try-to-change_clientScope_invalid_protocolMapper.json");

        ImportProcessingException thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), matchesPattern("Cannot update protocolMapper 'my_replaced_protocol_mapper' for clientScope 'my_other_clientScope' for realm 'realmWithClientScopes': .*"));
    }

    @Test
    @Order(96)
    void shouldChangeClientScopeDeleteProtocolMapper() {
        doImport("96_update_realm__change_clientScope_delete_protocolMapper.json");

        RealmRepresentation realm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientScopeRepresentation clientScope = getClientScope(
                "my_other_clientScope"
        );

        assertThat(clientScope.getName(), is("my_other_clientScope"));
        assertThat(clientScope.getDescription(), is("My changed other clientScope"));
        assertThat(clientScope.getProtocol(), is("openid-connect"));
        assertThat(clientScope.getAttributes(), aMapWithSize(2));
        assertThat(clientScope.getAttributes().get("include.in.token.scope"), is("false"));
        assertThat(clientScope.getAttributes().get("display.on.consent.screen"), is("false"));
        assertThat(clientScope.getProtocolMappers(), nullValue());
    }

    @Test
    @Order(97)
    void shouldDeleteClientScope() {
        doImport("97_update_realm__delete_clientScope.json");

        RealmRepresentation realm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientScopeRepresentation deletedClientScope = getClientScope(
                "my_other_clientScope"
        );

        assertThat(deletedClientScope, is(nullValue()));
    }

    @Test
    @Order(98)
    void shouldDeleteNothingWithNonExistingClientScopes() {
        doImport("98_update_realm__skip_delete.json");

        RealmRepresentation realm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        ClientScopeRepresentation clientScope = getClientScope(
                "my_clientScope"
        );
        ClientScopeRepresentation otherClientScope = getClientScope(
                "my_other_clientScope"
        );

        assertThat(clientScope, notNullValue());
        assertThat(otherClientScope, is(nullValue()));
    }

    @Test
    @Order(99)
    void shouldDeleteEverythingExpectDefaultScopesWithEmptyClientScopes() {
        doImport("99_update_realm__delete_all.json");

        RealmResource realmResource = keycloakProvider.get().realm(REALM_NAME);
        RealmRepresentation realm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        final List<ClientScopeRepresentation> defaultDefaultClientScopes = realmResource.getDefaultDefaultClientScopes();
        final List<ClientScopeRepresentation> defaultOptionalClientScopes = realmResource.getDefaultOptionalClientScopes();
        final List<ClientScopeRepresentation> defaultClientScopes = Stream.concat(StreamUtil.collectionAsStream(defaultDefaultClientScopes), StreamUtil.collectionAsStream(defaultOptionalClientScopes)).collect(Collectors.toList());

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        List<ClientScopeRepresentation> clientScopes = getClientScopes();

        assertThat(clientScopes.stream().allMatch(s -> defaultClientScopes.stream().anyMatch(d -> Objects.equals(s.getName(), d.getName()))), is(true));
    }

    private ClientScopeRepresentation getClientScope(String clientScopeName) {
        return keycloakProvider.get()
                .realm(REALM_NAME)
                .partialExport(true, true)
                .getClientScopes()
                .stream()
                .filter(s -> Objects.equals(s.getName(), clientScopeName))
                .findFirst()
                .orElse(null);
    }

    private List<ClientScopeRepresentation> getClientScopes() {
        return keycloakProvider.get()
                .realm(REALM_NAME)
                .partialExport(true, true)
                .getClientScopes();
    }

}
