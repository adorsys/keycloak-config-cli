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

package io.github.doriangrelu.keycloak.config.service;

import io.github.doriangrelu.keycloak.config.exception.ImportProcessingException;
import io.github.doriangrelu.keycloak.config.exception.KeycloakRepositoryException;
import io.github.doriangrelu.keycloak.config.model.RealmImport;
import io.github.doriangrelu.keycloak.config.properties.ImportConfigProperties;
import io.github.doriangrelu.keycloak.config.provider.KeycloakProvider;
import io.github.doriangrelu.keycloak.config.repository.ClientRepository;
import io.github.doriangrelu.keycloak.config.repository.GroupRepository;
import io.github.doriangrelu.keycloak.config.repository.IdentityProviderRepository;
import io.github.doriangrelu.keycloak.config.repository.RoleRepository;
import io.github.doriangrelu.keycloak.config.service.clientauthorization.ClientPermissionResolver;
import io.github.doriangrelu.keycloak.config.service.clientauthorization.GroupPermissionResolver;
import io.github.doriangrelu.keycloak.config.service.clientauthorization.IdpPermissionResolver;
import io.github.doriangrelu.keycloak.config.service.clientauthorization.PermissionResolver;
import io.github.doriangrelu.keycloak.config.service.clientauthorization.PermissionTypeAndId;
import io.github.doriangrelu.keycloak.config.service.clientauthorization.RolePermissionResolver;
import io.github.doriangrelu.keycloak.config.service.state.StateService;
import io.github.doriangrelu.keycloak.config.util.CloneUtil;
import io.github.doriangrelu.keycloak.config.util.JsonUtil;
import io.github.doriangrelu.keycloak.config.util.KeycloakUtil;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.WebApplicationException;

import static io.github.doriangrelu.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues.FULL;

