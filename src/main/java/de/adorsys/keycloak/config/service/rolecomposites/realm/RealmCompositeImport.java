package com.github.borisskert.keycloak.config.service.rolecomposites.realm;

import com.github.borisskert.keycloak.config.repository.RoleRepository;
import org.keycloak.representations.idm.RoleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service("realmRoleRealmCompositeImport")
public class RealmCompositeImport {
    private static final Logger logger = LoggerFactory.getLogger(RealmCompositeImport.class);

    private final RoleRepository roleRepository;

    @Autowired
    public RealmCompositeImport(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public void update(String realm, RoleRepresentation realmRole, Set<String> realmComposites) {
        String roleName = realmRole.getName();

        Set<String> existingRealmCompositeNames = findRealmRoleRealmCompositeNames(realm, roleName);

        if (Objects.equals(realmComposites, existingRealmCompositeNames)) {
            logger.debug("No need to update realm-level role '{}'s composites realm-roles in realm '{}'", roleName, realm);
        } else {
            logger.debug("Update realm-level role '{}'s composites realm-roles in realm '{}'", roleName, realm);

            updateRealmRoleRealmComposites(realm, roleName, existingRealmCompositeNames, realmComposites);
        }
    }

    private Set<String> findRealmRoleRealmCompositeNames(String realm, String roleName) {
        Set<RoleRepresentation> existingRealmComposites = roleRepository.findRealmRoleRealmComposites(realm, roleName);

        return existingRealmComposites.stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());
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
}
