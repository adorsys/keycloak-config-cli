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

import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.resource.ManagementPermissions;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.IdentityProvidersResource;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ManagementPermissionRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class IdentityProviderRepository {

    private final RealmRepository realmRepository;
    private final KeycloakProvider keycloakProvider;

    @Autowired
    public IdentityProviderRepository(RealmRepository realmRepository, KeycloakProvider keycloakProvider) {
        this.realmRepository = realmRepository;
        this.keycloakProvider = keycloakProvider;
    }

    public Optional<IdentityProviderRepresentation> search(String realmName, String alias) {
        Optional<IdentityProviderRepresentation> maybeIdentityProvider;

        IdentityProviderResource identityProviderResource = getResourceByAlias(realmName, alias);

        try {
            maybeIdentityProvider = Optional.of(identityProviderResource.toRepresentation());
        } catch (NotFoundException e) {
            maybeIdentityProvider = Optional.empty();
        }

        return maybeIdentityProvider;
    }

    public IdentityProviderRepresentation getByAlias(String realmName, String alias) {
        IdentityProviderResource identityProviderResource = getResourceByAlias(realmName, alias);
        if (identityProviderResource == null) {
            return null;
        }
        return identityProviderResource.toRepresentation();
    }

    public List<IdentityProviderRepresentation> getAll(String realmName) {
        return realmRepository.getResource(realmName).identityProviders().findAll();
    }

    public void create(String realmName, IdentityProviderRepresentation identityProvider) {
        IdentityProvidersResource identityProvidersResource = realmRepository.getResource(realmName).identityProviders();
        try (Response response = identityProvidersResource.create(identityProvider)) {
            CreatedResponseUtil.getCreatedId(response);
        }
    }

    public void update(String realmName, IdentityProviderRepresentation identityProviderToUpdate) {
        IdentityProviderResource identityProviderResource = realmRepository.getResource(realmName)
                .identityProviders()
                .get(identityProviderToUpdate.getAlias());

        identityProviderResource.update(identityProviderToUpdate);
    }

    public void delete(String realmName, IdentityProviderRepresentation identityProviderToDelete) {
        IdentityProviderResource identityProviderResource = realmRepository.getResource(realmName)
                .identityProviders()
                .get(identityProviderToDelete.getInternalId());

        identityProviderResource.remove();
    }

    public boolean isPermissionEnabled(String realmName, String alias) {
        ManagementPermissions permissions = keycloakProvider.getCustomApiProxy(ManagementPermissions.class);
        return permissions.getIdpPermissions(realmName, alias).isEnabled();
    }

    public void enablePermission(String realmName, String alias) {
        ManagementPermissions permissions = keycloakProvider.getCustomApiProxy(ManagementPermissions.class);
        permissions.setIdpPermissions(realmName, alias, new ManagementPermissionRepresentation(true));
    }

    private IdentityProviderResource getResourceByAlias(String realmName, String identityProviderAlias) {
        return realmRepository.getResource(realmName).identityProviders().get(identityProviderAlias);
    }
}
