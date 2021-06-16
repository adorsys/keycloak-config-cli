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

import de.adorsys.keycloak.config.AbstractImportTest;
import org.hamcrest.core.IsNull;
import org.junit.internal.matchers.ThrowableMessageMatcher;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.Arrays;
import javax.ws.rs.InternalServerErrorException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestPropertySource(properties = {
        "import.sync-user-federation=true"
})
public class SyncUserFederationIT extends AbstractImportTest {

    private static final String REALM_NAME = "realmWithLdap";
    private static final String REALM_NAME_WITHOUT_FEDERATION = "realmWithoutLdap";
    private static final String FUNCTION_WHERE_THE_ERROR_MUST_BE_GENERATED = "syncUserFederationIfNecessary";

    public SyncUserFederationIT() {
        this.resourcePath = "import-files/user-federation";
    }

    @Test
    @Order(0)
    void shouldCreateRealmWithUser() {
        InternalServerErrorException thrown = assertThrows(
                InternalServerErrorException.class,
                () -> doImport("00_create_realm_with_federation.json")
        );

        // This matching use the name of the function where the error occurs in order to guarantee the source.
        boolean throwableCameFromUserFederationSync = Arrays.stream(thrown.getStackTrace())
                .anyMatch(stackTraceElement -> stackTraceElement.getMethodName().contains(FUNCTION_WHERE_THE_ERROR_MUST_BE_GENERATED));
        assertThat(thrown, new ThrowableMessageMatcher<>(is("HTTP 500 Internal Server Error")));
        assertThat(throwableCameFromUserFederationSync, is(true));

        RealmRepresentation createdRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));
    }

    @Test
    @Order(1)
    void withoutFederationNoUsersAreCreated() throws IOException {
        doImport("01_create_realm_without_federation.json");

        RealmRepresentation createdRealm = keycloakProvider.getInstance().realm(REALM_NAME_WITHOUT_FEDERATION).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME_WITHOUT_FEDERATION));
        assertThat(createdRealm.isEnabled(), is(true));

        assertThat(createdRealm.getUsers(), is(new IsNull<>()));
    }

    @Test
    @Order(2)
    void withoutComponentNoUsersAreCreated() throws IOException {
        doImport("02_create_realm_without_component.json");

        RealmRepresentation createdRealm = keycloakProvider.getInstance().realm(REALM_NAME_WITHOUT_FEDERATION).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME_WITHOUT_FEDERATION));
        assertThat(createdRealm.isEnabled(), is(true));

        assertThat(createdRealm.getUsers(), is(new IsNull<>()));
    }

    @Test
    @Order(3)
    void importDisableShouldNotMakeRequest() throws IOException {
        doImport("03_create_realm_with_federation_import_disable.json");

        RealmRepresentation createdRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));
    }
}
