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

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues;
import de.adorsys.keycloak.config.repository.ComponentRepository;
import de.adorsys.keycloak.config.util.CloneUtil;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ComponentImportService {
    private static final Logger logger = LoggerFactory.getLogger(ComponentImportService.class);

    private final ComponentRepository componentRepository;
    private final ImportConfigProperties importConfigProperties;

    @Autowired
    public ComponentImportService(ComponentRepository componentRepository, ImportConfigProperties importConfigProperties) {
        this.componentRepository = componentRepository;
        this.importConfigProperties = importConfigProperties;
    }

    public void doImport(RealmImport realmImport) {
        MultivaluedHashMap<String, ComponentExportRepresentation> components = realmImport.getComponents();

        if (components == null) {
            return;
        }

        importComponents(realmImport.getRealm(), components);

        if (importConfigProperties.getManaged().getComponent() == ImportManagedPropertiesValues.FULL) {
            deleteComponentsMissingInImport(realmImport.getRealm(), components);
        }
    }

    private void importComponents(String realm, Map<String, List<ComponentExportRepresentation>> componentsToImport) {
        for (Map.Entry<String, List<ComponentExportRepresentation>> entry : componentsToImport.entrySet()) {
            createOrUpdateComponents(realm, entry.getKey(), entry.getValue());
        }
    }

    private void createOrUpdateComponents(String realm, String providerType, List<ComponentExportRepresentation> componentsToImport) {
        for (ComponentExportRepresentation componentToImport : componentsToImport) {
            createOrUpdateComponent(realm, providerType, componentToImport);
        }
    }

    private void createOrUpdateComponent(String realm, String providerType, ComponentExportRepresentation componentToImport) {
        Optional<ComponentRepresentation> maybeComponent = componentRepository.tryToGetComponent(realm, componentToImport.getName(), componentToImport.getSubType());

        if (maybeComponent.isPresent()) {
            updateComponentIfNeeded(realm, providerType, componentToImport, maybeComponent.get());
        } else {
            logger.debug("Creating component: {}/{}", providerType, componentToImport.getName());
            createComponent(realm, providerType, componentToImport);
        }
    }

    private void createComponent(
            String realm,
            String providerType,
            ComponentExportRepresentation component
    ) {
        createComponent(realm, providerType, component, null);
    }

    private void createComponent(
            String realm,
            String providerType,
            ComponentExportRepresentation component,
            String parentId
    ) {
        ComponentRepresentation subComponentToAdd = CloneUtil.deepClone(component, ComponentRepresentation.class);

        if (subComponentToAdd.getProviderType() == null) {
            subComponentToAdd.setProviderType(providerType);
        }

        if (subComponentToAdd.getParentId() == null) {
            subComponentToAdd.setParentId(parentId);
        }

        try {
            componentRepository.create(realm, subComponentToAdd);
        } catch (KeycloakRepositoryException e) {
            throw new ImportProcessingException("Cannot create component '" + subComponentToAdd.getName() + "' in realm '" + realm + "'", e);
        }

        MultivaluedHashMap<String, ComponentExportRepresentation> subComponents = component.getSubComponents();

        if (subComponents == null) {
            return;
        }

        ComponentRepresentation exitingComponent = componentRepository.get(realm, providerType, component.getName());

        if (!subComponents.isEmpty()) {
            createOrUpdateSubComponents(realm, subComponents, exitingComponent.getId());
        }

        if (importConfigProperties.getManaged().getComponent() == ImportManagedPropertiesValues.FULL) {
            deleteSubComponentsMissingInImport(realm, subComponents, exitingComponent.getId());
        }
    }

    private void updateComponentIfNeeded(
            String realm,
            String providerType,
            ComponentExportRepresentation componentToImport,
            ComponentRepresentation existingComponent
    ) {
        ComponentRepresentation patchedComponent = CloneUtil.patch(existingComponent, componentToImport, "id");

        boolean hasToBeUpdated = !CloneUtil.deepEquals(existingComponent, patchedComponent);

        if (hasToBeUpdated) {
            updateComponent(realm, providerType, componentToImport, patchedComponent);
        } else {
            logger.debug("No need to update component: {}/{}", existingComponent.getProviderType(), componentToImport.getName());
        }
    }

    private void updateComponent(
            String realm,
            String providerType,
            ComponentExportRepresentation componentToImport,
            ComponentRepresentation patchedComponent
    ) {
        logger.debug("Updating component: {}/{}", patchedComponent.getProviderType(), componentToImport.getName());

        if (patchedComponent.getProviderType() == null) {
            patchedComponent.setProviderType(providerType);
        }

        componentRepository.update(realm, patchedComponent);

        MultivaluedHashMap<String, ComponentExportRepresentation> subComponents = componentToImport.getSubComponents();

        if (subComponents != null) {
            if (!subComponents.isEmpty()) {
                createOrUpdateSubComponents(realm, subComponents, patchedComponent.getId());
            }

            if (importConfigProperties.getManaged().getSubComponent() == ImportManagedPropertiesValues.FULL) {
                deleteSubComponentsMissingInImport(realm, subComponents, patchedComponent.getId());
            }
        }
    }

    private void createOrUpdateSubComponents(String realm, Map<String, List<ComponentExportRepresentation>> subComponents, String parentId) {
        for (Map.Entry<String, List<ComponentExportRepresentation>> entry : subComponents.entrySet()) {
            createOrUpdateSubComponents(realm, entry.getKey(), entry.getValue(), parentId);
        }
    }

    private void createOrUpdateSubComponents(String realm, String providerType, List<ComponentExportRepresentation> subComponents, String parentId) {
        for (ComponentExportRepresentation subComponent : subComponents) {
            createOrUpdateSubComponent(realm, parentId, providerType, subComponent);
        }
    }

    private void createOrUpdateSubComponent(String realm, String parentId, String providerType, ComponentExportRepresentation subComponent) {
        Optional<ComponentRepresentation> maybeComponent = componentRepository.tryToGetSubComponent(realm, parentId, subComponent.getSubType(), subComponent.getName());

        if (maybeComponent.isPresent()) {
            updateComponentIfNeeded(realm, providerType, subComponent, maybeComponent.get());
        } else {
            createComponent(realm, providerType, subComponent, parentId);
        }
    }

    private void deleteComponentsMissingInImport(String realm, MultivaluedHashMap<String, ComponentExportRepresentation> componentsToImport) {
        List<ComponentRepresentation> existingComponents = componentRepository.getAllComponents(realm);

        for (ComponentRepresentation existingComponent : existingComponents) {
            if (checkIfComponentMissingImport(existingComponent, componentsToImport)) {
                logger.debug("Delete component: {}/{}", existingComponent.getProviderType(), existingComponent.getName());
                componentRepository.delete(realm, existingComponent);
            }
        }
    }

    private void deleteSubComponentsMissingInImport(String realm, MultivaluedHashMap<String, ComponentExportRepresentation> subComponentsToImport, String parentId) {
        List<ComponentRepresentation> existingSubComponents = componentRepository.getAllSubComponentsByParentId(realm, parentId);

        for (ComponentRepresentation existingSubComponent : existingSubComponents) {
            if (checkIfComponentMissingImport(existingSubComponent, subComponentsToImport)) {
                logger.debug("Delete component: {}/{}", existingSubComponent.getProviderType(), existingSubComponent.getName());
                componentRepository.delete(realm, existingSubComponent);
            }
        }
    }

    private boolean checkIfComponentMissingImport(ComponentRepresentation existingComponent, MultivaluedHashMap<String, ComponentExportRepresentation> componentsToImport) {
        String existingComponentProviderType = existingComponent.getProviderType();
        String existingComponentName = existingComponent.getName();

        for (Map.Entry<String, List<ComponentExportRepresentation>> entry : componentsToImport.entrySet()) {
            String providerType = entry.getKey();
            List<ComponentExportRepresentation> componentToImport = entry.getValue();

            if (!existingComponentProviderType.equals(providerType)) {
                continue;
            }

            boolean isInImport = componentToImport.stream().anyMatch((component) -> existingComponentName.equals(component.getName()));

            if (isInImport) {
                return false;
            }
        }

        return true;
    }
}
