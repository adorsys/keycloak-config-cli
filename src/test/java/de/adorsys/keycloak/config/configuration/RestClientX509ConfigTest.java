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

package de.adorsys.keycloak.config.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class RestClientX509ConfigTest {
    private RestClientX509Config restClientX509Config;

    @BeforeEach
    void setup() {
        restClientX509Config = new RestClientX509Config();
    }

    @Test
    void testGetSslContextWithNullPaths() throws Exception {
        restClientX509Config.setKeystorePath(null);
        restClientX509Config.setTrustStorePath(null);
        restClientX509Config.setKeystorePassword(null);
        restClientX509Config.setTrustStorePassword(null);
        Assertions.assertNotNull(restClientX509Config.getSslContext());
    }

    @Test
    void testGetSslContextWithValidPaths() throws Exception {
        restClientX509Config.setKeystorePath("src/test/resources/config-files-x509/keystore.p12");
        restClientX509Config.setKeystorePassword("keystorepass");
        restClientX509Config.setTrustStorePath("src/test/resources/config-files-x509/truststore.jks");
        restClientX509Config.setTrustStorePassword("truststorepass");

        Assertions.assertNotNull(restClientX509Config.getSslContext());
    }

    @Test
    void testIsX509Configured() {
        restClientX509Config.setKeystorePath("src/test/resources/config-files-x509/keystore.p12");
        restClientX509Config.setKeystorePassword("keystorepass");
        restClientX509Config.setTrustStorePath("src/test/resources/config-files-x509/truststore.jks");
        restClientX509Config.setTrustStorePassword("truststorepass");

        Assertions.assertTrue(restClientX509Config.isX509Configured());

        restClientX509Config.setKeystorePath(null);
        Assertions.assertFalse(restClientX509Config.isX509Configured());
    }

}
