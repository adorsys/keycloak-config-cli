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
        EXPECTED_CHECKSUMS.put("9.0.3", "30975f1460b5ecbc931539c0b101e91b37438531a81c30a5457f362c98d9630450ee4f6c6154f3ce8f53249d155d14afcdcd4896e2c7cea5842fb16310b8b32e");
        EXPECTED_CHECKSUMS.put("10.0.0", "07f5271570a45ce6c6e0277a0cec5a11870546353f5c435afcae1fcfbb30502b834960b6b5f1cf8660cf55514cabecfd7d13497fead3709f7c908dab6fecb6ef");
        EXPECTED_CHECKSUMS.put("10.0.1", "c5985e3d1bfbd2b5dc03b574971492e818586045668ad5457098a6bff4f010d7bb35a9d4154d4a0dfcc14a0e74c4ac69c38f5a268b18f7db9762f19fd1605fc1");
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
