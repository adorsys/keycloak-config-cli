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

import de.adorsys.keycloak.config.exception.InvalidImportException;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ContextConfiguration()
public class CommandLineIT extends AbstractImportTest {
    @Autowired
    KeycloakConfigApplication keycloakConfigApplication;

    @Autowired
    KeycloakConfigRunner runner;

    @Override
    public void setup() {
    }

    @Test
    public void testException() {
        InvalidImportException thrown = assertThrows(InvalidImportException.class, runner::run);

        assertThat(thrown.getMessage(), is("Either 'import.path' or 'import.file' has to be defined"));
    }

    /* TODO: find better call to test this
    @Test
    public void testImportNonExistFile() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
                KeycloakConfigApplication.main(new String[]{
                        "--spring.main.allow-bean-definition-overriding=true",
                        "--import.file=nonexist",
                });
        });
    }

    @Test
    public void testImportNonExistDirectory() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
            KeycloakConfigApplication.main(new String[]{
                    "--spring.main.allow-bean-definition-overriding=true",
                    "--import.path=nonexist",
            });
        });
    }
    */

    @Test
    public void testImportFile() {
        KeycloakConfigApplication.main(new String[]{
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
        KeycloakConfigApplication.main(new String[]{
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
