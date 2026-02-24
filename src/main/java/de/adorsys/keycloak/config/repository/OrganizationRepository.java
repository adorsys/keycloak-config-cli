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

import de.adorsys.keycloak.config.condition.ConditionalOnKeycloakVersion26OrNewer;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.OrganizationIdentityProviderResource;
import org.keycloak.admin.client.resource.OrganizationMemberResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.OrganizationsResource;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
@ConditionalOnKeycloakVersion26OrNewer
public class OrganizationRepository {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationRepository.class);

    private final RealmRepository realmRepository;

    public OrganizationRepository(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    public List<OrganizationRepresentation> getAll(String realmName) {
        return getOrganizationsResource(realmName).getAll();
    }

    public Optional<OrganizationRepresentation> search(String realmName, String alias) {
        return getAll(realmName)
                .stream()
                .filter(o -> Objects.equals(alias, o.getAlias()))
                .findFirst();
    }

    public OrganizationRepresentation getByAlias(String realmName, String alias) {
        OrganizationRepresentation org = search(realmName, alias)
                .orElseThrow(() -> new NotFoundException("Organization with alias '" + alias + "' not found"));
        return getResourceById(realmName, org.getId()).toRepresentation();
    }

    public void create(String realmName, OrganizationRepresentation organization) {
        OrganizationsResource organizationsResource = getOrganizationsResource(realmName);
        try (Response response = organizationsResource.create(organization)) {
            String createdId = CreatedResponseUtil.getCreatedId(response);
            logger.debug("Created organization '{}' with id '{}'", organization.getAlias(), createdId);
        }
    }

    public void update(String realmName, OrganizationRepresentation organization) {
        OrganizationResource resource = getResourceById(realmName, organization.getId());
        try (Response ignored = resource.update(organization)) {
            logger.debug("Updated organization '{}'", organization.getAlias());
        }
    }

    public void delete(String realmName, OrganizationRepresentation organization) {
        OrganizationResource resource = getResourceById(realmName, organization.getId());
        try (Response ignored = resource.delete()) {
            logger.debug("Deleted organization '{}'", organization.getAlias());
        }
    }

    public List<IdentityProviderRepresentation> getIdentityProviders(String realmName, String organizationId) {
        return getResourceById(realmName, organizationId)
                .identityProviders()
                .getIdentityProviders();
    }

    public void addIdentityProvider(String realmName, String organizationId, String idpAlias) {
        OrganizationResource resource = getResourceById(realmName, organizationId);
        try (Response response = resource.identityProviders().addIdentityProvider(idpAlias)) {
            logger.debug("Added identity provider '{}' to organization '{}' (status={})", idpAlias, organizationId, response.getStatus());
        }
    }

    public void removeIdentityProvider(String realmName, String organizationId, String idpAlias) {
        OrganizationResource resource = getResourceById(realmName, organizationId);
        OrganizationIdentityProviderResource idpResource = resource.identityProviders().get(idpAlias);
        try (Response response = idpResource.delete()) {
            logger.debug("Removed identity provider '{}' from organization '{}' (status={})", idpAlias, organizationId, response.getStatus());
        }
    }

    public List<MemberRepresentation> getMembers(String realmName, String organizationId) {
        return getResourceById(realmName, organizationId)
                .members()
                .getAll();
    }

    public void addMember(String realmName, String organizationId, String userId) {
        OrganizationResource resource = getResourceById(realmName, organizationId);
        try (Response response = resource.members().addMember(userId)) {
            logger.debug("Added member '{}' to organization '{}' (status={})", userId, organizationId, response.getStatus());
        }
    }

    public void removeMember(String realmName, String organizationId, String userId) {
        OrganizationResource resource = getResourceById(realmName, organizationId);
        OrganizationMemberResource memberResource = resource.members().member(userId);
        try (Response response = memberResource.delete()) {
            logger.debug("Removed member '{}' from organization '{}' (status={})", userId, organizationId, response.getStatus());
        }
    }

    private OrganizationsResource getOrganizationsResource(String realmName) {
        return realmRepository.getResource(realmName).organizations();
    }

    private OrganizationResource getResourceById(String realmName, String id) {
        return getOrganizationsResource(realmName).get(id);
    }
}
