package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.RealmRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.admin.client.resource.ComponentResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RealmComponentImportService {

    private final RealmRepository realmRepository;

    @Autowired
    public RealmComponentImportService(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    public void doImport(RealmImport realmImport) {
        MultivaluedHashMap<String, ComponentExportRepresentation> componentsToImport = realmImport.getComponents();

        RealmResource realmResource = realmRepository.loadRealm(realmImport.getRealm());

        createOrUpdateComponents(realmImport.getComponents(), realmResource, realmImport.getRealm());

    }

    private void createOrUpdateComponents(Map<String, List<ComponentExportRepresentation>> componentsToImport, RealmResource realmResource, String parentId) {
        for (Map.Entry<String, List<ComponentExportRepresentation>> entry : componentsToImport.entrySet()) {
            createOrUpdateComponents(entry.getValue(), realmResource, parentId);
        }
    }

    private void createOrUpdateComponents(List<ComponentExportRepresentation> components, RealmResource realmResource, String parentId) {
        for (ComponentExportRepresentation component : components) {

            Optional<ComponentRepresentation> maybeComponent = tryToLoadComponent(realmResource, parentId, component.getSubType(), component.getName());
            MultivaluedHashMap<String, ComponentExportRepresentation> subComponentChildren = component.getSubComponents();

            if (maybeComponent.isPresent()) {
                ComponentRepresentation existingComponent = maybeComponent.get();

                ComponentRepresentation patchedComponent = CloneUtils.deepPatch(existingComponent, component);

                ComponentResource componentResource = realmResource.components().component(existingComponent.getId());
                componentResource.update(patchedComponent);

                if (subComponentChildren != null) {
                    createOrUpdateComponents(subComponentChildren, realmResource, patchedComponent.getId());
                }
            } else {
                ComponentRepresentation subComponentToAdd = CloneUtils.deepClone(component, ComponentRepresentation.class);
                Response response = realmResource.components().add(subComponentToAdd);

                if(response.getStatus() < 400) {
                    if (subComponentChildren != null) {
                        ComponentRepresentation exitingComponent = loadComponent(realmResource, parentId, component.getSubType(), component.getName());
                        createOrUpdateComponents(subComponentChildren, realmResource, exitingComponent.getId());
                    }
                } else {
                    throw new RuntimeException("Unable to create component " + component.getName() + ", Response status: " + response.getStatus());
                }

                response.close();
            }
        }
    }

    private Optional<ComponentRepresentation> tryToLoadComponent(RealmResource realmResource, String parentId, String subType, String name) {
        Optional<ComponentRepresentation> maybeComponent;
        List<ComponentRepresentation> existingComponents = realmResource.components().query(parentId, subType, name);

        if(existingComponents.isEmpty()) {
            maybeComponent = Optional.empty();
        } else {
            maybeComponent = Optional.of(existingComponents.get(0));
        }

        return maybeComponent;
    }

    private ComponentRepresentation loadComponent(RealmResource realmResource, String parentId, String subType, String name) {
        List<ComponentRepresentation> existingComponents = realmResource.components().query(parentId, subType, name);
        return existingComponents.get(0);
    }
}
