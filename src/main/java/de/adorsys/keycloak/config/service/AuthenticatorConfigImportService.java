/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2020 adorsys GmbH & Co. KG @ https://adorsys.de
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

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.AuthenticationFlowRepository;
import de.adorsys.keycloak.config.repository.AuthenticatorConfigRepository;
import org.keycloak.representations.idm.AbstractAuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AuthenticatorConfigImportService {
    private final AuthenticatorConfigRepository authenticatorConfigRepository;
    private final AuthenticationFlowRepository authenticationFlowRepository;

    @Autowired
    public AuthenticatorConfigImportService(
            AuthenticatorConfigRepository authenticatorConfigRepository,
            AuthenticationFlowRepository authenticationFlowRepository
    ) {
        this.authenticatorConfigRepository = authenticatorConfigRepository;
        this.authenticationFlowRepository = authenticationFlowRepository;
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

    private void deleteUnused(RealmImport realmImport) {
        getUnusedAuthenticatorConfigs(realmImport)
                .forEach(x ->
                        authenticatorConfigRepository.deletedAuthenticatorConfig(realmImport.getRealm(), x.getId())
                );
    }

    /**
     * creates or updates only the top-level flow and its executions or execution-flows
     */
    private void updateAuthenticatorConfig(
            RealmImport realm,
            AuthenticatorConfigRepresentation authenticatorConfigRepresentation
    ) {

        AuthenticatorConfigRepresentation existingAuthConfig = authenticatorConfigRepository
                .getAuthenticatorConfig(realm.getRealm(), authenticatorConfigRepresentation.getAlias());

        authenticatorConfigRepresentation.setId(existingAuthConfig.getId());
        authenticatorConfigRepository.updateAuthenticatorConfig(realm.getRealm(), authenticatorConfigRepresentation);
    }

    private List<AuthenticatorConfigRepresentation> getUnusedAuthenticatorConfigs(RealmImport realm) {
        List<AuthenticationFlowRepresentation> authenticationFlows = authenticationFlowRepository.getAll(realm.getRealm());

        List<AuthenticationExecutionExportRepresentation> authenticationExecutions = authenticationFlows
                .stream()
                .flatMap((Function<AuthenticationFlowRepresentation, Stream<AuthenticationExecutionExportRepresentation>>) x -> x.getAuthenticationExecutions().stream())
                .collect(Collectors.toList());

        List<AuthenticatorConfigRepresentation> authenticatorConfigs = authenticatorConfigRepository.getAll(realm.getRealm());

        List<String> authExecutionsWithAuthenticatorConfigs = authenticationExecutions
                .stream()
                .filter(x -> x.getAuthenticatorConfig() != null)
                .map(AbstractAuthenticationExecutionRepresentation::getAuthenticatorConfig)
                .collect(Collectors.toList());

        return authenticatorConfigs
                .stream()
                .filter(
                        x -> !authExecutionsWithAuthenticatorConfigs.contains(x.getAlias()))
                .collect(Collectors.toList());
    }
}
