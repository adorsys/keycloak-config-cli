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

package de.adorsys.keycloak.config.service.role;

import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportProtectedRolesProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportProtectedRolesProperties.ProtectedRolesMode;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves whether a realm-level or client-level role is protected from update/delete/composite-sync
 * during import. This is a pure lookup - it consults no remote state and performs no I/O, so it is
 * safe to consult per-role, including from within parallel import loops.
 * <p>
 * A protected client role entry of {@code "*"} protects every role of that client, including roles
 * unknown at configuration time.
 */
@Component
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class ProtectedRoleResolver {

    private static final Collection<String> BUILT_IN_PROTECTED_REALM_ROLES = List.of("admin", "create-realm");
    private static final String BUILT_IN_PROTECTED_CLIENT_ID = "realm-management";
    private static final String WILDCARD = "*";

    private final Set<String> protectedRealmRoles;
    private final Map<String, Set<String>> protectedClientRoles;

    public ProtectedRoleResolver(ImportConfigProperties importConfigProperties) {
        ImportProtectedRolesProperties protectedRolesProperties = importConfigProperties.getProtectedRoles();
        Set<String> realmRoles = new HashSet<>();
        Map<String, Set<String>> clientRoles = new HashMap<>();

        if (protectedRolesProperties.getMode() == ProtectedRolesMode.ADD) {
            realmRoles.addAll(BUILT_IN_PROTECTED_REALM_ROLES);
            clientRoles.put(BUILT_IN_PROTECTED_CLIENT_ID, new HashSet<>(Set.of(WILDCARD)));
        }

        realmRoles.addAll(protectedRolesProperties.getRealmRoles());

        for (Map.Entry<String, Collection<String>> entry : protectedRolesProperties.getClientRoles().entrySet()) {
            clientRoles.computeIfAbsent(entry.getKey(), key -> new HashSet<>()).addAll(entry.getValue());
        }

        this.protectedRealmRoles = Set.copyOf(realmRoles);

        Map<String, Set<String>> immutableClientRoles = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : clientRoles.entrySet()) {
            immutableClientRoles.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        this.protectedClientRoles = Map.copyOf(immutableClientRoles);
    }

    public boolean isRealmRoleProtected(String roleName) {
        if (roleName == null) {
            return false;
        }
        return protectedRealmRoles.contains(roleName);
    }

    public boolean isClientRoleProtected(String clientId, String roleName) {
        if (clientId == null || roleName == null) {
            return false;
        }
        Set<String> roles = protectedClientRoles.get(clientId);
        if (roles == null) {
            return false;
        }
        return roles.contains(WILDCARD) || roles.contains(roleName);
    }

    public List<RoleRepresentation> filterUnprotectedRealmRoles(List<RoleRepresentation> realmRoles) {
        return realmRoles.stream()
                .filter(role -> !isRealmRoleProtected(role.getName()))
                .collect(Collectors.toList());
    }

    public Map<String, List<RoleRepresentation>> filterUnprotectedClientRoles(Map<String, List<RoleRepresentation>> clientRoles) {
        Map<String, List<RoleRepresentation>> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, List<RoleRepresentation>> client : clientRoles.entrySet()) {
            String clientId = client.getKey();
            List<RoleRepresentation> unprotectedRoles = client.getValue().stream()
                    .filter(role -> !isClientRoleProtected(clientId, role.getName()))
                    .collect(Collectors.toList());
            filtered.put(clientId, unprotectedRoles);
        }
        return filtered;
    }
}
