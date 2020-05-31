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

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@QuarkusTest
public class CustomImportIT extends AbstractImportTest {
    private static final String REALM_NAME = "realmWithCustomImport";

    CustomImportIT() {
        this.resourcePath = "import-files/custom-import";
    }

    @Test
    @Order(0)
    public void shouldCreateRealm() {
        doImport("0_create_realm_with_empty_custom-import.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        boolean isImpersonationClientRoleExisting = keycloakRepository.isClientRoleExisting("master", "realmWithCustomImport-realm", "impersonation");

        assertThat(isImpersonationClientRoleExisting, is(true));
    }

    @Test
    @Order(1)
    public void shouldRemoveImpersonation() {
        doImport("1_update_realm__remove_impersonation.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        boolean isImpersonationClientRoleExisting = keycloakRepository.isClientRoleExisting("master", "realmWithCustomImport-realm", "impersonation");

        assertThat(isImpersonationClientRoleExisting, is(false));
    }
}
