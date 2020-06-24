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
import de.adorsys.keycloak.config.repository.ClientRepository;
import de.adorsys.keycloak.config.util.CloneUtil;
import de.adorsys.keycloak.config.util.ProtocolMapperUtil;
import de.adorsys.keycloak.config.util.ResponseUtil;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Optional;

@Service
public class ClientImportService {
    private static final Logger logger = LoggerFactory.getLogger(ClientImportService.class);

    private final ClientRepository clientRepository;

    @Autowired
    public ClientImportService(
            ClientRepository clientRepository
    ) {
        this.clientRepository = clientRepository;
    }

    public void doImport(RealmImport realmImport) {
        List<ClientRepresentation> clients = realmImport.getClients();
        if (clients == null) {
            return;
        }

        createOrUpdateClients(realmImport, clients);
    }

    private void createOrUpdateClients(RealmImport realmImport, List<ClientRepresentation> clients) {
        for (ClientRepresentation client : clients) {
            createOrUpdateClient(realmImport, client);
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
            try {
                clientRepository.create(realm, client);
            } catch (WebApplicationException error) {
                String errorMessage = ResponseUtil.getErrorMessage(error);
                throw new ImportProcessingException("Cannot create client for realm '" + realm + "': " + errorMessage, error);
            }
        }
    }

    private void updateClientIfNeeded(String realm, ClientRepresentation clientToUpdate, ClientRepresentation existingClient) {
        if (!areClientsEqual(realm, clientToUpdate, existingClient)) {
            logger.debug("Update client '{}' in realm '{}'", clientToUpdate.getClientId(), realm);
            updateClient(realm, existingClient, clientToUpdate);
        } else {
            logger.debug("No need to update client '{}' in realm '{}'", clientToUpdate.getClientId(), realm);
        }
    }

    private boolean areClientsEqual(String realm, ClientRepresentation clientToUpdate, ClientRepresentation existingClient) {
        // Clients are never equal except every properties if defined in import realm
        if (CloneUtil.deepEquals(clientToUpdate, existingClient, "id", "secret", "access")) {
            String clientSecret = clientRepository.getClientSecret(realm, clientToUpdate.getClientId());
            return clientSecret.equals(clientToUpdate.getSecret());
        }

        return false;
    }

    private void updateClient(String realm, ClientRepresentation existingClient, ClientRepresentation clientToImport) {
        ClientRepresentation patchedClient = CloneUtil.patch(existingClient, clientToImport, "id");
        try {
            clientRepository.update(realm, patchedClient);
        } catch (WebApplicationException error) {
            String errorMessage = ResponseUtil.getErrorMessage(error);
            throw new ImportProcessingException("Update create client for realm '" + realm + "': " + errorMessage, error);
        }

        List<ProtocolMapperRepresentation> protocolMappers = patchedClient.getProtocolMappers();
        if (protocolMappers != null) {
            String clientId = patchedClient.getId();
            updateProtocolMappers(realm, clientId, protocolMappers);
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
}
