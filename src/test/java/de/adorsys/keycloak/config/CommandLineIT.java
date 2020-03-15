package de.adorsys.keycloak.config;

import de.adorsys.keycloak.config.exception.InvalidImportException;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ContextConfiguration()
public class CommandLineIT extends AbstractImportTest {
    @Autowired
    Application application;

    public void setup() {
    }

    public void shouldReadImports() {
    }

    public void keycloakRunning() {
    }

    @Test
    public void testException() {
        InvalidImportException thrown = assertThrows(InvalidImportException.class, application::run);

        assertEquals("Either 'import.path' or 'import.file' has to be defined", thrown.getMessage());
    }

    /* TODO: find better call to test this
    @Test
    public void testImportNonExistFile() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
                Application.main(new String[]{
                        "--spring.main.allow-bean-definition-overriding=true",
                        "--import.file=nonexist",
                });
        });
    }

    @Test
    public void testImportNonExistDirectory() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
            Application.main(new String[]{
                    "--spring.main.allow-bean-definition-overriding=true",
                    "--import.path=nonexist",
            });
        });
    }
    */

    @Test
    public void testImportFile() {
        Application.main(new String[]{
                "--spring.main.allow-bean-definition-overriding=true",
                "--keycloak.sslVerify=true",
                "--import.file=src/test/resources/import-files/cli/file.json",
        });

        RealmRepresentation fileRealm = keycloakProvider.get().realm("file").toRepresentation();

        assertThat(fileRealm.getRealm(), is("file"));
        assertThat(fileRealm.isEnabled(), is(true));
    }

    @Test
    public void testImportDirectory() {
        Application.main(new String[]{
                "--spring.main.allow-bean-definition-overriding=true",
                "--keycloak.sslVerify=true",
                "--import.path=src/test/resources/import-files/cli/dir/",
        });

        RealmRepresentation file1Realm = keycloakProvider.get().realm("file1").toRepresentation();

        assertThat(file1Realm.getRealm(), is("file1"));
        assertThat(file1Realm.isEnabled(), is(true));

        RealmRepresentation file2Realm = keycloakProvider.get().realm("file2").toRepresentation();

        assertThat(file2Realm.getRealm(), is("file2"));
        assertThat(file2Realm.isEnabled(), is(true));
    }
}
