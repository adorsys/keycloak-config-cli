package com.github.borisskert.keycloak.config.service.rolecomposites.client;

import com.github.borisskert.keycloak.config.repository.ClientRepository;
import com.github.borisskert.keycloak.config.repository.RoleCompositeRepository;
import org.keycloak.representations.idm.RoleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service("clientRoleClientCompositeImport")
public class ClientCompositeImport {
    private static final Logger logger = LoggerFactory.getLogger(ClientCompositeImport.class);

    private final ClientRepository clientRepository;
    private final RoleCompositeRepository roleCompositeRepository;

    @Autowired
    public ClientCompositeImport(
            ClientRepository clientRepository,
            RoleCompositeRepository roleCompositeRepository
    ) {
        this.clientRepository = clientRepository;
        this.roleCompositeRepository = roleCompositeRepository;
    }

    public void update(String realm, String roleClientId, String roleName, Map<String, List<String>> clientComposites) {
        for (Map.Entry<String, List<String>> clientCompositesByClients : clientComposites.entrySet()) {
            String clientId = clientCompositesByClients.getKey();
            List<String> clientCompositesByClient = clientCompositesByClients.getValue();

            updateClientComposites(realm, roleClientId, roleName, clientId, clientCompositesByClient);
        }

        removeClientRoleClientComposites(realm, roleClientId, roleName, clientComposites);
    }

    private void updateClientComposites(
            String realm,
            String roleClientId,
            String roleName,
            String clientId,
            List<String> composites
    ) {
        List<String> existingClientCompositeNames = findClientRoleClientCompositeNames(realm, roleClientId, roleName, clientId);

        if (Objects.equals(existingClientCompositeNames, composites)) {
            logger.debug("No need to update client-level role '{}'s composites client-roles for client '{}' in realm '{}'", roleName, clientId, realm);
        } else {
            logger.debug("Update client-level role '{}'s composites client-roles for client '{}' in realm '{}'", roleName, clientId, realm);

            removeClientRoleClientComposites(realm, roleClientId, roleName, clientId, existingClientCompositeNames, composites);
            addClientRoleClientComposites(realm, roleClientId, roleName, clientId, existingClientCompositeNames, composites);
        }
    }

    private List<String> findClientRoleClientCompositeNames(
            String realm,
            String roleClientId,
            String realmRole,
            String clientId
    ) {
        Set<RoleRepresentation> existingClientComposites = roleCompositeRepository.findClientRoleClientComposites(realm, roleClientId, realmRole, clientId);

        return existingClientComposites.stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());
    }

    private void removeClientRoleClientComposites(
            String realm,
            String roleClientId,
            String realmRole,
            String clientId,
            Collection<String> existingClientCompositeNames,
            Collection<String> clientCompositesByClient
    ) {
        Set<String> clientRoleCompositesToRemove = existingClientCompositeNames.stream()
                .filter(name -> !clientCompositesByClient.contains(name))
                .collect(Collectors.toSet());

        roleCompositeRepository.removeClientRoleClientComposites(realm, roleClientId, realmRole, clientId, clientRoleCompositesToRemove);
    }

    private void addClientRoleClientComposites(
            String realm,
            String clientRoleId,
            String realmRole,
            String clientId,
            Collection<String> existingClientCompositeNames,
            Collection<String> clientCompositesByClient
    ) {
        Set<String> clientRoleCompositesToAdd = clientCompositesByClient.stream()
                .filter(name -> !existingClientCompositeNames.contains(name))
                .collect(Collectors.toSet());

        roleCompositeRepository.addClientRoleClientComposites(realm, clientRoleId, realmRole, clientId, clientRoleCompositesToAdd);
    }

    private void removeClientRoleClientComposites(
            String realm,
            String roleClientId,
            String realmRole,
            Map<String, List<String>> clientComposites
    ) {
        Set<String> existingCompositeClients = clientRepository.getClientIds(realm);

        Set<String> compositeClientsToRemove = existingCompositeClients.stream()
                .filter(name -> !clientComposites.containsKey(name))
                .collect(Collectors.toSet());

        Map<String, List<String>> clientCompositeRolesToBeRemoved = estimateClientCompositeRolesToBeRemoved(
                realm,
                roleClientId,
                realmRole,
                compositeClientsToRemove
        );

        roleCompositeRepository.removeClientRoleClientComposites(realm, roleClientId, realmRole, clientCompositeRolesToBeRemoved);
    }

    private Map<String, List<String>> estimateClientCompositeRolesToBeRemoved(String realm, String roleClientId, String roleName, Set<String> compositeClientsToRemove) {
        Map<String, List<String>> existingClientCompositeNames = roleCompositeRepository.findClientRoleClientComposites(
                realm,
                roleClientId,
                roleName
        );

        Map<String, List<String>> clientRolesToRemove = new HashMap<>();
        for (String clientId : compositeClientsToRemove) {
            if (existingClientCompositeNames.containsKey(clientId)) {
                clientRolesToRemove.put(clientId, existingClientCompositeNames.get(clientId));
            }
        }

        return clientRolesToRemove;
    }
}
