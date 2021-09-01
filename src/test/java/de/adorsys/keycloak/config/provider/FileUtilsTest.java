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

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.extensions.GithubActionsExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.function.Predicate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(GithubActionsExtension.class)
class FileUtilsTest {
    @Test
    void shouldThrowOnNew() {
        assertThrows(IllegalStateException.class, FileUtils::new);
    }

    @Test
    void shouldHandleTempFileCorrect() throws Exception {
        // Given
        String test = "Hello, this is awesome ...";
        InputStream stream = new ByteArrayInputStream(test.getBytes(StandardCharsets.UTF_8));

        // When
        File tempFile = FileUtils.createTempFile("demo.json", stream);

        // Then
        Assertions.assertNotNull(tempFile);
        String actual = new String(Files.readAllBytes(tempFile.toPath()));
        Assertions.assertEquals(actual, test);
        Assertions.assertTrue(tempFile.getName().endsWith(".json"));
        Assertions.assertTrue(tempFile.getName().startsWith("demo"));
    }

    @Test
    void shouldNotBeAffectedByZipSlip() throws Exception {
        // Given
        Resource zip = new ClassPathResource("/import-files/import-zip/zip-slip.zip");

        // When
        Collection<File> files = FileUtils.extractFile(zip.getFile());

        // Then
        Assertions.assertNotNull(files);
        Assertions.assertEquals(2, files.size());

        Predicate<File> goodNamePredicate = f -> f.getName().startsWith("good") && f.getName().endsWith(".txt");
        Assertions.assertTrue(files.stream().anyMatch(goodNamePredicate));

        Predicate<File> evilNamePredicate = f -> f.getName().startsWith("evil") && f.getName().endsWith(".txt");
        Assertions.assertTrue(files.stream().anyMatch(evilNamePredicate));
    }

    @Test
    void shouldThrowInvalidZip() throws Exception {
        // Given
        File zip = new ClassPathResource("/import-files/import-zip/invalid.zip").getFile();

        // When
        ImportProcessingException thrown = assertThrows(ImportProcessingException.class, () -> FileUtils.extractFile(zip));

        assertThat(thrown.getMessage(), matchesPattern("Unable to extract zip file .+"));
    }
}
