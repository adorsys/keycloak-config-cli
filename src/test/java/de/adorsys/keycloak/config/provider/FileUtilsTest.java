package de.adorsys.keycloak.config.provider;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FileUtilsTest {

    @Test
    public void shouldSplitFileNameCorrect() {
        // Given
        String fileName = "test-test.temp.json";

        // When
        String[] splitted = fileName.split(FileUtils.REGEX_FILE_NAME_EXTENSION_SPLITTER);

        // Then
        Assertions.assertEquals(2, splitted.length);
        Assertions.assertEquals("test-test.temp", splitted[0]);
        Assertions.assertEquals("json", splitted[1]);
    }
}
