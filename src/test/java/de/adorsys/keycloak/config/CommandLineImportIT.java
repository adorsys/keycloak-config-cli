/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2021 adorsys GmbH & Co. KG @ https://adorsys.com
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

package de.adorsys.keycloak.config;

import com.ginsberg.junit.exit.ExpectSystemExitWithStatus;
import de.adorsys.keycloak.config.exception.InvalidImportException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandLineImportIT extends AbstractImportIT {
    @Nested
    @ContextConfiguration()
    class CommandLineImportFilesIT extends AbstractImportIT {
        @Test
        @ExpectSystemExitWithStatus(0)
        void testImportFile() {
            KeycloakConfigApplication.main(new String[]{
                    "--import.files.locations=src/test/resources/import-files/cli/file.json"
            });

            RealmRepresentation fileRealm = keycloakProvider.getInstance().realm("file").toRepresentation();

            assertThat(fileRealm.getRealm(), is("file"));
            assertThat(fileRealm.isEnabled(), is(true));
        }

        @Test
        @ExpectSystemExitWithStatus(0)
        void testImportDirectory() {
            KeycloakConfigApplication.main(new String[]{
                    "--keycloak.sslVerify=false",
                    "--import.files.locations=src/test/resources/import-files/cli/dir/*"
            });

            RealmRepresentation file1Realm = keycloakProvider.getInstance().realm("file1").toRepresentation();

            assertThat(file1Realm.getRealm(), is("file1"));
            assertThat(file1Realm.isEnabled(), is(true));

            RealmRepresentation file2Realm = keycloakProvider.getInstance().realm("file2").toRepresentation();

            assertThat(file2Realm.getRealm(), is("file2"));
            assertThat(file2Realm.isEnabled(), is(true));
        }

        @Test
        @ExpectSystemExitWithStatus(1)
        @SuppressWarnings({"java:S2699"})
        void testImportInvalid() {
            KeycloakConfigApplication.main(new String[]{
                    "--import.files.locations=invalid",
                    "--logging.level.de.adorsys.keycloak.config.KeycloakConfigRunner=error",
            });
        }
    }

    @Nested
    @ContextConfiguration()
    @TestPropertySource(properties = {
            "import.files.locations=src/test/resources/application-IT.properties",
    })
    class CommandLineImportInvalidIT extends AbstractImportIT {
        @Autowired
        KeycloakConfigRunner runner;

        @Test
        void testInvalidFileFormatException() {
            InvalidImportException thrown = assertThrows(InvalidImportException.class, runner::run);

            assertThat(thrown.getMessage(), startsWith("Unable to parse file 'file:src/test/resources/application-IT.properties': Cannot construct instance of `de.adorsys.keycloak.config.model.RealmImport`"));
        }
    }

    @Nested
    @ContextConfiguration()
    @TestPropertySource(properties = {
            "import.files.locations=invalid"
    })
    class CommandLineImportNotExistsExceptionIT extends AbstractImportIT {
        @Autowired
        KeycloakConfigRunner runner;

        @Test
        void testInvalidImportException() {
            InvalidImportException thrown = assertThrows(InvalidImportException.class, runner::run);

            assertThat(thrown.getMessage(), Matchers.is("Unable to proceed resource 'URL [file:invalid]': invalid (No such file or directory)"));
        }
    }
}
