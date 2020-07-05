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
import de.adorsys.keycloak.config.util.ResponseUtil;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GroupRepository {

    private final RealmRepository realmRepository;
    private final RoleRepository roleRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    @Autowired
    public GroupRepository(
            RealmRepository realmRepository,
            RoleRepository roleRepository,
            ClientRepository clientRepository,
            UserRepository userRepository) {
        this.realmRepository = realmRepository;
        this.roleRepository = roleRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
    }

    public List<GroupRepresentation> getGroups(String realm) {
        GroupsResource groupsResource = realmRepository.loadRealm(realm)
                .groups();

        return groupsResource.groups();
    }

    public List<GroupRepresentation> searchGroups(String realmName, List<String> groupNames) {
        List<GroupRepresentation> roles = new ArrayList<>();
        GroupsResource groupsResource = realmRepository.loadRealm(realmName).groups();

        for (String groupName : groupNames) {
            GroupRepresentation role = groupsResource.groups(groupName, 0, 500)
                    .stream().filter(group -> group.getName().equals(groupName))
                    .findFirst()
                    .orElseThrow(() -> new ImportProcessingException("Could not find group '" + groupName + "' in realm '" + realmName + "'!"));

            roles.add(role);
        }

        return roles;
    }

    public Optional<GroupRepresentation> tryToFindGroupByName(String realm, String groupName) {
        GroupsResource groupsResource = realmRepository.loadRealm(realm)
                .groups();

        return groupsResource.groups()
                .stream()
                .filter(g -> Objects.equals(g.getName(), groupName))
                .findFirst();
    }

    public void createGroup(String realm, GroupRepresentation group) {
        Response response = realmRepository.loadRealm(realm)
                .groups()
                .add(group);

        ResponseUtil.validate(response);
    }

    public void addSubGroup(String realm, String parentGroupId, GroupRepresentation subGroup) {
        GroupResource groupResource = loadGroupById(realm, parentGroupId);
        Response response = groupResource.subGroup(subGroup);

        ResponseUtil.validate(response);
    }

    public GroupRepresentation getSubGroupByName(String realm, String parentGroupId, String name) {
        GroupRepresentation existingGroup = loadGroupById(realm, parentGroupId).toRepresentation();

        return existingGroup.getSubGroups()
                .stream()
                .filter(subgroup -> Objects.equals(subgroup.getName(), name))
                .findFirst()
                .orElse(null);
    }

    public void addRealmRoles(String realm, String groupId, List<String> roleNames) {
        GroupResource groupResource = loadGroupById(realm, groupId);
        RoleMappingResource groupRoles = groupResource.roles();
        RoleScopeResource groupRealmRoles = groupRoles.realmLevel();

        List<RoleRepresentation> existingRealmRoles = roleNames.stream()
                .map(realmRole -> roleRepository.findRealmRole(realm, realmRole))
                .collect(Collectors.toList());

        groupRealmRoles.add(existingRealmRoles);
    }

    public void removeRealmRoles(String realm, String groupId, List<String> roleNames) {
        GroupResource groupResource = loadGroupById(realm, groupId);
        RoleMappingResource groupRoles = groupResource.roles();
        RoleScopeResource groupRealmRoles = groupRoles.realmLevel();

        List<RoleRepresentation> existingRealmRoles = roleNames.stream()
                .map(realmRole -> roleRepository.findRealmRole(realm, realmRole))
                .collect(Collectors.toList());

        groupRealmRoles.remove(existingRealmRoles);
    }

    public void deleteGroup(String realm, String id) {
        GroupResource groupResource = loadGroupById(realm, id);
        groupResource.remove();
    }

    public void addGroupsToUser(String realm, String username, List<GroupRepresentation> groups) {
        UserResource userResource = userRepository.getUserResource(realm, username);
        for (GroupRepresentation group : groups) {
            userResource.joinGroup(group.getId());
        }
    }

    public void removeGroupsFromUser(String realm, String username, List<GroupRepresentation> groups) {
        UserResource userResource = userRepository.getUserResource(realm, username);
        for (GroupRepresentation group : groups) {
            userResource.leaveGroup(group.getId());
        }
    }


    public void addClientRoles(String realm, String groupId, String clientId, List<String> roleNames) {
        GroupResource groupResource = loadGroupById(realm, groupId);
        RoleMappingResource rolesResource = groupResource.roles();

        ClientRepresentation client = clientRepository.getClientByClientId(realm, clientId);
        RoleScopeResource groupClientRolesResource = rolesResource.clientLevel(client.getId());

        List<RoleRepresentation> clientRoles = roleRepository.searchClientRoles(realm, clientId, roleNames);
        groupClientRolesResource.add(clientRoles);
    }

    public void removeClientRoles(String realm, String groupId, String clientId, List<String> roleNames) {
        GroupResource groupResource = loadGroupById(realm, groupId);
        RoleMappingResource rolesResource = groupResource.roles();

        ClientRepresentation client = clientRepository.getClientByClientId(realm, clientId);
        RoleScopeResource groupClientRolesResource = rolesResource.clientLevel(client.getId());

        List<RoleRepresentation> clientRoles = roleRepository.searchClientRoles(realm, clientId, roleNames);
        groupClientRolesResource.remove(clientRoles);
    }

    public void update(String realm, GroupRepresentation group) {
        GroupResource groupResource = loadGroupById(realm, group.getId());
        groupResource.update(group);
    }

    public GroupRepresentation getGroupByName(String realm, String groupName) {
        GroupResource groupResource = loadGroupByName(realm, groupName);

        if (groupResource == null) {
            return null;
        }

        return groupResource.toRepresentation();
    }

    public GroupRepresentation getGroupById(String realm, String groupId) {
        GroupResource groupResource = loadGroupById(realm, groupId);
        return groupResource.toRepresentation();
    }

    private GroupResource loadGroupByName(String realm, String groupName) {
        Optional<GroupRepresentation> maybeGroup = tryToFindGroupByName(realm, groupName);

        GroupRepresentation existingGroup = maybeGroup.orElse(null);

        if (existingGroup == null) {
            return null;
        }

        return loadGroupById(realm, existingGroup.getId());
    }

    private GroupResource loadGroupById(String realm, String groupId) {
        return realmRepository.loadRealm(realm)
                .groups()
                .group(groupId);
    }
}
