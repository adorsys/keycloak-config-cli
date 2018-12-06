package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.AuthenticationFlowRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFlowsImportService.class);

    private final AuthenticationFlowRepository authenticationFlowRepository;
    private final ExecutionFlowsImportService executionFlowsImportService;

    @Autowired
    public AuthenticationFlowsImportService(
            AuthenticationFlowRepository authenticationFlowRepository,
            ExecutionFlowsImportService executionFlowsImportService
    ) {
        this.authenticationFlowRepository = authenticationFlowRepository;
        this.executionFlowsImportService = executionFlowsImportService;
    }

    /**
     * How the import works:
     * - check the authentication flows:
     * -- if the flow is not present: create the authentication flow
     * -- if the flow is present, check:
     * --- if the flow contains any changes: update the authentication flow, which means: delete and recreate the authentication flow
     * --- if nothing of above: do nothing
     */
    public void doImport(RealmImport realmImport) {
        List<AuthenticationFlowRepresentation> topLevelFlowsToImport = realmImport.getTopLevelFlows();
        createOrUpdateTopLevelFlows(realmImport, topLevelFlowsToImport);
    }

    /**
     * creates or updates only the top-level flows and its executions or execution-flows
     */
    private void createOrUpdateTopLevelFlows(RealmImport realmImport, List<AuthenticationFlowRepresentation> topLevelFlowsToImport) {
        for (AuthenticationFlowRepresentation topLevelFlowToImport : topLevelFlowsToImport) {
            createOrUpdateTopLevelFlow(realmImport, topLevelFlowToImport);
        }
    }

    /**
     * creates or updates only the top-level flow and its executions or execution-flows
     */
    private void createOrUpdateTopLevelFlow(
            RealmImport realm,
            AuthenticationFlowRepresentation topLevelFlowToImport
    ) {
        String alias = topLevelFlowToImport.getAlias();

        Optional<AuthenticationFlowRepresentation> maybeTopLevelFlow = authenticationFlowRepository.tryToGetTopLevelFlow(realm.getRealm(), alias);

        if (maybeTopLevelFlow.isPresent()) {
            AuthenticationFlowRepresentation existingTopLevelFlow = maybeTopLevelFlow.get();
            updateTopLevelFlow(realm, topLevelFlowToImport, existingTopLevelFlow);
        } else {
            if(logger.isDebugEnabled()) logger.debug("Creating top-level flow: {}", topLevelFlowToImport.getAlias());
            authenticationFlowRepository.createTopLevelFlow(realm, topLevelFlowToImport);

            AuthenticationFlowRepresentation createdTopLevelFlow = authenticationFlowRepository.getTopLevelFlow(realm.getRealm(), topLevelFlowToImport.getAlias());
            executionFlowsImportService.createExecutionsAndExecutionFlows(realm, topLevelFlowToImport, createdTopLevelFlow);
        }
    }

    private void updateTopLevelFlow(
            RealmImport realm,
            AuthenticationFlowRepresentation topLevelFlowToImport,
            AuthenticationFlowRepresentation existingAuthenticationFlow
    ) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm.getRealm());

        boolean hasToBeUpdated = hasToBeUpdated(topLevelFlowToImport, existingAuthenticationFlow);

        if(hasToBeUpdated) {
            if(logger.isDebugEnabled()) logger.debug("Updating top-level flow: {}", topLevelFlowToImport.getAlias());
            recreateTopLevelFlow(realm, topLevelFlowToImport, existingAuthenticationFlow, flowsResource);
        } else {
            if(logger.isDebugEnabled()) logger.debug("No need to update flow: {}", topLevelFlowToImport.getAlias());
        }
    }

    /**
     * Checks if the top-level flow to import and the existing representation differs in any property except "id" and:
     * @param topLevelFlowToImport the top-level flow coming from import file
     * @param existingAuthenticationFlow the existing top-level flow in keycloak
     * @return true if there is any change, false if not
     */
    private boolean hasToBeUpdated(AuthenticationFlowRepresentation topLevelFlowToImport, AuthenticationFlowRepresentation existingAuthenticationFlow) {
        return !CloneUtils.deepEquals(
                topLevelFlowToImport,
                existingAuthenticationFlow,
                "id"
        );
    }

    /**
     * Deletes the top-level flow and all its executions and recreates them
     */
    private void recreateTopLevelFlow(
            RealmImport realm,
            AuthenticationFlowRepresentation topLevelFlowToImport,
            AuthenticationFlowRepresentation existingAuthenticationFlow,
            AuthenticationManagementResource flowsResource
    ) {
        AuthenticationFlowRepresentation patchedAuthenticationFlow = CloneUtils.deepPatch(existingAuthenticationFlow, topLevelFlowToImport);

        flowsResource.deleteFlow(patchedAuthenticationFlow.getId());
        authenticationFlowRepository.createTopLevelFlow(realm, patchedAuthenticationFlow);

        AuthenticationFlowRepresentation createdTopLevelFlow = authenticationFlowRepository.getTopLevelFlow(realm.getRealm(), topLevelFlowToImport.getAlias());
        executionFlowsImportService.createExecutionsAndExecutionFlows(realm, topLevelFlowToImport, createdTopLevelFlow);
    }
}
