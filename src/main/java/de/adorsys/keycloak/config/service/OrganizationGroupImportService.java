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

import de.adorsys.keycloak.config.condition.ConditionalOnKeycloakVersion26OrNewer;
import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.model.OrganizationGroupRepresentation;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.repository.OrganizationGroupRepository;
import de.adorsys.keycloak.config.repository.UserRepository;
import de.adorsys.keycloak.config.util.VersionUtil;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Imports the {@code groups} of an organization (Keycloak 26.6+, #1693).
 * <pre>
 * organizations:
 *   - alias: acme
 *     groups:
 *       - name: engineering
 *         description: Engineering          # optional, not declared = keep
 *         attributes: {cost-center: ["42"]} # optional, not declared = keep
 *         members: [alice, bob]             # optional usernames, must be organization members
 *         subGroups:
 *           - name: backend
 * </pre>
 * Groups are matched by name below their parent. Groups that are not declared on a level whose list is declared
 * are deleted when {@code import.managed.organization} is {@code full} (the default). An organization without a
 * {@code groups} key, or a group without {@code subGroups} / {@code members}, leaves that part untouched.
 */
@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
@ConditionalOnKeycloakVersion26OrNewer
public class OrganizationGroupImportService {

    static final String MIN_KEYCLOAK_VERSION = "26.6";

    private static final Logger logger = LoggerFactory.getLogger(OrganizationGroupImportService.class);

    private final OrganizationGroupRepository groupRepository;
    private final UserRepository userRepository;
    private final KeycloakProvider keycloakProvider;
    private final ImportConfigProperties importConfigProperties;

    public OrganizationGroupImportService(
            OrganizationGroupRepository groupRepository,
            UserRepository userRepository,
            KeycloakProvider keycloakProvider,
            ImportConfigProperties importConfigProperties
    ) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.keycloakProvider = keycloakProvider;
        this.importConfigProperties = importConfigProperties;
    }

    /**
     * @param declaredGroups the parsed {@code groups} of the organization, {@code null} if not declared
     */
    public void importGroups(String realmName, String orgId, String orgAlias, List<DeclaredGroup> declaredGroups) {
        if (declaredGroups == null) return;

        String keycloakVersion = keycloakProvider.getKeycloakVersion();
        if (!VersionUtil.ge(keycloakVersion, MIN_KEYCLOAK_VERSION)) {
            logger.warn("Organization '{}' in realm '{}' declares groups, but organization groups require Keycloak {}+ "
                    + "(found {}); skipping them.", orgAlias, realmName, MIN_KEYCLOAK_VERSION, keycloakVersion);
            return;
        }

        syncLevel(new Context(realmName, orgId, orgAlias), null, "", declaredGroups);
    }

    private void syncLevel(Context ctx, String parentId, String parentPath, List<DeclaredGroup> declaredGroups) {
        Map<String, OrganizationGroupRepresentation> existingByName = groupRepository
                .getChildren(ctx.realmName(), ctx.orgId(), parentId)
                .stream()
                .filter(group -> group.getName() != null)
                .collect(Collectors.toMap(OrganizationGroupRepresentation::getName, Function.identity(),
                        (first, second) -> first, LinkedHashMap::new));

        for (DeclaredGroup declared : declaredGroups) {
            String path = parentPath + "/" + declared.name();
            OrganizationGroupRepresentation existing = existingByName.get(declared.name());
            String groupId = existing == null
                    ? createGroup(ctx, parentId, path, declared)
                    : updateGroupIfNecessary(ctx, existing.getId(), path, declared);

            if (declared.members() != null) {
                syncMembers(ctx, groupId, path, declared.members());
            }
            if (declared.subGroups() != null) {
                syncLevel(ctx, groupId, path, declared.subGroups());
            }
        }

        if (importConfigProperties.getManaged().getOrganization() != ImportManagedPropertiesValues.FULL) return;

        Set<String> declaredNames = declaredGroups.stream().map(DeclaredGroup::name).collect(Collectors.toSet());
        for (OrganizationGroupRepresentation existing : existingByName.values()) {
            if (!declaredNames.contains(existing.getName())) {
                logger.debug("Delete group '{}/{}' of organization '{}' in realm '{}'",
                        parentPath, existing.getName(), ctx.orgAlias(), ctx.realmName());
                groupRepository.delete(ctx.realmName(), ctx.orgId(), existing.getId());
            }
        }
    }

    private String createGroup(Context ctx, String parentId, String path, DeclaredGroup declared) {
        logger.debug("Create group '{}' of organization '{}' in realm '{}'", path, ctx.orgAlias(), ctx.realmName());
        String groupId = groupRepository.create(ctx.realmName(), ctx.orgId(), parentId,
                new OrganizationGroupRepresentation(declared.name(), null, null));

        // Keycloak ignores description and attributes when creating an organization group
        if (declared.description() != null || declared.attributes() != null) {
            groupRepository.update(ctx.realmName(), ctx.orgId(), groupId,
                    new OrganizationGroupRepresentation(declared.name(), declared.description(), declared.attributes()));
        }
        return groupId;
    }

    private String updateGroupIfNecessary(Context ctx, String groupId, String path, DeclaredGroup declared) {
        OrganizationGroupRepresentation current = groupRepository.getGroup(ctx.realmName(), ctx.orgId(), groupId);

        String description = declared.description() != null ? declared.description() : current.getDescription();
        Map<String, List<String>> attributes = declared.attributes() != null
                ? declared.attributes()
                : current.getAttributes();

        boolean descriptionChanged = !Objects.equals(emptyToNull(description), emptyToNull(current.getDescription()));
        boolean attributesChanged = !Objects.equals(emptyToNull(attributes), emptyToNull(current.getAttributes()));
        if (descriptionChanged || attributesChanged) {
            logger.debug("Update group '{}' of organization '{}' in realm '{}'", path, ctx.orgAlias(), ctx.realmName());
            groupRepository.update(ctx.realmName(), ctx.orgId(), groupId,
                    new OrganizationGroupRepresentation(declared.name(), description, attributes));
        } else {
            logger.debug("No need to update group '{}' of organization '{}' in realm '{}'",
                    path, ctx.orgAlias(), ctx.realmName());
        }
        return groupId;
    }

    private void syncMembers(Context ctx, String groupId, String path, List<String> declaredUsernames) {
        List<String> existingUsernames = groupRepository.getMemberUsernames(ctx.realmName(), ctx.orgId(), groupId);

        for (String username : declaredUsernames) {
            if (!existingUsernames.contains(username)) {
                groupRepository.addMember(ctx.realmName(), ctx.orgId(), groupId, userId(ctx, username, path));
            }
        }
        for (String username : existingUsernames) {
            if (!declaredUsernames.contains(username)) {
                groupRepository.removeMember(ctx.realmName(), ctx.orgId(), groupId, userId(ctx, username, path));
            }
        }
    }

    private String userId(Context ctx, String username, String path) {
        return userRepository.search(ctx.realmName(), username)
                .map(UserRepresentation::getId)
                .orElseThrow(() -> new ImportProcessingException(
                        "Cannot find user '%s' in realm '%s' for group '%s' of organization '%s'",
                        username, ctx.realmName(), path, ctx.orgAlias()));
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static <K, V> Map<K, V> emptyToNull(Map<K, V> value) {
        return value == null || value.isEmpty() ? null : value;
    }

    /**
     * Parses the raw {@code groups} of an organization.
     *
     * @return {@code null} if the organization does not declare {@code groups}
     */
    public static List<DeclaredGroup> parse(Map<String, Object> rawOrganization) {
        if (!rawOrganization.containsKey("groups")) return null;
        return parseList(rawOrganization.get("groups"), "groups");
    }

    @SuppressWarnings("unchecked")
    private static List<DeclaredGroup> parseList(Object raw, String property) {
        if (raw == null) return List.of();
        if (!(raw instanceof Collection<?> rawGroups)) {
            throw new ImportProcessingException("Organization '%s' must be a list", property);
        }
        List<DeclaredGroup> groups = new ArrayList<>();
        for (Object rawGroup : rawGroups) {
            if (!(rawGroup instanceof Map<?, ?> map) || map.get("name") == null) {
                throw new ImportProcessingException("Every organization group needs a 'name'");
            }
            Map<String, Object> group = (Map<String, Object>) map;
            groups.add(new DeclaredGroup(
                    String.valueOf(group.get("name")),
                    group.get("description") == null ? null : String.valueOf(group.get("description")),
                    group.containsKey("attributes") ? toAttributes(group.get("attributes")) : null,
                    group.containsKey("subGroups") ? parseList(group.get("subGroups"), "subGroups") : null,
                    group.containsKey("members") ? toStrings(group.get("members")) : null));
        }
        return groups;
    }

    private static Map<String, List<String>> toAttributes(Object raw) {
        Map<String, List<String>> attributes = new LinkedHashMap<>();
        if (raw instanceof Map<?, ?> map) {
            map.forEach((key, value) -> attributes.put(String.valueOf(key), toStrings(value)));
        }
        return attributes;
    }

    private static List<String> toStrings(Object raw) {
        if (raw == null) return List.of();
        if (raw instanceof Collection<?> values) {
            return values.stream().map(String::valueOf).toList();
        }
        return List.of(String.valueOf(raw));
    }

    /**
     * An organization group as declared in the import; {@code null} properties are not declared and kept as they
     * are in Keycloak.
     */
    public record DeclaredGroup(
            String name,
            String description,
            Map<String, List<String>> attributes,
            List<DeclaredGroup> subGroups,
            List<String> members
    ) {
    }

    private record Context(String realmName, String orgId, String orgAlias) {
    }
}
