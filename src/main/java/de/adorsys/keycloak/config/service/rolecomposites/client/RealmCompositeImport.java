package com.github.borisskert.keycloak.config.service.rolecomposites.client;

import com.github.borisskert.keycloak.config.repository.RoleCompositeRepository;
import org.keycloak.representations.idm.RoleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service("clientRoleRealmCompositeImport")
public class RealmCompositeImport {
    private static final Logger logger = LoggerFactory.getLogger(RealmCompositeImport.class);

    private final RoleCompositeRepository roleCompositeRepository;

    @Autowired
    public RealmCompositeImport(RoleCompositeRepository roleCompositeRepository) {
        this.roleCompositeRepository = roleCompositeRepository;
    }

    public void update(String realm, String roleClientId, RoleRepresentation clientRole, Set<String> realmComposites) {
        String roleName = clientRole.getName();
        Set<String> existingRealmCompositeNames = findClientRoleRealmCompositeNames(realm, roleClientId, roleName);

        if (Objects.equals(realmComposites, existingRealmCompositeNames)) {
            logger.debug("No need to update client-level role '{}'s composites realm-roles in realm '{}'", roleName, realm);
        } else {
            logger.debug("Update client-level role '{}'s composites realm-roles in realm '{}'", roleName, realm);
            updateClientRoleRealmComposites(realm, roleClientId, roleName, realmComposites, existingRealmCompositeNames);
        }
    }

    private Set<String> findClientRoleRealmCompositeNames(String realm, String roleClientId, String roleName) {
        Set<RoleRepresentation> existingRealmComposites = roleCompositeRepository.findClientRoleRealmComposites(realm, roleClientId, roleName);

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

        roleCompositeRepository.removeClientRoleRealmComposites(realm, roleClientId, roleName, realmCompositesToRemove);
    }

    private void addClientRoleRealmComposites(String realm, String roleClientId, String roleName, Set<String> existingRealmCompositeNames, Set<String> realmComposites) {
        Set<String> realmCompositesToAdd = realmComposites.stream()
                .filter(name -> !existingRealmCompositeNames.contains(name))
                .collect(Collectors.toSet());

        roleCompositeRepository.addClientRoleRealmComposites(
                realm,
                roleClientId,
                roleName,
                realmCompositesToAdd
        );
    }
}
