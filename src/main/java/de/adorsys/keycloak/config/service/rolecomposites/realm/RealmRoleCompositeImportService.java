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

package de.adorsys.keycloak.config.service.rolecomposites.realm;

import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implements the update mechanism for role composites of realm-level roles
 */
@Service
public class RealmRoleCompositeImportService {

    private final RealmCompositeImport realmCompositeImport;
    private final ClientCompositeImport clientCompositeImport;

    @Autowired
    public RealmRoleCompositeImportService(
            RealmCompositeImport realmCompositeImport,
            ClientCompositeImport clientCompositeImport
    ) {
        this.clientCompositeImport = clientCompositeImport;
        this.realmCompositeImport = realmCompositeImport;
    }

    /**
     * Updates the role composites for all realm-level roles
     *
     * @param realm the realm name
     * @param roles containing all realm-level roles containing role-composites to be imported
     */
    public void update(String realm, RolesRepresentation roles) {
        List<RoleRepresentation> realmRoles = roles.getRealm();

        for (RoleRepresentation realmRole : realmRoles) {
            updateRealmRoleRealmCompositesIfNecessary(realm, realmRole);
            updateRealmRoleClientCompositesIfNecessary(realm, realmRole);
        }
    }

    private void updateRealmRoleRealmCompositesIfNecessary(String realm, RoleRepresentation realmRole) {
        Optional.ofNullable(realmRole.getComposites())
                .flatMap(composites -> Optional.ofNullable(composites.getRealm()))
                .ifPresent(realmComposites -> realmCompositeImport.update(realm, realmRole, realmComposites));
    }

    private void updateRealmRoleClientCompositesIfNecessary(String realm, RoleRepresentation realmRole) {
        Optional.ofNullable(realmRole.getComposites())
                .flatMap(composites -> Optional.ofNullable(composites.getClient()))
                .ifPresent(clientComposites -> clientCompositeImport.update(
                        realm,
                        realmRole.getName(),
                        clientComposites
                ));
    }
}
