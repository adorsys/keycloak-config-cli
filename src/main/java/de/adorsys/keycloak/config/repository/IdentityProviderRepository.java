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

package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.util.ResponseUtil;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.IdentityProvidersResource;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@Service
public class IdentityProviderRepository {

    private final RealmRepository realmRepository;

    @Autowired
    public IdentityProviderRepository(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    public Optional<IdentityProviderRepresentation> tryToFindIdentityProvider(String realm, String alias) {
        Optional<IdentityProviderRepresentation> maybeIdentityProvider;

        IdentityProviderResource identityProviderResource = loadIdentityProviderByAlias(realm, alias);

        try {
            maybeIdentityProvider = Optional.of(identityProviderResource.toRepresentation());
        } catch (NotFoundException e) {
            maybeIdentityProvider = Optional.empty();
        }

        return maybeIdentityProvider;
    }

    public IdentityProviderRepresentation getIdentityProviderByAlias(String realm, String alias) {
        IdentityProviderResource identityProviderResource = loadIdentityProviderByAlias(realm, alias);
        if (identityProviderResource == null) {
            return null;
        }
        return identityProviderResource.toRepresentation();
    }

    public List<IdentityProviderRepresentation> getIdentityProviders(String realm) {
        return realmRepository.loadRealm(realm).identityProviders().findAll();
    }

    public void createIdentityProvider(String realm, IdentityProviderRepresentation identityProvider) {
        IdentityProvidersResource identityProvidersResource = realmRepository.loadRealm(realm).identityProviders();
        Response response = identityProvidersResource.create(identityProvider);
        ResponseUtil.validate(response);
    }

    public void updateIdentityProvider(String realm, IdentityProviderRepresentation identityProviderToUpdate) {
        IdentityProviderResource identityProviderResource = realmRepository.loadRealm(realm)
                .identityProviders()
                .get(identityProviderToUpdate.getAlias());

        identityProviderResource.update(identityProviderToUpdate);
    }

    public void deleteIdentityProvider(String realm, IdentityProviderRepresentation identityProviderToDelete) {
        IdentityProviderResource identityProviderResource = realmRepository.loadRealm(realm)
                .identityProviders()
                .get(identityProviderToDelete.getInternalId());

        identityProviderResource.remove();
    }

    private IdentityProviderResource loadIdentityProviderByAlias(String realm, String identityProviderAlias) {
        return realmRepository.loadRealm(realm)
                .identityProviders().get(identityProviderAlias);
    }
}
