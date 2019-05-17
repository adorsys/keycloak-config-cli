package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.ExecutionFlowRepository;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.Optional;

/**
 * Imports executions and execution-flows of existing top-level flows
 */
@Service
public class ExecutionFlowsImportService {
    private static final Logger logger = LoggerFactory.getLogger(ExecutionFlowsImportService.class);

    private final ExecutionFlowRepository executionFlowRepository;

    @Autowired
    public ExecutionFlowsImportService(
            ExecutionFlowRepository executionFlowRepository
    ) {
        this.executionFlowRepository = executionFlowRepository;
    }

    public void createExecutionsAndExecutionFlows(
            RealmImport realm,
            AuthenticationFlowRepresentation topLevelFlowToImport,
            AuthenticationFlowRepresentation existingTopLevelFlow
    ) {
        for (AuthenticationExecutionExportRepresentation executionToImport : topLevelFlowToImport.getAuthenticationExecutions()) {
            createExecutionOrExecutionFlow(realm, topLevelFlowToImport, existingTopLevelFlow, executionToImport);
        }
    }

    private void createExecutionOrExecutionFlow(
            RealmImport realm,
            AuthenticationFlowRepresentation topLevelFlowToImport,
            AuthenticationFlowRepresentation existingTopLevelFlow,
            AuthenticationExecutionExportRepresentation executionOrExecutionFlowToImport
    ) {
        if (executionOrExecutionFlowToImport.isAutheticatorFlow()) {
            createAndConfigureExecutionFlow(realm, topLevelFlowToImport, executionOrExecutionFlowToImport);
        } else {
            createExecutionForTopLevelFlow(realm, existingTopLevelFlow, executionOrExecutionFlowToImport);
        }
    }

    private void createAndConfigureExecutionFlow(
            RealmImport realm,
            AuthenticationFlowRepresentation topLevelFlowToImport,
            AuthenticationExecutionExportRepresentation executionFlowToImport
    ) {
        AuthenticationFlowRepresentation nonTopLevelFlowToImport = realm.getNonTopLevelFlow(executionFlowToImport.getFlowAlias());

        createNonTopLevelFlowByExecutionFlow(realm, topLevelFlowToImport, executionFlowToImport, nonTopLevelFlowToImport);
        configureExecutionFlow(realm, topLevelFlowToImport, executionFlowToImport);

        createExecutionAndExecutionFlowsForNonTopLevelFlows(realm, nonTopLevelFlowToImport);
    }

    private void createExecutionForTopLevelFlow(
            RealmImport realm,
            AuthenticationFlowRepresentation existingTopLevelFlow,
            AuthenticationExecutionExportRepresentation executionToImport
    ) {
        logger.debug("Creating execution '{}' for top-level-flow: '{}' in realm '{}'", executionToImport.getAuthenticator(), existingTopLevelFlow.getAlias(), realm.getRealm());

        AuthenticationExecutionRepresentation executionToCreate = new AuthenticationExecutionRepresentation();

        executionToCreate.setParentFlow(existingTopLevelFlow.getId());
        executionToCreate.setAuthenticator(executionToImport.getAuthenticator());
        executionToCreate.setRequirement(executionToImport.getRequirement());
        executionToCreate.setPriority(executionToImport.getPriority());
        executionToCreate.setAutheticatorFlow(false);

        try {
            executionFlowRepository.createTopLevelFlowExecution(realm.getRealm(), executionToCreate);
        } catch (KeycloakRepositoryException error) {
            throw new ImportProcessingException(
                    "Cannot create execution-flow '" + executionToImport.getAuthenticator()
                            + "' for top-level-flow '" + existingTopLevelFlow.getAlias()
                            + "' for realm '" + realm.getRealm() + "'",
                    error
            );
        }
    }

    /**
     * Creates the executionFlow within the topLevel-flow AND creates the non-topLevel flow because keycloak does
     * this automatically while calling `flowsResource.addExecutionFlow`
     */
    private void createNonTopLevelFlowByExecutionFlow(
            RealmImport realm,
            AuthenticationFlowRepresentation topLevelFlowToImport,
            AuthenticationExecutionExportRepresentation executionToImport,
            AuthenticationFlowRepresentation nonTopLevelFlow
    ) {
        logger.debug("Creating non-top-level-flow '{}' for top-level-flow '{}' by its execution '{}' in realm '{}'", nonTopLevelFlow.getAlias(), topLevelFlowToImport.getAlias(), executionToImport.getFlowAlias(), realm.getRealm());

        HashMap<String, String> executionFlow = new HashMap<>();
        executionFlow.put("alias", executionToImport.getFlowAlias());
        executionFlow.put("provider", executionToImport.getAuthenticator());
        executionFlow.put("type", nonTopLevelFlow.getProviderId());
        executionFlow.put("description", nonTopLevelFlow.getDescription());
        executionFlow.put("authenticator", nonTopLevelFlow.getProviderId());

        try {
            executionFlowRepository.createExecutionFlow(realm.getRealm(), topLevelFlowToImport.getAlias(), executionFlow);
        } catch (WebApplicationException error) {
            throw new ImportProcessingException(
                    "Cannot create execution-flow '" + executionToImport.getFlowAlias()
                            + "' for top-level-flow '" + topLevelFlowToImport.getAlias()
                            + "' for realm '" + realm.getRealm() + "'",
                    error
            );
        }
    }

