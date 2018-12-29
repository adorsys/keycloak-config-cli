package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.RoleRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RoleImportService {

    private final RoleRepository roleRepository;

    @Autowired
    public RoleImportService(
            RoleRepository roleRepository
    ) {
        this.roleRepository = roleRepository;
    }

    public void doImport(RealmImport realmImport) {
        createOrUpdateRealmRoles(realmImport);
        createOrUpdateClientRoles(realmImport);
    }

    private void createOrUpdateRealmRoles(RealmImport realmImport) {
        RolesRepresentation roles = realmImport.getRoles();
        List<RoleRepresentation> realmRoles = roles.getRealm();

        for(RoleRepresentation role : realmRoles) {
            Optional<RoleRepresentation> maybeRole = roleRepository.tryToFindRealmRole(realmImport.getRealm(), role.getName());

            if(maybeRole.isPresent()) {
                updateRealmRole(realmImport.getRealm(), maybeRole.get(), role);
            } else {
                roleRepository.createRealmRole(realmImport.getRealm(), role);
            }
        }
    }

    private void createOrUpdateClientRoles(RealmImport realmImport) {
        RolesRepresentation roles = realmImport.getRoles();
        Map<String, List<RoleRepresentation>> clientsRoles = roles.getClient();

        for (Map.Entry<String, List<RoleRepresentation>> entry : clientsRoles.entrySet()) {
            String clientId = entry.getKey();
            List<RoleRepresentation> clientRoles = entry.getValue();

            for(RoleRepresentation role : clientRoles) {
                Optional<RoleRepresentation> maybeRole = roleRepository.tryToFindClientRole(realmImport.getRealm(), clientId, role.getName());

                if(maybeRole.isPresent()) {
                    updateClientRole(realmImport.getRealm(), clientId, maybeRole.get(), role);
                } else {
                    roleRepository.createClientRole(realmImport.getRealm(), clientId, role);
                }
            }
        }
    }

    private void updateRealmRole(String realm, RoleRepresentation existingRole, RoleRepresentation roleToImport) {
        RoleRepresentation patchedRole = CloneUtils.deepPatch(existingRole, roleToImport);
        roleRepository.updateRealmRole(realm, patchedRole);
    }

    private void updateClientRole(String realm, String clientId, RoleRepresentation existingRole, RoleRepresentation roleToImport) {
        RoleRepresentation patchedRole = CloneUtils.deepPatch(existingRole, roleToImport);
        roleRepository.updateClientRole(realm, clientId, patchedRole);
    }
}