@Service
@SuppressWarnings({"java:S1192"})
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class ClientAuthorizationImportService {
    private static final Logger logger = LoggerFactory.getLogger(ClientAuthorizationImportService.class);

    public static final String REALM_MANAGEMENT_CLIENT_ID = "realm-management";
    public static final String ADMIN_PERMISSIONS_CLIENT_ID = "admin-permissions";
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_NOT_IMPLEMENTED = 501;
    private static final String FGAP_V2_RESOURCE_WARNING = "Cannot {} authorization resource '{}' for client '{}' - {}";
    private static final String FGAP_V2_SCOPE_WARNING = "Cannot {} authorization scope '{}' for client '{}' - {}";
    private static final String FGAP_V2_POLICY_WARNING = "Cannot {} authorization policy '{}' for client '{}' - {}";

    /**
     * Maps FGAP V2 resource types to V1 permission types.
     * V2 uses plural forms (Clients, Groups), V1 uses singular (client, group).
     */
    private enum ResourceTypeMapping {
        CLIENTS("Clients", "client"),
        GROUPS("Groups", "group"),
        USERS("Users", "user"),
        ROLES("Roles", "role"),
        IDENTITY_PROVIDERS("IdentityProviders", "idp");

        private final String v2ResourceType;
        private final String v1PermissionType;

        ResourceTypeMapping(String v2ResourceType, String v1PermissionType) {
            this.v2ResourceType = v2ResourceType;
            this.v1PermissionType = v1PermissionType;
        }

        static String getV1PermissionType(String v2ResourceType) {
            for (ResourceTypeMapping mapping : values()) {
                if (mapping.v2ResourceType.equals(v2ResourceType)) {
                    return mapping.v1PermissionType;
                }
            }
            return null;
        }
    }

    private final ClientRepository clientRepository;
    private final IdentityProviderRepository identityProviderRepository;
    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;
    private final ImportConfigProperties importConfigProperties;
    private final StateService stateService;
    private final KeycloakProvider keycloakProvider;

    public ClientAuthorizationImportService(
            ClientRepository clientRepository,
            IdentityProviderRepository identityProviderRepository,
            RoleRepository roleRepository,
            GroupRepository groupRepository,
            ImportConfigProperties importConfigProperties,
            StateService stateService,
            KeycloakProvider keycloakProvider
    ) {
        this.clientRepository = clientRepository;
        this.identityProviderRepository = identityProviderRepository;
        this.roleRepository = roleRepository;
        this.groupRepository = groupRepository;
        this.importConfigProperties = importConfigProperties;
        this.stateService = stateService;
        this.keycloakProvider = keycloakProvider;
    }

    private String getFgapV2Message() {
        try {
            if (keycloakProvider != null && keycloakProvider.isFgapV2Active()) {
                return "FGAP V2 is active (authorization managed at realm level).";
            }
        } catch (Exception e) {
            logger.debug("Unable to determine FGAP V2 status, using fallback message", e);
        }
        return "Authorization API not supported.";
    }

    public void doImport(RealmImport realmImport) {
        List<ClientRepresentation> clients = realmImport.getClients();
        if (clients == null) {
            return;
        }

        updateClientAuthorizationSettings(realmImport, clients);
    }

    /**
     * Updates client authorization settings.
     *
     * <p>In FGAP V2 (Keycloak 26.2+), the 'admin-permissions' client is system-managed.
     * Keycloak blocks API access to its authorization settings, making existing state retrieval impossible.
     * Attempting to recreate existing policies would cause conflicts.
     *
     * <p>When FGAP V2 is detected, authorization processing is skipped for 'admin-permissions'.
     * Remove this client from import configurations and use the realm-level
     * {@code adminPermissionsEnabled} flag instead.
     *
     * @see <a href="https://github.com/keycloak/keycloak/issues/43977">Keycloak Issue #43977</a>
     */
    private void updateClientAuthorizationSettings(
            RealmImport realmImport,
            List<ClientRepresentation> clients
    ) {
        String realmName = realmImport.getRealm();

        List<ClientRepresentation> clientsWithAuthorization = clients.stream()
                .filter(client -> client.getAuthorizationSettings() != null)
                .toList();

        boolean fgapV2Active = false;
        try {
            fgapV2Active = keycloakProvider.isFgapV2Active();
        } catch (Exception e) {
            logger.debug("Unable to determine FGAP V2 status in updateClientAuthorizationSettings: {}", e.getMessage());
        }

        for (ClientRepresentation client : clientsWithAuthorization) {
            if (fgapV2Active && ADMIN_PERMISSIONS_CLIENT_ID.equals(client.getClientId())) {
                logger.info("Skipping authorization settings for 'admin-permissions' client in realm '{}' - "
                        + "FGAP V2 manages this client internally and blocks API access (see https://github.com/keycloak/keycloak/issues/43977). "
                        + "Remove this client from your import configuration and use realm-level adminPermissionsEnabled flag instead.", realmName);
                continue;
            }

            ClientRepresentation existingClient = getExistingClient(realmName, client);
            updateAuthorization(realmName, existingClient, client.getAuthorizationSettings());
        }
    }

    private void updateAuthorization(
            String realmName,
            ClientRepresentation client,
            ResourceServerRepresentation authorizationSettingsToImport
    ) {
        // FGAP V2: admin-permissions client authorization handled via error handling
        // Cannot detect authorizationSchema (only in KC client lib 26.2+) - rely on runtime errors

        if (importConfigProperties.isValidate() && !REALM_MANAGEMENT_CLIENT_ID.equals(client.getClientId())
                && (Boolean.TRUE.equals(client.isBearerOnly()) || Boolean.TRUE.equals(client.isPublicClient()))) {
            throw new ImportProcessingException(
                    "Unsupported authorization settings for client '%s' in realm '%s': client must be confidential.",
                    getClientIdentifier(client), realmName
            );
        }

        // Detect FGAP V2 with error handling fallback
        boolean fgapV2;
        try {
            fgapV2 = keycloakProvider.isFgapV2Active();
        } catch (Exception e) {
            logger.warn("Unable to detect FGAP V2 status for realm '{}', falling back to V1 behavior: {}",
                    realmName, e.getMessage());
            fgapV2 = false;
        }

        RealmManagementPermissionsResolver realmManagementPermissionsResolver = new RealmManagementPermissionsResolver(realmName, fgapV2);
        if (REALM_MANAGEMENT_CLIENT_ID.equals(client.getClientId())) {
            realmManagementPermissionsResolver.createFineGrantedPermissions(authorizationSettingsToImport);
        }

        ResourceServerRepresentation existingAuthorization = getExistingAuthorization(realmName, client);

        handleAuthorizationSettings(realmName, client, existingAuthorization, authorizationSettingsToImport);

        final List<ResourceRepresentation> sanitizedAuthorizationResources =
                sanitizeAuthorizationResources(authorizationSettingsToImport, realmManagementPermissionsResolver);
        final List<PolicyRepresentation> sanitizedAuthorizationPolicies = sanitizeAuthorizationPolicies(authorizationSettingsToImport,
                realmManagementPermissionsResolver);

        createOrUpdateAuthorizationResources(realmName, client, existingAuthorization.getResources(), sanitizedAuthorizationResources);
        createOrUpdateAuthorizationScopes(realmName, client, existingAuthorization.getScopes(), authorizationSettingsToImport.getScopes());

        if (importConfigProperties.getManaged().getClientAuthorizationResources() == FULL) {
            removeAuthorizationResources(realmName, client, existingAuthorization.getResources(), sanitizedAuthorizationResources);
        }

        if (importConfigProperties.getManaged().getClientAuthorizationPolicies() == FULL) {
            removeAuthorizationPolicies(realmName, client, existingAuthorization.getPolicies(), sanitizedAuthorizationPolicies);
        }

        if (importConfigProperties.getManaged().getClientAuthorizationScopes() == FULL) {
            removeAuthorizationScopes(realmName, client, existingAuthorization.getScopes(), authorizationSettingsToImport.getScopes());
        }

        // refresh existingAuthorization
        try {
            existingAuthorization = clientRepository.getAuthorizationConfigById(
                    realmName, client.getId()
            );
        } catch (NotFoundException | BadRequestException | ServerErrorException e) {
            int statusCode = e.getResponse().getStatus();
            if (statusCode == HTTP_NOT_FOUND || statusCode == HTTP_NOT_IMPLEMENTED || statusCode == 400) {
                logger.debug("Cannot refresh authorization config for client '{}' in realm '{}' (HTTP {}) - {} "
                        + "Using existing authorization settings.",
                        getClientIdentifier(client), realmName, statusCode, getFgapV2Message());
                // Use the existing authorization settings we already have
            } else {
                throw e;
            }
        }

        createOrUpdateAuthorizationPolicies(realmName, client, existingAuthorization.getPolicies(), sanitizedAuthorizationPolicies);
    }

    private List<ResourceRepresentation> sanitizeAuthorizationResources(ResourceServerRepresentation authorizationSettings,
                                                                        RealmManagementPermissionsResolver realmManagementPermissionsResolver) {
        return authorizationSettings.getResources()
                .stream()
                .map(resource -> sanitizeAuthorizationResource(resource, realmManagementPermissionsResolver))
                .toList();
    }

    private List<PolicyRepresentation> sanitizeAuthorizationPolicies(ResourceServerRepresentation authorizationSettings,
                                                                     RealmManagementPermissionsResolver realmManagementPermissionsResolver) {
        return authorizationSettings.getPolicies()
                .stream()
                .map(policy -> sanitizeAuthorizationPolicy(policy, realmManagementPermissionsResolver))
                .toList();
    }

    private ResourceRepresentation sanitizeAuthorizationResource(ResourceRepresentation resource,
                                                                 RealmManagementPermissionsResolver realmManagementPermissionsResolver) {
        resource.setName(realmManagementPermissionsResolver.getSanitizedAuthzResourceName(resource.getName()));
        return resource;
    }

    private PolicyRepresentation sanitizeAuthorizationPolicy(PolicyRepresentation policy,
                                                             RealmManagementPermissionsResolver realmManagementPermissionsResolver) {
        policy.setName(realmManagementPermissionsResolver.getSanitizedAuthzPolicyName(policy.getName()));

        if (policy.getConfig().containsKey("resources") && policy.getConfig().get("resources").contains("$")) {
            String defaultResourceType = policy.getConfig().get("defaultResourceType");
            String resources = sanitizeAuthorizationPolicyResource(
                    policy.getConfig().get("resources"),
                    defaultResourceType,
                    realmManagementPermissionsResolver
            );
            policy.getConfig().put("resources", resources);
        }

        return policy;
    }

    private String sanitizeAuthorizationPolicyResource(String resources,
                                                       String defaultResourceType,
                                                       RealmManagementPermissionsResolver realmManagementPermissionsResolver) {
        List<String> resourcesList = JsonUtil.fromJson(resources);
        resourcesList = resourcesList.stream()
                .map(resource -> sanitizeSinglePolicyResource(resource, defaultResourceType, realmManagementPermissionsResolver))
                .toList();

        return JsonUtil.toJson(resourcesList);
    }

    private String sanitizeSinglePolicyResource(String resource,
                                               String defaultResourceType,
                                               RealmManagementPermissionsResolver realmManagementPermissionsResolver) {
        if (resource.contains(".resource.")) {
            return realmManagementPermissionsResolver.getSanitizedAuthzResourceName(resource);
        }

        if (resource.startsWith("$")) {
            if (defaultResourceType == null || defaultResourceType.isEmpty()) {
                logger.warn("Found bare placeholder '{}' but no defaultResourceType specified in policy config, skipping transformation", resource);
                return resource;
            }

            String permissionType = mapResourceTypeToPermissionType(defaultResourceType);
            if (permissionType == null) {
                logger.warn("Unknown defaultResourceType '{}' for bare placeholder '{}', skipping transformation", defaultResourceType, resource);
                return resource;
            }

            String fullResourceName = permissionType + ".resource." + resource;
            return realmManagementPermissionsResolver.getSanitizedAuthzResourceName(fullResourceName);
        }

        // No placeholder to resolve
        return resource;
    }

    private String mapResourceTypeToPermissionType(String resourceType) {
        return ResourceTypeMapping.getV1PermissionType(resourceType);
    }

    private void handleAuthorizationSettings(
            String realmName,
            ClientRepresentation client,
            ResourceServerRepresentation existingClientAuthorizationResources,
            ResourceServerRepresentation authorizationResourcesToImport
    ) {
        String[] ignoredProperties = new String[]{"clientId", "policies", "resources", "permissions", "scopes"};

        boolean isEquals = CloneUtil.deepEquals(authorizationResourcesToImport, existingClientAuthorizationResources, ignoredProperties);

        if (isEquals) return;

        ResourceServerRepresentation patchedAuthorizationSettings = CloneUtil
                .patch(existingClientAuthorizationResources, authorizationResourcesToImport);

        patchedAuthorizationSettings.setId(client.getClientId());
        logger.debug("Update authorization settings for client '{}' in realm '{}'", getClientIdentifier(client), realmName);
        try {
            clientRepository.updateAuthorizationSettings(realmName, client.getId(), patchedAuthorizationSettings);
        } catch (NotFoundException e) {
            logger.debug("Client '{}' in realm '{}' does not support authorization settings updates - "
                    + "This is normal for FGAP V2 or clients without authorization support",
                    getClientIdentifier(client), realmName);
        } catch (BadRequestException e) {
            // admin-permissions client rejects authorization settings updates (V2 manages this internally)
            if (ADMIN_PERMISSIONS_CLIENT_ID.equals(client.getClientId())) {
                logger.debug("Skipping authorization settings update for 'admin-permissions' client in realm '{}' - "
                        + "V2 manages authorization configuration internally", realmName);
            } else {
                throw e;
            }
        } catch (ServerErrorException e) {
            if (e.getResponse().getStatus() == HTTP_NOT_IMPLEMENTED || e.getResponse().getStatus() == HTTP_NOT_FOUND) {
                logger.debug("Client '{}' in realm '{}' does not support authorization settings operations - "
                        + "This is normal for FGAP V2 or clients without authorization support",
                        getClientIdentifier(client), realmName);
            } else {
                throw e;
            }
        }
    }

    private void createOrUpdateAuthorizationResources(
            String realmName,
            ClientRepresentation client,
            List<ResourceRepresentation> existingClientAuthorizationResources,
            List<ResourceRepresentation> authorizationResourcesToImport
    ) {
        Map<String, ResourceRepresentation> existingClientAuthorizationResourcesMap =
                existingClientAuthorizationResources
                        .stream()
                        .collect(Collectors.toMap(ResourceRepresentation::getName, resource -> resource));

        for (ResourceRepresentation authorizationResourceToImport : authorizationResourcesToImport) {
            createOrUpdateAuthorizationResource(realmName, client, existingClientAuthorizationResourcesMap, authorizationResourceToImport);
        }
    }

    private void createOrUpdateAuthorizationResource(
            String realmName,
            ClientRepresentation client,
            Map<String, ResourceRepresentation> existingClientAuthorizationResourcesMap,
            ResourceRepresentation authorizationResourceToImport
    ) {
        if (!existingClientAuthorizationResourcesMap.containsKey(authorizationResourceToImport.getName())) {
            createAuthorizationResource(realmName, client, authorizationResourceToImport);
        } else {
            updateAuthorizationResource(realmName, client, existingClientAuthorizationResourcesMap, authorizationResourceToImport);
        }
    }

    private void createAuthorizationResource(
            String realmName,
            ClientRepresentation client,
            ResourceRepresentation authorizationResourceToImport
    ) {
        // https://github.com/adorsys/keycloak-config-cli/issues/589
        setAuthorizationResourceOwner(authorizationResourceToImport);

        logger.debug("Create authorization resource '{}' for client '{}' in realm '{}'",
                authorizationResourceToImport.getName(), getClientIdentifier(client), realmName);

        try {
            clientRepository.createAuthorizationResource(realmName, client.getId(), authorizationResourceToImport);
        } catch (KeycloakRepositoryException e) {
            if (e.getMessage().contains("Authorization API not supported")) {
                // V2 resource type definitions (Groups, Users, Clients, Roles) are auto-created by Keycloak
                boolean isV2ResourceType = authorizationResourceToImport.getName().matches("^(Groups|Users|Clients|Roles)$");
                if (ADMIN_PERMISSIONS_CLIENT_ID.equals(client.getClientId()) && isV2ResourceType) {
                    logger.debug("Skipping V2 resource type '{}' - auto-managed by Keycloak",
                            authorizationResourceToImport.getName());
                } else {
                    logger.warn("Cannot create authorization resource '{}' for client '{}' - {}",
                            authorizationResourceToImport.getName(), getClientIdentifier(client), getFgapV2Message());
                }
                return;
            }
            throw e;
        } catch (NotFoundException | ServerErrorException e) {
            if (isFgapV2Error(e.getResponse().getStatus())) {
                boolean isV2ResourceType = authorizationResourceToImport.getName().matches("^(Groups|Users|Clients|Roles)$");
                if (ADMIN_PERMISSIONS_CLIENT_ID.equals(client.getClientId()) && isV2ResourceType) {
                    logger.debug("Skipping V2 resource type '{}' - auto-managed by Keycloak",
                            authorizationResourceToImport.getName());
                } else {
                    logger.warn("Cannot create authorization resource '{}' for client '{}' - Client does not support FGAP V1 authorization. {}",
                            authorizationResourceToImport.getName(), getClientIdentifier(client), getFgapV2Message());
                }
                return;
            }
            throw e;
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == HTTP_NOT_FOUND) {
                logger.warn("Cannot create authorization resource '{}' for client '{}' - {}",
                        authorizationResourceToImport.getName(), getClientIdentifier(client), getFgapV2Message());
                return;
            }
            throw e;
        }
    }

    private void updateAuthorizationResource(
            String realmName,
            ClientRepresentation client,
            Map<String, ResourceRepresentation> existingClientAuthorizationResourcesMap,
            ResourceRepresentation authorizationResourceToImport
    ) {
        ResourceRepresentation existingClientAuthorizationResource = existingClientAuthorizationResourcesMap
                .get(authorizationResourceToImport.getName());

        if (existingClientAuthorizationResource.getOwner() != null
                && existingClientAuthorizationResource.getOwner().getId() == null
                && Objects.equals(existingClientAuthorizationResource.getOwner().getName(), authorizationResourceToImport.getOwner().getId())) {
            existingClientAuthorizationResource.getOwner().setId(authorizationResourceToImport.getOwner().getId());
            existingClientAuthorizationResource.getOwner().setName(null);
        }

        if (existingClientAuthorizationResource.getAttributes() != null
                && existingClientAuthorizationResource.getAttributes().isEmpty()
                && authorizationResourceToImport.getAttributes() == null) {
            existingClientAuthorizationResource.setAttributes(null);
        }

        boolean isEquals = CloneUtil.deepEquals(
                authorizationResourceToImport, existingClientAuthorizationResource, "id", "_id"
        );

        if (isEquals) return;

        setAuthorizationResourceOwner(authorizationResourceToImport);

        authorizationResourceToImport.setId(existingClientAuthorizationResource.getId());
        logger.debug("Update authorization resource '{}' for client '{}' in realm '{}'",
                authorizationResourceToImport.getName(), getClientIdentifier(client), realmName);

        try {
            clientRepository.updateAuthorizationResource(realmName, client.getId(), authorizationResourceToImport);
        } catch (NotFoundException | ServerErrorException e) {
            if (isFgapV2Error(e.getResponse().getStatus())) {
                logger.warn(FGAP_V2_RESOURCE_WARNING,
                        "update", authorizationResourceToImport.getName(), getClientIdentifier(client), getFgapV2Message());
                return;
            }
            throw e;
        }
    }

    private void removeAuthorizationResources(
            String realmName,
            ClientRepresentation client,
            List<ResourceRepresentation> existingClientAuthorizationResources,
            List<ResourceRepresentation> authorizationResourcesToImport
    ) {
        List<String> authorizationResourceNamesToImport = authorizationResourcesToImport
                .stream().map(ResourceRepresentation::getName)
                .toList();

        List<ResourceRepresentation> managedClientAuthorizationResources = getManagedClientResources(client, existingClientAuthorizationResources);

        managedClientAuthorizationResources.stream()
                .filter(resource -> !authorizationResourceNamesToImport.contains(resource.getName()))
                .forEach(resource -> removeAuthorizationResource(realmName, client, resource));
    }

    private void removeAuthorizationResource(
            String realmName,
            ClientRepresentation client,
            ResourceRepresentation existingClientAuthorizationResource
    ) {
        logger.debug("Remove authorization resource '{}' for client '{}' in realm '{}'",
                existingClientAuthorizationResource.getName(), getClientIdentifier(client), realmName
        );
        try {
            clientRepository.removeAuthorizationResource(
                    realmName, client.getId(), existingClientAuthorizationResource.getName()
            );
        } catch (NotFoundException | ServerErrorException e) {
            if (isFgapV2Error(e.getResponse().getStatus())) {
                logger.warn(FGAP_V2_RESOURCE_WARNING,
                        "remove", existingClientAuthorizationResource.getName(), getClientIdentifier(client), getFgapV2Message());
                return;
            }
            throw e;
        }
    }

    private void createOrUpdateAuthorizationScopes(
            String realmName,
            ClientRepresentation client,
            List<ScopeRepresentation> existingClientAuthorizationScopes,
            List<ScopeRepresentation> authorizationScopesToImport
    ) {
        Map<String, ScopeRepresentation> existingClientAuthorizationScopesMap = existingClientAuthorizationScopes
                .stream()
                .collect(Collectors.toMap(ScopeRepresentation::getName, scope -> scope));

        for (ScopeRepresentation authorizationScopeToImport : authorizationScopesToImport) {
            createOrUpdateAuthorizationScope(
                    realmName, client, existingClientAuthorizationScopesMap, authorizationScopeToImport
            );
        }
    }

    private void createOrUpdateAuthorizationScope(
            String realmName,
            ClientRepresentation client,
            Map<String, ScopeRepresentation> existingClientAuthorizationScopesMap,
            ScopeRepresentation authorizationScopeToImport
    ) {
        if (!existingClientAuthorizationScopesMap.containsKey(authorizationScopeToImport.getName())) {
            logger.debug("Add authorization scope '{}' for client '{}' in realm '{}'",
                    authorizationScopeToImport.getName(), getClientIdentifier(client), realmName
            );
            try {
                clientRepository.addAuthorizationScope(
                        realmName, client.getId(), authorizationScopeToImport
                );
            } catch (KeycloakRepositoryException e) {
                if (e.getMessage().contains("Authorization API not supported")) {
                    logger.warn(FGAP_V2_SCOPE_WARNING,
                            "add", authorizationScopeToImport.getName(), getClientIdentifier(client), getFgapV2Message());
                    return; // Continue gracefully
                }
                throw e;
            } catch (NotFoundException | ServerErrorException e) {
                if (isFgapV2Error(e.getResponse().getStatus())) {
                    logger.warn(FGAP_V2_SCOPE_WARNING,
                            "add", authorizationScopeToImport.getName(), getClientIdentifier(client), getFgapV2Message());
                    return;
                }
                throw e;
            }
        } else {
            updateAuthorizationScope(
                    realmName, client, existingClientAuthorizationScopesMap,
                    authorizationScopeToImport
            );
        }
    }

    private void updateAuthorizationScope(
            String realmName,
            ClientRepresentation client,
            Map<String, ScopeRepresentation> existingClientAuthorizationScopesMap,
            ScopeRepresentation authorizationScopeToImport
    ) {
        ScopeRepresentation existingClientAuthorizationScope = existingClientAuthorizationScopesMap
                .get(authorizationScopeToImport.getName());

        if (!CloneUtil.deepEquals(authorizationScopeToImport, existingClientAuthorizationScope, "id")) {
            authorizationScopeToImport.setId(existingClientAuthorizationScope.getId());
            logger.debug("Update authorization scope '{}' for client '{}' in realm '{}'",
                    authorizationScopeToImport.getName(), getClientIdentifier(client), realmName);

            try {
                clientRepository.updateAuthorizationScope(realmName, client.getId(), authorizationScopeToImport);
            } catch (NotFoundException | ServerErrorException e) {
                if (isFgapV2Error(e.getResponse().getStatus())) {
                    logger.warn(FGAP_V2_SCOPE_WARNING,
                            "update", authorizationScopeToImport.getName(), getClientIdentifier(client), getFgapV2Message());
                    return;
                }
                throw e;
            }
        }
    }

    private void removeAuthorizationScopes(
            String realmName,
            ClientRepresentation client,
            List<ScopeRepresentation> existingClientAuthorizationScopes,
            List<ScopeRepresentation> authorizationScopesToImport
    ) {
        List<String> authorizationScopeNamesToImport = authorizationScopesToImport
                .stream().map(ScopeRepresentation::getName)
                .toList();

        for (ScopeRepresentation existingClientAuthorizationScope : existingClientAuthorizationScopes) {
            if (!authorizationScopeNamesToImport.contains(existingClientAuthorizationScope.getName())) {
                removeAuthorizationScope(realmName, client, existingClientAuthorizationScope);
            }
        }
    }

    private void removeAuthorizationScope(
            String realmName,
            ClientRepresentation client,
            ScopeRepresentation existingClientAuthorizationScope
    ) {
        logger.debug("Remove authorization scope '{}' for client '{}' in realm '{}'",
                existingClientAuthorizationScope.getName(), getClientIdentifier(client), realmName);

        try {
            clientRepository.removeAuthorizationScope(realmName, client.getId(), existingClientAuthorizationScope.getName());
        } catch (NotFoundException | ServerErrorException e) {
            if (isFgapV2Error(e.getResponse().getStatus())) {
                logger.warn(FGAP_V2_SCOPE_WARNING,
                        "remove", existingClientAuthorizationScope.getName(), getClientIdentifier(client), getFgapV2Message());
                return;
            }
            throw e;
        }
    }

    private void createOrUpdateAuthorizationPolicies(
            String realmName,
            ClientRepresentation client,
            List<PolicyRepresentation> existingClientAuthorizationPolicies,
            List<PolicyRepresentation> authorizationPoliciesToImport
    ) {
        Map<String, PolicyRepresentation> existingClientAuthorizationPoliciesMap = existingClientAuthorizationPolicies
                .stream()
                .collect(Collectors.toMap(PolicyRepresentation::getName, resource -> resource));

        for (PolicyRepresentation authorizationPolicyToImport : authorizationPoliciesToImport) {
            createOrUpdateAuthorizationPolicy(
                    realmName, client, existingClientAuthorizationPoliciesMap, authorizationPolicyToImport
            );
        }
    }

    private void createOrUpdateAuthorizationPolicy(
            String realmName,
            ClientRepresentation client,
            Map<String, PolicyRepresentation> existingClientAuthorizationPoliciesMap,
            PolicyRepresentation authorizationPolicyToImport
    ) {
        if (!existingClientAuthorizationPoliciesMap.containsKey(authorizationPolicyToImport.getName())) {
            logger.debug("Create authorization policy '{}' for client '{}' in realm '{}'",
                    authorizationPolicyToImport.getName(), getClientIdentifier(client), realmName);

            try {
                clientRepository.createAuthorizationPolicy(
                        realmName, client.getId(), authorizationPolicyToImport
                );
            } catch (KeycloakRepositoryException e) {
                if (e.getMessage().contains("Authorization API not supported")) {
                    logger.warn(FGAP_V2_POLICY_WARNING,
                            "create", authorizationPolicyToImport.getName(), getClientIdentifier(client), getFgapV2Message());
                    return; // Continue gracefully
                }
                throw e;
            } catch (NotFoundException | ServerErrorException e) {
                if (isFgapV2Error(e.getResponse().getStatus())) {
                    logger.warn(FGAP_V2_SCOPE_WARNING,
                            "create", authorizationPolicyToImport.getName(), getClientIdentifier(client), getFgapV2Message());
                    return;
                }
                throw e;
            }
        } else {
            updateAuthorizationPolicy(
                    realmName, client, existingClientAuthorizationPoliciesMap, authorizationPolicyToImport
            );
        }
    }

    private void updateAuthorizationPolicy(
            String realmName,
            ClientRepresentation client,
            Map<String, PolicyRepresentation> existingClientAuthorizationPoliciesMap,
            PolicyRepresentation authorizationPolicyToImport
    ) {
        PolicyRepresentation existingClientAuthorizationPolicy = existingClientAuthorizationPoliciesMap
                .get(authorizationPolicyToImport.getName());

        if (!CloneUtil.deepEquals(authorizationPolicyToImport, existingClientAuthorizationPolicy, "id")) {
            authorizationPolicyToImport.setId(existingClientAuthorizationPolicy.getId());

            logger.debug(
                    "Update authorization policy '{}' for client '{}' in realm '{}'",
                    authorizationPolicyToImport.getName(), getClientIdentifier(client), realmName
            );

            try {
                clientRepository.updateAuthorizationPolicy(realmName, client.getId(), authorizationPolicyToImport);
            } catch (NotFoundException | ServerErrorException e) {
                if (isFgapV2Error(e.getResponse().getStatus())) {
                    logger.warn(FGAP_V2_POLICY_WARNING,
                            "update", authorizationPolicyToImport.getName(), getClientIdentifier(client), getFgapV2Message());
                    return;
                }
                throw e;
            }
        }
    }

    private void removeAuthorizationPolicies(
            String realmName,
            ClientRepresentation client,
            List<PolicyRepresentation> existingClientAuthorizationPolicies,
            List<PolicyRepresentation> authorizationPoliciesToImport
    ) {
        List<String> authorizationPolicyNamesToImport = authorizationPoliciesToImport
                .stream().map(PolicyRepresentation::getName)
                .toList();

        for (PolicyRepresentation existingClientAuthorizationPolicy : existingClientAuthorizationPolicies) {
            if (!authorizationPolicyNamesToImport.contains(existingClientAuthorizationPolicy.getName())) {
                removeAuthorizationPolicy(realmName, client, existingClientAuthorizationPolicy);
            }
        }
    }

    private void removeAuthorizationPolicy(
            String realmName,
            ClientRepresentation client,
            PolicyRepresentation existingClientAuthorizationPolicy
    ) {
        logger.debug(
                "Remove authorization policy '{}' for client '{}' in realm '{}'",
                existingClientAuthorizationPolicy.getName(), getClientIdentifier(client), realmName
        );

        try {
            clientRepository.removeAuthorizationPolicy(
                    realmName, client.getId(), existingClientAuthorizationPolicy.getName()
            );
        } catch (NotFoundException ignored) {
            // policies got deleted if linked resources are deleted, too.
        } catch (ServerErrorException e) {
            if (isFgapV2Error(e.getResponse().getStatus())) {
                logger.warn(FGAP_V2_POLICY_WARNING,
                        "remove", existingClientAuthorizationPolicy.getName(), getClientIdentifier(client), getFgapV2Message());
                return;
            }
            throw e;
        }
    }

    private String getClientIdentifier(ClientRepresentation client) {
        return client.getName() != null && !KeycloakUtil.isDefaultClient(client) ? client.getName() : client.getClientId();
    }

    private ResourceServerRepresentation getExistingAuthorization(String realmName, ClientRepresentation client) {
        try {
            return clientRepository.getAuthorizationConfigById(realmName, client.getId());
        } catch (NotFoundException e) {
            logger.debug("No existing authorization settings found for client '{}' in realm '{}' - "
                    + "This is normal for FGAP V2 or clients without authorization",
                    getClientIdentifier(client), realmName);
            return createEmptyAuthorization(client.getId());
        } catch (BadRequestException | ServerErrorException e) {
            int statusCode = e.getResponse().getStatus();
            if (statusCode == 400 || statusCode == HTTP_NOT_FOUND || statusCode == HTTP_NOT_IMPLEMENTED) {
                logger.debug("Cannot retrieve authorization settings for client '{}' in realm '{}' (HTTP {}) - "
                        + "This is expected for FGAP V2 admin-permissions client or clients without authorization support",
                        getClientIdentifier(client), realmName, statusCode);
                return createEmptyAuthorization(client.getId());
            }
            throw e;
        }
    }

    private ResourceServerRepresentation createEmptyAuthorization(String clientId) {
        ResourceServerRepresentation authorization = new ResourceServerRepresentation();
        authorization.setClientId(clientId);
        authorization.setResources(new java.util.ArrayList<>());
        authorization.setPolicies(new java.util.ArrayList<>());
        authorization.setScopes(new java.util.ArrayList<>());
        return authorization;
    }

    private boolean isFgapV2Error(int statusCode) {
        return statusCode == HTTP_NOT_FOUND || statusCode == HTTP_NOT_IMPLEMENTED;
    }

    // https://github.com/adorsys/keycloak-config-cli/issues/589
    private void setAuthorizationResourceOwner(ResourceRepresentation representation) {
        if (representation.getOwner() != null && representation.getOwner().getId() == null && representation.getOwner().getName() != null) {
            representation.getOwner().setId(representation.getOwner().getName());
            representation.getOwner().setName(null);
        }
    }

    private List<ResourceRepresentation> getManagedClientResources(ClientRepresentation client, List<ResourceRepresentation> existingResources) {
        if (importConfigProperties.getRemoteState().isEnabled()) {
            String clientKey = Objects.equals(client.getId(), client.getClientId()) ? "name:" + client.getName() : client.getClientId();
            List<String> clientResourcesInState = stateService.getClientAuthorizationResources(clientKey);
            // ignore all object there are not in state
            return existingResources.stream()
                    .filter(resource -> clientResourcesInState.contains(resource.getName()) || Objects.equals(resource.getName(), "Default Resource"))
                    .toList();
        } else {
            return existingResources;
        }
    }

    /**
     * Helper class that is scoped per realm per import, so it can keep a cache of certain objects
     */
    private class RealmManagementPermissionsResolver {

        private final String realmName;
        private final Map<String, PermissionResolver> resolvers;
        private final boolean isFgapV2;

        public RealmManagementPermissionsResolver(String realmName, boolean isFgapV2) {
            this.isFgapV2 = isFgapV2;
            this.realmName = realmName;
            this.resolvers = new HashMap<>();

            resolvers.put("client", new ClientPermissionResolver(realmName, clientRepository));
            resolvers.put("idp", new IdpPermissionResolver(realmName, identityProviderRepository));
            resolvers.put("role", new RolePermissionResolver(realmName, roleRepository));
            resolvers.put("group", new GroupPermissionResolver(realmName, groupRepository));
        }

        public void createFineGrantedPermissions(ResourceServerRepresentation authorizationSettingsToImport) {
            for (ResourceRepresentation resource : authorizationSettingsToImport.getResources()) {
                PermissionTypeAndId typeAndId = PermissionTypeAndId.fromResourceName(resource.getName());
                if (typeAndId != null) {
                    String id = resolveObjectId(typeAndId, resource.getName());
                    enableFineGrainedPermission(typeAndId.type, id, resource.getName());
                }
            }
        }

        public String resolveObjectId(PermissionTypeAndId typeAndId, String authzName) {
            if (!typeAndId.isPlaceholder()) {
                return typeAndId.idOrPlaceholder;
            }

            PermissionResolver resolver = getPermissionResolver(typeAndId.type, authzName);
            return resolver.resolveObjectId(typeAndId.getPlaceholder(), authzName);
        }

        private void enableFineGrainedPermission(String type, String id, String authzName) {
            PermissionResolver resolver;
            try {
                resolver = getPermissionResolver(type, authzName);
                resolver.enablePermissions(id);
            } catch (ImportProcessingException ex) {
                logger.warn(String.format("Unable to enable permissions for '%s'. Import will continue, but may fail later. Reason: %s",
                        authzName, ex.getMessage()));
            } catch (ServerErrorException ex) {
                if (isFgapV2Error(ex.getResponse().getStatus())) {
                    logger.warn("Unable to enable permissions for '{}' - FGAP V2 active. Permissions managed at realm level.", authzName);
                } else {
                    throw ex;
                }
            } catch (WebApplicationException ex) {
                if (ex.getResponse() != null && isFgapV2Error(ex.getResponse().getStatus())) {
                    logger.warn("Unable to enable permissions for '{}' - FGAP V2 active. Permissions managed at realm level.", authzName);
                } else {
                    throw ex;
                }
            }
        }

        private PermissionResolver getPermissionResolver(String type, String authzName) {
            PermissionResolver resolver = resolvers.get(type);
            if (resolver == null) {
                throw new ImportProcessingException("Cannot resolve '%s' in realm '%s', the type '%s' is not supported by keycloak-config-cli.",
                        authzName, realmName, type);
            }
            return resolver;
        }

        private String getSanitizedAuthzPolicyName(String authzName) {
            PermissionTypeAndId typeAndId = PermissionTypeAndId.fromPolicyName(authzName);
            if (typeAndId == null || !typeAndId.isPlaceholder()) {
                return authzName;
            }
            String id = resolveObjectId(typeAndId, authzName);
            return authzName.replace(typeAndId.idOrPlaceholder, id);
        }

        private String getSanitizedAuthzResourceName(String authzName) {
            PermissionTypeAndId typeAndId = PermissionTypeAndId.fromResourceName(authzName);
            return getSanitizedAuthzName(authzName, typeAndId, true);
        }


        private String getSanitizedAuthzName(String authzName, PermissionTypeAndId typeAndId, boolean isResourceName) {
            if (typeAndId == null || !typeAndId.isPlaceholder()) {
                return authzName;
            }

            String id = resolveObjectId(typeAndId, authzName);

            if (isFgapV2 && isResourceName) {
                return id;
            }

            return authzName.replace(typeAndId.idOrPlaceholder, id);
        }
    }

    private ClientRepresentation getExistingClient(String realmName, ClientRepresentation client) {
        if (client.getClientId() != null) {
            return clientRepository.getByClientId(realmName, client.getClientId());
        } else if (client.getName() != null) {
            return clientRepository.getByName(realmName, client.getName());
        } else {
            throw new ImportProcessingException("clients require client id or name.");
        }
    }
}
