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

import de.adorsys.keycloak.config.exception.InvalidImportException;
import de.adorsys.keycloak.config.factory.UsedAuthenticationFlowWorkaroundFactory;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues;
import de.adorsys.keycloak.config.repository.AuthenticationFlowRepository;
import de.adorsys.keycloak.config.repository.ExecutionFlowRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    private final RealmRepository realmRepository;
    private final AuthenticationFlowRepository authenticationFlowRepository;
    private final ExecutionFlowsImportService executionFlowsImportService;
    private final ExecutionFlowRepository executionFlowRepository;
    private final UsedAuthenticationFlowWorkaroundFactory workaroundFactory;

    private final ImportConfigProperties importConfigProperties;

    @Autowired
    public AuthenticationFlowsImportService(
            RealmRepository realmRepository,
            AuthenticationFlowRepository authenticationFlowRepository,
            ExecutionFlowsImportService executionFlowsImportService,
            ExecutionFlowRepository executionFlowRepository,
            UsedAuthenticationFlowWorkaroundFactory workaroundFactory,
            ImportConfigProperties importConfigProperties
    ) {
        this.realmRepository = realmRepository;
        this.authenticationFlowRepository = authenticationFlowRepository;
        this.executionFlowsImportService = executionFlowsImportService;
        this.executionFlowRepository = executionFlowRepository;
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
        setupFlowsInRealm(realmImport);

        if (importConfigProperties.getManaged().getAuthenticationFlow() == ImportManagedPropertiesValues.FULL) {
            deleteTopLevelFlowsMissingInImport(realmImport, topLevelFlowsToImport);
        }
    }

    private void setupFlowsInRealm(RealmImport realmImport) {
        RealmRepresentation existingRealm = realmRepository.get(realmImport.getRealm());
        RealmRepresentation realmToUpdate = CloneUtil.deepPatchFieldsOnly(existingRealm, realmImport, RealmImportService.patchingPropertiesForFlowImport);

        realmRepository.update(realmToUpdate);
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
            RealmImport realmImport,
            AuthenticationFlowRepresentation topLevelFlowToImport
    ) {
        for (AuthenticationFlowRepresentation nonTopLevelFlowToImport : AuthenticationFlowUtil.getNonTopLevelFlowsForTopLevelFlow(realmImport, topLevelFlowToImport)) {
            if (isNonTopLevelFlowNotExistingOrHasToBeUpdated(realmImport, topLevelFlowToImport, nonTopLevelFlowToImport)) {
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
        return !CloneUtil.deepEquals(
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
        AuthenticationFlowRepresentation patchedAuthenticationFlow = CloneUtil.deepPatch(existingAuthenticationFlow, topLevelFlowToImport, "id");

        if (patchedAuthenticationFlow.isBuiltIn() || existingAuthenticationFlow.isBuiltIn()) {
            throw new InvalidImportException("Unable to recreate flow '" + patchedAuthenticationFlow.getAlias() + "' in realm '" + realm.getRealm() + "': Deletion or creation of built-in flows is not possible");
        }

        UsedAuthenticationFlowWorkaroundFactory.UsedAuthenticationFlowWorkaround workaround = workaroundFactory.buildFor(realm);
        workaround.disableTopLevelFlowIfNeeded(topLevelFlowToImport.getAlias());

        authenticationFlowRepository.deleteTopLevelFlow(realm.getRealm(), patchedAuthenticationFlow.getId());
        authenticationFlowRepository.createTopLevelFlow(realm.getRealm(), patchedAuthenticationFlow);

        AuthenticationFlowRepresentation createdTopLevelFlow = authenticationFlowRepository.getTopLevelFlow(realm.getRealm(), topLevelFlowToImport.getAlias());
        executionFlowsImportService.createExecutionsAndExecutionFlows(realm, topLevelFlowToImport, createdTopLevelFlow);

        workaround.resetFlowIfNeeded();
    }

    private void deleteTopLevelFlowsMissingInImport(RealmImport realmImport, List<AuthenticationFlowRepresentation> topLevelFlowsToImport) {
        String realm = realmImport.getRealm();
        List<AuthenticationFlowRepresentation> existingTopLevelFlows = authenticationFlowRepository.getTopLevelFlows(realm);
        existingTopLevelFlows = existingTopLevelFlows.stream().filter(flow -> !flow.isBuiltIn()).collect(Collectors.toList());

        /*
        if (importConfigProperties.isState()) {
            // ignore all object there are not in state
            existingTopLevelFlows = stateService.getTopLevelFlows(existingTopLevelFlows);
        }
        */

        for (AuthenticationFlowRepresentation existingTopLevelFlow : existingTopLevelFlows) {
            if (checkIfTopLevelFlowMissingImport(existingTopLevelFlow, topLevelFlowsToImport)) {
                logger.debug("Delete authentication flow: {}", existingTopLevelFlow.getAlias());
                authenticationFlowRepository.deleteTopLevelFlow(realm, existingTopLevelFlow.getId());
            }
        }
    }

    private boolean checkIfTopLevelFlowMissingImport(AuthenticationFlowRepresentation existingTopLevelFlow, List<AuthenticationFlowRepresentation> topLevelFlowsToImport) {
        return topLevelFlowsToImport.stream().noneMatch(topLevelFlow -> existingTopLevelFlow.getAlias().equals(topLevelFlow.getAlias()));
    }
}
