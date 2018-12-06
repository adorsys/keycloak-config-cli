package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.AuthenticationFlowRepository;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Optional;

/**
 * Imports executions and execution-flows of existing top-level flows
 */
@Service
public class ExecutionFlowsImportService {

    private final AuthenticationFlowRepository authenticationFlowRepository;

    @Autowired
    public ExecutionFlowsImportService(AuthenticationFlowRepository authenticationFlowRepository) {
        this.authenticationFlowRepository = authenticationFlowRepository;
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
        if(executionOrExecutionFlowToImport.isAutheticatorFlow()) {
            createAndConfigureExecutionFlow(realm, topLevelFlowToImport, executionOrExecutionFlowToImport);
        } else {
            createExecution(realm, existingTopLevelFlow, executionOrExecutionFlowToImport);
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

    private void createExecution(
            RealmImport realm,
            AuthenticationFlowRepresentation existingTopLevelFlow,
            AuthenticationExecutionExportRepresentation executionToImport
    ) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm.getRealm());

        AuthenticationExecutionRepresentation executionToCreate = new AuthenticationExecutionRepresentation();

        executionToCreate.setParentFlow(existingTopLevelFlow.getId());
        executionToCreate.setAuthenticator(executionToImport.getAuthenticator());
        executionToCreate.setRequirement(executionToImport.getRequirement());
        executionToCreate.setPriority(executionToImport.getPriority());
        executionToCreate.setAutheticatorFlow(false);

        Response response = flowsResource.addExecution(executionToCreate);
        if (response.getStatus() > 201) {
            throw new RuntimeException(response.getStatusInfo().getReasonPhrase());
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
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm.getRealm());

        HashMap<String, String> executionFlow = new HashMap<>();
        executionFlow.put("alias", executionToImport.getFlowAlias());
        executionFlow.put("provider", executionToImport.getAuthenticator());
        executionFlow.put("type", nonTopLevelFlow.getProviderId());
        executionFlow.put("description", nonTopLevelFlow.getDescription());
        executionFlow.put("authenticator", nonTopLevelFlow.getProviderId());

        flowsResource.addExecutionFlow(topLevelFlowToImport.getAlias(), executionFlow);
    }

    /**
     * We have to configure the requirement property separately because keycloak is ignoring the value and sets the
     * requirement hardcoded to DISABLED while create execution-flow.
     */
    private void configureExecutionFlow(RealmImport realm, AuthenticationFlowRepresentation topLevelFlowToImport, AuthenticationExecutionExportRepresentation executionToImport) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm.getRealm());

        Optional<AuthenticationExecutionInfoRepresentation> maybeStoredExecutionFlow = flowsResource.getExecutions(topLevelFlowToImport.getAlias())
                .stream()
                .filter(f -> f.getProviderId().equals(executionToImport.getAuthenticator()))
                .findFirst();

        if(maybeStoredExecutionFlow.isPresent()) {
            AuthenticationExecutionInfoRepresentation storedExecutionFlow = maybeStoredExecutionFlow.get();
            storedExecutionFlow.setRequirement(executionToImport.getRequirement());
            flowsResource.updateExecutions(topLevelFlowToImport.getAlias(), storedExecutionFlow);
        } else {
            throw new RuntimeException("Cannot find stored execution-flow by alias: " + topLevelFlowToImport.getAlias());
        }
    }

    private void createExecutionAndExecutionFlowsForNonTopLevelFlows(RealmImport realm, AuthenticationFlowRepresentation nonTopLevelFlow) {

        for (AuthenticationExecutionExportRepresentation executionOrExecutionFlowToImport : nonTopLevelFlow.getAuthenticationExecutions()) {

            if(executionOrExecutionFlowToImport.isAutheticatorFlow()) {
                createAndConfigureExecutionFlow(realm, nonTopLevelFlow, executionOrExecutionFlowToImport);
            } else {
                createExecutionForNonTopLevelFlow(realm, nonTopLevelFlow, executionOrExecutionFlowToImport);
                configureExecutionForNonTopLevelFlow(realm, nonTopLevelFlow, executionOrExecutionFlowToImport);
            }
        }
    }

    /**
     * Keycloak is only allowing to set the 'provider' property while creating an execution. The other properties have
     * to be set afterwards with an update.
     * @see {@link #configureExecutionForNonTopLevelFlow}
     */
    private void createExecutionForNonTopLevelFlow(
            RealmImport realm,
            AuthenticationFlowRepresentation nonTopLevelFlow,
            AuthenticationExecutionExportRepresentation executionToImport
    ) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm.getRealm());

        HashMap<String, String> execution = new HashMap<>();
        execution.put("provider", executionToImport.getAuthenticator());

        flowsResource.addExecution(nonTopLevelFlow.getAlias(), execution);
    }

    /**
     * Re-configures the execution with all properties unless keycloak is only allowing to set the 'provider' property
     * while creating an execution.
     * @see {@link #createExecutionForNonTopLevelFlow}
     */
    private void configureExecutionForNonTopLevelFlow(
            RealmImport realm,
            AuthenticationFlowRepresentation nonTopLevelFlow,
            AuthenticationExecutionExportRepresentation executionToImport
    ) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm.getRealm());

        Optional<AuthenticationExecutionInfoRepresentation> maybeStoredExecutionFlow = flowsResource.getExecutions(nonTopLevelFlow.getAlias())
                .stream()
                .filter(f -> f.getProviderId().equals(executionToImport.getAuthenticator()))
                .findFirst();

        if(maybeStoredExecutionFlow.isPresent()) {
            AuthenticationExecutionInfoRepresentation existingNonTopLevelFlow = maybeStoredExecutionFlow.get();
            existingNonTopLevelFlow.setRequirement(executionToImport.getRequirement());
            flowsResource.updateExecutions(nonTopLevelFlow.getAlias(), existingNonTopLevelFlow);
        } else {
            throw new RuntimeException("Cannot find stored non-toplevel execution-flow by alias: " + nonTopLevelFlow.getAlias());
        }
    }
}
