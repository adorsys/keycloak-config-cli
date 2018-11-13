package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;

import java.util.List;
import java.util.Optional;

public class RealmClientImportService {

    private final RealmImport realmImport;
    private final RealmResource realmResource;

    public RealmClientImportService(RealmImport realmImport, RealmResource realmResource) {
        this.realmImport = realmImport;
        this.realmResource = realmResource;
    }

    public void doImport() {
        List<ClientRepresentation> clients = realmImport.getClients();

        if(clients != null) {
            for(ClientRepresentation client : clients) {
                Optional<ClientRepresentation> maybeClient = tryToFindClient(client.getClientId());

                if(maybeClient.isPresent()) {
                    updateClient(maybeClient.get(), client);
                } else {
                    createClient(client);
                }
            }
        }
    }

    private void createClient(ClientRepresentation client) {
        realmResource.clients().create(client);
    }

    private void updateClient(ClientRepresentation existingClient, ClientRepresentation clientToImport) {
        ClientRepresentation patchedClient = CloneUtils.deepPatch(existingClient, clientToImport);
        ClientResource clientResource = realmResource.clients().get(existingClient.getId());

        clientResource.update(patchedClient);
    }

    private Optional<ClientRepresentation> tryToFindClient(String clientId) {
        Optional<ClientRepresentation> maybeClient;

        List<ClientRepresentation> foundClients = realmResource.clients().findByClientId(clientId);

        if(foundClients.isEmpty()) {
            maybeClient = Optional.empty();
        } else {
            maybeClient = Optional.of(foundClients.get(0));
        }

        return maybeClient;
    }
}
