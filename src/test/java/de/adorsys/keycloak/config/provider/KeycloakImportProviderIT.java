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
import de.adorsys.keycloak.config.model.KeycloakImport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Predicate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

class KeycloakImportProviderIT extends AbstractImportTest {

    @Container
    private static final NginxContainer<?> nginx;

    static {
        nginx = new NginxContainer<>("nginx:stable")
                .withClasspathResourceMapping("/import-files/import-remote/nginx.conf", "/etc/nginx/conf.d/nginx.conf",
                        BindMode.READ_ONLY)
                .withClasspathResourceMapping("/import-files/import-remote/.htpasswd", "/etc/apache2/.htpasswd",
                        BindMode.READ_ONLY)
                .withClasspathResourceMapping("/import-files/import-remote/data/", "/usr/share/nginx/html/test/",
                        BindMode.READ_ONLY)
                .waitingFor(new HttpWaitStrategy());
        nginx.start();
    }

    @Test
    void shouldReadLocalFile() {
        KeycloakImport keycloakImport = keycloakImportProvider
                .readFromPath("classpath:import-files/import-single/0_create_realm.json");
        assertThat(keycloakImport.getRealmImports().keySet(), contains(
                "0_create_realm.json"
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
        assertThat(keycloakImport.getRealmImports().keySet(), contains(tempFilePath.getFileName().toString()));
    }

    @Test
    void shouldReadLocalFilesFromDirectorySorted() {
        KeycloakImport keycloakImport = keycloakImportProvider.readFromPath("classpath:import-files/import-sorted/");
        assertThat(keycloakImport.getRealmImports().keySet(), contains(
                "0_create_realm.json",
                "1_update_realm.json",
                "2_update_realm.json",
                "3_update_realm.json",
                "4_update_realm.json",
                "5_update_realm.json",
                "6_update_realm.json",
                "7_update_realm.json",
                "8_update_realm.json",
                "9_update_realm.json"
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
        Assertions.assertEquals(6, importedFileNames.size());
        Predicate<String> namePredicate = s -> s.contains("_realm") && s.endsWith(".json");
        Assertions.assertTrue(importedFileNames.stream().allMatch(namePredicate));
    }

    @Test
    void shouldReadRemoteFile() {
        // Given
        KeycloakImport keycloakImport = keycloakImportProvider.readFromPath(nginxUrl() + "/test/0_create_realm.json");

        // When
        Set<String> importedFileNames = keycloakImport.getRealmImports().keySet();

        // Then
        Assertions.assertEquals(1, importedFileNames.size());
        Predicate<String> namePredicate = s -> s.contains("0_create_realm") && s.endsWith(".json");
        Assertions.assertTrue(importedFileNames.stream().allMatch(namePredicate));
    }

    @Test
    void shouldReadRemoteFilesFromZipArchive() {
        // Given
        KeycloakImport keycloakImport = keycloakImportProvider.readFromPath(nginxUrl() + "/test/realm-import.zip");

        // When
        Set<String> importedFileNames = keycloakImport.getRealmImports().keySet();

        // Then
        Assertions.assertEquals(6, importedFileNames.size());
        Predicate<String> namePredicate = s -> s.contains("_realm") && s.endsWith(".json");
        Assertions.assertTrue(importedFileNames.stream().allMatch(namePredicate));
    }

    @Test
    void shouldReadRemoteFileUsingBasicAuth() {
        // Given
        KeycloakImport keycloakImport = keycloakImportProvider
                .readFromPath(nginxUrl("user:password") + "/test/auth/0_create_realm.json");

        // When
        Set<String> importedFileNames = keycloakImport.getRealmImports().keySet();

        // Then
        Assertions.assertEquals(1, importedFileNames.size());
        Predicate<String> namePredicate = s -> s.contains("0_create_realm") && s.endsWith(".json");
        Assertions.assertTrue(importedFileNames.stream().allMatch(namePredicate));
    }

    @Test
    void shouldReadRemoteFilesFromZipArchiveUsingBasicAuth() {
        // Given
        KeycloakImport keycloakImport = keycloakImportProvider
                .readFromPath(nginxUrl("user:password") + "/test/auth/realm-import.zip");

        // When
        Set<String> importedFileNames = keycloakImport.getRealmImports().keySet();

        // Then
        Assertions.assertEquals(6, importedFileNames.size());
        Predicate<String> namePredicate = s -> s.contains("_realm") && s.endsWith(".json");
        Assertions.assertTrue(importedFileNames.stream().allMatch(namePredicate));
    }

    private String nginxUrl() {
        return nginxUrl(null);
    }

    private String nginxUrl(String userInfo) {
        StringBuilder builder = new StringBuilder();
        builder.append("http://");

        if (StringUtils.isNotBlank(userInfo)) {
            builder.append(userInfo)
                    .append("@");
        }

        builder.append(nginx.getHost())
                .append(":")
                .append(nginx.getFirstMappedPort());
        return builder.toString();
    }
}
