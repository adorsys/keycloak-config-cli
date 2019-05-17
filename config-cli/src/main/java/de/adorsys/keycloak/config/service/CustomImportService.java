package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.ClientRepository;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomImportService {
    private static final Logger logger = LoggerFactory.getLogger(CustomImportService.class);

    private final KeycloakProvider keycloakProvider;

    private final ClientRepository clientRepository;

    @Autowired
    public CustomImportService(KeycloakProvider keycloakProvider, ClientRepository clientRepository) {
        this.keycloakProvider = keycloakProvider;
        this.clientRepository = clientRepository;
    }

    public void doImport(RealmImport realmImport) {
        realmImport.getCustomImport().ifPresent(customImport -> setupImpersonation(realmImport, customImport));
    }

    private void setupImpersonation(RealmImport realmImport, RealmImport.CustomImport customImport) {
        if(customImport.removeImpersonation()) {
            removeImpersonation(realmImport);
        }
    }

    private void removeImpersonation(RealmImport realmImport) {
        RealmResource master = keycloakProvider.get().realm("master");

        String clientId = realmImport.getRealm() + "-realm";
        List<ClientRepresentation> foundClients = master.clients()
                .findByClientId(clientId);

        if(!foundClients.isEmpty()) {
            removeImpersonationRoleFromClient(master, clientId);
        }
    }

    private void removeImpersonationRoleFromClient(RealmResource master, String clientId) {
        ClientRepresentation client = clientRepository.getClient("master", clientId);
        ClientResource clientResource = master.clients()
                .get(client.getId());

        RoleResource impersonationRole = clientResource.roles().get("impersonation");

        try {
            logger.debug("Remove role 'impersonation' from client '{}' in realm 'master'", clientId);

            impersonationRole.remove();
        } catch(javax.ws.rs.NotFoundException e) {
            logger.info("Cannot remove 'impersonation' role from client '{}' in 'master' realm: Not found", clientId);
        }
    }
}
