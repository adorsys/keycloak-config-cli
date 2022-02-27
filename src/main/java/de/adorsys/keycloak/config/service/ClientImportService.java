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
import de.adorsys.keycloak.config.repository.AuthenticationFlowRepository;
import de.adorsys.keycloak.config.repository.ClientRepository;
import de.adorsys.keycloak.config.repository.ClientScopeRepository;
import de.adorsys.keycloak.config.service.state.StateService;
import de.adorsys.keycloak.config.util.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import static de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues.FULL;
import static java.lang.Boolean.TRUE;

@Service
@SuppressWarnings({"java:S1192"})
public class ClientImportService {
    private static final String[] propertiesWithDependencies = new String[]{
            "authenticationFlowBindingOverrides",
            "authorizationSettings",
    };

    public static final String REALM_MANAGEMENT_CLIENT_ID = "realm-management";

    private static final Logger logger = LoggerFactory.getLogger(ClientImportService.class);

    private final ClientRepository clientRepository;
    private final ClientScopeRepository clientScopeRepository;
    private final AuthenticationFlowRepository authenticationFlowRepository;
    private final ImportConfigProperties importConfigProperties;
    private final StateService stateService;

    @Autowired
    public ClientImportService(
            ClientRepository clientRepository,
            ClientScopeRepository clientScopeRepository,
            AuthenticationFlowRepository authenticationFlowRepository,
            ImportConfigProperties importConfigProperties,
            StateService stateService) {
        this.clientRepository = clientRepository;
        this.clientScopeRepository = clientScopeRepository;
        this.authenticationFlowRepository = authenticationFlowRepository;
        this.importConfigProperties = importConfigProperties;
        this.stateService = stateService;
    }

    public void doImport(RealmImport realmImport) {
        List<ClientRepresentation> clients = realmImport.getClients();
        if (clients == null) {
            return;
        }

        if (importConfigProperties.getManaged().getClient() == FULL) {
            deleteClientsMissingInImport(realmImport, clients);
        }
        createOrUpdateClients(realmImport, clients);
    }

    public void doImportDependencies(RealmImport realmImport) {
        List<ClientRepresentation> clients = realmImport.getClients();
        if (clients == null) {
            return;
        }

        updateClientAuthorizationSettings(realmImport, clients);
        updateClientAuthenticationFlowBindingOverrides(realmImport, clients);
    }

    private void createOrUpdateClients(
            RealmImport realmImport,
            List<ClientRepresentation> clients
    ) {
        Consumer<ClientRepresentation> loop = client -> createOrUpdateClient(realmImport, client);
        if (importConfigProperties.isParallel()) {
            clients.parallelStream().forEach(loop);
        } else {
            clients.forEach(loop);
        }
    }

    private void deleteClientsMissingInImport(
            RealmImport realmImport,
            List<ClientRepresentation> clients
    ) {
        Set<String> importedClients = clients.stream()
                .map(ClientRepresentation::getClientId)
                .collect(Collectors.toSet());

        boolean isState = importConfigProperties.isState();
        final List<String> stateClients = stateService.getClients();

        List<ClientRepresentation> clientsToRemove = clientRepository.getAll(realmImport.getRealm())
                .stream()
                .filter(client -> !KeycloakUtil.isDefaultClient(client)
                        && !importedClients.contains(client.getClientId())
                        && (!isState || stateClients.contains(client.getClientId()))
                        && !(Objects.equals(realmImport.getRealm(), "master")
                        && client.getClientId().endsWith("-realm"))
                )
                .collect(Collectors.toList());

        for (ClientRepresentation clientToRemove : clientsToRemove) {
            logger.debug("Remove client '{}' in realm '{}'", clientToRemove.getClientId(), realmImport.getRealm());
            clientRepository.remove(realmImport.getRealm(), clientToRemove);
        }
    }

