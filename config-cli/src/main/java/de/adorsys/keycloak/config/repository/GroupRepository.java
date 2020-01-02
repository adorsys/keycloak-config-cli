package de.adorsys.keycloak.config.repository;

import com.google.common.collect.Sets;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.util.ResponseUtil;
import de.adorsys.keycloak.config.util.ToStringUtils;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class GroupRepository {
    private static final Logger logger = LoggerFactory.getLogger(GroupRepository.class);

    private final RealmRepository realmRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public GroupRepository(RealmRepository realmRepository, RoleRepository roleRepository) {
        this.realmRepository = realmRepository;
        this.roleRepository = roleRepository;
    }

    private Optional<GroupRepresentation> tryToFindGroupByPath(String realm, String groupPath) {
        try {
            return Optional.ofNullable(realmRepository.loadRealm(realm).getGroupByPath(groupPath));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    public GroupRepresentation findGroupByPath(String realm, String groupPath) {
        return tryToFindGroupByPath(realm, groupPath)
                .orElseThrow(
                        () -> new KeycloakRepositoryException(
                                "Cannot find group by path '" + groupPath + "' within realm '" + realm + "'"
                        )
                );
    }

    public void createOrUpdate(String realm, GroupRepresentation group) throws KeycloakRepositoryException {
        GroupsResource groupsResource = realmRepository.loadRealm(realm).groups();

        Optional<GroupRepresentation> existingGroupRepresentation = tryToFindGroupByPath(realm, group.getPath());

        Set<String> newlyAddedRealmRoles = new HashSet<>(group.getRealmRoles());
        Set<String> existingRealmRoles = new HashSet<>();
        String groupId = null;
        if (!existingGroupRepresentation.isPresent()) {
            logger.debug("create new group: {}", ToStringUtils.jsonToString(group));
            Response response = groupsResource.add(group);
            ResponseUtil.throwOnError(response);

            // find id of newly created group
            groupId = findGroupByPath(realm, group.getPath()).getId();
        } else {
            logger.debug("updating group: {}", ToStringUtils.jsonToString(group));
            // get existing group
            GroupRepresentation existingGroup = existingGroupRepresentation.get();
            groupId = existingGroup.getId();

            // update group
            groupsResource.group(groupId).update(group);

            // set existing roles
            existingRealmRoles = new HashSet<>(existingGroup.getRealmRoles());
        }

        RoleScopeResource roleScopeResource = groupsResource.group(groupId).roles().realmLevel();

        // find newly added roles that not in existing roles
        Set<String> toAdd = Sets.difference(newlyAddedRealmRoles, existingRealmRoles);
        // add to group
        if (!toAdd.isEmpty()) {
            roleScopeResource.add(roleRepository.findRealmRoles(realm, toAdd));
        }


        // find existing roles that not in newly added roles
        Set<String> toRemove = Sets.difference(existingRealmRoles, newlyAddedRealmRoles);
        // remove from group
        if (!toRemove.isEmpty()) {
            roleScopeResource.remove(roleRepository.findRealmRoles(realm, toRemove));
        }
    }
}
