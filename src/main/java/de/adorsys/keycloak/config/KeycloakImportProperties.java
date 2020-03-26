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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Component
@ConfigurationProperties(prefix = "keycloak")
@Validated
public class KeycloakImportProperties {

    @NotNull
    @Size(min = 1)
    private String realm;

    @NotNull
    @Size(min = 1)
    private String clientId;

    @NotNull
    @Size(min = 1)
    @Pattern(regexp = "https?://.+")
    private String url;

    @NotNull
    @Size(min = 1)
    private String user;

    @NotNull
    @Size(min = 1)
    private String password;

    @NotNull
    @Size(min = 1)
    private String migrationKey;

    @NotNull
    private boolean sslVerify = false;

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
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

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMigrationKey() {
        return migrationKey;
    }

    public void setMigrationKey(String migrationKey) {
        this.migrationKey = migrationKey;
    }

    public Boolean getSslVerify() {
        return sslVerify;
    }

    public void setSslVerify(Boolean sslVerify) {
        this.sslVerify = sslVerify;
    }
}
