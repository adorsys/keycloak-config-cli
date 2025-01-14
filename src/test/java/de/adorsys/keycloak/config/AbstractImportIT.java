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

import de.adorsys.keycloak.config.extensions.ContainerLogsExtension;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.util.VersionUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(ContainerLogsExtension.class)
abstract public class AbstractImportIT extends AbstractImportTest {
    public static final ToStringConsumer KEYCLOAK_CONTAINER_LOGS = new ToStringConsumer();

    @Container
    public static final GenericContainer<?> KEYCLOAK_CONTAINER;

    @Autowired
    public KeycloakProvider keycloakProvider;

    protected static final String KEYCLOAK_VERSION = System.getProperty("keycloak.version");
    protected static final String KEYCLOAK_IMAGE = System.getProperty("keycloak.dockerImage", "quay.io/keycloak/keycloak");
    protected static final String KEYCLOAK_TAG_SUFFIX = System.getProperty("keycloak.dockerTagSuffix", "");
    protected static final String KEYCLOAK_LOG_LEVEL = System.getProperty("keycloak.loglevel", "INFO");

    static {
        KEYCLOAK_CONTAINER = new GenericContainer<>(DockerImageName.parse(KEYCLOAK_IMAGE + ":" + KEYCLOAK_VERSION + KEYCLOAK_TAG_SUFFIX))
                .withExposedPorts(8080)
                .withEnv("KEYCLOAK_USER", "admin")
                .withEnv("KEYCLOAK_PASSWORD", "admin123")
                .withEnv("KEYCLOAK_LOGLEVEL", KEYCLOAK_LOG_LEVEL)
                .withEnv("ROOT_LOGLEVEL", "ERROR")
                // keycloak-x
                .withEnv("KEYCLOAK_ADMIN", "admin")
                .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin123")
                .withEnv("QUARKUS_PROFILE", "dev")
                .withEnv("KC_LOG_LEVEL", KEYCLOAK_LOG_LEVEL)
                .withExtraHost("host.docker.internal", "host-gateway")
                .waitingFor(Wait.forHttp("/"))
                .withStartupTimeout(Duration.ofSeconds(300));

        boolean isLegacyDistribution = KEYCLOAK_CONTAINER.getDockerImageName().contains("legacy");

        List<String> command = new ArrayList<>();

        if (isLegacyDistribution) {
            command.add("-c");
            command.add("standalone.xml");
            command.add("-Dkeycloak.profile.feature.admin_fine_grained_authz=enabled");
            command.add("-Dkeycloak.profile.feature.declarative_user_profile=enabled");
        } else {
            KEYCLOAK_CONTAINER.setCommand("start-dev");
            command.add("start-dev");
            command.add("--features");

            StringBuilder featuresBuilder =
                    new StringBuilder("admin-fine-grained-authz,client-policies,client-secret-rotation");

            if (VersionUtil.lt(KEYCLOAK_VERSION, "24")) {
                featuresBuilder.append(",declarative-user-profile");
            }

            command.add(featuresBuilder.toString());
        }

        if (System.getProperties().getOrDefault("skipContainerStart", "false").equals("false")) {
            KEYCLOAK_CONTAINER.setCommand(command.toArray(new String[0]));
            KEYCLOAK_CONTAINER.start();
            KEYCLOAK_CONTAINER.followOutput(KEYCLOAK_CONTAINER_LOGS);

            // KEYCLOAK_CONTAINER.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger("\uD83D\uDC33 [" + KEYCLOAK_CONTAINER.getDockerImageName() + "]")));
            System.setProperty("keycloak.user", KEYCLOAK_CONTAINER.getEnvMap().get("KEYCLOAK_USER"));
            System.setProperty("keycloak.password", KEYCLOAK_CONTAINER.getEnvMap().get("KEYCLOAK_PASSWORD"));
            System.setProperty("keycloak.baseUrl", String.format(
                    "http://%s:%d", KEYCLOAK_CONTAINER.getContainerIpAddress(), KEYCLOAK_CONTAINER.getMappedPort(8080)
            ));

            if (isLegacyDistribution) {
                System.setProperty("keycloak.url", System.getProperty("keycloak.baseUrl") + "/auth/");
            } else {
                System.setProperty("keycloak.url", System.getProperty("keycloak.baseUrl"));
            }
        }
    }
}
