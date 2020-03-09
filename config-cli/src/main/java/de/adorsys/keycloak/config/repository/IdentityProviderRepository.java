package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.IdentityProvidersResource;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class IdentityProviderRepository {

    private final RealmRepository realmRepository;

    @Autowired
    public IdentityProviderRepository(
            RealmRepository realmRepository
    ) {
        this.realmRepository = realmRepository;
    }

    public Optional<IdentityProviderRepresentation> tryToFindIdentityProvider(String realm, String name) {
        Optional<IdentityProviderRepresentation> maybeIdentityProvider;

        IdentityProvidersResource identityProvidersResource = realmRepository.loadRealm(realm).identityProviders();
        IdentityProviderResource identityProviderResource = identityProvidersResource.get(name);

        try {
            maybeIdentityProvider = Optional.of(identityProviderResource.toRepresentation());
        } catch (NotFoundException e) {
            maybeIdentityProvider = Optional.empty();
        }

        return maybeIdentityProvider;
    }

    public void createIdentityProvider(String realm, IdentityProviderRepresentation identityProvider) {
        IdentityProvidersResource identityProvidersResource = realmRepository.loadRealm(realm).identityProviders();
        identityProvidersResource.create(identityProvider);
    }

    public void updateIdentityProvider(String realm, IdentityProviderRepresentation identityProviderToUpdate) {
        IdentityProviderResource identityProviderResource = realmRepository.loadRealm(realm)
                .identityProviders()
                .get(identityProviderToUpdate.getAlias());

        identityProviderResource.update(identityProviderToUpdate);
    }

    public IdentityProviderRepresentation findIdentityProvider(String realm, String identityProviderName) {
        return tryToFindIdentityProvider(realm, identityProviderName)
                .orElseThrow(
                        () -> new KeycloakRepositoryException(
                                "Cannot find identityProvider '" + identityProviderName + "' within realm '" + realm + "'"
                        )
                );
    }

    public List<IdentityProviderRepresentation> findIdentityProviders(String realm, Collection<String> identityProviders) {
        return identityProviders.stream()
                .map(identityProvider -> findIdentityProvider(realm, identityProvider))
                .collect(Collectors.toList());
    }

    public List<IdentityProviderRepresentation> searchIdentityProviders(String realm, List<String> identityProviders) {
        return identityProviders.stream()
                .map(identityProvider -> realmRepository.loadRealm(realm)
                        .identityProviders()
                        .get(identityProvider)
                        .toRepresentation()
                )
                .collect(Collectors.toList());
    }
}
