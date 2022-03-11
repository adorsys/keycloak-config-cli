package de.adorsys.keycloak.config.service.clientauthorization;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.repository.IdentityProviderRepository;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.NotFoundException;

public class IdpPermissionResolver implements PermissionResolver {
    private static final Logger logger = LoggerFactory.getLogger(IdpPermissionResolver.class);

    private final String realmName;
    private final IdentityProviderRepository identityProviderRepository;
    private final Map<String, String> idpInternalIdToAlias = new HashMap<>();
    private final Map<String, String> idpAliasToInternalId = new HashMap<>();

    public IdpPermissionResolver(String realmName, IdentityProviderRepository identityProviderRepository) {
        this.realmName = realmName;
        this.identityProviderRepository = identityProviderRepository;
    }

    @Override
    public String resolveObjectId(String alias, String authzName) {
        if (idpAliasToInternalId.containsKey(alias)) {
            return idpAliasToInternalId.get(alias);
        }

        try {
            IdentityProviderRepresentation provider = identityProviderRepository.getByAlias(realmName, alias);
            if (provider == null) {
                throw new NotFoundException();
            }
            idpInternalIdToAlias.put(provider.getInternalId(), provider.getAlias());
            idpAliasToInternalId.put(provider.getAlias(), provider.getInternalId());
            return provider.getInternalId();
        } catch (NotFoundException | KeycloakRepositoryException e) {
            throw new ImportProcessingException("Cannot find identity provider with alias '%s' in realm '%s' for '%s'", alias, realmName, authzName);
        }
    }

    @Override
    public void enablePermissions(String id) {
        String alias = getAliasFromIdentityProviderInternalId(id);
        if (!identityProviderRepository.isPermissionEnabled(realmName, alias)) {
            logger.debug("Enable permissions for Identity Provider '{}' in realm '{}'", id, realmName);
            identityProviderRepository.enablePermission(realmName, alias);
        }
    }

    private String getAliasFromIdentityProviderInternalId(String internalId) {
        if (idpInternalIdToAlias.containsKey(internalId)) {
            return idpInternalIdToAlias.get(internalId);
        }

        try {
            List<IdentityProviderRepresentation> providers = identityProviderRepository.getAll(realmName);
            for (IdentityProviderRepresentation provider : providers) {
                idpInternalIdToAlias.put(provider.getInternalId(), provider.getAlias());
                idpAliasToInternalId.put(provider.getAlias(), provider.getInternalId());
            }
            if (idpInternalIdToAlias.containsKey(internalId)) {
                return idpInternalIdToAlias.get(internalId);
            }
            throw new NotFoundException();
        } catch (NotFoundException | KeycloakRepositoryException e) {
            throw new ImportProcessingException("Cannot find identity provider with internal id '%s' in realm '%s'", internalId, realmName);
        }
    }
}
