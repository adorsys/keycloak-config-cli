/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2021 adorsys GmbH & Co. KG @ https://adorsys.com
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
import de.adorsys.keycloak.config.repository.ClientRepository;
import de.adorsys.keycloak.config.repository.RoleRepository;
import de.adorsys.keycloak.config.service.rolecomposites.client.ClientRoleCompositeImportService;
import de.adorsys.keycloak.config.service.rolecomposites.realm.RealmRoleCompositeImportService;
import de.adorsys.keycloak.config.service.state.StateService;
import de.adorsys.keycloak.config.util.CloneUtil;
import de.adorsys.keycloak.config.util.KeycloakUtil;
import de.adorsys.keycloak.config.util.ParallelUtil;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues.FULL;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class RoleImportService {
    private static final Logger logger = LoggerFactory.getLogger(RoleImportService.class);
    private static final String[] propertiesWithDependencies = new String[]{
            "composites",
    };

    private final RealmRoleCompositeImportService realmRoleCompositeImport;
    private final ClientRoleCompositeImportService clientRoleCompositeImport;

    private final RoleRepository roleRepository;
    private final ClientRepository clientRepository;
    private final ImportConfigProperties importConfigProperties;
    private final StateService stateService;

    @Autowired
    public RoleImportService(
            RealmRoleCompositeImportService realmRoleCompositeImportService,
            ClientRoleCompositeImportService clientRoleCompositeImportService,
            RoleRepository roleRepository,
            ClientRepository clientRepository,
            ImportConfigProperties importConfigProperties, StateService stateService) {
        this.realmRoleCompositeImport = realmRoleCompositeImportService;
        this.clientRoleCompositeImport = clientRoleCompositeImportService;
        this.roleRepository = roleRepository;
        this.clientRepository = clientRepository;
        this.importConfigProperties = importConfigProperties;
        this.stateService = stateService;
    }

    public void doImport(RealmImport realmImport) {
        RolesRepresentation roles = realmImport.getRoles();
        if (roles == null) return;

        String realmName = realmImport.getRealm();

        var realmRoles = roles.getRealm();
        var clientRoles = roles.getClient();

        if (realmRoles != null) {
            importRealmRoles(realmName, realmRoles);
        }

        if (clientRoles != null) {
            importClientRoles(realmName, clientRoles);
        }
    }

    private void importRealmRoles(
            String realmName,
            List<RoleRepresentation> realmRoles
    ) {
        var existingRealmRoles = roleRepository.getRealmRoles(realmName);

        if (importConfigProperties.getManaged().getRole() == FULL) {
            deleteRealmRolesMissingInImport(realmName, realmRoles, existingRealmRoles);
        }

        createOrUpdateRealmRoles(realmName, realmRoles, existingRealmRoles);

        realmRoleCompositeImport.update(realmName, realmRoles);
    }

    private void importClientRoles(
            String realmName,
            Map<String, List<RoleRepresentation>> clientRoles
    ) {
        for (var client : clientRoles.entrySet()) {
            var clientResource = clientRepository.getResourceByClientId(realmName, client.getKey());
            var existingClientRoles = clientResource.roles().list();

            if (importConfigProperties.getManaged().getRole() == FULL) {
                deleteClientRolesMissingInImport(
                        realmName,
                        client.getKey(),
                        client.getValue(),
                        existingClientRoles
                );
            }
            createOrUpdateClientRoles(
                    realmName,
                    client.getKey(),
                    client.getValue(),
                    existingClientRoles
            );
        }

        clientRoleCompositeImport.update(realmName, clientRoles);
    }

    private void createOrUpdateRealmRoles(
            String realmName,
            List<RoleRepresentation> rolesToImport,
            List<RoleRepresentation> existingRealmRoles
    ) {
        Consumer<RoleRepresentation> loop = role -> createOrUpdateRealmRole(realmName, role, existingRealmRoles);
        if (importConfigProperties.isParallel()) {
            ParallelUtil.forEach(rolesToImport, loop);
        } else {
            rolesToImport.forEach(loop);
        }
    }

    private void createOrUpdateRealmRole(
            String realmName,
            RoleRepresentation roleToImport,
            List<RoleRepresentation> existingRoles
    ) {
        String roleName = roleToImport.getName();

        RoleRepresentation existingRole = existingRoles.stream()
                .filter(r -> Objects.equals(r.getName(), roleToImport.getName()))
                .findFirst().orElse(null);

        if (existingRole != null) {
            updateRoleIfNeeded(realmName, existingRole, roleToImport);
        } else {
            createRole(realmName, roleToImport, roleName);
        }
    }

    private void createRole(String realmName, RoleRepresentation roleToImport, String roleName) {
        logger.debug("Create realm-level role '{}' in realm '{}'", roleName, realmName);
        RoleRepresentation roleToImportWithoutDependencies = CloneUtil.deepClone(
                roleToImport, RoleRepresentation.class, propertiesWithDependencies
        );

        roleRepository.createRealmRole(realmName, roleToImportWithoutDependencies);
    }

    private void createOrUpdateClientRoles(
            String realmName,
            String clientId,
            List<RoleRepresentation> rolesToImport,
            List<RoleRepresentation> existingRoles
    ) {
        for (RoleRepresentation roleToImport : rolesToImport) {
            String roleName = roleToImport.getName();

            existingRoles.stream()
                    .filter(r -> Objects.equals(r.getName(), roleName))
                    .findFirst()
                    .ifPresentOrElse(existingClientRole -> {
                        updateClientRoleIfNecessary(
                                realmName,
                                clientId,
                                existingClientRole,
                                roleToImport
                        );
                    }, () -> {
                        createClientRole(
                                realmName,
                                clientId,
                                roleToImport,
                                roleName
                        );
                    });
        }
    }

    private void createClientRole(String realmName, String clientId, RoleRepresentation roleToImport, String roleName) {
        logger.debug("Create client-level role '{}' for client '{}' in realm '{}'", roleName, clientId, realmName);
        RoleRepresentation roleToImportWithoutDependencies = CloneUtil.deepClone(
                roleToImport, RoleRepresentation.class, propertiesWithDependencies
        );
        roleRepository.createClientRole(realmName, clientId, roleToImportWithoutDependencies);
    }

    private void updateRoleIfNeeded(
            String realmName,
            RoleRepresentation existingRole,
            RoleRepresentation roleToImport
    ) {
        String roleName = roleToImport.getName();
        RoleRepresentation patchedRole = CloneUtil.patch(existingRole, roleToImport, propertiesWithDependencies);
        if (roleToImport.getAttributes() != null) {
            patchedRole.setAttributes(roleToImport.getAttributes());
        }

        if (!CloneUtil.deepEquals(existingRole, patchedRole)) {
            logger.debug("Update realm-level role '{}' in realm '{}'", roleName, realmName);
            roleRepository.updateRealmRole(realmName, patchedRole);
        } else {
            logger.debug("No need to update realm-level '{}' in realm '{}'", roleName, realmName);
        }
    }

    private void updateClientRoleIfNecessary(
            String realmName,
            String clientId,
            RoleRepresentation existingRole,
            RoleRepresentation roleToImport
    ) {
        RoleRepresentation patchedRole = CloneUtil.patch(existingRole, roleToImport, propertiesWithDependencies);
        String roleName = existingRole.getName();

        if (CloneUtil.deepEquals(existingRole, patchedRole)) {
            logger.debug("No need to update client-level role '{}' for client '{}' in realm '{}'", roleName, clientId, realmName);
        } else {
            logger.debug("Update client-level role '{}' for client '{}' in realm '{}'", roleName, clientId, realmName);
            roleRepository.updateClientRole(realmName, clientId, patchedRole);
        }
    }

    private void deleteRealmRolesMissingInImport(
            String realmName,
            List<RoleRepresentation> importedRoles,
            List<RoleRepresentation> existingRoles
    ) {
        if (importConfigProperties.getRemoteState().isEnabled()) {
            List<String> realmRolesInState = stateService.getRealmRoles();

            // ignore all object there are not in state
            existingRoles = existingRoles.stream()
                    .filter(role -> realmRolesInState.contains(role.getName()))
                    .toList();
        }

        Set<String> importedRealmRoles = importedRoles.stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());

        for (RoleRepresentation existingRole : existingRoles) {
            if (KeycloakUtil.isDefaultRole(existingRole) || importedRealmRoles.contains(existingRole.getName())) {
                continue;
            }

            logger.debug("Delete realm-level role '{}' in realm '{}'", existingRole.getName(), realmName);
            roleRepository.deleteRealmRole(realmName, existingRole);
        }
    }

    private void deleteClientRolesMissingInImport(
            String realmName,
            String clientId,
            List<RoleRepresentation> importedClientRoles,
            List<RoleRepresentation> existingRoles
    ) {
        List<RoleRepresentation> managedRoles = getManagedClientRoles(clientId, existingRoles);

        for (RoleRepresentation role : managedRoles) {
            boolean isImported = importedClientRoles != null 
                    && importedClientRoles.stream()
                            .anyMatch(r -> r.getName().equals(role.getName()));
            if (!(isImported || KeycloakUtil.isDefaultRole(role))) {
                logger.debug("Delete client-level role '{}' for client '{}' in realm '{}'",
                        role.getName(), clientId, realmName);
                roleRepository.deleteClientRole(realmName, clientId, role);
            }
        }
    }

    private List<RoleRepresentation> getManagedClientRoles(String client, List<RoleRepresentation> existingRoles) {
        if (importConfigProperties.getRemoteState().isEnabled()) {
            List<String> clientRolesInState = stateService.getClientRoles(client);
            // ignore all object there are not in state
            return existingRoles.stream()
                    .filter(role -> clientRolesInState.contains(role.getName()))
                    .toList();
        } else {
            return existingRoles;
        }
    }
}
