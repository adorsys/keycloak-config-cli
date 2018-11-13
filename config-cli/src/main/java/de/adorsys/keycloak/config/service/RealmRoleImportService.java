package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;

import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.Optional;

public class RealmRoleImportService {

    private final RealmImport realmImport;
    private final RealmResource realmResource;

    public RealmRoleImportService(RealmImport realmImport, RealmResource realmResource) {
        this.realmImport = realmImport;
        this.realmResource = realmResource;
    }

    public void doImport() {
        RolesRepresentation roles = realmImport.getRoles();

        if(roles != null) {
            List<RoleRepresentation> rolesRealm = roles.getRealm();

            if(rolesRealm != null) {
                for(RoleRepresentation role : rolesRealm) {
                    Optional<RoleRepresentation> maybeRole = tryToFindRole(role.getName());

                    if(maybeRole.isPresent()) {
                        updateRole(maybeRole.get(), role);
                    } else {
                        createRole(role);
                    }
                }
            }
        }
    }

    private void updateRole(RoleRepresentation existingRole, RoleRepresentation roleToImport) {
        RoleRepresentation patchedRole = CloneUtils.deepPatch(existingRole, roleToImport);
        realmResource.roles().get(existingRole.getName()).update(patchedRole);
    }

    private void createRole(RoleRepresentation role) {
        RolesResource rolesResource = realmResource.roles();
        rolesResource.create(role);
    }

    private Optional<RoleRepresentation> tryToFindRole(String name) {
        Optional<RoleRepresentation> maybeRole;

        RolesResource rolesResource = realmResource.roles();
        RoleResource roleResource = rolesResource.get(name);

        try {
            maybeRole = Optional.of(roleResource.toRepresentation());
        } catch(NotFoundException e) {
            maybeRole = Optional.empty();
        }

        return maybeRole;
    }
}
