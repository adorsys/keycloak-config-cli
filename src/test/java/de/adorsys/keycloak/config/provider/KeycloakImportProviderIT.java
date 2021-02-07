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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

class KeycloakImportProviderIT extends AbstractImportTest {

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
    void shouldReadLocalFilesFromDirectorySorted() throws IOException {
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
        KeycloakImport keycloakImport = keycloakImportProvider
                .readFromPath("classpath:import-files/import-zip/realm-import.zip");
        assertThat(keycloakImport.getRealmImports().keySet(), contains(
                "0_create_realm.json",
                "1_update_realm.json",
                "2_update_realm.json",
                "3_update_realm.json",
                "4_update_realm.json",
                "5_update_realm.json"
        ));
    }

    @Test
    void shouldReadRemoteFile() {
//        String nginxUrl = getNginxUrl();
//        KeycloakImport keycloakImport = keycloakImportProvider
//                .readFromPath(nginxUrl + "/import/0_create_realm.json");
//        assertThat(keycloakImport.getRealmImports().keySet(), contains(
//                "0_create_realm.json"
//        ));
    }

    @Test
    void shouldReadRemoteFilesFromZipArchive() {
//        KeycloakImport keycloakImport = keycloakImportProvider
//                .readFromPath("https://localhost.com/realm.json");
    }

    @Test
    void shouldReadRemoteFileUsingBasicAuth() {
//        KeycloakImport keycloakImport = keycloakImportProvider
//                .readFromPath("https://user:password@localhost.com/realm.json");
    }

    @Test
    void shouldReadRemoteFilesFromZipArchiveUsingBasicAuth() {
//        KeycloakImport keycloakImport = keycloakImportProvider
//                .readFromPath("https://user:passwordlocalhost.com/realms.zip");
    }
}
