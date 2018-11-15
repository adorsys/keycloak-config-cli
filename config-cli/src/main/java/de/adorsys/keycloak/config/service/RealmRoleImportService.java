package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.RealmRepository;
import de.adorsys.keycloak.config.repository.RoleRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RealmRoleImportService {

    private final RealmRepository realmRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public RealmRoleImportService(
            RealmRepository realmRepository, RoleRepository roleRepository
    ) {
        this.realmRepository = realmRepository;
        this.roleRepository = roleRepository;
    }

    public void doImport(RealmImport realmImport) {
        RolesRepresentation roles = realmImport.getRoles();

        if(roles != null) {
            List<RoleRepresentation> rolesRealm = roles.getRealm();

            if(rolesRealm != null) {
                for(RoleRepresentation role : rolesRealm) {
                    Optional<RoleRepresentation> maybeRole = roleRepository.tryToFindRole(realmImport.getRealm(), role.getName());

                    if(maybeRole.isPresent()) {
                        updateRole(realmImport.getRealm(), maybeRole.get(), role);
                    } else {
                        createRole(realmImport.getRealm(), role);
                    }
                }
            }
        }
    }

    private void updateRole(String realm, RoleRepresentation existingRole, RoleRepresentation roleToImport) {
        RoleRepresentation patchedRole = CloneUtils.deepPatch(existingRole, roleToImport);
        realmRepository.loadRealm(realm).roles().get(existingRole.getName()).update(patchedRole);
    }

    private void createRole(String realm, RoleRepresentation role) {
        RolesResource rolesResource = realmRepository.loadRealm(realm).roles();
        rolesResource.create(role);
    }
}
