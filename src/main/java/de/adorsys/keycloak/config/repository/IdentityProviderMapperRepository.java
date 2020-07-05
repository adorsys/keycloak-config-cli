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
import de.adorsys.keycloak.config.util.StreamUtil;
import org.keycloak.admin.client.resource.IdentityProvidersResource;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class IdentityProviderMapperRepository {

    private final RealmRepository realmRepository;

    @Autowired
    public IdentityProviderMapperRepository(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    public Optional<IdentityProviderMapperRepresentation> tryToFindIdentityProviderMapper(String realm, String identityProviderAlias, String name) {
        return loadIdentityProviderMapperByName(realm, identityProviderAlias, name);
    }

    public IdentityProviderMapperRepresentation getIdentityProviderMapperByName(String realm, String identityProviderAlias, String name) {
        Optional<IdentityProviderMapperRepresentation> maybeIdentityProviderMapper = loadIdentityProviderMapperByName(realm, identityProviderAlias, name);

        return maybeIdentityProviderMapper.orElse(null);
    }

    public List<IdentityProviderMapperRepresentation> getIdentityProviderMappers(String realm) {
        List<IdentityProviderMapperRepresentation> mappers = new ArrayList<>();
        IdentityProvidersResource identityProvidersResource = realmRepository.loadRealm(realm).identityProviders();
        List<IdentityProviderRepresentation> identityProviders = identityProvidersResource.findAll();

        for (IdentityProviderRepresentation identityProvider : identityProviders) {
            mappers.addAll(identityProvidersResource.get(identityProvider.getAlias()).getMappers());
        }
        return mappers;
    }

    public void createIdentityProviderMapper(String realm, IdentityProviderMapperRepresentation identityProviderMapper) {
        IdentityProvidersResource identityProvidersResource = realmRepository.loadRealm(realm).identityProviders();

        Response response = identityProvidersResource.get(identityProviderMapper.getIdentityProviderAlias()).addMapper(identityProviderMapper);
        ResponseUtil.validate(response);
    }

    public void updateIdentityProviderMapper(String realm, IdentityProviderMapperRepresentation identityProviderMapperToUpdate) {
        IdentityProvidersResource identityProvidersResource = realmRepository.loadRealm(realm).identityProviders();

        identityProvidersResource.get(identityProviderMapperToUpdate.getIdentityProviderAlias()).update(identityProviderMapperToUpdate.getId(), identityProviderMapperToUpdate);
    }

    public void deleteIdentityProviderMapper(String realm, IdentityProviderMapperRepresentation identityProviderMapperToDelete) {
        IdentityProvidersResource identityProvidersResource = realmRepository.loadRealm(realm).identityProviders();
        String identityProviderAlias = identityProviderMapperToDelete.getIdentityProviderAlias();

        identityProvidersResource.get(identityProviderAlias).delete(identityProviderMapperToDelete.getId());
    }

    private Optional<IdentityProviderMapperRepresentation> loadIdentityProviderMapperByName(String realm, String identityProviderAlias, String name) {
        return StreamUtil.collectionAsStream(realmRepository.get(realm).getIdentityProviderMappers()).filter(m -> m.getName().equals(name) && m.getIdentityProviderAlias().equals(identityProviderAlias)).findFirst();
    }

}
