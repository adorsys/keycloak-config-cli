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

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.repository.ClientRepository;
import de.adorsys.keycloak.config.util.CloneUtil;
import de.adorsys.keycloak.config.util.ProtocolMapperUtil;
import de.adorsys.keycloak.config.util.ResponseUtil;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

@Service
public class ClientImportService {
    private static final Logger logger = LoggerFactory.getLogger(ClientImportService.class);

    private final ClientRepository clientRepository;
    private final ImportConfigProperties importConfigProperties;

    @Autowired
    public ClientImportService(
            ClientRepository clientRepository,
            ImportConfigProperties importConfigProperties) {
        this.clientRepository = clientRepository;
        this.importConfigProperties = importConfigProperties;
    }

    public void doImport(RealmImport realmImport) {
        List<ClientRepresentation> clients = realmImport.getClients();
        if (clients == null) {
            return;
        }

        createOrUpdateClients(realmImport, clients);
    }

    public void importAuthorizationSettings(RealmImport realmImport) {
        List<ClientRepresentation> clients = realmImport.getClients();
        if (clients == null) {
            return;
        }

        List<ClientRepresentation> clientsWithAuthorization = clients.stream()
                .filter(client -> client.getAuthorizationSettings() != null)
                .collect(Collectors.toList());

        updateClientAuthorizationSettings(realmImport, clientsWithAuthorization);
    }

    private void createOrUpdateClients(RealmImport realmImport, List<ClientRepresentation> clients) {
        Consumer<ClientRepresentation> loop = client -> createOrUpdateClient(realmImport, client);
        if (importConfigProperties.isParallel()) {
            clients.parallelStream().forEach(loop);
        } else {
            clients.forEach(loop);
        }
    }

    private void createOrUpdateClient(RealmImport realmImport, ClientRepresentation client) {
        String clientId = client.getClientId();
        String realm = realmImport.getRealm();

        Optional<ClientRepresentation> maybeClient = clientRepository.tryToFindClient(realm, clientId);

        if (maybeClient.isPresent()) {
            updateClientIfNeeded(realm, client, maybeClient.get());
        } else {
            logger.debug("Create client '{}' in realm '{}'", clientId, realm);
            createClient(realm, client);
        }
    }

    private void updateClientIfNeeded(String realm, ClientRepresentation clientToUpdate, ClientRepresentation existingClient) {
        ClientRepresentation patchedClient = CloneUtil.patch(existingClient, clientToUpdate, "id", "access", "authorizationSettings");

        if (!isClientEqual(realm, existingClient, patchedClient)) {
            logger.debug("Update client '{}' in realm '{}'", clientToUpdate.getClientId(), realm);
            updateClient(realm, patchedClient);
        } else {
            logger.debug("No need to update client '{}' in realm '{}'", clientToUpdate.getClientId(), realm);
        }
    }

    private void createClient(String realm, ClientRepresentation client) {
        ClientRepresentation clientToImport = CloneUtil.deepClone(client, ClientRepresentation.class, "authorizationSettings");
        clientRepository.create(realm, clientToImport);
    }

    private boolean isClientEqual(String realm, ClientRepresentation existingClient, ClientRepresentation patchedClient) {
        if (!CloneUtil.deepEquals(existingClient, patchedClient, "id", "secret", "access", "authorizationSettings", "protocolMappers")) {
            return false;
        }

        if (!ProtocolMapperUtil.areProtocolMappersEqual(patchedClient.getProtocolMappers(), existingClient.getProtocolMappers())) {
            return false;
        }

        String patchedClientSecret = patchedClient.getSecret();
        if (patchedClientSecret == null) {
            return true;
        }

        String clientSecret = clientRepository.getClientSecret(realm, patchedClient.getClientId());
        return clientSecret.equals(patchedClientSecret);
    }

    private void updateClient(String realm, ClientRepresentation patchedClient) {
        try {
            clientRepository.update(realm, patchedClient);
        } catch (WebApplicationException error) {
            String errorMessage = ResponseUtil.getErrorMessage(error);
            throw new ImportProcessingException("Cannot update client '" + patchedClient.getClientId() + "' for realm '" + realm + "': " + errorMessage, error);
        }

        List<ProtocolMapperRepresentation> protocolMappers = patchedClient.getProtocolMappers();

        if (protocolMappers != null) {
            updateProtocolMappers(realm, patchedClient.getId(), protocolMappers);
        }
    }

