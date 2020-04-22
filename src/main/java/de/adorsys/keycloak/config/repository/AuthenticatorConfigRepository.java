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
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.util.ResponseUtil;
import org.checkerframework.checker.units.qual.A;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.representations.idm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class AuthenticatorConfigRepository {
    private static final Logger logger = LoggerFactory.getLogger(ExecutionFlowRepository.class);

    private final AuthenticationFlowRepository authenticationFlowRepository;
    private final RealmRepository realmRepository;

    @Autowired
    public AuthenticatorConfigRepository(
            AuthenticationFlowRepository authenticationFlowRepository,
            RealmRepository realmRepository
    ) {
        this.authenticationFlowRepository = authenticationFlowRepository;
        this.realmRepository = realmRepository;
    }

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

    public List<AuthenticatorConfigRepresentation> getAll(String realm){
        RealmRepresentation realmExport = realmRepository.partialExport(realm);
        return realmExport.getAuthenticatorConfig();
    }
}