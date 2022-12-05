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

import de.adorsys.keycloak.config.properties.NormalizationConfigProperties;
import de.adorsys.keycloak.config.properties.NormalizationKeycloakConfigProperties;
import de.adorsys.keycloak.config.provider.KeycloakExportProvider;
import de.adorsys.keycloak.config.service.normalize.RealmNormalizationService;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "NORMALIZE")
@EnableConfigurationProperties({NormalizationConfigProperties.class, NormalizationKeycloakConfigProperties.class})
public class KeycloakConfigNormalizationRunner implements CommandLineRunner, ExitCodeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakConfigNormalizationRunner.class);
    private static final long START_TIME = System.currentTimeMillis();

    private final RealmNormalizationService normalizationService;
    private final KeycloakExportProvider exportProvider;

    private int exitCode;

    @Autowired
    public KeycloakConfigNormalizationRunner(RealmNormalizationService normalizationService, KeycloakExportProvider exportProvider) {
        this.normalizationService = normalizationService;
        this.exportProvider = exportProvider;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            for (Map<String, List<RealmRepresentation>> exportLocations : exportProvider.readFromLocations().values()) {
                for (Map.Entry<String, List<RealmRepresentation>> export : exportLocations.entrySet()) {
                    logger.info("Normalizing file '{}'", export.getKey());
                    for (RealmRepresentation realm : export.getValue()) {
                        normalizationService.normalize(realm);
                    }
                }
            }
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
