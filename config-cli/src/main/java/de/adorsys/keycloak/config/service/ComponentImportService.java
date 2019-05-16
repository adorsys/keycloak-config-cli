package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.ComponentRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
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

    @Autowired
    public ComponentImportService(ComponentRepository componentRepository) {
        this.componentRepository = componentRepository;
    }

    public void doImport(RealmImport realmImport) {
        createOrUpdateComponents(realmImport.getRealm(), realmImport.getComponents());
    }

    private void createOrUpdateComponents(String realm, Map<String, List<ComponentExportRepresentation>> componentsToImport) {
        for (Map.Entry<String, List<ComponentExportRepresentation>> entry : componentsToImport.entrySet()) {
            createOrUpdateComponents(realm, entry.getKey(), entry.getValue());
        }
    }

    private void createOrUpdateComponents(String realm, String providerType, List<ComponentExportRepresentation> componentsToImport) {
        for (ComponentExportRepresentation componentToImport : componentsToImport) {

            Optional<ComponentRepresentation> maybeComponent = componentRepository.tryToGetComponent(realm, componentToImport.getName(), componentToImport.getSubType());
            MultivaluedHashMap<String, ComponentExportRepresentation> subComponentsToImport = componentToImport.getSubComponents();

            if (maybeComponent.isPresent()) {
                updateComponentIfNeeded(realm, componentToImport, maybeComponent.get(), subComponentsToImport);
            } else {
                logger.debug("Creating component: {}/{}", providerType, componentToImport.getName());
                createComponent(realm, providerType, componentToImport, subComponentsToImport);
            }
        }
    }

    private void createComponent(
            String realm,
            String providerType,
            ComponentExportRepresentation component,
            MultivaluedHashMap<String, ComponentExportRepresentation> subComponentChildren
    ) {
        ComponentRepresentation subComponentToAdd = CloneUtils.deepClone(component, ComponentRepresentation.class);

        if(subComponentToAdd.getProviderType() == null) {
            subComponentToAdd.setProviderType(providerType);
        }

        componentRepository.create(realm, subComponentToAdd);

        if (subComponentChildren != null && !subComponentChildren.isEmpty()) {
            ComponentRepresentation exitingComponent = componentRepository.get(realm, component.getSubType(), component.getName());
            createOrUpdateSubComponents(realm, subComponentChildren, exitingComponent.getId());
        }
    }

    private void updateComponentIfNeeded(
            String realm,
            ComponentExportRepresentation componentToImport,
            ComponentRepresentation existingComponent,
            MultivaluedHashMap<String, ComponentExportRepresentation> subComponentChildren
    ) {
        ComponentRepresentation patchedComponent = CloneUtils.patch(existingComponent, componentToImport, "id");

        boolean hasToBeUpdated = !CloneUtils.deepEquals(existingComponent, patchedComponent);

        if(hasToBeUpdated) {
            updateComponent(realm, componentToImport, patchedComponent, subComponentChildren);
        } else {
            logger.debug("No need to update component: {}/{}", existingComponent.getProviderType(), componentToImport.getName());
        }
    }

    private void updateComponent(String realm, ComponentExportRepresentation componentToImport,  ComponentRepresentation patchedComponent, MultivaluedHashMap<String, ComponentExportRepresentation> subComponentChildren) {
        logger.debug("Updating component: {}/{}", patchedComponent.getProviderType(), componentToImport.getName());

        componentRepository.update(realm, patchedComponent);

        if (subComponentChildren != null && !subComponentChildren.isEmpty()) {
            createOrUpdateSubComponents(realm, subComponentChildren, patchedComponent.getId());
        }
    }

    private void createOrUpdateSubComponents(String realm, Map<String, List<ComponentExportRepresentation>> componentsToImport, String parentId) {
        for (Map.Entry<String, List<ComponentExportRepresentation>> entry : componentsToImport.entrySet()) {
            createOrUpdateSubComponents(realm, entry.getValue(), parentId);
        }
    }

    private void createOrUpdateSubComponents(String realm, List<ComponentExportRepresentation> components, String parentId) {
        for (ComponentExportRepresentation component : components) {

            Optional<ComponentRepresentation> maybeComponent = componentRepository.tryToGet(realm, parentId, component.getSubType(), component.getName());
            MultivaluedHashMap<String, ComponentExportRepresentation> subComponentChildren = component.getSubComponents();

            if (maybeComponent.isPresent()) {
                updateComponentIfNeeded(realm, component, maybeComponent.get(), subComponentChildren);
            } else {
                updateSubComponent(realm, parentId, component, subComponentChildren);
            }
        }
    }

    private void updateSubComponent(String realm, String parentId, ComponentExportRepresentation component, MultivaluedHashMap<String, ComponentExportRepresentation> subComponentChildren) {
        ComponentRepresentation subComponentToAdd = CloneUtils.deepClone(component, ComponentRepresentation.class);

        componentRepository.create(realm, subComponentToAdd);

        if (subComponentChildren != null) {
            ComponentRepresentation exitingComponent = componentRepository.getSubComponent(realm, parentId, component.getSubType(), component.getName());
            createOrUpdateSubComponents(realm, subComponentChildren, exitingComponent.getId());
        }
    }
}
