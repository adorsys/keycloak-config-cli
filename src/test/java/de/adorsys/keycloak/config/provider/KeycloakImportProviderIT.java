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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@ExtendWith(MockServerExtension.class)
class KeycloakImportProviderIT extends AbstractImportTest {
    private MockServerClient client;

    @BeforeEach
    public void resetServer(MockServerClient client) {
        this.client = client.reset();
    }

    @Test
    void shouldReadLocalFile() {
        KeycloakImport keycloakImport = keycloakImportProvider
                .readFromPath("classpath:import-files/import-single/0_create_realm.json");

        assertThat(keycloakImport.getRealmImports().keySet(), contains(
                matchesPattern(".+/0_create_realm\\.json$")
        ));
    }

    @Test
    void shouldReadLocalFileLegacy() throws IOException {
        Path realmFile = Files.createTempFile("realm", ".json");
        Path tempFilePath = Files.write(realmFile,
                "{\"enabled\": true, \"realm\": \"realm-sorted-import\"}" .getBytes(StandardCharsets.UTF_8));
        String importPath = tempFilePath.toAbsolutePath().toString();
        KeycloakImport keycloakImport = keycloakImportProvider
                .readFromPath(importPath);

        assertThat(keycloakImport.getRealmImports().keySet(), contains(importPath));
    }

    @Test
    void shouldReadLocalFilesFromDirectorySorted() {
        KeycloakImport keycloakImport = keycloakImportProvider.readFromPath("classpath:import-files/import-sorted/");
        assertThat(keycloakImport.getRealmImports().keySet(), contains(
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
    void shouldReadLocalFilesFromZipArchive() {
        // Given
        KeycloakImport keycloakImport = keycloakImportProvider
                .readFromPath("classpath:import-files/import-zip/realm-import.zip");

        // When
        Set<String> importedFileNames = keycloakImport.getRealmImports().keySet();

        // Then
        assertThat(importedFileNames, hasSize(6));
        assertThat(importedFileNames, contains(
                matchesPattern(".+/0_create_realm.+\\.json$"),
                matchesPattern(".+/1_update_realm.+\\.json$"),
                matchesPattern(".+/2_update_realm.+\\.json$"),
                matchesPattern(".+/3_update_realm.+\\.json$"),
                matchesPattern(".+/4_update_realm.+\\.json$"),
                matchesPattern(".+/5_update_realm.+\\.json$")
        ));
    }

    @Test
    void shouldFailOnUnknownPath() {
        InvalidImportException exception = assertThrows(InvalidImportException.class, () -> keycloakImportProvider.readFromPath("classpath::"));

        assertThat(exception.getMessage(), is("No resource extractor found to handle config property import.path=classpath::! Check your settings."));
    }

    @Test
    void shouldReadRemoteFile() {
        client.when(request()).respond(this::mockServerResponse);
        // Given
        KeycloakImport keycloakImport = keycloakImportProvider.readFromPath(mockServerUrl() + "/import-single/0_create_realm.json");

        // When
        Set<String> importedFileNames = keycloakImport.getRealmImports().keySet();

        // Then
        assertThat(importedFileNames, hasSize(1));
        assertThat(importedFileNames, contains(
                matchesPattern(".+/0_create_realm.+\\.json$")
        ));
    }

    @Test
    void shouldReadRemoteFilesFromZipArchive() {
        client.when(request()).respond(this::mockServerResponse);

        // Given
        KeycloakImport keycloakImport = keycloakImportProvider.readFromPath(mockServerUrl() + "/import-zip/realm-import.zip");

        // When
        Set<String> importedFileNames = keycloakImport.getRealmImports().keySet();

        // Then
        assertThat(importedFileNames, hasSize(6));
        assertThat(importedFileNames, contains(
                matchesPattern(".+/0_create_realm.+\\.json$"),
                matchesPattern(".+/1_update_realm.+\\.json$"),
                matchesPattern(".+/2_update_realm.+\\.json$"),
                matchesPattern(".+/3_update_realm.+\\.json$"),
                matchesPattern(".+/4_update_realm.+\\.json$"),
                matchesPattern(".+/5_update_realm.+\\.json$")
        ));
    }

    @Test
    void shouldReadRemoteFileUsingBasicAuth() {
        String userInfo = "user:password";

        client.when(
                request().withHeaders(header("Authorization", "Basic dXNlcjpwYXNzd29yZA=="))
        ).respond(this::mockServerResponse);

        // Given
        KeycloakImport keycloakImport = keycloakImportProvider
                .readFromPath(mockServerUrl(userInfo) + "/import-single/0_create_realm.json");

        // When
        Set<String> importedFileNames = keycloakImport.getRealmImports().keySet();

        // Then
        assertThat(importedFileNames, hasSize(1));
        assertThat(importedFileNames, contains(
                matchesPattern(".+/0_create_realm.+\\.json$")
        ));
    }

    @Test
    void shouldReadRemoteFilesFromZipArchiveUsingBasicAuth() {
        String userInfo = "user:password";

        client.when(
                request().withHeaders(header("Authorization", "Basic dXNlcjpwYXNzd29yZA=="))
        ).respond(this::mockServerResponse);

        // Given
        KeycloakImport keycloakImport = keycloakImportProvider
                .readFromPath(mockServerUrl(userInfo) + "/import-zip/realm-import.zip");

        // When
        Set<String> importedFileNames = keycloakImport.getRealmImports().keySet();

        // Then
        assertThat(importedFileNames, hasSize(6));
        assertThat(importedFileNames, contains(
                matchesPattern(".+/0_create_realm.+\\.json$"),
                matchesPattern(".+/1_update_realm.+\\.json$"),
                matchesPattern(".+/2_update_realm.+\\.json$"),
                matchesPattern(".+/3_update_realm.+\\.json$"),
                matchesPattern(".+/4_update_realm.+\\.json$"),
                matchesPattern(".+/5_update_realm.+\\.json$")
        ));
    }

    private String mockServerUrl() {
        return mockServerUrl(null);
    }

    private String mockServerUrl(String userInfo) {
        URIBuilder builder = new URIBuilder();
        builder.setScheme("http");
        builder.setHost(client.remoteAddress().getAddress().getHostAddress());
        builder.setPort(client.getPort());
        builder.setUserInfo(userInfo);
        return builder.toString();
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
}
