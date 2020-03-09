package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.ClientRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
            clientRepository.create(realm, client);
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
