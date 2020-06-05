/*
 * Copyright 2019-2020 adorsys GmbH & Co. KG @ https://adorsys.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package de.adorsys.keycloak.config;

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
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.oneOf;
import static org.hamcrest.core.Is.is;

public class ImportClientScopesIT extends AbstractImportTest {
    private static final String REALM_NAME = "realmWithClientScopes";

    ImportClientScopesIT() {
        this.resourcePath = "import-files/client-scopes";
    }

    @Test
    @Order(0)
    public void shouldCreateRealmWithClientScope() {
        doImport("0_create_realm_with_clientScope.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        ClientScopeRepresentation createdClientScope = getClientScope(
            "my_clientScope"
        );

        assertThat(createdClientScope.getName(), is("my_clientScope"));
        assertThat(createdClientScope.getDescription(), is("My clientScope"));
        assertThat(createdClientScope.getProtocol(), is("openid-connect"));
        assertThat(createdClientScope.getAttributes().get("include.in.token.scope"), is ("true"));
        assertThat(createdClientScope.getAttributes().get("display.on.consent.screen"), is ("false"));
        assertThat(createdClientScope.getProtocolMappers(), is(nullValue()));
    }

    @Test
    @Order(1)
    public void shouldAddClientScope() {
        doImport("1_update_realm__add_clientScope.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        ClientScopeRepresentation createdClientScope = getClientScope(
                "my_other_clientScope"
        );

        assertThat(createdClientScope.getName(), is("my_other_clientScope"));
        assertThat(createdClientScope.getDescription(), is("My other clientScope"));
        assertThat(createdClientScope.getProtocol(), is("openid-connect"));
        assertThat(createdClientScope.getAttributes().get("include.in.token.scope"), is ("false"));
        assertThat(createdClientScope.getAttributes().get("display.on.consent.screen"), is ("true"));
        assertThat(createdClientScope.getProtocolMappers(), is(nullValue()));
    }

    @Test
    @Order(2)
    public void shouldChangeClientScope() {
        doImport("2_update_realm__change_clientScope.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        ClientScopeRepresentation createdClientScope = getClientScope(
                "my_other_clientScope"
        );

        assertThat(createdClientScope.getName(), is("my_other_clientScope"));
        assertThat(createdClientScope.getDescription(), is("My changed other clientScope"));
        assertThat(createdClientScope.getProtocol(), is("openid-connect"));
        assertThat(createdClientScope.getAttributes().get("include.in.token.scope"), is ("false"));
        assertThat(createdClientScope.getAttributes().get("display.on.consent.screen"), is ("false"));
        assertThat(createdClientScope.getProtocolMappers(), is(nullValue()));
    }

    @Test
    @Order(3)
    public void shouldChangeClientScopeWithAddProtocolMapper() {
        doImport("3_update_realm__change_clientScope_add_protocolMapper.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        ClientScopeRepresentation createdClientScope = getClientScope(
            "my_other_clientScope"
        );

        assertThat(createdClientScope.getName(), is("my_other_clientScope"));
        assertThat(createdClientScope.getDescription(), is("My changed other clientScope"));
        assertThat(createdClientScope.getProtocol(), is("openid-connect"));
        assertThat(createdClientScope.getAttributes().get("include.in.token.scope"), is ("false"));
        assertThat(createdClientScope.getAttributes().get("display.on.consent.screen"), is ("false"));
        assertThat(createdClientScope.getAttributes().get("display.on.consent.screen"), is ("false"));
        assertThat(createdClientScope.getProtocolMappers(), hasSize(1));

        ProtocolMapperRepresentation protocolMapper = createdClientScope.getProtocolMappers().stream().filter(m -> Objects.equals(m.getName(), "my_protocol_mapper")).findFirst().orElse(null);

        assertThat(protocolMapper, not(nullValue()));
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
    public void shouldChangeClientScopeWithChangeProtocolMapper() {
        doImport("4_update_realm__change_clientScope_change_protocolMapper.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        ClientScopeRepresentation createdClientScope = getClientScope(
            "my_other_clientScope"
        );

        assertThat(createdClientScope.getName(), is("my_other_clientScope"));
        assertThat(createdClientScope.getDescription(), is("My changed other clientScope"));
        assertThat(createdClientScope.getProtocol(), is("openid-connect"));
        assertThat(createdClientScope.getAttributes().get("include.in.token.scope"), is ("false"));
        assertThat(createdClientScope.getAttributes().get("display.on.consent.screen"), is ("false"));
        assertThat(createdClientScope.getAttributes().get("display.on.consent.screen"), is ("false"));
        assertThat(createdClientScope.getProtocolMappers(), hasSize(1));

        ProtocolMapperRepresentation protocolMapper = createdClientScope.getProtocolMappers().stream().filter(m -> Objects.equals(m.getName(), "my_protocol_mapper")).findFirst().orElse(null);

        assertThat(protocolMapper, not(nullValue()));
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
    public void shouldChangeClientScopeWithReplaceProtocolMapper() {
        doImport("5_update_realm__change_clientScope_replace_protocolMapper.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        ClientScopeRepresentation createdClientScope = getClientScope(
            "my_other_clientScope"
        );

        assertThat(createdClientScope.getName(), is("my_other_clientScope"));
        assertThat(createdClientScope.getDescription(), is("My changed other clientScope"));
        assertThat(createdClientScope.getProtocol(), is("openid-connect"));
        assertThat(createdClientScope.getAttributes().get("include.in.token.scope"), is ("false"));
        assertThat(createdClientScope.getAttributes().get("display.on.consent.screen"), is ("false"));
        assertThat(createdClientScope.getAttributes().get("display.on.consent.screen"), is ("false"));
        assertThat(createdClientScope.getProtocolMappers(), hasSize(1));

        ProtocolMapperRepresentation protocolMapper = createdClientScope.getProtocolMappers().stream().filter(m -> Objects.equals(m.getName(), "my_replaced_protocol_mapper")).findFirst().orElse(null);

        assertThat(protocolMapper, not(nullValue()));
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
    public void shouldDeleteClientScope() {
        doImport("6_update_realm__delete_clientScope.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        ClientScopeRepresentation deletedClientScope = getClientScope(
            "my_other_clientScope"
        );

        assertThat(deletedClientScope, is(nullValue()));
    }

    @Test
    @Order(7)
    public void shouldDeleteNothingWithNonExistingClientScopes() {
        doImport("7_update_realm__delete_none.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        ClientScopeRepresentation clientScope = getClientScope(
            "my_clientScope"
        );
        ClientScopeRepresentation otherClientScope = getClientScope(
            "my_other_clientScope"
        );

        assertThat(clientScope, not(nullValue()));
        assertThat(otherClientScope, is(nullValue()));
    }

    @Test
    @Order(8)
    public void shouldDeleteEverythingExpectDefaultScopesWithEmptyClientScopes() {
        doImport("8_update_realm__delete_all.json");

        RealmResource createdRealmResource = keycloakProvider.get().realm(REALM_NAME);
        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        final List<ClientScopeRepresentation> defaultDefaultClientScopes = createdRealmResource.getDefaultDefaultClientScopes();
        final List<ClientScopeRepresentation> defaultOptionalClientScopes = createdRealmResource.getDefaultOptionalClientScopes();
        final List<ClientScopeRepresentation> defaultClientScopes = Stream.concat(StreamUtil.collectionAsStream(defaultDefaultClientScopes), StreamUtil.collectionAsStream(defaultOptionalClientScopes)).collect(Collectors.toList());

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

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