    /**
     * We have to re-configure the requirement property separately as long as keycloak is only allowing to set the 'provider'
     * and is ignoring the value and sets the requirement hardcoded to DISABLED while creating execution-flow.
     *
     * @see {@link #createExecutionForNonTopLevelFlow}
     */
    private void configureExecutionFlow(
            RealmImport realm,
            AuthenticationFlowRepresentation topLevelOrNonTopLevelFlowToImport,
            AuthenticationExecutionExportRepresentation executionToImport
    ) {
        debugLogExecutionFlowCreation(realm, topLevelOrNonTopLevelFlowToImport.getAlias(), executionToImport);

        AuthenticationExecutionInfoRepresentation storedExecutionFlow = executionFlowRepository.getExecutionFlow(
                realm.getRealm(), topLevelOrNonTopLevelFlowToImport.getAlias(), executionToImport.getAuthenticator()
        );

        storedExecutionFlow.setRequirement(executionToImport.getRequirement());

        try {
            executionFlowRepository.updateExecutionFlow(realm.getRealm(), topLevelOrNonTopLevelFlowToImport.getAlias(), storedExecutionFlow);
        } catch (WebApplicationException error) {
            throw new ImportProcessingException(
                    "Cannot update execution-flow '" + executionToImport.getAuthenticator()
                            + "' for flow '" + topLevelOrNonTopLevelFlowToImport.getAlias()
                            + "' for realm '" + realm.getRealm() + "'",
                    error
            );
        }
    }

    private void createExecutionAndExecutionFlowsForNonTopLevelFlows(RealmImport realm, AuthenticationFlowRepresentation nonTopLevelFlow) {

        for (AuthenticationExecutionExportRepresentation executionOrExecutionFlowToImport : nonTopLevelFlow.getAuthenticationExecutions()) {

            if (executionOrExecutionFlowToImport.isAutheticatorFlow()) {
                createAndConfigureExecutionFlow(realm, nonTopLevelFlow, executionOrExecutionFlowToImport);
            } else {
                createExecutionForNonTopLevelFlow(realm, nonTopLevelFlow, executionOrExecutionFlowToImport);
                configureExecutionFlow(realm, nonTopLevelFlow, executionOrExecutionFlowToImport);
            }
        }
    }

    /**
     * Keycloak is only allowing to set the 'provider' property while creating an execution. The other properties have
     * to be set afterwards with an update.
     *
     * @see {@link #configureExecutionFlow}
     */
    private void createExecutionForNonTopLevelFlow(
            RealmImport realm,
            AuthenticationFlowRepresentation nonTopLevelFlow,
            AuthenticationExecutionExportRepresentation executionToImport
    ) {
        logger.debug("Create execution '{}' for non-top-level-flow '{}' in realm '{}'", executionToImport.getAuthenticator(), nonTopLevelFlow.getAlias(), realm.getRealm());

        HashMap<String, String> execution = new HashMap<>();
        execution.put("provider", executionToImport.getAuthenticator());

        try {
            executionFlowRepository.createNonTopLevelFlowExecution(realm.getRealm(), nonTopLevelFlow.getAlias(), execution);
        } catch (WebApplicationException error) {
            throw new ImportProcessingException(
                    "Cannot create execution '" + executionToImport.getAuthenticator()
                            + "' for non-top-level-flow '" + nonTopLevelFlow.getAlias()
                            + "' for realm '" + realm.getRealm() + "'",
                    error
            );
        }
    }

    private void debugLogExecutionFlowCreation(RealmImport realm, String authenticationFlowAlias, AuthenticationExecutionExportRepresentation executionToImport) {
        if (logger.isDebugEnabled()) {
            String execution = Optional.ofNullable(executionToImport.getFlowAlias())
                    .orElse(executionToImport.getAuthenticator());
            logger.debug("Configuring execution-flow '{}' for authentication-flow '{}' in realm '{}'", execution, authenticationFlowAlias, realm.getRealm());
        }
    }
}
