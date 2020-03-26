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

import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ImportSimpleRealmIT extends AbstractImportTest {
    private static final String REALM_NAME = "simple";

    ImportSimpleRealmIT() {
        this.resourcePath = "import-files/simple-realm";
    }

    @Test
    @Order(0)
    public void shouldCreateSimpleRealm() {
        doImport("0_create_simple-realm.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));
        assertThat(createdRealm.getLoginTheme(), is(nullValue()));
        assertThat(
                createdRealm.getAttributes().get("de.adorsys.keycloak.config.import-checksum-default"),
                is("3796660d3087308ee757d9d86e14dd6e6fe4bfd66cc1435851ff2f5c6fa432c5991b3042f95c4f11238e1dfb81676ae2a00bde0bbad17c1f66ef530841df2e66")
        );
    }

    @Test
    @Order(1)
    public void shouldNotUpdateSimpleRealm() {
        doImport("0.1_update_simple-realm_with_same_config.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));
        assertThat(createdRealm.getLoginTheme(), is(nullValue()));
        assertThat(
                createdRealm.getAttributes().get("de.adorsys.keycloak.config.import-checksum-default"),
                is("3796660d3087308ee757d9d86e14dd6e6fe4bfd66cc1435851ff2f5c6fa432c5991b3042f95c4f11238e1dfb81676ae2a00bde0bbad17c1f66ef530841df2e66")
        );
    }

    @Test
    @Order(2)
    public void shouldUpdateSimpleRealm() {
        doImport("1_update_login-theme_to_simple-realm.json");

        RealmRepresentation updatedRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));
        assertThat(updatedRealm.getLoginTheme(), is("moped"));
        assertThat(
                updatedRealm.getAttributes().get("de.adorsys.keycloak.config.import-checksum-default"),
                is("d3913c179bf6d1ed1afbc2580207f3d7d78efed3ef13f9e12dea3afd5c28e9b307dd930fecfcc100038e540d1e23dc5b5c74d0321a410c7ba330e9dbf9d4211c")
        );
    }

    @Test
    @Order(3)
    public void shouldCreateSimpleRealmWithLoginTheme() {
        doImport("2_create_simple-realm_with_login-theme.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm("simpleWithLoginTheme").toRepresentation();

        assertThat(createdRealm.getRealm(), is("simpleWithLoginTheme"));
        assertThat(createdRealm.isEnabled(), is(true));
        assertThat(createdRealm.getLoginTheme(), is("moped"));
        assertThat(
                createdRealm.getAttributes().get("de.adorsys.keycloak.config.import-checksum-default"),
                is("5d75698bacb06b1779e2b303069266664d63eec9c52038e2e6ae930bfc6e33ec7e7493b067ee0253e73a6b19cdf8905fd75cc6bb394ca333d32c784063aa65c8")
        );
    }

    @Test
    @Order(4)
    public void shouldNotCreateSimpleRealmWithInvalidName() {
        KeycloakRepositoryException thrown = assertThrows(
                KeycloakRepositoryException.class,
                () -> doImport("4_create_simple-realm_with_invalid_name.json")
        );

        assertThat(thrown.getMessage(), matchesPattern("^Cannot create realm '.+'$"));
    }
}
