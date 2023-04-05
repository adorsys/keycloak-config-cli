/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2022 adorsys GmbH & Co. KG @ https://adorsys.com
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

package de.adorsys.keycloak.config.service.normalize;

import org.javers.core.Javers;
import org.keycloak.representations.idm.AbstractAuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.adorsys.keycloak.config.service.normalize.RealmNormalizationService.getNonNull;
import static java.util.function.Predicate.not;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "NORMALIZE")
public class AuthFlowNormalizationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthFlowNormalizationService.class);

    private final Javers unOrderedJavers;

    public AuthFlowNormalizationService(Javers unOrderedJavers) {
        this.unOrderedJavers = unOrderedJavers;
    }

    public List<AuthenticationFlowRepresentation> normalizeAuthFlows(List<AuthenticationFlowRepresentation> exportedAuthFlows,
                                                                     List<AuthenticationFlowRepresentation> baselineAuthFlows) {
        var exportedFiltered = filterBuiltIn(exportedAuthFlows);
        var baselineFiltered = filterBuiltIn(baselineAuthFlows);

        Map<String, AuthenticationFlowRepresentation> exportedMap = exportedFiltered.stream()
                .collect(Collectors.toMap(AuthenticationFlowRepresentation::getAlias, Function.identity()));
        Map<String, AuthenticationFlowRepresentation> baselineMap = baselineFiltered.stream()
                .collect(Collectors.toMap(AuthenticationFlowRepresentation::getAlias, Function.identity()));

        List<AuthenticationFlowRepresentation> normalizedFlows = new ArrayList<>();
        for (var entry : baselineMap.entrySet()) {
            var alias = entry.getKey();
            var exportedFlow = exportedMap.remove(alias);
            if (exportedFlow == null) {
                logger.warn("Default realm authentication flow '{}' was deleted in exported realm. It may be reintroduced during import", alias);
                continue;
            }
            var baselineFlow = entry.getValue();
            var diff = unOrderedJavers.compare(baselineFlow, exportedFlow);

            if (diff.hasChanges() || executionsChanged(exportedFlow.getAuthenticationExecutions(), baselineFlow.getAuthenticationExecutions())) {
                normalizedFlows.add(exportedFlow);
            }
        }
        normalizedFlows.addAll(exportedMap.values());
        for (var flow : normalizedFlows) {
            flow.setId(null);

            /*
             * Very old (upgraded) keycloak systems will sometimes export executions that are flows with an authenticator value of
             * "registration-page-form". This is semantically invalid, and keycloak-config-cli refuses such configurations on import.
             * Explicitly remove the authenticator here if the execution is a flow (has a flow alias)
             */
            for (var execution : flow.getAuthenticationExecutions()) {
                if (execution.getFlowAlias() != null) {
                    execution.setAuthenticator(null);
                }
            }
        }
        return normalizedFlows.isEmpty() ? null : filterUnusedNonTopLevel(normalizedFlows);
    }

    public List<AuthenticatorConfigRepresentation> normalizeAuthConfig(List<AuthenticatorConfigRepresentation> configs,
                                                                       List<AuthenticationFlowRepresentation> flows) {
        List<AuthenticationFlowRepresentation> flowsOrEmpty = flows == null ? List.of() : flows;
        // Find out which configs are actually used by the normalized flows
        var usedConfigs = flowsOrEmpty.stream()
                .map(AuthenticationFlowRepresentation::getAuthenticationExecutions)
                .map(l -> l.stream()
                        .map(AbstractAuthenticationExecutionRepresentation::getAuthenticatorConfig)
                        .collect(Collectors.toList())).flatMap(Collection::stream)
                .collect(Collectors.toSet());

        var configOrEmpty = getNonNull(configs);
        // Only return configs that are used
        var filteredConfigs = configOrEmpty.stream()
                .filter(acr -> usedConfigs.contains(acr.getAlias())).collect(Collectors.toList());

        for (var config : filteredConfigs) {
            config.setId(null);
        }
        return filteredConfigs.isEmpty() ? null : filteredConfigs;
    }

    private List<AuthenticationFlowRepresentation> filterBuiltIn(List<AuthenticationFlowRepresentation> flows) {
        if (flows == null) {
            return new ArrayList<>();
        }
        return flows.stream().filter(not(AuthenticationFlowRepresentation::isBuiltIn)).collect(Collectors.toList());
    }

    private List<AuthenticationFlowRepresentation> filterUnusedNonTopLevel(List<AuthenticationFlowRepresentation> flows) {
        if (flows == null) {
            return new ArrayList<>();
        }

        // Assume all top level flows are used
        var usedFlows = flows.stream().filter(AuthenticationFlowRepresentation::isTopLevel).collect(Collectors.toList());
        var toCheck = flows.stream().filter(Predicate.not(AuthenticationFlowRepresentation::isTopLevel))
                .collect(Collectors.toMap(AuthenticationFlowRepresentation::getAlias, Function.identity()));
        var removedEntry = false;
        do {
            removedEntry = false;
            var toRemove = new ArrayList<String>();
            for (var flow : usedFlows) {
                for (var execution : flow.getAuthenticationExecutions()) {
                    var alias = execution.getFlowAlias();
                    if (alias != null) {
                        if (toCheck.containsKey(alias)) {
                            toRemove.add(alias);
                            removedEntry = true;
                        }
                    }
                }
            }
            for (var alias : toRemove) {
                usedFlows.add(toCheck.remove(alias));
            }
        } while (removedEntry);
        return usedFlows;
    }

    public boolean executionsChanged(List<AuthenticationExecutionExportRepresentation> exportedExecutions,
                                     List<AuthenticationExecutionExportRepresentation> baselineExecutions) {
        if (exportedExecutions == null && baselineExecutions != null) {
            return true;
        }

        if (exportedExecutions != null && baselineExecutions == null) {
            return true;
        }

        if (exportedExecutions == null) {
            return false;
        }

        if (exportedExecutions.size() != baselineExecutions.size()) {
            return true;
        }

        exportedExecutions.sort(Comparator.comparing(AbstractAuthenticationExecutionRepresentation::getPriority));
        baselineExecutions.sort(Comparator.comparing(AbstractAuthenticationExecutionRepresentation::getPriority));

        for (int i = 0; i < exportedExecutions.size(); i++) {
            if (executionChanged(exportedExecutions.get(i), baselineExecutions.get(i))) {
                return true;
            }
        }
        return false;
    }

    public boolean executionChanged(AuthenticationExecutionExportRepresentation exportedExecution,
                                    AuthenticationExecutionExportRepresentation baselineExecution) {
        if (!Objects.equals(exportedExecution.getAuthenticatorConfig(), baselineExecution.getAuthenticatorConfig())) {
            return true;
        }
        if (!Objects.equals(exportedExecution.getAuthenticator(), baselineExecution.getAuthenticator())) {
            return true;
        }
        if (!Objects.equals(exportedExecution.isAuthenticatorFlow(), baselineExecution.isAuthenticatorFlow())) {
            return true;
        }
        if (!Objects.equals(exportedExecution.getRequirement(), baselineExecution.getRequirement())) {
            return true;
        }
        if (!Objects.equals(exportedExecution.getPriority(), baselineExecution.getPriority())) {
            return true;
        }
        if (!Objects.equals(exportedExecution.getFlowAlias(), baselineExecution.getFlowAlias())) {
            return true;
        }
        return !Objects.equals(exportedExecution.isUserSetupAllowed(), baselineExecution.isUserSetupAllowed());
    }
}
