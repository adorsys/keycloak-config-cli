/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2025 adorsys GmbH & Co. KG @ https://adorsys.com
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.AbstractImportIT;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.service.checksum.ChecksumService;
import org.junit.jupiter.api.AfterEach;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;

public class AbstractChecksumServiceIT extends AbstractImportIT {

    private static final String REALM_NAME = "simple";

    @Autowired
    ChecksumService checksumService;

    @AfterEach
    void clearRealms() {
        keycloakProvider.getInstance().realms().findAll().forEach(realm -> {
            if (!Objects.equals(realm.getRealm(), "master")) {
                keycloakProvider.getInstance().realm(realm.getRealm()).remove();
            }
        });
    }

    void verifyChecksum(RealmRepresentation realm, String checksum) {
        Map<String, String> attributes = realm.getAttributes();
        var prefix = ImportConfigProperties.REALM_CHECKSUM_ATTRIBUTE_PREFIX_KEY.substring(0, ImportConfigProperties.REALM_CHECKSUM_ATTRIBUTE_PREFIX_KEY.length() - 3);
        assertThat(attributes, hasEntry(startsWith(prefix), is(checksum)));
    }

    void verifyHasToBeUpdated(RealmImport realmImport, boolean expected) {
        var hasToBe = checksumService.hasToBeUpdated(realmImport);
        assertThat(hasToBe, is(expected));
    }

    void verifyHasToBeUpdated(String fileName, boolean expected) throws IOException {
        var realmImport = getFirstImport(fileName);
        var hasToBe = checksumService.hasToBeUpdated(realmImport);
        assertThat(hasToBe, is(expected));
    }

    void importAndVerifyChecksum(String filename, String checksum) throws Exception {
        doImport(filename);

        var createdRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();
        verifyChecksum(createdRealm, checksum);
    }

    List<RealmImport> importFromDirectory(String location) {
        var keycloakImport = keycloakImportProvider.readFromLocations(location);
        var realmImports = keycloakImport.getRealmImports().values().stream()
                .flatMap(e -> e.values().stream())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        realmImports.forEach(realmImport -> realmImportService.doImport(realmImport));

        return realmImports;
    }
}
