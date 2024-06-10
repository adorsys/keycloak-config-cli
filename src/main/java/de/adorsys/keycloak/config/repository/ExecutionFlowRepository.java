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

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@Service
public class ExecutionFlowRepository {
    private static final Logger logger = LoggerFactory.getLogger(ExecutionFlowRepository.class);

    private final AuthenticationFlowRepository authenticationFlowRepository;

    @Autowired
    public ExecutionFlowRepository(AuthenticationFlowRepository authenticationFlowRepository) {
        this.authenticationFlowRepository = authenticationFlowRepository;
    }

    public List<AuthenticationExecutionInfoRepresentation> getExecutionFlowsByAlias(
            String realmName,
            String topLevelFlowAlias,
            AuthenticationExecutionExportRepresentation execution) {
        List<AuthenticationExecutionInfoRepresentation> executions = searchByAlias(
                realmName, topLevelFlowAlias, execution.getAuthenticator(), execution.getFlowAlias());

        if (executions.isEmpty()) {
            String withSubFlow = execution.getFlowAlias() != null
                    ? "' or flow by alias '" + execution.getFlowAlias()
                    : "";

            throw new KeycloakRepositoryException(
                    "Cannot find stored execution by authenticator '%s%s' in top-level flow '%s' in realm '%s'",
                    execution.getAuthenticator(), withSubFlow, topLevelFlowAlias, realmName
            );
        }
        return executions;
    }

    public List<AuthenticationExecutionInfoRepresentation> getExecutionsByAuthFlow(
            String realmName,
            String topLevelFlowAlias
    ) {
        return authenticationFlowRepository.getFlowResources(realmName).getExecutions(topLevelFlowAlias);
    }

    public void createExecutionFlow(
            String realmName,
            String topLevelFlowAlias,
            Map<String, String> executionFlowData
    ) {
        logger.trace("Create non-top-level-flow in realm '{}' and top-level-flow '{}'", realmName, topLevelFlowAlias);

        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlowResources(realmName);
        flowsResource.addExecutionFlow(topLevelFlowAlias, new HashMap<>(executionFlowData));
    }

    public void updateExecutionFlow(
            String realmName,
            String flowAlias,
            AuthenticationExecutionInfoRepresentation executionFlowToUpdate
    ) {
        logger.trace("Update non-top-level-flow '{}' from realm '{}' and top-level-flow '{}'",
                executionFlowToUpdate.getAlias(), realmName, flowAlias);

        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlowResources(realmName);
        flowsResource.updateExecutions(flowAlias, executionFlowToUpdate);
    }

    public String createTopLevelFlowExecution(
            String realmName,
            AuthenticationExecutionRepresentation executionToCreate
    ) {
        logger.trace("Create flow-execution '{}' in realm '{}' and top-level-flow '{}'...",
                executionToCreate.getAuthenticator(), realmName, executionToCreate.getParentFlow());

        AuthenticationManagementResource flowsResource = authenticationFlowRepository
                .getFlowResources(realmName);

        try (Response response = flowsResource.addExecution(executionToCreate)) {
            return CreatedResponseUtil.getCreatedId(response);
        } catch (WebApplicationException error) {
            AuthenticationFlowRepresentation parentFlow = authenticationFlowRepository
                    .getFlowById(realmName, executionToCreate.getParentFlow());

            throw new ImportProcessingException(
                    String.format(
                            "Cannot create execution-flow '%s' for top-level-flow '%s' in realm '%s'",
                            executionToCreate.getAuthenticator(), parentFlow.getAlias(), realmName
                    ),
                    error
            );
        }
    }

    public void createSubFlowExecution(
            String realmName,
            String subFlowAlias,
            Map<String, String> executionData
    ) {
        logger.trace("Create flow-execution in realm '{}' and non-top-level-flow '{}'",
                realmName, subFlowAlias);

        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlowResources(realmName);
        flowsResource.addExecution(subFlowAlias, new HashMap<>(executionData));

        logger.trace("Created flow-execution in realm '{}' and non-top-level-flow '{}'",
                realmName, subFlowAlias);
    }

    private List<AuthenticationExecutionInfoRepresentation> searchByAlias(
            String realmName,
            String topLevelFlowAlias,
            String executionProviderId,
            String subFlowAlias
    ) {
        return getExecutionsByAuthFlow(realmName, topLevelFlowAlias)
                .stream()
                .filter(f -> Objects.equals(f.getProviderId(), executionProviderId))
                .filter(f -> {
                    if (subFlowAlias != null) {
                        return Objects.equals(f.getDisplayName(), subFlowAlias);
                    }
                    return true;
                })
                .toList();
    }
}
