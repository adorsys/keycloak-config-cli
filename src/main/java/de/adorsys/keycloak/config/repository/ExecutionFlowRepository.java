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

import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.util.ResponseUtil;
import org.jboss.logging.Logger;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Dependent
public class ExecutionFlowRepository {
    private static final Logger LOG = Logger.getLogger(ExecutionFlowRepository.class);

    @Inject
    AuthenticationFlowRepository authenticationFlowRepository;

    public AuthenticationExecutionInfoRepresentation getExecutionFlow(String realm, String topLevelFlowAlias, String executionProviderId) {
        Optional<AuthenticationExecutionInfoRepresentation> maybeExecution = tryToGetExecutionFlow(realm, topLevelFlowAlias, executionProviderId);

        if (maybeExecution.isPresent()) {
            return maybeExecution.get();
        }

        throw new KeycloakRepositoryException("Cannot find stored execution-flow by alias '" + executionProviderId + "' in top-level flow '" + topLevelFlowAlias + "' in realm '" + realm + "'");
    }

    public Optional<AuthenticationExecutionInfoRepresentation> tryToGetNonTopLevelFlow(String realm, String topLevelFlowAlias, String nonTopLevelFlowAlias) {
        LOG.tracef("Try to get non-top-level-flow '%s' from realm '%s' and top-level-flow '%s'", nonTopLevelFlowAlias, realm, topLevelFlowAlias);

        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm);

        return flowsResource.getExecutions(topLevelFlowAlias)
                .stream()
                /* we have to compare the display name with the alias, because the alias property in
                 AuthenticationExecutionInfoRepresentation representations is always set to null. */
                .filter(f -> f.getDisplayName().equals(nonTopLevelFlowAlias))
                .findFirst();
    }

    public void createExecutionFlow(String realm, String topLevelFlowAlias, Map<String, String> executionFlowData) {
        LOG.tracef("Create non-top-level-flow in realm '%s' and top-level-flow '%s'", realm, topLevelFlowAlias);

        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm);
        flowsResource.addExecutionFlow(topLevelFlowAlias, executionFlowData);
    }

    public void updateExecutionFlow(String realm, String flowAlias, AuthenticationExecutionInfoRepresentation executionFlowToUpdate) {
        LOG.tracef("Update non-top-level-flow '%s' from realm '%s' and top-level-flow '%s'", executionFlowToUpdate.getAlias(), realm, flowAlias);

        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm);
        flowsResource.updateExecutions(flowAlias, executionFlowToUpdate);
    }

    public void createTopLevelFlowExecution(String realm, AuthenticationExecutionRepresentation executionToCreate) {
        LOG.tracef("Create flow-execution '%s' in realm '%s' and top-level-flow '%s'...", executionToCreate.getAuthenticator(), realm, executionToCreate.getParentFlow());

        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm);

        Response response = flowsResource.addExecution(executionToCreate);
        ResponseUtil.throwOnError(response);

        LOG.tracef("Created flow-execution '%s' in realm '%s' and top-level-flow '%s'", executionToCreate.getAuthenticator(), realm, executionToCreate.getParentFlow());
    }

    public void createNonTopLevelFlowExecution(String realm, String nonTopLevelFlowAlias, Map<String, String> executionData) {
        LOG.tracef("Create flow-execution in realm '%s' and non-top-level-flow '%s'...", realm, nonTopLevelFlowAlias);

        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm);
        flowsResource.addExecution(nonTopLevelFlowAlias, executionData);

        LOG.tracef("Created flow-execution in realm '%s' and non-top-level-flow '%s'", realm, nonTopLevelFlowAlias);
    }

    private Optional<AuthenticationExecutionInfoRepresentation> tryToGetExecutionFlow(String realm, String topLevelFlowAlias, String executionProviderId) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm);

        return flowsResource.getExecutions(topLevelFlowAlias)
                .stream()
                .filter(f -> Objects.equals(f.getProviderId(), executionProviderId))
                .findFirst();
    }
}
