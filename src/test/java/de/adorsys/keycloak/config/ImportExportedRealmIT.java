/*
 * Copyright 2019-2020 adorsys GmbH & Co. KG @ https://adorsys.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

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
        EXPECTED_CHECKSUMS.put("9.0.3", "3742d73d49b2fa7b47a1bfd93728ab3389735e6d613feaad5d88051907eed5d7c7109c0642a1d2b28f868713af39d90eb0942f8bd5bedc61e471b72110bc0ac2");
    }

    private final String keycloakVersion;

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
