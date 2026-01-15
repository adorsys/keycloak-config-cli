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

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@EnabledIf(value = "isKeycloak262orHigher", disabledReason = "FGAP V2 only available in Keycloak 26.2+")
class ImportFgapV2SkipIT extends AbstractImportIT {
    private static final String REALM_NAME = "fgap-v2-repro";

    ImportFgapV2SkipIT() {
        this.resourcePath = "import-files/fgap-v2-compatibility";
    }

    static boolean isKeycloak262orHigher() {
        return VersionUtil.ge(KEYCLOAK_VERSION, "26.2");
    }

    @Test
    @Order(1)
    void shouldSkipAdminPermissionsClientOnUpdate() throws IOException {
        // 1. Initial Import
        // This contains the admin-permissions client. It should be skipped (logged) but not fail.
        doImport("09_create_realm_with_admin_permissions_client.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        // 2. Update Import
        // attempts to update the admin-permissions client.
        doImport("10_update_realm_with_admin_permissions_client.json");
    }

    @Test
    void shouldHandleAdminPermissionsWithPolicies() throws IOException {
        // This test uses a configuration with complex policies on admin-permissions.
        // It should be skipped and pass successfully.
        doImport("11_import_realm_with_admin_permissions_client_and_policies.json");
        
        // Run again to ensure idempotency (update phase)
        doImport("11_import_realm_with_admin_permissions_client_and_policies.json");
    }
}