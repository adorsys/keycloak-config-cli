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
        EXPECTED_CHECKSUMS.put("9.0.2", "da21bfc3ddd053d880c1633ea2d7d6003823504491c79169d9bd11935652bd194bca9fba763eebb7c72d818ec141e7d21651f7db5d9a2156d54c07952f12d59f");
    }

    private String keycloakVersion;

    ImportExportedRealmIT() {
        keycloakVersion = System.getProperty("keycloak.version");
        this.resourcePath = "import-files/exported-realm/" + keycloakVersion;
    }

    @Test
    public void shouldImportExportedRealm() {
        doImport("master-realm.json");

        RealmRepresentation updatedRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(updatedRealm.getRealm(), is(REALM_NAME));
        assertThat(updatedRealm.isEnabled(), is(true));
        assertThat(updatedRealm.getLoginTheme(), is(nullValue()));
        assertThat(
                updatedRealm.getAttributes().get("de.adorsys.keycloak.config.import-checksum-default"),
                is(expectedImportFileChecksum(keycloakVersion))
        );
    }

    private String expectedImportFileChecksum(String keycloakVersion) {
        return EXPECTED_CHECKSUMS.get(keycloakVersion);
    }
}
