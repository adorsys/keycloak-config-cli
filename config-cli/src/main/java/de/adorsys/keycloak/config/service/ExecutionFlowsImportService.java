package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.ExecutionFlowRepository;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.ServerErrorException;
import java.util.HashMap;

/**
 * Imports executions and execution-flows of existing top-level flows
 */
@Service
public class ExecutionFlowsImportService {

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
        AuthenticationExecutionRepresentation executionToCreate = new AuthenticationExecutionRepresentation();

        executionToCreate.setParentFlow(existingTopLevelFlow.getId());
        executionToCreate.setAuthenticator(executionToImport.getAuthenticator());
        executionToCreate.setRequirement(executionToImport.getRequirement());
        executionToCreate.setPriority(executionToImport.getPriority());
        executionToCreate.setAutheticatorFlow(false);

        executionFlowRepository.createTopLevelFlowExecution(realm.getRealm(), executionToCreate);
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
        HashMap<String, String> executionFlow = new HashMap<>();
        executionFlow.put("alias", executionToImport.getFlowAlias());
        executionFlow.put("provider", executionToImport.getAuthenticator());
        executionFlow.put("type", nonTopLevelFlow.getProviderId());
        executionFlow.put("description", nonTopLevelFlow.getDescription());
        executionFlow.put("authenticator", nonTopLevelFlow.getProviderId());

        try {
            executionFlowRepository.createExecutionFlow(realm.getRealm(), topLevelFlowToImport.getAlias(), executionFlow);
        } catch (ServerErrorException error) {
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
            AuthenticationFlowRepresentation topLevelFlowToImport,
            AuthenticationExecutionExportRepresentation executionToImport
    ) {
        AuthenticationExecutionInfoRepresentation storedExecutionFlow = executionFlowRepository.getExecutionFlow(
                realm.getRealm(), topLevelFlowToImport.getAlias(), executionToImport.getAuthenticator()
        );

        storedExecutionFlow.setRequirement(executionToImport.getRequirement());

        try {
            executionFlowRepository.updateExecutionFlow(realm.getRealm(), topLevelFlowToImport.getAlias(), storedExecutionFlow);
        } catch (ServerErrorException error) {
            throw new ImportProcessingException(
                    "Cannot update execution-flow '" + executionToImport.getFlowAlias()
                            + "' for top-level-flow '" + topLevelFlowToImport.getAlias()
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
        HashMap<String, String> execution = new HashMap<>();
        execution.put("provider", executionToImport.getAuthenticator());

        executionFlowRepository.createNonTopLevelFlowExecution(realm.getRealm(), nonTopLevelFlow.getAlias(), execution);
    }
}
