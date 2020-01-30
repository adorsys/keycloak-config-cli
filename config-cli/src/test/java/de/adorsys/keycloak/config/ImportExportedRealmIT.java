package de.adorsys.keycloak.config;

import de.adorsys.keycloak.config.configuration.TestConfiguration;
import de.adorsys.keycloak.config.model.KeycloakImport;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.service.KeycloakImportProvider;
import de.adorsys.keycloak.config.service.KeycloakProvider;
import de.adorsys.keycloak.config.service.RealmImportService;
import de.adorsys.keycloak.config.util.ResourceLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { TestConfiguration.class },
                      initializers = { ConfigFileApplicationContextInitializer.class })
@ActiveProfiles("IT")
@DirtiesContext
public class ImportExportedRealmIT {
    private static final Map<String, String> EXPECTED_CHECKSUMS = new HashMap<>();
    private static final String REALM_NAME = "master";

    @Autowired
    RealmImportService realmImportService;

    @Autowired
    KeycloakImportProvider keycloakImportProvider;

    @Autowired
    KeycloakProvider keycloakProvider;

    private KeycloakImport keycloakImport;

    private String keycloakVersion;

    @Before
    public void setup() throws Exception {
        keycloakVersion = readKeycloakVersion();
        File configsFolder = ResourceLoader.loadResource("import-files/exported-realm/" + keycloakVersion);
        this.keycloakImport = keycloakImportProvider.readRealmImportsFromDirectory(configsFolder);
    }

    @After
    public void cleanup() {
        keycloakProvider.close();
    }

    @Test
    public void shouldReadImports() {
        assertThat(keycloakImport, is(not(nullValue())));
    }

    @Test
    public void integrationTests() throws Exception {
        shouldImportExportedRealm();
    }

    private void shouldImportExportedRealm() {
        doImport( "master-realm.json");

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
                .get();
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

    static {
        EXPECTED_CHECKSUMS.put("8.0.1", "aa438506ab38c595232ad9e6b6c5df0e8592482672e5e4785400af7fc02ad2a7aa9f559664bdb94df8667b43f382bf9355b6dd3def7b5acf2932a9138bd9ea06");
    }
}
