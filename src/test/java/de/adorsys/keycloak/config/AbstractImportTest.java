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

package de.adorsys.keycloak.config;

import de.adorsys.keycloak.config.configuration.TestConfiguration;
import de.adorsys.keycloak.config.extensions.ContainerLogsExtension;
import de.adorsys.keycloak.config.extensions.GithubActionsExtension;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.provider.KeycloakImportProvider;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.service.RealmImportService;
import de.adorsys.keycloak.config.test.util.KeycloakAuthentication;
import de.adorsys.keycloak.config.test.util.KeycloakRepository;
import de.adorsys.keycloak.config.util.VersionUtil;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;

@ExtendWith(SpringExtension.class)
@ExtendWith(GithubActionsExtension.class)
@ExtendWith(ContainerLogsExtension.class)
@ContextConfiguration(
        classes = {TestConfiguration.class},
        initializers = {ConfigDataApplicationContextInitializer.class}
)
@ActiveProfiles("IT")
@TestMethodOrder(OrderAnnotation.class)
//@ClassOrderer(ClassOrderer.OrderAnnotation)
@Timeout(value = 30, unit = SECONDS)
abstract public class AbstractImportTest {
    public static final ToStringConsumer KEYCLOAK_CONTAINER_LOGS = new ToStringConsumer();

    @Container
    public static final GenericContainer<?> KEYCLOAK_CONTAINER;

    protected static final String KEYCLOAK_VERSION = System.getProperty("keycloak.version");
    protected static final String KEYCLOAK_IMAGE = System.getProperty("keycloak.dockerImage", "quay.io/keycloak/keycloak");
    protected static final String KEYCLOAK_LOG_LEVEL = System.getProperty("keycloak.loglevel", "INFO");

    static {
        KEYCLOAK_CONTAINER = new GenericContainer<>(DockerImageName.parse(KEYCLOAK_IMAGE + ":" + KEYCLOAK_VERSION))
                .withExposedPorts(8080)
                .withEnv("KEYCLOAK_USER", "admin")
                .withEnv("KEYCLOAK_PASSWORD", "admin123")
                .withEnv("KEYCLOAK_LOGLEVEL", KEYCLOAK_LOG_LEVEL)
                .withEnv("ROOT_LOGLEVEL", "ERROR")
                // keycloak-x
                .withEnv("KEYCLOAK_ADMIN", "admin")
                .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin123")
                .withEnv("QUARKUS_PROFILE", "dev")
                .withExtraHost("host.docker.internal", "host-gateway")
                .waitingFor(Wait.forHttp("/"))
                .withStartupTimeout(Duration.ofSeconds(300));

        boolean isQuarkusDistribution = (VersionUtil.ge(KEYCLOAK_VERSION, "17") && !KEYCLOAK_IMAGE.contains("legacy"))
                || KEYCLOAK_IMAGE.contains("keycloak-x");

        if (isQuarkusDistribution) {
            KEYCLOAK_CONTAINER.setCommand("start-dev");
        }

        if (System.getProperties().getOrDefault("skipContainerStart", "false").equals("false")) {
            KEYCLOAK_CONTAINER.start();
            KEYCLOAK_CONTAINER.followOutput(KEYCLOAK_CONTAINER_LOGS);

            // KEYCLOAK_CONTAINER.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger("\uD83D\uDC33 [" + KEYCLOAK_CONTAINER.getDockerImageName() + "]")));
            System.setProperty("keycloak.user", KEYCLOAK_CONTAINER.getEnvMap().get("KEYCLOAK_USER"));
            System.setProperty("keycloak.password", KEYCLOAK_CONTAINER.getEnvMap().get("KEYCLOAK_PASSWORD"));
            System.setProperty("keycloak.baseUrl", String.format(
                    "http://%s:%d", KEYCLOAK_CONTAINER.getContainerIpAddress(), KEYCLOAK_CONTAINER.getMappedPort(8080)
            ));

            if (isQuarkusDistribution) {
                System.setProperty("keycloak.url", System.getProperty("keycloak.baseUrl"));
            } else {
                System.setProperty("keycloak.url", System.getProperty("keycloak.baseUrl") + "/auth/");
            }
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

    public String resourcePath;

    public void doImport(String fileName) throws IOException {
        doImport(fileName, realmImportService);
    }

    public void doImport(String fileName, RealmImportService _realmImportService) throws IOException {
        List<RealmImport> realmImports = getImport(fileName);

        for (RealmImport realmImport : realmImports) {
            _realmImportService.doImport(realmImport);
        }
    }

    public RealmImport getFirstImport(String fileName) throws IOException {
        return getImport(fileName).get(0);
    }

    public List<RealmImport> getImport(String fileName) throws IOException {
        File realmImportFile = new ClassPathResource(this.resourcePath + '/' + fileName).getFile();

        return keycloakImportProvider
                .readRealmImportFromFile(realmImportFile)
                .getRealmImports()
                .get(realmImportFile.getAbsolutePath());
    }
}
