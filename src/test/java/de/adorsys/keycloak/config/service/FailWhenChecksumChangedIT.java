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
