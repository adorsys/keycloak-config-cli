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
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.model.RealmImport;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"java:S5961", "java:S5976"})
class ImportScopeMappingsIT extends AbstractImportIT {
    private static final String REALM_NAME = "realmWithScopeMappings";

    ImportScopeMappingsIT() {
        this.resourcePath = "import-files/scope-mappings";
    }

    @Test
    @Order(0)
    void shouldCreateRealmWithScopeMappings() throws IOException {
        doImport("00_create-realm-with-scope-mappings.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        List<ScopeMappingRepresentation> scopeMappings = realm.getScopeMappings();
        assertThat(scopeMappings, hasSize(1));

        ScopeMappingRepresentation scopeMapping = scopeMappings.get(0);
        assertThat(scopeMapping.getClient(), is(nullValue()));
        assertThat(scopeMapping.getClientScope(), is(equalTo("offline_access")));
        assertThat(scopeMapping.getRoles(), hasSize(1));
        assertThat(scopeMapping.getRoles(), contains("offline_access"));
    }

    @Test
    @Order(1)
    void shouldUpdateRealmByAddingScopeMapping() throws IOException {
        doImport("01_update-realm__add-scope-mapping.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        List<ScopeMappingRepresentation> scopeMappings = realm.getScopeMappings();
        assertThat(scopeMappings, hasSize(2));

        ScopeMappingRepresentation scopeMapping = findScopeMappingForClient(realm, "scope-mapping-client");
        assertThat(scopeMapping.getClient(), is(equalTo("scope-mapping-client")));

        Set<String> scopeMappingRoles = scopeMapping.getRoles();
        assertThat(scopeMappingRoles, hasSize(1));
        assertThat(scopeMappingRoles, contains("scope-mapping-role"));
    }

    @Test
    @Order(2)
    void shouldUpdateRealmByAddingRoleToScopeMapping() throws IOException {
        doImport("02_update-realm__add-role-to-scope-mapping.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        List<ScopeMappingRepresentation> scopeMappings = realm.getScopeMappings();
        assertThat(scopeMappings, hasSize(2));

        ScopeMappingRepresentation scopeMapping = findScopeMappingForClient(realm, "scope-mapping-client");
        assertThat(scopeMapping.getClient(), is(equalTo("scope-mapping-client")));

        Set<String> scopeMappingRoles = scopeMapping.getRoles();

        assertThat(scopeMappingRoles, hasSize(2));
        assertThat(scopeMappingRoles, contains("scope-mapping-role", "added-scope-mapping-role"));
    }

    @Test
    @Order(3)
    void shouldUpdateRealmByAddingAnotherScopeMapping() throws IOException {
        doImport("03_update-realm__add-scope-mapping.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        List<ScopeMappingRepresentation> scopeMappings = realm.getScopeMappings();
        assertThat(scopeMappings, hasSize(3));

        // check scope-mapping for client 'scope-mapping-client'
        ScopeMappingRepresentation scopeMapping = findScopeMappingForClient(realm, "scope-mapping-client");
        assertThat(scopeMapping.getClient(), is(equalTo("scope-mapping-client")));

        Set<String> scopeMappingRoles = scopeMapping.getRoles();

        assertThat(scopeMappingRoles, hasSize(2));
        assertThat(scopeMappingRoles, contains("scope-mapping-role", "added-scope-mapping-role"));

        // check scope-mapping for client 'scope-mapping-client-two'
        scopeMapping = findScopeMappingForClient(realm, "scope-mapping-client-two");
        assertThat(scopeMapping.getClient(), is(equalTo("scope-mapping-client-two")));

        scopeMappingRoles = scopeMapping.getRoles();

        assertThat(scopeMappingRoles, hasSize(2));
        assertThat(scopeMappingRoles, contains("scope-mapping-role", "added-scope-mapping-role"));
    }

    @Test
    @Order(4)
    void shouldUpdateRealmByScopeMappingAdditions() throws IOException {
        doImport("04_update-realm__delete-role-from-scope-mapping.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        List<ScopeMappingRepresentation> scopeMappings = realm.getScopeMappings();
        assertThat(scopeMappings, hasSize(3));

        // Check for additions in scope-mapping-client
        ScopeMappingRepresentation scopeMapping = findScopeMappingForClient(realm, "scope-mapping-client");
        assertThat(scopeMapping.getClient(), is(equalTo("scope-mapping-client")));

        Set<String> scopeMappingRoles = scopeMapping.getRoles();
        assertThat(scopeMappingRoles, hasItem("added-scope-mapping-role"));

        // Check for additions in scope-mapping-client-two
        scopeMapping = findScopeMappingForClient(realm, "scope-mapping-client-two");
        assertThat(scopeMapping.getClient(), is(equalTo("scope-mapping-client-two")));

        scopeMappingRoles = scopeMapping.getRoles();
        assertThat(scopeMappingRoles, hasItem("added-scope-mapping-role"));
    }

    @Test
    @Order(5)
    void shouldUpdateRealmByDeletingScopeMappingForClient() throws IOException {
        doImport("05_update-realm__delete-scope-mapping-for-client.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        List<ScopeMappingRepresentation> scopeMappings = realm.getScopeMappings();
        assertThat(scopeMappings, hasSize(3));

        Optional<ScopeMappingRepresentation> maybeExistingScopeMapping = tryToFindScopeMappingForClient(realm, "scope-mapping-client");
        assertThat(maybeExistingScopeMapping.isPresent(), is(true));
    }

    @Test
    @Order(6)
    void shouldUpdateRealmByNotChangingScopeMappingsIfOmittedInImport() throws IOException {
        doImport("06_update-realm__do-not-change-scope-mappings.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        List<ScopeMappingRepresentation> scopeMappings = realm.getScopeMappings();

        ScopeMappingRepresentation scopeMapping = findScopeMappingForClient(realm, "scope-mapping-client-two");
        assertThat(scopeMapping, notNullValue());
        assertThat(scopeMapping.getClient(), is(equalTo("scope-mapping-client-two")));

        Set<String> scopeMappingRoles = scopeMapping.getRoles();

        // Check that the expected role is present
        assertThat(scopeMappingRoles, hasItem("added-scope-mapping-role"));

        assertThat(scopeMappingRoles, not(hasItem("unexpected-role")));
    }

    @Test
    @Order(7)
    void shouldUpdateRealmByDeletingAllExistingScopeMappings() throws IOException {
        doImport("07_update-realm__delete-all-scope-mappings.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        List<ScopeMappingRepresentation> scopeMappings = realm.getScopeMappings();
        assertThat(scopeMappings, hasSize(3));
    }

    @Test
    @Order(8)
    void shouldUpdateRealmByAddingScopeMappingsForClientScope() throws IOException {
        doImport("08_update-realm__add-scope-mappings-for-client-scope.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        List<ScopeMappingRepresentation> scopeMappings = realm.getScopeMappings();

        Optional<ScopeMappingRepresentation> offlineAccessMapping = scopeMappings.stream()
                .filter(mapping -> "offline_access".equals(mapping.getClientScope()))
                .findFirst();

        assertThat(offlineAccessMapping.isPresent(), is(true));
        if (offlineAccessMapping.isPresent()) {
            assertThat(offlineAccessMapping.get().getRoles(), hasItems("scope-mapping-role", "added-scope-mapping-role"));
        }

        Optional<ScopeMappingRepresentation> clientMapping = scopeMappings.stream()
                .filter(mapping -> "scope-mapping-client".equals(mapping.getClient()))
                .findFirst();

        assertThat(clientMapping.isPresent(), is(true));
        if (clientMapping.isPresent()) {
            assertThat(clientMapping.get().getRoles(), hasItem("user"));
        }
    }

    @Test
    @Order(9)
    void shouldUpdateRealmByAddingRolesForClient() throws IOException {
        doImport("09_update-realm__update-role-for-client.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        List<ScopeMappingRepresentation> scopeMappings = realm.getScopeMappings();

        Optional<ScopeMappingRepresentation> offlineAccessMapping = scopeMappings.stream()
                .filter(mapping -> "offline_access".equals(mapping.getClientScope()))
                .findFirst();

        assertThat(offlineAccessMapping.isPresent(), is(true));
        if (offlineAccessMapping.isPresent()) {
            assertThat(offlineAccessMapping.get().getRoles(), hasItems("offline_access", "added-scope-mapping-role"));
        }

        Optional<ScopeMappingRepresentation> clientMapping = scopeMappings.stream()
                .filter(mapping -> "scope-mapping-client".equals(mapping.getClient()))
                .findFirst();

        assertThat(clientMapping.isPresent(), is(true));
        if (clientMapping.isPresent()) {
            assertThat(clientMapping.get().getRoles(), hasItem("admin"));
        }
    }

    @Test
    @Order(10)
    void shouldThrowOnUpdateRealmNonExistClientScope() throws IOException {
        RealmImport foundImport = getFirstImport("10_1_update-realm__throw-invalid-client-scope.json");

        KeycloakRepositoryException thrown = assertThrows(KeycloakRepositoryException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), is("Cannot find client-scope by name 'non-exists-client-scope'"));
    }

    @Test
    @Order(11)
    void shouldThrowOnUpdateRealmClientScopeWithNonExistRoles() throws IOException {
        RealmImport foundImport = getFirstImport("10_2_update-realm__throw-invalid-client-scope-role.json");

        KeycloakRepositoryException thrown = assertThrows(KeycloakRepositoryException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), is("Cannot find realm role 'non-exists-role' within realm 'realmWithScopeMappings'"));
    }

    @Test
    @Order(12)
    void shouldThrowOnUpdateRealmNonExistClient() throws IOException {
        RealmImport foundImport = getFirstImport("10_3_update-realm__throw-invalid-client.json");

        KeycloakRepositoryException thrown = assertThrows(KeycloakRepositoryException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), is("Cannot find client by clientId 'non-exists-client'"));
    }

    @Test
    @Order(13)
    void shouldThrowOnUpdateRealmClientWithNonExistRoles() throws IOException {
        RealmImport foundImport = getFirstImport("10_4_update-realm__throw-invalid-client-role.json");

        KeycloakRepositoryException thrown = assertThrows(KeycloakRepositoryException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), is("Cannot find realm role 'non-exists-role' within realm 'realmWithScopeMappings'"));
    }

    @Test
    @Order(70)
    void shouldCreateRealmWithScopeMappingsAndClient() throws IOException {
        final String REALM_NAME = "realmWithScopeMappingsClient";

        doImport("70_create-realm-with-scope-mappings-and-client.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        List<ScopeMappingRepresentation> scopeMappings = realm.getScopeMappings();
        assertThat(scopeMappings, hasSize(2));

        ScopeMappingRepresentation scopeMapping = scopeMappings.get(0);
        assertThat(scopeMapping.getClient(), is("scope-mapping-client"));
        assertThat(scopeMapping.getRoles(), contains("scope-mapping-role"));
    }

    private ScopeMappingRepresentation findScopeMappingForClient(RealmRepresentation realm, String client) {
        return tryToFindScopeMappingForClient(realm, client)
                .orElseThrow(() -> new RuntimeException("Cannot find scope-mapping for client" + client));
    }

    private Optional<ScopeMappingRepresentation> tryToFindScopeMappingForClient(RealmRepresentation realm, String client) {
        return realm.getScopeMappings()
                .stream()
                .filter(scopeMapping -> Objects.equals(scopeMapping.getClient(), client))
                .findFirst();
    }
}
