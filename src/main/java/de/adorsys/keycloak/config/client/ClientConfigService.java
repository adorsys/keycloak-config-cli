package de.adorsys.keycloak.config.client;

import de.adorsys.keycloak.config.realm.RealmConfigService;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClientConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(RealmConfigService.class);
    private final Keycloak keycloak;

    @Autowired
    public ClientConfigService(Keycloak keycloak) {
        this.keycloak = keycloak;
    }


    public void handleClients(String realmId, List<ClientRepresentation> clientConfigurations) {
        ClientsResource clientsResource = keycloak.realms().realm(realmId).clients();
        for (ClientRepresentation clientRepresentation : clientConfigurations) {
            handleClient(clientsResource, realmId, clientRepresentation);
        }
        List<String> definedClientIds = clientConfigurations.stream().map(cc -> cc.getClientId()).collect(Collectors.toList());
        deleteUndefinedClients(clientsResource, definedClientIds);
    }

    private void handleClient(ClientsResource clientsResource, String realmId, ClientRepresentation clientRepresentation) {
        ClientResource clientResource;
        List<ClientRepresentation> clientsById = clientsResource.findByClientId(clientRepresentation.getClientId());
        clientsById.stream().forEach(client -> clientsResource.get(client.getId()).remove());

        Response response = clientsResource.create(clientRepresentation);
        if (response.getStatus() < 400) {
            LOG.debug("Creating client '{}' in realm '{}'.", clientRepresentation.getClientId(), realmId);
            clientResource = keycloak.proxy(ClientResource.class, response.getLocation());
        } else {
            throw new RuntimeException("Could not create client: " + clientRepresentation.getId());
        }
        response.close();
        assignScopedRoles(clientRepresentation, clientResource);
    }

    private void deleteUndefinedClients(ClientsResource clientsResource, List<String> definedClientIds) {
        clientsResource.findAll().stream().filter(c -> !definedClientIds.contains(c.getName())).forEach(c -> clientsResource.get(c.getId()).remove());
    }

    private void assignScopedRoles(ClientRepresentation clientRepresentation, ClientResource clientResource) {
        LOG.debug("Assign scoped roles for '{}'", clientRepresentation.getName());
        final List<ProtocolMapperRepresentation> protocolMappers = clientRepresentation.getProtocolMappers();
        if (protocolMappers != null) {
            List<String> assignedRoles = protocolMappers.stream().filter(pm -> "oidc-role-name-mapper".equals(pm.getProtocolMapper())).map(m -> m.getConfig().get("role")).collect(Collectors.toList());
            List<RoleRepresentation> scopedRoles = clientResource.getScopeMappings().realmLevel().listAvailable().stream().filter(r -> assignedRoles.contains(r.getName())).collect(Collectors.toList());
            clientResource.getScopeMappings().realmLevel().add(scopedRoles);
        }
    }
}
