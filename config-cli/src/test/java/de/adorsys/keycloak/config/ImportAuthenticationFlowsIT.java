package de.adorsys.keycloak.config;

import de.adorsys.keycloak.config.configuration.TestConfiguration;
import de.adorsys.keycloak.config.model.KeycloakImport;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.service.KeycloakImportProvider;
import de.adorsys.keycloak.config.service.RealmImportService;
import de.adorsys.keycloak.config.util.ResourceLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.core.Is.is;
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
public class ImportAuthenticationFlowsIT {
    
    @Autowired
    RealmImportService realmImportService;

    @Autowired
    KeycloakImportProvider keycloakImportProvider;

    @Autowired
    Keycloak keycloak;

    KeycloakImport keycloakImport;

    @Before
    public void setup() throws Exception {
        File configsFolder = ResourceLoader.loadResource("import-files/auth-flows");
        this.keycloakImport = keycloakImportProvider.readRealmImportsFromDirectory(configsFolder);
    }

    @Test
    public void shouldReadImports() {
        assertThat(keycloakImport, is(not(nullValue())));
    }

    @Test
    public void integrationTests() throws Exception {
        shouldCreateRealmWithFlows();
    }

    private void shouldCreateRealmWithFlows() throws Exception {
        doImport("create_realm_with_flows.json");

        RealmRepresentation createdRealm = keycloak.realm("realmWithFlow").partialExport(true, true);

        assertThat(createdRealm.getRealm(), is("realmWithFlow"));
        assertThat(createdRealm.isEnabled(), is(true));

        List<AuthenticationFlowRepresentation> authenticationFlows = createdRealm.getAuthenticationFlows();
        Optional<AuthenticationFlowRepresentation> maybeImportedFlow = authenticationFlows.stream().filter(f -> f.getAlias().equals("my docker auth")).findFirst();
        assertThat(maybeImportedFlow.isPresent(), is(true));

        AuthenticationFlowRepresentation importedFlow = maybeImportedFlow.get();
        assertThat(importedFlow.getDescription(), is("My custom docker auth flow for testing"));
        assertThat(importedFlow.getProviderId(), is("basic-flow"));
        assertThat(importedFlow.isBuiltIn(), is(false));
        assertThat(importedFlow.isTopLevel(), is(true));

        List<AuthenticationExecutionExportRepresentation> importedExecutions = importedFlow.getAuthenticationExecutions();
        Optional<AuthenticationExecutionExportRepresentation> maybeImportedExecution = importedExecutions.stream().filter(e -> e.getAuthenticator().equals("docker-http-basic-authenticator")).findFirst();

        assertThat(maybeImportedExecution.isPresent(), is(true));

        AuthenticationExecutionExportRepresentation importedExecution = maybeImportedExecution.get();
        assertThat(importedExecution.getAuthenticator(), is("docker-http-basic-authenticator"));
        assertThat(importedExecution.getRequirement(), is("DISABLED"));
        assertThat(importedExecution.getPriority(), is(0));
        assertThat(importedExecution.isAutheticatorFlow(), is(false));
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
