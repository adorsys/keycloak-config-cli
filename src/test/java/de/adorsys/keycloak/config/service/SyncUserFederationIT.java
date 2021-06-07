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

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.AbstractImportTest;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class SyncUserFederationIT extends AbstractImportTest {

    public static final ToStringConsumer LDAP_CONTAINER_LOGS = new ToStringConsumer();
    @Container
    public static final GenericContainer<?> LDAP_CONTAINER;
    private static final String REALM_NAME = "realmWithLdap";

    static {
        LDAP_CONTAINER = new GenericContainer<>(DockerImageName.parse("osixia/openldap" + ":" + "1.5.0"))
                .withExposedPorts(389, 636)
                .withEnv("LDAP_ORGANISATION", "test-suit")
                .withEnv("LDAP_ADMIN_PASSWORD", "admin123")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("import-files/user-federation/ldap-openldap-docker-init.ldif"),
                        "/container/service/slapd/assets/config/bootstrap/ldif/custom/ldap-openldap-docker-init.ldif"
                )

                .withNetwork(NETWORK)
                .withNetworkAliases("ldap")

                .withStartupTimeout(Duration.ofSeconds(300));

        if (System.getProperties().getOrDefault("skipContainerStart", "false").equals("false")) {
            LDAP_CONTAINER.start();
            LDAP_CONTAINER.followOutput(LDAP_CONTAINER_LOGS);

            System.setProperty("import.sync-user-federation", "true");
        }
    }

    public SyncUserFederationIT() {
        this.resourcePath = "import-files/user-federation";
    }

    @Test
    @Order(0)
    @Timeout(value = 10, unit = HOURS)
    void shouldCreateRealmWithUser() throws IOException {
//        File realmImportTemplateFile = new ClassPathResource(this.resourcePath + "/00_create_realm_with_federation.json").getFile();
//        File realmImportFile = new File(realmImportTemplateFile.getParent() + "/00_create_realm_with_federation.json");

//        StringBuilder stringBuilder = new StringBuilder();
//        Files.readAllLines(Paths.get(realmImportTemplateFile.getPath()), StandardCharsets.UTF_8).forEach(stringBuilder::append);
//        String format = String.format(
//                stringBuilder.toString(),
//                LDAP_CONTAINER.getContainerName(),
//                LDAP_CONTAINER.getMappedPort(389)
//        );
//
//        System.out.println(LDAP_CONTAINER.getHost());
//
//        if (!realmImportFile.exists() && !realmImportFile.createNewFile()) {
//            assertThat("Fail to create needed file.", false);
//        }
//        Files.write(Paths.get(realmImportFile.getPath()), format.getBytes(StandardCharsets.UTF_8));

        doImport("00_create_realm_with_federation.json");

        RealmRepresentation createdRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        UserRepresentation createdUser = keycloakRepository.getUser(REALM_NAME, "jbrown");
        assertThat(createdUser.getUsername(), is("jbrown"));
        assertThat(createdUser.getEmail(), is("jbrown@keycloak.org"));
        assertThat(createdUser.isEnabled(), is(true));
        assertThat(createdUser.getFirstName(), is("James"));
        assertThat(createdUser.getLastName(), is("Brown"));
    }
}
