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

import de.adorsys.keycloak.config.exception.InvalidImportException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@ContextConfiguration()
@TestPropertySource(properties = {
        "import.behaviors.checksum-with-cache-key=false",
        "import.behaviors.checksum-changed=fail"
})
public class FailWhenChecksumChangedIT extends AbstractChecksumServiceIT {

    @Test
    void hasToBeUpdated_with_multiple_files_fails() throws Exception {
        this.resourcePath = "import-files/simple-realm";

        var fileName = "00_create_simple-realm.json";
        importAndVerifyChecksum(fileName, "6292be0628c50ff8fc02bd4092f48a731133e4802e158e7bc2ba174524b4ccf1");

        var realmImport = getFirstImport(fileName);
        realmImport.setChecksum("");
        Assertions.assertThatThrownBy(() -> checksumService.hasToBeUpdated(realmImport))
                .isInstanceOf(InvalidImportException.class)
                .hasMessageContaining("checksum", "changed");
    }

    @Test
    void hasToBeUpdated_with_same_filenames() {
        var realmImports = importFromDirectory("classpath:import-files/import/same-names/**/*.yaml");
        realmImports.forEach(realmImport -> verifyHasToBeUpdated(realmImport, false));
    }

}
