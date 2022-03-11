package de.adorsys.keycloak.config.service.clientauthorization;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.repository.GroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;

public class GroupPermissionResolver implements PermissionResolver {
    private static final Logger logger = LoggerFactory.getLogger(GroupPermissionResolver.class);

    private final String realmName;
    private final GroupRepository groupRepository;

    public GroupPermissionResolver(String realmName, GroupRepository groupRepository) {
        this.realmName = realmName;
        this.groupRepository = groupRepository;
    }

    @Override
    public String resolveObjectId(String groupPath, String authzName) {
        try {
            return groupRepository.getGroupByPath(realmName, groupPath).getId();
        } catch (NotFoundException | KeycloakRepositoryException e) {
            throw new ImportProcessingException("Cannot find group with path '%s' in realm '%s' for '%s'", groupPath, realmName, authzName);
        }
    }

    @Override
    public void enablePermissions(String id) {
        if (!groupRepository.isPermissionEnabled(realmName, id)) {
            logger.debug("Enable permissions for client '{}' in realm '{}'", id, realmName);
            groupRepository.enablePermission(realmName, id);
        }
    }
}
