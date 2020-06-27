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
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
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
import java.util.function.Consumer;

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
            try {
                clientRepository.create(realm, client);
            } catch (KeycloakRepositoryException error) {
                throw new ImportProcessingException("Cannot create client '" + client.getClientId() + "' for realm '" + realm + "': " + error.getMessage(), error);
            }
        }
    }

    private void updateClientIfNeeded(String realm, ClientRepresentation clientToUpdate, ClientRepresentation existingClient) {
        ClientRepresentation patchedClient = CloneUtil.patch(existingClient, clientToUpdate, "id", "access");

        if (!isClientEqual(realm, existingClient, patchedClient)) {
            logger.debug("Update client '{}' in realm '{}'", clientToUpdate.getClientId(), realm);
            updateClient(realm, patchedClient);
        } else {
            logger.debug("No need to update client '{}' in realm '{}'", clientToUpdate.getClientId(), realm);
        }
    }

    private boolean isClientEqual(String realm, ClientRepresentation existingClient, ClientRepresentation patchedClient) {
        if (!CloneUtil.deepEquals(existingClient, patchedClient, "id", "secret", "access", "protocolMappers")) {
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
