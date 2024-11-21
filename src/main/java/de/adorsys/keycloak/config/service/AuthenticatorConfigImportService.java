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

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.AuthenticationFlowRepository;
import de.adorsys.keycloak.config.repository.AuthenticatorConfigRepository;
import de.adorsys.keycloak.config.repository.ExecutionFlowRepository;
import org.keycloak.representations.idm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class AuthenticatorConfigImportService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticatorConfigImportService.class);

    private final AuthenticationFlowRepository authenticationFlowRepository;
    private final ExecutionFlowRepository executionFlowRepository;
    private final AuthenticatorConfigRepository authenticatorConfigRepository;

    @Autowired
    public AuthenticatorConfigImportService(
            AuthenticationFlowRepository authenticationFlowRepository,
            ExecutionFlowRepository executionFlowRepository,
            AuthenticatorConfigRepository authenticatorConfigRepository
    ) {
        this.authenticationFlowRepository = authenticationFlowRepository;
        this.executionFlowRepository = executionFlowRepository;
        this.authenticatorConfigRepository = authenticatorConfigRepository;
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
        deleteUnused(realmImport);

        List<AuthenticatorConfigRepresentation> authenticatorConfigs = realmImport.getAuthenticatorConfig();
        if (authenticatorConfigs == null) return;

        for (AuthenticatorConfigRepresentation authenticatorConfig : authenticatorConfigs) {
            updateAuthenticatorConfig(realmImport, authenticatorConfig);
        }
    }

    public void deleteAuthenticationConfigs(RealmImport realmImport, AuthenticationFlowRepresentation authFlow) {
        List<AuthenticationExecutionInfoRepresentation> authenticationExecutions = executionFlowRepository
                .getExecutionsByAuthFlow(realmImport.getRealm(), authFlow.getAlias());

        authenticationExecutions.stream()
                .map(AuthenticationExecutionInfoRepresentation::getAuthenticationConfig)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(authenticationConfigId -> {
                    logger.debug("Delete authenticator config: '{}'", authenticationConfigId);
                    authenticatorConfigRepository.delete(realmImport.getRealm(), authenticationConfigId);
                });
    }

    private void deleteUnused(RealmImport realmImport) {
        List<AuthenticatorConfigRepresentation> unusedAuthenticatorConfigs = getUnusedAuthenticatorConfigs(realmImport);

        for (AuthenticatorConfigRepresentation unusedAuthenticatorConfig : unusedAuthenticatorConfigs) {
            logger.debug("Delete authenticator config: {}", unusedAuthenticatorConfig.getAlias());
            authenticatorConfigRepository.delete(realmImport.getRealm(), unusedAuthenticatorConfig.getId());
        }
    }

    /**
     * creates or updates only the top-level flow and its executions or execution-flows
     */
    private void updateAuthenticatorConfig(
            RealmImport realmImport,
            AuthenticatorConfigRepresentation authenticatorConfigRepresentation
    ) {
        List<AuthenticatorConfigRepresentation> existingAuthConfigs = authenticatorConfigRepository
                .getConfigsByAlias(realmImport.getRealm(), authenticatorConfigRepresentation.getAlias());

        if (existingAuthConfigs.isEmpty()) {
            throw new ImportProcessingException(String.format(
                    "Authenticator Config '%s' not found. Config must be used in execution",
                    authenticatorConfigRepresentation.getAlias()
            ));
        }

        existingAuthConfigs.forEach(existingAuthConfig -> {
            authenticatorConfigRepresentation.setId(existingAuthConfig.getId());
            authenticatorConfigRepository.update(realmImport.getRealm(), authenticatorConfigRepresentation);
        });
    }

    private List<AuthenticatorConfigRepresentation> getUnusedAuthenticatorConfigs(RealmImport realmImport) {
        List<AuthenticationFlowRepresentation> authenticationFlowsToImport = realmImport.getAuthenticationFlows();
        if (authenticationFlowsToImport == null) {
            return Collections.emptyList();
        }

        List<AuthenticationFlowRepresentation> authenticationFlows = mergeAuthenticationFlowsFromImportAndKeycloak(
                realmImport, authenticationFlowsToImport
        );

        List<AuthenticationExecutionExportRepresentation> authenticationExecutions = authenticationFlows
                .stream()
                .flatMap(
                        (Function<AuthenticationFlowRepresentation, Stream<AuthenticationExecutionExportRepresentation>>) x ->
                                x.getAuthenticationExecutions().stream()
                )
                .toList();

        List<AuthenticatorConfigRepresentation> authenticatorConfigs = authenticatorConfigRepository.getAll(realmImport.getRealm());

        List<String> authExecutionsWithAuthenticatorConfigs = authenticationExecutions
                .stream()
                .map(AbstractAuthenticationExecutionRepresentation::getAuthenticatorConfig)
                .filter(Objects::nonNull)
                .toList();

        return authenticatorConfigs
                .stream()
                .filter(x -> !authExecutionsWithAuthenticatorConfigs.contains(x.getAlias()))
                .toList();
    }

    private List<AuthenticationFlowRepresentation> mergeAuthenticationFlowsFromImportAndKeycloak(
            RealmImport realmImport,
            List<AuthenticationFlowRepresentation> authenticationFlowsToImport
    ) {
        List<AuthenticationFlowRepresentation> existingAuthenticationFlows = authenticationFlowRepository.getAll(realmImport.getRealm());
        List<AuthenticationFlowRepresentation> authenticationFlows = new ArrayList<>();

        // Merge authenticationFlows from keycloak and import
        for (AuthenticationFlowRepresentation existingAuthenticationFlow : existingAuthenticationFlows) {
            authenticationFlows.add(
                    authenticationFlowsToImport.stream()
                            .filter(flow -> Objects.equals(flow.getAlias(), existingAuthenticationFlow.getAlias()))
                            .findFirst()
                            .orElse(existingAuthenticationFlow)
            );
        }
        return authenticationFlows;
    }
}
