package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.util.ResponseUtil;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@Service
public class GroupRepository {

    private final RealmRepository realmRepository;

    @Autowired
    public GroupRepository(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    public Optional<GroupRepresentation> tryToFindGroupByPath(String realm, String groupPath) {
        try {
            return Optional.ofNullable(realmRepository.loadRealm(realm).getGroupByPath(groupPath));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    public void createOrUpdate(String realm, GroupRepresentation toCreate) throws KeycloakRepositoryException {
        GroupsResource groupsResource = realmRepository.loadRealm(realm).groups();

        Response response = groupsResource.add(toCreate);
        ResponseUtil.throwOnError(response);

        // TODO: handle delta
    }
}
