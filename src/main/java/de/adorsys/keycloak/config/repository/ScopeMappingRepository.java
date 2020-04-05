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
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ScopeMappingRepository {

    private final RealmRepository realmRepository;
    private final ClientRepository clientRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public ScopeMappingRepository(
            RealmRepository realmRepository,
            ClientRepository clientRepository,
            RoleRepository roleRepository
    ) {
        this.realmRepository = realmRepository;
        this.clientRepository = clientRepository;
        this.roleRepository = roleRepository;
    }

    public void addScopeMappingRolesForClient(String realm, String clientId, Collection<String> roles) {
        ClientResource clientResource = clientRepository.getClientResource(realm, clientId);
        RoleMappingResource scopeMappingsResource = clientResource.getScopeMappings();
        RoleScopeResource roleScopeResource = scopeMappingsResource.realmLevel();

        List<RoleRepresentation> realmRoles = roleRepository.findRealmRoles(realm, roles);
        roleScopeResource.add(realmRoles);
    }

    public void addScopeMappingRolesForClientScope(String realm, String clientScopeName, Collection<String> roles) {
        RoleScopeResource roleScopeResource = loadClientScope(realm, clientScopeName);

        List<RoleRepresentation> realmRoles = roleRepository.findRealmRoles(realm, roles);
        roleScopeResource.add(realmRoles);
    }

    public void removeScopeMappingRolesForClient(String realm, String clientId, Collection<String> roles) {
        RoleMappingResource scopeMappingsResource = clientRepository.getClientResource(realm, clientId).getScopeMappings();

        List<RoleRepresentation> realmRoles = roles.stream()
                .map(role -> roleRepository.findRealmRole(realm, role))
                .collect(Collectors.toList());

        scopeMappingsResource.realmLevel().remove(realmRoles);
    }

    public void removeScopeMappingRolesForClientScope(String realm, String clientScopeName, Collection<String> roles) {
        RoleScopeResource roleScopeResource = loadClientScope(realm, clientScopeName);

        List<RoleRepresentation> realmRoles = roleRepository.findRealmRoles(realm, roles);
        roleScopeResource.remove(realmRoles);
    }

    public void addScopeMapping(String realm, ScopeMappingRepresentation scopeMapping) {
        String client = scopeMapping.getClient();
        String clientScope = scopeMapping.getClientScope();

        if (client != null) {
            addScopeMappingRolesForClient(realm, client, scopeMapping.getRoles());
        } else if (clientScope != null) {
            addScopeMappingRolesForClientScope(realm, clientScope, scopeMapping.getRoles());
        }
    }

    private RoleScopeResource loadClientScope(String realm, String clientScopeName) {
        RealmResource realmResource = realmRepository.loadRealm(realm);
        ClientScopesResource clientScopesResource = realmResource.clientScopes();
        ClientScopeRepresentation clientScope = findClientScope(realm, clientScopeName);

        ClientScopeResource clientScopeResource = clientScopesResource.get(clientScope.getId());

        RoleMappingResource scopeMappingsResource = clientScopeResource.getScopeMappings();

        return scopeMappingsResource.realmLevel();
    }

    private ClientScopeRepresentation findClientScope(String realm, String clientScopeName) {
        RealmResource realmResource = realmRepository.loadRealm(realm);
        ClientScopesResource clientScopesResource = realmResource.clientScopes();

        return clientScopesResource.findAll().stream()
                .filter(c -> Objects.equals(c.getName(), clientScopeName))
                .findFirst()
                .orElseThrow(() -> new KeycloakRepositoryException("Cannot find client-scope by name '" + clientScopeName + "'"));
    }
}
