package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.IdentityProviderRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IdentityProviderImportService {
    private static final Logger logger = LoggerFactory.getLogger(IdentityProviderImportService.class);

    private final IdentityProviderRepository identityProviderRepository;

    @Autowired
    public IdentityProviderImportService(
            IdentityProviderRepository identityProviderRepository
    ) {
        this.identityProviderRepository = identityProviderRepository;
    }

    public void doImport(RealmImport realmImport) {
        createOrUpdateRealmIdentityProviders(realmImport);
    }

    private void createOrUpdateRealmIdentityProviders(RealmImport realmImport) {
        List<IdentityProviderRepresentation> realmIdentityProviders = realmImport.getIdentityProviders();

        for (IdentityProviderRepresentation identityProvider : realmIdentityProviders) {
            createOrUpdateRealmIdentityProvider(realmImport, identityProvider);
        }
    }

    private void createOrUpdateRealmIdentityProvider(RealmImport realmImport, IdentityProviderRepresentation identityProvider) {
        String identityProviderName = identityProvider.getAlias();
        String realm = realmImport.getRealm();

        Optional<IdentityProviderRepresentation> maybeIdentityProvider = identityProviderRepository.tryToFindRealmIdentityProvider(realm, identityProviderName);

        if (maybeIdentityProvider.isPresent()) {
            logger.debug("Update identityProvider '{}' in realm '{}'", identityProviderName, realm);
            updateRealmIdentityProvider(realm, maybeIdentityProvider.get(), identityProvider);
        } else {
            logger.debug("Create identityProvider '{}' in realm '{}'", identityProviderName, realm);
            identityProviderRepository.createRealmIdentityProvider(realm, identityProvider);
        }
    }

    private void updateRealmIdentityProvider(String realm, IdentityProviderRepresentation existingIdentityProvider, IdentityProviderRepresentation identityProviderToImport) {
        IdentityProviderRepresentation patchedIdentityProvider = CloneUtils.deepPatch(existingIdentityProvider, identityProviderToImport);
        identityProviderRepository.updateRealmIdentityProvider(realm, patchedIdentityProvider);
    }
}
