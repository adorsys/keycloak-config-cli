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
import java.net.URL;
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

    KeycloakImport keycloakImport;

    String keycloakVersion;

    @Before
    public void setup() throws Exception {
        keycloakVersion = readKeycloakVersion();
        File configsFolder = ResourceLoader.loadResource("import-files/exported-realm/" + keycloakVersion);
        this.keycloakImport = keycloakImportProvider.readRealmImportsFromDirectory(configsFolder);
    }

    @After
    public void cleanup() throws Exception {
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

    private void shouldImportExportedRealm() throws Exception {
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
        EXPECTED_CHECKSUMS.put("7.0.0", "5291e515f8feae914bb3910902d3e3c18e3e1c127e7e43bf53e2eee459b9e7c5664141b3fce0a7a46ac83d876304df906943d944f24e7760b2bb253fc9001405");
        EXPECTED_CHECKSUMS.put("6.0.1", "8d0b874ecd38b3644dedad57113433049d5a8d97ca8832a9da76d5dd4c60eab163cfbd377612566cf166aa3cb1905fc60bf0203bd1c50447a7d09a162d385eab");
        EXPECTED_CHECKSUMS.put("5.0.0", "fe9646358e7d2f4b5e6a4b43b4177747868fa7b5e125731fcbfa9544d4faadf0068c2facc5d8ac508fa83d5666f2039055e74e032f1d825b3b4e6bcefbb331a3");
        EXPECTED_CHECKSUMS.put("4.8.3.Final", "9794ab71f998edf94fe50a622331f41d4cda10887df9402a647338578615b8ccfdae10c566b83914e799146e548ffbf207eb29b0cd90c19599510c179d6d3b69");
        EXPECTED_CHECKSUMS.put("4.7.0.Final", "0382fd2be6e3d06e88e7e2d672bc36c06cc0c802c0e71074b200d082fb5f3ab4d3a883d5b49712a297898945116dc62ce5356baf1952bfb520c8485f8eb419ce");
        EXPECTED_CHECKSUMS.put("4.6.0.Final", "22a533278371606695abb1372939f14a1420aad31863097b2e6e74549fb027ab16c24b6eb33661c0afa336dfa75f610fc2d27d9c221506063567eb32868849fa");
        EXPECTED_CHECKSUMS.put("4.5.0.Final", "36c55d5b739b9447edbe94c4b492c66e93a1e2dadd27a618d2ac86ec4af63a41330a631625bf43ae9bc3da6a9cbdbb9a412ee19ea063f72a87a136583a77d72d");
        EXPECTED_CHECKSUMS.put("4.4.0.Final", "4f20c5f3757556f9a82fd674c8fb5c09171e6f3a734e185f37c2b6efee8e602e3a9658a50b4c62c47f2214aa87a87c7a110e6d5bad330e2b9885e2f6fe7885d8");
    }
}
