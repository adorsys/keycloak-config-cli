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
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues;
import de.adorsys.keycloak.config.repository.RequiredActionRepository;
import de.adorsys.keycloak.config.util.CloneUtil;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Creates and updates required-actions in your realm
 */
@Service
public class RequiredActionsImportService {
    private static final Logger logger = LoggerFactory.getLogger(RequiredActionsImportService.class);

    private final RequiredActionRepository requiredActionRepository;
    private final ImportConfigProperties importConfigProperties;

    public RequiredActionsImportService(
            RequiredActionRepository requiredActionRepository,
            ImportConfigProperties importConfigProperties) {
        this.requiredActionRepository = requiredActionRepository;
        this.importConfigProperties = importConfigProperties;
    }

    public void doImport(RealmImport realmImport) {
        List<RequiredActionProviderRepresentation> requiredActions = realmImport.getRequiredActions();
        if (requiredActions == null) return;

        String realm = realmImport.getRealm();
        doImport(realm, requiredActions);
    }

    public void doImport(String realm, List<RequiredActionProviderRepresentation> requiredActions) {
        List<RequiredActionProviderRepresentation> existingRequiredActions = requiredActionRepository.getRequiredActions(realm);

        if (requiredActions.isEmpty()) {
            if (importConfigProperties.getManaged().getClientScope() == ImportManagedPropertiesValues.noDelete) {
                logger.info("Skip deletion of requiredActions");
                return;
            }

            deleteAllExistingRequiredActions(realm, existingRequiredActions);
        } else {
            if (importConfigProperties.getManaged().getClientScope() == ImportManagedPropertiesValues.full) {
                deleteRequiredActionsMissingInImport(realm, requiredActions, existingRequiredActions);
            }

            for (RequiredActionProviderRepresentation requiredActionToImport : requiredActions) {
                throwErrorIfInvalid(requiredActionToImport);
                createOrUpdateRequireAction(realm, requiredActionToImport);
            }
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

    private void createOrUpdateRequireAction(String realm, RequiredActionProviderRepresentation requiredActionToImport) {
        String requiredActionAlias = requiredActionToImport.getAlias();
        Optional<RequiredActionProviderRepresentation> maybeRequiredAction = requiredActionRepository.tryToGetRequiredAction(realm, requiredActionAlias);

        if (maybeRequiredAction.isPresent()) {
            RequiredActionProviderRepresentation existingRequiredAction = maybeRequiredAction.get();

            updateRequiredActionIfNeeded(realm, requiredActionToImport, requiredActionAlias, existingRequiredAction);
        } else {
            logger.debug("Creating required action: {}", requiredActionAlias);
            createAndConfigureRequiredAction(realm, requiredActionToImport, requiredActionAlias);
        }
    }

    private void updateRequiredActionIfNeeded(String realm, RequiredActionProviderRepresentation requiredActionToImport, String requiredActionAlias, RequiredActionProviderRepresentation existingRequiredAction) {
        if (hasToBeUpdated(requiredActionToImport, existingRequiredAction)) {
            logger.debug("Updating required action: {}", requiredActionAlias);
            updateRequiredAction(realm, requiredActionToImport, existingRequiredAction);
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

    private void createAndConfigureRequiredAction(String realm, RequiredActionProviderRepresentation requiredActionToImport, String requiredActionAlias) {
        RequiredActionProviderSimpleRepresentation requiredActionToCreate = CloneUtil.deepClone(requiredActionToImport, RequiredActionProviderSimpleRepresentation.class);
        requiredActionRepository.createRequiredAction(realm, requiredActionToCreate);

        RequiredActionProviderRepresentation createdRequiredAction = requiredActionRepository.getRequiredAction(realm, requiredActionAlias);

        /*
         we need to update the required-action after creation because the creation only accepts following properties to be set:
         - providerId
         - name
        */
        updateRequiredAction(realm, requiredActionToImport, createdRequiredAction);
    }

    private void updateRequiredAction(
            String realm,
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

        requiredActionRepository.updateRequiredAction(realm, requiredActionToBeConfigured);
    }

    private void deleteAllExistingRequiredActions(String realm, List<RequiredActionProviderRepresentation> existingRequiredActions) {
        for (RequiredActionProviderRepresentation existingRequiredAction : existingRequiredActions) {
            logger.debug("Delete requiredAction '{}' in realm '{}'", existingRequiredAction.getName(), realm);
            requiredActionRepository.deleteRequiredAction(realm, existingRequiredAction);
        }
    }

    private void deleteRequiredActionsMissingInImport(String realm, List<RequiredActionProviderRepresentation> requiredActions, List<RequiredActionProviderRepresentation> existingRequiredActions) {
        for (RequiredActionProviderRepresentation existingRequiredAction : existingRequiredActions) {
            if (!hasRequiredActionWithAlias(existingRequiredAction.getAlias(), requiredActions)) {
                logger.debug("Delete requiredAction '{}' in realm '{}'", existingRequiredAction.getName(), realm);
                requiredActionRepository.deleteRequiredAction(realm, existingRequiredAction);
            }
        }
    }

    private boolean hasRequiredActionWithAlias(String requiredActionAlias, List<RequiredActionProviderRepresentation> existingRequiredAction) {
        return existingRequiredAction.stream().anyMatch(s -> requiredActionAlias.equals(s.getAlias()));
    }
}
