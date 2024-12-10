/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2021 adorsys GmbH & Co. KG @ https://adorsys.com
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

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.IdentityProvidersResource;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.ws.rs.core.Response;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class IdentityProviderMapperRepository {

    private final RealmRepository realmRepository;
    private final IdentityProviderRepository identityProviderRepository;

    @Autowired
    public IdentityProviderMapperRepository(RealmRepository realmRepository, IdentityProviderRepository identityProviderRepository) {
        this.realmRepository = realmRepository;
        this.identityProviderRepository = identityProviderRepository;
    }

    public Optional<IdentityProviderMapperRepresentation> search(String realmName, String identityProviderAlias, String name) {
        List<IdentityProviderMapperRepresentation> identityProviderMappers = realmRepository
                .getResource(realmName).identityProviders().get(identityProviderAlias).getMappers();

        if (identityProviderMappers == null) {
            return Optional.empty();
        }

        return identityProviderMappers.stream()
                .filter(m -> Objects.equals(m.getName(), name) && Objects.equals(m.getIdentityProviderAlias(), identityProviderAlias))
                .findFirst();
    }

    public IdentityProviderMapperRepresentation get(String realmName, String identityProviderAlias, String name) {
        Optional<IdentityProviderMapperRepresentation> maybeIdentityProviderMapper = search(realmName, identityProviderAlias, name);

        return maybeIdentityProviderMapper.orElse(null);
    }

    public List<IdentityProviderMapperRepresentation> getAll(String realmName) {
        List<IdentityProviderMapperRepresentation> mappers = new ArrayList<>();
        IdentityProvidersResource identityProvidersResource = realmRepository
                .getResource(realmName).identityProviders();

        List<IdentityProviderRepresentation> identityProviders = identityProviderRepository.getAll(realmName);

        for (IdentityProviderRepresentation identityProvider : identityProviders) {
            mappers.addAll(identityProvidersResource.get(identityProvider.getAlias()).getMappers());
        }
        return mappers;
    }

    public void create(String realmName, IdentityProviderMapperRepresentation identityProviderMapper) {
        IdentityProviderResource resource = realmRepository
                .getResource(realmName).identityProviders()
                .get(identityProviderMapper.getIdentityProviderAlias());

        try (Response response = resource.addMapper(identityProviderMapper)) {
            CreatedResponseUtil.getCreatedId(response);
        }
    }

    public void update(String realmName, IdentityProviderMapperRepresentation identityProviderMapperToUpdate) {
        IdentityProvidersResource identityProvidersResource = realmRepository
                .getResource(realmName).identityProviders();

        identityProvidersResource
                .get(identityProviderMapperToUpdate.getIdentityProviderAlias())
                .update(identityProviderMapperToUpdate.getId(), identityProviderMapperToUpdate);
    }

    public void delete(String realmName, IdentityProviderMapperRepresentation identityProviderMapperToDelete) {
        IdentityProvidersResource identityProvidersResource = realmRepository
                .getResource(realmName).identityProviders();

        String identityProviderAlias = identityProviderMapperToDelete.getIdentityProviderAlias();

        identityProvidersResource
                .get(identityProviderAlias)
                .delete(identityProviderMapperToDelete.getId());
    }
}
