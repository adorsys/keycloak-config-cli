/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2020 adorsys GmbH & Co. KG @ https://adorsys.de
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.IdentityProviderRepository;
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
        createOrUpdateIdentityProviders(realmImport);
    }

    private void createOrUpdateIdentityProviders(RealmImport realmImport) {
        List<IdentityProviderRepresentation> identityProviders = realmImport.getIdentityProviders();
        if (identityProviders == null) return;

        for (IdentityProviderRepresentation identityProvider : identityProviders) {
            createOrUpdateIdentityProvider(realmImport, identityProvider);
        }
    }

    private void createOrUpdateIdentityProvider(RealmImport realmImport, IdentityProviderRepresentation identityProvider) {
        String identityProviderName = identityProvider.getAlias();
        String realm = realmImport.getRealm();

        Optional<IdentityProviderRepresentation> maybeIdentityProvider = identityProviderRepository.tryToFindIdentityProvider(realm, identityProviderName);

        if (maybeIdentityProvider.isPresent()) {
            logger.debug("Update identityProvider '{}' in realm '{}'", identityProviderName, realm);
            identityProviderRepository.updateIdentityProvider(realm, identityProvider);
        } else {
            logger.debug("Create identityProvider '{}' in realm '{}'", identityProviderName, realm);
            identityProviderRepository.createIdentityProvider(realm, identityProvider);
        }
    }
}
