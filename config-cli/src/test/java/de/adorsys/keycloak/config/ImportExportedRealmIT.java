package de.adorsys.keycloak.config;

import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

public class ImportExportedRealmIT extends AbstractImportTest {
    private static final Map<String, String> EXPECTED_CHECKSUMS = new HashMap<>();
    private static final String REALM_NAME = "master";

    static {
        EXPECTED_CHECKSUMS.put("8.0.1", "83563e05222431f51654e7e4fe6b87e696aec8a43614a10197b327d6a653e51e7b2517f7c289fb90a7d034e1b9617bbd7b4a4ff027378802f2cc716d0e290c64");
        EXPECTED_CHECKSUMS.put("9.0.0", "9f9eff8a6cd78cf98603ff2d8dfec9ce2b909ff45e58e952d4c39b16aa16ecd2aa136dba269cae678b5f70a641244c43969e597060c1ff502b8bdd2247ac0461");
    }

    private String keycloakVersion;

    ImportExportedRealmIT() {
        keycloakVersion = System.getProperty("keycloak.version");
        this.resourcePath = "import-files/exported-realm/" + keycloakVersion;
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

    private String expectedImportFileChecksum(String keycloakVersion) {
        return EXPECTED_CHECKSUMS.get(keycloakVersion);
    }
}
