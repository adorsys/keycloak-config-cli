package de.adorsys.keycloak.config.service.clientauthorization;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.repository.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;

public class ClientPermissionResolver implements PermissionResolver {
    private static final Logger logger = LoggerFactory.getLogger(ClientPermissionResolver.class);

    private final String realmName;
    private final ClientRepository clientRepository;

    public ClientPermissionResolver(String realmName, ClientRepository clientRepository) {
        this.realmName = realmName;
        this.clientRepository = clientRepository;
    }

    @Override
    public String resolveObjectId(String clientId, String authzName) {
        try {
            return clientRepository.getByClientId(realmName, clientId).getId();
        } catch (NotFoundException | KeycloakRepositoryException e) {
            throw new ImportProcessingException("Cannot find client '%s' in realm '%s' for '%s'", clientId, realmName, authzName);
        }
    }

    @Override
    public void enablePermissions(String id) {
        if (!clientRepository.isPermissionEnabled(realmName, id)) {
            logger.debug("Enable permissions for client '{}' in realm '{}'", id, realmName);
            clientRepository.enablePermission(realmName, id);
        }
    }
}