    private void createOrUpdateClient(
            RealmImport realmImport,
            ClientRepresentation client
    ) {
        String realmName = realmImport.getRealm();

        // https://github.com/keycloak/keycloak/blob/74695c02423345dab892a0808bf9203c3f92af7c/server-spi-private/src/main/java/org/keycloak/models/utils/RepresentationToModel.java#L2878-L2881
        if (importConfigProperties.isValidate()
                && client.getAuthorizationSettings() != null && !REALM_MANAGEMENT_CLIENT_ID.equals(client.getClientId())) {
            if (TRUE.equals(client.isBearerOnly()) || TRUE.equals(client.isPublicClient())) {
                throw new ImportProcessingException(String.format(
                        "Unsupported authorization settings for client '%s' in realm '%s': "
                                + "client must be confidential.",
                        getClientIdentifier(client), realmName
                ));
            }

            if (!TRUE.equals(client.isServiceAccountsEnabled())) {
                throw new ImportProcessingException(String.format(
                        "Unsupported authorization settings for client '%s' in realm '%s': "
                                + "serviceAccountsEnabled must be 'true'.",
                        getClientIdentifier(client), realmName
                ));
            }
        }

        Optional<ClientRepresentation> existingClient;
        if (client.getClientId() != null) {
            existingClient = clientRepository.searchByClientId(realmName, client.getClientId());
        } else if (client.getName() != null) {
            existingClient = clientRepository.searchByName(realmName, client.getName());
        } else {
            throw new ImportProcessingException("clients require client id or name.");
        }

        if (existingClient.isPresent()) {
            updateClientIfNeeded(realmName, client, existingClient.get());
        } else {
            logger.debug("Create client '{}' in realm '{}'", getClientIdentifier(client), realmName);
            createClient(realmName, client);
        }
    }

    private void updateClientIfNeeded(
            String realmName,
            ClientRepresentation clientToUpdate,
            ClientRepresentation existingClient
    ) {
        String[] propertiesToIgnore = ArrayUtils.addAll(propertiesWithDependencies, "id", "access");
        ClientRepresentation mergedClient = CloneUtil.patch(existingClient, clientToUpdate, propertiesToIgnore);

        if (!isClientEqual(realmName, existingClient, mergedClient)) {
            logger.debug("Update client '{}' in realm '{}'", getClientIdentifier(clientToUpdate), realmName);
            updateClient(realmName, mergedClient);
            updateClientDefaultOptionalClientScopes(realmName, mergedClient, existingClient);
        } else {
            logger.debug("No need to update client '{}' in realm '{}'", getClientIdentifier(clientToUpdate), realmName);
        }
    }

    private void createClient(String realmName, ClientRepresentation client) {
        ClientRepresentation clientToImport = CloneUtil.deepClone(
                client, ClientRepresentation.class, propertiesWithDependencies
        );
        clientRepository.create(realmName, clientToImport);
    }

    private boolean isClientEqual(
            String realmName,
            ClientRepresentation existingClient,
            ClientRepresentation patchedClient
    ) {
        String[] propertiesToIgnore = ArrayUtils.addAll(
                propertiesWithDependencies, "id", "secret", "access", "protocolMappers"
        );

        if (!CloneUtil.deepEquals(existingClient, patchedClient, propertiesToIgnore)) {
            return false;
        }

        boolean areProtocolMapperDifferent = !ProtocolMapperUtil.areProtocolMappersEqual(
                patchedClient.getProtocolMappers(),
                existingClient.getProtocolMappers()
        );

        if (areProtocolMapperDifferent) {
            return false;
        }

        String patchedClientSecret = patchedClient.getSecret();
        if (patchedClientSecret == null) {
            return true;
        }

        String clientSecret = clientRepository.getClientSecret(realmName, patchedClient.getClientId());
        return Objects.equals(clientSecret, patchedClientSecret);
    }

    private void updateClient(
            String realmName,
            ClientRepresentation patchedClient
    ) {
        try {
            clientRepository.update(realmName, patchedClient);
        } catch (WebApplicationException error) {
            String errorMessage = ResponseUtil.getErrorMessage(error);
            throw new ImportProcessingException(
                    String.format("Cannot update client '%s' in realm '%s': %s",
                            getClientIdentifier(patchedClient), realmName, errorMessage
                    ),
                    error
            );
        }
    }

    private void updateClientAuthorizationSettings(
            RealmImport realmImport,
            List<ClientRepresentation> clients
    ) {
        String realmName = realmImport.getRealm();

        List<ClientRepresentation> clientsWithAuthorization = clients.stream()
                .filter(client -> client.getAuthorizationSettings() != null)
                .collect(Collectors.toList());

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
            throw new ImportProcessingException(String.format(
                    "Unsupported authorization settings for client '%s' in realm '%s': "
                            + "client must be confidential.",
                    getClientIdentifier(client), realmName
            ));
        }

