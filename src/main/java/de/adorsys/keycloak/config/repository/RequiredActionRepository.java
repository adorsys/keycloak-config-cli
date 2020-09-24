/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2020 adorsys GmbH & Co. KG @ https://adorsys.com
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

package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides methods to retrieve and store required-actions in your realm
 */
@Service
public class RequiredActionRepository {

    private final AuthenticationFlowRepository authenticationFlowRepository;

    @Autowired
    public RequiredActionRepository(AuthenticationFlowRepository authenticationFlowRepository) {
        this.authenticationFlowRepository = authenticationFlowRepository;
    }

    public RequiredActionProviderRepresentation get(String realmName, String requiredActionAlias) {
        Optional<RequiredActionProviderRepresentation> maybeRequiredAction = search(realmName, requiredActionAlias);

        if (maybeRequiredAction.isPresent()) {
            return maybeRequiredAction.get();
        }

        throw new KeycloakRepositoryException("Cannot get required action: " + requiredActionAlias);
    }

    public List<RequiredActionProviderRepresentation> getAll(String realmName) {
        AuthenticationManagementResource flows = authenticationFlowRepository.getFlowResources(realmName);

        return flows.getRequiredActions();
    }

    public Optional<RequiredActionProviderRepresentation> search(String realmName, String requiredActionAlias) {
        List<RequiredActionProviderRepresentation> requiredActions = getAll(realmName);
        return requiredActions.stream()
                .filter(r -> Objects.equals(r.getAlias(), requiredActionAlias))
                .map(this::setProviderId)
                .findFirst();
    }

    private RequiredActionProviderRepresentation setProviderId(RequiredActionProviderRepresentation r) {
        // keycloak is NOT mapping the field 'providerId' into required-action representations, so we have to enrich
        // the required-action; the provider-id has always the same value like alias
        r.setProviderId(r.getAlias());
        return r;
    }

    public void create(String realmName, RequiredActionProviderSimpleRepresentation requiredAction) {
        AuthenticationManagementResource flows = authenticationFlowRepository.getFlowResources(realmName);
        flows.registerRequiredAction(requiredAction);
    }

    public void update(String realmName, RequiredActionProviderRepresentation requiredAction) {
        AuthenticationManagementResource flows = authenticationFlowRepository.getFlowResources(realmName);
        flows.updateRequiredAction(requiredAction.getAlias(), requiredAction);
    }

    public void delete(String realmName, RequiredActionProviderRepresentation requiredAction) {
        AuthenticationManagementResource flows = authenticationFlowRepository.getFlowResources(realmName);
        flows.removeRequiredAction(requiredAction.getAlias());
    }
}
