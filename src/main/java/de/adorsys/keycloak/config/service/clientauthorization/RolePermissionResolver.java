package de.adorsys.keycloak.config.service.clientauthorization;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;

public class RolePermissionResolver implements PermissionResolver {
    private static final Logger logger = LoggerFactory.getLogger(RolePermissionResolver.class);

    private final String realmName;
    private final RoleRepository roleRepository;

    public RolePermissionResolver(String realmName, RoleRepository roleRepository) {
        this.realmName = realmName;
        this.roleRepository = roleRepository;
    }

    @Override
    public String resolveObjectId(String roleName, String authzName) {
        try {
            return roleRepository.getRealmRole(realmName, roleName).getId();
        } catch (NotFoundException | KeycloakRepositoryException e) {
            throw new ImportProcessingException("Cannot find realm role '%s' in realm '%s' for '%s'", roleName, realmName, authzName);
        }
    }

    @Override
    public void enablePermissions(String id) {
        if (!roleRepository.isPermissionEnabled(realmName, id)) {
            logger.debug("Enable permissions for client '{}' in realm '{}'", id, realmName);
            roleRepository.enablePermission(realmName, id);
        }
    }
}
