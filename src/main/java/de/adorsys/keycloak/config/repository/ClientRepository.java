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

package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.util.ResponseUtil;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ClientRepository {

    private final RealmRepository realmRepository;

    @Autowired
    public ClientRepository(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    public Optional<ClientRepresentation> tryToFindClient(String realm, String clientId) {
        Optional<ClientRepresentation> maybeClient;

        RealmResource realmResource = realmRepository.loadRealm(realm);
        ClientsResource clients = realmResource.clients();

        List<ClientRepresentation> foundClients = clients.findByClientId(clientId);

        if (foundClients.isEmpty()) {
            maybeClient = Optional.empty();
        } else {
            maybeClient = Optional.of(foundClients.get(0));
        }

        return maybeClient;
    }

    public ClientRepresentation getClientByClientId(String realm, String clientId) {
        return loadClientByClientId(realm, clientId);
    }

    public ClientRepresentation getClientById(String realm, String id) {
        return loadClientById(realm, id).toRepresentation();
    }

    public String getClientSecret(String realm, String clientId) {
        ClientResource clientResource = getClientResource(realm, clientId);
        return clientResource.getSecret().getValue();
    }

    public void create(String realm, ClientRepresentation client) {
        RealmResource realmResource = realmRepository.loadRealm(realm);
        ClientsResource clientsResource = realmResource.clients();

        try {
            Response response = clientsResource.create(client);
            ResponseUtil.validate(response);
        } catch (WebApplicationException error) {
            String errorMessage = ResponseUtil.getErrorMessage(error);

            throw new ImportProcessingException(
                    "Cannot create client '" + client.getClientId()
                            + "' in realm '" + realm + "'"
                            + ": " + errorMessage,
                    error
            );
        }
    }

    public void update(String realm, ClientRepresentation clientToUpdate) {
        RealmResource realmResource = realmRepository.loadRealm(realm);
        ClientsResource clientsResource = realmResource.clients();
        ClientResource clientResource = clientsResource.get(clientToUpdate.getId());

        clientResource.update(clientToUpdate);
    }

    private ClientRepresentation loadClientByClientId(String realm, String clientId) {
        List<ClientRepresentation> foundClients = realmRepository.loadRealm(realm)
                .clients()
                .findByClientId(clientId);

        if (foundClients.isEmpty()) {
            throw new KeycloakRepositoryException("Cannot find client by clientId '" + clientId + "'");
        }

        return foundClients.get(0);
    }

    private ClientResource loadClientById(String realm, String id) {
        ClientResource client = realmRepository.loadRealm(realm)
                .clients()
                .get(id);

        if (client == null) {
            throw new KeycloakRepositoryException("Cannot find client by id '" + id + "'");
        }

        return client;
    }

    final ClientResource getClientResource(String realm, String clientId) {
        ClientRepresentation client = loadClientByClientId(realm, clientId);
        return realmRepository.loadRealm(realm)
                .clients()
                .get(client.getId());
    }

    public final Set<String> getClientIds(String realm) {
        return realmRepository.loadRealm(realm)
                .clients()
                .findAll()
                .stream()
                .map(ClientRepresentation::getClientId)
                .collect(Collectors.toSet());
    }

    public final List<ClientRepresentation> getClients(String realm) {
        return realmRepository.loadRealm(realm)
                .clients()
                .findAll();
    }

    public void addProtocolMappers(String realm, String clientId, List<ProtocolMapperRepresentation> protocolMappers) {
        ClientResource clientResource = loadClientById(realm, clientId);
        ProtocolMappersResource protocolMappersResource = clientResource.getProtocolMappers();

        for (ProtocolMapperRepresentation protocolMapper : protocolMappers) {
            Response response = protocolMappersResource.createMapper(protocolMapper);
            ResponseUtil.validate(response);
        }
    }

    public void removeProtocolMappers(String realm, String clientId, List<ProtocolMapperRepresentation> protocolMappers) {
        ClientResource clientResource = loadClientById(realm, clientId);
        ProtocolMappersResource protocolMappersResource = clientResource.getProtocolMappers();

        List<ProtocolMapperRepresentation> existingProtocolMappers = clientResource.getProtocolMappers().getMappers();
        List<ProtocolMapperRepresentation> protocolMapperToRemove = existingProtocolMappers.stream().filter(em -> protocolMappers.stream().anyMatch(m -> Objects.equals(m.getName(), em.getName()))).collect(Collectors.toList());

        for (ProtocolMapperRepresentation protocolMapper : protocolMapperToRemove) {
            protocolMappersResource.delete(protocolMapper.getId());
        }
    }

    public void updateProtocolMappers(String realm, String clientId, List<ProtocolMapperRepresentation> protocolMappers) {
        ClientResource clientResource = loadClientById(realm, clientId);
        ProtocolMappersResource protocolMappersResource = clientResource.getProtocolMappers();

        for (ProtocolMapperRepresentation protocolMapper : protocolMappers) {
            try {
                protocolMappersResource.update(protocolMapper.getId(), protocolMapper);
            } catch (WebApplicationException error) {
                String errorMessage = ResponseUtil.getErrorMessage(error);
                throw new ImportProcessingException(
                        "Cannot update protocolMapper '" + protocolMapper.getName()
                                + "' for client '" + clientResource.toRepresentation().getClientId()
                                + "' for realm '" + realm + "'"
                                + ": " + errorMessage,
                        error
                );
            }
        }
    }
}
