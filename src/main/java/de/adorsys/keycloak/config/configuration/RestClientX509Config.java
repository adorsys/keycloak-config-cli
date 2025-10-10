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

import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;

@Component
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class RestClientX509Config {

    private static final Logger logger = LoggerFactory.getLogger(RestClientX509Config.class);

    private final KeycloakConfigProperties.X509Config x509Config;
    private SSLContext sslContext;

    @Autowired
    public RestClientX509Config(KeycloakConfigProperties properties) {
        this.x509Config = properties.getX509();
    }

    public boolean isX509Configured() {
        return x509Config != null && x509Config.isConfigured();
    }

    public SSLContext getSslContext() throws Exception {

        if (sslContext == null && isX509Configured()) {
            sslContext = createSslContext();
        }

        return sslContext;
    }

    private SSLContext createSslContext() throws Exception {

        logger.info("Loading X.509 certificates for client authentication");

        // Load keystore
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream keystoreFile = new FileInputStream(x509Config.keystorePath())) {
            keystore.load(keystoreFile, x509Config.keystorePassword().toCharArray());
            logger.debug("Keystore loaded from: {}", x509Config.keystorePath());
        }

        // Load truststore
        KeyStore truststore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream truststoreFile = new FileInputStream(x509Config.truststorePath())) {
            truststore.load(truststoreFile, x509Config.truststorePassword().toCharArray());
            logger.debug("Truststore loaded from: {}", x509Config.truststorePath());
        }

        // Build SSLContext
        SSLContext context = SSLContextBuilder.create()
                .loadKeyMaterial(keystore, x509Config.keystorePassword().toCharArray())
                .loadTrustMaterial(truststore, null)
                .build();

        logger.info("SSL context successfully created for X.509 authentication");
        return context;
    }
}




