package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.util.ResponseUtil;
import org.apache.logging.log4j.util.Strings;
import org.keycloak.admin.client.resource.ComponentResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@Service
public class ComponentRepository {

    private final RealmRepository realmRepository;

    @Autowired
    public ComponentRepository(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    public void create(String realm, ComponentRepresentation componentToCreate) {
        RealmResource realmResource = realmRepository.loadRealm(realm);
        Response response = realmResource.components().add(componentToCreate);

        ResponseUtil.throwOnError(response);
    }

    public void update(String realm, ComponentRepresentation componentToUpdate) {
        assert(Strings.isNotBlank(componentToUpdate.getId()));

        RealmResource realmResource = realmRepository.loadRealm(realm);
        ComponentResource componentResource = realmResource.components().component(componentToUpdate.getId());

        componentResource.update(componentToUpdate);
    }

    public ComponentRepresentation get(String realm, String subType, String name) {
        RealmResource realmResource = realmRepository.loadRealm(realm);

        Optional<ComponentRepresentation> maybeComponent = realmResource.components()
                .query(realm, subType, name)
                .stream()
                .findFirst();

        if(maybeComponent.isPresent()) {
            return maybeComponent.get();
        }

        throw new RuntimeException("Cannot find component by name '" + name + "' and subtype '" + subType + "' in realm '" + realm + "' ");
    }

    public Optional<ComponentRepresentation> tryToGet(String realm, String parentId, String subType, String name) {
        RealmResource realmResource = realmRepository.loadRealm(realm);

        Optional<ComponentRepresentation> maybeComponent;
        List<ComponentRepresentation> existingComponents = realmResource.components()
                .query(parentId, subType, name);

        if(existingComponents.isEmpty()) {
            maybeComponent = Optional.empty();
        } else {
            maybeComponent = Optional.of(existingComponents.get(0));
        }

        return maybeComponent;
    }

    public Optional<ComponentRepresentation> tryToGetComponent(String realm, String name) {
        RealmResource realmResource = realmRepository.loadRealm(realm);

        List<ComponentRepresentation> existingComponents = realmResource.components()
                .query();

        return existingComponents.stream()
                .filter(c -> c.getName().equals(name))
                .findFirst();
    }

    public ComponentRepresentation getSubComponent(String realm, String parentId, String subType, String name) {
        RealmResource realmResource = realmRepository.loadRealm(realm);

        Optional<ComponentRepresentation> maybeComponent = realmResource.components()
                .query(parentId, subType, name)
                .stream()
                .findFirst();

        if(maybeComponent.isPresent()) {
            return maybeComponent.get();
        }

        throw new RuntimeException("Cannot find sub-component by name '" + name
                + "', subtype '" + subType + "' and parent-id '" + parentId
                + "' in realm '" + realm + "' ");

    }
}
