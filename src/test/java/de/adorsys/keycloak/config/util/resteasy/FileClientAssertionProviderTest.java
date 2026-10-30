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

package de.adorsys.keycloak.config.util.resteasy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileClientAssertionProviderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReadTheClientAssertionFileForEachCall() throws IOException {
        Path tokenFile = writeToken("assertion-one\n");
        FileClientAssertionProvider provider = new FileClientAssertionProvider(tokenFile.toString());

        String firstAssertion = provider.getClientAssertion();
        Files.writeString(tokenFile, "assertion-two");
        String secondAssertion = provider.getClientAssertion();

        assertThat(firstAssertion, is("assertion-one"));
        assertThat(secondAssertion, is("assertion-two"));
    }

    @Test
    void shouldFailForUnreadableClientAssertionFile() {
        FileClientAssertionProvider provider = new FileClientAssertionProvider(tempDir.toString());

        assertThrows(IOException.class, provider::getClientAssertion);
    }

    @Test
    void shouldFailForBlankClientAssertionFile() throws IOException {
        FileClientAssertionProvider provider = new FileClientAssertionProvider(writeToken(" \n\t ").toString());

        assertThrows(IOException.class, provider::getClientAssertion);
    }

    private Path writeToken(String token) throws IOException {
        return Files.writeString(tempDir.resolve("client-assertion"), token);
    }
}
