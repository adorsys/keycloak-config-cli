/*
 * Copyright 2019-2020 adorsys GmbH & Co. KG @ https://adorsys.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues;
import de.adorsys.keycloak.config.repository.ClientScopeRepository;
import de.adorsys.keycloak.config.util.CloneUtil;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ClientScopeImportService {
    private static final Logger logger = LoggerFactory.getLogger(ClientScopeImportService.class);

    private final ClientScopeRepository clientScopeRepository;
    private final ImportConfigProperties importConfigProperties;

    public ClientScopeImportService(ClientScopeRepository clientScopeRepository, ImportConfigProperties importConfigProperties) {
        this.clientScopeRepository = clientScopeRepository;
        this.importConfigProperties = importConfigProperties;
    }

    public void importClientScopes(RealmImport realmImport) {
        List<ClientScopeRepresentation> clientScopes = realmImport.getClientScopes();
        String realm = realmImport.getRealm();

        if (clientScopes == null) {
            logger.debug("No clientScopes to import into realm '{}'", realm);
        } else {
            importClientScopes(realm, clientScopes);
        }
    }

    private void importClientScopes(String realm, List<ClientScopeRepresentation> clientScopes) {
        List<ClientScopeRepresentation> existingClientScopes = clientScopeRepository.getClientScopes(realm);
        List<ClientScopeRepresentation> existingDefaultClientScopes = clientScopeRepository.getDefaultClientScopes(realm);

        if (clientScopes.isEmpty()) {
            if (importConfigProperties.getManaged().getClientScope() == ImportManagedPropertiesValues.noDelete) {
                logger.info("Skip deletion of clientScopes");
                return;
            }

            deleteAllExistingClientScopes(realm, existingClientScopes, existingDefaultClientScopes);
        } else {
            if (importConfigProperties.getManaged().getClientScope() == ImportManagedPropertiesValues.full) {
                deleteClientScopesMissingInImport(realm, clientScopes, existingClientScopes, existingDefaultClientScopes);
            }

            for (ClientScopeRepresentation clientScope : clientScopes) {
                createOrUpdateClientScope(realm, clientScope, existingDefaultClientScopes);
            }
        }
    }

    private void deleteAllExistingClientScopes(String realm, List<ClientScopeRepresentation> existingClientScopes, List<ClientScopeRepresentation> existingDefaultClientScopes) {
        for (ClientScopeRepresentation existingClientScope : existingClientScopes) {
            if (isNotDefaultScope(existingClientScope.getName(), existingDefaultClientScopes)) {
                logger.debug("Delete clientScope '{}' in realm '{}'", existingClientScope.getName(), realm);
                clientScopeRepository.deleteClientScope(realm, existingClientScope.getId());
            }
        }
    }

    private void deleteClientScopesMissingInImport(String realm, List<ClientScopeRepresentation> clientScopes, List<ClientScopeRepresentation> existingClientScopes, List<ClientScopeRepresentation> existingDefaultClientScopes) {
        for (ClientScopeRepresentation existingClientScope : existingClientScopes) {
            if (isNotDefaultScope(existingClientScope.getName(), existingDefaultClientScopes) && !hasClientScopeWithName(clientScopes, existingClientScope.getName())) {
                logger.debug("Delete clientScope '{}' in realm '{}'", existingClientScope.getName(), realm);
                clientScopeRepository.deleteClientScope(realm, existingClientScope.getId());
            }
        }
    }

    private boolean isNotDefaultScope(String clientScopeName, List<ClientScopeRepresentation> existingDefaultClientScopes) {
        return existingDefaultClientScopes.stream().noneMatch(s -> Objects.equals(s.getName(), clientScopeName));
    }

    private boolean hasClientScopeWithName(List<ClientScopeRepresentation> clientScopes, String clientScopeName) {
        return clientScopes.stream().anyMatch(s -> Objects.equals(s.getName(), clientScopeName));
    }

    private void createOrUpdateClientScope(String realm, ClientScopeRepresentation clientScope, List<ClientScopeRepresentation> existingDefaultClientScopes) {
        String clientScopeName = clientScope.getName();

        Optional<ClientScopeRepresentation> maybeClientScope = clientScopeRepository.tryToFindClientScopeByName(realm, clientScopeName);

        if (!isNotDefaultScope(clientScope.getName(), existingDefaultClientScopes)) {
            logger.debug("Ignore default clientScope '{}' in realm '{}'", clientScopeName, realm);
            return;
        }

        if (maybeClientScope.isPresent()) {
            updateClientScopeIfNecessary(realm, clientScope);
        } else {
            logger.debug("Create clientScope '{}' in realm '{}'", clientScopeName, realm);
            createClientScope(realm, clientScope);
        }
    }

    private void createClientScope(String realm, ClientScopeRepresentation clientScope) {
        clientScopeRepository.createClientScope(realm, clientScope);

        List<ProtocolMapperRepresentation> protocolMappers = clientScope.getProtocolMappers();
        if (protocolMappers != null) {
            clientScopeRepository.addProtocolMappers(realm, clientScope.getId(), protocolMappers);
        }
    }

    private void updateClientScopeIfNecessary(String realm, ClientScopeRepresentation clientScope) {
        ClientScopeRepresentation existingClientScope = clientScopeRepository.getClientScopeByName(realm, clientScope.getName());
        ClientScopeRepresentation patchedClientScope = CloneUtil.patch(existingClientScope, clientScope);
        String clientScopeName = existingClientScope.getName();

        if (CloneUtil.deepEquals(existingClientScope, patchedClientScope)) {
            logger.debug("No need to update clientScope '{}' in realm '{}'", clientScopeName, realm);
        } else {
            logger.debug("Update clientScope '{}' in realm '{}'", clientScopeName, realm);
            updateClientScope(realm, patchedClientScope);
        }
    }

    private void updateClientScope(String realm, ClientScopeRepresentation patchedClientScope) {
        clientScopeRepository.updateClientScope(realm, patchedClientScope);

        List<ProtocolMapperRepresentation> protocolMappers = patchedClientScope.getProtocolMappers();
        if (protocolMappers != null) {
            String clientScopeId = patchedClientScope.getId();
            updateProtocolMappers(realm, clientScopeId, protocolMappers);
        }
    }

    private void updateProtocolMappers(String realm, String clientScopeId, List<ProtocolMapperRepresentation> protocolMappers) {
        ClientScopeRepresentation existingClientScope = clientScopeRepository.getClientScopeById(realm, clientScopeId);

        List<ProtocolMapperRepresentation> existingProtocolMappers = existingClientScope.getProtocolMappers();

        List<ProtocolMapperRepresentation> protocolMappersToAdd = estimateProtocolMappersToAdd(protocolMappers, existingProtocolMappers);
        List<ProtocolMapperRepresentation> protocolMappersToRemove = estimateProtocolMappersToRemove(protocolMappers, existingProtocolMappers);
        List<ProtocolMapperRepresentation> protocolMappersToUpdate = estimateProtocolMappersToUpdate(protocolMappers, existingProtocolMappers);

        clientScopeRepository.addProtocolMappers(realm, clientScopeId, protocolMappersToAdd);
        clientScopeRepository.removeProtocolMappers(realm, clientScopeId, protocolMappersToRemove);
        clientScopeRepository.updateProtocolMappers(realm, clientScopeId, protocolMappersToUpdate);
    }

    private List<ProtocolMapperRepresentation> estimateProtocolMappersToRemove(List<ProtocolMapperRepresentation> protocolMappers, List<ProtocolMapperRepresentation> existingProtocolMappers) {
        List<ProtocolMapperRepresentation> protocolMappersToRemove = new ArrayList<>();

        if (existingProtocolMappers == null) {
            return protocolMappersToRemove;
        }

        for (ProtocolMapperRepresentation existingProtocolMapper : existingProtocolMappers) {
            if (protocolMappers.stream().noneMatch(m -> Objects.equals(m.getName(), existingProtocolMapper.getName()))) {
                protocolMappersToRemove.add(existingProtocolMapper);
            }
        }

        return protocolMappersToRemove;
    }

    private List<ProtocolMapperRepresentation> estimateProtocolMappersToAdd(List<ProtocolMapperRepresentation> protocolMappers, List<ProtocolMapperRepresentation> existingProtocolMappers) {
        List<ProtocolMapperRepresentation> protocolMappersToAdd = new ArrayList<>();

        if (existingProtocolMappers == null) {
            return protocolMappers;
        }

        for (ProtocolMapperRepresentation protocolMapper : protocolMappers) {
            if (existingProtocolMappers.stream().noneMatch(em -> Objects.equals(em.getName(), protocolMapper.getName()))) {
                protocolMappersToAdd.add(protocolMapper);
            }
        }

        return protocolMappersToAdd;
    }

    private List<ProtocolMapperRepresentation> estimateProtocolMappersToUpdate(List<ProtocolMapperRepresentation> protocolMappers, List<ProtocolMapperRepresentation> existingProtocolMappers) {
        List<ProtocolMapperRepresentation> protocolMappersToUpdate = new ArrayList<>();

        if (existingProtocolMappers == null) {
            return protocolMappersToUpdate;
        }

        for (ProtocolMapperRepresentation protocolMapper : protocolMappers) {
            Optional<ProtocolMapperRepresentation> existingProtocolMapper = existingProtocolMappers.stream().filter(em -> Objects.equals(em.getName(), protocolMapper.getName())).findFirst();
            if (existingProtocolMapper.isPresent()) {
                ProtocolMapperRepresentation patchedProtocolMapper = CloneUtil.patch(existingProtocolMapper.get(), protocolMapper);
                protocolMappersToUpdate.add(patchedProtocolMapper);
            }
        }

        return protocolMappersToUpdate;
    }

}
