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

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.exception.InvalidImportException;
import de.adorsys.keycloak.config.factory.UsedAuthenticationFlowWorkaroundFactory;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues;
import de.adorsys.keycloak.config.repository.AuthenticationFlowRepository;
import de.adorsys.keycloak.config.repository.RealmRepository;
import de.adorsys.keycloak.config.util.AuthenticationFlowUtil;
import de.adorsys.keycloak.config.util.CloneUtil;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * We have to import authentication-flows separately because in case of an existing realmName, keycloak is ignoring or
 * not supporting embedded objects in realm-import's property called "authenticationFlows"
 * <p>
 * Glossar:
 * topLevel-flow: any flow which has the property 'topLevel' set to 'true'. Can contain execution-flows and executions
 * sub-flow: any flow which has the property 'topLevel' set to 'false' and which are related to execution-flows within topLevel-flows
 */
@Service
public class AuthenticationFlowsImportService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFlowsImportService.class);

    private final RealmRepository realmRepository;
    private final AuthenticationFlowRepository authenticationFlowRepository;
    private final ExecutionFlowsImportService executionFlowsImportService;
    private final AuthenticatorConfigImportService authenticatorConfigImportService;
    private final UsedAuthenticationFlowWorkaroundFactory workaroundFactory;

    private final ImportConfigProperties importConfigProperties;

    @Autowired
    public AuthenticationFlowsImportService(
            RealmRepository realmRepository,
            AuthenticationFlowRepository authenticationFlowRepository,
            ExecutionFlowsImportService executionFlowsImportService,
            AuthenticatorConfigImportService authenticatorConfigImportService, UsedAuthenticationFlowWorkaroundFactory workaroundFactory,
            ImportConfigProperties importConfigProperties
    ) {
        this.realmRepository = realmRepository;
        this.authenticationFlowRepository = authenticationFlowRepository;
        this.executionFlowsImportService = executionFlowsImportService;
        this.authenticatorConfigImportService = authenticatorConfigImportService;
        this.workaroundFactory = workaroundFactory;
        this.importConfigProperties = importConfigProperties;
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
        List<AuthenticationFlowRepresentation> authenticationFlows = realmImport.getAuthenticationFlows();
        if (authenticationFlows == null) return;

        List<AuthenticationFlowRepresentation> topLevelFlowsToImport = AuthenticationFlowUtil.getTopLevelFlows(realmImport);
        createOrUpdateTopLevelFlows(realmImport, topLevelFlowsToImport);
        updateBuiltInFlows(realmImport, authenticationFlows);
        setupFlowsInRealm(realmImport);

        if (importConfigProperties.getManaged().getAuthenticationFlow() == ImportManagedPropertiesValues.FULL) {
            deleteTopLevelFlowsMissingInImport(realmImport, topLevelFlowsToImport);
        }
    }

    private void setupFlowsInRealm(RealmImport realmImport) {
        RealmRepresentation realm = realmRepository.get(realmImport.getRealm());

        realm.setBrowserFlow(realmImport.getBrowserFlow());
        realm.setDirectGrantFlow(realmImport.getDirectGrantFlow());
        realm.setClientAuthenticationFlow(realmImport.getClientAuthenticationFlow());
        realm.setDockerAuthenticationFlow(realmImport.getDockerAuthenticationFlow());
        realm.setRegistrationFlow(realmImport.getRegistrationFlow());
        realm.setResetCredentialsFlow(realmImport.getResetCredentialsFlow());

        realmRepository.update(realm);
    }

    /**
     * creates or updates only the top-level flows and its executions or execution-flows
     */
    private void createOrUpdateTopLevelFlows(RealmImport realmImport, List<AuthenticationFlowRepresentation> topLevelFlowsToImport) {
        for (AuthenticationFlowRepresentation topLevelFlowToImport : topLevelFlowsToImport) {
            if (!topLevelFlowToImport.isBuiltIn()) {
                createOrUpdateTopLevelFlow(realmImport, topLevelFlowToImport);
            }
        }
    }

    /**
     * creates or updates only the top-level flow and its executions or execution-flows
     */
    private void createOrUpdateTopLevelFlow(
            RealmImport realmImport,
            AuthenticationFlowRepresentation topLevelFlowToImport
    ) {
        String alias = topLevelFlowToImport.getAlias();

        Optional<AuthenticationFlowRepresentation> maybeTopLevelFlow = authenticationFlowRepository.searchByAlias(realmImport.getRealm(), alias);

        if (maybeTopLevelFlow.isPresent()) {
            AuthenticationFlowRepresentation existingTopLevelFlow = maybeTopLevelFlow.get();
            updateTopLevelFlowIfNeeded(realmImport, topLevelFlowToImport, existingTopLevelFlow);
        } else {
            createTopLevelFlow(realmImport, topLevelFlowToImport);
        }
    }

    private void createTopLevelFlow(RealmImport realmImport, AuthenticationFlowRepresentation topLevelFlowToImport) {
        logger.debug("Creating top-level flow: {}", topLevelFlowToImport.getAlias());
        authenticationFlowRepository.createTopLevel(realmImport.getRealm(), topLevelFlowToImport);

        AuthenticationFlowRepresentation createdTopLevelFlow = authenticationFlowRepository.getByAlias(
                realmImport.getRealm(), topLevelFlowToImport.getAlias()
        );
        executionFlowsImportService.createExecutionsAndExecutionFlows(realmImport, topLevelFlowToImport, createdTopLevelFlow);
    }

    private void updateTopLevelFlowIfNeeded(
            RealmImport realmName,
            AuthenticationFlowRepresentation topLevelFlowToImport,
            AuthenticationFlowRepresentation existingAuthenticationFlow
    ) {
        boolean hasToBeUpdated = hasAuthenticationFlowToBeUpdated(topLevelFlowToImport, existingAuthenticationFlow)
                || hasAnySubFlowToBeUpdated(realmName, topLevelFlowToImport);

        if (hasToBeUpdated) {
            logger.debug("Recreate top-level flow: {}", topLevelFlowToImport.getAlias());
            recreateTopLevelFlow(realmName, topLevelFlowToImport, existingAuthenticationFlow);
        } else {
            logger.debug("No need to update flow: {}", topLevelFlowToImport.getAlias());
        }
    }

    private boolean hasAnySubFlowToBeUpdated(
            RealmImport realmImport,
            AuthenticationFlowRepresentation topLevelFlowToImport
    ) {
        List<AuthenticationFlowRepresentation> subFlows = getAllSubFlows(realmImport, topLevelFlowToImport);

        for (AuthenticationFlowRepresentation subFlowToImport : subFlows) {
            if (isSubFlowNotExistingOrHasToBeUpdated(realmImport, topLevelFlowToImport, subFlowToImport)) {
                return true;
            }
        }

        return false;
    }

    private List<AuthenticationFlowRepresentation> getAllSubFlows(RealmImport realmImport,
                                                                  AuthenticationFlowRepresentation topLevelFlowToImport) {

        final List<AuthenticationFlowRepresentation> subFlows = AuthenticationFlowUtil.getSubFlowsForTopLevelFlow(
                realmImport, topLevelFlowToImport);
        final List<AuthenticationFlowRepresentation> allSubFlows = new ArrayList<>(subFlows);

        for (AuthenticationFlowRepresentation subflow : subFlows) {
            allSubFlows.addAll(getAllSubFlows(realmImport, subflow));
        }

        return allSubFlows;
    }

    private boolean isSubFlowNotExistingOrHasToBeUpdated(
            RealmImport realmImport,
            AuthenticationFlowRepresentation topLevelFlowToImport,
            AuthenticationFlowRepresentation subFlowToImport
    ) {
        Optional<AuthenticationExecutionInfoRepresentation> maybeSubFlow = authenticationFlowRepository.searchSubFlow(
                realmImport.getRealm(), topLevelFlowToImport.getAlias(), subFlowToImport.getAlias()
        );

        return maybeSubFlow
                .map(authenticationExecutionInfoRepresentation -> hasExistingSubFlowToBeUpdated(
                        realmImport, subFlowToImport, authenticationExecutionInfoRepresentation
                ))
                .orElse(true);
    }

    private boolean hasExistingSubFlowToBeUpdated(
            RealmImport realmImport,
            AuthenticationFlowRepresentation subFlowToImport,
            AuthenticationExecutionInfoRepresentation existingSubExecutionFlow
    ) {
        AuthenticationFlowRepresentation existingSubFlow = authenticationFlowRepository.getFlowById(
                realmImport.getRealm(), existingSubExecutionFlow.getFlowId()
        );

        return hasAuthenticationFlowToBeUpdated(subFlowToImport, existingSubFlow);
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
        return !CloneUtil.deepEquals(
                authenticationFlowToImport,
                existingAuthenticationFlow,
                "id"
        );
    }

    private void updateBuiltInFlows(
            RealmImport realmImport,
            List<AuthenticationFlowRepresentation> flowsToImport
    ) {
        for (AuthenticationFlowRepresentation flowToImport : flowsToImport) {
            if (!flowToImport.isBuiltIn()) continue;

            String flowAlias = flowToImport.getAlias();
            Optional<AuthenticationFlowRepresentation> maybeFlow = authenticationFlowRepository
                    .searchByAlias(realmImport.getRealm(), flowAlias);

            if (maybeFlow.isEmpty()) {
                throw new InvalidImportException(String.format(
                        "Cannot create flow '%s' in realm '%s': Unable to create built-in flows.",
                        flowToImport.getAlias(), realmImport.getRealm()
                ));
            }

            AuthenticationFlowRepresentation existingFlow = maybeFlow.get();
            if (hasAuthenticationFlowToBeUpdated(flowToImport, existingFlow)) {
                logger.debug("Updating builtin flow: {}", flowToImport.getAlias());
                updateBuiltInFlow(realmImport, flowToImport, existingFlow);
            }
        }
    }

    private void updateBuiltInFlow(
            RealmImport realmImport,
            AuthenticationFlowRepresentation topLevelFlowToImport,
            AuthenticationFlowRepresentation existingAuthenticationFlow
    ) {
        if (!existingAuthenticationFlow.isBuiltIn()) {
            throw new InvalidImportException(String.format(
                    "Unable to update flow '%s' in realm '%s': Change built-in flag is not possible",
                    topLevelFlowToImport.getAlias(), realmImport.getRealm()
            ));
        }
        AuthenticationFlowRepresentation patchedAuthenticationFlow = CloneUtil.patch(
                existingAuthenticationFlow, topLevelFlowToImport, "id"
        );

        authenticationFlowRepository.update(realmImport.getRealm(), patchedAuthenticationFlow);

        executionFlowsImportService.updateExecutionFlows(realmImport, topLevelFlowToImport);
    }

    /**
     * Deletes the top-level flow and all its executions and recreates them.
     */
    private void recreateTopLevelFlow(
            RealmImport realmImport,
            AuthenticationFlowRepresentation topLevelFlowToImport,
            AuthenticationFlowRepresentation existingAuthenticationFlow
    ) {
        AuthenticationFlowRepresentation patchedAuthenticationFlow = CloneUtil.patch(
                existingAuthenticationFlow, topLevelFlowToImport, "id"
        );

        if (existingAuthenticationFlow.isBuiltIn()) {
            throw new InvalidImportException(String.format(
                    "Unable to recreate flow '%s' in realm '%s': Deletion or creation of built-in flows is not possible",
                    patchedAuthenticationFlow.getAlias(), realmImport.getRealm()
            ));
        }

        UsedAuthenticationFlowWorkaroundFactory.UsedAuthenticationFlowWorkaround workaround = workaroundFactory.buildFor(realmImport);
        workaround.disableTopLevelFlowIfNeeded(topLevelFlowToImport.getAlias());

        final Map<String, Map<String, String>> overrides = workaround.removeFlowOverridesInClients(patchedAuthenticationFlow);

        authenticatorConfigImportService.deleteAuthenticationConfigs(realmImport, patchedAuthenticationFlow);
        authenticationFlowRepository.delete(realmImport.getRealm(), patchedAuthenticationFlow.getId());
        authenticationFlowRepository.createTopLevel(realmImport.getRealm(), patchedAuthenticationFlow);

        workaround.restoreClientOverrides(overrides);

        AuthenticationFlowRepresentation createdTopLevelFlow = authenticationFlowRepository.getByAlias(
                realmImport.getRealm(), topLevelFlowToImport.getAlias()
        );
        executionFlowsImportService.createExecutionsAndExecutionFlows(realmImport, topLevelFlowToImport, createdTopLevelFlow);

        workaround.resetFlowIfNeeded();
    }

    private void deleteTopLevelFlowsMissingInImport(
            RealmImport realmImport,
            List<AuthenticationFlowRepresentation> importedTopLevelFlows
    ) {
        String realmName = realmImport.getRealm();
        List<AuthenticationFlowRepresentation> existingTopLevelFlows = authenticationFlowRepository.getTopLevelFlows(realmName)
                .stream().filter(flow -> !flow.isBuiltIn()).toList();

        Set<String> topLevelFlowsToImportAliases = importedTopLevelFlows.stream()
                .map(AuthenticationFlowRepresentation::getAlias)
                .collect(Collectors.toSet());

        for (AuthenticationFlowRepresentation existingTopLevelFlow : existingTopLevelFlows) {
            if (topLevelFlowsToImportAliases.contains(existingTopLevelFlow.getAlias())) continue;

            logger.debug("Delete authentication flow: {}", existingTopLevelFlow.getAlias());
            authenticationFlowRepository.delete(realmName, existingTopLevelFlow.getId());
        }
    }
}
