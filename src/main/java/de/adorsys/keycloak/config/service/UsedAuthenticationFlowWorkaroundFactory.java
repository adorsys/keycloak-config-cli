/*
 * Copyright 2019-2020 adorsys GmbH & Co. KG @ https://adorsys.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.AuthenticationFlowRepository;
import de.adorsys.keycloak.config.repository.RealmRepository;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Dependent
public class UsedAuthenticationFlowWorkaroundFactory {

    @Inject
    RealmRepository realmRepository;

    @Inject
    AuthenticationFlowRepository authenticationFlowRepository;

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
        private static final String TEMPORARY_CREATED_AUTH_FLOW = "TEMPORARY_CREATED_AUTH_FLOW";

        private final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(UsedAuthenticationFlowWorkaround.class);
        private final RealmImport realmImport;

        private String browserFlow;
        private String directGrantFlow;
        private String clientAuthenticationFlow;
        private String dockerAuthenticationFlow;
        private String registrationFlow;
        private String resetCredentialsFlow;

        UsedAuthenticationFlowWorkaround(RealmImport realmImport) {
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
            if (Objects.equals(existingRealm.getBrowserFlow(), topLevelFlowAlias)) {
                LOG.debugf("Temporary unuse browser-flow in realm '%s' which is '%s'", realmImport.getRealm(), topLevelFlowAlias);
                unuseBrowserFlow(existingRealm);
            }
        }

        private void unuseDirectGrantFlowIfNeeded(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            if (Objects.equals(existingRealm.getDirectGrantFlow(), topLevelFlowAlias)) {
                LOG.debugf("Temporary unuse direct-grant-flow in realm '%s' which is '%s'", realmImport.getRealm(), topLevelFlowAlias);
                unuseDirectGrantFlow(existingRealm);
            }
        }

        private void unuseClientAuthenticationFlowIfNeeded(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            if (Objects.equals(existingRealm.getClientAuthenticationFlow(), topLevelFlowAlias)) {
                LOG.debugf("Temporary unuse client-authentication-flow in realm '%s' which is '%s'", realmImport.getRealm(), topLevelFlowAlias);
                unuseClientAuthenticationFlow(existingRealm);
            }
        }

        private void unuseDockerAuthenticationFlowIfNeeded(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            if (Objects.equals(existingRealm.getDockerAuthenticationFlow(), topLevelFlowAlias)) {
                LOG.debugf("Temporary unuse docker-authentication-flow in realm '%s' which is '%s'", realmImport.getRealm(), topLevelFlowAlias);
                unuseDockerAuthenticationFlow(existingRealm);
            }
        }

        private void unuseRegistrationFlowIfNeeded(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            if (Objects.equals(existingRealm.getRegistrationFlow(), topLevelFlowAlias)) {
                LOG.debugf("Temporary unuse registration-flow in realm '%s' which is '%s'", realmImport.getRealm(), topLevelFlowAlias);
                unuseRegistrationFlow(existingRealm);
            }
        }

        private void unuseResetCredentialsFlowIfNeeded(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            if (Objects.equals(existingRealm.getResetCredentialsFlow(), topLevelFlowAlias)) {
                LOG.debugf("Temporary unuse reset-credentials-flow in realm '%s' which is '%s'", realmImport.getRealm(), topLevelFlowAlias);
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

            if (maybeTemporaryCreatedFlow.isPresent()) {
                otherFlow = maybeTemporaryCreatedFlow.get();
            } else {
                LOG.debugf("Create top-level-flow '%s' in realm '%s' to be used temporarily", realmImport.getRealm(), TEMPORARY_CREATED_AUTH_FLOW);

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
            return (browserFlow != null && !browserFlow.isEmpty()) ||
                    (directGrantFlow != null && !directGrantFlow.isEmpty()) ||
                    (clientAuthenticationFlow != null && !clientAuthenticationFlow.isEmpty()) ||
                    (dockerAuthenticationFlow != null && !dockerAuthenticationFlow.isEmpty()) ||
                    (registrationFlow != null && !registrationFlow.isEmpty()) ||
                    (resetCredentialsFlow != null && !resetCredentialsFlow.isEmpty());
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
            if (browserFlow != null && !browserFlow.isEmpty()) {
                LOG.debugf("Reset browser-flow in realm '%s' to '%s'", realmImport.getRealm(), browserFlow);

                existingRealm.setBrowserFlow(browserFlow);
            }
        }

        private void resetDirectGrantFlowIfNeeded(RealmRepresentation existingRealm) {
            if (directGrantFlow != null && !directGrantFlow.isEmpty()) {
                LOG.debugf("Reset direct-grant-flow in realm '%s' to '%s'", realmImport.getRealm(), directGrantFlow);

                existingRealm.setDirectGrantFlow(directGrantFlow);
            }
        }

        private void resetClientAuthenticationFlowIfNeeded(RealmRepresentation existingRealm) {
            if (clientAuthenticationFlow != null && !clientAuthenticationFlow.isEmpty()) {
                LOG.debugf("Reset client-authentication-flow in realm '%s' to '%s'", realmImport.getRealm(), clientAuthenticationFlow);

                existingRealm.setClientAuthenticationFlow(clientAuthenticationFlow);
            }
        }

        private void resetDockerAuthenticationFlowIfNeeded(RealmRepresentation existingRealm) {
            if (dockerAuthenticationFlow != null && !dockerAuthenticationFlow.isEmpty()) {
                LOG.debugf("Reset docker-authentication-flow in realm '%s' to '%s'", realmImport.getRealm(), dockerAuthenticationFlow);

                existingRealm.setDockerAuthenticationFlow(dockerAuthenticationFlow);
            }
        }

        private void resetRegistrationFlowIfNeeded(RealmRepresentation existingRealm) {
            if (registrationFlow != null && !registrationFlow.isEmpty()) {
                LOG.debugf("Reset registration-flow in realm '%s' to '%s'", realmImport.getRealm(), registrationFlow);

                existingRealm.setRegistrationFlow(registrationFlow);
            }
        }

        private void resetCredentialsFlowIfNeeded(RealmRepresentation existingRealm) {
            if (resetCredentialsFlow != null && !resetCredentialsFlow.isEmpty()) {
                LOG.debugf("Reset reset-credentials-flow in realm '%s' to '%s'", realmImport.getRealm(), resetCredentialsFlow);

                existingRealm.setResetCredentialsFlow(resetCredentialsFlow);
            }
        }

        private void deleteTemporaryCreatedFlow() {
            LOG.debugf("Delete temporary created top-level-flow '%s' in realm '%s'", TEMPORARY_CREATED_AUTH_FLOW, realmImport.getRealm());

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