        if (REALM_MANAGEMENT_CLIENT_ID.equals(client.getClientId())) {
            createFineGrantedPermissions(realmName, authorizationSettingsToImport);
        }

        ResourceServerRepresentation existingAuthorization = clientRepository.getAuthorizationConfigById(
                realmName, client.getId()
        );

        handleAuthorizationSettings(realmName, client, existingAuthorization, authorizationSettingsToImport);

        List<ResourceRepresentation> sanitizedAuthorizationResources = sanitizeAuthorizationResources(realmName, authorizationSettingsToImport);
        List<PolicyRepresentation> sanitizedAuthorizationPolicies = sanitizeAuthorizationPolicies(realmName, authorizationSettingsToImport);

        createOrUpdateAuthorizationResources(realmName, client, existingAuthorization.getResources(), sanitizedAuthorizationResources);
        createOrUpdateAuthorizationScopes(realmName, client, existingAuthorization.getScopes(), authorizationSettingsToImport.getScopes());
        createOrUpdateAuthorizationPolicies(realmName, client, existingAuthorization.getPolicies(), sanitizedAuthorizationPolicies);

        if (importConfigProperties.getManaged().getClientAuthorizationResources() == FULL) {
            removeAuthorizationResources(realmName, client, existingAuthorization.getResources(), sanitizedAuthorizationResources);
        }

