/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2020 adorsys GmbH & Co. KG @ https://adorsys.de
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

import de.adorsys.keycloak.config.configuration.TestConfiguration;
import de.adorsys.keycloak.config.exception.InvalidImportException;
import de.adorsys.keycloak.config.model.KeycloakImport;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.provider.KeycloakImportProvider;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.service.RealmImportService;
import de.adorsys.keycloak.config.test.util.KeycloakAuthentication;
import de.adorsys.keycloak.config.test.util.KeycloakRepository;
import de.adorsys.keycloak.config.test.util.KeycloakVersion;
import de.adorsys.keycloak.config.test.util.ResourceLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;

import java.io.File;
import java.time.Duration;
import java.util.Map;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {TestConfiguration.class},
        initializers = {ConfigDataApplicationContextInitializer.class}
)
@ActiveProfiles("IT")
@TestMethodOrder(OrderAnnotation.class)
abstract public class AbstractImportTest {
    @Container
    static final GenericContainer<?> KEYCLOAK_CONTAINER;

    static {
        KEYCLOAK_CONTAINER = new GenericContainer<>("jboss/keycloak:" + KeycloakVersion.get())
                .withExposedPorts(8080)
                .withEnv("KEYCLOAK_USER", "admin")
                .withEnv("KEYCLOAK_PASSWORD", "admin123")
                .withEnv("KEYCLOAK_LOGLEVEL", "WARN")
                .withEnv("ROOT_LOGLEVEL", "ERROR")
                .withCommand("-c", "standalone.xml")
                .waitingFor(Wait.forHttp("/auth/"))
                .withStartupTimeout(Duration.ofSeconds(300));

        if (System.getProperties().getOrDefault("skipContainerStart", "false").equals("false")) {
            KEYCLOAK_CONTAINER.start();

            // KEYCLOAK_CONTAINER.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger("\uD83D\uDC33 [" + KEYCLOAK_CONTAINER.getDockerImageName() + "]")));
            System.setProperty("keycloak.user", KEYCLOAK_CONTAINER.getEnvMap().get("KEYCLOAK_USER"));
            System.setProperty("keycloak.password", KEYCLOAK_CONTAINER.getEnvMap().get("KEYCLOAK_PASSWORD"));
            System.setProperty("keycloak.url", "http://" + KEYCLOAK_CONTAINER.getContainerIpAddress() + ":" + KEYCLOAK_CONTAINER.getMappedPort(8080));
        }
    }

    @Autowired
    public RealmImportService realmImportService;

    @Autowired
    public KeycloakImportProvider keycloakImportProvider;

    @Autowired
    public KeycloakProvider keycloakProvider;

    @Autowired
    public KeycloakRepository keycloakRepository;

    @Autowired
    public KeycloakAuthentication keycloakAuthentication;

    public KeycloakImport keycloakImport;

    public String resourcePath;

    @BeforeEach
    public void setup() {
        if (this.resourcePath != null) {
            File configsFolder = ResourceLoader.loadResource(this.resourcePath);
            this.keycloakImport = keycloakImportProvider.readRealmImportsFromDirectory(configsFolder);
        }
    }

    @AfterEach
    public void cleanup() {
        keycloakProvider.close();
    }

    public void doImport(String realmImport) {
        RealmImport foundImport = getImport(realmImport);
        realmImportService.doImport(foundImport);
    }

    public RealmImport getImport(String importName) {
        Map<String, RealmImport> realmImports = keycloakImport.getRealmImports();

        return realmImports.entrySet()
                .stream()
                .filter(e -> e.getKey().equals(importName))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new InvalidImportException("Cannot find '" + importName + "'"));
    }
}
