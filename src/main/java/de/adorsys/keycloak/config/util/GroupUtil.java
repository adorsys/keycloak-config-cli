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

package de.adorsys.keycloak.config.util;

import de.adorsys.keycloak.config.repository.GroupRepository;
import de.adorsys.keycloak.config.repository.RealmRepository;
import org.keycloak.representations.idm.GroupRepresentation;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.ws.rs.NotFoundException;

public class GroupUtil {

    private static final String DELIMITER = "/";

    private GroupUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static GroupRepresentation getGroupByPath(
            GroupRepository groupRepository,
            RealmRepository realmRepository,
            String realmName,
            String groupPath
    ) {
        String groupPathStartingWithSlash = groupPath.startsWith(DELIMITER) ? groupPath : DELIMITER + groupPath;

        return realmRepository.getResource(realmName).groups().groups().stream()
                .map(group -> getMatchingGroup(groupRepository, realmName, group, groupPathStartingWithSlash, 0))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new NotFoundException(String.format(
                        "Unable to add default group '%s'. Does group exists?",
                        groupPathStartingWithSlash
                )));
    }

    private static GroupRepresentation getMatchingGroup(
            GroupRepository groupRepository,
            String realmName,
            GroupRepresentation groupRepresentation,
            String groupPath,
            int depth
    ) {
        if (groupPath.equals(groupRepresentation.getPath())) {
            return groupRepresentation;
        }

        int consideredParts = depth + 2;
        String[] parts = groupPath.split(DELIMITER);

        String currentDepthPath =
                Arrays.stream(parts).limit(consideredParts).collect(Collectors.joining(DELIMITER));

        if (!currentDepthPath.equals(groupRepresentation.getPath())) {
            return null;
        }

        return groupRepository.getSubGroups(realmName, groupRepresentation.getId()).stream()
            .map(group -> getMatchingGroup(groupRepository, realmName, group, groupPath, depth + 1))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }
}
