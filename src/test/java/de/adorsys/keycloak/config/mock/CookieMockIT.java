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

import de.adorsys.keycloak.config.configuration.TestConfiguration;
import de.adorsys.keycloak.config.extensions.GithubActionsExtension;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.provider.KeycloakImportProvider;
import de.adorsys.keycloak.config.service.RealmImportService;
import de.adorsys.keycloak.config.test.util.KeycloakMock;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Cookie;
import org.mockserver.springtest.MockServerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockserver.model.HttpRequest.request;

@ActiveProfiles("IT")
@MockServerTest("keycloak.url=http://localhost:${mockServerPort}")
@ExtendWith(SpringExtension.class)
@ExtendWith(GithubActionsExtension.class)
@ContextConfiguration(
        classes = {TestConfiguration.class},
        initializers = {ConfigDataApplicationContextInitializer.class}
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(GithubActionsExtension.class)
@TestPropertySource(properties = {
        "import.state=false",
        "import.force=true"
})
class CookieMockIT {
    private MockServerClient client;

    @Autowired
    public KeycloakImportProvider keycloakImportProvider;
    @Autowired
    public RealmImportService realmImportService;

    @Test
    void run() throws Exception {
        client.when(request().withPath("/realms/master/protocol/openid-connect/token")).respond(KeycloakMock::grantToken);
        client.when(request().withPath("/admin/realms/simple")).respond(KeycloakMock::realm);
        client.when(request().withPath("/admin/realms/simple/default-default-client-scopes")).respond(KeycloakMock::emptyList);
        client.when(request().withPath("/admin/realms/simple/default-optional-client-scopes")).respond(KeycloakMock::emptyList);
        client.when(request().withPath("/admin/realms/simple/identity-provider/instances")).respond(KeycloakMock::emptyList);
        client.when(request().withPath("/realms/master/protocol/openid-connect/logout")).respond(KeycloakMock::noContent);

        client.when(request().withPath("/admin/serverinfo")).respond((request) -> {
            assertThat(request.getCookieList(), hasSize(2));
            assertThat(request.getCookieList(), containsInAnyOrder(
                    new Cookie("key_expires", "value"),
                    new Cookie("key", "value")
            ));

            return KeycloakMock.serverInfo(request);
        });

        RealmImport realmImport = getRealmImport("import-files/simple-realm/00_create_simple-realm.json");
        realmImportService.doImport(realmImport);
    }

    @SuppressWarnings("SameParameterValue")
    private RealmImport getRealmImport(String file) throws IOException {
        File realmImportFile = new ClassPathResource(file).getFile();

        return keycloakImportProvider
                .readRealmImportFromFile(realmImportFile)
                .getRealmImports()
                .get(realmImportFile.getAbsolutePath());
    }
}
