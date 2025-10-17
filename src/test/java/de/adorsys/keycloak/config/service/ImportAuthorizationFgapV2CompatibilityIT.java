/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2025 adorsys GmbH & Co. KG @ https://adorsys.com
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
import de.adorsys.keycloak.config.util.VersionUtil;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Integration test for FGAP V1/V2 compatibility handling.
 *
 * This test verifies that when Keycloak 26.2+ has FGAP V2 enabled (which returns HTTP 501 for V1 APIs),
 * the keycloak-config-cli gracefully handles these errors and continues processing instead of crashing.
 */
@SuppressWarnings({"java:S5961", "java:S5976"})
@EnabledIf(value = "isKeycloak262orHigher", disabledReason = "FGAP V2 only available in Keycloak 26.2+")
class ImportAuthorizationFgapV2CompatibilityIT extends AbstractImportIT {
    private static final String REALM_NAME = "fgap-v2-compatibility-test";

    ImportAuthorizationFgapV2CompatibilityIT() {
        this.resourcePath = "import-files/fgap-v2-compatibility";
    }

    /**
     * Check if current Keycloak version is 26.2 or higher where FGAP V2 is available.
     */
    static boolean isKeycloak262orHigher() {
        return VersionUtil.ge(KEYCLOAK_VERSION, "26.2");
    }

    @Test
    @Order(0)
    void shouldCreateRealmWithAuthorizationSettingsWhenFgapV2IsActive() throws IOException {

        doImport("00_create_realm_with_authorization_fgap_v2.json");

        RealmRepresentation createdRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        // Verify that the realm was created successfully despite HTTP 501 warnings
        List<ClientRepresentation> clients = keycloakProvider.getInstance().realm(REALM_NAME).clients().findAll();
        assertThat(clients, is(notNullValue()));
        assertThat(clients.size(), is(greaterThan(0)));

        ClientRepresentation realmManagementClient = clients.stream()
                .filter(client -> "realm-management".equals(client.getClientId()))
                .findFirst()
                .orElse(null);

        assertThat("Realm management client should exist", realmManagementClient, is(notNullValue()));
    }

