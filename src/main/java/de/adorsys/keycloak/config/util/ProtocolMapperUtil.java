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

import org.keycloak.representations.idm.ProtocolMapperRepresentation;

import java.util.*;
import java.util.stream.Collectors;

public class ProtocolMapperUtil {
    private ProtocolMapperUtil() {
    }

    public static List<ProtocolMapperRepresentation> estimateProtocolMappersToRemove(
            List<ProtocolMapperRepresentation> protocolMappers,
            List<ProtocolMapperRepresentation> existingProtocolMappers
    ) {
        if (existingProtocolMappers == null || existingProtocolMappers.isEmpty()) {
            return List.of(); // Return an immutable empty list
        }

        Set<String> protocolMapperNames = Optional.ofNullable(protocolMappers)
                .stream()
                .flatMap(List::stream)
                .map(ProtocolMapperRepresentation::getName)
                .collect(Collectors.toSet());

        return existingProtocolMappers.stream()
                .filter(existingMapper -> !protocolMapperNames.contains(existingMapper.getName()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static List<ProtocolMapperRepresentation> estimateProtocolMappersToAdd(
            List<ProtocolMapperRepresentation> protocolMappers,
            List<ProtocolMapperRepresentation> existingProtocolMappers
    ) {
        List<ProtocolMapperRepresentation> protocolMappersToAdd = new ArrayList<>();

        if (existingProtocolMappers == null) {
            return protocolMappers;
        }

        for (ProtocolMapperRepresentation protocolMapper : protocolMappers) {
            boolean shouldAdd = existingProtocolMappers.stream().noneMatch(
                    m -> Objects.equals(m.getName(), protocolMapper.getName())
            );

            if (shouldAdd) {
                protocolMappersToAdd.add(protocolMapper);
            }
        }

        return protocolMappersToAdd;
    }

    public static List<ProtocolMapperRepresentation> estimateProtocolMappersToUpdate(
            List<ProtocolMapperRepresentation> protocolMappers,
            List<ProtocolMapperRepresentation> existingProtocolMappers
    ) {
        List<ProtocolMapperRepresentation> protocolMappersToUpdate = new ArrayList<>();

        if (existingProtocolMappers == null) {
            return protocolMappersToUpdate;
        }

        for (ProtocolMapperRepresentation protocolMapper : protocolMappers) {
            Optional<ProtocolMapperRepresentation> existingProtocolMapper = existingProtocolMappers
                    .stream()
                    .filter(m -> Objects.equals(m.getName(), protocolMapper.getName()))
                    .findFirst();

            if (existingProtocolMapper.isPresent()) {
                ProtocolMapperRepresentation patchedProtocolMapper = CloneUtil.patch(
                        existingProtocolMapper.get(), protocolMapper, "id"
                );
                protocolMappersToUpdate.add(patchedProtocolMapper);
            }
        }

        return protocolMappersToUpdate;
    }

    public static boolean areProtocolMappersEqual(
            List<ProtocolMapperRepresentation> protocolMappers,
            List<ProtocolMapperRepresentation> existingProtocolMappers
    ) {
        if (protocolMappers == null || protocolMappers.isEmpty()) return existingProtocolMappers == null;
        if (existingProtocolMappers == null || protocolMappers.size() != existingProtocolMappers.size()) return false;

        for (ProtocolMapperRepresentation protocolMapper : protocolMappers) {
            ProtocolMapperRepresentation existingProtocolMapper = existingProtocolMappers.stream()
                    .filter(em -> Objects.equals(em.getName(), protocolMapper.getName()))
                    .findFirst().orElse(null);

            if (existingProtocolMapper == null) {
                return false;
            }

            ProtocolMapperRepresentation patchedSubGroup = CloneUtil.patch(existingProtocolMapper, protocolMapper);
            if (!CloneUtil.deepEquals(existingProtocolMapper, patchedSubGroup, "id")) {
                return false;
            }
        }

        return true;
    }
}
