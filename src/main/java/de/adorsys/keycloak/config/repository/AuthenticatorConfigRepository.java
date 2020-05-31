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

package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.util.List;

@Dependent
public class AuthenticatorConfigRepository {
    @Inject
    AuthenticationFlowRepository authenticationFlowRepository;

    @Inject
    RealmRepository realmRepository;

    public AuthenticatorConfigRepresentation getAuthenticatorConfig(String realm, String alias) {

        RealmRepresentation realmExport = realmRepository.partialExport(realm);
        return realmExport.getAuthenticatorConfig()
                .stream()
                .filter(flow -> flow.getAlias().equals(alias))
                .findFirst()
                .orElseThrow(() -> new ImportProcessingException("Authenticator Config '" + alias + "' not found. Config must be used in execution"));
    }

    public void deletedAuthenticatorConfig(String realm, String id) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm);
        flowsResource.removeAuthenticatorConfig(id);
    }

    public void createAuthenticatorConfig(
            String realm,
            String executionId,
            AuthenticatorConfigRepresentation authenticatorConfigRepresentation
    ) throws WebApplicationException {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm);
        flowsResource.newExecutionConfig(executionId, authenticatorConfigRepresentation);
    }

    public void updateAuthenticatorConfig(
            String realm,
            AuthenticatorConfigRepresentation authenticatorConfigRepresentation
    ) throws WebApplicationException {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm);
        flowsResource.updateAuthenticatorConfig(authenticatorConfigRepresentation.getId(), authenticatorConfigRepresentation);
    }

    public List<AuthenticatorConfigRepresentation> getAll(String realm) {
        RealmRepresentation realmExport = realmRepository.partialExport(realm);
        return realmExport.getAuthenticatorConfig();
    }
}
