package com.github.borisskert.keycloak.config.service;

import com.github.borisskert.keycloak.config.model.RealmImport;
import com.github.borisskert.keycloak.config.repository.ClientRepository;
import com.github.borisskert.keycloak.config.repository.RoleRepository;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClientRoleCompositeImportService {
    private static final Logger logger = LoggerFactory.getLogger(ClientRoleCompositeImportService.class);

    private final RoleRepository roleRepository;
    private final ClientRepository clientRepository;

    @Autowired
    public ClientRoleCompositeImportService(RoleRepository roleRepository, ClientRepository clientRepository) {
        this.roleRepository = roleRepository;
        this.clientRepository = clientRepository;
    }

    public void updateClientRoleComposites(RealmImport realmImport) {
        String realm = realmImport.getRealm();
        RolesRepresentation roles = realmImport.getRoles();

        Map<String, List<RoleRepresentation>> clientRolesPerClient = roles.getClient();

        for (Map.Entry<String, List<RoleRepresentation>> clientRoles : clientRolesPerClient.entrySet()) {
            String clientId = clientRoles.getKey();

            for (RoleRepresentation clientRole : clientRoles.getValue()) {
                updateClientRoleRealmCompositesIfNecessary(realm, clientId, clientRole);
                updateClientRoleClientCompositesIfNecessary(realm, clientId, clientRole);
            }
        }
    }

    private void updateClientRoleRealmCompositesIfNecessary(String realm, String roleClientId, RoleRepresentation clientRole) {
        Optional.ofNullable(clientRole.getComposites())
                .flatMap(composites -> Optional.ofNullable(composites.getRealm()))
                .ifPresent(realmComposites -> {
                    String roleName = clientRole.getName();
                    Set<String> existingRealmCompositeNames = findClientRoleRealmCompositeNames(realm, roleClientId, roleName);

                    if (Objects.equals(realmComposites, existingRealmCompositeNames)) {
                        logger.debug("No need to update client-level role '{}'s composites realm-roles in realm '{}'", roleName, realm);
                    } else {
                        logger.debug("Update client-level role '{}'s composites realm-roles in realm '{}'", roleName, realm);
                        updateClientRoleRealmComposites(realm, roleClientId, roleName, realmComposites, existingRealmCompositeNames);
                    }
                });
    }

    private Set<String> findClientRoleRealmCompositeNames(String realm, String roleClientId, String roleName) {
        Set<RoleRepresentation> existingRealmComposites = roleRepository.findClientRoleRealmComposites(realm, roleClientId, roleName);

        return existingRealmComposites.stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());
    }

    private void updateClientRoleRealmComposites(String realm, String roleClientId, String roleName, Set<String> realmComposites, Set<String> existingRealmCompositeNames) {
        removeClientRoleRealmComposites(realm, roleClientId, roleName, existingRealmCompositeNames, realmComposites);
        addClientRoleRealmComposites(realm, roleClientId, roleName, existingRealmCompositeNames, realmComposites);
    }

    private void removeClientRoleRealmComposites(String realm, String roleClientId, String roleName, Set<String> existingRealmCompositeNames, Set<String> realmComposites) {
        Set<String> realmCompositesToRemove = existingRealmCompositeNames.stream()
                .filter(name -> !realmComposites.contains(name))
                .collect(Collectors.toSet());

        roleRepository.removeClientRoleRealmComposites(realm, roleClientId, roleName, realmCompositesToRemove);
    }

    private void addClientRoleRealmComposites(String realm, String roleClientId, String roleName, Set<String> existingRealmCompositeNames, Set<String> realmComposites) {
        Set<String> realmCompositesToAdd = realmComposites.stream()
                .filter(name -> !existingRealmCompositeNames.contains(name))
                .collect(Collectors.toSet());

        roleRepository.addClientRoleRealmComposites(
                realm,
                roleClientId,
                roleName,
                realmCompositesToAdd
        );
    }

    private void updateClientRoleClientCompositesIfNecessary(String realm, String roleClientId, RoleRepresentation clientRole) {
        Optional.ofNullable(clientRole.getComposites())
                .flatMap(composites -> Optional.ofNullable(composites.getClient()))
                .ifPresent(clientComposites -> updateClientRoleClientComposites(realm, roleClientId, clientRole.getName(), clientComposites));
    }

    private void updateClientRoleClientComposites(String realm, String roleClientId, String roleName, Map<String, List<String>> clientComposites) {
        for (Map.Entry<String, List<String>> clientCompositesByClients : clientComposites.entrySet()) {
            String clientId = clientCompositesByClients.getKey();
            List<String> clientCompositesByClient = clientCompositesByClients.getValue();

            List<String> existingClientCompositeNames = findClientRoleClientCompositeNames(realm, roleClientId, roleName, clientId);

            if (Objects.equals(existingClientCompositeNames, clientCompositesByClient)) {
                logger.debug("No need to update client-level role '{}'s composites client-roles for client '{}' in realm '{}'", roleName, clientId, realm);
            } else {
                logger.debug("Update client-level role '{}'s composites client-roles for client '{}' in realm '{}'", roleName, clientId, realm);

                removeClientRoleClientComposites(realm, roleClientId, roleName, clientId, existingClientCompositeNames, clientCompositesByClient);
                addClientRoleClientComposites(realm, roleClientId, roleName, clientId, existingClientCompositeNames, clientCompositesByClient);
            }
        }

        removeClientRoleClientComposites(realm, roleClientId, roleName, clientComposites);
    }

    private List<String> findClientRoleClientCompositeNames(String realm, String roleClientId, String realmRole, String clientId) {
        Set<RoleRepresentation> existingClientComposites = roleRepository.findClientRoleClientComposites(realm, roleClientId, realmRole, clientId);

        return existingClientComposites.stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());
    }

    private void removeClientRoleClientComposites(String realm, String roleClientId, String realmRole, Map<String, List<String>> clientComposites) {
        Set<String> existingCompositeClients = clientRepository.getClientIds(realm);

        Set<String> compositeClientsToRemove = existingCompositeClients.stream()
                .filter(name -> !clientComposites.containsKey(name))
                .collect(Collectors.toSet());

        roleRepository.removeClientRoleClientComposites(realm, roleClientId, realmRole, compositeClientsToRemove);
    }

    private void removeClientRoleClientComposites(String realm, String roleClientId, String realmRole, String clientId, Collection<String> existingClientCompositeNames, Collection<String> clientCompositesByClient) {
        Set<String> clientRoleCompositesToRemove = existingClientCompositeNames.stream()
                .filter(name -> !clientCompositesByClient.contains(name))
                .collect(Collectors.toSet());

        roleRepository.removeClientRoleClientComposites(realm, roleClientId, realmRole, clientId, clientRoleCompositesToRemove);
    }

    private void addClientRoleClientComposites(String realm, String clientRoleId, String realmRole, String clientId, Collection<String> existingClientCompositeNames, Collection<String> clientCompositesByClient) {
        Set<String> clientRoleCompositesToAdd = clientCompositesByClient.stream()
                .filter(name -> !existingClientCompositeNames.contains(name))
                .collect(Collectors.toSet());

        roleRepository.addClientRoleClientComposites(realm, clientRoleId, realmRole, clientId, clientRoleCompositesToAdd);
    }
}
