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

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.repository.RoleRepository;
import de.adorsys.keycloak.config.service.rolecomposites.client.ClientRoleCompositeImportService;
import de.adorsys.keycloak.config.service.rolecomposites.realm.RealmRoleCompositeImportService;
import de.adorsys.keycloak.config.util.CloneUtil;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Service
public class RoleImportService {
    private static final Logger logger = LoggerFactory.getLogger(RoleImportService.class);

    private final RealmRoleCompositeImportService realmRoleCompositeImport;
    private final ClientRoleCompositeImportService clientRoleCompositeImport;

    private final RoleRepository roleRepository;
    private final ImportConfigProperties importConfigProperties;

    @Autowired
    public RoleImportService(
            RealmRoleCompositeImportService realmRoleCompositeImportService,
            ClientRoleCompositeImportService clientRoleCompositeImportService,
            RoleRepository roleRepository,
            ImportConfigProperties importConfigProperties) {
        this.realmRoleCompositeImport = realmRoleCompositeImportService;
        this.clientRoleCompositeImport = clientRoleCompositeImportService;
        this.roleRepository = roleRepository;
        this.importConfigProperties = importConfigProperties;
    }

    public void doImport(RealmImport realmImport) {
        RolesRepresentation roles = realmImport.getRoles();
        if (roles == null) return;

        String realm = realmImport.getRealm();
        createOrUpdateRealmRoles(realm, roles);
        createOrUpdateClientRoles(realm, roles);

        realmRoleCompositeImport.update(realm, roles);
        clientRoleCompositeImport.update(realm, roles);
    }

    private void createOrUpdateRealmRoles(String realm, RolesRepresentation roles) {
        List<RoleRepresentation> realmRoles = roles.getRealm();

        Consumer<RoleRepresentation> loop = role -> createOrUpdateRealmRole(realm, role);
        if (importConfigProperties.isParallel()) {
            realmRoles.parallelStream().forEach(loop);
        } else {
            realmRoles.forEach(loop);
        }
    }

    private void createOrUpdateRealmRole(String realm, RoleRepresentation role) {
        String roleName = role.getName();

        Optional<RoleRepresentation> maybeRole = roleRepository.tryToFindRealmRole(realm, roleName);

        if (maybeRole.isPresent()) {
            updateClientIfNeeded(realm, maybeRole.get(), role);
        } else {
            logger.debug("Create realm-level role '{}' in realm '{}'", roleName, realm);
            roleRepository.createRealmRole(realm, role);
        }
    }

    private void createOrUpdateClientRoles(String realm, RolesRepresentation roles) {
        Map<String, List<RoleRepresentation>> clientRolesPerClient = roles.getClient();
        if (clientRolesPerClient == null) return;

        for (Map.Entry<String, List<RoleRepresentation>> clientRoles : clientRolesPerClient.entrySet()) {
            createOrUpdateClientRoles(realm, clientRoles);
        }
    }

    private void createOrUpdateClientRoles(String realm, Map.Entry<String, List<RoleRepresentation>> clientRolesForClient) {
        String clientId = clientRolesForClient.getKey();
        List<RoleRepresentation> clientRoles = clientRolesForClient.getValue();

        for (RoleRepresentation role : clientRoles) {
            createOrUpdateClientRole(realm, clientId, role);
        }
    }

    private void createOrUpdateClientRole(String realm, String clientId, RoleRepresentation role) {
        String roleName = role.getName();

        Optional<RoleRepresentation> maybeRole = roleRepository.tryToFindClientRole(realm, clientId, roleName);

        if (maybeRole.isPresent()) {
            updateClientRoleIfNecessary(realm, clientId, maybeRole.get(), role);
        } else {
            logger.debug("Create client-level role '{}' for client '{}' in realm '{}'", roleName, clientId, realm);
            roleRepository.createClientRole(realm, clientId, role);
        }
    }

    private void updateClientIfNeeded(String realm, RoleRepresentation existingRole, RoleRepresentation roleToImport) {
        String roleName = roleToImport.getName();
        RoleRepresentation patchedRole = CloneUtil.deepPatch(existingRole, roleToImport);
        if (roleToImport.getAttributes() != null) {
            patchedRole.setAttributes(roleToImport.getAttributes());
        }

        if (!CloneUtil.deepEquals(existingRole, patchedRole)) {
            logger.debug("Update realm-level role '{}' in realm '{}'", roleName, realm);
            roleRepository.updateRealmRole(realm, patchedRole);
        } else {
            logger.debug("No need to update realm-level '{}' in realm '{}'", roleName, realm);
        }
    }

    private void updateClientRoleIfNecessary(String realm, String clientId, RoleRepresentation existingRole, RoleRepresentation roleToImport) {
        RoleRepresentation patchedRole = CloneUtil.deepPatch(existingRole, roleToImport);
        String roleName = existingRole.getName();

        if (CloneUtil.deepEquals(existingRole, patchedRole)) {
            logger.debug("No need to update client-level role '{}' for client '{}' in realm '{}'", roleName, clientId, realm);
        } else {
            logger.debug("Update client-level role '{}' for client '{}' in realm '{}'", roleName, clientId, realm);
            roleRepository.updateClientRole(realm, clientId, patchedRole);
        }
    }
}
