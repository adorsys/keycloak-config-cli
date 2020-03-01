package com.github.borisskert.keycloak.config.service.rolecomposites.client;

import com.github.borisskert.keycloak.config.model.RealmImport;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implements the update mechanism for role composites of client-level roles
 */
@Service
public class ClientRoleCompositeImportService {

    private final RealmCompositeImport realmCompositeImport;
    private final ClientCompositeImport clientCompositeImport;

    @Autowired
    public ClientRoleCompositeImportService(
            RealmCompositeImport realmCompositeImport,
            ClientCompositeImport clientCompositeImport
    ) {
        this.realmCompositeImport = realmCompositeImport;
        this.clientCompositeImport = clientCompositeImport;
    }

    /**
     * Updates the role composites for all client-level roles
     *
     * @param realmImport the realm-import containing all client-level roles containing role-composites to be imported
     */
    public void update(RealmImport realmImport) {
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
                    realmCompositeImport.update(realm, roleClientId, clientRole, realmComposites);
                });
    }

    private void updateClientRoleClientCompositesIfNecessary(String realm, String roleClientId, RoleRepresentation clientRole) {
        Optional.ofNullable(clientRole.getComposites())
                .flatMap(composites -> Optional.ofNullable(composites.getClient()))
                .ifPresent(clientComposites -> clientCompositeImport.update(
                        realm,
                        roleClientId,
                        clientRole.getName(),
                        clientComposites
                ));
    }
}
