package de.adorsys.keycloak.config;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ImportAuthenticatorConfigIT extends AbstractImportTest {
    private static final String REALM_NAME = "realmWithAuthConfig";

    ImportAuthenticatorConfigIT() {
        this.resourcePath = "import-files/auth-config";
    }

    @Test
    @Order(0)
    public void shouldCreateRealmWithFlows() {
        doImport("0_create_realm_with_flow_auth_config.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        Optional<AuthenticatorConfigRepresentation> authConfig = getAuthenticatorConfig(createdRealm, "test auth config");
        assertThat(authConfig.isPresent(), is(true));
        assertThat(authConfig.get().getConfig().get("require.password.update.after.registration"), is("false"));
    }

    @Test
    @Order(1)
    public void shouldAddExecutionToFlow() {
        doImport("1_update_realm_auth_config.json");

        RealmRepresentation updatedRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));

        Optional<AuthenticatorConfigRepresentation> changedAuthConfig = getAuthenticatorConfig(updatedRealm, "test auth config");
        assertThat(changedAuthConfig.isPresent(), is(true));
        assertThat(changedAuthConfig.get().getConfig().get("require.password.update.after.registration"), is("true"));
    }

    @Test
    @Order(2)
    public void shouldChangeExecutionRequirement() {
        doImport("2_remove_realm_auth_config.json");

        RealmRepresentation updatedRealm = keycloakProvider.get().realm(REALM_NAME).partialExport(true, true);

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));

        Optional<AuthenticatorConfigRepresentation> deletedAuthConfig = getAuthenticatorConfig(updatedRealm, "test auth config");
        assertThat(deletedAuthConfig.isPresent(), is(false));
    }


    private Optional<AuthenticatorConfigRepresentation> getAuthenticatorConfig(RealmRepresentation updatedRealm, String configAlias) {
        return updatedRealm
                .getAuthenticatorConfig()
                .stream()
                .filter(x -> x.getAlias().equals(configAlias)).findAny();
    }
}
