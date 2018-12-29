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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.util.Map;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(
        classes = { TestConfiguration.class },
        initializers = { ConfigFileApplicationContextInitializer.class }
)
@ActiveProfiles("IT")
@DirtiesContext
public class ImportUsersIT {
    private static final String REALM_NAME = "realmWithUsers";

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

    @Before
    public void setup() throws Exception {
        File configsFolder = ResourceLoader.loadResource("import-files/users");
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
        shouldCreateRealmWithUser();
        shouldUpdateRealmWithAddingClientUser();
        shouldUpdateRealmWithChangedClientUserPassword();
    }

    private void shouldCreateRealmWithUser() throws Exception {
        doImport("0_create_realm_with_user.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        UserRepresentation createdUser = keycloakRepository.getUser(REALM_NAME, "myuser");

        assertThat(createdUser.getUsername(), is("myuser"));
        assertThat(createdUser.getEmail(), is("my@mail.de"));
        assertThat(createdUser.isEnabled(), is(true));
        assertThat(createdUser.getFirstName(), is("My firstname"));
        assertThat(createdUser.getLastName(), is("My lastname"));
    }

    private void shouldUpdateRealmWithAddingClientUser() throws Exception {
        doImport("1_update_realm_add_clientuser.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        UserRepresentation createdUser = keycloakRepository.getUser(REALM_NAME, "myclientuser");

        assertThat(createdUser.getUsername(), is("myclientuser"));
        assertThat(createdUser.getEmail(), is("myclientuser@mail.de"));
        assertThat(createdUser.isEnabled(), is(true));
        assertThat(createdUser.getFirstName(), is("My clientuser's firstname"));
        assertThat(createdUser.getLastName(), is("My clientuser's lastname"));

        // check if login with password is successful
        KeycloakAuthentication.AuthenticationToken token = keycloakAuthentication.login(
                REALM_NAME,
                "moped-client",
                "my-special-client-secret",
                "myclientuser",
                "myclientuser123"
        );

        assertThat(token.getAccessToken(), is(not(nullValue())));
        assertThat(token.getRefreshToken(), is(not(nullValue())));
        assertThat(token.getExpiresIn(), is(greaterThan(0)));
        assertThat(token.getRefreshExpiresIn(), is(greaterThan(0)));
        assertThat(token.getTokenType(), is("bearer"));
    }

    private void shouldUpdateRealmWithChangedClientUserPassword() throws Exception {
        doImport("2_update_realm_change_clientusers_password.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        UserRepresentation user = keycloakRepository.getUser(REALM_NAME, "myclientuser");

        assertThat(user.getUsername(), is("myclientuser"));
        assertThat(user.getEmail(), is("myclientuser@mail.de"));
        assertThat(user.isEnabled(), is(true));
        assertThat(user.getFirstName(), is("My clientuser's firstname"));
        assertThat(user.getLastName(), is("My clientuser's lastname"));

        // check if login with old password fails
        catchException(keycloakAuthentication).login(
                REALM_NAME,
                "moped-client",
                "my-special-client-secret",
                "myclientuser",
                "myclientuser123"
        );

        Assert.assertThat(caughtException(),
                allOf(
                        instanceOf(KeycloakAuthentication.AuthenticationException.class)
                )
        );

        // check if login with new password is successful
        KeycloakAuthentication.AuthenticationToken token = keycloakAuthentication.login(
                REALM_NAME,
                "moped-client",
                "my-special-client-secret",
                "myclientuser",
                "changedclientuser123"
        );

        assertThat(token.getAccessToken(), is(not(nullValue())));
        assertThat(token.getRefreshToken(), is(not(nullValue())));
        assertThat(token.getExpiresIn(), is(greaterThan(0)));
        assertThat(token.getRefreshExpiresIn(), is(greaterThan(0)));
        assertThat(token.getTokenType(), is("bearer"));
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
