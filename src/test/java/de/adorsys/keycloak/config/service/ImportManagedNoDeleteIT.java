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

import de.adorsys.keycloak.config.AbstractImportIT;
import de.adorsys.keycloak.config.repository.IdentityProviderMapperRepository;
import de.adorsys.keycloak.config.repository.IdentityProviderRepository;
import de.adorsys.keycloak.config.util.VersionUtil;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.*;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@TestPropertySource(properties = {
        "import.managed.authentication-flow=no-delete",
        "import.managed.group=no-delete",
        "import.managed.required-action=no-delete",
        "import.managed.client-scope=no-delete",
        "import.managed.scope-mapping=no-delete",
        "import.managed.client-scope-mapping=no-delete",
        "import.managed.component=no-delete",
        "import.managed.sub-component=no-delete",
        "import.managed.identity-provider=no-delete",
        "import.managed.identity-provider-mapper=no-delete",
        "import.managed.role=no-delete",
        "import.managed.client=no-delete",
        "import.managed.client-authorization-resources=no-delete",
        "import.managed.client-authorization-policies=no-delete",
        "import.managed.client-authorization-scopes=no-delete",
})
@SuppressWarnings({"java:S5961", "java:S5976"})
class ImportManagedNoDeleteIT extends AbstractImportIT {
    private static final String REALM_NAME = "realmWithNoDelete";

    ImportManagedNoDeleteIT() {
        this.resourcePath = "import-files/managed-no-delete";
    }

    @Autowired
    private IdentityProviderRepository identityProviderRepository;

    @Autowired
    private IdentityProviderMapperRepository identityProviderMapperRepository;

    @Test
    @Order(0)
    void shouldCreateSimpleRealm() throws IOException {
        doImport("0_create_realm.json");

        assertRealm();
    }

    @Test
    @Order(1)
    void shouldUpdateRealmNotDeleteOne() throws IOException {
        doImport("1_update-realm_not-delete-one.json");

        assertRealm();
    }

    @Test
    @Order(2)
    void shouldUpdateRealmNotDeleteAll() throws IOException {
        doImport("2_update-realm_not-delete-all.json");

        assertRealm();
    }

    private void assertRealm() {
        RealmRepresentation createdRealm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        List<GroupRepresentation> createdGroup = createdRealm.getGroups();
        assertThat(createdGroup, hasSize(2));

        List<RequiredActionProviderRepresentation> createdRequiredActions = createdRealm.getRequiredActions()
                .stream()
                .filter((action) -> action.getAlias().equals("CONFIGURE_TOTP") || action.getAlias().equals("TERMS_AND_CONDITIONS"))
                .toList();
        assertThat(createdRequiredActions, hasSize(2));

        List<ClientScopeRepresentation> createdClientScopes = createdRealm.getClientScopes()
                .stream()
                .filter((clientScope) -> clientScope.getName().equals("my_clientScope") || clientScope.getName().equals("my_other_clientScope"))
                .toList();
        assertThat(createdClientScopes, hasSize(2));

        List<ScopeMappingRepresentation> createdScopeMappings = createdRealm.getScopeMappings()
                .stream()
                .filter((scopeMapping) -> scopeMapping.getClientScope().equals("offline_access"))
                .toList();
        assertThat(createdScopeMappings, hasSize(1));

        List<ScopeMappingRepresentation> createdClientScopeMappings = createdRealm.getClientScopeMappings()
                .entrySet()
                .stream()
                .filter((clientScopeMappingEntry) -> clientScopeMappingEntry.getKey().equals("moped-client"))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .toList();
        assertThat(createdClientScopeMappings, hasSize(2));

        List<ComponentExportRepresentation> createdComponents = createdRealm.getComponents().get("org.keycloak.storage.UserStorageProvider")
                .stream()
                .filter(c -> c.getName().equals("my-realm-userstorage"))
                .toList();
        assertThat(createdComponents, hasSize(1));

        List<ComponentExportRepresentation> createdSubComponents = createdComponents.get(0)
                .getSubComponents().getList("org.keycloak.storage.ldap.mappers.LDAPStorageMapper");
        if (VersionUtil.lt(KEYCLOAK_VERSION, "26")) {
            assertThat(createdSubComponents, hasSize(10));
        } else {
            assertThat(createdSubComponents, hasSize(11));
        }

        List<String> authenticationFlowsList = Arrays.asList("my auth flow", "my registration", "my registration form");
        List<AuthenticationFlowRepresentation> createdAuthenticationFlows = createdRealm.getAuthenticationFlows()
                .stream()
                .filter((authenticationFlow) -> authenticationFlowsList.contains(authenticationFlow.getAlias()))
                .toList();
        assertThat(createdAuthenticationFlows, hasSize(3));

        List<String> identityProviderList = Arrays.asList("my-first-idp", "my-second-idp");
        List<IdentityProviderRepresentation> createdIdentityProviders = identityProviderRepository.getAll(createdRealm.getRealm())
                .stream()
                .filter((identityProvider) -> identityProviderList.contains(identityProvider.getAlias()))
                .toList();
        assertThat(createdIdentityProviders, hasSize(2));

        List<String> identityProviderMapperList = Arrays.asList("my-first-idp-mapper", "my-second-idp-mapper");
        List<IdentityProviderMapperRepresentation> createdIdentityProviderMappers = identityProviderMapperRepository.getAll(createdRealm.getRealm())
                .stream()
                .filter((identityProviderMapper) -> identityProviderMapperList.contains(identityProviderMapper.getName()))
                .toList();
        assertThat(createdIdentityProviderMappers, hasSize(2));


        List<String> clientResourcesList = Arrays.asList("Admin Resource", "Protected Resource");
        List<ResourceRepresentation> createdClientResourcesList = createdRealm
                .getClients()
                .stream().filter(client -> Objects.equals(client.getName(), "moped-client")).findAny()
                .orElseThrow(() -> new RuntimeException("Cannot find client 'moped-client'"))
                .getAuthorizationSettings().getResources()
                .stream().filter(resource -> clientResourcesList.contains(resource.getName()))
                .toList();
        assertThat(createdClientResourcesList, hasSize(2));

        int createdScopesCount = createdRealm
            .getClients()
            .stream().filter(client -> Objects.equals(client.getName(), "moped-client")).findAny()
            .orElseThrow(() -> new RuntimeException("Cannot find client 'moped-client'"))
            .getAuthorizationSettings().getScopes()
            .size();
        assertThat(createdScopesCount, is(4));

        long createdPoliciesCount = createdRealm
            .getClients()
            .stream().filter(client -> Objects.equals(client.getName(), "moped-client")).findAny()
            .orElseThrow(() -> new RuntimeException("Cannot find client 'moped-client'"))
            .getAuthorizationSettings().getPolicies().stream()
            .filter(policy -> !policy.getName().equals("Default Policy"))
            .filter(policy -> !policy.getName().equals("Default Permission"))
            .count();
        assertThat(createdPoliciesCount, is(1L));
    }
}
