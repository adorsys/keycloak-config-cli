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

package de.adorsys.keycloak.config.util;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.model.RealmImport;
import org.keycloak.representations.idm.AbstractAuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AuthenticationFlowUtil {
    public static AuthenticationFlowRepresentation getNonTopLevelFlow(RealmImport realmImport, String alias) {
        Optional<AuthenticationFlowRepresentation> maybeNonTopLevelFlow = tryToGetNonTopLevelFlow(realmImport, alias);

        if (!maybeNonTopLevelFlow.isPresent()) {
            throw new ImportProcessingException("Non-toplevel flow not found: " + alias);
        }

        return maybeNonTopLevelFlow.get();
    }

    private static Optional<AuthenticationFlowRepresentation> tryToGetNonTopLevelFlow(RealmImport realmImport, String alias) {
        return getNonTopLevelFlows(realmImport)
                .stream()
                .filter(f -> f.getAlias().equals(alias))
                .findFirst();
    }

    private static List<AuthenticationFlowRepresentation> getNonTopLevelFlows(RealmImport realmImport) {
        return realmImport.getAuthenticationFlows()
                .stream()
                .filter(f -> !f.isTopLevel())
                .collect(Collectors.toList());
    }

    public static List<AuthenticationFlowRepresentation> getTopLevelFlows(RealmImport realmImport) {
        return realmImport.getAuthenticationFlows()
                .stream()
                .filter(AuthenticationFlowRepresentation::isTopLevel)
                .collect(Collectors.toList());
    }


    public static List<AuthenticationFlowRepresentation> getNonTopLevelFlowsForTopLevelFlow(RealmImport realmImport, AuthenticationFlowRepresentation topLevelFlow) {
        return topLevelFlow.getAuthenticationExecutions()
                .stream()
                .filter(AbstractAuthenticationExecutionRepresentation::isAutheticatorFlow)
                .map(AuthenticationExecutionExportRepresentation::getFlowAlias)
                .map((alias) -> getNonTopLevelFlow(realmImport, alias))
                .collect(Collectors.toList());
    }
}
