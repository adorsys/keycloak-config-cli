package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.AuthenticationFlowRepository;
import de.adorsys.keycloak.config.repository.ExecutionFlowRepository;
import de.adorsys.keycloak.config.repository.RealmRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.apache.logging.log4j.util.Strings;
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

        UsedAuthenticationFlowWorkaround workaround = new UsedAuthenticationFlowWorkaround(realm);
        workaround.unuseTopLevelFlowIfNeeded(topLevelFlowToImport.getAlias());

        authenticationFlowRepository.deleteTopLevelFlow(realm.getRealm(), patchedAuthenticationFlow.getId());
        authenticationFlowRepository.createTopLevelFlow(realm.getRealm(), patchedAuthenticationFlow);

        AuthenticationFlowRepresentation createdTopLevelFlow = authenticationFlowRepository.getTopLevelFlow(realm.getRealm(), topLevelFlowToImport.getAlias());
        executionFlowsImportService.createExecutionsAndExecutionFlows(realm, topLevelFlowToImport, createdTopLevelFlow);

        workaround.resetFlowIfNeeded();
    }

    /**
     * There is no possibility to update a top-level-flow and it's not possible to recreate a top-level-flow
     * which is currently in use.
     * So we have to unuse our top-level-flow by use a temporary created flow as long as updating the considered flow.
     * This code could be maybe replace by a better update-algorithm of top-level-flows
     */
    private class UsedAuthenticationFlowWorkaround {
        private static final String TEMPORARY_CREATED_AUTH_FLOW = "TEMPORARY_CREATED_AUTH_FLOW";

        private final RealmImport realmImport;

        private String browserFlow;
        private String directGrantFlow;
        private String clientAuthenticationFlow;
        private String dockerAuthenticationFlow;
        private String registrationFlow;
        private String resetCredentialsFlow;

        private UsedAuthenticationFlowWorkaround(RealmImport realmImport) {
            this.realmImport = realmImport;
        }

        private void unuseTopLevelFlowIfNeeded(String topLevelFlowAlias) {
            RealmRepresentation existingRealm = realmRepository.get(realmImport.getRealm());

            if(Objects.equals(existingRealm.getBrowserFlow(), topLevelFlowAlias)) {
                if (logger.isDebugEnabled()) logger.debug("Temporary unuse browser-flow in realm '{}' which is '{}'", realmImport.getRealm(), topLevelFlowAlias);
                unuseBrowserFlow(existingRealm);
            }

            if(Objects.equals(existingRealm.getDirectGrantFlow(), topLevelFlowAlias)) {
                if (logger.isDebugEnabled()) logger.debug("Temporary unuse direct-grant-flow in realm '{}' which is '{}'", realmImport.getRealm(), topLevelFlowAlias);
                unuseDirectGrantFlow(existingRealm);
            }

            if(Objects.equals(existingRealm.getClientAuthenticationFlow(), topLevelFlowAlias)) {
                if (logger.isDebugEnabled()) logger.debug("Temporary unuse client-authentication-flow in realm '{}' which is '{}'", realmImport.getRealm(), topLevelFlowAlias);
                unuseClientAuthenticationFlow(existingRealm);
            }

            if(Objects.equals(existingRealm.getDockerAuthenticationFlow(), topLevelFlowAlias)) {
                if (logger.isDebugEnabled()) logger.debug("Temporary unuse docker-authentication-flow in realm '{}' which is '{}'", realmImport.getRealm(), topLevelFlowAlias);
                unuseDockerAuthenticationFlow(existingRealm);
            }

            if(Objects.equals(existingRealm.getRegistrationFlow(), topLevelFlowAlias)) {
                if (logger.isDebugEnabled()) logger.debug("Temporary unuse registration-flow in realm '{}' which is '{}'", realmImport.getRealm(), topLevelFlowAlias);
                unuseRegistrationFlow(existingRealm);
            }

            if(Objects.equals(existingRealm.getResetCredentialsFlow(), topLevelFlowAlias)) {
                if (logger.isDebugEnabled()) logger.debug("Temporary unuse reset-credentials-flow in realm '{}' which is '{}'", realmImport.getRealm(), topLevelFlowAlias);
                unuseResetCredentialsFlow(existingRealm);
            }
        }

        private void unuseBrowserFlow(RealmRepresentation existingRealm) {
            String otherFlowAlias = searchTemporaryCreatedTopLevelFlowForReplacement();

            browserFlow = existingRealm.getBrowserFlow();

            existingRealm.setBrowserFlow(otherFlowAlias);
            realmRepository.update(existingRealm);
        }

        private void unuseDirectGrantFlow(RealmRepresentation existingRealm) {
            String otherFlowAlias = searchTemporaryCreatedTopLevelFlowForReplacement();

            directGrantFlow = existingRealm.getDirectGrantFlow();

            existingRealm.setDirectGrantFlow(otherFlowAlias);
            realmRepository.update(existingRealm);
        }

        private void unuseClientAuthenticationFlow(RealmRepresentation existingRealm) {
            String otherFlowAlias = searchTemporaryCreatedTopLevelFlowForReplacement();

            clientAuthenticationFlow = existingRealm.getClientAuthenticationFlow();

            existingRealm.setClientAuthenticationFlow(otherFlowAlias);
            realmRepository.update(existingRealm);
        }

        private void unuseDockerAuthenticationFlow(RealmRepresentation existingRealm) {
            String otherFlowAlias = searchTemporaryCreatedTopLevelFlowForReplacement();

            dockerAuthenticationFlow = existingRealm.getDockerAuthenticationFlow();

            existingRealm.setDockerAuthenticationFlow(otherFlowAlias);
            realmRepository.update(existingRealm);
        }

        private void unuseRegistrationFlow(RealmRepresentation existingRealm) {
            String otherFlowAlias = searchTemporaryCreatedTopLevelFlowForReplacement();

            registrationFlow = existingRealm.getRegistrationFlow();

            existingRealm.setRegistrationFlow(otherFlowAlias);
            realmRepository.update(existingRealm);
        }

        private void unuseResetCredentialsFlow(RealmRepresentation existingRealm) {
            String otherFlowAlias = searchTemporaryCreatedTopLevelFlowForReplacement();

            resetCredentialsFlow = existingRealm.getResetCredentialsFlow();

            existingRealm.setResetCredentialsFlow(otherFlowAlias);
            realmRepository.update(existingRealm);
        }

        private String searchTemporaryCreatedTopLevelFlowForReplacement() {
            AuthenticationFlowRepresentation otherFlow;

            Optional<AuthenticationFlowRepresentation> maybeTemporaryCreatedFlow = searchForTemporaryCreatedFlow();

            if(maybeTemporaryCreatedFlow.isPresent()) {
                otherFlow = maybeTemporaryCreatedFlow.get();
            } else {
                if (logger.isDebugEnabled()) logger.debug("Create top-level-flow '{}' in realm '{}' to be used temporarily", realmImport.getRealm(), TEMPORARY_CREATED_AUTH_FLOW);

                AuthenticationFlowRepresentation temporaryCreatedFlow = setupTemporaryCreatedFlow();
                authenticationFlowRepository.createTopLevelFlow(realmImport.getRealm(), temporaryCreatedFlow);

                otherFlow = temporaryCreatedFlow;
            }

            return otherFlow.getAlias();
        }

        private Optional<AuthenticationFlowRepresentation> searchForTemporaryCreatedFlow() {
            List<AuthenticationFlowRepresentation> existingTopLevelFlows = authenticationFlowRepository.getTopLevelFlows(realmImport.getRealm());
            return existingTopLevelFlows.stream()
                    .filter(f -> Objects.equals(f.getAlias(), TEMPORARY_CREATED_AUTH_FLOW))
                    .findFirst();
        }

        private void resetFlowIfNeeded() {
            if (hasToResetFlows()) {
                RealmRepresentation existingRealm = realmRepository.get(realmImport.getRealm());

                resetFlows(existingRealm);
                realmRepository.update(existingRealm);

                deleteTemporaryCreatedFlow();
            }
        }

        private boolean hasToResetFlows() {
            return Strings.isNotBlank(browserFlow) ||
                    Strings.isNotBlank(directGrantFlow) ||
                    Strings.isNotBlank(clientAuthenticationFlow) ||
                    Strings.isNotBlank(dockerAuthenticationFlow) ||
                    Strings.isNotBlank(registrationFlow) ||
                    Strings.isNotBlank(resetCredentialsFlow);
        }

        private void resetFlows(RealmRepresentation existingRealm) {
            if(Strings.isNotBlank(browserFlow)) {
                if (logger.isDebugEnabled()) logger.debug("Reset browser-flow in realm '{}' to '{}'", realmImport.getRealm(), browserFlow);

                existingRealm.setBrowserFlow(browserFlow);
            }
            if(Strings.isNotBlank(directGrantFlow)) {
                if (logger.isDebugEnabled()) logger.debug("Reset direct-grant-flow in realm '{}' to '{}'", realmImport.getRealm(), directGrantFlow);

                existingRealm.setDirectGrantFlow(directGrantFlow);
            }
            if(Strings.isNotBlank(clientAuthenticationFlow)) {
                if (logger.isDebugEnabled()) logger.debug("Reset client-authentication-flow in realm '{}' to '{}'", realmImport.getRealm(), clientAuthenticationFlow);

                existingRealm.setClientAuthenticationFlow(clientAuthenticationFlow);
            }
            if(Strings.isNotBlank(dockerAuthenticationFlow)) {
                if (logger.isDebugEnabled()) logger.debug("Reset docker-authentication-flow in realm '{}' to '{}'", realmImport.getRealm(), dockerAuthenticationFlow);

                existingRealm.setDockerAuthenticationFlow(dockerAuthenticationFlow);
            }
            if(Strings.isNotBlank(registrationFlow)) {
                if (logger.isDebugEnabled()) logger.debug("Reset registration-flow in realm '{}' to '{}'", realmImport.getRealm(), registrationFlow);

                existingRealm.setRegistrationFlow(registrationFlow);
            }
            if(Strings.isNotBlank(resetCredentialsFlow)) {
                if (logger.isDebugEnabled()) logger.debug("Reset reset-credentials-flow in realm '{}' to '{}'", realmImport.getRealm(), resetCredentialsFlow);

                existingRealm.setResetCredentialsFlow(resetCredentialsFlow);
            }
        }

        private void deleteTemporaryCreatedFlow() {
            if (logger.isDebugEnabled()) logger.debug("Delete temporary created top-level-flow '{}' in realm '{}'", TEMPORARY_CREATED_AUTH_FLOW, realmImport.getRealm());

            AuthenticationFlowRepresentation existingTemporaryCreatedFlow = authenticationFlowRepository.getTopLevelFlow(realmImport.getRealm(), TEMPORARY_CREATED_AUTH_FLOW);
            authenticationFlowRepository.deleteTopLevelFlow(realmImport.getRealm(), existingTemporaryCreatedFlow.getId());
        }

        private AuthenticationFlowRepresentation setupTemporaryCreatedFlow() {
            AuthenticationFlowRepresentation temporaryCreatedAuthenticationFlow = new AuthenticationFlowRepresentation();

            temporaryCreatedAuthenticationFlow.setAlias(TEMPORARY_CREATED_AUTH_FLOW);
            temporaryCreatedAuthenticationFlow.setTopLevel(true);
            temporaryCreatedAuthenticationFlow.setBuiltIn(false);
            temporaryCreatedAuthenticationFlow.setProviderId(TEMPORARY_CREATED_AUTH_FLOW);

            return temporaryCreatedAuthenticationFlow;
        }
    }
}
