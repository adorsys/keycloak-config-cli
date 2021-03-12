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

import de.adorsys.keycloak.config.exception.InvalidImportException;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.GroupRepository;
import de.adorsys.keycloak.config.repository.RealmRepository;
import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultGroupsImportService {
    private final RealmRepository realmRepository;
    private final GroupRepository groupRepository;

    @Autowired
    public DefaultGroupsImportService(
            RealmRepository realmRepository,
            GroupRepository groupRepository
    ) {
        this.realmRepository = realmRepository;
        this.groupRepository = groupRepository;
    }

    public void doImport(RealmImport realmImport) {
        List<String> newDefaultGroups = realmImport.getDefaultGroups();
        if (newDefaultGroups == null) return;

        String realmName = realmImport.getRealm();

        RealmResource realmResource = realmRepository.getResource(realmName);
        List<String> existingDefaultGroups = realmResource.toRepresentation().getDefaultGroups();

        if (existingDefaultGroups != null) {
            for (String existingDefaultGroup : existingDefaultGroups) {
                if (!newDefaultGroups.contains(existingDefaultGroup)) {
                    String existingDefaultGroupId = groupRepository.getGroupByPath(realmName, existingDefaultGroup).getId();
                    realmResource.removeDefaultGroup(existingDefaultGroupId);
                }
            }
        }

        for (String newDefaultGroup : newDefaultGroups) {
            if (existingDefaultGroups == null || !existingDefaultGroups.contains(newDefaultGroup)) {
                try {
                    String newDefaultGroupId = groupRepository.getGroupByPath(realmName, newDefaultGroup).getId();
                    realmResource.addDefaultGroup(newDefaultGroupId);
                } catch (javax.ws.rs.NotFoundException ignored) {
                    throw new InvalidImportException(String.format("Unable to add default group '%s'. Does group exists?", newDefaultGroup));
                }
            }
        }
    }
}