    private void updateProtocolMappers(String realm, String clientId, List<ProtocolMapperRepresentation> protocolMappers) {
        ClientRepresentation existingClient = clientRepository.getClientById(realm, clientId);

        List<ProtocolMapperRepresentation> existingProtocolMappers = existingClient.getProtocolMappers();

        List<ProtocolMapperRepresentation> protocolMappersToAdd = ProtocolMapperUtil.estimateProtocolMappersToAdd(protocolMappers, existingProtocolMappers);
        List<ProtocolMapperRepresentation> protocolMappersToRemove = ProtocolMapperUtil.estimateProtocolMappersToRemove(protocolMappers, existingProtocolMappers);
        List<ProtocolMapperRepresentation> protocolMappersToUpdate = ProtocolMapperUtil.estimateProtocolMappersToUpdate(protocolMappers, existingProtocolMappers);

        clientRepository.addProtocolMappers(realm, clientId, protocolMappersToAdd);
        clientRepository.removeProtocolMappers(realm, clientId, protocolMappersToRemove);
        clientRepository.updateProtocolMappers(realm, clientId, protocolMappersToUpdate);
    }

    private void updateClientAuthorizationSettings(RealmImport realmImport, List<ClientRepresentation> clients) {
        String realm = realmImport.getRealm();

        for (ClientRepresentation client : clients) {
            ClientRepresentation existingClient = clientRepository.getClientByClientId(realm, client.getClientId());
            updateAuthorization(realm, existingClient, client.getAuthorizationSettings());
        }
    }

    private void updateAuthorization(String realm, ClientRepresentation client, ResourceServerRepresentation authorizationSettingsToImport) {
        ResourceServerRepresentation existingAuthorization = clientRepository.getAuthorizationConfigById(realm, client.getId());

        handleAuthorizationSettings(realm, client, existingAuthorization, authorizationSettingsToImport);

        createOrUpdateAuthorizationResources(realm, client,
                existingAuthorization.getResources(), authorizationSettingsToImport.getResources());
        removeAuthorizationResources(realm, client,
                existingAuthorization.getResources(), authorizationSettingsToImport.getResources());

        createOrUpdateAuthorizationScopes(realm, client,
                existingAuthorization.getScopes(), authorizationSettingsToImport.getScopes());
        removeAuthorizationScopes(realm, client,
                existingAuthorization.getScopes(), authorizationSettingsToImport.getScopes());

        createOrUpdateAuthorizationPolicies(realm, client,
                existingAuthorization.getPolicies(), authorizationSettingsToImport.getPolicies());
        removeAuthorizationPolicies(realm, client,
                existingAuthorization.getPolicies(), authorizationSettingsToImport.getPolicies());
    }

    private void handleAuthorizationSettings(String realm, ClientRepresentation client, ResourceServerRepresentation existingClientAuthorizationResources, ResourceServerRepresentation authorizationResourcesToImport) {
        if (!CloneUtil.deepEquals(authorizationResourcesToImport, existingClientAuthorizationResources, "policies", "resources", "permissions", "scopes")) {
            ResourceServerRepresentation patchedAuthorizationSettings = CloneUtil
                    .deepPatch(existingClientAuthorizationResources, authorizationResourcesToImport);
            logger.debug("Update authorization settings for client '{}' in realm '{}'", client.getClientId(), realm);
            clientRepository.updateAuthorizationSettings(realm, client.getId(), patchedAuthorizationSettings);
        }
    }

    private void createOrUpdateAuthorizationResources(String realm, ClientRepresentation client, List<ResourceRepresentation> existingClientAuthorizationResources, List<ResourceRepresentation> authorizationResourcesToImport) {
        Map<String, ResourceRepresentation> existingClientAuthorizationResourcesMap = existingClientAuthorizationResources
                .stream()
                .collect(Collectors.toMap(ResourceRepresentation::getName, resource -> resource));

        for (ResourceRepresentation authorizationResourceToImport : authorizationResourcesToImport) {
            createOrUpdateAuthorizationResource(realm, client, existingClientAuthorizationResourcesMap, authorizationResourceToImport);
        }
    }

    private void createOrUpdateAuthorizationResource(String realm, ClientRepresentation client, Map<String, ResourceRepresentation> existingClientAuthorizationResourcesMap, ResourceRepresentation authorizationResourceToImport) {
        if (!existingClientAuthorizationResourcesMap.containsKey(authorizationResourceToImport.getName())) {
            logger.debug("Create authorization resource '{}' for client '{}' in realm '{}'", authorizationResourceToImport.getName(), client.getClientId(), realm);
            clientRepository.createAuthorizationResource(realm, client.getId(), authorizationResourceToImport);
        } else {
            updateAuthorizationResource(realm, client, existingClientAuthorizationResourcesMap, authorizationResourceToImport);
        }
    }

