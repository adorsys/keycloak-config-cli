package de.adorsys.keycloak.config;

import de.adorsys.keycloak.config.configuration.TestConfiguration;
import de.adorsys.keycloak.config.exception.InvalidImportException;
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
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {TestConfiguration.class},
        initializers = {ConfigFileApplicationContextInitializer.class}
)
@ActiveProfiles("IT")
@DirtiesContext
public class ImportRequiredActionsIT {
    private static final String REALM_NAME = "realmWithRequiredActions";

    @Autowired
    RealmImportService realmImportService;

    @Autowired
    KeycloakImportProvider keycloakImportProvider;

    @Autowired
    KeycloakProvider keycloakProvider;

    KeycloakImport keycloakImport;

    @BeforeEach
    public void setup() {
        File configsFolder = ResourceLoader.loadResource("import-files/required-actions");
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
        shouldCreateRealmWithRequiredActions();
        shouldFailIfAddingInvalidRequiredActionName();
        shouldAddRequiredAction();
        shouldChangeRequiredActionName();
        shouldEnableRequiredAction();
        shouldChangePriorities();
    }

    private void shouldCreateRealmWithRequiredActions() {
        doImport("0_create_realm_with_required-action.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        RequiredActionProviderRepresentation createdRequiredAction = getRequiredAction(createdRealm, "MY_CONFIGURE_TOTP");
        assertThat(createdRequiredAction.getAlias(), is("MY_CONFIGURE_TOTP"));
        assertThat(createdRequiredAction.getName(), is("My Configure OTP"));
        assertThat(createdRequiredAction.getProviderId(), is("MY_CONFIGURE_TOTP"));
        assertThat(createdRequiredAction.isEnabled(), is(true));
        assertThat(createdRequiredAction.isDefaultAction(), is(false));
        assertThat(createdRequiredAction.getPriority(), is(0));
    }

    private void shouldFailIfAddingInvalidRequiredActionName() {
        RealmImport foundImport = getImport("1_update_realm__try_adding_invalid_required-action.json");

        InvalidImportException thrown = assertThrows(InvalidImportException.class, () -> realmImportService.doImport(foundImport));

        assertEquals(thrown.getMessage(), "Cannot import Required-Action 'my_terms_and_conditions': alias and provider-id have to be equal");
    }

    private void shouldAddRequiredAction() {
        doImport("2_update_realm__add_required-action.json");

        RealmRepresentation updatedRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));

        RequiredActionProviderRepresentation unchangedRequiredAction = getRequiredAction(updatedRealm, "MY_CONFIGURE_TOTP");
        assertThat(unchangedRequiredAction.getAlias(), is("MY_CONFIGURE_TOTP"));
        assertThat(unchangedRequiredAction.getName(), is("My Configure OTP"));
        assertThat(unchangedRequiredAction.getProviderId(), is("MY_CONFIGURE_TOTP"));
        assertThat(unchangedRequiredAction.isEnabled(), is(true));
        assertThat(unchangedRequiredAction.isDefaultAction(), is(false));
        assertThat(unchangedRequiredAction.getPriority(), is(0));

