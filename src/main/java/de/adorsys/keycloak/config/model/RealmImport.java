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

package de.adorsys.keycloak.config.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class RealmImport extends RealmRepresentation {
    private List<AuthenticationFlowImport> authenticationFlowImports;

    private UPConfig userProfile;

    private Map<String, Map<String, String>> messageBundles;

    private String checksum;
    private String source;

    @Override
    @SuppressWarnings("java:S1168")
    public List<AuthenticationFlowRepresentation> getAuthenticationFlows() {
        if (authenticationFlowImports == null) return null;

        return new ArrayList<>(authenticationFlowImports);
    }

    @SuppressWarnings("unused")
    @JsonSetter("authenticationFlows")
    public void setAuthenticationFlowImports(List<AuthenticationFlowImport> authenticationFlowImports) {
        this.authenticationFlowImports = authenticationFlowImports;
    }

    @SuppressWarnings("unused")
    @JsonSetter("userProfile")
    public void setUserProfile(UPConfig userProfile) {
        this.userProfile = userProfile;
    }

    public Map<String, Map<String, String>> getMessageBundles() {
        return messageBundles;
    }

    @SuppressWarnings("unused")
    @JsonSetter("messageBundles")
    public void setMessageBundles(Map<String, Map<String, String>> messageBundles) {
        this.messageBundles = messageBundles;
    }

    public UPConfig getUserProfile() {
        return userProfile;
    }

    @JsonIgnore
    public String getChecksum() {
        return checksum;
    }

    @JsonIgnore
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    @JsonIgnore
    public String getSource() {
        return source;
    }

    @JsonIgnore
    public void setSource(String source) {
        this.source = source;
    }
}
