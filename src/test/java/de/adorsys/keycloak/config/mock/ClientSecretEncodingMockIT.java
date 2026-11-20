/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2026 adorsys GmbH & Co. KG @ https://adorsys.com
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

package de.adorsys.keycloak.config.mock;

import de.adorsys.keycloak.config.AbstractImportTest;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.test.util.KeycloakMock;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.springtest.MockServerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockserver.model.HttpRequest.request;

@MockServerTest("keycloak.url=http://localhost:${mockServerPort}")
@TestPropertySource(properties = {
        "keycloak.client-id=config-cli",
        "keycloak.client-secret=abc+def",
        "keycloak.grant-type=client_credentials",
        "keycloak.skip-server-info=true",
        "keycloak.version=26.7.4"
})
class ClientSecretEncodingMockIT extends AbstractImportTest {
    private MockServerClient mockServerClient;

    @Autowired
    private KeycloakProvider keycloakProvider;

    @Test
    void shouldFormEncodeClientSecretInBasicAuthorizationHeader() {
        String tokenPath = "/realms/master/protocol/openid-connect/token";
        mockServerClient.when(request().withPath(tokenPath)).respond(KeycloakMock::grantToken);

        keycloakProvider.getInstance();

        HttpRequest[] requests = mockServerClient.retrieveRecordedRequests(request().withPath(tokenPath));
        assertThat("Expected exactly one token request", requests.length, is(1));

        String authorization = requests[0].getFirstHeader("Authorization");
        assertNotNull(authorization, "Expected HTTP Basic client authentication");
        assertThat(authorization, startsWith("Basic "));

        String credentials = new String(
                Base64.getDecoder().decode(authorization.substring("Basic ".length())),
                StandardCharsets.UTF_8);
        assertThat("OAuth 2.0 Basic credentials must be form-encoded before Base64 encoding",
                credentials, is("config-cli:abc%2Bdef"));
    }
}