        RequiredActionProviderRepresentation addedRequiredAction = getRequiredAction(updatedRealm, "my_terms_and_conditions");
        assertThat(addedRequiredAction.getAlias(), is("my_terms_and_conditions"));
        assertThat(addedRequiredAction.getName(), is("My Terms and Conditions"));
        assertThat(addedRequiredAction.getProviderId(), is("my_terms_and_conditions"));
        assertThat(addedRequiredAction.isEnabled(), is(false));
        assertThat(addedRequiredAction.isDefaultAction(), is(false));
        assertThat(addedRequiredAction.getPriority(), is(1));
    }

    private void shouldChangeRequiredActionName() {
        doImport("3_update_realm__change_name_of_required-action.json");

        RealmRepresentation updatedRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));

        RequiredActionProviderRepresentation unchangedRequiredAction = getRequiredAction(updatedRealm, "MY_CONFIGURE_TOTP");
        assertThat(unchangedRequiredAction.getAlias(), is("MY_CONFIGURE_TOTP"));
        assertThat(unchangedRequiredAction.getName(), is("My Configure OTP"));
        assertThat(unchangedRequiredAction.getProviderId(), is("MY_CONFIGURE_TOTP"));
        assertThat(unchangedRequiredAction.isEnabled(), is(true));
        assertThat(unchangedRequiredAction.isDefaultAction(), is(false));
        assertThat(unchangedRequiredAction.getPriority(), is(0));

        RequiredActionProviderRepresentation changedRequiredAction = getRequiredAction(updatedRealm, "my_terms_and_conditions");
        assertThat(changedRequiredAction.getAlias(), is("my_terms_and_conditions"));
        assertThat(changedRequiredAction.getName(), is("Changed: My Terms and Conditions"));
        assertThat(changedRequiredAction.getProviderId(), is("my_terms_and_conditions"));
        assertThat(changedRequiredAction.isEnabled(), is(false));
        assertThat(changedRequiredAction.isDefaultAction(), is(false));
        assertThat(changedRequiredAction.getPriority(), is(1));
    }

    private void shouldEnableRequiredAction() {
        doImport("4_update_realm__enable_required-action.json");

        RealmRepresentation updatedRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));

        RequiredActionProviderRepresentation unchangedRequiredAction = getRequiredAction(updatedRealm, "MY_CONFIGURE_TOTP");
        assertThat(unchangedRequiredAction.getAlias(), is("MY_CONFIGURE_TOTP"));
        assertThat(unchangedRequiredAction.getName(), is("My Configure OTP"));
        assertThat(unchangedRequiredAction.getProviderId(), is("MY_CONFIGURE_TOTP"));
        assertThat(unchangedRequiredAction.isEnabled(), is(true));
        assertThat(unchangedRequiredAction.isDefaultAction(), is(false));
        assertThat(unchangedRequiredAction.getPriority(), is(0));

        RequiredActionProviderRepresentation changedRequiredAction = getRequiredAction(updatedRealm, "my_terms_and_conditions");
        assertThat(changedRequiredAction.getAlias(), is("my_terms_and_conditions"));
        assertThat(changedRequiredAction.getName(), is("Changed: My Terms and Conditions"));
        assertThat(changedRequiredAction.getProviderId(), is("my_terms_and_conditions"));
        assertThat(changedRequiredAction.isEnabled(), is(true));
        assertThat(changedRequiredAction.isDefaultAction(), is(false));
        assertThat(changedRequiredAction.getPriority(), is(1));
    }

    private void shouldChangePriorities() {
        doImport("5_update_realm__change_priorities_required-action.json");

        RealmRepresentation updatedRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));

        RequiredActionProviderRepresentation unchangedRequiredAction = getRequiredAction(updatedRealm, "MY_CONFIGURE_TOTP");
        assertThat(unchangedRequiredAction.getAlias(), is("MY_CONFIGURE_TOTP"));
        assertThat(unchangedRequiredAction.getName(), is("My Configure OTP"));
        assertThat(unchangedRequiredAction.getProviderId(), is("MY_CONFIGURE_TOTP"));
        assertThat(unchangedRequiredAction.isEnabled(), is(true));
        assertThat(unchangedRequiredAction.isDefaultAction(), is(false));
        assertThat(unchangedRequiredAction.getPriority(), is(1));

        RequiredActionProviderRepresentation changedRequiredAction = getRequiredAction(updatedRealm, "my_terms_and_conditions");
        assertThat(changedRequiredAction.getAlias(), is("my_terms_and_conditions"));
        assertThat(changedRequiredAction.getName(), is("Changed: My Terms and Conditions"));
        assertThat(changedRequiredAction.getProviderId(), is("my_terms_and_conditions"));
        assertThat(changedRequiredAction.isEnabled(), is(true));
        assertThat(changedRequiredAction.isDefaultAction(), is(false));
        assertThat(changedRequiredAction.getPriority(), is(0));
    }

    private RequiredActionProviderRepresentation getRequiredAction(RealmRepresentation realm, String requiredActionAlias) {
        Optional<RequiredActionProviderRepresentation> maybeRequiredAction = realm.getRequiredActions()
                .stream()
                .filter(r -> r.getAlias().equals(requiredActionAlias))
                .findFirst();

        assertThat("Cannot find required-action: '" + requiredActionAlias + "'", maybeRequiredAction.isPresent(), is(true));

        return maybeRequiredAction.get();
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
