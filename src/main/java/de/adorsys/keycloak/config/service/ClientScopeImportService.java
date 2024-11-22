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
import de.adorsys.keycloak.config.repository.ClientScopeRepository;
import de.adorsys.keycloak.config.repository.RealmRepository;
import de.adorsys.keycloak.config.util.CloneUtil;
import de.adorsys.keycloak.config.util.ProtocolMapperUtil;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class ClientScopeImportService {
    private static final Logger logger = LoggerFactory.getLogger(ClientScopeImportService.class);

    private final ClientScopeRepository clientScopeRepository;
    private final ImportConfigProperties importConfigProperties;
    private final RealmRepository realmRepository;

    public ClientScopeImportService(
            ClientScopeRepository clientScopeRepository,
            ImportConfigProperties importConfigProperties,
            RealmRepository realmRepository) {
        this.clientScopeRepository = clientScopeRepository;
        this.importConfigProperties = importConfigProperties;
        this.realmRepository = realmRepository;
    }

    public void doImport(RealmImport realmImport) {
        List<ClientScopeRepresentation> clientScopes = realmImport.getClientScopes();
        String realmName = realmImport.getRealm();

        if (clientScopes == null) return;

        createOrUpdateClientScopes(realmName, clientScopes);
    }

    public void updateDefaultClientScopes(RealmImport realmImport, RealmRepresentation existingRealm) {
        List<String> existingDefaultDefaultClientScopes = realmRepository.getResource(realmImport.getRealm())
                .getDefaultDefaultClientScopes()
                .stream().map(ClientScopeRepresentation::getName)
                .toList();

        List<String> existingDefaultOptionalClientScopes = realmRepository.getResource(realmImport.getRealm())
                .getDefaultOptionalClientScopes()
                .stream().map(ClientScopeRepresentation::getName)
                .toList();

        removeDefaultDefaultClientScopes(realmImport, existingDefaultDefaultClientScopes);
        removeDefaultOptionalClientScopes(realmImport, existingDefaultOptionalClientScopes);

        addDefaultDefaultClientScopes(realmImport, existingDefaultDefaultClientScopes);
        addDefaultOptionalClientScopes(realmImport, existingDefaultOptionalClientScopes);

        existingRealm.setDefaultDefaultClientScopes(realmImport.getDefaultDefaultClientScopes());
        existingRealm.setDefaultOptionalClientScopes(realmImport.getDefaultOptionalClientScopes());
    }

    private void addDefaultDefaultClientScopes(RealmImport realmImport, List<String> existingDefaultClientScopes) {
        if (realmImport.getDefaultDefaultClientScopes() == null) return;

        for (String scope : realmImport.getDefaultDefaultClientScopes()) {
            if (existingDefaultClientScopes != null && existingDefaultClientScopes.contains(scope)) continue;

            ClientScopeRepresentation scopeResource = clientScopeRepository.getByName(realmImport.getRealm(), scope);

            if (scopeResource == null) {
                throw new ImportProcessingException(
                        String.format("Could not find client scope '%s' in realm '%s'!", scope, realmImport.getRealm())
                );
            }

            realmRepository.addDefaultDefaultClientScope(realmImport.getRealm(), scopeResource.getId());
        }
    }

    private void addDefaultOptionalClientScopes(RealmImport realmImport, List<String> existingDefaultClientScopes) {
        if (realmImport.getDefaultOptionalClientScopes() == null) return;

        for (String scope : realmImport.getDefaultOptionalClientScopes()) {
            if (existingDefaultClientScopes != null && existingDefaultClientScopes.contains(scope)) continue;

            ClientScopeRepresentation scopeResource = clientScopeRepository.getByName(realmImport.getRealm(), scope);

            if (scopeResource == null) {
                throw new ImportProcessingException(
                        String.format("Could not find client scope '%s' in realm '%s'!", scope, realmImport.getRealm())
                );
            }

            realmRepository.addDefaultOptionalClientScope(realmImport.getRealm(), scopeResource.getId());
        }
    }

    private void removeDefaultDefaultClientScopes(RealmImport realmImport, List<String> existingDefaultClientScopes) {
        if (realmImport.getDefaultDefaultClientScopes() == null || existingDefaultClientScopes == null) return;

        for (String scope : existingDefaultClientScopes) {
            if (realmImport.getDefaultDefaultClientScopes().contains(scope)) continue;

            ClientScopeRepresentation scopeResource = clientScopeRepository.getByName(realmImport.getRealm(), scope);
            realmRepository.removeDefaultDefaultClientScope(realmImport.getRealm(), scopeResource.getId());
        }
    }

    private void removeDefaultOptionalClientScopes(RealmImport realmImport, List<String> existingDefaultClientScopes) {
        if (realmImport.getDefaultOptionalClientScopes() == null || existingDefaultClientScopes == null) return;

        for (String scope : existingDefaultClientScopes) {
            if (realmImport.getDefaultOptionalClientScopes().contains(scope)) continue;

            ClientScopeRepresentation scopeResource = clientScopeRepository.getByName(realmImport.getRealm(), scope);
            realmRepository.removeDefaultOptionalClientScope(realmImport.getRealm(), scopeResource.getId());
        }
    }

    public void doRemoveOrphan(RealmImport realmImport) {
        List<ClientScopeRepresentation> clientScopes = realmImport.getClientScopes();
        String realmName = realmImport.getRealm();

        if (clientScopes == null) return;

        List<ClientScopeRepresentation> existingClientScopes = clientScopeRepository
                .getAll(realmName);

        List<ClientScopeRepresentation> existingDefaultClientScopes = clientScopeRepository
                .getDefaultClientScopes(realmName);

        if (importConfigProperties.getManaged().getClientScope()
                == ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues.FULL) {
            deleteClientScopesMissingInImport(
                    realmName, clientScopes, existingClientScopes, existingDefaultClientScopes
            );
        }
    }

    private void createOrUpdateClientScopes(
            String realmName,
            List<ClientScopeRepresentation> clientScopes
    ) {
        Consumer<ClientScopeRepresentation> loop = clientScope -> createOrUpdateClientScope(realmName, clientScope);
        if (importConfigProperties.isParallel()) {
            clientScopes.parallelStream().forEach(loop);
        } else {
            clientScopes.forEach(loop);
        }
    }

    private void deleteClientScopesMissingInImport(
            String realmName,
            List<ClientScopeRepresentation> clientScopes,
            List<ClientScopeRepresentation> existingClientScopes,
            List<ClientScopeRepresentation> existingDefaultClientScopes
    ) {
        for (ClientScopeRepresentation existingClientScope : existingClientScopes) {
            if (
                    isNotDefaultScope(existingClientScope.getName(), existingDefaultClientScopes)
                            && !hasClientScopeWithName(clientScopes, existingClientScope.getName())
            ) {
                logger.debug("Delete clientScope '{}' in realm '{}'", existingClientScope.getName(), realmName);
                clientScopeRepository.delete(realmName, existingClientScope.getId());
            }
        }
    }

    private boolean isNotDefaultScope(
            String clientScopeName,
            List<ClientScopeRepresentation> existingDefaultClientScopes
    ) {
        return existingDefaultClientScopes
                .stream()
                .noneMatch(s -> Objects.equals(s.getName(), clientScopeName));
    }

    private boolean hasClientScopeWithName(
            List<ClientScopeRepresentation> clientScopes,
            String clientScopeName
    ) {
        return clientScopes.stream().anyMatch(s -> Objects.equals(s.getName(), clientScopeName));
    }

    private void createOrUpdateClientScope(
            String realmName,
            ClientScopeRepresentation clientScope
    ) {
        String clientScopeName = clientScope.getName();

        Optional<ClientScopeRepresentation> maybeClientScope = clientScopeRepository
                .searchByName(realmName, clientScopeName);

        if (maybeClientScope.isPresent()) {
            updateClientScopeIfNecessary(realmName, clientScope);
        } else {
            logger.debug("Create clientScope '{}' in realm '{}'", clientScopeName, realmName);
            createClientScope(realmName, clientScope);
        }
    }

    private void createClientScope(
            String realmName,
            ClientScopeRepresentation clientScope
    ) {
        clientScopeRepository.create(realmName, clientScope);
    }

    private void updateClientScopeIfNecessary(
            String realmName,
            ClientScopeRepresentation clientScope
    ) {
        ClientScopeRepresentation existingClientScope = clientScopeRepository.getByName(
                realmName, clientScope.getName()
        );
        ClientScopeRepresentation patchedClientScope = CloneUtil.patch(
                existingClientScope, clientScope, "id"
        );

        String clientScopeName = existingClientScope.getName();

        if (isClientScopeEqual(existingClientScope, patchedClientScope)) {
            logger.debug("No need to update clientScope '{}' in realm '{}'", clientScopeName, realmName);
        } else {
            logger.debug("Update clientScope '{}' in realm '{}'", clientScopeName, realmName);
            updateClientScope(realmName, patchedClientScope);
        }
    }

    private boolean isClientScopeEqual(
            ClientScopeRepresentation existingClientScope,
            ClientScopeRepresentation patchedClientScope
    ) {
        return CloneUtil.deepEquals(existingClientScope, patchedClientScope, "protocolMappers")
                && ProtocolMapperUtil.areProtocolMappersEqual(
                patchedClientScope.getProtocolMappers(),
                existingClientScope.getProtocolMappers()
        );
    }

    private void updateClientScope(String realmName, ClientScopeRepresentation patchedClientScope) {
        clientScopeRepository.update(realmName, patchedClientScope);

        List<ProtocolMapperRepresentation> protocolMappers = patchedClientScope.getProtocolMappers();
        if (protocolMappers != null) {
            String clientScopeId = patchedClientScope.getId();
            updateProtocolMappers(realmName, clientScopeId, protocolMappers);
        }
    }

    private void updateProtocolMappers(
            String realmName,
            String clientScopeId,
            List<ProtocolMapperRepresentation> protocolMappers
    ) {
        ClientScopeRepresentation existingClientScope = clientScopeRepository.getById(realmName, clientScopeId);

        List<ProtocolMapperRepresentation> existingProtocolMappers = existingClientScope.getProtocolMappers();

        List<ProtocolMapperRepresentation> protocolMappersToAdd = ProtocolMapperUtil
                .estimateProtocolMappersToAdd(protocolMappers, existingProtocolMappers);
        List<ProtocolMapperRepresentation> protocolMappersToRemove = ProtocolMapperUtil
                .estimateProtocolMappersToRemove(protocolMappers, existingProtocolMappers);
        List<ProtocolMapperRepresentation> protocolMappersToUpdate = ProtocolMapperUtil
                .estimateProtocolMappersToUpdate(protocolMappers, existingProtocolMappers);

        clientScopeRepository.addProtocolMappers(realmName, clientScopeId, protocolMappersToAdd);
        clientScopeRepository.removeProtocolMappers(realmName, clientScopeId, protocolMappersToRemove);
        clientScopeRepository.updateProtocolMappers(realmName, clientScopeId, protocolMappersToUpdate);
    }
}
