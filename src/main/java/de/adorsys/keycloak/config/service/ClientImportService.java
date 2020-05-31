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
import de.adorsys.keycloak.config.repository.ClientRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.jboss.logging.Logger;
import org.keycloak.representations.idm.ClientRepresentation;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

@Dependent
public class ClientImportService {
    private static final Logger LOG = Logger.getLogger(ClientImportService.class);

    @Inject
    ClientRepository clientRepository;


    public void doImport(RealmImport realmImport) {
        List<ClientRepresentation> clients = realmImport.getClients();
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
            LOG.debugf("Create client '%s' in realm '%s'", clientId, realm);
            clientRepository.create(realm, client);
        }
    }

    private void updateClientIfNeeded(String realm, ClientRepresentation clientToUpdate, ClientRepresentation existingClient) {
        if (!areClientsEqual(realm, clientToUpdate, existingClient)) {
            LOG.debugf("Update client '%s' in realm '%s'", clientToUpdate.getClientId(), realm);
            updateClient(realm, existingClient, clientToUpdate);
        } else {
            LOG.debugf("No need to update client '%s' in realm '%s'", clientToUpdate.getClientId(), realm);
        }
    }

    private boolean areClientsEqual(String realm, ClientRepresentation clientToUpdate, ClientRepresentation existingClient) {
        if (CloneUtils.deepEquals(clientToUpdate, existingClient, "id", "secret")) {
            String clientSecret = clientRepository.getClientSecret(realm, clientToUpdate.getClientId());
            return clientSecret.equals(clientToUpdate.getSecret());
        }

        return false;
    }

    private void updateClient(String realm, ClientRepresentation existingClient, ClientRepresentation clientToImport) {
        ClientRepresentation patchedClient = CloneUtils.patch(existingClient, clientToImport, "id");
        clientRepository.update(realm, patchedClient);
    }
}
