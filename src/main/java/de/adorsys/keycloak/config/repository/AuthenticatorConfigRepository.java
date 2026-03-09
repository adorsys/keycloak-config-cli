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

package de.adorsys.keycloak.config.repository;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import jakarta.ws.rs.NotFoundException;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class AuthenticatorConfigRepository {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticatorConfigRepository.class);
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

    public List<AuthenticatorConfigRepresentation> getConfigsByAlias(String realmName, String alias) {
        RealmRepresentation realmExport = realmRepository.partialExport(realmName, false, false);
        return realmExport.getAuthenticatorConfig()
                .stream()
                .filter(flow -> Objects.equals(flow.getAlias(), alias))
                .toList();
    }

    public void delete(String realmName, String id) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlowResources(realmName);
        try {
            flowsResource.removeAuthenticatorConfig(id);
        } catch (NotFoundException ex) {
            //ignore already missing Authenticator config.
            //some AuthenticatorConfig created for script have no real config #1382
            logger.info("AuthenticatorConfig with id '{}' on realm '{}' not found. Skipping deletion.", id, realmName);
        }
    }

    public void create(
            String realmName,
            String executionId,
            AuthenticatorConfigRepresentation authenticatorConfigRepresentation
    ) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlowResources(realmName);
        flowsResource.newExecutionConfig(executionId, authenticatorConfigRepresentation);
    }

    public void update(
            String realmName,
            AuthenticatorConfigRepresentation authenticatorConfigRepresentation
    ) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlowResources(realmName);
        flowsResource.updateAuthenticatorConfig(authenticatorConfigRepresentation.getId(), authenticatorConfigRepresentation);
    }

    public List<AuthenticatorConfigRepresentation> getAll(String realmName) {
        RealmRepresentation realmExport = realmRepository.partialExport(realmName, false, false);
        return realmExport.getAuthenticatorConfig();
    }
}
