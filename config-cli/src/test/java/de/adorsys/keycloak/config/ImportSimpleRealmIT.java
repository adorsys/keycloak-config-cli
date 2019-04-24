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
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { TestConfiguration.class },
                      initializers = { ConfigFileApplicationContextInitializer.class })
@ActiveProfiles("IT")
@DirtiesContext
public class ImportSimpleRealmIT {
    private static final String REALM_NAME = "simple";

    @Autowired
    RealmImportService realmImportService;

    @Autowired
    KeycloakImportProvider keycloakImportProvider;

    @Autowired
    KeycloakProvider keycloakProvider;

    KeycloakImport keycloakImport;

    @Before
    public void setup() throws Exception {
        File configsFolder = ResourceLoader.loadResource("import-files/simple-realm");
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
        shouldCreateSimpleRealm();
        shouldNotUpdateSimpleRealm();
        shouldUpdateSimpleRealm();
        shouldCreateSimpleRealmWithLoginTheme();
    }

    private void shouldCreateSimpleRealm() throws Exception {
        doImport("0_create_simple-realm.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));
        assertThat(createdRealm.getLoginTheme(), is(nullValue()));
        assertThat(
                createdRealm.getAttributes().get("de.adorsys.keycloak.config.import-checksum"),
                is("3796660d3087308ee757d9d86e14dd6e6fe4bfd66cc1435851ff2f5c6fa432c5991b3042f95c4f11238e1dfb81676ae2a00bde0bbad17c1f66ef530841df2e66")
        );
    }

    private void shouldNotUpdateSimpleRealm() throws Exception {
        doImport("0.1_update_simple-realm_with_same_config.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));
        assertThat(createdRealm.getLoginTheme(), is(nullValue()));
        assertThat(
                createdRealm.getAttributes().get("de.adorsys.keycloak.config.import-checksum"),
                is("3796660d3087308ee757d9d86e14dd6e6fe4bfd66cc1435851ff2f5c6fa432c5991b3042f95c4f11238e1dfb81676ae2a00bde0bbad17c1f66ef530841df2e66")
        );
    }

    private void shouldUpdateSimpleRealm() throws Exception {
        doImport("1_update_login-theme_to_simple-realm.json");

        RealmRepresentation updatedRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));
        assertThat(updatedRealm.getLoginTheme(), is("moped"));
        assertThat(
                updatedRealm.getAttributes().get("de.adorsys.keycloak.config.import-checksum"),
                is("d3913c179bf6d1ed1afbc2580207f3d7d78efed3ef13f9e12dea3afd5c28e9b307dd930fecfcc100038e540d1e23dc5b5c74d0321a410c7ba330e9dbf9d4211c")
        );
    }

    private void shouldCreateSimpleRealmWithLoginTheme() throws Exception {
        doImport("2_create_simple-realm_with_login-theme.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm("simpleWithLoginTheme").toRepresentation();

        assertThat(createdRealm.getRealm(), is("simpleWithLoginTheme"));
        assertThat(createdRealm.isEnabled(), is(true));
        assertThat(createdRealm.getLoginTheme(), is("moped"));
        assertThat(
                createdRealm.getAttributes().get("de.adorsys.keycloak.config.import-checksum"),
                is("5d75698bacb06b1779e2b303069266664d63eec9c52038e2e6ae930bfc6e33ec7e7493b067ee0253e73a6b19cdf8905fd75cc6bb394ca333d32c784063aa65c8")
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
}
