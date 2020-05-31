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

package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

/**
 * Provides methods to retrieve and store required-actions in your realm
 */

@Dependent
public class RequiredActionRepository {

    @Inject
    AuthenticationFlowRepository authenticationFlowRepository;

    public List<RequiredActionProviderRepresentation> getRequiredActions(String realm) {
        AuthenticationManagementResource flows = authenticationFlowRepository.getFlows(realm);

        return flows.getRequiredActions();
    }

    public Optional<RequiredActionProviderRepresentation> tryToGetRequiredAction(String realm, String requiredActionAlias) {
        List<RequiredActionProviderRepresentation> requiredActions = getRequiredActions(realm);
        return requiredActions.stream()
                .filter(r -> r.getAlias().equals(requiredActionAlias))
                .map(this::enrichWithProviderId)
                .findFirst();
    }

    private RequiredActionProviderRepresentation enrichWithProviderId(RequiredActionProviderRepresentation r) {
        // keycloak is NOT mapping the field 'providerId' into required-action representations, so we have to enrich
        // the required-action; the provider-id has always the same value like alias
        r.setProviderId(r.getAlias());
        return r;
    }

    public RequiredActionProviderRepresentation getRequiredAction(String realm, String requiredActionAlias) {
        Optional<RequiredActionProviderRepresentation> maybeRequiredAction = tryToGetRequiredAction(realm, requiredActionAlias);

        if (maybeRequiredAction.isPresent()) {
            return maybeRequiredAction.get();
        }

        throw new KeycloakRepositoryException("Cannot get required action: " + requiredActionAlias);
    }

    public void createRequiredAction(String realm, RequiredActionProviderSimpleRepresentation requiredActionToCreate) {
        AuthenticationManagementResource flows = authenticationFlowRepository.getFlows(realm);
        flows.registerRequiredAction(requiredActionToCreate);
    }

    public void updateRequiredAction(String realm, RequiredActionProviderRepresentation requiredActionToCreate) {
        AuthenticationManagementResource flows = authenticationFlowRepository.getFlows(realm);
        flows.updateRequiredAction(requiredActionToCreate.getAlias(), requiredActionToCreate);
    }
}
