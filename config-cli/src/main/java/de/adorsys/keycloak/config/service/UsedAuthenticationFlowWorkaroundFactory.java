package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.AuthenticationFlowRepository;
import de.adorsys.keycloak.config.repository.RealmRepository;
import org.apache.logging.log4j.util.Strings;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UsedAuthenticationFlowWorkaroundFactory {

    private final RealmRepository realmRepository;
    private final AuthenticationFlowRepository authenticationFlowRepository;

    @Autowired
    public UsedAuthenticationFlowWorkaroundFactory(RealmRepository realmRepository, AuthenticationFlowRepository authenticationFlowRepository) {
        this.realmRepository = realmRepository;
        this.authenticationFlowRepository = authenticationFlowRepository;
    }

    public UsedAuthenticationFlowWorkaround buildFor(RealmImport realmImport) {
        return new UsedAuthenticationFlowWorkaround(realmImport);
    }

    /**
     * There is no possibility to update a top-level-flow and it's not possible to recreate a top-level-flow
     * which is currently in use.
     * So we have to unuse our top-level-flow by use a temporary created flow as long as updating the considered flow.
     * This code could be maybe replace by a better update-algorithm of top-level-flows
     */
    public class UsedAuthenticationFlowWorkaround {
        private final Logger logger = LoggerFactory.getLogger(UsedAuthenticationFlowWorkaround.class);

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

        public void unuseTopLevelFlowIfNeeded(String topLevelFlowAlias) {
            RealmRepresentation existingRealm = realmRepository.get(realmImport.getRealm());

            unuseBrowserFlowIfNeeded(topLevelFlowAlias, existingRealm);
            unuseDirectGrantFlowIfNeeded(topLevelFlowAlias, existingRealm);
            unuseClientAuthenticationFlowIfNeeded(topLevelFlowAlias, existingRealm);
            unuseDockerAuthenticationFlowIfNeeded(topLevelFlowAlias, existingRealm);
            unuseRegistrationFlowIfNeeded(topLevelFlowAlias, existingRealm);
            unuseResetCredentialsFlowIfNeeded(topLevelFlowAlias, existingRealm);
        }

        private void unuseBrowserFlowIfNeeded(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            if(Objects.equals(existingRealm.getBrowserFlow(), topLevelFlowAlias)) {
                logger.debug("Temporary unuse browser-flow in realm '{}' which is '{}'", realmImport.getRealm(), topLevelFlowAlias);
                unuseBrowserFlow(existingRealm);
            }
        }

        private void unuseDirectGrantFlowIfNeeded(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            if(Objects.equals(existingRealm.getDirectGrantFlow(), topLevelFlowAlias)) {
                logger.debug("Temporary unuse direct-grant-flow in realm '{}' which is '{}'", realmImport.getRealm(), topLevelFlowAlias);
                unuseDirectGrantFlow(existingRealm);
            }
        }

        private void unuseClientAuthenticationFlowIfNeeded(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            if(Objects.equals(existingRealm.getClientAuthenticationFlow(), topLevelFlowAlias)) {
                logger.debug("Temporary unuse client-authentication-flow in realm '{}' which is '{}'", realmImport.getRealm(), topLevelFlowAlias);
                unuseClientAuthenticationFlow(existingRealm);
            }
        }

        private void unuseDockerAuthenticationFlowIfNeeded(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            if(Objects.equals(existingRealm.getDockerAuthenticationFlow(), topLevelFlowAlias)) {
                logger.debug("Temporary unuse docker-authentication-flow in realm '{}' which is '{}'", realmImport.getRealm(), topLevelFlowAlias);
                unuseDockerAuthenticationFlow(existingRealm);
            }
        }

        private void unuseRegistrationFlowIfNeeded(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            if(Objects.equals(existingRealm.getRegistrationFlow(), topLevelFlowAlias)) {
                logger.debug("Temporary unuse registration-flow in realm '{}' which is '{}'", realmImport.getRealm(), topLevelFlowAlias);
                unuseRegistrationFlow(existingRealm);
            }
        }

        private void unuseResetCredentialsFlowIfNeeded(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            if(Objects.equals(existingRealm.getResetCredentialsFlow(), topLevelFlowAlias)) {
                logger.debug("Temporary unuse reset-credentials-flow in realm '{}' which is '{}'", realmImport.getRealm(), topLevelFlowAlias);
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
                logger.debug("Create top-level-flow '{}' in realm '{}' to be used temporarily", realmImport.getRealm(), TEMPORARY_CREATED_AUTH_FLOW);

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

        public void resetFlowIfNeeded() {
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
            resetBrowserFlowIfNeeded(existingRealm);
            resetDirectGrantFlowIfNeeded(existingRealm);
            resetClientAuthenticationFlowIfNeeded(existingRealm);
            resetDockerAuthenticationFlowIfNeeded(existingRealm);
            resetRegistrationFlowIfNeeded(existingRealm);
            resetCredentialsFlowIfNeeded(existingRealm);
        }

        private void resetBrowserFlowIfNeeded(RealmRepresentation existingRealm) {
            if(Strings.isNotBlank(browserFlow)) {
                logger.debug("Reset browser-flow in realm '{}' to '{}'", realmImport.getRealm(), browserFlow);

                existingRealm.setBrowserFlow(browserFlow);
            }
        }

        private void resetDirectGrantFlowIfNeeded(RealmRepresentation existingRealm) {
            if(Strings.isNotBlank(directGrantFlow)) {
                logger.debug("Reset direct-grant-flow in realm '{}' to '{}'", realmImport.getRealm(), directGrantFlow);

                existingRealm.setDirectGrantFlow(directGrantFlow);
            }
        }

        private void resetClientAuthenticationFlowIfNeeded(RealmRepresentation existingRealm) {
            if(Strings.isNotBlank(clientAuthenticationFlow)) {
                logger.debug("Reset client-authentication-flow in realm '{}' to '{}'", realmImport.getRealm(), clientAuthenticationFlow);

                existingRealm.setClientAuthenticationFlow(clientAuthenticationFlow);
            }
        }

        private void resetDockerAuthenticationFlowIfNeeded(RealmRepresentation existingRealm) {
            if(Strings.isNotBlank(dockerAuthenticationFlow)) {
                logger.debug("Reset docker-authentication-flow in realm '{}' to '{}'", realmImport.getRealm(), dockerAuthenticationFlow);

                existingRealm.setDockerAuthenticationFlow(dockerAuthenticationFlow);
            }
        }

        private void resetRegistrationFlowIfNeeded(RealmRepresentation existingRealm) {
            if(Strings.isNotBlank(registrationFlow)) {
                logger.debug("Reset registration-flow in realm '{}' to '{}'", realmImport.getRealm(), registrationFlow);

                existingRealm.setRegistrationFlow(registrationFlow);
            }
        }

        private void resetCredentialsFlowIfNeeded(RealmRepresentation existingRealm) {
            if(Strings.isNotBlank(resetCredentialsFlow)) {
                logger.debug("Reset reset-credentials-flow in realm '{}' to '{}'", realmImport.getRealm(), resetCredentialsFlow);

                existingRealm.setResetCredentialsFlow(resetCredentialsFlow);
            }
        }

        private void deleteTemporaryCreatedFlow() {
            logger.debug("Delete temporary created top-level-flow '{}' in realm '{}'", TEMPORARY_CREATED_AUTH_FLOW, realmImport.getRealm());

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
