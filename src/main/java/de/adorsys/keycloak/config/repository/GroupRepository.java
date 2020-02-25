package com.github.borisskert.keycloak.config.repository;

import com.github.borisskert.keycloak.config.util.ResponseUtil;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GroupRepository {

    private final RealmRepository realmRepository;
    private final RoleRepository roleRepository;
    private final ClientRepository clientRepository;

    @Autowired
    public GroupRepository(
            RealmRepository realmRepository,
            RoleRepository roleRepository,
            ClientRepository clientRepository
    ) {
        this.realmRepository = realmRepository;
        this.roleRepository = roleRepository;
        this.clientRepository = clientRepository;
    }

    public List<GroupRepresentation> getGroups(String realm) {
        GroupsResource groupsResource = realmRepository.loadRealm(realm)
                .groups();

        return groupsResource.groups();
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

        ResponseUtil.throwOnError(response);
    }

    public void addSubGroup(String realm, String parentGroupId, GroupRepresentation subGroup) {
        GroupResource groupResource = loadGroupById(realm, parentGroupId);
        Response response = groupResource.subGroup(subGroup);

        ResponseUtil.throwOnError(response);
    }

    public GroupRepresentation getSubGroupByName(String realm, String parentGroupId, String name) {
        GroupRepresentation existingGroup = loadGroupById(realm, parentGroupId).toRepresentation();

        return existingGroup.getSubGroups()
                .stream()
                .filter(subgroup -> Objects.equals(subgroup.getName(), name))
                .findFirst()
                .get();
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

    public void addClientRoles(String realm, String groupId, String clientId, List<String> roleNames) {
        GroupResource groupResource = loadGroupById(realm, groupId);
        RoleMappingResource rolesResource = groupResource.roles();

        ClientRepresentation client = clientRepository.getClient(realm, clientId);
        RoleScopeResource groupClientRolesResource = rolesResource.clientLevel(client.getId());

        List<RoleRepresentation> clientRoles = roleRepository.searchClientRoles(realm, clientId, roleNames);
        groupClientRolesResource.add(clientRoles);
    }

    public void removeClientRoles(String realm, String groupId, String clientId, List<String> roleNames) {
        GroupResource groupResource = loadGroupById(realm, groupId);
        RoleMappingResource rolesResource = groupResource.roles();

        ClientRepresentation client = clientRepository.getClient(realm, clientId);
        RoleScopeResource groupClientRolesResource = rolesResource.clientLevel(client.getId());

        List<RoleRepresentation> clientRoles = roleRepository.searchClientRoles(realm, clientId, roleNames);
        groupClientRolesResource.remove(clientRoles);
    }

    public void update(String realm, GroupRepresentation group) {
        GroupResource groupResource = loadGroupById(realm, group.getId());
        groupResource.update(group);
    }

    private GroupResource loadGroupByName(String realm, String groupName) {
        Optional<GroupRepresentation> maybeGroup = tryToFindGroupByName(realm, groupName);

        GroupRepresentation existingGroup = maybeGroup.get();

        return loadGroupById(realm, existingGroup.getId());
    }

    public GroupRepresentation getGroupByName(String realm, String groupName) {
        GroupResource groupResource = loadGroupByName(realm, groupName);
        return groupResource.toRepresentation();
    }

    private GroupResource loadGroupById(String realm, String groupId) {
        return realmRepository.loadRealm(realm)
                .groups()
                .group(groupId);
    }

    public GroupRepresentation getGroupById(String realm, String groupId) {
        GroupResource groupResource = loadGroupById(realm, groupId);
        return groupResource.toRepresentation();
    }
}
