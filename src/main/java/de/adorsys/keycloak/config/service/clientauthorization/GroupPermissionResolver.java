/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2022 adorsys GmbH & Co. KG @ https://adorsys.com
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

package de.adorsys.keycloak.config.service.clientauthorization;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.repository.GroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;

public class GroupPermissionResolver implements PermissionResolver {
    private static final Logger logger = LoggerFactory.getLogger(GroupPermissionResolver.class);

    private final String realmName;
    private final GroupRepository groupRepository;

    public GroupPermissionResolver(String realmName, GroupRepository groupRepository) {
        this.realmName = realmName;
        this.groupRepository = groupRepository;
    }

    @Override
    public String resolveObjectId(String groupPath, String authzName) {
        try {
            return groupRepository.getGroupByPath(realmName, groupPath).getId();
        } catch (NotFoundException | KeycloakRepositoryException e) {
            throw new ImportProcessingException("Cannot find group with path '%s' in realm '%s' for '%s'", groupPath, realmName, authzName);
        }
    }

    @Override
    public void enablePermissions(String id) {
        try {
            if (!groupRepository.isPermissionEnabled(realmName, id)) {
                logger.debug("Enable permissions for client '{}' in realm '{}'", id, realmName);
                groupRepository.enablePermission(realmName, id);
            }
        } catch (NotFoundException e) {
            throw new ImportProcessingException("Cannot find group with id '%s' in realm '%s'", id, realmName);
        }
    }
}
