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

import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class RoleCompositeRepository {

    private final RoleRepository roleRepository;
    private final ClientRepository clientRepository;

    @Autowired
    public RoleCompositeRepository(RoleRepository roleRepository, ClientRepository clientRepository) {
        this.roleRepository = roleRepository;
        this.clientRepository = clientRepository;
    }

    public Set<RoleRepresentation> findRealmRoleRealmComposites(String realm, String roleName) {
        RoleResource roleResource = loadRealmRole(realm, roleName);
        return roleResource.getRealmRoleComposites();
    }

    public Set<RoleRepresentation> findClientRoleRealmComposites(String realm, String roleClientId, String roleName) {
        RoleResource roleResource = loadClientRole(realm, roleClientId, roleName);
        return roleResource.getRealmRoleComposites();
    }

    public Set<RoleRepresentation> findRealmRoleClientComposites(String realm, String roleName, String compositeClientId) {
        return findClientComposites(
                realm,
                compositeClientId,
                () -> loadRealmRole(realm, roleName)
        );
    }

    public Set<RoleRepresentation> findClientRoleClientComposites(
            String realm,
            String roleClientId,
            String roleName,
            String compositeClientId
    ) {
        return findClientComposites(
                realm,
                compositeClientId,
                () -> loadClientRole(realm, roleClientId, roleName)
        );
    }

    public Map<String, List<String>> findRealmRoleClientComposites(String realm, String roleName) {
        return findClientComposites(
                realm,
                () -> loadRealmRole(realm, roleName)
        ).entrySet()
                .stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().stream()
                                        .map(RoleRepresentation::getName)
                                        .collect(Collectors.toList())
                        )
                );
    }

    public Map<String, List<String>> findClientRoleClientComposites(String realm, String roleClientId, String roleName) {
        return findClientComposites(
                realm,
                () -> loadClientRole(realm, roleClientId, roleName)
        ).entrySet()
                .stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().stream()
                                        .map(RoleRepresentation::getName)
                                        .collect(Collectors.toList())
                        )
                );
    }

    public void addRealmRoleRealmComposites(String realm, String roleName, Set<String> realmComposites) {
        addRealmComposites(
                realm,
                realmComposites,
                () -> loadRealmRole(realm, roleName)
        );
    }

    public void addClientRoleRealmComposites(
            String realm,
            String roleClientId,
            String roleName,
            Set<String> realmComposites
    ) {
        addRealmComposites(
                realm,
                realmComposites,
                () -> loadClientRole(realm, roleClientId, roleName)
        );
    }

    public void addRealmRoleClientComposites(String realm, String roleName, String compositeClientId, Collection<String> clientRoles) {
        addClientComposites(
                realm,
                compositeClientId,
                clientRoles,
                () -> loadRealmRole(realm, roleName)
        );
    }

    public void addClientRoleClientComposites(
            String realm,
            String roleClientId,
            String roleName,
            String compositeClientId,
            Collection<String> clientComposites
    ) {
        addClientComposites(
                realm,
                compositeClientId,
                clientComposites,
                () -> loadClientRole(realm, roleClientId, roleName)
        );
    }

    public void removeRealmRoleRealmComposites(String realm, String roleName, Set<String> realmComposites) {
        removeRealmComposites(
                realm,
                realmComposites,
                () -> loadRealmRole(realm, roleName)
        );
    }

    public void removeClientRoleRealmComposites(String realm, String roleClientId, String roleName, Set<String> realmComposites) {
        removeRealmComposites(
                realm,
                realmComposites,
                () -> loadClientRole(realm, roleClientId, roleName)
        );
    }

    public void removeRealmRoleClientComposites(String realm, String roleName, Map<String, List<String>> clientCompositesToRemove) {
        removeClientComposites(
                realm,
                clientCompositesToRemove,
                () -> loadRealmRole(realm, roleName)
        );
    }

    public void removeClientRoleClientComposites(String realm, String roleClientId, String roleName, Map<String, List<String>> clientCompositesToRemove) {
        removeClientComposites(
                realm,
                clientCompositesToRemove,
                () -> loadClientRole(realm, roleClientId, roleName)
        );
    }

    public void removeRealmRoleClientComposites(String realm, String roleName, String compositeClientId, Collection<String> clientRoleNames) {
        removeClientComposites(
                realm,
                compositeClientId,
                clientRoleNames,
                () -> loadRealmRole(realm, roleName)
        );
    }

    public void removeClientRoleClientComposites(String realm, String roleClientId, String roleName, String compositeClientId, Collection<String> clientRoleNames) {
        removeClientComposites(
                realm,
                compositeClientId,
                clientRoleNames,
                () -> loadClientRole(realm, roleClientId, roleName)
        );
    }

    private void addRealmComposites(String realm, Set<String> realmComposites, Supplier<RoleResource> roleSupplier) {
        RoleResource roleResource = roleSupplier.get();

        List<RoleRepresentation> realmRoles = realmComposites.stream()
                .map(realmRoleName -> roleRepository.findRealmRole(realm, realmRoleName))
                .collect(Collectors.toList());

        roleResource.addComposites(realmRoles);
    }

    private void addClientComposites(String realm, String compositeClientId, Collection<String> clientRoles, Supplier<RoleResource> roleSupplier) {
        RoleResource roleResource = roleSupplier.get();

        List<RoleRepresentation> realmRoles = clientRoles.stream()
                .map(clientRoleName -> roleRepository.findClientRole(realm, compositeClientId, clientRoleName))
                .collect(Collectors.toList());

        roleResource.addComposites(realmRoles);
    }

    private void removeRealmComposites(String realm, Set<String> realmComposites, Supplier<RoleResource> roleSupplier) {
        RoleResource roleResource = roleSupplier.get();

        List<RoleRepresentation> realmRoles = realmComposites.stream()
                .map(realmRoleName -> roleRepository.findRealmRole(realm, realmRoleName))
                .collect(Collectors.toList());

        roleResource.deleteComposites(realmRoles);
    }

    private void removeClientComposites(String realm, Map<String, List<String>> clientCompositesToRemove, Supplier<RoleResource> roleSupplier) {
        RoleResource roleResource = roleSupplier.get();
        List<RoleRepresentation> clientRolesToRemove = findAllClientRoles(realm, clientCompositesToRemove);

        roleResource.deleteComposites(clientRolesToRemove);
    }

    private void removeClientComposites(String realm, String compositeClientId, Collection<String> clientRoleNames, Supplier<RoleResource> roleSupplier) {
        RoleResource roleResource = roleSupplier.get();

        List<RoleRepresentation> clientRoles = clientRoleNames.stream()
                .map(clientRoleName -> roleRepository.findClientRole(realm, compositeClientId, clientRoleName))
                .collect(Collectors.toList());

        roleResource.deleteComposites(clientRoles);
    }

    private MultivaluedHashMap<String, RoleRepresentation> findClientComposites(String realm, Supplier<RoleResource> roleSupplier) {
        MultivaluedHashMap<String, RoleRepresentation> clientComposites = new MultivaluedHashMap<>();

        List<ClientRepresentation> clients = clientRepository.getClients(realm);

        for (ClientRepresentation client : clients) {
            Set<RoleRepresentation> clientRoleComposites = findClientComposites(realm, client.getClientId(), roleSupplier);
            clientComposites.addAll(client.getClientId(), new ArrayList<>(clientRoleComposites));
        }

        return clientComposites;
    }

    private Set<RoleRepresentation> findClientComposites(String realm, String clientId, Supplier<RoleResource> roleSupplier) {
        RoleResource roleResource = roleSupplier.get();
        ClientRepresentation client = clientRepository.getClient(realm, clientId);

        return roleResource.getClientRoleComposites(client.getId());
    }

    private List<RoleRepresentation> findAllClientRoles(String realm, Map<String, List<String>> clientCompositesToRemove) {
        List<RoleRepresentation> clientRolesToRemove = new ArrayList<>();

        for (Map.Entry<String, List<String>> clientCompositeToRemove : clientCompositesToRemove.entrySet()) {
            String clientId = clientCompositeToRemove.getKey();

            for (String role : clientCompositeToRemove.getValue()) {
                clientRolesToRemove.add(roleRepository.findClientRole(realm, clientId, role));
            }
        }

        return clientRolesToRemove;
    }

    private RoleResource loadRealmRole(String realm, String roleName) {
        return roleRepository.loadRealmRole(realm, roleName);
    }

    private RoleResource loadClientRole(String realm, String clientId, String roleName) {
        return roleRepository.loadClientRole(realm, clientId, roleName);
    }
}
