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
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues;
import de.adorsys.keycloak.config.repository.GroupRepository;
import de.adorsys.keycloak.config.util.CloneUtil;
import org.keycloak.representations.idm.GroupRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GroupImportService {
    private static final Logger logger = LoggerFactory.getLogger(GroupImportService.class);

    private final GroupRepository groupRepository;
    private final ImportConfigProperties importConfigProperties;

    public GroupImportService(GroupRepository groupRepository, ImportConfigProperties importConfigProperties) {
        this.groupRepository = groupRepository;
        this.importConfigProperties = importConfigProperties;
    }

    public void importGroups(RealmImport realmImport) {
        List<GroupRepresentation> groups = realmImport.getGroups();
        String realm = realmImport.getRealm();

        if (groups == null) {
            logger.debug("No groups to import into realm '{}'", realm);
        } else {
            importGroups(realm, groups);
        }
    }

    private void importGroups(String realm, List<GroupRepresentation> groups) {
        List<GroupRepresentation> existingGroups = groupRepository.getGroups(realm);

        if (groups.isEmpty()) {
            if (importConfigProperties.getManaged().getClientScope() == ImportManagedPropertiesValues.noDelete) {
                logger.info("Skip deletion of groups");
                return;
            }

            deleteAllExistingGroups(realm, existingGroups);
        } else {
            if (importConfigProperties.getManaged().getClientScope() == ImportManagedPropertiesValues.full) {
                deleteGroupsMissingInImport(realm, groups, existingGroups);
            }

            for (GroupRepresentation group : groups) {
                createOrUpdateRealmGroup(realm, group);
            }
        }
    }

    private void deleteGroupsMissingInImport(String realm, List<GroupRepresentation> groups, List<GroupRepresentation> existingGroups) {
        for (GroupRepresentation existingGroup : existingGroups) {
            if (!hasGroupWithName(groups, existingGroup.getName())) {
                logger.debug("Delete group '{}' in realm '{}'", existingGroup.getName(), realm);
                groupRepository.deleteGroup(realm, existingGroup.getId());
            }
        }
    }

    private void deleteAllExistingGroups(String realm, List<GroupRepresentation> existingGroups) {
        for (GroupRepresentation existingGroup : existingGroups) {
            logger.debug("Delete group '{}' in realm '{}'", existingGroup.getName(), realm);
            groupRepository.deleteGroup(realm, existingGroup.getId());
        }
    }

    private boolean hasGroupWithName(List<GroupRepresentation> groups, String groupName) {
        return groups.stream().anyMatch(g -> Objects.equals(g.getName(), groupName));
    }

    private void createOrUpdateRealmGroup(String realm, GroupRepresentation group) {
        String groupName = group.getName();

        Optional<GroupRepresentation> maybeGroup = groupRepository.tryToFindGroupByName(realm, groupName);

        if (maybeGroup.isPresent()) {
            updateGroupIfNecessary(realm, group);
        } else {
            logger.debug("Create group '{}' in realm '{}'", groupName, realm);
            createGroup(realm, group);
        }
    }

    private void createGroup(String realm, GroupRepresentation group) {
        groupRepository.createGroup(realm, group);

        GroupRepresentation existingGroup = groupRepository.getGroupByName(realm, group.getName());
        GroupRepresentation patchedGroup = CloneUtil.patch(existingGroup, group);

        addRealmRoles(realm, patchedGroup);
        addClientRoles(realm, patchedGroup);
        addSubGroups(realm, patchedGroup);
    }

    private void addRealmRoles(String realm, GroupRepresentation existingGroup) {
        List<String> realmRoles = existingGroup.getRealmRoles();

        if (realmRoles != null && !realmRoles.isEmpty()) {
            groupRepository.addRealmRoles(realm, existingGroup.getId(), realmRoles);
        }
    }

    private void addClientRoles(String realm, GroupRepresentation existingGroup) {
        Map<String, List<String>> existingClientRoles = existingGroup.getClientRoles();
        String groupId = existingGroup.getId();

        if (existingClientRoles != null && !existingClientRoles.isEmpty()) {
            for (Map.Entry<String, List<String>> existingClientRolesEntry : existingClientRoles.entrySet()) {
                String clientId = existingClientRolesEntry.getKey();
                List<String> clientRoleNames = existingClientRolesEntry.getValue();

                groupRepository.addClientRoles(realm, groupId, clientId, clientRoleNames);
            }
        }
    }

    private void addSubGroups(String realm, GroupRepresentation existingGroup) {
        List<GroupRepresentation> subGroups = existingGroup.getSubGroups();
        String groupId = existingGroup.getId();

        if (subGroups != null && !subGroups.isEmpty()) {
            for (GroupRepresentation subGroup : subGroups) {
                addSubGroup(realm, groupId, subGroup);
            }
        }
    }

    public void addSubGroup(String realm, String parentGroupId, GroupRepresentation subGroup) {
        groupRepository.addSubGroup(realm, parentGroupId, subGroup);

        GroupRepresentation existingSubGroup = groupRepository.getSubGroupByName(realm, parentGroupId, subGroup.getName());
        GroupRepresentation patchedGroup = CloneUtil.patch(existingSubGroup, subGroup);

        addRealmRoles(realm, patchedGroup);
        addClientRoles(realm, patchedGroup);
        addSubGroups(realm, patchedGroup);
    }

    private void updateGroupIfNecessary(String realm, GroupRepresentation group) {
        GroupRepresentation existingGroup = groupRepository.getGroupByName(realm, group.getName());
        GroupRepresentation patchedGroup = CloneUtil.patch(existingGroup, group);
        String groupName = existingGroup.getName();

        if (CloneUtil.deepEquals(existingGroup, patchedGroup)) {
            logger.debug("No need to update group '{}' in realm '{}'", groupName, realm);
        } else {
            logger.debug("Update group '{}' in realm '{}'", groupName, realm);
            updateGroup(realm, group, patchedGroup);
        }
    }

    private void updateGroup(String realm, GroupRepresentation group, GroupRepresentation patchedGroup) {
        groupRepository.update(realm, patchedGroup);

        String groupId = patchedGroup.getId();

        List<String> realmRoles = group.getRealmRoles();
        if (realmRoles != null) {
            updateGroupRealmRoles(realm, groupId, realmRoles);
        }

        Map<String, List<String>> clientRoles = group.getClientRoles();
        if (clientRoles != null) {
            updateGroupClientRoles(realm, groupId, clientRoles);
        }

        List<GroupRepresentation> subGroups = group.getSubGroups();
        if (subGroups != null) {
            updateSubGroups(realm, patchedGroup.getId(), subGroups);
        }
    }

    private void updateGroupRealmRoles(String realm, String groupId, List<String> realmRoles) {
        GroupRepresentation existingGroup = groupRepository.getGroupById(realm, groupId);

        List<String> existingRealmRolesNames = existingGroup.getRealmRoles();

        List<String> realmRoleNamesToAdd = estimateRealmRolesToAdd(realmRoles, existingRealmRolesNames);
        List<String> realmRoleNamesToRemove = estimateRealmRolesToRemove(realmRoles, existingRealmRolesNames);

        groupRepository.addRealmRoles(realm, groupId, realmRoleNamesToAdd);
        groupRepository.removeRealmRoles(realm, groupId, realmRoleNamesToRemove);
    }

    private List<String> estimateRealmRolesToRemove(List<String> realmRoles, List<String> existingRealmRolesNames) {
        List<String> realmRoleNamesToRemove = new ArrayList<>();

        for (String existingRealmRolesName : existingRealmRolesNames) {
            if (!realmRoles.contains(existingRealmRolesName)) {
                realmRoleNamesToRemove.add(existingRealmRolesName);
            }
        }

        return realmRoleNamesToRemove;
    }

    private List<String> estimateRealmRolesToAdd(List<String> realmRoles, List<String> existingRealmRolesNames) {
        List<String> realmRoleNamesToAdd = new ArrayList<>();

        for (String realmRoleName : realmRoles) {
            if (!existingRealmRolesNames.contains(realmRoleName)) {
                realmRoleNamesToAdd.add(realmRoleName);
            }
        }

        return realmRoleNamesToAdd;
    }

    private void updateGroupClientRoles(String realm, String groupId, Map<String, List<String>> groupClientRoles) {
        GroupRepresentation existingGroup = groupRepository.getGroupById(realm, groupId);

        Map<String, List<String>> existingClientRoleNames = existingGroup.getClientRoles();

        deleteClientRolesMissingInImport(realm, groupId, existingClientRoleNames, groupClientRoles);
        updateClientRoles(realm, groupId, existingClientRoleNames, groupClientRoles);
    }

    private void updateClientRoles(
            String realm,
            String groupId,
            Map<String, List<String>> existingClientRoleNames,
            Map<String, List<String>> groupClientRoles
    ) {
        for (Map.Entry<String, List<String>> clientRole : groupClientRoles.entrySet()) {
            String clientId = clientRole.getKey();
            List<String> clientRoleNames = clientRole.getValue();

            List<String> existingClientRoleNamesForClient = existingClientRoleNames.get(clientId);

            List<String> clientRoleNamesToAdd = estimateClientRolesToAdd(existingClientRoleNamesForClient, clientRoleNames);
            List<String> clientRoleNamesToRemove = estimateClientRolesToRemove(existingClientRoleNamesForClient, clientRoleNames);

            groupRepository.addClientRoles(realm, groupId, clientId, clientRoleNamesToAdd);
            groupRepository.removeClientRoles(realm, groupId, clientId, clientRoleNamesToRemove);
        }
    }

    private void deleteClientRolesMissingInImport(
            String realm,
            String groupId,
            Map<String, List<String>> existingClientRoleNames,
            Map<String, List<String>> groupClientRoles
    ) {
        for (Map.Entry<String, List<String>> existingClientRoleNamesEntry : existingClientRoleNames.entrySet()) {
            String clientId = existingClientRoleNamesEntry.getKey();
            List<String> clientRoleNames = existingClientRoleNamesEntry.getValue();

            if (!clientRoleNames.isEmpty() && !groupClientRoles.containsKey(clientId)) {
                groupRepository.removeClientRoles(realm, groupId, clientId, clientRoleNames);
            }
        }
    }

    private List<String> estimateClientRolesToRemove(List<String> existingClientRoleNamesForClient, List<String> clientRoleNamesFromImport) {
        List<String> clientRoleNamesToRemove = new ArrayList<>();

        if (existingClientRoleNamesForClient != null) {
            for (String existingClientRoleNameForClient : existingClientRoleNamesForClient) {
                if (!clientRoleNamesFromImport.contains(existingClientRoleNameForClient)) {
                    clientRoleNamesToRemove.add(existingClientRoleNameForClient);
                }
            }
        }

        return clientRoleNamesToRemove;
    }

    private List<String> estimateClientRolesToAdd(List<String> existingClientRoleNamesForClient, List<String> clientRoleNamesFromImport) {
        List<String> clientRoleNamesToAdd = new ArrayList<>();

        for (String clientRoleName : clientRoleNamesFromImport) {
            if (existingClientRoleNamesForClient == null || !existingClientRoleNamesForClient.contains(clientRoleName)) {
                clientRoleNamesToAdd.add(clientRoleName);
            }
        }

        return clientRoleNamesToAdd;
    }

    private void updateSubGroups(String realm, String parentGroupId, List<GroupRepresentation> subGroups) {
        GroupRepresentation existingGroup = groupRepository.getGroupById(realm, parentGroupId);
        List<GroupRepresentation> existingSubGroups = existingGroup.getSubGroups();

        deleteAllSubGroupsMissingInImport(realm, subGroups, existingSubGroups);

        for (GroupRepresentation subGroup : subGroups) {
            if (hasGroupWithName(existingSubGroups, subGroup.getName())) {
                updateSubGroupIfNecessary(realm, parentGroupId, subGroup);
            } else {
                addSubGroup(realm, parentGroupId, subGroup);
            }
        }
    }

    private void deleteAllSubGroupsMissingInImport(String realm, List<GroupRepresentation> subGroups, List<GroupRepresentation> existingSubGroups) {
        for (GroupRepresentation existingSubGroup : existingSubGroups) {
            if (!hasGroupWithName(subGroups, existingSubGroup.getName())) {
                groupRepository.deleteGroup(realm, existingSubGroup.getId());
            }
        }
    }

    public void updateSubGroupIfNecessary(String realm, String parentGroupId, GroupRepresentation subGroup) {
        String subGroupName = subGroup.getName();
        GroupRepresentation existingSubGroup = groupRepository.getSubGroupByName(realm, parentGroupId, subGroupName);

        GroupRepresentation patchedSubGroup = CloneUtil.patch(existingSubGroup, subGroup);

        if (CloneUtil.deepEquals(existingSubGroup, patchedSubGroup)) {
            logger.debug("No need to update subGroup '{}' in group with id '{}' in realm '{}'", subGroupName, parentGroupId, realm);
        } else {
            logger.debug("Update subGroup '{}' in group with id '{}' in realm '{}'", subGroupName, parentGroupId, realm);

            updateGroup(realm, subGroup, patchedSubGroup);
        }
    }
}
