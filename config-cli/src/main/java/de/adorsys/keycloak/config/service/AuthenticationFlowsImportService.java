package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.AuthenticationFlowRepository;
import de.adorsys.keycloak.config.repository.ExecutionFlowRepository;
import de.adorsys.keycloak.config.repository.RealmRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
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

    private final RealmRepository realmRepository;
    private final AuthenticationFlowRepository authenticationFlowRepository;
    private final ExecutionFlowsImportService executionFlowsImportService;
    private final ExecutionFlowRepository executionFlowRepository;

    @Autowired
    public AuthenticationFlowsImportService(
            RealmRepository realmRepository,
            AuthenticationFlowRepository authenticationFlowRepository,
            ExecutionFlowsImportService executionFlowsImportService,
            ExecutionFlowRepository executionFlowRepository
    ) {
        this.realmRepository = realmRepository;
        this.authenticationFlowRepository = authenticationFlowRepository;
        this.executionFlowsImportService = executionFlowsImportService;
        this.executionFlowRepository = executionFlowRepository;
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
            if(logger.isDebugEnabled()) logger.debug("Creating top-level flow: {}", topLevelFlowToImport.getAlias());
            authenticationFlowRepository.createTopLevelFlow(realm.getRealm(), topLevelFlowToImport);

            AuthenticationFlowRepresentation createdTopLevelFlow = authenticationFlowRepository.getTopLevelFlow(realm.getRealm(), topLevelFlowToImport.getAlias());
            executionFlowsImportService.createExecutionsAndExecutionFlows(realm, topLevelFlowToImport, createdTopLevelFlow);
        }
    }

    private void updateTopLevelFlowIfNeeded(
            RealmImport realm,
            AuthenticationFlowRepresentation topLevelFlowToImport,
            AuthenticationFlowRepresentation existingAuthenticationFlow
    ) {
        boolean hasToBeUpdated = hasAuthenticationFlowToBeUpdated(topLevelFlowToImport, existingAuthenticationFlow)
                || hasAnyNonTopLevelFlowToBeUpdated(realm, topLevelFlowToImport);

        if(hasToBeUpdated) {
            if(logger.isDebugEnabled()) logger.debug("Updating top-level flow: {}", topLevelFlowToImport.getAlias());
            recreateTopLevelFlow(realm, topLevelFlowToImport, existingAuthenticationFlow);
        } else {
            if(logger.isDebugEnabled()) logger.debug("No need to update flow: {}", topLevelFlowToImport.getAlias());
        }
    }

    private boolean hasAnyNonTopLevelFlowToBeUpdated(
            RealmImport realm,
            AuthenticationFlowRepresentation topLevelFlowToImport
    ) {
        for (AuthenticationFlowRepresentation nonTopLevelFlowToImport : realm.getNonTopLevelFlowsForTopLevelFlow(topLevelFlowToImport)) {
            Optional<AuthenticationExecutionInfoRepresentation> maybeNonTopLevelFlow = executionFlowRepository.tryToGetNonTopLevelFlow(
                    realm.getRealm(), topLevelFlowToImport.getAlias(), nonTopLevelFlowToImport.getAlias()
            );

            if(maybeNonTopLevelFlow.isPresent()) {
                AuthenticationExecutionInfoRepresentation existingNonTopLevelExecutionFlow = maybeNonTopLevelFlow.get();
                AuthenticationFlowRepresentation existingNonTopLevelFlow = authenticationFlowRepository.getFlowById(
                        realm.getRealm(), existingNonTopLevelExecutionFlow.getFlowId()
                );

                if(hasAuthenticationFlowToBeUpdated(nonTopLevelFlowToImport, existingNonTopLevelFlow)) {
                    return true;
                }
            } else {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the authentication flow to import and the existing representation differs in any property except "id" and:
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
        AuthenticationFlowRepresentation patchedAuthenticationFlow = CloneUtils.deepPatch(existingAuthenticationFlow, topLevelFlowToImport);

        UsedRegistrationFlowWorkaround workaround = new UsedRegistrationFlowWorkaround(realm);
        workaround.unuseTopLevelFlowIfNeeded(topLevelFlowToImport.getAlias());

        authenticationFlowRepository.deleteTopLevelFlow(realm.getRealm(), patchedAuthenticationFlow.getId());
        authenticationFlowRepository.createTopLevelFlow(realm.getRealm(), patchedAuthenticationFlow);

        AuthenticationFlowRepresentation createdTopLevelFlow = authenticationFlowRepository.getTopLevelFlow(realm.getRealm(), topLevelFlowToImport.getAlias());
        executionFlowsImportService.createExecutionsAndExecutionFlows(realm, topLevelFlowToImport, createdTopLevelFlow);

        workaround.resetFlowIfNeeded();
    }

    /**
     * There is no possibility to update a top-level-flow and it's not possible to recreate a top-level-flow
     * which is currently in use. So we have to unuse our top-level-flow by use another flow temporary
     * If no other top-level-flow can be found, this workaround will create a new one
     * This code could be maybe replace by a better update-algorithm of top-level-flows
     */
    private class UsedRegistrationFlowWorkaround {
        private static final String TEMPORARY_CREATED_AUTH_FLOW = "TEMPORARY_CREATED_AUTH_FLOW";

        private final RealmImport realmImport;

        private boolean hasToDeleteTemporaryCreatedAuthFlow = false;

        private UsedRegistrationFlowWorkaround(RealmImport realmImport) {
            this.realmImport = realmImport;
        }

        private void unuseTopLevelFlowIfNeeded(String topLevelFlowAlias) {
            RealmRepresentation existingRealm = realmRepository.getRealm(realmImport.getRealm());

            if(Objects.equals(existingRealm.getBrowserFlow(), topLevelFlowAlias)) {
                unuseBrowserFlow(topLevelFlowAlias, existingRealm);
            }

            if(Objects.equals(existingRealm.getDirectGrantFlow(), topLevelFlowAlias)) {
                unuseDirectGrantFlow(topLevelFlowAlias, existingRealm);
            }

            if(Objects.equals(existingRealm.getClientAuthenticationFlow(), topLevelFlowAlias)) {
                unuseClientAuthenticationFlow(topLevelFlowAlias, existingRealm);
            }

            if(Objects.equals(existingRealm.getDockerAuthenticationFlow(), topLevelFlowAlias)) {
                unuseDockerAuthenticationFlow(topLevelFlowAlias, existingRealm);
            }

            if(Objects.equals(existingRealm.getRegistrationFlow(), topLevelFlowAlias)) {
                unuseRegistrationFlow(topLevelFlowAlias, existingRealm);
            }

            if(Objects.equals(existingRealm.getResetCredentialsFlow(), topLevelFlowAlias)) {
                unuseResetCredentialsFlow(topLevelFlowAlias, existingRealm);
            }
        }

        private void unuseBrowserFlow(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            String otherFlowAlias = searchForAnotherTopLevelFlow(topLevelFlowAlias);

            existingRealm.setBrowserFlow(otherFlowAlias);
            realmRepository.updateRealm(existingRealm);
        }

        private void unuseDirectGrantFlow(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            String otherFlowAlias = searchForAnotherTopLevelFlow(topLevelFlowAlias);

            existingRealm.setDirectGrantFlow(otherFlowAlias);
            realmRepository.updateRealm(existingRealm);
        }

        private void unuseClientAuthenticationFlow(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            String otherFlowAlias = searchForAnotherTopLevelFlow(topLevelFlowAlias);

            existingRealm.setClientAuthenticationFlow(otherFlowAlias);
            realmRepository.updateRealm(existingRealm);
        }

        private void unuseDockerAuthenticationFlow(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            String otherFlowAlias = searchForAnotherTopLevelFlow(topLevelFlowAlias);

            existingRealm.setDockerAuthenticationFlow(otherFlowAlias);
            realmRepository.updateRealm(existingRealm);
        }

        private void unuseRegistrationFlow(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            String otherFlowAlias = searchForAnotherTopLevelFlow(topLevelFlowAlias);

            existingRealm.setRegistrationFlow(otherFlowAlias);
            realmRepository.updateRealm(existingRealm);
        }

        private void unuseResetCredentialsFlow(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            String otherFlowAlias = searchForAnotherTopLevelFlow(topLevelFlowAlias);

            existingRealm.setResetCredentialsFlow(otherFlowAlias);
            realmRepository.updateRealm(existingRealm);
        }

        private String searchForAnotherTopLevelFlow(String topLevelFlowAlias) {
            String otherFlowAlias;

            List<AuthenticationFlowRepresentation> existingTopLevelFlows = authenticationFlowRepository.getTopLevelFlows(realmImport.getRealm());
            Optional<String> maybeOtherFlowAlias = existingTopLevelFlows.stream()
                    .filter(f -> !f.getAlias().equals(topLevelFlowAlias))
                    .findFirst()
                    .map(AuthenticationFlowRepresentation::getAlias);

            if(maybeOtherFlowAlias.isPresent()) {
                otherFlowAlias = maybeOtherFlowAlias.get();
            } else {
                AuthenticationFlowRepresentation temporaryCreatedAuthenticationFlow = setupTemporaryCreatedAuthenticationFlow();
                authenticationFlowRepository.createTopLevelFlow(realmImport.getRealm(), temporaryCreatedAuthenticationFlow);

                otherFlowAlias = TEMPORARY_CREATED_AUTH_FLOW;
                hasToDeleteTemporaryCreatedAuthFlow = true;
            }

            return otherFlowAlias;
        }

        private void resetFlowIfNeeded() {
            if (hasToDeleteTemporaryCreatedAuthFlow) {
                deleteTemporaryCreatedFlow();
            }
        }

        private void deleteTemporaryCreatedFlow() {
            AuthenticationFlowRepresentation existingTemporaryCreatedFlow = authenticationFlowRepository.getTopLevelFlow(realmImport.getRealm(), TEMPORARY_CREATED_AUTH_FLOW);
            authenticationFlowRepository.deleteTopLevelFlow(realmImport.getRealm(), existingTemporaryCreatedFlow.getId());
        }

        private AuthenticationFlowRepresentation setupTemporaryCreatedAuthenticationFlow() {
            AuthenticationFlowRepresentation temporaryCreatedAuthenticationFlow = new AuthenticationFlowRepresentation();

            temporaryCreatedAuthenticationFlow.setAlias(TEMPORARY_CREATED_AUTH_FLOW);
            temporaryCreatedAuthenticationFlow.setTopLevel(true);
            temporaryCreatedAuthenticationFlow.setBuiltIn(false);
            temporaryCreatedAuthenticationFlow.setProviderId(TEMPORARY_CREATED_AUTH_FLOW);

            return temporaryCreatedAuthenticationFlow;
        }
    }
}
