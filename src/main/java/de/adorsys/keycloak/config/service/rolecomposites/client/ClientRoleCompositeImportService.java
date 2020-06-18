/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2020 adorsys GmbH & Co. KG @ https://adorsys.de
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package de.adorsys.keycloak.config.service.rolecomposites.client;

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
     * @param realm the realm name
     * @param roles import containing all client-level roles containing role-composites to be imported
     */
    public void update(String realm, RolesRepresentation roles) {
        Map<String, List<RoleRepresentation>> clientRolesPerClient = roles.getClient();
        if (clientRolesPerClient == null) return;

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
                .ifPresent(realmComposites -> realmCompositeImport.update(realm, roleClientId, clientRole, realmComposites));
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
