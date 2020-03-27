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

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

public class ImportClientsIT extends AbstractImportTest {
    private static final String REALM_NAME = "realmWithClients";

    ImportClientsIT() {
        this.resourcePath = "import-files/clients";
    }

    @Test
    @Order(1)
    public void shouldCreateRealmWithClient() {
        doImport("0_create_realm_with_client.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        ClientRepresentation createdClient = keycloakRepository.getClient(
                REALM_NAME,
                "moped-client"
        );

        assertThat(createdClient.getName(), is("moped-client"));
        assertThat(createdClient.getClientId(), is("moped-client"));
        assertThat(createdClient.getDescription(), is("Moped-Client"));
        assertThat(createdClient.isEnabled(), is(true));
        assertThat(createdClient.getClientAuthenticatorType(), is("client-secret"));
        assertThat(createdClient.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(createdClient.getWebOrigins(), is(containsInAnyOrder("*")));

        // client secret on this place is always null...
        assertThat(createdClient.getSecret(), is(nullValue()));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(createdClient.getId());
        assertThat(clientSecret, is("my-special-client-secret"));
    }

    @Test
    @Order(2)
    public void shouldUpdateRealmByAddingClient() {
        doImport("1_update_realm__add_client.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        ClientRepresentation createdClient = keycloakRepository.getClient(
                REALM_NAME,
                "another-client"
        );

        assertThat(createdClient.getName(), is("another-client"));
        assertThat(createdClient.getClientId(), is("another-client"));
        assertThat(createdClient.getDescription(), is("Another-Client"));
        assertThat(createdClient.isEnabled(), is(true));
        assertThat(createdClient.getClientAuthenticatorType(), is("client-secret"));
        assertThat(createdClient.getRedirectUris(), is(containsInAnyOrder("*")));
        assertThat(createdClient.getWebOrigins(), is(containsInAnyOrder("*")));

        // client secret on this place is always null...
        assertThat(createdClient.getSecret(), is(nullValue()));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(createdClient.getId());
        assertThat(clientSecret, is("my-other-client-secret"));
    }

    @Test
    @Order(3)
    public void shouldUpdateRealmWithChangedClientProperties() {
        doImport("1_update_realm__change_clients_properties.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        ClientRepresentation createdClient = keycloakRepository.getClient(
                REALM_NAME,
                "moped-client"
        );

        assertThat(createdClient.getName(), is("moped-client"));
        assertThat(createdClient.getClientId(), is("moped-client"));
        assertThat(createdClient.getDescription(), is("Moped-Client"));
        assertThat(createdClient.isEnabled(), is(true));
        assertThat(createdClient.getClientAuthenticatorType(), is("client-secret"));
        assertThat(createdClient.getRedirectUris(), is(containsInAnyOrder("https://moped-client.org/redirect")));
        assertThat(createdClient.getWebOrigins(), is(containsInAnyOrder("https://moped-client.org/webOrigin")));

        // client secret on this place is always null...
        assertThat(createdClient.getSecret(), is(nullValue()));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(createdClient.getId());
        assertThat(clientSecret, is("changed-special-client-secret"));
    }

    @Test
    @Order(4)
    void shouldUpdateRealmWithChangedClientProperties() throws Exception {
        doImport("2_update_realm__change_clients_properties.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        ClientRepresentation createdClient = keycloakRepository.getClient(
                REALM_NAME,
                "moped-client"
        );

        assertThat(createdClient.getName(), is("moped-client"));
        assertThat(createdClient.getClientId(), is("moped-client"));
        assertThat(createdClient.getDescription(), is("Moped-Client"));
        assertThat(createdClient.isEnabled(), is(true));
        assertThat(createdClient.getClientAuthenticatorType(), is("client-secret"));
        assertThat(createdClient.getRedirectUris(), is(containsInAnyOrder("https://moped-client.org/redirect")));
        assertThat(createdClient.getWebOrigins(), is(containsInAnyOrder("https://moped-client.org/webOrigin")));

        // client secret on this place is always null...
        assertThat(createdClient.getSecret(), is(nullValue()));

        // ... and has to be retrieved separately
        String clientSecret = getClientSecret(createdClient.getId());
        assertThat(clientSecret, is("changed-special-client-secret"));
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

    /**
     * @param id (not client-id)
     */
    private String getClientSecret(String id) {
        CredentialRepresentation secret = keycloakProvider.get()
                .realm(REALM_NAME)
                .clients().get(id).getSecret();

        return secret.getValue();
    }
}
