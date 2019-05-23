package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.exception.InvalidImportException;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.AuthenticationFlowRepository;
import de.adorsys.keycloak.config.repository.ExecutionFlowRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
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
 * <p>
 * Glossar:
 * topLevel-flow: any flow which has the property 'topLevel' set to 'true'. Can contain execution-flows and executions
 * non-topLevel-flow: any flow which has the property 'topLevel' set to 'false' and which are related to execution-flows within topLevel-flows
 */
@Service
public class AuthenticationFlowsImportService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFlowsImportService.class);

    private final AuthenticationFlowRepository authenticationFlowRepository;
    private final ExecutionFlowsImportService executionFlowsImportService;
    private final ExecutionFlowRepository executionFlowRepository;
    private final UsedAuthenticationFlowWorkaroundFactory workaroundFactory;

    @Autowired
    public AuthenticationFlowsImportService(
            AuthenticationFlowRepository authenticationFlowRepository,
            ExecutionFlowsImportService executionFlowsImportService,
            ExecutionFlowRepository executionFlowRepository,
            UsedAuthenticationFlowWorkaroundFactory workaroundFactory
    ) {
        this.authenticationFlowRepository = authenticationFlowRepository;
        this.executionFlowsImportService = executionFlowsImportService;
        this.executionFlowRepository = executionFlowRepository;
        this.workaroundFactory = workaroundFactory;
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
            updateTopLevelFlowIfNeeded(realm, topLevelFlowToImport, existingTopLevelFlow);
        } else {
            createTopLevelFlow(realm, topLevelFlowToImport);
        }
    }

    private void createTopLevelFlow(RealmImport realm, AuthenticationFlowRepresentation topLevelFlowToImport) {
        logger.debug("Creating top-level flow: {}", topLevelFlowToImport.getAlias());
        authenticationFlowRepository.createTopLevelFlow(realm.getRealm(), topLevelFlowToImport);

        AuthenticationFlowRepresentation createdTopLevelFlow = authenticationFlowRepository.getTopLevelFlow(realm.getRealm(), topLevelFlowToImport.getAlias());
        executionFlowsImportService.createExecutionsAndExecutionFlows(realm, topLevelFlowToImport, createdTopLevelFlow);
    }

    private void updateTopLevelFlowIfNeeded(
            RealmImport realm,
            AuthenticationFlowRepresentation topLevelFlowToImport,
            AuthenticationFlowRepresentation existingAuthenticationFlow
    ) {
        boolean hasToBeUpdated = hasAuthenticationFlowToBeUpdated(topLevelFlowToImport, existingAuthenticationFlow)
                || hasAnyNonTopLevelFlowToBeUpdated(realm, topLevelFlowToImport);

        if (hasToBeUpdated) {
            logger.debug("Updating top-level flow: {}", topLevelFlowToImport.getAlias());
            recreateTopLevelFlow(realm, topLevelFlowToImport, existingAuthenticationFlow);
        } else {
            logger.debug("No need to update flow: {}", topLevelFlowToImport.getAlias());
        }
    }

    private boolean hasAnyNonTopLevelFlowToBeUpdated(
            RealmImport realm,
            AuthenticationFlowRepresentation topLevelFlowToImport
    ) {
        for (AuthenticationFlowRepresentation nonTopLevelFlowToImport : realm.getNonTopLevelFlowsForTopLevelFlow(topLevelFlowToImport)) {
            if (isNonTopLevelFlowNotExistingOrHasToBeUpdated(realm, topLevelFlowToImport, nonTopLevelFlowToImport)) {
                return true;
            }
        }

        return false;
    }

    private boolean isNonTopLevelFlowNotExistingOrHasToBeUpdated(RealmImport realm, AuthenticationFlowRepresentation topLevelFlowToImport, AuthenticationFlowRepresentation nonTopLevelFlowToImport) {
        Optional<AuthenticationExecutionInfoRepresentation> maybeNonTopLevelFlow = executionFlowRepository.tryToGetNonTopLevelFlow(
                realm.getRealm(), topLevelFlowToImport.getAlias(), nonTopLevelFlowToImport.getAlias()
        );

        return maybeNonTopLevelFlow
                .map(authenticationExecutionInfoRepresentation -> hasExistingNonTopLevelFlowToBeUpdated(realm, nonTopLevelFlowToImport, authenticationExecutionInfoRepresentation))
                .orElse(true);
    }

    private boolean hasExistingNonTopLevelFlowToBeUpdated(RealmImport realm, AuthenticationFlowRepresentation nonTopLevelFlowToImport, AuthenticationExecutionInfoRepresentation existingNonTopLevelExecutionFlow) {
        AuthenticationFlowRepresentation existingNonTopLevelFlow = authenticationFlowRepository.getFlowById(
                realm.getRealm(), existingNonTopLevelExecutionFlow.getFlowId()
        );

        return hasAuthenticationFlowToBeUpdated(nonTopLevelFlowToImport, existingNonTopLevelFlow);
    }

    /**
     * Checks if the authentication flow to import and the existing representation differs in any property except "id" and:
     *
     * @param authenticationFlowToImport the top-level or non-top-level flow coming from import file
     * @param existingAuthenticationFlow the existing top-level or non-top-level flow in keycloak
     * @return true if there is any change, false if not
     */
    private boolean hasAuthenticationFlowToBeUpdated(
            AuthenticationFlowRepresentation authenticationFlowToImport,
            AuthenticationFlowRepresentation existingAuthenticationFlow
    ) {
        return !CloneUtils.deepEquals(
                authenticationFlowToImport,
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
            AuthenticationFlowRepresentation existingAuthenticationFlow
    ) {
        AuthenticationFlowRepresentation patchedAuthenticationFlow = CloneUtils.deepPatch(existingAuthenticationFlow, topLevelFlowToImport, "id");

        if(patchedAuthenticationFlow.isBuiltIn() || existingAuthenticationFlow.isBuiltIn()) {
            throw new InvalidImportException("Unable to recreate flow '" + patchedAuthenticationFlow.getAlias() + "' in realm '" + realm.getRealm() + "': Deletion or creation of built-in flows is not possible");
        }

        UsedAuthenticationFlowWorkaroundFactory.UsedAuthenticationFlowWorkaround workaround = workaroundFactory.buildFor(realm);
        workaround.unuseTopLevelFlowIfNeeded(topLevelFlowToImport.getAlias());

        authenticationFlowRepository.deleteTopLevelFlow(realm.getRealm(), patchedAuthenticationFlow.getId());
        authenticationFlowRepository.createTopLevelFlow(realm.getRealm(), patchedAuthenticationFlow);

        AuthenticationFlowRepresentation createdTopLevelFlow = authenticationFlowRepository.getTopLevelFlow(realm.getRealm(), topLevelFlowToImport.getAlias());
        executionFlowsImportService.createExecutionsAndExecutionFlows(realm, topLevelFlowToImport, createdTopLevelFlow);

        workaround.resetFlowIfNeeded();
    }
}
