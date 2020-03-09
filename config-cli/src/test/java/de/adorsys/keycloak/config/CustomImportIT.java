package de.adorsys.keycloak.config;

import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CustomImportIT extends AbstractImportTest {
    private static final String REALM_NAME = "realmWithCustomImport";

    CustomImportIT() {
        this.resourcePath = "import-files/custom-import";
    }

    @Test
    public void integrationTests() {
        shouldCreateRealm();
        shouldRemoveImpersonation();
    }

    private void shouldCreateRealm() {
        doImport("0_create_realm_with_empty_custom-import.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        boolean isImpersonationClientRoleExisting = keycloakRepository.isClientRoleExisting("master", "realmWithCustomImport-realm", "impersonation");

        assertThat(isImpersonationClientRoleExisting, is(true));
    }

    private void shouldRemoveImpersonation() {
        doImport("1_update_realm__remove_impersonation.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        boolean isImpersonationClientRoleExisting = keycloakRepository.isClientRoleExisting("master", "realmWithCustomImport-realm", "impersonation");

        assertThat(isImpersonationClientRoleExisting, is(false));
    }
}
