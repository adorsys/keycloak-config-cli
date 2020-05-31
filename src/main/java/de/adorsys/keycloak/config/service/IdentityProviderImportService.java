/*
 * Copyright 2019-2020 adorsys GmbH & Co. KG @ https://adorsys.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.IdentityProviderRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.jboss.logging.Logger;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

@Dependent
public class IdentityProviderImportService {
    private static final Logger LOG = Logger.getLogger(IdentityProviderImportService.class);

    @Inject
    IdentityProviderRepository identityProviderRepository;

    public void doImport(RealmImport realmImport) {
        createOrUpdateIdentityProviders(realmImport);
    }

    private void createOrUpdateIdentityProviders(RealmImport realmImport) {
        List<IdentityProviderRepresentation> identityProviders = realmImport.getIdentityProviders();

        if (identityProviders != null) {
            for (IdentityProviderRepresentation identityProvider : identityProviders) {
                createOrUpdateIdentityProvider(realmImport, identityProvider);
            }
        }
    }

    private void createOrUpdateIdentityProvider(RealmImport realmImport, IdentityProviderRepresentation identityProvider) {
        String identityProviderName = identityProvider.getAlias();
        String realm = realmImport.getRealm();

        Optional<IdentityProviderRepresentation> maybeIdentityProvider = identityProviderRepository.tryToFindIdentityProvider(realm, identityProviderName);

        if (maybeIdentityProvider.isPresent()) {
            LOG.debugf("Update identityProvider '%s' in realm '%s'", identityProviderName, realm);
            updateIdentityProvider(realm, maybeIdentityProvider.get(), identityProvider);
        } else {
            LOG.debugf("Create identityProvider '%s' in realm '%s'", identityProviderName, realm);
            identityProviderRepository.createIdentityProvider(realm, identityProvider);
        }
    }

    private void updateIdentityProvider(String realm, IdentityProviderRepresentation existingIdentityProvider, IdentityProviderRepresentation identityProviderToImport) {
        IdentityProviderRepresentation patchedIdentityProvider = CloneUtils.deepPatch(existingIdentityProvider, identityProviderToImport);
        identityProviderRepository.updateIdentityProvider(realm, patchedIdentityProvider);
    }
}
