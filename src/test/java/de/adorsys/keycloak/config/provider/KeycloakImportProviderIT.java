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

package de.adorsys.keycloak.config.provider;

import de.adorsys.keycloak.config.AbstractImportTest;
import de.adorsys.keycloak.config.exception.InvalidImportException;
import de.adorsys.keycloak.config.model.KeycloakImport;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.springtest.MockServerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@MockServerTest("mockServerUrl=http://localhost:${mockServerPort}")
class KeycloakImportProviderIT extends AbstractImportTest {
    private MockServerClient mockServerClient;

    @Test
    void shouldReadLocalFile() {
        String location = "classpath:import-files/import-single/0_create_realm.json";
        KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location);

        assertThat(keycloakImport.getRealmImports(), hasKey(is(location)));
        assertThat(keycloakImport.getRealmImports().keySet(), contains(
                matchesPattern(".+/0_create_realm\\.json$")
        ));
    }

    @Test
    void shouldReadLocalFileLegacy() throws IOException {
        Path realmFile = Files.createTempFile("realm", ".json");
        Path tempFilePath = Files.writeString(realmFile, "{\"enabled\": true, \"realm\": \"realm-sorted-import\"}");

        String importPath = "file:" + tempFilePath.toAbsolutePath();
        KeycloakImport keycloakImport = keycloakImportProvider
                .readFromLocations(importPath);

        assertThat(keycloakImport.getRealmImports(), hasKey(is(importPath)));
        assertThat(keycloakImport.getRealmImports().get(importPath).keySet(), contains(importPath));
    }

    @Test
    void shouldReadLocalFilesFromDirectorySorted() {
        String location = "classpath:import-files/import-sorted/*";
        KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location);
        assertThat(keycloakImport.getRealmImports(), hasKey(is(location)));
        assertThat(keycloakImport.getRealmImports().get(location).keySet(), contains(
                matchesPattern(".+/0_create_realm\\.json"),
                matchesPattern(".+/1_update_realm\\.json"),
                matchesPattern(".+/2_update_realm\\.json"),
                matchesPattern(".+/3_update_realm\\.json"),
                matchesPattern(".+/4_update_realm\\.json"),
                matchesPattern(".+/5_update_realm\\.json"),
                matchesPattern(".+/6_update_realm\\.json"),
                matchesPattern(".+/7_update_realm\\.json"),
                matchesPattern(".+/8_update_realm\\.json"),
                matchesPattern(".+/9_update_realm\\.json")
        ));
    }

    @Test
    void shouldReadLocalFilesFromDirectorySortedWithoutHiddenFiles() {
        String location = "classpath:import-files/import-sorted-hidden-files/*";
        KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location);
        assertThat(keycloakImport.getRealmImports(), hasKey(is(location)));
        assertThat(keycloakImport.getRealmImports().get(location).keySet(), contains(
                matchesPattern(".+/0_create_realm\\.json"),
                matchesPattern(".+/1_update_realm\\.json"),
                matchesPattern(".+/2_update_realm\\.json"),
                matchesPattern(".+/4_update_realm\\.json"),
                matchesPattern(".+/5_update_realm\\.json"),
                matchesPattern(".+/6_update_realm\\.json"),
                matchesPattern(".+/8_update_realm\\.json"),
                matchesPattern(".+/9_update_realm\\.json")
        ));
    }

    @Test
    void shouldReadLocalFilesFromDirectorySortedWithoutHiddenFilesWithRegex() {
        String location = "classpath:import-files/import-sorted-hidden-files/{filename:[^8]+}";
        KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location);
        assertThat(keycloakImport.getRealmImports(), hasKey(is(location)));
        assertThat(keycloakImport.getRealmImports().get(location).keySet(), contains(
                matchesPattern(".+/0_create_realm\\.json"),
                matchesPattern(".+/1_update_realm\\.json"),
                matchesPattern(".+/2_update_realm\\.json"),
                matchesPattern(".+/4_update_realm\\.json"),
                matchesPattern(".+/5_update_realm\\.json"),
                matchesPattern(".+/6_update_realm\\.json"),
                matchesPattern(".+/9_update_realm\\.json")
        ));
    }

    @Test
    void shouldReadLocalFilesFromWildcardPattern() {
        String location = "classpath:import-files/import-wildcard/*.json";
        KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location);
        assertThat(keycloakImport.getRealmImports(), hasKey(is(location)));
        assertThat(keycloakImport.getRealmImports().get(location).keySet(), contains(
                matchesPattern(".+/0_create_realm\\.json")
        ));
    }

    @Test
    void shouldReadLocalFilesFromRegexPattern() {
        String location = "classpath:import-files/import-wildcard/{filename:.+\\.json}";
        KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location);
        assertThat(keycloakImport.getRealmImports(), hasKey(is(location)));
        assertThat(keycloakImport.getRealmImports().get(location).keySet(), contains(
                matchesPattern(".+/0_create_realm\\.json")
        ));
    }

    @Test
    void shouldReadLocalFilesFromDoubleWildcardPattern() {
        String location = "classpath:import-files/import-wildcard/**/*.json";
        KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location);
        assertThat(keycloakImport.getRealmImports(), hasKey(is(location)));
        assertThat(keycloakImport.getRealmImports().get(location).keySet(), contains(
                matchesPattern(".+/0_create_realm\\.json"),
                matchesPattern(".+/another/directory/1_update_realm\\.json"),
                matchesPattern(".+/another/directory/2_update_realm\\.json"),
                matchesPattern(".+/another/directory/3_update_realm\\.json"),
                matchesPattern(".+/sub/directory/4_update_realm\\.json"),
                matchesPattern(".+/sub/directory/5_update_realm\\.json"),
                matchesPattern(".+/sub/directory/6_update_realm\\.json")
        ));
    }

    @Test
    void shouldReadLocalFilesFromManyDirectories() {
        String location1 = "classpath:import-files/import-wildcard/sub/**";
        String location2 = "classpath:import-files/import-wildcard/another/**/*.json";
        KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location1, location2);
        assertThat(keycloakImport.getRealmImports().keySet(), contains(is(location1), is(location2)));
        assertThat(keycloakImport.getRealmImports().get(location1).keySet(), contains(
                matchesPattern(".+/sub/directory/4_update_realm\\.json"),
                matchesPattern(".+/sub/directory/5_update_realm\\.json"),
                matchesPattern(".+/sub/directory/6_update_realm\\.json"),
                matchesPattern(".+/sub/directory/7_update_realm\\.yaml")
        ));
        assertThat(keycloakImport.getRealmImports().get(location2).keySet(), contains(
                matchesPattern(".+/another/directory/1_update_realm\\.json"),
                matchesPattern(".+/another/directory/2_update_realm\\.json"),
                matchesPattern(".+/another/directory/3_update_realm\\.json")
        ));
    }

    @Test
    void shouldReadLocalFilesFromZipArchive() {
        String location = "zip:file:src/test/resources/import-files/import-zip/realm-import.zip!/**/*";
        KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location);

        assertThat(keycloakImport.getRealmImports(), hasKey(is(location)));
        assertThat(keycloakImport.getRealmImports().get(location).keySet(), contains(
                matchesPattern(".+/0_create_realm\\.json$"),
                matchesPattern(".+/1_update_realm\\.json$"),
                matchesPattern(".+/2_update_realm\\.json$"),
                matchesPattern(".+/3_update_realm\\.json$"),
                matchesPattern(".+/4_update_realm\\.json$"),
                matchesPattern(".+/5_update_realm\\.json$")
        ));
    }

    @Test
    void shouldReadLocalFilesFromZipArchiveWithRegex() {
        String location = "zip:file:src/test/resources/import-files/import-zip/realm-import.zip!/**/{filename:[^3]+.json}";
        KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location);

        assertThat(keycloakImport.getRealmImports(), hasKey(is(location)));
        assertThat(keycloakImport.getRealmImports().get(location).keySet(), contains(
                matchesPattern(".+/0_create_realm\\.json$"),
                matchesPattern(".+/1_update_realm\\.json$"),
                matchesPattern(".+/2_update_realm\\.json$"),
                matchesPattern(".+/4_update_realm\\.json$"),
                matchesPattern(".+/5_update_realm\\.json$")
        ));
    }

    @Test
    void shouldFailOnUnknownPath() {
        InvalidImportException exception = assertThrows(InvalidImportException.class, () -> keycloakImportProvider.readFromLocations("classpath::"));

        assertThat(exception.getMessage(), is("Unable to proceed resource 'class path resource [:]': class path resource [:] cannot be opened because it does not exist"));
    }

    @Test
    void shouldReadRemoteFile() {
        mockServerClient.when(request()).respond(this::mockServerResponse);

        String location = mockServerUrl() + "/import-single/0_create_realm.json";
        KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location);

        assertThat(keycloakImport.getRealmImports(), hasKey(is(location)));
        assertThat(keycloakImport.getRealmImports().get(location).keySet(), contains(
                matchesPattern(".+/0_create_realm\\.json$")
        ));
    }

    @Test
    void shouldReadRemoteFilesFromZipArchive() {
        mockServerClient.when(request()).respond(this::mockServerResponse);

        String location = "zip:" + mockServerUrl() + "/import-zip/realm-import.zip!/**/*";
        KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location);

        assertThat(keycloakImport.getRealmImports(), hasKey(is(location)));
        assertThat(keycloakImport.getRealmImports().get(location).keySet(), contains(
                matchesPattern(".+/0_create_realm\\.json$"),
                matchesPattern(".+/1_update_realm\\.json$"),
                matchesPattern(".+/2_update_realm\\.json$"),
                matchesPattern(".+/3_update_realm\\.json$"),
                matchesPattern(".+/4_update_realm\\.json$"),
                matchesPattern(".+/5_update_realm\\.json$")
        ));
    }

    @Test
    void shouldReadRemoteFileUsingBasicAuth() {
        String userInfo = "user:password";
        String location = mockServerUrl(userInfo) + "/import-single/0_create_realm.json";

        mockServerClient.when(
                request().withHeaders(header("Authorization", "Basic dXNlcjpwYXNzd29yZA=="))
        ).respond(this::mockServerResponse);
        mockServerClient.when(request()).respond(this::mockServerAuthorizationRequiredResponse);

        KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location);

        assertThat(keycloakImport.getRealmImports(), hasKey(is(location)));
        assertThat(keycloakImport.getRealmImports().get(location).keySet(), contains(
                matchesPattern(".+/0_create_realm\\.json$")
        ));
    }

    private String mockServerUrl(String userInfo) {
        URIBuilder builder = new URIBuilder();
        builder.setScheme("http");
        builder.setHost(mockServerClient.remoteAddress().getAddress().getHostAddress());
        builder.setPort(mockServerClient.getPort());
        builder.setUserInfo(userInfo);
        return builder.toString();
    }

    @Nested
    @TestPropertySource(properties = {
            "import.files.include-hidden-files=true"
    })
    class HiddenFilesTrue extends AbstractImportTest {
        @Autowired
        KeycloakImportProvider keycloakImportProvider;

        @Test
        void shouldReadLocalFilesFromDirectorySorted() {
            String location = "classpath:import-files/import-sorted-hidden-files/*";
            KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location);

            assertThat(keycloakImport.getRealmImports(), hasKey(is(location)));
            assertThat(keycloakImport.getRealmImports().get(location).keySet(), contains(
                    matchesPattern(".+/.3_update_realm\\.json"),
                    matchesPattern(".+/.7_update_realm\\.json"),
                    matchesPattern(".+/0_create_realm\\.json"),
                    matchesPattern(".+/1_update_realm\\.json"),
                    matchesPattern(".+/2_update_realm\\.json"),
                    matchesPattern(".+/4_update_realm\\.json"),
                    matchesPattern(".+/5_update_realm\\.json"),
                    matchesPattern(".+/6_update_realm\\.json"),
                    matchesPattern(".+/8_update_realm\\.json"),
                    matchesPattern(".+/9_update_realm\\.json")
            ));
        }
    }


    private String mockServerUrl() {
        return mockServerUrl(null);
    }

    @Nested
    @TestPropertySource(properties = {
            "import.files.excludes=**/*create*,**/4_*"
    })
    class Exclude extends AbstractImportTest {
        @Autowired
        KeycloakImportProvider keycloakImportProvider;

        @Test
        void shouldReadLocalFilesFromDirectorySorted() {
            String location = "classpath:import-files/import-sorted-hidden-files/*";
            KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location);

            assertThat(keycloakImport.getRealmImports(), hasKey(is(location)));
            assertThat(keycloakImport.getRealmImports().get(location).keySet(), contains(
                    matchesPattern(".+/1_update_realm\\.json"),
                    matchesPattern(".+/2_update_realm\\.json"),
                    matchesPattern(".+/5_update_realm\\.json"),
                    matchesPattern(".+/6_update_realm\\.json"),
                    matchesPattern(".+/8_update_realm\\.json"),
                    matchesPattern(".+/9_update_realm\\.json")
            ));
        }
    }

    private HttpResponse mockServerResponse(HttpRequest request) throws IOException {
        return response().withBody(
                IOUtils.toByteArray(
                        new ClassPathResource(
                                "import-files" + request.getPath().getValue()
                        ).getInputStream()
                )
        );
    }

    private HttpResponse mockServerAuthorizationRequiredResponse(HttpRequest request) {
        return response().withHeader("WWW-Authenticate", "Basic realm=\"protected\"").withStatusCode(401);
    }
}
