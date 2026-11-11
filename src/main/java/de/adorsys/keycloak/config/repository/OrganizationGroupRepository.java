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

package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.condition.ConditionalOnKeycloakVersion26OrNewer;
import de.adorsys.keycloak.config.model.OrganizationGroupRepresentation;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.resource.OrganizationGroupsResource;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import jakarta.ws.rs.core.Response;

/**
 * Organization groups (Keycloak 26.6+). Keycloak only allows managing them through the organization API; the
 * realm groups API answers "Cannot manage organization related group via non Organization API".
 */
@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
@ConditionalOnKeycloakVersion26OrNewer
public class OrganizationGroupRepository {

    private static final int FIRST = 0;
    private static final int MAX = Integer.MAX_VALUE;

    private final KeycloakProvider keycloakProvider;

    public OrganizationGroupRepository(KeycloakProvider keycloakProvider) {
        this.keycloakProvider = keycloakProvider;
    }

    /**
     * @param parentId the parent group, or {@code null} for the top-level groups of the organization
     * @return the (brief) child groups; use {@link #getGroup} for description and attributes
     */
    public List<OrganizationGroupRepresentation> getChildren(String realmName, String orgId, String parentId) {
        List<OrganizationGroupRepresentation> groups = parentId == null
                ? resource().getTopLevelGroups(realmName, orgId, FIRST, MAX)
                : resource().getChildren(realmName, orgId, parentId, FIRST, MAX);
        return groups == null ? List.of() : groups;
    }

    public OrganizationGroupRepresentation getGroup(String realmName, String orgId, String groupId) {
        return resource().getGroup(realmName, orgId, groupId);
    }

    /**
     * @return the id of the created group
     */
    public String create(String realmName, String orgId, String parentId, OrganizationGroupRepresentation group) {
        try (Response response = parentId == null
                ? resource().createTopLevelGroup(realmName, orgId, group)
                : resource().createChild(realmName, orgId, parentId, group)) {
            return CreatedResponseUtil.getCreatedId(response);
        }
    }

    public void update(String realmName, String orgId, String groupId, OrganizationGroupRepresentation group) {
        resource().updateGroup(realmName, orgId, groupId, group);
    }

    public void delete(String realmName, String orgId, String groupId) {
        resource().deleteGroup(realmName, orgId, groupId);
    }

    public List<String> getMemberUsernames(String realmName, String orgId, String groupId) {
        List<UserRepresentation> members = resource().getMembers(realmName, orgId, groupId, FIRST, MAX);
        return members == null ? List.of() : members.stream()
                .map(UserRepresentation::getUsername)
                .filter(Objects::nonNull)
                .toList();
    }

    public void addMember(String realmName, String orgId, String groupId, String userId) {
        resource().addMember(realmName, orgId, groupId, userId);
    }

    public void removeMember(String realmName, String orgId, String groupId, String userId) {
        resource().removeMember(realmName, orgId, groupId, userId);
    }

    private OrganizationGroupsResource resource() {
        return keycloakProvider.getCustomApiProxy(OrganizationGroupsResource.class);
    }
}
