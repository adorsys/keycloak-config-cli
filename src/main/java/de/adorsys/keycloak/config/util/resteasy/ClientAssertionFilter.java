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

import java.io.IOException;
import java.util.Optional;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriBuilder;

public final class ClientAssertionFilter implements ClientRequestFilter {
    private static final String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    private final ClientAssertionProvider clientAssertionProvider;
    private final String tokenEndpointPath;
    private final String logoutEndpointPath;

    public ClientAssertionFilter(ClientAssertionProvider clientAssertionProvider, String keycloakUrl, String realm) {
        this.clientAssertionProvider = clientAssertionProvider;
        this.tokenEndpointPath = endpointPath(keycloakUrl, realm, "token");
        this.logoutEndpointPath = endpointPath(keycloakUrl, realm, "logout");
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        Optional<MultivaluedMap<String, String>> form = getClientAuthenticationForm(requestContext);
        if (form.isEmpty()) {
            return;
        }

        String clientAssertion = clientAssertionProvider.getClientAssertion();
        MultivaluedMap<String, String> clientAuthenticationForm = form.orElseThrow();
        clientAuthenticationForm.remove("client_id");
        clientAuthenticationForm.remove("client_secret");
        clientAuthenticationForm.putSingle("client_assertion_type", CLIENT_ASSERTION_TYPE);
        clientAuthenticationForm.putSingle("client_assertion", clientAssertion);
    }

    @SuppressWarnings("unchecked")
    private Optional<MultivaluedMap<String, String>> getClientAuthenticationForm(ClientRequestContext requestContext) {
        String path = requestContext.getUri().getPath();
        if (path == null || !(path.equals(tokenEndpointPath) || path.equals(logoutEndpointPath))) {
            return Optional.empty();
        }

        Object entity = requestContext.getEntity();
        if (entity instanceof Form form) {
            return Optional.of(form.asMap());
        }
        if (entity instanceof MultivaluedMap<?, ?>) {
            return Optional.of((MultivaluedMap<String, String>) entity);
        }
        return Optional.empty();
    }

    private static String endpointPath(String keycloakUrl, String realm, String endpoint) {
        return UriBuilder.fromUri(keycloakUrl)
                .path("realms")
                .path(realm)
                .path("protocol")
                .path("openid-connect")
                .path(endpoint)
                .build()
                .getPath();
    }
}