    private void updateAuthorizationResource(String realm, ClientRepresentation client, Map<String, ResourceRepresentation> existingClientAuthorizationResourcesMap, ResourceRepresentation authorizationResourceToImport) {
        ResourceRepresentation existingClientAuthorizationResource = existingClientAuthorizationResourcesMap
                .get(authorizationResourceToImport.getName());

        if (!CloneUtil.deepEquals(authorizationResourceToImport, existingClientAuthorizationResource, "id", "_id")) {
            authorizationResourceToImport.setId(existingClientAuthorizationResource.getId());
            logger.debug("Update authorization resource '{}' for client '{}' in realm '{}'", authorizationResourceToImport.getName(), client.getClientId(), realm);
            clientRepository.updateAuthorizationResource(realm, client.getId(), authorizationResourceToImport);
        }
    }

    private void removeAuthorizationResources(String realm, ClientRepresentation client, List<ResourceRepresentation> existingClientAuthorizationResources, List<ResourceRepresentation> authorizationResourcesToImport) {
        List<String> authorizationResourceNamesToImport = authorizationResourcesToImport
                .stream().map(ResourceRepresentation::getName)
                .collect(Collectors.toList());

        for (ResourceRepresentation existingClientAuthorizationResource : existingClientAuthorizationResources) {
            if (!authorizationResourceNamesToImport.contains(existingClientAuthorizationResource.getName())) {
                removeAuthorizationResource(realm, client, existingClientAuthorizationResource);
            }
        }
    }

    private void removeAuthorizationResource(String realm, ClientRepresentation client, ResourceRepresentation existingClientAuthorizationResource) {
        logger.debug("Remove authorization resource '{}' for client '{}' in realm '{}'", existingClientAuthorizationResource.getName(), client.getClientId(), realm);
        clientRepository.removeAuthorizationResource(realm, client.getId(), existingClientAuthorizationResource.getId());
    }

    private void createOrUpdateAuthorizationScopes(String realm, ClientRepresentation client, List<ScopeRepresentation> existingClientAuthorizationScopes, List<ScopeRepresentation> authorizationScopesToImport) {
        Map<String, ScopeRepresentation> existingClientAuthorizationScopesMap = existingClientAuthorizationScopes
                .stream()
                .collect(Collectors.toMap(ScopeRepresentation::getName, scope -> scope));

        for (ScopeRepresentation authorizationScopeToImport : authorizationScopesToImport) {
            createOrUpdateAuthorizationScope(realm, client, existingClientAuthorizationScopesMap, authorizationScopeToImport);
        }
    }

    private void createOrUpdateAuthorizationScope(String realm, ClientRepresentation client, Map<String, ScopeRepresentation> existingClientAuthorizationScopesMap, ScopeRepresentation authorizationScopeToImport) {
        String authorizationScopeNameToImport = authorizationScopeToImport.getName();
        if (!existingClientAuthorizationScopesMap.containsKey(authorizationScopeToImport.getName())) {
            logger.debug("Add authorization scope '{}' for client '{}' in realm '{}'", authorizationScopeNameToImport, client.getClientId(), realm);
            clientRepository.addAuthorizationScope(realm, client.getId(), authorizationScopeNameToImport);
        } else {
            updateAuthorizationScope(realm, client, existingClientAuthorizationScopesMap, authorizationScopeToImport, authorizationScopeNameToImport);
        }
    }

    private void updateAuthorizationScope(String realm, ClientRepresentation client, Map<String, ScopeRepresentation> existingClientAuthorizationScopesMap, ScopeRepresentation authorizationScopeToImport, String authorizationScopeNameToImport) {
        ScopeRepresentation existingClientAuthorizationScope = existingClientAuthorizationScopesMap
                .get(authorizationScopeNameToImport);

        if (!CloneUtil.deepEquals(authorizationScopeToImport, existingClientAuthorizationScope, "id")) {
            authorizationScopeToImport.setId(existingClientAuthorizationScope.getId());
            logger.debug("Update authorization scope '{}' for client '{}' in realm '{}'", authorizationScopeNameToImport, client.getClientId(), realm);
            clientRepository.updateAuthorizationScope(realm, client.getId(), authorizationScopeToImport);
        }
    }

