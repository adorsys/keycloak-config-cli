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

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.exception.InvalidImportException;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.AuthenticatorConfigRepository;
import de.adorsys.keycloak.config.repository.ExecutionFlowRepository;
import de.adorsys.keycloak.config.util.AuthenticationFlowUtil;
import de.adorsys.keycloak.config.util.ResponseUtil;
import org.keycloak.representations.idm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.ws.rs.WebApplicationException;

/**
 * Imports executions and execution-flows of existing top-level flows
 */
@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class ExecutionFlowsImportService {
    private static final Logger logger = LoggerFactory.getLogger(ExecutionFlowsImportService.class);

    private final ExecutionFlowRepository executionFlowRepository;
    private final AuthenticatorConfigRepository authenticatorConfigRepository;

    @Autowired
    public ExecutionFlowsImportService(
            ExecutionFlowRepository executionFlowRepository,
            AuthenticatorConfigRepository authenticatorConfigRepository
    ) {
        this.executionFlowRepository = executionFlowRepository;
        this.authenticatorConfigRepository = authenticatorConfigRepository;
    }

    public void createExecutionsAndExecutionFlows(
            RealmImport realmImport,
            AuthenticationFlowRepresentation topLevelFlowToImport,
            AuthenticationFlowRepresentation existingTopLevelFlow
    ) {
        for (AuthenticationExecutionExportRepresentation executionToImport : topLevelFlowToImport.getAuthenticationExecutions()) {
            createExecutionOrExecutionFlow(realmImport, topLevelFlowToImport, existingTopLevelFlow, executionToImport);
        }
    }

    public void updateExecutionFlows(
            RealmImport realmImport,
            AuthenticationFlowRepresentation flowToImport
    ) {
        for (AuthenticationExecutionExportRepresentation execution : flowToImport.getAuthenticationExecutions()) {
            configureExecutionFlow(realmImport, flowToImport, execution);
        }
    }

    @SuppressWarnings("deprecation")
    private void createExecutionOrExecutionFlow(
            RealmImport realmImport,
            AuthenticationFlowRepresentation topLevelFlowToImport,
            AuthenticationFlowRepresentation existingTopLevelFlow,
            AuthenticationExecutionExportRepresentation executionOrExecutionFlowToImport
    ) {
        if (executionOrExecutionFlowToImport.isAutheticatorFlow()) {
            createAndConfigureExecutionFlow(realmImport, topLevelFlowToImport, executionOrExecutionFlowToImport);
        } else {
            createExecutionForTopLevelFlow(realmImport, existingTopLevelFlow, executionOrExecutionFlowToImport);
        }
    }

    private void createAndConfigureExecutionFlow(
            RealmImport realmImport,
            AuthenticationFlowRepresentation topLevelFlowToImport,
            AuthenticationExecutionExportRepresentation executionFlowToImport
    ) {
        AuthenticationFlowRepresentation subFlowToImport = AuthenticationFlowUtil
                .getSubFlow(realmImport, executionFlowToImport.getFlowAlias());

        createSubFlowByExecutionFlow(realmImport, topLevelFlowToImport, executionFlowToImport, subFlowToImport);
        configureExecutionFlow(realmImport, topLevelFlowToImport, executionFlowToImport);

        createExecutionAndExecutionFlowsForSubFlows(realmImport, subFlowToImport);
    }

    @SuppressWarnings("deprecation")
    private void createExecutionForTopLevelFlow(
            RealmImport realmImport,
            AuthenticationFlowRepresentation existingTopLevelFlow,
            AuthenticationExecutionExportRepresentation executionToImport
    ) {
        logger.debug("Creating execution '{}' for top-level-flow: '{}' in realm '{}'",
                executionToImport.getAuthenticator(), existingTopLevelFlow.getAlias(), realmImport.getRealm()
        );

        AuthenticationExecutionRepresentation executionToCreate = new AuthenticationExecutionRepresentation();

        executionToCreate.setParentFlow(existingTopLevelFlow.getId());
        executionToCreate.setAuthenticator(executionToImport.getAuthenticator());
        executionToCreate.setRequirement(executionToImport.getRequirement());
        executionToCreate.setPriority(executionToImport.getPriority());
        executionToCreate.setAutheticatorFlow(false);

        String executionId = executionFlowRepository.createTopLevelFlowExecution(realmImport.getRealm(), executionToCreate);

        if (executionToImport.getAuthenticatorConfig() != null) {
            createAuthenticatorConfig(realmImport, executionToImport.getAuthenticatorConfig(), executionId);
        }
    }

    /**
     * Creates the executionFlow within the topLevel-flow AND creates the non-topLevel flow because keycloak does
     * this automatically while calling `flowsResource.addExecutionFlow`
     */
    private void createSubFlowByExecutionFlow(
            RealmImport realmImport,
            AuthenticationFlowRepresentation topLevelFlowToImport,
            AuthenticationExecutionExportRepresentation executionToImport,
            AuthenticationFlowRepresentation subFlow
    ) {
        logger.debug("Creating non-top-level-flow '{}' for top-level-flow '{}' by its execution '{}' in realm '{}'",
                subFlow.getAlias(), topLevelFlowToImport.getAlias(),
                executionToImport.getFlowAlias(), realmImport.getRealm()
        );

        if (!Objects.equals(executionToImport.getAuthenticator(), null) && !Objects.equals(subFlow.getProviderId(), "form-flow")) {
            throw new InvalidImportException(String.format(
                    "Execution property authenticator '%s' can be only set if the sub-flow '%s' type is 'form-flow'.",
                    executionToImport.getAuthenticator(), subFlow.getAlias()
            ));
        }

        HashMap<String, String> executionFlow = new HashMap<>();
        executionFlow.put("alias", executionToImport.getFlowAlias());
        executionFlow.put("provider", executionToImport.getAuthenticator());
        executionFlow.put("type", subFlow.getProviderId());
        executionFlow.put("description", subFlow.getDescription());
        executionFlow.put("authenticator", subFlow.getProviderId());

        try {
            executionFlowRepository.createExecutionFlow(realmImport.getRealm(), topLevelFlowToImport.getAlias(), executionFlow);
        } catch (WebApplicationException error) {
            String errorMessage = ResponseUtil.getErrorMessage(error);
            throw new ImportProcessingException(
                    String.format(
                            "Cannot create execution-flow '%s' for top-level-flow '%s' in realm '%s': %s",
                            executionToImport.getFlowAlias(), topLevelFlowToImport.getAlias(),
                            realmImport.getRealm(), errorMessage
                    ),
                    error
            );
        }
    }

    /**
     * We have to re-configure the requirement property separately as long as keycloak is only allowing to set the 'provider'
     * and is ignoring the value and sets the requirement hardcoded to DISABLED while creating execution-flow.
     *
     * @see #createExecutionForSubFlow
     */
    private void configureExecutionFlow(
            RealmImport realmImport,
            AuthenticationFlowRepresentation topLevelOrSubFlowToImport,
            AuthenticationExecutionExportRepresentation executionToImport
    ) {
        debugLogExecutionFlowCreation(realmImport, topLevelOrSubFlowToImport.getAlias(), executionToImport);

        List<AuthenticationExecutionInfoRepresentation> storedExecutionFlows = executionFlowRepository.getExecutionFlowsByAlias(
                realmImport.getRealm(), topLevelOrSubFlowToImport.getAlias(), executionToImport
        );

        if (storedExecutionFlows.size() != 1) {
            throw new ImportProcessingException(String.format(
                    "Unexpected size of execution %s in flow %s found.",
                    executionToImport.getAuthenticator(), topLevelOrSubFlowToImport.getAlias()
            ));
        }

        AuthenticationExecutionInfoRepresentation storedExecutionFlow = storedExecutionFlows.get(0);
        storedExecutionFlow.setRequirement(executionToImport.getRequirement());

        try {
            executionFlowRepository.updateExecutionFlow(
                    realmImport.getRealm(),
                    topLevelOrSubFlowToImport.getAlias(),
                    storedExecutionFlow
            );
        } catch (WebApplicationException error) {
            String errorMessage = ResponseUtil.getErrorMessage(error);
            throw new ImportProcessingException(
                    String.format(
                            "Cannot update execution-flow '%s' for flow '%s' in realm '%s': %s",
                            executionToImport.getAuthenticator(), topLevelOrSubFlowToImport.getAlias(),
                            realmImport.getRealm(), errorMessage
                    ),
                    error
            );
        }

        if (storedExecutionFlow.getAuthenticationConfig() == null
                && executionToImport.getAuthenticatorConfig() != null) {
            createAuthenticatorConfig(
                    realmImport,
                    executionToImport.getAuthenticatorConfig(),
                    storedExecutionFlow.getId()
            );
        }
    }

    @SuppressWarnings("deprecation")
    private void createExecutionAndExecutionFlowsForSubFlows(
            RealmImport realmImport,
            AuthenticationFlowRepresentation subFlow
    ) {
        for (AuthenticationExecutionExportRepresentation executionOrExecutionFlowToImport : subFlow.getAuthenticationExecutions()) {

            if (executionOrExecutionFlowToImport.isAutheticatorFlow()) {
                createAndConfigureExecutionFlow(realmImport, subFlow, executionOrExecutionFlowToImport);
            } else {
                createExecutionForSubFlow(realmImport, subFlow, executionOrExecutionFlowToImport);
                configureExecutionFlow(realmImport, subFlow, executionOrExecutionFlowToImport);
            }
        }
    }

    /**
     * Keycloak is only allowing to set the 'provider' property while creating an execution. The other properties have
     * to be set afterwards with an update.
     *
     * @see #configureExecutionFlow
     */
    private void createExecutionForSubFlow(
            RealmImport realmImport,
            AuthenticationFlowRepresentation subFlow,
            AuthenticationExecutionExportRepresentation executionToImport
    ) {
        logger.debug("Create execution '{}' for non-top-level-flow '{}' in realm '{}'",
                executionToImport.getAuthenticator(), subFlow.getAlias(), realmImport.getRealm());

        HashMap<String, String> execution = new HashMap<>();
        execution.put("provider", executionToImport.getAuthenticator());

        try {
            executionFlowRepository.createSubFlowExecution(realmImport.getRealm(), subFlow.getAlias(), execution);
        } catch (WebApplicationException error) {
            String errorMessage = ResponseUtil.getErrorMessage(error);
            throw new ImportProcessingException(
                    String.format(
                            "Cannot create execution '%s' for non-top-level-flow '%s' in realm '%s': %s",
                            executionToImport.getAuthenticator(), subFlow.getAlias(),
                            realmImport.getRealm(), errorMessage),
                    error
            );
        }

        if (executionToImport.getAuthenticatorConfig() != null) {
            List<AuthenticationExecutionInfoRepresentation> executionFlows = executionFlowRepository
                    .getExecutionFlowsByAlias(
                            realmImport.getRealm(),
                            subFlow.getAlias(),
                            executionToImport
                    )
                    .stream()
                    .filter(flow -> flow.getAuthenticationConfig() == null)
                    .toList();

            if (executionFlows.size() != 1) {
                throw new ImportProcessingException(
                        String.format(
                                "Unexpected size of execution %s in flow %s. Expected: 1. Actual: %d",
                                executionToImport.getAuthenticator(), subFlow.getAlias(), executionFlows.size()
                        )
                );
            }

            createAuthenticatorConfig(
                    realmImport,
                    executionToImport.getAuthenticatorConfig(),
                    executionFlows.get(0).getId()
            );
        }
    }

    private void createAuthenticatorConfig(
            RealmImport realmImport,
            String authenticatorConfigName,
            String flowExecutionId
    ) {
        AuthenticatorConfigRepresentation authenticatorConfig = realmImport
                .getAuthenticatorConfig()
                .stream()
                .filter(x -> Objects.equals(x.getAlias(), authenticatorConfigName))
                .findAny()
                .orElseThrow(() -> new ImportProcessingException(
                        String.format("Authenticator config '%s' definition not found", authenticatorConfigName)
                ));

        authenticatorConfigRepository.create(
                realmImport.getRealm(),
                flowExecutionId,
                authenticatorConfig
        );
    }

    private void debugLogExecutionFlowCreation(
            RealmImport realmImport,
            String authenticationFlowAlias,
            AuthenticationExecutionExportRepresentation executionToImport
    ) {
        if (logger.isDebugEnabled()) {
            String execution = Optional.ofNullable(executionToImport.getFlowAlias())
                    .orElse(executionToImport.getAuthenticator());
            logger.debug("Configuring execution-flow '{}' for authentication-flow '{}' in realm '{}'",
                    execution, authenticationFlowAlias, realmImport.getRealm());
        }
    }
}
