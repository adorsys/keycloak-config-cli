package de.adorsys.keycloak.config;

import de.adorsys.keycloak.config.configuration.TestConfiguration;
import de.adorsys.keycloak.config.model.KeycloakImport;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.service.KeycloakImportProvider;
import de.adorsys.keycloak.config.service.KeycloakProvider;
import de.adorsys.keycloak.config.service.RealmImportService;
import de.adorsys.keycloak.config.util.ResourceLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfiguration.class},
        initializers = {ConfigFileApplicationContextInitializer.class})
@ActiveProfiles("IT")
@DirtiesContext
public class ImportExportedRealmIT {
    private static final Map<String, String> EXPECTED_CHECKSUMS = new HashMap<>();
    private static final String REALM_NAME = "master";

    static {
        EXPECTED_CHECKSUMS.put("8.0.1", "83563e05222431f51654e7e4fe6b87e696aec8a43614a10197b327d6a653e51e7b2517f7c289fb90a7d034e1b9617bbd7b4a4ff027378802f2cc716d0e290c64");
        EXPECTED_CHECKSUMS.put("9.0.0", "9f9eff8a6cd78cf98603ff2d8dfec9ce2b909ff45e58e952d4c39b16aa16ecd2aa136dba269cae678b5f70a641244c43969e597060c1ff502b8bdd2247ac0461");
    }

    @Autowired
    RealmImportService realmImportService;
    @Autowired
    KeycloakImportProvider keycloakImportProvider;
    @Autowired
    KeycloakProvider keycloakProvider;
    private KeycloakImport keycloakImport;
    private String keycloakVersion;

    @BeforeEach
    public void setup() throws IOException {
        keycloakVersion = readKeycloakVersion();
        File configsFolder = ResourceLoader.loadResource("import-files/exported-realm/" + keycloakVersion);
        this.keycloakImport = keycloakImportProvider.readRealmImportsFromDirectory(configsFolder);
    }

    @AfterEach
    public void cleanup() {
        keycloakProvider.close();
    }

    @Test
    public void shouldReadImports() {
        assertThat(keycloakImport, is(not(nullValue())));
    }

    @Test
    public void integrationTests() {
        shouldImportExportedRealm();
    }

    private void shouldImportExportedRealm() {
        doImport("master-realm.json");

        RealmRepresentation updatedRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));
        assertThat(updatedRealm.getLoginTheme(), is(nullValue()));
        assertThat(
                updatedRealm.getAttributes().get("de.adorsys.keycloak.config.import-checksum"),
                is(expectedImportFileChecksum(keycloakVersion))
        );
    }

    private void doImport(String realmImport) {
        RealmImport foundImport = getImport(realmImport);
        realmImportService.doImport(foundImport);
    }

    private RealmImport getImport(String importName) {
        Map<String, RealmImport> realmImports = keycloakImport.getRealmImports();

        return realmImports.entrySet()
                .stream()
                .filter(e -> e.getKey().equals(importName))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private String readKeycloakVersion() throws IOException {
        Properties properties = new Properties();
        properties.load(readMavenPropertiesFile());

        return properties.getProperty("keycloak.version");
    }

    private InputStream readMavenPropertiesFile() throws FileNotFoundException {
        File targetFile = ResourceLoader.loadTargetFile("maven.properties");
        return new FileInputStream(targetFile);
    }

    private String expectedImportFileChecksum(String keycloakVersion) {
        return EXPECTED_CHECKSUMS.get(keycloakVersion);
    }
}
