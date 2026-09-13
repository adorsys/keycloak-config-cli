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

package de.adorsys.keycloak.config.mock;

import de.adorsys.keycloak.config.AbstractImportTest;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.test.util.KeycloakMock;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.springtest.MockServerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@MockServerTest("keycloak.url=http://localhost:${mockServerPort}")
@TestPropertySource(properties = {
        "keycloak.client-id=config-cli",
        "keycloak.client-secret=secret",
        "keycloak.grant-type=client_credentials",
        "keycloak.client-assertion-file=src/test/resources/client-assertion-token",
        "keycloak.skip-server-info=true",
        "keycloak.version=26.6.2"
})
class ClientAssertionMockIT extends AbstractImportTest {
    private MockServerClient mockServerClient;

    @Autowired
    private KeycloakProvider keycloakProvider;

    @Test
    void shouldSendClientAssertionWithoutClientCredentials() {
        mockServerClient.when(request().withPath("/realms/master/protocol/openid-connect/token")).respond(tokenRequest -> {
            String body = tokenRequest.getBodyAsString();
            assertThat(body, containsString("grant_type=client_credentials"));
            assertThat(body, containsString("client_assertion_type=urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer"));
            assertThat(body, containsString("client_assertion=assertion-from-file"));
            assertThat(body, not(containsString("client_id=")));
            assertThat(body, not(containsString("client_secret=")));
            assertThat(tokenRequest.containsHeader("Authorization"), is(false));
            return KeycloakMock.grantToken(tokenRequest);
        });

        keycloakProvider.getInstance();

        mockServerClient.when(request().withPath("/realms/master/protocol/openid-connect/logout")).respond(logoutRequest -> {
            String body = logoutRequest.getBodyAsString();
            assertThat(body, containsString("refresh_token="));
            assertThat(body, containsString("client_assertion=assertion-from-file"));
            assertThat(body, not(containsString("client_id=")));
            assertThat(body, not(containsString("client_secret=")));
            assertThat(logoutRequest.containsHeader("Authorization"), is(false));
            return response().withStatusCode(204);
        });

        keycloakProvider.close();
    }
}
