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

package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.util.MultiValueMap;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.util.*;
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

    public void addRealmRoleRealmComposites(String realm, String roleName, Set<String> realmComposites) {
        RoleResource roleResource = realmRepository.loadRealm(realm)
                .roles()
                .get(roleName);

        List<RoleRepresentation> realmRoles = realmComposites.stream()
                .map(realmRoleName -> findRealmRole(realm, realmRoleName))
                .collect(Collectors.toList());

        roleResource.addComposites(realmRoles);
    }

    public void removeRealmRoleRealmComposites(String realm, String roleName, Set<String> realmComposites) {
        RoleResource roleResource = realmRepository.loadRealm(realm)
                .roles()
                .get(roleName);

        List<RoleRepresentation> realmRoles = realmComposites.stream()
                .map(realmRoleName -> findRealmRole(realm, realmRoleName))
                .collect(Collectors.toList());

        roleResource.deleteComposites(realmRoles);
    }

    public void removeClientRoleRealmComposites(String realm, String roleClientId, String roleName, Set<String> realmComposites) {
        RoleResource roleResource = loadClientRole(realm, roleClientId, roleName);

        List<RoleRepresentation> realmRoles = realmComposites.stream()
                .map(realmRoleName -> findRealmRole(realm, realmRoleName))
                .collect(Collectors.toList());

        roleResource.deleteComposites(realmRoles);
    }

    public Set<RoleRepresentation> findRealmRoleRealmComposites(String realm, String roleName) {
        RoleResource roleResource = realmRepository.loadRealm(realm)
                .roles()
                .get(roleName);

        return roleResource.getRealmRoleComposites();
    }

    public void addClientRoleRealmComposites(
            String realm,
            String roleClientId,
            String roleName,
            Set<String> realmComposites
    ) {
        RoleResource roleResource = loadClientRole(realm, roleClientId, roleName);

        List<RoleRepresentation> realmRoles = realmComposites.stream()
                .map(realmRoleName -> findRealmRole(realm, realmRoleName))
                .collect(Collectors.toList());

        roleResource.addComposites(realmRoles);
    }

    public Set<RoleRepresentation> findClientRoleRealmComposites(
            String realm,
            String roleClientId,
            String roleName
    ) {
        RoleResource roleResource = loadClientRole(realm, roleClientId, roleName);

        return roleResource.getRealmRoleComposites();
    }

    public void addRealmRoleClientComposites(String realm, String roleName, String compositeClientId, Collection<String> clientRoles) {
        RoleResource roleResource = realmRepository.loadRealm(realm)
                .roles()
                .get(roleName);

        List<RoleRepresentation> realmRoles = clientRoles.stream()
                .map(clientRoleName -> findClientRole(realm, compositeClientId, clientRoleName))
                .collect(Collectors.toList());

        roleResource.addComposites(realmRoles);
    }

    public void removeRealmRoleClientComposites(String realm, String roleName, String compositeClientId, Collection<String> clientRoleNames) {
        RoleResource roleResource = loadRealmRole(realm, roleName);

        List<RoleRepresentation> clientRoles = clientRoleNames.stream()
                .map(clientRoleName -> findClientRole(realm, compositeClientId, clientRoleName))
                .collect(Collectors.toList());

        roleResource.deleteComposites(clientRoles);
    }

    public void removeRealmRoleClientComposites(String realm, String roleName, Map<String, List<String>> clientCompositesToRemove) {
        RoleResource roleResource = realmRepository.loadRealm(realm)
                .roles()
                .get(roleName);

        List<RoleRepresentation> clientRolesToRemove = mapToClientRoles(realm, clientCompositesToRemove);

        roleResource.deleteComposites(clientRolesToRemove);
    }

    public Set<RoleRepresentation> findRealmRoleClientComposites(String realm, String roleName, String compositeClientId) {
        RoleResource roleResource = loadRealmRole(realm, roleName);

        ClientRepresentation client = clientRepository.getClient(realm, compositeClientId);

        return roleResource.getClientRoleComposites(client.getId());
    }

    public Map<String, List<String>> findRealmRoleClientComposites(String realm, String roleName) {
        RoleResource roleResource = loadRealmRole(realm, roleName);

        List<ClientRepresentation> clients = clientRepository.getClients(realm);
        MultiValueMap<String, String> clientComposites = new MultiValueMap<>();

        for (ClientRepresentation client : clients) {
            Set<String> clientRoleComposites = roleResource.getClientRoleComposites(client.getId())
                    .stream().map(RoleRepresentation::getName)
                    .collect(Collectors.toSet());

            clientComposites.putAll(client.getClientId(), clientRoleComposites);
        }

        return clientComposites.toMap();
    }

    private RoleResource loadRealmRole(String realm, String roleName) {
        RealmResource realmResource = realmRepository.loadRealm(realm);
        return realmResource
                .roles()
                .get(roleName);
    }

    public void addClientRoleClientComposites(
            String realm,
            String roleClientId,
            String roleName,
            String compositeClientId,
            Collection<String> clientComposites
    ) {
        RoleResource roleResource = loadClientRole(realm, roleClientId, roleName);

        List<RoleRepresentation> clientRoles = clientComposites.stream()
                .map(clientRoleName -> findClientRole(realm, compositeClientId, clientRoleName))
                .collect(Collectors.toList());

        roleResource.addComposites(clientRoles);
    }

    public Set<RoleRepresentation> findClientRoleClientComposites(
            String realm,
            String roleClientId,
            String roleName,
            String compositeClientId
    ) {
        RoleResource roleResource = loadClientRole(realm, roleClientId, roleName);
        ClientRepresentation client = clientRepository.getClient(realm, compositeClientId);

        return roleResource.getClientRoleComposites(client.getId());
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

    public Optional<RoleRepresentation> tryToFindClientRole(String realm, String clientId, String roleName) {
        ClientRepresentation client = clientRepository.getClient(realm, clientId);
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
        ClientRepresentation client = clientRepository.getClient(realm, clientId);
        RealmResource realmResource = realmRepository.loadRealm(realm);

        List<RoleRepresentation> clientRoles = realmResource.clients()
                .get(client.getId())
                .roles()
                .list();

        return clientRoles.stream()
                .filter(r -> r.getName().equals(roleName))
                .findFirst()
                .get();
    }

    public List<RoleRepresentation> searchClientRoles(String realm, String clientId, List<String> roles) {
        ClientRepresentation foundClient = clientRepository.getClient(realm, clientId);

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
        ClientRepresentation client = clientRepository.getClient(realm, clientId);
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

    public List<RoleRepresentation> searchRealmRoles(String realm, List<String> roles) {
        return roles.stream()
                .map(role -> realmRepository.loadRealm(realm)
                        .roles()
                        .get(role)
                        .toRepresentation()
                )
                .collect(Collectors.toList());
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
        ClientRepresentation client = clientRepository.getClient(realm, clientId);
        UserResource userResource = userRepository.getUserResource(realm, username);

        RoleScopeResource userClientRoles = userResource.roles()
                .clientLevel(client.getId());

        userClientRoles.add(clientRoles);
    }

    public void removeClientRolesForUser(String realm, String username, String clientId, List<RoleRepresentation> clientRoles) {
        ClientRepresentation client = clientRepository.getClient(realm, clientId);
        UserResource userResource = userRepository.getUserResource(realm, username);

        RoleScopeResource userClientRoles = userResource.roles()
                .clientLevel(client.getId());

        userClientRoles.remove(clientRoles);
    }

    public List<String> getUserClientLevelRoles(String realm, String username, String clientId) {
        ClientRepresentation client = clientRepository.getClient(realm, clientId);
        UserResource userResource = userRepository.getUserResource(realm, username);

        List<RoleRepresentation> roles = userResource.roles()
                .clientLevel(client.getId())
                .listEffective();

        return roles.stream().map(RoleRepresentation::getName).collect(Collectors.toList());
    }

    private RoleResource loadClientRole(String realm, String roleClientId, String roleName) {
        ClientRepresentation client = clientRepository.getClient(realm, roleClientId);

        return realmRepository.loadRealm(realm)
                .clients()
                .get(client.getId())
                .roles()
                .get(roleName);
    }

    public void removeClientRoleClientComposites(String realm, String roleClientId, String roleName, String compositeClientId, Collection<String> clientRoleNames) {
        RoleResource roleResource = loadClientRole(realm, roleClientId, roleName);

        List<RoleRepresentation> clientRoles = clientRoleNames.stream()
                .map(clientRoleName -> findClientRole(realm, compositeClientId, clientRoleName))
                .collect(Collectors.toList());

        roleResource.deleteComposites(clientRoles);
    }

    public void removeClientRoleClientComposites(String realm, String roleClientId, String roleName, Map<String, List<String>> clientCompositesToRemove) {
        RoleResource roleResource = loadClientRole(realm, roleClientId, roleName);
        List<RoleRepresentation> clientRolesToRemove = findAllClientRoles(realm, clientCompositesToRemove);

        roleResource.deleteComposites(clientRolesToRemove);
    }

    private List<RoleRepresentation> findAllClientRoles(String realm, Map<String, List<String>> clientCompositesToRemove) {
        List<RoleRepresentation> clientRolesToRemove = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : clientCompositesToRemove.entrySet()) {
            String clientId = entry.getKey();
            for (String role : entry.getValue()) {
                RoleRepresentation clientRole = findClientRole(realm, clientId, role);
                clientRolesToRemove.add(clientRole);
            }
        }
        return clientRolesToRemove;
    }

    public Map<String, List<String>> findClientRoleClientComposites(String realm, String roleClientId, String roleName) {
        RoleResource roleResource = loadClientRole(realm, roleClientId, roleName);

        List<ClientRepresentation> clients = clientRepository.getClients(realm);
        MultiValueMap<String, String> clientComposites = new MultiValueMap<>();

        for (ClientRepresentation client : clients) {
            Set<String> clientRoleComposites = roleResource.getClientRoleComposites(client.getId())
                    .stream()
                    .map(RoleRepresentation::getName)
                    .collect(Collectors.toSet());

            clientComposites.putAll(client.getClientId(), clientRoleComposites);
        }

        return clientComposites.toMap();
    }

    private List<RoleRepresentation> mapToClientRoles(String realm, Map<String, List<String>> clientCompositesToRemove) {
        List<RoleRepresentation> clientRolesToRemove = new ArrayList<>();

        for (Map.Entry<String, List<String>> compositesByClients : clientCompositesToRemove.entrySet()) {
            String clientId = compositesByClients.getKey();
            List<String> compositesByClient = compositesByClients.getValue();

            List<RoleRepresentation> clientRoles = compositesByClient.stream()
                    .map(composite -> findClientRole(realm, clientId, composite))
                    .collect(Collectors.toList());

            clientRolesToRemove.addAll(clientRoles);
        }

        return clientRolesToRemove;
    }
}
