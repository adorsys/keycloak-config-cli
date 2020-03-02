package com.github.borisskert.keycloak.config.service.rolecomposites.realm;

import com.github.borisskert.keycloak.config.repository.ClientRepository;
import com.github.borisskert.keycloak.config.repository.RoleCompositeRepository;
import org.keycloak.representations.idm.RoleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service("realmRoleClientCompositeImport")
public class ClientCompositeImport {
    private static final Logger logger = LoggerFactory.getLogger(ClientCompositeImport.class);

    private final ClientRepository clientRepository;
    private final RoleCompositeRepository roleCompositeRepository;

    public ClientCompositeImport(
            ClientRepository clientRepository,
            RoleCompositeRepository roleCompositeRepository
    ) {
        this.clientRepository = clientRepository;
        this.roleCompositeRepository = roleCompositeRepository;
    }

    public void update(String realm, String realmRole, Map<String, List<String>> clientComposites) {
        for (Map.Entry<String, List<String>> clientCompositesByClients : clientComposites.entrySet()) {
            String clientId = clientCompositesByClients.getKey();
            List<String> clientCompositesByClient = clientCompositesByClients.getValue();

            updateClientComposites(realm, realmRole, clientId, clientCompositesByClient);
        }

        removeRealmRoleClientComposites(realm, realmRole, clientComposites);
    }

    private void updateClientComposites(String realm, String realmRole, String clientId, List<String> Composites) {
        List<String> existingClientCompositeNames = findRealmRoleClientCompositeNames(realm, realmRole, clientId);

        if (Objects.equals(existingClientCompositeNames, Composites)) {
            logger.debug("No need to update client-level role '{}'s composites client-roles for client '{}' in realm '{}'", realmRole, clientId, realm);
        } else {
            logger.debug("Update client-level role '{}'s composites client-roles for client '{}' in realm '{}'", realmRole, clientId, realm);

            removeRealmRoleClientComposites(realm, realmRole, clientId, existingClientCompositeNames, Composites);
            addRealmRoleClientComposites(realm, realmRole, clientId, existingClientCompositeNames, Composites);
        }
    }

    private List<String> findRealmRoleClientCompositeNames(String realm, String realmRole, String clientId) {
        Set<RoleRepresentation> existingClientComposites = roleCompositeRepository.findRealmRoleClientComposites(realm, realmRole, clientId);

        return existingClientComposites.stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());
    }

    private void removeRealmRoleClientComposites(String realm, String realmRole, String clientId, Collection<String> existingClientCompositeNames, Collection<String> clientCompositesByClient) {
        Set<String> clientRoleCompositesToRemove = existingClientCompositeNames.stream()
                .filter(name -> !clientCompositesByClient.contains(name))
                .collect(Collectors.toSet());

        roleCompositeRepository.removeRealmRoleClientComposites(realm, realmRole, clientId, clientRoleCompositesToRemove);
    }

    private void removeRealmRoleClientComposites(String realm, String realmRole, Map<String, List<String>> clientComposites) {
        Set<String> existingCompositeClients = clientRepository.getClientIds(realm);

        Set<String> compositeClientsToRemove = existingCompositeClients.stream()
                .filter(name -> !clientComposites.containsKey(name))
                .collect(Collectors.toSet());

        Map<String, List<String>> clientCompositesToRemove = estimateRealmCompositeRolesToBeRemoved(
                realm,
                realmRole,
                compositeClientsToRemove
        );

        roleCompositeRepository.removeRealmRoleClientComposites(realm, realmRole, clientCompositesToRemove);
    }

    private void addRealmRoleClientComposites(String realm, String realmRole, String clientId, Collection<String> existingClientCompositeNames, Collection<String> clientCompositesByClient) {
        Set<String> clientRoleCompositesToAdd = clientCompositesByClient.stream()
                .filter(name -> !existingClientCompositeNames.contains(name))
                .collect(Collectors.toSet());

        roleCompositeRepository.addRealmRoleClientComposites(realm, realmRole, clientId, clientRoleCompositesToAdd);
    }

    private Map<String, List<String>> estimateRealmCompositeRolesToBeRemoved(String realm, String roleName, Set<String> compositeClientsToRemove) {
        Map<String, List<String>> clientRolesToRemove = new HashMap<>();
        Map<String, List<String>> existingCompositesByClients = roleCompositeRepository.findRealmRoleClientComposites(realm, roleName);

        for (String clientId : compositeClientsToRemove) {
            if (existingCompositesByClients.containsKey(clientId)) {
                List<String> existingCompositesByClient = existingCompositesByClients.get(clientId);

                clientRolesToRemove.put(clientId, existingCompositesByClient);
            }
        }

        return clientRolesToRemove;
    }
}
