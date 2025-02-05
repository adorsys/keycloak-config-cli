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

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.repository.ClientRepository;
import de.adorsys.keycloak.config.repository.GroupRepository;
import de.adorsys.keycloak.config.repository.IdentityProviderRepository;
import de.adorsys.keycloak.config.repository.RoleRepository;
import de.adorsys.keycloak.config.service.clientauthorization.ClientPermissionResolver;
import de.adorsys.keycloak.config.service.clientauthorization.GroupPermissionResolver;
import de.adorsys.keycloak.config.service.clientauthorization.IdpPermissionResolver;
import de.adorsys.keycloak.config.service.clientauthorization.PermissionResolver;
import de.adorsys.keycloak.config.service.clientauthorization.PermissionTypeAndId;
import de.adorsys.keycloak.config.service.clientauthorization.RolePermissionResolver;
import de.adorsys.keycloak.config.service.state.StateService;
import de.adorsys.keycloak.config.util.CloneUtil;
import de.adorsys.keycloak.config.util.JsonUtil;
import de.adorsys.keycloak.config.util.KeycloakUtil;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.ws.rs.NotFoundException;

import static de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues.FULL;
import static java.lang.Boolean.TRUE;

@Service
@SuppressWarnings({"java:S1192"})
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class ClientAuthorizationImportService {
    private static final Logger logger = LoggerFactory.getLogger(ClientAuthorizationImportService.class);

    public static final String REALM_MANAGEMENT_CLIENT_ID = "realm-management";

    private final ClientRepository clientRepository;
    private final IdentityProviderRepository identityProviderRepository;
    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;
    private final ImportConfigProperties importConfigProperties;
    private final StateService stateService;

    @Autowired
    public ClientAuthorizationImportService(
            ClientRepository clientRepository,
            IdentityProviderRepository identityProviderRepository,
            RoleRepository roleRepository,
            GroupRepository groupRepository,
            ImportConfigProperties importConfigProperties,
            StateService stateService
    ) {
        this.clientRepository = clientRepository;
        this.identityProviderRepository = identityProviderRepository;
        this.roleRepository = roleRepository;
        this.groupRepository = groupRepository;
        this.importConfigProperties = importConfigProperties;
        this.stateService = stateService;
    }

    public void doImport(RealmImport realmImport) {
        List<ClientRepresentation> clients = realmImport.getClients();
        if (clients == null) {
            return;
        }

        updateClientAuthorizationSettings(realmImport, clients);
    }

    private void updateClientAuthorizationSettings(
            RealmImport realmImport,
            List<ClientRepresentation> clients
    ) {
        String realmName = realmImport.getRealm();

        List<ClientRepresentation> clientsWithAuthorization = clients.stream()
                .filter(client -> client.getAuthorizationSettings() != null)
                .toList();

        for (ClientRepresentation client : clientsWithAuthorization) {
            ClientRepresentation existingClient;
            if (client.getClientId() != null) {
                existingClient = clientRepository.getByClientId(realmName, client.getClientId());
            } else if (client.getName() != null) {
                existingClient = clientRepository.getByName(realmName, client.getName());
            } else {
                throw new ImportProcessingException("clients require client id or name.");
            }

            updateAuthorization(realmName, existingClient, client.getAuthorizationSettings());
        }
    }

    private void updateAuthorization(
            String realmName,
            ClientRepresentation client,
            ResourceServerRepresentation authorizationSettingsToImport
    ) {
        if (importConfigProperties.isValidate() && !REALM_MANAGEMENT_CLIENT_ID.equals(client.getClientId())
                && (TRUE.equals(client.isBearerOnly()) || TRUE.equals(client.isPublicClient()))) {
            throw new ImportProcessingException(
                    "Unsupported authorization settings for client '%s' in realm '%s': client must be confidential.",
                    getClientIdentifier(client), realmName
            );
        }

        RealmManagementPermissionsResolver realmManagementPermissionsResolver = new RealmManagementPermissionsResolver(realmName);
        if (REALM_MANAGEMENT_CLIENT_ID.equals(client.getClientId())) {
            realmManagementPermissionsResolver.createFineGrantedPermissions(authorizationSettingsToImport);
        }

        ResourceServerRepresentation existingAuthorization = clientRepository.getAuthorizationConfigById(
                realmName, client.getId()
        );

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
        existingAuthorization = clientRepository.getAuthorizationConfigById(
                realmName, client.getId()
        );

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

        if (policy.getConfig().containsKey("resources") && policy.getConfig().get("resources").contains(".$")) {
            String resources = sanitizeAuthorizationPolicyResource(policy.getConfig().get("resources"), realmManagementPermissionsResolver);
            policy.getConfig().put("resources", resources);
        }

        return policy;
    }

    private String sanitizeAuthorizationPolicyResource(String resources,
                                                       RealmManagementPermissionsResolver realmManagementPermissionsResolver) {
        List<String> resourcesList = JsonUtil.fromJson(resources);
        resourcesList = resourcesList.stream()
                .map(realmManagementPermissionsResolver::getSanitizedAuthzResourceName)
                .toList();

        resources = JsonUtil.toJson(resourcesList);
        return resources;
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
        clientRepository.updateAuthorizationSettings(realmName, client.getId(), patchedAuthorizationSettings);
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

        clientRepository.createAuthorizationResource(realmName, client.getId(), authorizationResourceToImport);
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

        clientRepository.updateAuthorizationResource(realmName, client.getId(), authorizationResourceToImport);
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
        clientRepository.removeAuthorizationResource(
                realmName, client.getId(), existingClientAuthorizationResource.getName()
        );
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
            clientRepository.addAuthorizationScope(
                    realmName, client.getId(), authorizationScopeToImport
            );
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

            clientRepository.updateAuthorizationScope(realmName, client.getId(), authorizationScopeToImport);
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

        clientRepository.removeAuthorizationScope(realmName, client.getId(), existingClientAuthorizationScope.getName());
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

            clientRepository.createAuthorizationPolicy(
                    realmName, client.getId(), authorizationPolicyToImport
            );
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
            clientRepository.updateAuthorizationPolicy(realmName, client.getId(), authorizationPolicyToImport);
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
        }
    }

    private String getClientIdentifier(ClientRepresentation client) {
        return client.getName() != null && !KeycloakUtil.isDefaultClient(client) ? client.getName() : client.getClientId();
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

        public RealmManagementPermissionsResolver(String realmName) {
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
            return getSanitizedAuthzName(authzName, typeAndId);
        }

        private String getSanitizedAuthzResourceName(String authzName) {
            PermissionTypeAndId typeAndId = PermissionTypeAndId.fromResourceName(authzName);
            return getSanitizedAuthzName(authzName, typeAndId);
        }


        private String getSanitizedAuthzName(String authzName, PermissionTypeAndId typeAndId) {
            if (typeAndId == null || !typeAndId.isPlaceholder()) {
                return authzName;
            }

            String id = resolveObjectId(typeAndId, authzName);
            return authzName.replace(typeAndId.idOrPlaceholder, id);
        }
    }
}
