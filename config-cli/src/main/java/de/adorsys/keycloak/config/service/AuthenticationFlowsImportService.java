package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.AuthenticationFlowRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * We have to import authentication-flows separately because in case of an existing realm, keycloak is ignoring or
 * not supporting embedded objects in realm-import's property called "authenticationFlows"
 *
 * Glossar:
 * topLevel-flow: any flow which has the property 'topLevel' set to 'true'. Can contain execution-flows and executions
 * non-topLevel-flow: any flow which has the property 'topLevel' set to 'false' and which are related to execution-flows within topLevel-flows
 */
@Service
public class AuthenticationFlowsImportService {

    private final AuthenticationFlowRepository authenticationFlowRepository;

    @Autowired
    public AuthenticationFlowsImportService(
            AuthenticationFlowRepository authenticationFlowRepository
    ) {
        this.authenticationFlowRepository = authenticationFlowRepository;
    }

    public void doImport(RealmImport realmImport) {
        List<AuthenticationFlowRepresentation> topLevelFlowsToImport = realmImport.getTopLevelFlows();

        createOrUpdateTopLevelFlows(realmImport, topLevelFlowsToImport);
        createOrUpdateExecutionsForTopLevelFlows(realmImport, topLevelFlowsToImport);
    }

    /**
     * creates or updates only the top-level flows WITHOUT its executions or execution-flows
     */
    private void createOrUpdateTopLevelFlows(RealmImport realmImport, List<AuthenticationFlowRepresentation> topLevelFlowsToImport) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.get(realmImport.getRealm());

        // keycloak is returning here only so-called toplevel-flows
        List<AuthenticationFlowRepresentation> existingTopLevelFlows = flowsResource.getFlows();

        for (AuthenticationFlowRepresentation topLevelFlowToImport : topLevelFlowsToImport) {
            createOrUpdateTopLevelFlow(realmImport, existingTopLevelFlows, topLevelFlowToImport);
        }
    }

    /**
     * creates or updates only the top-level flow WITHOUT its executions or execution-flows
     */
    private void createOrUpdateTopLevelFlow(
            RealmImport realmImport,
            List<AuthenticationFlowRepresentation> existingAuthenticationFlows,
            AuthenticationFlowRepresentation topLevelFlowToImport
    ) {
        String alias = topLevelFlowToImport.getAlias();

        Optional<AuthenticationFlowRepresentation> maybeTopLevelFlowFlow = existingAuthenticationFlows.stream()
                .filter(f -> f.getAlias().equals(alias))
                .findFirst();

        if (maybeTopLevelFlowFlow.isPresent()) {
            AuthenticationFlowRepresentation existingTopLevelFlow = maybeTopLevelFlowFlow.get();
            updateTopLevelFlow(realmImport.getRealm(), topLevelFlowToImport, existingTopLevelFlow);
        } else {
            createTopLevelFlow(realmImport.getRealm(), topLevelFlowToImport);
        }
    }

    private void createOrUpdateExecutionsForTopLevelFlows(RealmImport realmImport, List<AuthenticationFlowRepresentation> topLevelFlowsToImport) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.get(realmImport.getRealm());

        for (AuthenticationFlowRepresentation flowToImport : topLevelFlowsToImport) {
            AuthenticationFlowRepresentation existingTopLevelFlow = flowsResource.getFlows()
                    .stream()
                    .filter(f -> f.getAlias().equals(flowToImport.getAlias()))
                    .findFirst().get();

            createOrUpdateExecutions(realmImport, flowToImport, existingTopLevelFlow);
        }
    }

    /**
     * creates only the top-level flow WITHOUT its executions or execution-flows
     */
    private void createTopLevelFlow(String realm, AuthenticationFlowRepresentation topLevelFlowToImport) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.get(realm);
        Response response = flowsResource.createFlow(topLevelFlowToImport);

        if (response.getStatus() != 201) {
            throw new RuntimeException(response.getStatusInfo().getReasonPhrase());
        }
    }

    private void updateTopLevelFlow(String realm, AuthenticationFlowRepresentation authenticationFlowToImport, AuthenticationFlowRepresentation existingAuthenticationFlow) {
        AuthenticationFlowRepresentation patchedAuthenticationFlow = CloneUtils.deepPatch(existingAuthenticationFlow, authenticationFlowToImport);

        AuthenticationManagementResource flowsResource = authenticationFlowRepository.get(realm);

        // TODO really? delete and re-create? Try to use keycloak's update endpoint
        flowsResource.deleteFlow(patchedAuthenticationFlow.getId());
        createTopLevelFlow(realm, patchedAuthenticationFlow);
    }

    private void createOrUpdateExecutions(
            RealmImport realm,
            AuthenticationFlowRepresentation topLevelFlowToImport,
            AuthenticationFlowRepresentation existingTopLevelFlow
    ) {
        for (AuthenticationExecutionExportRepresentation executionToImport : topLevelFlowToImport.getAuthenticationExecutions()) {
            Optional<AuthenticationExecutionExportRepresentation> maybeExecution = existingTopLevelFlow.getAuthenticationExecutions()
                    .stream()
                    .filter(e -> e.getFlowAlias().equals(executionToImport.getFlowAlias()))
                    .findFirst();

            if(maybeExecution.isPresent()) {
                AuthenticationExecutionExportRepresentation executionToUpdate = maybeExecution.get();
                // TODO update executions
            } else {
                createExecutionOrExecutionFlow(realm, topLevelFlowToImport, existingTopLevelFlow, executionToImport);
            }
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

    private void createExecution(
            RealmImport realm,
            AuthenticationFlowRepresentation existingTopLevelFlow,
            AuthenticationExecutionExportRepresentation executionToImport
    ) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.get(realm.getRealm());

        AuthenticationExecutionRepresentation executionToCreate = new AuthenticationExecutionRepresentation();

        executionToCreate.setParentFlow(existingTopLevelFlow.getId());
        executionToCreate.setAuthenticator(executionToImport.getAuthenticator());
        executionToCreate.setRequirement(executionToImport.getRequirement());

        Response response = flowsResource.addExecution(executionToCreate);
        if (response.getStatus() > 201) {
            throw new RuntimeException(response.getStatusInfo().getReasonPhrase());
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
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.get(realm.getRealm());

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
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.get(realm.getRealm());

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

    /**
     * Creates the executionFlow within the topLevel-flow AND creates the non-topLevel flow because keycloak does
     * this automatically while calling `flowsResource.addExecutionFlow`
     */
    private void createNonTopLevelFlowByExecutionFlow(RealmImport realm, AuthenticationFlowRepresentation topLevelFlowToImport, AuthenticationExecutionExportRepresentation executionToImport, AuthenticationFlowRepresentation nonTopLevelFlow) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.get(realm.getRealm());

        HashMap<String, String> executionFlow = new HashMap<>();
        executionFlow.put("alias", executionToImport.getFlowAlias());
        executionFlow.put("provider", executionToImport.getAuthenticator());
        executionFlow.put("type", nonTopLevelFlow.getProviderId());
        executionFlow.put("description", nonTopLevelFlow.getDescription());
        executionFlow.put("authenticator", nonTopLevelFlow.getProviderId());

        flowsResource.addExecutionFlow(topLevelFlowToImport.getAlias(), executionFlow);
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
     * We have to configure the requirement property separately because keycloak is ignoring the value and sets the
     * requirement hardcoded to DISABLED while create execution-flow.
     */
    private void configureExecutionFlow(RealmImport realm, AuthenticationFlowRepresentation topLevelFlowToImport, AuthenticationExecutionExportRepresentation executionToImport) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.get(realm.getRealm());

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
}
