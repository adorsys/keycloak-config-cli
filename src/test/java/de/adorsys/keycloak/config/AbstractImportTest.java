package de.adorsys.keycloak.config;

import de.adorsys.keycloak.config.configuration.TestConfiguration;
import de.adorsys.keycloak.config.model.KeycloakImport;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.service.KeycloakImportProvider;
import de.adorsys.keycloak.config.service.KeycloakProvider;
import de.adorsys.keycloak.config.service.RealmImportService;
import de.adorsys.keycloak.config.util.KeycloakAuthentication;
import de.adorsys.keycloak.config.util.KeycloakRepository;
import de.adorsys.keycloak.config.util.ResourceLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;

import java.io.File;
import java.time.Duration;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {TestConfiguration.class},
        initializers = {ConfigFileApplicationContextInitializer.class}
)
@ActiveProfiles("IT")
@DirtiesContext
abstract class AbstractImportTest {
    @Container
    static final GenericContainer<?> KEYCLOAK_CONTAINER;

    static {
        KEYCLOAK_CONTAINER = new GenericContainer<>("jboss/keycloak:" + System.getProperty("keycloak.version"))
                .withExposedPorts(8080)
                .withEnv("KEYCLOAK_USER", "admin")
                .withEnv("KEYCLOAK_PASSWORD", "admin123")
                .withEnv("KEYCLOAK_LOGLEVEL", "WARN")
                .withEnv("ROOT_LOGLEVEL", "ERROR")
                .withCommand("-c", "standalone.xml")
                .waitingFor(Wait.forHttp("/auth/"))
                .withStartupTimeout(Duration.ofSeconds(300));

        if (System.getProperty("skipContainerStart") == null || !System.getProperty("skipContainerStart").equals("false")) {
            KEYCLOAK_CONTAINER.start();

            // KEYCLOAK_CONTAINER.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger("\uD83D\uDC33 [" + KEYCLOAK_CONTAINER.getDockerImageName() + "]")));
            System.setProperty("keycloak.user", KEYCLOAK_CONTAINER.getEnvMap().get("KEYCLOAK_USER"));
            System.setProperty("keycloak.password", KEYCLOAK_CONTAINER.getEnvMap().get("KEYCLOAK_PASSWORD"));
            System.setProperty("keycloak.url", "http://" + KEYCLOAK_CONTAINER.getContainerIpAddress() + ":" + KEYCLOAK_CONTAINER.getMappedPort(8080));
        }
    }

    @Autowired
    RealmImportService realmImportService;
    @Autowired
    KeycloakImportProvider keycloakImportProvider;
    @Autowired
    KeycloakProvider keycloakProvider;
    @Autowired
    KeycloakRepository keycloakRepository;
    @Autowired
    KeycloakAuthentication keycloakAuthentication;
    KeycloakImport keycloakImport;
    String resourcePath;

    @BeforeEach
    public void setup() {
        File configsFolder = ResourceLoader.loadResource(this.resourcePath);
        this.keycloakImport = keycloakImportProvider.readRealmImportsFromDirectory(configsFolder);
    }

    @Test
    public void shouldReadImports() {
        assertThat(keycloakImport, is(not(nullValue())));
    }

    @AfterEach
    public void cleanup() {
        keycloakProvider.close();
    }

    void doImport(String realmImport) {
        RealmImport foundImport = getImport(realmImport);
        realmImportService.doImport(foundImport);
    }

    RealmImport getImport(String importName) {
        Map<String, RealmImport> realmImports = keycloakImport.getRealmImports();

        return realmImports.entrySet()
                .stream()
                .filter(e -> e.getKey().equals(importName))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
