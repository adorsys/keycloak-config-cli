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
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@Service
public class AuthenticationFlowRepository {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFlowRepository.class);

    private final RealmRepository realmRepository;

    @Autowired
    public AuthenticationFlowRepository(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    public Optional<AuthenticationFlowRepresentation> tryToGetTopLevelFlow(String realm, String alias) {
        logger.trace("Try to get top-level-flow '{}' from realm '{}'", alias, realm);

        // with `AuthenticationManagementResource.getFlows()` keycloak is NOT returning all so-called top-level-flows so
        // we need a partial export
        RealmRepresentation realmExport = realmRepository.partialExport(realm);
        return realmExport.getAuthenticationFlows()
                .stream()
                .filter(flow -> flow.getAlias().equals(alias))
                .findFirst();
    }

    public AuthenticationFlowRepresentation getTopLevelFlow(String realm, String alias) {
        Optional<AuthenticationFlowRepresentation> maybeTopLevelFlow = tryToGetTopLevelFlow(realm, alias);

        if (maybeTopLevelFlow.isPresent()) {
            return maybeTopLevelFlow.get();
        }

        throw new KeycloakRepositoryException("Cannot find top-level flow: " + alias);
    }

    /**
     * creates only the top-level flow WITHOUT its executions or execution-flows
     */
    public void createTopLevelFlow(String realm, AuthenticationFlowRepresentation topLevelFlowToImport) {
        logger.trace("Create top-level-flow '{}' in realm '{}'", topLevelFlowToImport.getAlias(), realm);

        AuthenticationManagementResource flowsResource = getFlows(realm);
        Response response = flowsResource.createFlow(topLevelFlowToImport);

        ResponseUtil.throwOnError(response);
    }

    public AuthenticationFlowRepresentation getFlowById(String realm, String id) {
        logger.trace("Get flow by id '{}' in realm '{}'", id, realm);

        AuthenticationManagementResource flowsResource = getFlows(realm);
        return flowsResource.getFlow(id);
    }

    public void deleteTopLevelFlow(String realm, String topLevelFlowId) {
        AuthenticationManagementResource flowsResource = getFlows(realm);

        try {
            flowsResource.deleteFlow(topLevelFlowId);
        } catch (ClientErrorException e) {
            throw new ImportProcessingException("Error occurred while trying to delete top-level-flow by id '" + topLevelFlowId + "' in realm '" + realm + "'", e);
        }
    }

    AuthenticationManagementResource getFlows(String realm) {
        logger.trace("Get flows-resource for realm '{}'...", realm);

        RealmResource realmResource = realmRepository.loadRealm(realm);
        AuthenticationManagementResource flows = realmResource.flows();

        logger.trace("Got flows-resource for realm '{}'", realm);

        return flows;
    }

    public List<AuthenticationFlowRepresentation> getTopLevelFlows(String realm) {
        AuthenticationManagementResource flowsResource = getFlows(realm);
        return flowsResource.getFlows();
    }

    public List<AuthenticationFlowRepresentation> getAll(String realm) {
        RealmRepresentation realmExport = realmRepository.partialExport(realm);
        return realmExport.getAuthenticationFlows();
    }
}
