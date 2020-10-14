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

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.exception.InvalidImportException;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues;
import de.adorsys.keycloak.config.repository.RequiredActionRepository;
import de.adorsys.keycloak.config.service.state.StateService;
import de.adorsys.keycloak.config.util.CloneUtil;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Creates and updates required-actions in your realm
 */
@Service
public class RequiredActionsImportService {
    private static final Logger logger = LoggerFactory.getLogger(RequiredActionsImportService.class);

    private final RequiredActionRepository requiredActionRepository;
    private final ImportConfigProperties importConfigProperties;
    private final StateService stateService;

    public RequiredActionsImportService(
            RequiredActionRepository requiredActionRepository,
            ImportConfigProperties importConfigProperties, StateService stateService) {
        this.requiredActionRepository = requiredActionRepository;
        this.importConfigProperties = importConfigProperties;
        this.stateService = stateService;
    }

    public void doImport(RealmImport realmImport) {
        List<RequiredActionProviderRepresentation> requiredActions = realmImport.getRequiredActions();
        if (requiredActions == null) return;

        String realmName = realmImport.getRealm();

        List<RequiredActionProviderRepresentation> existingRequiredActions = requiredActionRepository.getAll(realmName);

        if (importConfigProperties.getManaged().getClientScope() == ImportManagedPropertiesValues.FULL) {
            deleteRequiredActionsMissingInImport(realmName, requiredActions, existingRequiredActions);
        }

        for (RequiredActionProviderRepresentation requiredActionToImport : requiredActions) {
            throwErrorIfInvalid(requiredActionToImport);
            createOrUpdateRequireAction(realmName, requiredActionToImport);
        }
    }

    /**
     * Cause of a weird keycloak endpoint behavior the alias and provider-id of an required-action should always be equal
     */
    private void throwErrorIfInvalid(RequiredActionProviderRepresentation requiredActionToImport) {
        if (!requiredActionToImport.getAlias().equals(requiredActionToImport.getProviderId())) {
            throw new InvalidImportException("Cannot import Required-Action '" + requiredActionToImport.getAlias() + "': alias and provider-id have to be equal");
        }
    }

    private void createOrUpdateRequireAction(String realmName, RequiredActionProviderRepresentation requiredActionToImport) {
        String requiredActionAlias = requiredActionToImport.getAlias();
        Optional<RequiredActionProviderRepresentation> maybeRequiredAction = requiredActionRepository.search(realmName, requiredActionAlias);

        if (maybeRequiredAction.isPresent()) {
            RequiredActionProviderRepresentation existingRequiredAction = maybeRequiredAction.get();

            updateRequiredActionIfNeeded(realmName, requiredActionToImport, requiredActionAlias, existingRequiredAction);
        } else {
            logger.debug("Creating required action: {}", requiredActionAlias);
            createAndConfigureRequiredAction(realmName, requiredActionToImport, requiredActionAlias);
        }
    }

    private void updateRequiredActionIfNeeded(String realmName, RequiredActionProviderRepresentation requiredActionToImport, String requiredActionAlias, RequiredActionProviderRepresentation existingRequiredAction) {
        if (hasToBeUpdated(requiredActionToImport, existingRequiredAction)) {
            logger.debug("Updating required action: {}", requiredActionAlias);
            updateRequiredAction(realmName, requiredActionToImport, existingRequiredAction);
        } else {
            logger.debug("No need to update required action: {}", requiredActionAlias);
        }
    }

    private boolean hasToBeUpdated(
            RequiredActionProviderRepresentation requiredActionToImport,
            RequiredActionProviderRepresentation existingRequiredAction
    ) {
        return !CloneUtil.deepEquals(requiredActionToImport, existingRequiredAction);
    }

    private void createAndConfigureRequiredAction(String realmName, RequiredActionProviderRepresentation requiredActionToImport, String requiredActionAlias) {
        RequiredActionProviderSimpleRepresentation requiredActionToCreate = CloneUtil.deepClone(requiredActionToImport, RequiredActionProviderSimpleRepresentation.class);
        requiredActionRepository.create(realmName, requiredActionToCreate);

        RequiredActionProviderRepresentation createdRequiredAction = requiredActionRepository.get(realmName, requiredActionAlias);

        /*
         we need to update the required-action after creation because the creation only accepts following properties to be set:
         - providerId
         - name
        */
        updateRequiredAction(realmName, requiredActionToImport, createdRequiredAction);
    }

    private void updateRequiredAction(
            String realmName,
            RequiredActionProviderRepresentation requiredActionToImport,
            RequiredActionProviderRepresentation existingRequiredAction
    ) {
        RequiredActionProviderRepresentation requiredActionToBeConfigured = CloneUtil.deepClone(existingRequiredAction);

        requiredActionToBeConfigured.setProviderId(requiredActionToImport.getProviderId());
        requiredActionToBeConfigured.setName(requiredActionToImport.getName());
        requiredActionToBeConfigured.setAlias(requiredActionToImport.getAlias());
        requiredActionToBeConfigured.setEnabled(requiredActionToImport.isEnabled());
        requiredActionToBeConfigured.setDefaultAction(requiredActionToImport.isDefaultAction());
        requiredActionToBeConfigured.setPriority(requiredActionToImport.getPriority());
        requiredActionToBeConfigured.setConfig(requiredActionToImport.getConfig());

        requiredActionRepository.update(realmName, requiredActionToBeConfigured);
    }

    private void deleteRequiredActionsMissingInImport(String realmName, List<RequiredActionProviderRepresentation> requiredActions, List<RequiredActionProviderRepresentation> existingRequiredActions) {
        if (importConfigProperties.isState()) {
            List<String> requiredActionsInState = stateService.getRequiredActions();

            // ignore all object there are not in state
            existingRequiredActions = existingRequiredActions.stream()
                    .filter(requiredAction -> requiredActionsInState.contains(requiredAction.getAlias()))
                    .collect(Collectors.toList());
        }

        for (RequiredActionProviderRepresentation existingRequiredAction : existingRequiredActions) {
            if (requiredActions.stream().noneMatch(s -> Objects.equals(existingRequiredAction.getAlias(), s.getAlias()))) {
                logger.debug("Delete requiredAction '{}' in realm '{}'", existingRequiredAction.getName(), realmName);
                requiredActionRepository.delete(realmName, existingRequiredAction);
            }
        }
    }
}
