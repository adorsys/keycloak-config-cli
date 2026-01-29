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

import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.util.ResteasyUtil;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.keycloak.admin.client.CreatedResponseUtil;
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
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;

/**
 * Repository for managing Keycloak Organizations (Keycloak 26.0+).
 * Provides CRUD operations and management of organization associations
 * (domains, identity providers, members).
 */
@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class OrganizationRepository {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationRepository.class);

    private final RealmRepository realmRepository;
    private final KeycloakProvider keycloakProvider;
    private final KeycloakConfigProperties properties;

    public OrganizationRepository(RealmRepository realmRepository, KeycloakProvider keycloakProvider, KeycloakConfigProperties properties) {
        this.realmRepository = realmRepository;
        this.keycloakProvider = keycloakProvider;
        this.properties = properties;
    }

    /**
     * Search for an organization by alias.
     *
     * @param realmName the realm name
     * @param alias     the organization alias
     * @return Optional containing the organization if found, empty otherwise
     */
    public Optional<OrganizationRepresentation> search(String realmName, String alias) {
        Optional<OrganizationRepresentation> maybeOrganization;

        try {
            OrganizationResource organizationResource = getResourceByAlias(realmName, alias);
            maybeOrganization = Optional.of(organizationResource.toRepresentation());
        } catch (NotFoundException e) {
            maybeOrganization = Optional.empty();
        }

        return maybeOrganization;
    }

    /**
     * Get an organization by alias, throwing exception if not found.
     *
     * @param realmName the realm name
     * @param alias     the organization alias
     * @return the organization representation
     * @throws NotFoundException if organization not found
     */
    public OrganizationRepresentation getByAlias(String realmName, String alias) {
        OrganizationResource organizationResource = getResourceByAlias(realmName, alias);
        return organizationResource.toRepresentation();
    }

    /**
     * Get all organizations in a realm.
     *
     * @param realmName the realm name
     * @return list of all organizations
     */
    public List<OrganizationRepresentation> getAll(String realmName) {
        OrganizationsResource organizationsResource = getOrganizationsResource(realmName);
        // Get all organizations
        return organizationsResource.getAll();
    }

    /**
     * Create a new organization.
     *
     * @param realmName    the realm name
     * @param organization the organization to create
     */
    public void create(String realmName, OrganizationRepresentation organization) {
        OrganizationsResource organizationsResource = getOrganizationsResource(realmName);

        Response response = null;
        try {
            response = organizationsResource.create(organization);
            if (response != null) {
                String createdId = CreatedResponseUtil.getCreatedId(response);
                logger.debug("Created organization '{}' with id '{}'", organization.getAlias(), createdId);
            } else {
                logger.debug("Created organization '{}' but create() returned null response", organization.getAlias());
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    /**
     * Update an existing organization.
     *
     * @param realmName    the realm name
     * @param organization the organization to update
     */
    public void update(String realmName, OrganizationRepresentation organization) {
        OrganizationResource organizationResource = getResourceById(realmName, organization.getId());
        organizationResource.update(organization);
        logger.debug("Updated organization '{}'", organization.getAlias());
    }

    /**
     * Delete an organization.
     *
     * @param realmName    the realm name
     * @param organization the organization to delete
     */
    public void delete(String realmName, OrganizationRepresentation organization) {
        OrganizationResource organizationResource = getResourceById(realmName, organization.getId());
        organizationResource.delete();
        logger.debug("Deleted organization '{}'", organization.getAlias());
    }

    /**
     * Add an identity provider to an organization.
     *
     * @param realmName the realm name
     * @param orgId     the organization ID
     * @param idpAlias  the identity provider alias
     */
    public void addIdentityProvider(String realmName, String orgId, String idpAlias) {
        // First verify the identity provider exists in the realm
        IdentityProviderRepresentation realmIdp = getRealmIdentityProvider(realmName, idpAlias);
        if (realmIdp == null) {
            logger.error("Identity provider '{}' not found in realm '{}'", idpAlias, realmName);
            throw new NotFoundException("Identity provider '" + idpAlias + "' not found in realm '" + realmName + "'");
        }
        
        // Use raw REST client to call the correct API endpoint
        // POST /admin/realms/{realm}/organizations/{orgId}/identity-providers
        // According to docs: Payload should contain only id or alias of the identity provider [string]
        String path = String.format("/admin/realms/%s/organizations/%s/identity-providers", realmName, orgId);
        
        try {
            ResteasyWebTarget target = ResteasyUtil.getClient(
                    !properties.isSslVerify(),
                    properties.getHttpProxy(),
                    properties.getConnectTimeout(),
                    properties.getReadTimeout()
            ).target(properties.getUrl() + path);
            
            // Send the alias as a JSON string to match @Consumes annotation
            Response response = target.request()
                    .header("Authorization", "Bearer " + keycloakProvider.getInstance().tokenManager().getAccessToken().getToken())
                    .post(Entity.json("\"" + idpAlias + "\""));
                    
            if (response.getStatus() == 201 || response.getStatus() == 204) {
                // Success - no logging needed
            } else if (response.getStatus() == 409) {
                // Already exists - no logging needed
            } else {
                String errorResponse = response.hasEntity() ? response.readEntity(String.class) : "No entity";
                logger.error("Failed to add identity provider '{}' to organization '{}': {} - {}",
                        idpAlias, orgId, response.getStatus(), errorResponse);
                throw new RuntimeException("Failed to add identity provider: " + errorResponse);
            }
            response.close();
        } catch (Exception e) {
            logger.error("Failed to add identity provider '{}' to organization '{}'", idpAlias, orgId, e);
            throw new RuntimeException("Failed to add identity provider: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get an identity provider from the realm.
     */
    private IdentityProviderRepresentation getRealmIdentityProvider(String realmName, String idpAlias) {
        return realmRepository.getResource(realmName)
                .identityProviders()
                .findAll()
                .stream()
                .filter(idp -> idpAlias.equals(idp.getAlias()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Remove an identity provider from an organization.
     *
     * @param realmName the realm name
     * @param orgId     the organization ID
     * @param idpAlias  the identity provider alias
     */
    public void removeIdentityProvider(String realmName, String orgId, String idpAlias) {
        OrganizationResource organizationResource = getResourceById(realmName, orgId);
        organizationResource.identityProviders().get(idpAlias).delete();
        logger.debug("Removed identity provider '{}' from organization '{}'", idpAlias, orgId);
    }

    /**
     * Get all identity providers associated with an organization.
     *
     * @param realmName the realm name
     * @param orgId     the organization ID
     * @return list of identity providers
     */
    public List<IdentityProviderRepresentation> getIdentityProviders(String realmName, String orgId) {
        OrganizationResource organizationResource = getResourceById(realmName, orgId);
        return organizationResource.identityProviders().getIdentityProviders();
    }

    /**
     * Add a member to an organization.
     *
     * @param realmName the realm name
     * @param orgId     the organization ID
     * @param userId    the user ID
     */
    public void addMember(String realmName, String orgId, String userId) {
        OrganizationResource organizationResource = getResourceById(realmName, orgId);

        try (Response response = organizationResource.members().addMember(userId)) {
            if (response.getStatus() == 201) {
                logger.debug("Added user '{}' to organization '{}'", userId, orgId);
            } else if (response.getStatus() == 409) {
                logger.debug("User '{}' already member of organization '{}'", userId, orgId);
            }
        }
    }

    /**
     * Remove a member from an organization.
     *
     * @param realmName the realm name
     * @param orgId     the organization ID
     * @param userId    the user ID
     */
    public void removeMember(String realmName, String orgId, String userId) {
        OrganizationResource organizationResource = getResourceById(realmName, orgId);
        organizationResource.members().member(userId).delete();
        logger.debug("Removed user '{}' from organization '{}'", userId, orgId);
    }

    /**
     * Get all members of an organization.
     *
     * @param realmName the realm name
     * @param orgId     the organization ID
     * @return list of members
     */
    public List<MemberRepresentation> getMembers(String realmName, String orgId) {
        OrganizationResource organizationResource = getResourceById(realmName, orgId);
        // Get all members
        return organizationResource.members().getAll();
    }

    private OrganizationsResource getOrganizationsResource(String realmName) {
        return realmRepository.getResource(realmName).organizations();
    }

    private OrganizationResource getResourceByAlias(String realmName, String alias) {
        // Resolve ID from alias
        Optional<OrganizationRepresentation> org = getOrganizationsResource(realmName).getAll().stream()
                .filter(o -> Objects.equals(o.getAlias(), alias))
                .findFirst();

        if (org.isEmpty()) {
            throw new NotFoundException("Organization with alias '" + alias + "' not found");
        }

        return getOrganizationsResource(realmName).get(org.get().getId());
    }

    private OrganizationResource getResourceById(String realmName, String id) {
        return getOrganizationsResource(realmName).get(id);
    }
}