    @Test
    @Order(1)
    void shouldUpdateAuthorizationSettingsWhenFgapV2Returns501() throws IOException {


        doImport("01_update_realm_authorization_fgap_v2.json");

        RealmRepresentation updatedRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));

        List<ClientRepresentation> clients = keycloakProvider.getInstance().realm(REALM_NAME).clients().findAll();
        ClientRepresentation testClient = clients.stream()
                .filter(client -> "test-auth-client".equals(client.getClientId()))
                .findFirst()
                .orElse(null);

        assertThat("Test authorization client should exist", testClient, is(notNullValue()));
        assertThat("Client should have authorization enabled", testClient.getAuthorizationServicesEnabled(), is(true));
    }

    @Test
    @Order(2)
    void shouldHandleRealmManagementPermissionsWhenFgapV2Active() throws IOException {

        doImport("02_update_realm_management_permissions_fgap_v2.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        // Verify realm-management client exists and has authorization enabled
        List<ClientRepresentation> clients = keycloakProvider.getInstance().realm(REALM_NAME).clients().findAll();
        ClientRepresentation realmManagementClient = clients.stream()
                .filter(client -> "realm-management".equals(client.getClientId()))
                .findFirst()
                .orElse(null);

        assertThat("Realm management client should exist", realmManagementClient, is(notNullValue()));

        Boolean authEnabled = realmManagementClient.getAuthorizationServicesEnabled();
        assertThat("Realm management should have authorization enabled or be in FGAP V2 mode",
                   authEnabled == null || authEnabled, is(true));

    }

    @Test
    @Order(3)
    void shouldGracefullyHandleFgapV2Http501Errors() throws IOException {

        // This configuration will trigger HTTP 501 responses from FGAP V2
        doImport("03_test_fgap_v2_501_error_handling.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        List<ClientRepresentation> clients = keycloakProvider.getInstance().realm(REALM_NAME).clients().findAll();
        assertThat("Clients should exist", clients, is(notNullValue()));
        assertThat("Should have multiple clients", clients.size(), is(greaterThan(1)));

        boolean hasAuthClient = clients.stream()
                .anyMatch(client -> "test-auth-client".equals(client.getClientId()));
        boolean hasRealmManagement = clients.stream()
                .anyMatch(client -> "realm-management".equals(client.getClientId()));

        assertThat("Should have authorization test client", hasAuthClient, is(true));
        assertThat("Should have realm management client", hasRealmManagement, is(true));
    }

    @Test
    @Order(4)
    void shouldHandleAuthorizationResourceCreationWithFgapV2() throws IOException {


        doImport("04_test_authorization_resource_fgap_v2.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        // Verify that authorization-enabled clients exist
        List<ClientRepresentation> clients = keycloakProvider.getInstance().realm(REALM_NAME).clients().findAll();
        ClientRepresentation resourceClient = clients.stream()
                .filter(client -> "resource-test-client".equals(client.getClientId()))
                .findFirst()
                .orElse(null);

        assertThat("Resource test client should exist", resourceClient, is(notNullValue()));
        assertThat("Client should have authorization services enabled",
                   resourceClient.getAuthorizationServicesEnabled(), is(true));
    }

    @Test
    @Order(5)
    void shouldHandleAdminPermissionsClientGracefully() throws IOException {
        // Test that admin-permissions client with authorization settings gets proper handling

        // Delete test realm if it exists
        try {
            keycloakProvider.getInstance().realm("fgap-v2-admin-permissions-test").remove();
        } catch (Exception e) {
            // Realm might not exist, ignore
        }

        doImport("05_test_admin_permissions_client_handling.json");

        RealmRepresentation realm = keycloakProvider.getInstance()
                .realm("fgap-v2-admin-permissions-test").toRepresentation();

        assertThat(realm.getRealm(), is("fgap-v2-admin-permissions-test"));
        assertThat(realm.isEnabled(), is(true));
        assertThat("Admin permissions should be enabled", realm.isAdminPermissionsEnabled(), is(true));

        // Verify the test client was created successfully
        List<ClientRepresentation> clients = keycloakProvider.getInstance()
                .realm("fgap-v2-admin-permissions-test").clients().findAll();

        boolean hasTestClient = clients.stream()
                .anyMatch(client -> "test-app-client".equals(client.getClientId()));
        boolean hasAdminPermissions = clients.stream()
                .anyMatch(client -> "admin-permissions".equals(client.getClientId()));

        assertThat("Should have test application client", hasTestClient, is(true));
        assertThat("Should have admin-permissions client", hasAdminPermissions, is(true));
    }

    @Test
    @Order(6)
    void shouldImportV2AuthorizationSchemaSuccessfully() throws IOException {
        // Test V2 authorizationSchema detection and import
        try {
            keycloakProvider.getInstance().realm("fgap-v2-schema-test").remove();
        } catch (Exception e) {
            // Realm might not exist, ignore
        }

        doImport("06_test_v2_authorizationSchema_import.json");

        RealmRepresentation realm = keycloakProvider.getInstance()
                .realm("fgap-v2-schema-test").toRepresentation();

        assertThat(realm.getRealm(), is("fgap-v2-schema-test"));
        assertThat(realm.isEnabled(), is(true));
        assertThat("Admin permissions should be enabled", realm.isAdminPermissionsEnabled(), is(true));

        // Verify clients exist
        List<ClientRepresentation> clients = keycloakProvider.getInstance()
                .realm("fgap-v2-schema-test").clients().findAll();

        boolean hasTestApp = clients.stream()
                .anyMatch(client -> "test-app".equals(client.getClientId()));
        assertThat("Admin-permissions client should exist", hasClientWithId(clients, "admin-permissions"), is(true));
    }

    private boolean hasClientWithId(List<ClientRepresentation> clients, String clientId) {
        return clients.stream()
                .anyMatch(client -> clientId.equals(client.getClientId()));
    }
}
