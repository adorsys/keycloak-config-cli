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

package de.adorsys.keycloak.config.repository;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.IdentityProvidersResource;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.util.Optional;

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
}