        removeAuthorizationPolicies(realmName, client, existingAuthorization.getPolicies(), sanitizedAuthorizationPolicies);
        removeAuthorizationScopes(realmName, client, existingAuthorization.getScopes(), authorizationSettingsToImport.getScopes());
    }

    private List<ResourceRepresentation> sanitizeAuthorizationResources(String realmName, ResourceServerRepresentation authorizationSettings) {
        return authorizationSettings.getResources()
                .stream()
                .map(resource -> sanitizeAuthorizationResource(realmName, resource))
                .collect(Collectors.toList());
    }

    private List<PolicyRepresentation> sanitizeAuthorizationPolicies(String realmName, ResourceServerRepresentation authorizationSettings) {
        return authorizationSettings.getPolicies()
                .stream()
                .map(policy -> sanitizeAuthorizationPolicy(realmName, policy))
                .collect(Collectors.toList());
    }

    private ResourceRepresentation sanitizeAuthorizationResource(String realmName, ResourceRepresentation resource) {
        if ("Client".equals(resource.getType())) {
            resource.setName(getSanitizedAuthzName(realmName, resource.getName()));
        }

        return resource;
    }

    private PolicyRepresentation sanitizeAuthorizationPolicy(String realmName, PolicyRepresentation policy) {
        policy.setName(getSanitizedAuthzName(realmName, policy.getName()));

        if (policy.getConfig().containsKey("resources") && policy.getConfig().get("resources").contains(".$")) {
            String resources = sanitizeAuthorizationPolicyResource(realmName, policy.getConfig().get("resources"));
            policy.getConfig().put("resources", resources);
        }

        return policy;
    }

    private String sanitizeAuthorizationPolicyResource(String realmName, String resources) {
        List<String> resourcesList = JsonUtil.fromJson(resources);
        resourcesList = resourcesList.stream()
                .map(resource -> getSanitizedAuthzName(realmName, resource))
                .collect(Collectors.toList());

        resources = JsonUtil.toJson(resourcesList);
        return resources;
    }

    private void createFineGrantedPermissions(String realmName, ResourceServerRepresentation authorizationSettingsToImport) {
        authorizationSettingsToImport.getResources()
                .stream()
                .filter(resource -> "Client".equals(resource.getType()) && resource.getName().contains("client.resource."))
                .forEach(resource -> {
                    String id = getClientIdFromName(realmName, resource.getName())
                            .replace("client.resource.", "");
                    try {
                        if (!clientRepository.isPermissionEnabled(realmName, id)) {
                            logger.debug("Enable permissions for client '{}' in realm '{}'", id, realmName);
                            clientRepository.enablePermission(realmName, id);
                        }
                    } catch (NotFoundException e) {
                        throw new ImportProcessingException("Cannot find client '%s' in realm '%s'", id, realmName);
                    }
                });
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

        if (existingClientAuthorizationResource.getAttributes().isEmpty() && authorizationResourceToImport.getAttributes() == null) {
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
                .collect(Collectors.toList());

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
                realmName, client.getId(), existingClientAuthorizationResource.getId()
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
        String authorizationScopeNameToImport = authorizationScopeToImport.getName();
        if (!existingClientAuthorizationScopesMap.containsKey(authorizationScopeToImport.getName())) {
            logger.debug("Add authorization scope '{}' for client '{}' in realm '{}'",
                    authorizationScopeNameToImport, getClientIdentifier(client), realmName
            );
            clientRepository.addAuthorizationScope(
                    realmName, client.getId(), authorizationScopeNameToImport
            );
        } else {
            updateAuthorizationScope(
                    realmName, client, existingClientAuthorizationScopesMap,
                    authorizationScopeToImport, authorizationScopeNameToImport
            );
        }
    }

    private void updateAuthorizationScope(
            String realmName,
            ClientRepresentation client,
            Map<String, ScopeRepresentation> existingClientAuthorizationScopesMap,
            ScopeRepresentation authorizationScopeToImport,
            String authorizationScopeNameToImport
    ) {
        ScopeRepresentation existingClientAuthorizationScope = existingClientAuthorizationScopesMap
                .get(authorizationScopeNameToImport);

        if (!CloneUtil.deepEquals(authorizationScopeToImport, existingClientAuthorizationScope, "id")) {
            authorizationScopeToImport.setId(existingClientAuthorizationScope.getId());
            logger.debug("Update authorization scope '{}' for client '{}' in realm '{}'",
                    authorizationScopeNameToImport, getClientIdentifier(client), realmName);

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
                .collect(Collectors.toList());

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

        clientRepository.removeAuthorizationScope(realmName, client.getId(), existingClientAuthorizationScope.getId());
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
                .collect(Collectors.toList());

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
                    realmName, client.getId(), existingClientAuthorizationPolicy.getId()
            );
        } catch (NotFoundException ignored) {
            // policies got deleted if linked resources are deleted, too.
        }
    }

    private void updateClientAuthenticationFlowBindingOverrides(
            RealmImport realmImport,
            List<ClientRepresentation> clients
    ) {
        String realmName = realmImport.getRealm();

        for (ClientRepresentation client : clients) {
            ClientRepresentation existingClient;
            if (client.getClientId() != null) {
                existingClient = clientRepository.getByClientId(realmName, client.getClientId());
            } else if (client.getName() != null) {
                existingClient = clientRepository.getByName(realmName, client.getName());
            } else {
                throw new ImportProcessingException("clients require client id or name.");
            }

            updateAuthenticationFlowBindingOverrides(
                    realmName, existingClient, client.getAuthenticationFlowBindingOverrides()
            );
        }
    }

    private void updateAuthenticationFlowBindingOverrides(
            String realmName,
            ClientRepresentation existingClient,
            Map<String, String> authenticationFlowBindingOverrides
    ) {
        boolean isEqual = Objects.equals(
                authenticationFlowBindingOverrides, existingClient.getAuthenticationFlowBindingOverrides()
        );

        if (isEqual) return;

        Map<String, String> authFlowUpdates = new HashMap<>(existingClient.getAuthenticationFlowBindingOverrides());

        // Be sure that all existing values will be cleared
        // See: https://github.com/keycloak/keycloak/blob/790b549cf99dbbba109e145654ee4a4cd1a047c9/server-spi-private/src/main/java/org/keycloak/models/utils/RepresentationToModel.java#L1516
        authFlowUpdates.replaceAll((k, v) -> v = null);

        // Compute new values
        if (authenticationFlowBindingOverrides != null) {
            for (Map.Entry<String, String> override : authenticationFlowBindingOverrides.entrySet()) {
                if (
                        override.getValue() == null || override.getValue().isEmpty()
                                || authenticationFlowRepository.exists(realmName, override.getValue())
                ) {
                    authFlowUpdates.put(override.getKey(), override.getValue());
                } else {
                    String flowId = authenticationFlowRepository.getByAlias(realmName, override.getValue()).getId();
                    authFlowUpdates.put(override.getKey(), flowId);
                }
            }
        }

        existingClient.setAuthenticationFlowBindingOverrides(authFlowUpdates);
        updateClient(realmName, existingClient);
    }

    private void updateClientDefaultOptionalClientScopes(
            String realmName,
            ClientRepresentation client,
            ClientRepresentation existingClient
    ) {
        final List<String> defaultClientScopeNamesToAdd = ClientScopeUtil
                .estimateClientScopesToAdd(client.getDefaultClientScopes(), existingClient.getDefaultClientScopes());
        final List<String> defaultClientScopeNamesToRemove = ClientScopeUtil
                .estimateClientScopesToRemove(client.getDefaultClientScopes(), existingClient.getDefaultClientScopes());


        final List<String> optionalClientScopeNamesToAdd = ClientScopeUtil
                .estimateClientScopesToAdd(client.getOptionalClientScopes(), existingClient.getOptionalClientScopes());
        final List<String> optionalClientScopeNamesToRemove = ClientScopeUtil
                .estimateClientScopesToRemove(client.getOptionalClientScopes(), existingClient.getOptionalClientScopes());

        if (!defaultClientScopeNamesToRemove.isEmpty()) {
            logger.debug("Remove default client scopes '{}' for client '{}' in realm '{}'",
                    defaultClientScopeNamesToRemove, client.getClientId(), realmName);

            List<ClientScopeRepresentation> defaultClientScopesToRemove = clientScopeRepository
                    .getListByNames(realmName, defaultClientScopeNamesToRemove);

            clientRepository.removeDefaultClientScopes(realmName, client.getClientId(), defaultClientScopesToRemove);
        }

        if (!optionalClientScopeNamesToRemove.isEmpty()) {
            logger.debug("Remove optional client scopes '{}' for client '{}' in realm '{}'",
                    optionalClientScopeNamesToRemove, client.getClientId(), realmName);

            List<ClientScopeRepresentation> optionalClientScopesToRemove = clientScopeRepository
                    .getListByNames(realmName, optionalClientScopeNamesToRemove);

            clientRepository.removeOptionalClientScopes(realmName, client.getClientId(), optionalClientScopesToRemove);
        }


        if (!defaultClientScopeNamesToAdd.isEmpty()) {
            logger.debug("Add default client scopes '{}' for client '{}' in realm '{}'",
                    defaultClientScopeNamesToAdd, client.getClientId(), realmName);

            List<ClientScopeRepresentation> defaultClientScopesToAdd = clientScopeRepository
                    .getListByNames(realmName, defaultClientScopeNamesToAdd);

            clientRepository.addDefaultClientScopes(realmName, client.getClientId(), defaultClientScopesToAdd);
        }

        if (!optionalClientScopeNamesToAdd.isEmpty()) {
            logger.debug("Add optional client scopes '{}' for client '{}' in realm '{}'",
                    optionalClientScopeNamesToAdd, client.getClientId(), realmName);

            List<ClientScopeRepresentation> optionalClientScopesToAdd = clientScopeRepository
                    .getListByNames(realmName, optionalClientScopeNamesToAdd);

            clientRepository.addOptionalClientScopes(realmName, client.getClientId(), optionalClientScopesToAdd);
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
        if (importConfigProperties.isState()) {
            String clientKey = Objects.equals(client.getId(), client.getClientId()) ? "name:" + client.getName() : client.getClientId();
            List<String> clientResourcesInState = stateService.getClientAuthorizationResources(clientKey);
            // ignore all object there are not in state
            return existingResources.stream()
                    .filter(resource -> clientResourcesInState.contains(resource.getName()) || Objects.equals(resource.getName(), "Default Resource"))
                    .collect(Collectors.toList());
        } else {
            return existingResources;
        }
    }

    private String getSanitizedAuthzName(String realmName, String name) {
        String client = StringUtils.substringAfterLast(name, ".");

        if (!client.startsWith("$")) {
            return name;
        }

        String id = getClientIdFromName(realmName, name);
        return name.replace(client, id);
    }

    private String getClientIdFromName(String realmName, String name) {
        String client = StringUtils.substringAfterLast(name, ".");

        if (!client.startsWith("$")) {
            return name;
        }

        try {
            return clientRepository.getByClientId(realmName, client.substring(1)).getId();
        } catch (NotFoundException e) {
            throw new ImportProcessingException("Cannot find client '%s' in realm '%s'", client.substring(1), realmName);
        }
    }
}
