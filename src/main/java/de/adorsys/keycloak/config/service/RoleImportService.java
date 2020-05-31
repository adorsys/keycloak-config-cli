/*
 * Copyright 2019-2020 adorsys GmbH & Co. KG @ https://adorsys.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.RoleRepository;
import de.adorsys.keycloak.config.service.rolecomposites.client.ClientRoleCompositeImportService;
import de.adorsys.keycloak.config.service.rolecomposites.realm.RealmRoleCompositeImportService;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.jboss.logging.Logger;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Dependent
public class RoleImportService {
    private static final Logger LOG = Logger.getLogger(RoleImportService.class);

    @Inject
    RealmRoleCompositeImportService realmRoleCompositeImport;

    @Inject
    ClientRoleCompositeImportService clientRoleCompositeImport;

    @Inject
    RoleRepository roleRepository;

    public void doImport(RealmImport realmImport) {
        createOrUpdateRealmRoles(realmImport);
        createOrUpdateClientRoles(realmImport);

        realmRoleCompositeImport.update(realmImport);
        clientRoleCompositeImport.update(realmImport);
    }

    private void createOrUpdateRealmRoles(RealmImport realmImport) {
        RolesRepresentation roles = realmImport.getRoles();
        List<RoleRepresentation> realmRoles = roles.getRealm();

        for (RoleRepresentation role : realmRoles) {
            createOrUpdateRealmRole(realmImport, role);
        }
    }

    private void createOrUpdateRealmRole(RealmImport realmImport, RoleRepresentation role) {
        String roleName = role.getName();
        String realm = realmImport.getRealm();

        Optional<RoleRepresentation> maybeRole = roleRepository.tryToFindRealmRole(realm, roleName);

        if (maybeRole.isPresent()) {
            LOG.debugf("Update realm-level role '%s' in realm '%s'", roleName, realm);
            updateRealmRole(realm, maybeRole.get(), role);
        } else {
            LOG.debugf("Create realm-level role '%s' in realm '%s'", roleName, realm);
            roleRepository.createRealmRole(realm, role);
        }
    }

    private void createOrUpdateClientRoles(RealmImport realmImport) {
        RolesRepresentation roles = realmImport.getRoles();
        Map<String, List<RoleRepresentation>> clientRolesPerClient = roles.getClient();

        for (Map.Entry<String, List<RoleRepresentation>> clientRoles : clientRolesPerClient.entrySet()) {
            createOrUpdateClientRoles(realmImport, clientRoles);
        }
    }

    private void createOrUpdateClientRoles(RealmImport realmImport, Map.Entry<String, List<RoleRepresentation>> clientRolesForClient) {
        String clientId = clientRolesForClient.getKey();
        List<RoleRepresentation> clientRoles = clientRolesForClient.getValue();

        for (RoleRepresentation role : clientRoles) {
            createOrUpdateClientRole(realmImport, clientId, role);
        }
    }

    private void createOrUpdateClientRole(RealmImport realmImport, String clientId, RoleRepresentation role) {
        String roleName = role.getName();
        String realm = realmImport.getRealm();

        Optional<RoleRepresentation> maybeRole = roleRepository.tryToFindClientRole(realmImport.getRealm(), clientId, roleName);

        if (maybeRole.isPresent()) {
            updateClientRoleIfNecessary(realmImport.getRealm(), clientId, maybeRole.get(), role);
        } else {
            LOG.debugf("Create client-level role '%s' for client '%s' in realm '%s'", roleName, clientId, realm);
            roleRepository.createClientRole(realmImport.getRealm(), clientId, role);
        }
    }

    private void updateRealmRole(String realm, RoleRepresentation existingRole, RoleRepresentation roleToImport) {
        RoleRepresentation patchedRole = CloneUtils.deepPatch(existingRole, roleToImport);
        roleRepository.updateRealmRole(realm, patchedRole);
    }

    private void updateClientRoleIfNecessary(String realm, String clientId, RoleRepresentation existingRole, RoleRepresentation roleToImport) {
        RoleRepresentation patchedRole = CloneUtils.deepPatch(existingRole, roleToImport);
        String roleName = existingRole.getName();

        if (CloneUtils.deepEquals(existingRole, patchedRole)) {
            LOG.debugf("No need to update client-level role '%s' for client '%s' in realm '%s'", roleName, clientId, realm);
        } else {
            LOG.debugf("Update client-level role '%s' for client '%s' in realm '%s'", roleName, clientId, realm);
            roleRepository.updateClientRole(realm, clientId, patchedRole);
        }
    }
}
