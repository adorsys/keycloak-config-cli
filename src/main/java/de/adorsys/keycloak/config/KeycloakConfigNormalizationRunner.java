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
import de.adorsys.keycloak.config.properties.NormalizationKeycloakConfigProperties;
import de.adorsys.keycloak.config.provider.KeycloakExportProvider;
import de.adorsys.keycloak.config.service.normalize.RealmNormalizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import static de.adorsys.keycloak.config.properties.NormalizationConfigProperties.OutputFormat.YAML;

@Component
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "NORMALIZE")
@EnableConfigurationProperties({NormalizationConfigProperties.class, NormalizationKeycloakConfigProperties.class})
public class KeycloakConfigNormalizationRunner implements CommandLineRunner, ExitCodeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakConfigNormalizationRunner.class);
    private static final long START_TIME = System.currentTimeMillis();

    private final RealmNormalizationService normalizationService;
    private final KeycloakExportProvider exportProvider;
    private final NormalizationConfigProperties normalizationConfigProperties;
    private final YAMLMapper yamlMapper;
    private final ObjectMapper objectMapper;
    private int exitCode;

    @Autowired
    public KeycloakConfigNormalizationRunner(RealmNormalizationService normalizationService,
                                             KeycloakExportProvider exportProvider,
                                             NormalizationConfigProperties normalizationConfigProperties,
                                             YAMLMapper yamlMapper,
                                             ObjectMapper objectMapper) {
        this.normalizationService = normalizationService;
        this.exportProvider = exportProvider;
        this.normalizationConfigProperties = normalizationConfigProperties;
        this.yamlMapper = yamlMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            var outputLocation = Paths.get(normalizationConfigProperties.getFiles().getOutputDirectory());
            if (!Files.exists(outputLocation)) {
                logger.info("Creating output directory '{}'", outputLocation);
                Files.createDirectories(outputLocation);
            }
            if (!Files.isDirectory(outputLocation)) {
                logger.error("Output location '{}' is not a directory. Aborting", outputLocation);
                exitCode = 1;
                return;
            }

            for (var exportLocations : exportProvider.readFromLocations().values()) {
                for (var export : exportLocations.entrySet()) {
                    logger.info("Normalizing file '{}'", export.getKey());
                    for (var realm : export.getValue()) {
                        var normalizedRealm = normalizationService.normalizeRealm(realm);
                        var suffix = normalizationConfigProperties.getOutputFormat() == YAML ? "yaml" : "json";
                        var outputFile = outputLocation.resolve(String.format("%s.%s", normalizedRealm.getRealm(), suffix));
                        try (var os = new FileOutputStream(outputFile.toFile())) {
                            if (normalizationConfigProperties.getOutputFormat() == YAML) {
                                yamlMapper.writeValue(os, normalizedRealm);
                            } else {
                                objectMapper.writeValue(os, normalizedRealm);
                            }
                        }
                    }
                }
            }
        } catch (NullPointerException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage());

            exitCode = 1;

            if (logger.isDebugEnabled()) {
                throw e;
            }
        } finally {
            long totalTime = System.currentTimeMillis() - START_TIME;
            String formattedTime = new SimpleDateFormat("mm:ss.SSS").format(new Date(totalTime));
            logger.info("keycloak-config-cli running in {}.", formattedTime);
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