    private void removeAuthorizationScopes(String realm, ClientRepresentation client, List<ScopeRepresentation> existingClientAuthorizationScopes, List<ScopeRepresentation> authorizationScopesToImport) {
        List<String> authorizationScopeNamesToImport = authorizationScopesToImport
                .stream().map(ScopeRepresentation::getName)
                .collect(Collectors.toList());

        for (ScopeRepresentation existingClientAuthorizationScope : existingClientAuthorizationScopes) {
            if (!authorizationScopeNamesToImport.contains(existingClientAuthorizationScope.getName())) {
                removeAuthorizationScope(realm, client, existingClientAuthorizationScope);
            }
        }
    }

    private void removeAuthorizationScope(String realm, ClientRepresentation client, ScopeRepresentation existingClientAuthorizationScope) {
        logger.debug("Remove authorization scope '{}' for client '{}' in realm '{}'", existingClientAuthorizationScope.getName(), client.getClientId(), realm);
        clientRepository.removeAuthorizationScope(realm, client.getId(), existingClientAuthorizationScope.getId());
    }

    private void createOrUpdateAuthorizationPolicies(String realm, ClientRepresentation client, List<PolicyRepresentation> existingClientAuthorizationPolicies, List<PolicyRepresentation> authorizationPoliciesToImport) {
        Map<String, PolicyRepresentation> existingClientAuthorizationPoliciesMap = existingClientAuthorizationPolicies
                .stream()
                .collect(Collectors.toMap(PolicyRepresentation::getName, resource -> resource));

        for (PolicyRepresentation authorizationPolicyToImport : authorizationPoliciesToImport) {
            createOrUpdateAuthorizationPolicy(realm, client, existingClientAuthorizationPoliciesMap, authorizationPolicyToImport);
        }
    }

    private void createOrUpdateAuthorizationPolicy(String realm, ClientRepresentation client, Map<String, PolicyRepresentation> existingClientAuthorizationPoliciesMap, PolicyRepresentation authorizationPolicyToImport) {
        if (!existingClientAuthorizationPoliciesMap.containsKey(authorizationPolicyToImport.getName())) {
            logger.debug("Create authorization policy '{}' for client '{}' in realm '{}'", authorizationPolicyToImport.getName(), client.getClientId(), realm);
            clientRepository.createAuthorizationPolicy(realm, client.getId(), authorizationPolicyToImport);
        } else {
            updateAuthorizationPolicy(realm, client, existingClientAuthorizationPoliciesMap, authorizationPolicyToImport);
        }
    }

    private void updateAuthorizationPolicy(String realm, ClientRepresentation client, Map<String, PolicyRepresentation> existingClientAuthorizationPoliciesMap, PolicyRepresentation authorizationPolicyToImport) {
        PolicyRepresentation existingClientAuthorizationPolicy = existingClientAuthorizationPoliciesMap
                .get(authorizationPolicyToImport.getName());

        if (!CloneUtil.deepEquals(authorizationPolicyToImport, existingClientAuthorizationPolicy, "id")) {
            authorizationPolicyToImport.setId(existingClientAuthorizationPolicy.getId());
            logger.debug("Update authorization policy '{}' for client '{}' in realm '{}'", authorizationPolicyToImport.getName(), client.getClientId(), realm);
            clientRepository.updateAuthorizationPolicy(realm, client.getId(), authorizationPolicyToImport);
        }
    }

    private void removeAuthorizationPolicies(String realm, ClientRepresentation client, List<PolicyRepresentation> existingClientAuthorizationPolicies, List<PolicyRepresentation> authorizationPoliciesToImport) {
        List<String> authorizationPolicyNamesToImport = authorizationPoliciesToImport
                .stream().map(PolicyRepresentation::getName)
                .collect(Collectors.toList());

        for (PolicyRepresentation existingClientAuthorizationPolicy : existingClientAuthorizationPolicies) {
            if (!authorizationPolicyNamesToImport.contains(existingClientAuthorizationPolicy.getName())) {
                removeAuthorizationPolicy(realm, client, existingClientAuthorizationPolicy);
            }
        }
    }

    private void removeAuthorizationPolicy(String realm, ClientRepresentation client, PolicyRepresentation existingClientAuthorizationPolicy) {
        logger.debug("Remove authorization policy '{}' for client '{}' in realm '{}'", existingClientAuthorizationPolicy.getName(), client.getClientId(), realm);
        try {
            clientRepository.removeAuthorizationPolicy(realm, client.getId(), existingClientAuthorizationPolicy.getId());
        } catch (NotFoundException ignored) {
            // policies got deleted if linked resources are deleted, too.
        }
    }
}
