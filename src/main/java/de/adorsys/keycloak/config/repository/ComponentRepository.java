package de.adorsys.keycloak.config.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.util.ResponseUtil;
import org.apache.logging.log4j.util.Strings;
import org.keycloak.admin.client.resource.ComponentResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.*;

@Service
public class ComponentRepository {

    private final RealmRepository realmRepository;

    private final ObjectMapper mapper;

    @Autowired
    public ComponentRepository(RealmRepository realmRepository, ObjectMapper mapper) {
        this.realmRepository = realmRepository;
        this.mapper = mapper;
    }

    public void create(String realm, ComponentRepresentation componentToCreate) throws KeycloakRepositoryException {
        RealmResource realmResource = realmRepository.loadRealm(realm);
        Response response = realmResource.components().add(componentToCreate);

        ResponseUtil.throwOnError(response);
    }

    public void update(String realm, ComponentRepresentation componentToUpdate) {
        assert (Strings.isNotBlank(componentToUpdate.getId()));

        RealmResource realmResource = realmRepository.loadRealm(realm);
        ComponentResource componentResource = realmResource.components().component(componentToUpdate.getId());

        componentResource.update(componentToUpdate);
    }

    public ComponentRepresentation get(String realm, String subType, String name) {
        RealmResource realmResource = realmRepository.loadRealm(realm);

        List<ComponentRepresentation> realmComponents = realmResource.components().query();

        Optional<ComponentRepresentation> maybeComponent = realmComponents
                .stream()
                .filter(c -> Objects.equals(c.getName(), name))
                .filter(c -> Objects.equals(c.getProviderType(), subType))
                .findFirst();

        if (maybeComponent.isPresent()) {
            return maybeComponent.get();
        }

        throw new KeycloakRepositoryException("Cannot find component by name '" + name + "' and subtype '" + subType + "' in realm '" + realm + "' ");
    }

    public Optional<ComponentRepresentation> tryToGet(String realm, String parentId, String subType, String name) {
        RealmResource realmResource = realmRepository.loadRealm(realm);

        Optional<ComponentRepresentation> maybeComponent;
        List<ComponentRepresentation> existingComponents = realmResource.components()
                .query(parentId, subType, name);

        if (existingComponents.isEmpty()) {
            maybeComponent = Optional.empty();
        } else {
            maybeComponent = Optional.of(existingComponents.get(0));
        }

        return maybeComponent;
    }

    /**
     * Try to get a component by its properties.
     *
     * @param subType may be null
     */
    public Optional<ComponentRepresentation> tryToGetComponent(String realm, String name, String subType) {
        RealmResource realmResource = realmRepository.loadRealm(realm);

        List<ComponentRepresentation> existingComponents = realmResource.components()
                .query();

        return existingComponents.stream()
                .filter(c -> c.getName().equals(name))
                .filter(c -> Objects.equals(c.getName(), name))
                .filter(c -> Objects.equals(c.getSubType(), subType))
                .findFirst();
    }

    public ComponentExportRepresentation getSubComponentByName(String realm, String parentId, String name) {
        ComponentExportRepresentation parentComponent = getComponentById(realm, parentId);

        MultivaluedHashMap<String, ComponentExportRepresentation> subComponents = parentComponent.getSubComponents();
        List<ComponentExportRepresentation> subComponentsAsList = toFlatList(subComponents);

        Optional<ComponentExportRepresentation> maybeSubComponent = subComponentsAsList.stream()
                .filter(c -> Objects.equals(c.getName(), name))
                .findFirst();

        if (maybeSubComponent.isPresent()) {
            return maybeSubComponent.get();
        }

        throw new RuntimeException("Cannot find sub-component by name '" + name
                + "', and parent-id '" + parentId
                + "' in realm '" + realm + "' ");
    }

    public ComponentExportRepresentation getComponentById(String realm, String id) {
        Optional<ComponentExportRepresentation> maybeComponent = tryToGetExportedComponentById(realm, id);

        if (maybeComponent.isPresent()) {
            return maybeComponent.get();
        }

        throw new KeycloakRepositoryException("Cannot find component by id '" + id + "'");
    }

    private Optional<ComponentExportRepresentation> tryToGetExportedComponentById(String realm, String id) {
        return getFlatComponentsAndSubComponents(realm)
                .stream()
                .filter(c -> Objects.equals(c.getId(), id))
                .findFirst();
    }

    private List<ComponentExportRepresentation> getFlatComponentsAndSubComponents(String realm) {
        RealmRepresentation exportedRealm = realmRepository.partialExport(realm);

        MultivaluedHashMap<String, ComponentExportRepresentation> components = exportedRealm.getComponents();

        return toFlatList(components);
    }

    private List<ComponentExportRepresentation> getAllComponentsAndSubComponents(ComponentExportRepresentation component) {
        List<ComponentExportRepresentation> allComponents = new ArrayList<>();
        allComponents.add(component);

        MultivaluedHashMap<String, ComponentExportRepresentation> subComponents = component.getSubComponents();
        allComponents.addAll(toFlatList(subComponents));

        return allComponents;
    }

    private List<ComponentExportRepresentation> toFlatList(MultivaluedHashMap<String, ComponentExportRepresentation> componentsMap) {
        List<ComponentExportRepresentation> allComponents = new ArrayList<>();

        Set<Map.Entry<String, List<ComponentExportRepresentation>>> componentsLists = componentsMap.entrySet();

        for (Map.Entry<String, List<ComponentExportRepresentation>> componentsList : componentsLists) {
            List<ComponentExportRepresentation> components = componentsList.getValue();

            for (ComponentExportRepresentation component : components) {
                allComponents.addAll(getAllComponentsAndSubComponents(component));
            }
        }

        return allComponents;
    }
}
