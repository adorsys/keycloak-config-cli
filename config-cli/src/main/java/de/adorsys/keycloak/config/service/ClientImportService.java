package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.ClientRepository;
import de.adorsys.keycloak.config.repository.RealmRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClientImportService {

    private final RealmRepository realmRepository;
    private final ClientRepository clientRepository;

    @Autowired
    public ClientImportService(
            RealmRepository realmRepository,
            ClientRepository clientRepository
    ) {
        this.realmRepository = realmRepository;
        this.clientRepository = clientRepository;
    }

    public void doImport(RealmImport realmImport) {
        List<ClientRepresentation> clients = realmImport.getClients();

        if(clients != null) {
            for(ClientRepresentation client : clients) {
                Optional<ClientRepresentation> maybeClient = clientRepository.tryToFindClient(realmImport.getRealm(), client.getClientId());

                if(maybeClient.isPresent()) {
                    updateClient(realmImport.getRealm(), maybeClient.get(), client);
                } else {
                    createClient(realmImport.getRealm(), client);
                }
            }
        }
    }

    private void createClient(String realm, ClientRepresentation client) {
        realmRepository.loadRealm(realm).clients().create(client);
    }

    private void updateClient(String realm, ClientRepresentation existingClient, ClientRepresentation clientToImport) {
        ClientRepresentation patchedClient = CloneUtils.patch(existingClient, clientToImport);
        ClientResource clientResource = realmRepository.loadRealm(realm).clients().get(existingClient.getId());

        clientResource.update(patchedClient);
    }
}
