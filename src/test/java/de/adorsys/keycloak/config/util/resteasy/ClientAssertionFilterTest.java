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

package de.adorsys.keycloak.config.util.resteasy;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ClientAssertionFilterTest {

    private static final String KEYCLOAK_URL = "https://keycloak.example";
    private static final String REALM = "master";
    private static final URI TOKEN_ENDPOINT = URI.create("https://keycloak.example/realms/master/protocol/openid-connect/token");
    private static final URI LOGOUT_ENDPOINT = URI.create("https://keycloak.example/realms/master/protocol/openid-connect/logout");
    private static final String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    @Test
    void shouldOnlyMutateTokenRequests() throws IOException {
        ClientAssertionProvider provider = mock(ClientAssertionProvider.class);
        MultivaluedMap<String, String> form = clientCredentialsForm();

        new ClientAssertionFilter(provider, KEYCLOAK_URL, REALM)
                .filter(request(URI.create("https://keycloak.example/admin/realms"), form));

        assertThat(form.getFirst("client_id"), is("config-cli"));
        assertThat(form.getFirst("client_secret"), is("secret"));
        assertThat(form.containsKey("client_assertion"), is(false));
        verifyNoInteractions(provider);
    }

    @Test
    void shouldReplaceClientCredentialsWithRfcClientAssertionFields() throws IOException {
        ClientAssertionProvider provider = mock(ClientAssertionProvider.class);
        when(provider.getClientAssertion()).thenReturn("assertion");
        Form form = new Form();
        form.param("grant_type", "client_credentials");
        form.param("client_id", "config-cli");
        form.param("client_secret", "secret");

        new ClientAssertionFilter(provider, KEYCLOAK_URL, REALM).filter(request(TOKEN_ENDPOINT, form));

        assertThat(form.asMap().getFirst("grant_type"), is("client_credentials"));
        assertThat(form.asMap().containsKey("client_id"), is(false));
        assertThat(form.asMap().containsKey("client_secret"), is(false));
        assertThat(form.asMap().getFirst("client_assertion_type"), is(CLIENT_ASSERTION_TYPE));
        assertThat(form.asMap().getFirst("client_assertion"), is("assertion"));
        verify(provider).getClientAssertion();
    }

    @Test
    void shouldAuthenticateLogoutWithClientAssertion() throws IOException {
        ClientAssertionProvider provider = mock(ClientAssertionProvider.class);
        when(provider.getClientAssertion()).thenReturn("assertion");
        Form form = new Form();
        form.param("refresh_token", "refresh-token");

        new ClientAssertionFilter(provider, KEYCLOAK_URL, REALM).filter(request(LOGOUT_ENDPOINT, form));

        assertThat(form.asMap().getFirst("refresh_token"), is("refresh-token"));
        assertThat(form.asMap().getFirst("client_assertion_type"), is(CLIENT_ASSERTION_TYPE));
        assertThat(form.asMap().getFirst("client_assertion"), is("assertion"));
    }

    private static MultivaluedMap<String, String> clientCredentialsForm() {
        MultivaluedMap<String, String> form = new MultivaluedHashMap<>();
        form.putSingle("grant_type", "client_credentials");
        form.putSingle("client_id", "config-cli");
        form.putSingle("client_secret", "secret");
        return form;
    }

    private static ClientRequestContext request(URI uri, Object entity) {
        ClientRequestContext request = mock(ClientRequestContext.class);
        when(request.getUri()).thenReturn(uri);
        when(request.getEntity()).thenReturn(entity);
        return request;
    }
}
