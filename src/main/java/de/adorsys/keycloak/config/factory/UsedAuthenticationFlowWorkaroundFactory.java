/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2021 adorsys GmbH & Co. KG @ https://adorsys.com
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package de.adorsys.keycloak.config.factory;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.AuthenticationFlowRepository;
import de.adorsys.keycloak.config.repository.ClientRepository;
import de.adorsys.keycloak.config.repository.IdentityProviderRepository;
import de.adorsys.keycloak.config.repository.RealmRepository;
import de.adorsys.keycloak.config.util.CloneUtil;
import org.apache.logging.log4j.util.Strings;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UsedAuthenticationFlowWorkaroundFactory {

    private final RealmRepository realmRepository;
    private final IdentityProviderRepository identityProviderRepository;
    private final AuthenticationFlowRepository authenticationFlowRepository;
    private final ClientRepository clientRepository;

    @Autowired
    public UsedAuthenticationFlowWorkaroundFactory(
            RealmRepository realmRepository,
            IdentityProviderRepository identityProviderRepository,
            AuthenticationFlowRepository authenticationFlowRepository,
            ClientRepository clientRepository
    ) {
        this.realmRepository = realmRepository;
        this.identityProviderRepository = identityProviderRepository;
        this.authenticationFlowRepository = authenticationFlowRepository;
        this.clientRepository = clientRepository;
    }

    public UsedAuthenticationFlowWorkaround buildFor(RealmImport realmImport) {
        return new UsedAuthenticationFlowWorkaround(realmImport);
    }

    /**
     * There is no chance to update a top-level-flow, and it's not possible to recreate a top-level-flow
     * which is currently in use.
     * So we have to disable our top-level-flow by use a temporary created flow as long as updating the considered flow.
     * This code could be maybe replace by a better update-algorithm of top-level-flows
     */
    public class UsedAuthenticationFlowWorkaround {
        private static final String TEMPORARY_CREATED_AUTH_FLOW = "TEMPORARY_CREATED_AUTH_FLOW";
        private static final String TEMPORARY_CREATED_CLIENT_AUTH_FLOW = "TEMPORARY_CREATED_CLIENT_AUTH_FLOW";
        private final Logger logger = LoggerFactory.getLogger(UsedAuthenticationFlowWorkaround.class);
        private final RealmImport realmImport;
        private final Map<String, String> resetFirstBrokerLoginFlow = new HashMap<>();
        private final Map<String, String> resetPostBrokerLoginFlow = new HashMap<>();
        private String browserFlow;
        private String directGrantFlow;
        private String clientAuthenticationFlow;
        private String dockerAuthenticationFlow;
        private String registrationFlow;
        private String resetCredentialsFlow;

        private UsedAuthenticationFlowWorkaround(RealmImport realmImport) {
            this.realmImport = realmImport;
        }

        public void disableTopLevelFlowIfNeeded(String topLevelFlowAlias) {
            RealmRepresentation existingRealm = realmRepository.get(realmImport.getRealm());

            disableBrowserFlowIfNeeded(topLevelFlowAlias, existingRealm);
            disableDirectGrantFlowIfNeeded(topLevelFlowAlias, existingRealm);
            disableClientAuthenticationFlowIfNeeded(topLevelFlowAlias, existingRealm);
            disableDockerAuthenticationFlowIfNeeded(topLevelFlowAlias, existingRealm);
            disableRegistrationFlowIfNeeded(topLevelFlowAlias, existingRealm);
            disableResetCredentialsFlowIfNeeded(topLevelFlowAlias, existingRealm);
            disableFirstBrokerLoginFlowsIfNeeded(topLevelFlowAlias, existingRealm);
            disablePostBrokerLoginFlowsIfNeeded(topLevelFlowAlias, existingRealm);
        }

        /**
         * Find and remove flow overrides with specified ID in all realm clients.
         *
         * @param patchedAuthenticationFlow flow to remove overrides
         * @return Map "client" -> "auth name" -> "flow id" which were removed. Used to restore overrides.
         */
        public Map<String, Map<String, String>> removeFlowOverridesInClients(AuthenticationFlowRepresentation patchedAuthenticationFlow) {
            final String flowId = patchedAuthenticationFlow.getId();

            final Map<String, Map<String, String>> clientsWithFlow = new HashMap<>();
            // For all clients
            for (ClientRepresentation client : clientRepository.getAll(realmImport.getRealm())) {
                boolean updateClient = false;
                final Map<String, String> authenticationFlowBindingOverrides = client.getAuthenticationFlowBindingOverrides();
                // Search overrides with flowId
                for (Map.Entry<String, String> flowBinding : authenticationFlowBindingOverrides.entrySet()) {
                    if (flowId.equals(flowBinding.getValue())) {
                        final Map<String, String> clientBinding = clientsWithFlow.computeIfAbsent(client.getClientId(), k -> new HashMap<>());
                        // Save override and ...
                        clientBinding.put(flowBinding.getKey(), flowBinding.getValue());

                        // Search or create temporary auth flow
                        final String temporaryClientFlow = createTemporaryClientFlow(patchedAuthenticationFlow);

                        authenticationFlowBindingOverrides.put(flowBinding.getKey(), temporaryClientFlow);
                        updateClient = true;
                    }
                }
                // Update client only if needed
                if (updateClient) {
                    clientRepository.update(realmImport.getRealm(), client);
                }
            }

            return clientsWithFlow;
        }

        /**
         * Restore flow overrides in clients.
         *
         * @param clientsWithFlow map "client" -> "auth name" -> "flow id" to restore flow overrides.
         */
        public void restoreClientOverrides(Map<String, Map<String, String>> clientsWithFlow) {
            boolean removeTemporaryFlow = false;

            // restore overrides with the new patched flow
            for (Map.Entry<String, Map<String, String>> clientWithFlow : clientsWithFlow.entrySet()) {
                removeTemporaryFlow = true;
                final String clientId = clientWithFlow.getKey();
                final Map<String, String> overrides = clientWithFlow.getValue();

                final ClientRepresentation client = clientRepository.getByClientId(realmImport.getRealm(), clientId);
                // Add all overrides with patched flow to existing overrides
                client.getAuthenticationFlowBindingOverrides().putAll(overrides);
                clientRepository.update(realmImport.getRealm(), client);
            }

            if (removeTemporaryFlow) {
                searchForTemporaryCreatedClientFlow().ifPresent(flow -> {
                    authenticationFlowRepository.delete(realmImport.getRealm(), flow.getId());
                });
            }
        }

        private void disableBrowserFlowIfNeeded(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            if (Objects.equals(existingRealm.getBrowserFlow(), topLevelFlowAlias)) {
                logger.debug(
                        "Temporary disable browser-flow in realm '{}' which is '{}'",
                        realmImport.getRealm(), topLevelFlowAlias
                );
                disableBrowserFlow(existingRealm);
            }
        }

        private void disableDirectGrantFlowIfNeeded(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            if (Objects.equals(existingRealm.getDirectGrantFlow(), topLevelFlowAlias)) {
                logger.debug(
                        "Temporary disable direct-grant-flow in realm '{}' which is '{}'",
                        realmImport.getRealm(), topLevelFlowAlias
                );
                disableDirectGrantFlow(existingRealm);
            }
        }

        private void disableClientAuthenticationFlowIfNeeded(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            if (Objects.equals(existingRealm.getClientAuthenticationFlow(), topLevelFlowAlias)) {
                logger.debug(
                        "Temporary disable client-authentication-flow in realm '{}' which is '{}'",
                        realmImport.getRealm(), topLevelFlowAlias
                );
                disableClientAuthenticationFlow(existingRealm);
            }
        }

        private void disableDockerAuthenticationFlowIfNeeded(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            if (Objects.equals(existingRealm.getDockerAuthenticationFlow(), topLevelFlowAlias)) {
                logger.debug(
                        "Temporary disable docker-authentication-flow in realm '{}' which is '{}'",
                        realmImport.getRealm(), topLevelFlowAlias
                );
                disableDockerAuthenticationFlow(existingRealm);
            }
        }

        private void disableRegistrationFlowIfNeeded(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            if (Objects.equals(existingRealm.getRegistrationFlow(), topLevelFlowAlias)) {
                logger.debug(
                        "Temporary disable registration-flow in realm '{}' which is '{}'",
                        realmImport.getRealm(), topLevelFlowAlias
                );
                disableRegistrationFlow(existingRealm);
            }
        }

        private void disableResetCredentialsFlowIfNeeded(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            if (Objects.equals(existingRealm.getResetCredentialsFlow(), topLevelFlowAlias)) {
                logger.debug(
                        "Temporary disable reset-credentials-flow in realm '{}' which is '{}'",
                        realmImport.getRealm(), topLevelFlowAlias
                );
                disableResetCredentialsFlow(existingRealm);
            }
        }

        private void disableFirstBrokerLoginFlowsIfNeeded(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            List<IdentityProviderRepresentation> identityProviders = identityProviderRepository.getAll(existingRealm.getRealm());
            if (identityProviders != null) {
                for (IdentityProviderRepresentation identityProvider : identityProviders) {
                    if (Objects.equals(identityProvider.getFirstBrokerLoginFlowAlias(), topLevelFlowAlias)) {
                        logger.debug(
                                "Temporary disable first-broker-login-flow for "
                                        + "identity-provider '{}' in realm '{}' which is '{}'",
                                identityProvider.getAlias(), realmImport.getRealm(), topLevelFlowAlias
                        );

                        disableFirstBrokerLoginFlow(existingRealm.getRealm(), identityProvider);
                    }
                }
            }
        }

        private void disablePostBrokerLoginFlowsIfNeeded(String topLevelFlowAlias, RealmRepresentation existingRealm) {
            List<IdentityProviderRepresentation> identityProviders = identityProviderRepository.getAll(existingRealm.getRealm());
            if (identityProviders != null) {
                for (IdentityProviderRepresentation identityProvider : identityProviders) {
                    if (Objects.equals(identityProvider.getPostBrokerLoginFlowAlias(), topLevelFlowAlias)) {
                        logger.debug(
                                "Temporary disable post-broker-login-flow for "
                                        + "identity-provider '{}' in realm '{}' which is '{}'",
                                identityProvider.getAlias(), realmImport.getRealm(), topLevelFlowAlias
                        );

                        disablePostBrokerLoginFlow(existingRealm.getRealm(), identityProvider);
                    }
                }
            }
        }

        private void disableBrowserFlow(RealmRepresentation existingRealm) {
            String otherFlowAlias = searchTemporaryCreatedTopLevelFlowForReplacement();

            browserFlow = existingRealm.getBrowserFlow();

            existingRealm.setBrowserFlow(otherFlowAlias);
            realmRepository.update(existingRealm);
        }

        private void disableDirectGrantFlow(RealmRepresentation existingRealm) {
            String otherFlowAlias = searchTemporaryCreatedTopLevelFlowForReplacement();

            directGrantFlow = existingRealm.getDirectGrantFlow();

            existingRealm.setDirectGrantFlow(otherFlowAlias);
            realmRepository.update(existingRealm);
        }

        private void disableClientAuthenticationFlow(RealmRepresentation existingRealm) {
            String otherFlowAlias = searchTemporaryCreatedTopLevelFlowForReplacement();

            clientAuthenticationFlow = existingRealm.getClientAuthenticationFlow();

            existingRealm.setClientAuthenticationFlow(otherFlowAlias);
            realmRepository.update(existingRealm);
        }

        private void disableDockerAuthenticationFlow(RealmRepresentation existingRealm) {
            String otherFlowAlias = searchTemporaryCreatedTopLevelFlowForReplacement();

            dockerAuthenticationFlow = existingRealm.getDockerAuthenticationFlow();

            existingRealm.setDockerAuthenticationFlow(otherFlowAlias);
            realmRepository.update(existingRealm);
        }

        private void disableRegistrationFlow(RealmRepresentation existingRealm) {
            String otherFlowAlias = searchTemporaryCreatedTopLevelFlowForReplacement();

            registrationFlow = existingRealm.getRegistrationFlow();

            existingRealm.setRegistrationFlow(otherFlowAlias);
            realmRepository.update(existingRealm);
        }

        private void disableResetCredentialsFlow(RealmRepresentation existingRealm) {
            String otherFlowAlias = searchTemporaryCreatedTopLevelFlowForReplacement();

            resetCredentialsFlow = existingRealm.getResetCredentialsFlow();

            existingRealm.setResetCredentialsFlow(otherFlowAlias);
            realmRepository.update(existingRealm);
        }

        private void disableFirstBrokerLoginFlow(String realmName, IdentityProviderRepresentation identityProvider) {
            String otherFlowAlias = searchTemporaryCreatedTopLevelFlowForReplacement();

            resetFirstBrokerLoginFlow.put(identityProvider.getAlias(), identityProvider
                    .getFirstBrokerLoginFlowAlias());

            identityProvider.setFirstBrokerLoginFlowAlias(otherFlowAlias);
            identityProviderRepository.update(realmName, identityProvider);
        }

        private void disablePostBrokerLoginFlow(String realmName, IdentityProviderRepresentation identityProvider) {
            String otherFlowAlias = searchTemporaryCreatedTopLevelFlowForReplacement();

            resetPostBrokerLoginFlow.put(identityProvider.getAlias(), identityProvider
                    .getPostBrokerLoginFlowAlias());

            identityProvider.setPostBrokerLoginFlowAlias(otherFlowAlias);
            identityProviderRepository.update(realmName, identityProvider);
        }

        private String searchTemporaryCreatedTopLevelFlowForReplacement() {
            AuthenticationFlowRepresentation otherFlow;

            Optional<AuthenticationFlowRepresentation> maybeTemporaryCreatedFlow = searchForTemporaryCreatedFlow();

            if (maybeTemporaryCreatedFlow.isPresent()) {
                otherFlow = maybeTemporaryCreatedFlow.get();
            } else {
                logger.debug(
                        "Create top-level-flow '{}' in realm '{}' to be used temporarily",
                        realmImport.getRealm(), TEMPORARY_CREATED_AUTH_FLOW
                );

                AuthenticationFlowRepresentation temporaryCreatedFlow = setupTemporaryCreatedFlow();
                authenticationFlowRepository.createTopLevel(realmImport.getRealm(), temporaryCreatedFlow);

                otherFlow = temporaryCreatedFlow;
            }

            return otherFlow.getAlias();
        }

        private Optional<AuthenticationFlowRepresentation> searchForTemporaryCreatedFlow() {
            List<AuthenticationFlowRepresentation> existingTopLevelFlows = authenticationFlowRepository
                    .getTopLevelFlows(realmImport.getRealm());

            return existingTopLevelFlows.stream()
                .filter(f -> Objects.equals(f.getAlias(), TEMPORARY_CREATED_AUTH_FLOW))
                .findFirst();
        }

        private Optional<AuthenticationFlowRepresentation> searchForTemporaryCreatedClientFlow() {
            List<AuthenticationFlowRepresentation> existingTopLevelFlows = authenticationFlowRepository
                    .getTopLevelFlows(realmImport.getRealm());

            return existingTopLevelFlows.stream()
                    .filter(f -> Objects.equals(f.getAlias(), TEMPORARY_CREATED_CLIENT_AUTH_FLOW))
                    .findFirst();
        }

        public void resetFlowIfNeeded() {
            if (hasToResetFlows()) {
                RealmRepresentation existingRealm = realmRepository.get(realmImport.getRealm());

                resetFlows(existingRealm);
                realmRepository.update(existingRealm);

                if (!flowInUse()) {
                    deleteTemporaryCreatedFlow();
                }
            }
        }

        private boolean flowInUse() {
            RealmRepresentation existingRealm = realmRepository.get(realmImport.getRealm());
            return existingRealm.getBrowserFlow().equals(TEMPORARY_CREATED_AUTH_FLOW)
                    || existingRealm.getDirectGrantFlow().equals(TEMPORARY_CREATED_AUTH_FLOW)
                    || existingRealm.getClientAuthenticationFlow().equals(TEMPORARY_CREATED_AUTH_FLOW)
                    || existingRealm.getDockerAuthenticationFlow().equals(TEMPORARY_CREATED_AUTH_FLOW)
                    || existingRealm.getRegistrationFlow().equals(TEMPORARY_CREATED_AUTH_FLOW)
                    || existingRealm.getResetCredentialsFlow().equals(TEMPORARY_CREATED_AUTH_FLOW);
        }

        private boolean hasToResetFlows() {
            return Strings.isNotBlank(browserFlow)
                    || Strings.isNotBlank(directGrantFlow)
                    || Strings.isNotBlank(clientAuthenticationFlow)
                    || Strings.isNotBlank(dockerAuthenticationFlow)
                    || Strings.isNotBlank(registrationFlow)
                    || Strings.isNotBlank(resetCredentialsFlow)
                    || !resetFirstBrokerLoginFlow.isEmpty()
                    || !resetPostBrokerLoginFlow.isEmpty();
        }

        private void resetFlows(RealmRepresentation existingRealm) {
            resetBrowserFlowIfNeeded(existingRealm);
            resetDirectGrantFlowIfNeeded(existingRealm);
            resetClientAuthenticationFlowIfNeeded(existingRealm);
            resetDockerAuthenticationFlowIfNeeded(existingRealm);
            resetRegistrationFlowIfNeeded(existingRealm);
            resetCredentialsFlowIfNeeded(existingRealm);
            resetFirstBrokerLoginFlowsIfNeeded(existingRealm);
            resetPostBrokerLoginFlowsIfNeeded(existingRealm);
        }

        private void resetBrowserFlowIfNeeded(RealmRepresentation existingRealm) {
            if (Strings.isNotBlank(browserFlow)) {
                logger.debug(
                        "Reset browser-flow in realm '{}' to '{}'",
                        realmImport.getRealm(), browserFlow
                );

                existingRealm.setBrowserFlow(browserFlow);
            }
        }

        private void resetDirectGrantFlowIfNeeded(RealmRepresentation existingRealm) {
            if (Strings.isNotBlank(directGrantFlow)) {
                logger.debug(
                        "Reset direct-grant-flow in realm '{}' to '{}'",
                        realmImport.getRealm(), directGrantFlow
                );

                existingRealm.setDirectGrantFlow(directGrantFlow);
            }
        }

        private void resetClientAuthenticationFlowIfNeeded(RealmRepresentation existingRealm) {
            if (Strings.isNotBlank(clientAuthenticationFlow)) {
                logger.debug(
                        "Reset client-authentication-flow in realm '{}' to '{}'",
                        realmImport.getRealm(), clientAuthenticationFlow
                );

                existingRealm.setClientAuthenticationFlow(clientAuthenticationFlow);
            }
        }

        private void resetDockerAuthenticationFlowIfNeeded(RealmRepresentation existingRealm) {
            if (Strings.isNotBlank(dockerAuthenticationFlow)) {
                logger.debug(
                        "Reset docker-authentication-flow in realm '{}' to '{}'",
                        realmImport.getRealm(), dockerAuthenticationFlow
                );

                existingRealm.setDockerAuthenticationFlow(dockerAuthenticationFlow);
            }
        }

        private void resetRegistrationFlowIfNeeded(RealmRepresentation existingRealm) {
            if (Strings.isNotBlank(registrationFlow)) {
                logger.debug(
                        "Reset registration-flow in realm '{}' to '{}'",
                        realmImport.getRealm(), registrationFlow
                );

                existingRealm.setRegistrationFlow(registrationFlow);
            }
        }

        private void resetCredentialsFlowIfNeeded(RealmRepresentation existingRealm) {
            if (Strings.isNotBlank(resetCredentialsFlow)) {
                logger.debug(
                        "Reset reset-credentials-flow in realm '{}' to '{}'",
                        realmImport.getRealm(), resetCredentialsFlow
                );

                existingRealm.setResetCredentialsFlow(resetCredentialsFlow);
            }
        }

        private void resetFirstBrokerLoginFlowsIfNeeded(RealmRepresentation existingRealm) {
            for (Map.Entry<String, String> entry : resetFirstBrokerLoginFlow.entrySet()) {
                logger.debug(
                        "Reset first-broker-login-flow for identity-provider '{}' in realm '{}' to '{}'",
                        entry.getKey(), realmImport.getRealm(), resetCredentialsFlow
                );

                IdentityProviderRepresentation identityProviderRepresentation = identityProviderRepository
                        .getByAlias(existingRealm.getRealm(), entry.getKey());

                identityProviderRepresentation.setFirstBrokerLoginFlowAlias(entry.getValue());
                identityProviderRepository.update(existingRealm.getRealm(), identityProviderRepresentation);
            }
        }

        private void resetPostBrokerLoginFlowsIfNeeded(RealmRepresentation existingRealm) {
            for (Map.Entry<String, String> entry : resetPostBrokerLoginFlow.entrySet()) {
                logger.debug(
                        "Reset post-broker-login-flow for identity-provider '{}' in realm '{}' to '{}'",
                        entry.getKey(), realmImport.getRealm(), resetCredentialsFlow
                );

                IdentityProviderRepresentation identityProviderRepresentation = identityProviderRepository
                        .getByAlias(existingRealm.getRealm(), entry.getKey());

                identityProviderRepresentation.setPostBrokerLoginFlowAlias(entry.getValue());
                identityProviderRepository.update(existingRealm.getRealm(), identityProviderRepresentation);
            }
        }

        private void deleteTemporaryCreatedFlow() {
            logger.debug("Delete temporary created top-level-flow '{}' in realm '{}'",
                    TEMPORARY_CREATED_AUTH_FLOW, realmImport.getRealm());

            AuthenticationFlowRepresentation existingTemporaryCreatedFlow = authenticationFlowRepository
                    .getByAlias(realmImport.getRealm(), TEMPORARY_CREATED_AUTH_FLOW);

            authenticationFlowRepository.delete(realmImport.getRealm(), existingTemporaryCreatedFlow.getId());
        }

        private AuthenticationFlowRepresentation setupTemporaryCreatedFlow() {
            AuthenticationFlowRepresentation tempFlow = new AuthenticationFlowRepresentation();

            tempFlow.setAlias(TEMPORARY_CREATED_AUTH_FLOW);
            tempFlow.setTopLevel(true);
            tempFlow.setBuiltIn(false);
            tempFlow.setProviderId(TEMPORARY_CREATED_AUTH_FLOW);

            return tempFlow;
        }

        private AuthenticationFlowRepresentation setupTemporaryClientFlow(AuthenticationFlowRepresentation patchedAuthenticationFlow) {
            AuthenticationFlowRepresentation tempFlow = CloneUtil.deepClone(patchedAuthenticationFlow, "id", "alias");

            tempFlow.setAlias(TEMPORARY_CREATED_CLIENT_AUTH_FLOW);
            tempFlow.setProviderId(TEMPORARY_CREATED_CLIENT_AUTH_FLOW);

            return tempFlow;
        }

        private String createTemporaryClientFlow(AuthenticationFlowRepresentation patchedAuthenticationFlow) {
            Optional<AuthenticationFlowRepresentation> authenticationFlowRepresentation = searchForTemporaryCreatedClientFlow();
            if (authenticationFlowRepresentation.isPresent()) {
                return authenticationFlowRepresentation.get().getId();
            }

            authenticationFlowRepository.createTopLevel(realmImport.getRealm(), setupTemporaryClientFlow(patchedAuthenticationFlow));

            return searchForTemporaryCreatedClientFlow().orElseThrow(
                () -> new RuntimeException("Unable to create temporary client authorization flow")).getId();
        }
    }
}
