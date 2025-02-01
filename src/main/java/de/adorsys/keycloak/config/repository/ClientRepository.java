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

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.util.ResponseUtil;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ManagementPermissionRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class ClientRepository {

    private final RealmRepository realmRepository;

    @Autowired
    public ClientRepository(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    public Optional<ClientRepresentation> searchByClientId(String realmName, String clientId) {
        List<ClientRepresentation> foundClients = getResource(realmName).findByClientId(Objects.requireNonNull(clientId));

        Optional<ClientRepresentation> client;
        if (foundClients.isEmpty()) {
            client = Optional.empty();
        } else {
            client = Optional.of(foundClients.get(0));
        }

        return client;
    }

    public Optional<ClientRepresentation> searchByName(String realmName, String name) {
        Objects.requireNonNull(name);

        // this is expensive, but easy to implement.
        // if this too expensive, please provide a PR which implement a pagination for findAll()
        Optional<ClientRepresentation> foundClients = realmRepository.getResource(realmName)
                .partialExport(false, true)
                .getClients()
                .stream()
                .filter(client -> Objects.equals(name, client.getName()))
                .findAny();

        return foundClients.map(clientRepresentation -> getByClientId(realmName, clientRepresentation.getId()));
    }

    public ClientRepresentation getByClientId(String realmName, String clientId) {
        Optional<ClientRepresentation> foundClients = searchByClientId(realmName, clientId);

        if (foundClients.isEmpty()) {
            throw new KeycloakRepositoryException("Cannot find client by clientId '%s'", clientId);
        }

        return foundClients.get();
    }

    public ClientRepresentation getByName(String realmName, String name) {
        Optional<ClientRepresentation> foundClients = searchByName(realmName, name);

        if (foundClients.isEmpty()) {
            throw new KeycloakRepositoryException("Cannot find client by name '%s'", name);
        }

        return foundClients.get();
    }

    public ResourceServerRepresentation getAuthorizationConfigById(String realmName, String id) {
        return getResourceById(realmName, id).authorization().exportSettings();
    }

    public String getClientSecret(String realmName, String clientId) {
        ClientResource clientResource = getResourceByClientId(realmName, clientId);
        return clientResource.getSecret().getValue();
    }

    public void create(String realmName, ClientRepresentation client) {
        try (Response response = getResource(realmName).create(client)) {
            CreatedResponseUtil.getCreatedId(response);
        } catch (WebApplicationException error) {
            String errorMessage = ResponseUtil.getErrorMessage(error);

            throw new ImportProcessingException(
                    String.format("Cannot create client '%s' in realm '%s': %s", client.getClientId(), realmName, errorMessage),
                    error
            );
        }
    }

    public void update(String realmName, ClientRepresentation client) {
        ClientResource clientResource = getResourceById(realmName, client.getId());
        clientResource.update(client);
    }

    public void remove(String realmName, ClientRepresentation client) {
        ClientResource clientResource = getResourceById(realmName, client.getId());
        clientResource.remove();
    }

    private ClientsResource getResource(String realmName) {
        return realmRepository.getResource(realmName).clients();
    }

    public ClientResource getResourceById(String realmName, String id) {
        ClientResource client = getResource(realmName).get(id);

        if (client == null) {
            throw new KeycloakRepositoryException("Cannot find client by id '%s'", id);
        }

        return client;
    }

    public ClientResource getResourceByClientId(String realmName, String clientId) {
        ClientRepresentation client = getByClientId(realmName, clientId);

        return getResourceById(realmName, client.getId());
    }

    public final Set<String> getAllIds(String realmName) {
        return getAll(realmName)
                .stream()
                .map(ClientRepresentation::getClientId)
                .collect(Collectors.toSet());
    }

    public final List<ClientRepresentation> getAll(String realmName) {
        return getResource(realmName).findAll();
    }

    public void updateAuthorizationSettings(String realmName, String id, ResourceServerRepresentation authorizationSettings) {
        ClientResource clientResource = getResourceById(realmName, id);
        clientResource.authorization().update(authorizationSettings);
    }

    public void createAuthorizationResource(String realmName, String id, ResourceRepresentation resource) {
        ClientResource clientResource = getResourceById(realmName, id);

        try (Response response = clientResource.authorization().resources().create(resource)) {
            CreatedResponseUtil.getCreatedId(response);
        }
    }

    public void updateAuthorizationResource(String realmName, String id, ResourceRepresentation resource) {
        ClientResource clientResource = getResourceById(realmName, id);
        String resourceId = getResourceId(clientResource, resource.getName());
        clientResource.authorization().resources().resource(resourceId).update(resource);
    }

    public void removeAuthorizationResource(String realmName, String id, String resourceName) {
        ClientResource clientResource = getResourceById(realmName, id);
        String resourceId = getResourceId(clientResource, resourceName);
        if (resourceId != null) {
            clientResource.authorization().resources().resource(resourceId).remove();
        }
    }

    private String getResourceId(ClientResource clientResource, String resourceName) {
        String clientId = clientResource.toRepresentation().getClientId();
        // find it with name and owner(clientId)
        // Note: findByName is not exact filter the resource with the exact name
        return clientResource.authorization().resources().findByName(resourceName, clientId).stream()
                .filter(r -> resourceName.equals(r.getName()))
                .findFirst().map(ResourceRepresentation::getId)
                .orElseGet(
                    () -> clientResource.authorization().resources()
                            .findByName(resourceName).stream()
                            .filter(r -> resourceName.equals(r.getName())
                                    && !clientId.equals(r.getOwner().getName()))
                            .findFirst().map(ResourceRepresentation::getId)
                    .orElse(null));
    }

    public void addAuthorizationScope(String realmName, String id, ScopeRepresentation scope) {
        ClientResource clientResource = getResourceById(realmName, id);

        try (Response response = clientResource.authorization().scopes().create(scope)) {
            CreatedResponseUtil.getCreatedId(response);
        }
    }

    public void updateAuthorizationScope(String realmName, String id, ScopeRepresentation scope) {
        ClientResource clientResource = getResourceById(realmName, id);
        String scopeId = getScopeId(clientResource, scope.getName());
        clientResource.authorization().scopes().scope(scopeId).update(scope);
    }

    public void removeAuthorizationScope(String realmName, String id, String scopeName) {
        ClientResource clientResource = getResourceById(realmName, id);
        String scopeId = getScopeId(clientResource, scopeName);
        if (scopeId != null) {
            clientResource.authorization().scopes().scope(scopeId).remove();
        }
    }

    private String getScopeId(ClientResource clientResource, String scopeName) {
        ScopeRepresentation scopeRepresentation = clientResource.authorization().scopes().findByName(scopeName);
        if (scopeRepresentation != null) {
            return scopeRepresentation.getId();
        }
        return null;
    }

    public void createAuthorizationPolicy(String realmName, String id, PolicyRepresentation policy) {
        ClientResource clientResource = getResourceById(realmName, id);

        try (Response response = clientResource.authorization().policies().create(policy)) {
            CreatedResponseUtil.getCreatedId(response);
        }
    }

    public void updateAuthorizationPolicy(String realmName, String id, PolicyRepresentation policy) {
        ClientResource clientResource = getResourceById(realmName, id);
        String policyId = getPolicyId(clientResource, policy.getName());
        clientResource.authorization().policies().policy(policyId).update(policy);
    }

    public void removeAuthorizationPolicy(String realmName, String id, String policyName) {
        ClientResource clientResource = getResourceById(realmName, id);
        String policyId = getPolicyId(clientResource, policyName);
        if (policyId != null) {
            clientResource.authorization().policies().policy(policyId).remove();
        }
    }

    private String getPolicyId(ClientResource clientResource, String policyName) {
        PolicyRepresentation policyRepresentation = clientResource.authorization().policies().findByName(policyName);
        if (policyRepresentation != null) {
            return policyRepresentation.getId();
        }
        return null;
    }

    public void addScopeMapping(String realmName, String clientId,
                                String clientLevelId, List<RoleRepresentation> roles) {
        ClientResource clientResource = getResourceByClientId(realmName, clientId);
        clientResource
                .getScopeMappings()
                .clientLevel(clientLevelId)
                .add(roles);
    }

    public void removeScopeMapping(String realmName, String clientId,
                                   String clientLevelId, List<RoleRepresentation> roles) {
        ClientResource clientResource = getResourceByClientId(realmName, clientId);
        clientResource
                .getScopeMappings()
                .clientLevel(clientLevelId)
                .remove(roles);
    }

    public void addDefaultClientScopes(String realmName, String clientId,
                                       List<ClientScopeRepresentation> defaultClientScopes) {
        ClientResource clientResource = getResourceByClientId(realmName, clientId);

        for (ClientScopeRepresentation defaultClientScope : defaultClientScopes) {
            clientResource.addDefaultClientScope(defaultClientScope.getId());
        }
    }

    public void removeDefaultClientScopes(String realmName, String clientId,
                                          List<ClientScopeRepresentation> defaultClientScopes) {
        ClientResource clientResource = getResourceByClientId(realmName, clientId);

        for (ClientScopeRepresentation defaultClientScope : defaultClientScopes) {
            clientResource.removeDefaultClientScope(defaultClientScope.getId());
        }
    }

    public void addOptionalClientScopes(String realmName, String clientId,
                                        List<ClientScopeRepresentation> optionalClientScopes) {
        ClientResource clientResource = getResourceByClientId(realmName, clientId);

        for (ClientScopeRepresentation optionalClientScope : optionalClientScopes) {
            clientResource.addOptionalClientScope(optionalClientScope.getId());
        }
    }

    public void removeOptionalClientScopes(String realmName, String clientId,
                                           List<ClientScopeRepresentation> optionalClientScopes) {
        ClientResource clientResource = getResourceByClientId(realmName, clientId);

        for (ClientScopeRepresentation optionalClientScope : optionalClientScopes) {
            clientResource.removeOptionalClientScope(optionalClientScope.getId());
        }
    }

    public void enablePermission(String realmName, String id) {
        ClientResource clientResource = getResourceById(realmName, id);

        clientResource.setPermissions(new ManagementPermissionRepresentation(true));
    }

    public boolean isPermissionEnabled(String realmName, String id) {
        ClientResource clientResource = getResourceById(realmName, id);

        return clientResource.getPermissions().isEnabled();
    }
}
