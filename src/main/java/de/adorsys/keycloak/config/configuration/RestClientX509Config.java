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

import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;


@Configuration
@ConfigurationProperties(prefix = "key.ssl", ignoreUnknownFields = false)
public class RestClientX509Config {

    private String keystorePath;
    private String keystorePassword;
    private String trustStorePath;
    private String trustStorePassword;
    private static final Logger logger = LoggerFactory.getLogger(RestClientX509Config.class);

    public String getKeystorePath() {
        return keystorePath;
    }

    public void setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getTrustStorePath() {
        return trustStorePath;
    }

    public void setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    @Bean
    public SSLContext getSslContext() throws Exception {
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        boolean isTrustStoreLoaded = false;
        boolean isKeystoreLoaded = false;

        if (keystorePath != null && keystorePassword != null) {
            try (FileInputStream keystoreFile = new FileInputStream(keystorePath)) {
                keystore.load(keystoreFile, keystorePassword.toCharArray());
                isKeystoreLoaded = true;
            }
        }
        KeyStore truststore = KeyStore.getInstance(KeyStore.getDefaultType());
        if (trustStorePath != null && trustStorePassword != null) {
            try (FileInputStream truststoreFile = new FileInputStream(trustStorePath)) {
                truststore.load(truststoreFile, trustStorePassword.toCharArray());
                isTrustStoreLoaded = true;
            }
        }

        SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();
        if (isKeystoreLoaded) {
            sslContextBuilder.loadKeyMaterial(keystore, keystorePassword.toCharArray());
        }
        if (isTrustStoreLoaded) {
            sslContextBuilder.loadTrustMaterial(truststore, null);
        }

        return sslContextBuilder.build();
    }

    @Bean
    public Boolean isX509Configured() {
        return keystorePath != null && keystorePassword != null && trustStorePath != null && trustStorePassword != null;
    }

}




