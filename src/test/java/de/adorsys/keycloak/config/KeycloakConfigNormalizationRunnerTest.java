/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2022 adorsys GmbH & Co. KG @ https://adorsys.com
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

package de.adorsys.keycloak.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import de.adorsys.keycloak.config.properties.NormalizationConfigProperties;
import de.adorsys.keycloak.config.provider.KeycloakExportProvider;
import de.adorsys.keycloak.config.service.normalize.RealmNormalizationService;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.adorsys.keycloak.config.properties.NormalizationConfigProperties.OutputFormat.JSON;
import static de.adorsys.keycloak.config.properties.NormalizationConfigProperties.OutputFormat.YAML;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KeycloakConfigNormalizationRunnerTest {

    @Test
    void run_shouldCreateOutputDirectoryAndWriteYaml() throws Exception {
        Path tempRoot = Files.createTempDirectory("kcc-normalize");
        Path outputDir = tempRoot.resolve("out");

        NormalizationConfigProperties.NormalizationFilesProperties filesProperties =
                new NormalizationConfigProperties.NormalizationFilesProperties(
                        List.of("classpath:dummy/*.yaml"),
                        List.of(),
                        false,
                        outputDir.toString()
                );
        NormalizationConfigProperties props = new NormalizationConfigProperties(filesProperties, YAML, null);

        RealmNormalizationService normalizationService = mock(RealmNormalizationService.class);
        KeycloakExportProvider exportProvider = mock(KeycloakExportProvider.class);

        RealmRepresentation exported = new RealmRepresentation();
        exported.setRealm("source");

        RealmRepresentation normalized = new RealmRepresentation();
        normalized.setRealm("normalized");

        when(normalizationService.normalizeRealm(exported)).thenReturn(normalized);

        Map<String, Map<String, List<RealmRepresentation>>> exports = new LinkedHashMap<>();
        exports.put("loc", Map.of("file", List.of(exported)));
        when(exportProvider.readFromLocations()).thenReturn(exports);

        KeycloakConfigNormalizationRunner runner = new KeycloakConfigNormalizationRunner(
                normalizationService,
                exportProvider,
                props,
                new YAMLMapper(),
                new ObjectMapper()
        );

        runner.run();

        assertTrue(Files.isDirectory(outputDir));
        assertTrue(Files.exists(outputDir.resolve("normalized.yaml")));
        assertThat(runner.getExitCode(), is(0));
    }

    @Test
    void run_shouldSetExitCodeWhenOutputLocationIsNotDirectory() throws Exception {
        Path tempRoot = Files.createTempDirectory("kcc-normalize");
        Path outputFile = tempRoot.resolve("out.txt");
        Files.writeString(outputFile, "x");

        NormalizationConfigProperties.NormalizationFilesProperties filesProperties =
                new NormalizationConfigProperties.NormalizationFilesProperties(
                        List.of("classpath:dummy/*.yaml"),
                        List.of(),
                        false,
                        outputFile.toString()
                );
        NormalizationConfigProperties props = new NormalizationConfigProperties(filesProperties, JSON, null);

        KeycloakConfigNormalizationRunner runner = new KeycloakConfigNormalizationRunner(
                mock(RealmNormalizationService.class),
                mock(KeycloakExportProvider.class),
                props,
                new YAMLMapper(),
                new ObjectMapper()
        );

        runner.run();

        assertThat(runner.getExitCode(), is(1));
    }
}
