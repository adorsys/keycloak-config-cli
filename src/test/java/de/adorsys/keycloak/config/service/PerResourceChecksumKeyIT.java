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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@ContextConfiguration()
@TestPropertySource(properties = {
        "import.behaviors.checksum-with-cache-key=false",
        "import.behaviors.checksum-changed=continue"
})
public class PerResourceChecksumKeyIT extends AbstractChecksumServiceIT {

    @Test
    void hasToBeUpdated_with_single_file() throws Exception {
        this.resourcePath = "import-files/simple-realm";

        var fileName = "00_create_simple-realm.json";
        importAndVerifyChecksum(fileName, "6292be0628c50ff8fc02bd4092f48a731133e4802e158e7bc2ba174524b4ccf1");

        verifyHasToBeUpdated(fileName, false);
    }

    @Test
    void hasToBeUpdated_with_multiple_files() throws Exception {
        this.resourcePath = "import-files/simple-realm";

        var fileName01 = "00_create_simple-realm.json";
        importAndVerifyChecksum(fileName01, "6292be0628c50ff8fc02bd4092f48a731133e4802e158e7bc2ba174524b4ccf1");

        var fileName02 = "01_update_login-theme_to_simple-realm.json";
        importAndVerifyChecksum(fileName02, "4ac94d3adb91122979e80816a8a355a01f9c7c90a25b6b529bf2a572e1158b1c");

        verifyHasToBeUpdated(fileName01, false);
        verifyHasToBeUpdated(fileName02, false);
    }

    @Test
    void hasToBeUpdated_with_multi_document() throws Exception {
        this.resourcePath = "import-files/realm-file-type/auto";

        var fileName = "2_multi_document.yaml";
        doImport(fileName);

        var createdRealm = keycloakProvider.getInstance().realm("realm-file-type-auto-0").toRepresentation();
        verifyChecksum(createdRealm, "950de6a46f669dbd6c42178fde9d9b6ab3315eed5226c7051ca02c5b35263996");

        createdRealm = keycloakProvider.getInstance().realm("realm-file-type-auto-1").toRepresentation();
        verifyChecksum(createdRealm, "950de6a46f669dbd6c42178fde9d9b6ab3315eed5226c7051ca02c5b35263996");

        getImport(fileName).forEach(realmImport -> verifyHasToBeUpdated(realmImport, false));
    }

    @Test
    void hasToBeUpdated_with_same_filenames() {
        this.resourcePath = null;

        var realmImports = importFromDirectory("classpath:import-files/import/same-names/**/*.yaml");
        realmImports.forEach(realmImport -> verifyHasToBeUpdated(realmImport, false));
    }
}
