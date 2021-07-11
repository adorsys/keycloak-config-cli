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

package de.adorsys.keycloak.config.operator.spec;

import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class SchemaSpec {
    @NotNull
    private KeycloakConfigPropertiesSpec keycloakConnection;
    @NotNull
    private String realmConfiguration;

    public KeycloakConfigPropertiesSpec getKeycloakConnection() {
        return keycloakConnection;
    }

    public void setKeycloakConnection(KeycloakConfigPropertiesSpec keycloakConnection) {
        this.keycloakConnection = keycloakConnection;
    }

    public String getRealmConfiguration() {
        return realmConfiguration;
    }

    public void setRealmConfiguration(String realmConfiguration) {
        this.realmConfiguration = realmConfiguration;
    }

    public static class KeycloakConfigPropertiesSpec {
        @NotBlank
        private String loginRealm = "master";
        @NotBlank
        private String clientId = "admin-cli";
        @NotNull
        private String url;
        @NotNull
        private String credentialSecret;
        @NotBlank
        private String grantType = "password";
        @NotNull
        private boolean sslVerify = true;

        private KeycloakConfigProperties.KeycloakAvailabilityCheck availabilityCheck;

        public String getLoginRealm() {
            return loginRealm;
        }

        public void setLoginRealm(String loginRealm) {
            this.loginRealm = loginRealm;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getCredentialSecret() {
            return credentialSecret;
        }

        public void setCredentialSecret(String credentialSecret) {
            this.credentialSecret = credentialSecret;
        }

        public String getGrantType() {
            return grantType;
        }

        public void setGrantType(String grantType) {
            this.grantType = grantType;
        }

        public boolean isSslVerify() {
            return sslVerify;
        }

        public void setSslVerify(boolean sslVerify) {
            this.sslVerify = sslVerify;
        }

        public KeycloakConfigProperties.KeycloakAvailabilityCheck getAvailabilityCheck() {
            return availabilityCheck;
        }

        public void setAvailabilityCheck(KeycloakConfigProperties.KeycloakAvailabilityCheck availabilityCheck) {
            this.availabilityCheck = availabilityCheck;
        }
    }
}
