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

package de.adorsys.keycloak.config.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.keycloak.config.exception.NormalizationException;
import de.adorsys.keycloak.config.properties.NormalizationConfigProperties;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BaselineProviderTest {

    private NormalizationConfigProperties configWithFallback(String fallbackVersion) {
        return new NormalizationConfigProperties(
                new NormalizationConfigProperties.NormalizationFilesProperties(
                        List.of("."),
                        List.of(),
                        false,
                        "target"),
                NormalizationConfigProperties.OutputFormat.YAML,
                fallbackVersion);
    }

    @Test
    void getRealmInputStream_WhenVersionExists_ReturnsStream() {
        BaselineProvider provider = new BaselineProvider(new ObjectMapper(), configWithFallback(null));

        try (InputStream is = provider.getRealmInputStream("26.0.5")) {
            assertNotNull(is);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getClientInputStream_WhenVersionExists_ReturnsStream() {
        BaselineProvider provider = new BaselineProvider(new ObjectMapper(), configWithFallback(null));

        try (InputStream is = provider.getClientInputStream("26.0.5")) {
            assertNotNull(is);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getRealmInputStream_WhenVersionMissing_AndFallbackExists_UsesFallback() {
        BaselineProvider provider = new BaselineProvider(new ObjectMapper(), configWithFallback("26.0.5"));

        try (InputStream is = provider.getRealmInputStream("does-not-exist")) {
            assertNotNull(is);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getClientInputStream_WhenVersionMissing_AndFallbackExists_UsesFallback() {
        BaselineProvider provider = new BaselineProvider(new ObjectMapper(), configWithFallback("26.0.5"));

        try (InputStream is = provider.getClientInputStream("does-not-exist")) {
            assertNotNull(is);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getRealmInputStream_WhenVersionMissing_AndFallbackMissing_Throws() {
        BaselineProvider provider = new BaselineProvider(new ObjectMapper(), configWithFallback(null));

        assertThrows(NormalizationException.class, () -> provider.getRealmInputStream("does-not-exist"));
    }

    @Test
    void getClientInputStream_WhenVersionMissing_AndFallbackMissing_Throws() {
        BaselineProvider provider = new BaselineProvider(new ObjectMapper(), configWithFallback(null));

        assertThrows(NormalizationException.class, () -> provider.getClientInputStream("does-not-exist"));
    }

    @Test
    void getRealm_WhenCalled_ReplacesPlaceholderAndParses() {
        BaselineProvider provider = new BaselineProvider(new ObjectMapper(), configWithFallback(null));

        RealmRepresentation realm = provider.getRealm("26.0.5", "my-test-realm");
        assertNotNull(realm);
        assertEquals("my-test-realm", realm.getRealm());
    }

    @Test
    void getClient_WhenCalled_SetsClientIdAndParses() {
        BaselineProvider provider = new BaselineProvider(new ObjectMapper(), configWithFallback(null));

        ClientRepresentation client = provider.getClient("26.0.5", "my-client");
        assertNotNull(client);
        assertEquals("my-client", client.getClientId());
    }
}
