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

package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoleRepository {

    private final RealmRepository realmRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    @Autowired
    public RoleRepository(
            RealmRepository realmRepository,
            ClientRepository clientRepository,
            UserRepository userRepository
    ) {
        this.realmRepository = realmRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
    }

    public Optional<RoleRepresentation> tryToFindRealmRole(String realm, String name) {
        Optional<RoleRepresentation> maybeRole;

        RolesResource rolesResource = realmRepository.loadRealm(realm).roles();
        RoleResource roleResource = rolesResource.get(name);

        try {
            maybeRole = Optional.of(roleResource.toRepresentation());
        } catch (NotFoundException e) {
            maybeRole = Optional.empty();
        }

        return maybeRole;
    }

    public void createRealmRole(String realm, RoleRepresentation role) {
        RolesResource rolesResource = realmRepository.loadRealm(realm).roles();
        rolesResource.create(role);
    }

    public void updateRealmRole(String realm, RoleRepresentation roleToUpdate) {
        RoleResource roleResource = realmRepository.loadRealm(realm)
                .roles()
                .get(roleToUpdate.getName());

        roleResource.update(roleToUpdate);
    }

    public RoleRepresentation findRealmRole(String realm, String roleName) {
        return tryToFindRealmRole(realm, roleName)
                .orElseThrow(
                        () -> new KeycloakRepositoryException(
                                "Cannot find realm role '" + roleName + "' within realm '" + realm + "'"
                        )
                );
    }

    public List<RoleRepresentation> findRealmRoles(String realm, Collection<String> roles) {
        return roles.stream()
                .map(role -> findRealmRole(realm, role))
                .collect(Collectors.toList());
    }

    public final Optional<RoleRepresentation> tryToFindClientRole(String realm, String clientId, String roleName) {
        ClientRepresentation client = clientRepository.getClientByClientId(realm, clientId);
        RealmResource realmResource = realmRepository.loadRealm(realm);

        List<RoleRepresentation> clientRoles = realmResource.clients()
                .get(client.getId())
                .roles()
                .list();

        return clientRoles.stream()
                .filter(r -> r.getName().equals(roleName))
                .findFirst();
    }

    public RoleRepresentation findClientRole(String realm, String clientId, String roleName) {
        return tryToFindClientRole(realm, clientId, roleName)
                .orElse(null);
    }

    public List<RoleRepresentation> searchClientRoles(String realm, String clientId, List<String> roles) {
        ClientRepresentation foundClient = clientRepository.getClientByClientId(realm, clientId);

        ClientResource clientResource = realmRepository.loadRealm(realm)
                .clients()
                .get(foundClient.getId());

        return roles.stream()
                .map(role -> clientResource.roles()
                        .get(role)
                        .toRepresentation()
                ).collect(Collectors.toList());
    }

    public void createClientRole(String realm, String clientId, RoleRepresentation role) {
        ClientRepresentation client = clientRepository.getClientByClientId(realm, clientId);
        RolesResource rolesResource = realmRepository.loadRealm(realm)
                .clients()
                .get(client.getId())
                .roles();

        rolesResource.create(role);
    }

    public void updateClientRole(String realm, String clientId, RoleRepresentation roleToUpdate) {
        RoleResource roleResource = loadClientRole(realm, clientId, roleToUpdate.getName());
        roleResource.update(roleToUpdate);
    }

    public List<RoleRepresentation> searchRealmRoles(String realmName, List<String> roleNames) {
        List<RoleRepresentation> roles = new ArrayList<>();
        RealmResource realm = realmRepository.loadRealm(realmName);

        for (String roleName : roleNames) {
            try {
                RoleRepresentation role = realm.roles().get(roleName).toRepresentation();

                roles.add(role);
            } catch (NotFoundException e) {
                throw new ImportProcessingException("Could not find role '" + roleName + "' in realm '" + realmName + "'!");
            }
        }

        return roles;
    }

    public List<String> getUserRealmLevelRoles(String realm, String username) {
        UserRepresentation user = userRepository.findUser(realm, username);
        UserResource userResource = realmRepository.loadRealm(realm)
                .users()
                .get(user.getId());

        List<RoleRepresentation> roles = userResource.roles()
                .realmLevel()
                .listEffective();

        return roles.stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());
    }

    public void addRealmRolesToUser(String realm, String username, List<RoleRepresentation> realmRoles) {
        UserResource userResource = userRepository.getUserResource(realm, username);
        userResource.roles().realmLevel().add(realmRoles);
    }

    public void removeRealmRolesForUser(String realm, String username, List<RoleRepresentation> realmRoles) {
        UserResource userResource = userRepository.getUserResource(realm, username);
        userResource.roles().realmLevel().remove(realmRoles);
    }

    public void addClientRolesToUser(String realm, String username, String clientId, List<RoleRepresentation> clientRoles) {
        ClientRepresentation client = clientRepository.getClientByClientId(realm, clientId);
        UserResource userResource = userRepository.getUserResource(realm, username);

        RoleScopeResource userClientRoles = userResource.roles()
                .clientLevel(client.getId());

        userClientRoles.add(clientRoles);
    }

    public void removeClientRolesForUser(String realm, String username, String clientId, List<RoleRepresentation> clientRoles) {
        ClientRepresentation client = clientRepository.getClientByClientId(realm, clientId);
        UserResource userResource = userRepository.getUserResource(realm, username);

        RoleScopeResource userClientRoles = userResource.roles()
                .clientLevel(client.getId());

        userClientRoles.remove(clientRoles);
    }

    public List<String> getUserClientLevelRoles(String realm, String username, String clientId) {
        ClientRepresentation client = clientRepository.getClientByClientId(realm, clientId);
        UserResource userResource = userRepository.getUserResource(realm, username);

        List<RoleRepresentation> roles = userResource.roles()
                .clientLevel(client.getId())
                .listEffective();

        return roles.stream().map(RoleRepresentation::getName).collect(Collectors.toList());
    }

    final RoleResource loadRealmRole(String realm, String roleName) {
        RealmResource realmResource = realmRepository.loadRealm(realm);
        return realmResource
                .roles()
                .get(roleName);
    }

    final RoleResource loadClientRole(String realm, String roleClientId, String roleName) {
        ClientRepresentation client = clientRepository.getClientByClientId(realm, roleClientId);

        return realmRepository.loadRealm(realm)
                .clients()
                .get(client.getId())
                .roles()
                .get(roleName);
    }
}
