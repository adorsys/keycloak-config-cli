/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2020 adorsys GmbH & Co. KG @ https://adorsys.de
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

import de.adorsys.keycloak.config.exception.InvalidImportException;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ContextConfiguration()
class CommandLineIT extends AbstractImportTest {
    @Autowired
    KeycloakConfigApplication keycloakConfigApplication;

    @Autowired
    KeycloakConfigRunner runner;

    @Override
    @SuppressWarnings("unused")
    public void setup() {
    }

    @Test
    void testInvalidImportException() {
        InvalidImportException thrown = assertThrows(InvalidImportException.class, runner::run);

        assertThat(thrown.getMessage(), matchesPattern("import\\.path does not exists: .+default$"));
    }

    @Test
    void testImportFile() {
        KeycloakConfigApplication.main(new String[]{
                "--keycloak.sslVerify=true",
                "--import.path=src/test/resources/import-files/cli/file.json",
        });

        RealmRepresentation fileRealm = keycloakProvider.get().realm("file").toRepresentation();

        assertThat(fileRealm.getRealm(), is("file"));
        assertThat(fileRealm.isEnabled(), is(true));
    }

    @Test
    void testImportDirectory() {
        KeycloakConfigApplication.main(new String[]{
                "--keycloak.sslVerify=false",
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
