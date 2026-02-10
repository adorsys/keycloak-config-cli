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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.adorsys.keycloak.config.ThreadHelper;
import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.repository.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.mockito.stubbing.OngoingStubbing;

import java.util.List;
import java.util.Map;

class GroupImportServiceTest {

    private final GroupRepository groupRepository = mock(GroupRepository.class);

    private final ImportConfigProperties importConfigProperties = mock(ImportConfigProperties.class);

    private final ThreadHelper threadHelper = mock(ThreadHelper.class);

    private final GroupImportService groupImportService =
        new GroupImportService(groupRepository, importConfigProperties, threadHelper);

    @Nested
    class CreatingGroupIT {

        private final GroupRepresentation group = new GroupRepresentation();

        private final String realmName = "someRealm";
        private final String groupId = "someGroupId";
        private final String groupName = "someGroup";

        @BeforeEach
        void init() {
            group.setId(groupId);
            group.setName(groupName);
            group.setRealmRoles(List.of("someRealmRole"));
            group.setClientRoles(Map.of("someClientId", List.of("someClientRoleName")));

            GroupRepresentation subGroup = new GroupRepresentation();
            group.setSubGroups(List.of(subGroup));
            subGroup.setName("someSubGroupName");

            when(groupRepository.getGroupByName(realmName, groupName)).thenReturn(null).thenReturn(group);
            when(groupRepository.getSubGroupByName(realmName, groupId, "someSubGroupName")).thenReturn(subGroup);
        }

        @Test
        void createOrUpdateGroups_shouldCreateGroup() {
            groupImportService.createOrUpdateGroups(List.of(group), realmName);
            verify(groupRepository).createGroup(realmName, group);
        }

        @Test
        void createOrUpdateGroups_shouldAddRealmRoles() {
            groupImportService.createOrUpdateGroups(List.of(group), realmName);

            verify(groupRepository).addRealmRoles(
                    eq(realmName),
                    eq(groupId),
                    argThat(realmRoles -> realmRoles.size() == 1 && realmRoles.get(0).equals("someRealmRole"))
            );
        }

        @Test
        void createOrUpdateGroups_shouldAddClientRoles() {
            groupImportService.createOrUpdateGroups(List.of(group), realmName);

            verify(groupRepository).addClientRoles(
                    eq(realmName),
                    eq(groupId),
                    eq("someClientId"),
                    argThat(clientRoleNames -> clientRoleNames.size() == 1 && clientRoleNames.get(0).equals("someClientRoleName"))
            );
        }

        @Test
        void createOrUpdateGroups_shouldAddSubGroups() {
            // Ensure getSubGroupByName returns null so that addSubGroup is called
            when(groupRepository.getSubGroupByName(realmName, groupId, "someSubGroupName")).thenReturn(null);

            groupImportService.createOrUpdateGroups(List.of(group), realmName);

            verify(groupRepository).addSubGroup(
                    eq(realmName),
                    eq(groupId),
                    argThat(subGroup -> subGroup.getName().equals("someSubGroupName"))
            );
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3, 4, 5})
        void createOrUpdateGroups_shouldRetryGettingCreatedGroup(int retries) {
            OngoingStubbing<GroupRepresentation> getGroupNameStubbing =
                    when(groupRepository.getGroupByName(realmName, groupName));

            for (int i = 0; i < retries; i++) {
                getGroupNameStubbing = getGroupNameStubbing.thenReturn(null);
            }

            getGroupNameStubbing.thenReturn(group);

            groupImportService.createOrUpdateGroups(List.of(group), realmName);

            verify(groupRepository, times(retries + 1)).getGroupByName(realmName, groupName);
            verify(threadHelper, times(retries - 1)).sleep(anyLong());
        }

        @Test
        void createOrUpdateGroups_shouldStopImportWithNullAfterEveryRetry() {
            List<GroupRepresentation> groups = List.of(group);

            when(groupRepository.getGroupByName(realmName, groupName))
                    .thenReturn(null)
                    .thenReturn(null)
                    .thenReturn(null)
                    .thenReturn(null)
                    .thenReturn(null)
                    .thenReturn(null)
                    .thenReturn(group);

            ImportProcessingException exception = assertThrows(
                    ImportProcessingException.class,
                    () -> groupImportService.createOrUpdateGroups(groups, realmName)
            );

            assertThat(exception)
                    .message()
                    .isEqualTo(String.format("Cannot find created group '%s' in realm '%s'", groupName, realmName));
        }
    }
}
