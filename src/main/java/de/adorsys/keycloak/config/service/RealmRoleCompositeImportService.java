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
public class RealmRoleCompositeImportService {

    private static final Logger logger = LoggerFactory.getLogger(RealmRoleCompositeImportService.class);

    private final RoleRepository roleRepository;
    private final ClientRepository clientRepository;

    @Autowired
    public RealmRoleCompositeImportService(RoleRepository roleRepository, ClientRepository clientRepository) {
        this.roleRepository = roleRepository;
        this.clientRepository = clientRepository;
    }

    public void updateRealmRoleComposites(RealmImport realmImport) {
        String realm = realmImport.getRealm();
        RolesRepresentation roles = realmImport.getRoles();
        List<RoleRepresentation> realmRoles = roles.getRealm();

        for (RoleRepresentation realmRole : realmRoles) {
            updateRealmRoleRealmCompositesIfNecessary(realm, realmRole);
            updateRealmRoleClientCompositesIfNecessary(realm, realmRole);
        }
    }

    private void updateRealmRoleRealmCompositesIfNecessary(String realm, RoleRepresentation realmRole) {
        Optional.ofNullable(realmRole.getComposites())
                .flatMap(composites -> Optional.ofNullable(composites.getRealm()))
                .ifPresent(realmComposites -> {
                    String roleName = realmRole.getName();

                    Set<String> existingRealmCompositeNames = findRealmRoleRealmCompositeNames(realm, roleName);

                    if (Objects.equals(realmComposites, existingRealmCompositeNames)) {
                        logger.debug("No need to update realm-level role '{}'s composites realm-roles in realm '{}'", roleName, realm);
                    } else {
                        logger.debug("Update realm-level role '{}'s composites realm-roles in realm '{}'", roleName, realm);

                        updateRealmRoleRealmComposites(realm, roleName, existingRealmCompositeNames, realmComposites);
                    }
                });
    }

    private void updateRealmRoleClientCompositesIfNecessary(String realm, RoleRepresentation realmRole) {
        Optional.ofNullable(realmRole.getComposites())
                .flatMap(composites -> Optional.ofNullable(composites.getClient()))
                .ifPresent(clientComposites -> updateRealmRoleClientComposites(realm, realmRole.getName(), clientComposites));
    }

    private void updateRealmRoleClientComposites(String realm, String realmRole, Map<String, List<String>> clientComposites) {
        for (Map.Entry<String, List<String>> clientCompositesByClients : clientComposites.entrySet()) {
            String clientId = clientCompositesByClients.getKey();
            List<String> clientCompositesByClient = clientCompositesByClients.getValue();

            List<String> existingClientCompositeNames = findRealmRoleClientCompositeNames(realm, realmRole, clientId);

            if (Objects.equals(existingClientCompositeNames, clientCompositesByClient)) {
                logger.debug("No need to update client-level role '{}'s composites client-roles for client '{}' in realm '{}'", realmRole, clientId, realm);
            } else {
                logger.debug("Update client-level role '{}'s composites client-roles for client '{}' in realm '{}'", realmRole, clientId, realm);

                removeRealmRoleClientComposites(realm, realmRole, clientId, existingClientCompositeNames, clientCompositesByClient);
                addRealmRoleClientComposites(realm, realmRole, clientId, existingClientCompositeNames, clientCompositesByClient);
            }
        }

        removeRealmRoleClientComposites(realm, realmRole, clientComposites);
    }

    private void removeRealmRoleClientComposites(String realm, String realmRole, Map<String, List<String>> clientComposites) {
        Set<String> existingCompositeClients = clientRepository.getClientIds(realm);

        Set<String> compositeClientsToRemove = existingCompositeClients.stream()
                .filter(name -> !clientComposites.containsKey(name))
                .collect(Collectors.toSet());

        roleRepository.removeRealmRoleClientComposites(realm, realmRole, compositeClientsToRemove);
    }

    private void addRealmRoleClientComposites(String realm, String realmRole, String clientId, Collection<String> existingClientCompositeNames, Collection<String> clientCompositesByClient) {
        Set<String> clientRoleCompositesToAdd = clientCompositesByClient.stream()
                .filter(name -> !existingClientCompositeNames.contains(name))
                .collect(Collectors.toSet());

        roleRepository.addRealmRoleClientComposites(realm, realmRole, clientId, clientRoleCompositesToAdd);
    }

    private void removeRealmRoleClientComposites(String realm, String realmRole, String clientId, Collection<String> existingClientCompositeNames, Collection<String> clientCompositesByClient) {
        Set<String> clientRoleCompositesToRemove = existingClientCompositeNames.stream()
                .filter(name -> !clientCompositesByClient.contains(name))
                .collect(Collectors.toSet());

        roleRepository.removeRealmRoleClientComposites(realm, realmRole, clientId, clientRoleCompositesToRemove);
    }

    private List<String> findRealmRoleClientCompositeNames(String realm, String realmRole, String clientId) {
        Set<RoleRepresentation> existingClientComposites = roleRepository.findRealmRoleClientComposites(realm, realmRole, clientId);

        return existingClientComposites.stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());
    }

    private void updateRealmRoleRealmComposites(String realm, String roleName, Set<String> existingRealmCompositeNames, Set<String> realmComposites) {
        removeRealmRoleRealmComposites(realm, roleName, existingRealmCompositeNames, realmComposites);
        addRealmRoleRealmComposites(realm, roleName, existingRealmCompositeNames, realmComposites);
    }

    private void removeRealmRoleRealmComposites(String realm, String roleName, Set<String> existingRealmCompositeNames, Set<String> realmComposites) {
        Set<String> realmCompositesToRemove = existingRealmCompositeNames.stream()
                .filter(name -> !realmComposites.contains(name))
                .collect(Collectors.toSet());

        roleRepository.removeRealmRoleRealmComposites(realm, roleName, realmCompositesToRemove);
    }

    private void addRealmRoleRealmComposites(String realm, String roleName, Set<String> existingRealmCompositeNames, Set<String> realmComposites) {
        Set<String> realmCompositesToAdd = realmComposites.stream()
                .filter(name -> !existingRealmCompositeNames.contains(name))
                .collect(Collectors.toSet());

        roleRepository.addRealmRoleRealmComposites(
                realm,
                roleName,
                realmCompositesToAdd
        );
    }

    private Set<String> findRealmRoleRealmCompositeNames(String realm, String roleName) {
        Set<RoleRepresentation> existingRealmComposites = roleRepository.findRealmRoleRealmComposites(realm, roleName);

        return existingRealmComposites.stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());
    }

}
