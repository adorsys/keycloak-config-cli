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

        if(clients != null) {
            for(ClientRepresentation client : clients) {
                String clientId = client.getClientId();
                String realm = realmImport.getRealm();

                Optional<ClientRepresentation> maybeClient = clientRepository.tryToFindClient(realm, clientId);

                if(maybeClient.isPresent()) {
                    if(logger.isDebugEnabled()) logger.debug("Update client '{}' in realm '{}'", clientId, realm);
                    updateClient(realm, maybeClient.get(), client);
                } else {
                    if(logger.isDebugEnabled()) logger.debug("Create client '{}' in realm '{}'", clientId, realm);
                    clientRepository.create(realm, client);
                }
            }
        }
    }

    private void updateClient(String realm, ClientRepresentation existingClient, ClientRepresentation clientToImport) {
        ClientRepresentation patchedClient = CloneUtils.patch(existingClient, clientToImport);
        clientRepository.update(realm, patchedClient);
    }
}
