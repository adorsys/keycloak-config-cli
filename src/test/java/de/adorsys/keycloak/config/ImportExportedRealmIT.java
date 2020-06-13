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

class ImportExportedRealmIT extends AbstractImportTest {
    private static final Map<String, String> EXPECTED_CHECKSUMS = new HashMap<>();
    private static final String REALM_NAME = "master";

    static {
        EXPECTED_CHECKSUMS.put("8.0.1", "50c90cec5aff9483ee7cfd1c50989b470fdf83055df5d03ef3bb008e98417709");
        EXPECTED_CHECKSUMS.put("9.0.3", "3655b85eb36eb89ea2b2198eb58c4d0da5ecba79b103c28247ca0511e2cac85b");
        EXPECTED_CHECKSUMS.put("10.0.1", "963dc7e34450f7487df325f9708e3ee036a717d22569bf6e4efe48ab530d81ea");
        EXPECTED_CHECKSUMS.put("10.0.2", "634b84b3ee12efdbb6000aa80ce0092cee1e76d02d41d3ac4df6b6af770dbcc2");
    }

    private final String keycloakVersion = System.getProperty("keycloak.version");

    ImportExportedRealmIT() {
        this.resourcePath = "import-files/exported-realm/" + keycloakVersion;
    }

    @Test
    void shouldImportExportedRealm() {
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
