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

package de.adorsys.keycloak.config.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import de.adorsys.keycloak.config.exception.ImportProcessingException;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class RealmImport extends RealmRepresentation {

    private CustomImport customImport;

    private List<AuthenticationFlowImport> authenticationFlowImports;

    private List<UserImport> userImports;

    private RolesImport rolesImport = new RolesImport();

    private String checksum;

    public Optional<CustomImport> getCustomImport() {
        return Optional.ofNullable(customImport);
    }

    /**
     * Override getter to make sure we never get null from import
     */
    @Override
    public MultivaluedHashMap<String, ComponentExportRepresentation> getComponents() {
        MultivaluedHashMap<String, ComponentExportRepresentation> components = super.getComponents();
        if (components == null) {
            return new MultivaluedHashMap<>();
        }

        return components;
    }

    @Override
    public List<AuthenticationFlowRepresentation> getAuthenticationFlows() {
        if (authenticationFlowImports == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(authenticationFlowImports);
    }

    @Override
    public List<UserRepresentation> getUsers() {
        if (userImports == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(userImports);
    }

    @JsonSetter("users")
    public void setUserImports(List<UserImport> users) {
        this.userImports = users;
    }

    @JsonSetter("authenticationFlows")
    public void setAuthenticationFlowImports(List<AuthenticationFlowImport> authenticationFlowImports) {
        this.authenticationFlowImports = authenticationFlowImports;
    }

    @Override
    public List<AuthenticatorConfigRepresentation> getAuthenticatorConfig() {
        List<AuthenticatorConfigRepresentation> authenticatorConfig = super.getAuthenticatorConfig();

        if (authenticatorConfig == null) {
            authenticatorConfig = Collections.emptyList();
        }

        return authenticatorConfig;
    }

    @JsonIgnore
    public List<AuthenticationFlowRepresentation> getTopLevelFlows() {
        return this.getAuthenticationFlows()
                .stream()
                .filter(AuthenticationFlowRepresentation::isTopLevel)
                .collect(Collectors.toList());
    }

    @JsonIgnore
    public List<AuthenticationFlowRepresentation> getNonTopLevelFlowsForTopLevelFlow(AuthenticationFlowRepresentation topLevelFlow) {
        return topLevelFlow.getAuthenticationExecutions()
                .stream()
                .filter(AbstractAuthenticationExecutionRepresentation::isAutheticatorFlow)
                .map(AuthenticationExecutionExportRepresentation::getFlowAlias)
                .map(this::getNonTopLevelFlow)
                .collect(Collectors.toList());
    }

    @JsonIgnore
    public AuthenticationFlowRepresentation getNonTopLevelFlow(String alias) {
        Optional<AuthenticationFlowRepresentation> maybeNonTopLevelFlow = tryToGetNonTopLevelFlow(alias);

        if (!maybeNonTopLevelFlow.isPresent()) {
            throw new ImportProcessingException("Non-toplevel flow not found: " + alias);
        }

        return maybeNonTopLevelFlow.get();
    }

    @Override
    public RolesRepresentation getRoles() {
        return rolesImport;
    }

    @JsonSetter("roles")
    public void setRolesImport(RolesImport rolesImport) {
        this.rolesImport = rolesImport;
    }

    @Override
    public List<ClientRepresentation> getClients() {
        List<ClientRepresentation> clients = super.getClients();

        if (clients == null) {
            return Collections.emptyList();
        }

        return clients;
    }

    private Optional<AuthenticationFlowRepresentation> tryToGetNonTopLevelFlow(String alias) {
        return this.getNonTopLevelFlows()
                .stream()
                .filter(f -> f.getAlias().equals(alias))
                .findFirst();
    }

    private List<AuthenticationFlowRepresentation> getNonTopLevelFlows() {
        return this.getAuthenticationFlows()
                .stream()
                .filter(f -> !f.isTopLevel())
                .collect(Collectors.toList());
    }

    @JsonIgnore
    public String getChecksum() {
        return checksum;
    }

    @JsonIgnore
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public static class CustomImport {
        @JsonProperty("removeImpersonation")
        private Boolean removeImpersonation;

        public Boolean removeImpersonation() {
            return removeImpersonation != null && removeImpersonation;
        }
    }
}
