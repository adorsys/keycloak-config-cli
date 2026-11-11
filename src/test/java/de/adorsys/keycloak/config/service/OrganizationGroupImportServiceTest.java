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

import de.adorsys.keycloak.config.model.OrganizationGroupRepresentation;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.repository.OrganizationGroupRepository;
import de.adorsys.keycloak.config.repository.UserRepository;
import de.adorsys.keycloak.config.service.OrganizationGroupImportService.DeclaredGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrganizationGroupImportServiceTest {

    private static final String REALM = "realm";
    private static final String ORG = "org-id";

    private final OrganizationGroupRepository repository = mock(OrganizationGroupRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final KeycloakProvider keycloakProvider = mock(KeycloakProvider.class);
    private final ImportConfigProperties properties = mock(ImportConfigProperties.class);
    private final ImportConfigProperties.ImportManagedProperties managed = mock(ImportConfigProperties.ImportManagedProperties.class);

    private final OrganizationGroupImportService service =
            new OrganizationGroupImportService(repository, userRepository, keycloakProvider, properties);

    @BeforeEach
    void setUp() {
        when(keycloakProvider.getKeycloakVersion()).thenReturn("26.8.1");
        when(properties.getManaged()).thenReturn(managed);
        when(managed.getOrganization()).thenReturn(ImportManagedPropertiesValues.FULL);
        when(repository.getChildren(eq(REALM), eq(ORG), any())).thenReturn(List.of());
    }

    @Test
    void parse_shouldReturnNullWhenGroupsAreNotDeclared() {
        assertThat(OrganizationGroupImportService.parse(Map.of("alias", "acme"))).isNull();
    }

    @Test
    void parse_shouldReadNestedGroups() {
        Map<String, Object> raw = Map.of("groups", List.of(Map.of(
                "name", "engineering",
                "description", "Eng",
                "attributes", Map.of("cost", List.of("42")),
                "members", List.of("alice"),
                "subGroups", List.of(Map.of("name", "backend")))));

        List<DeclaredGroup> groups = OrganizationGroupImportService.parse(raw);

        assertThat(groups).hasSize(1);
        DeclaredGroup group = groups.get(0);
        assertThat(group.name()).isEqualTo("engineering");
        assertThat(group.description()).isEqualTo("Eng");
        assertThat(group.attributes()).containsEntry("cost", List.of("42"));
        assertThat(group.members()).containsExactly("alice");
        assertThat(group.subGroups()).extracting(DeclaredGroup::name).containsExactly("backend");
        assertThat(group.subGroups().get(0).subGroups()).isNull();
        assertThat(group.subGroups().get(0).members()).isNull();
    }

    @Test
    void importGroups_shouldDoNothingWhenGroupsAreNotDeclared() {
        service.importGroups(REALM, ORG, "acme", null);

        verify(repository, never()).getChildren(anyString(), anyString(), any());
    }

    @Test
    void importGroups_shouldSkipOnKeycloakBefore266() {
        when(keycloakProvider.getKeycloakVersion()).thenReturn("26.5.5");

        service.importGroups(REALM, ORG, "acme", List.of(group("engineering")));

        verify(repository, never()).create(anyString(), anyString(), any(), any());
    }

    @Test
    void importGroups_shouldCreateGroupAndSetDescriptionAndAttributesAfterwards() {
        when(repository.create(eq(REALM), eq(ORG), isNull(), any())).thenReturn("g1");
        DeclaredGroup declared = new DeclaredGroup("engineering", "Eng", Map.of("cost", List.of("42")), List.of(group("backend")), null);
        when(repository.create(eq(REALM), eq(ORG), eq("g1"), any())).thenReturn("g2");

        service.importGroups(REALM, ORG, "acme", List.of(declared));

        verify(repository).create(eq(REALM), eq(ORG), isNull(), argThat(g -> "engineering".equals(g.getName())));
        verify(repository).update(eq(REALM), eq(ORG), eq("g1"),
                argThat(g -> "Eng".equals(g.getDescription()) && g.getAttributes().containsKey("cost")));
        verify(repository).create(eq(REALM), eq(ORG), eq("g1"), argThat(g -> "backend".equals(g.getName())));
        verify(repository, never()).update(eq(REALM), eq(ORG), eq("g2"), any());
    }

    @Test
    void importGroups_shouldUpdateOnlyChangedGroupsAndKeepUndeclaredProperties() {
        when(repository.getChildren(REALM, ORG, null)).thenReturn(List.of(existing("g1", "same"), existing("g2", "changed")));
        when(repository.getGroup(REALM, ORG, "g1")).thenReturn(full("g1", "same", "desc", Map.of("a", List.of("1"))));
        when(repository.getGroup(REALM, ORG, "g2")).thenReturn(full("g2", "changed", "old", Map.of("a", List.of("1"))));

        service.importGroups(REALM, ORG, "acme", List.of(
                new DeclaredGroup("same", null, null, null, null),
                new DeclaredGroup("changed", "new", null, null, null)));

        verify(repository, never()).update(eq(REALM), eq(ORG), eq("g1"), any());
        verify(repository).update(eq(REALM), eq(ORG), eq("g2"),
                argThat(g -> "new".equals(g.getDescription()) && g.getAttributes().equals(Map.of("a", List.of("1")))));
    }

    @Test
    void importGroups_shouldDeleteUndeclaredGroupsOnlyWithFullManagement() {
        when(repository.getChildren(REALM, ORG, null)).thenReturn(List.of(existing("g1", "keep"), existing("g2", "stale")));
        when(repository.getGroup(REALM, ORG, "g1")).thenReturn(full("g1", "keep", null, null));

        service.importGroups(REALM, ORG, "acme", List.of(group("keep")));
        verify(repository).delete(REALM, ORG, "g2");

        when(managed.getOrganization()).thenReturn(ImportManagedPropertiesValues.NO_DELETE);
        service.importGroups(REALM, ORG, "acme", List.of(group("keep")));
        verify(repository).delete(REALM, ORG, "g2"); // still only the first call
    }

    @Test
    void importGroups_shouldLeaveSubGroupsAloneWhenNotDeclared() {
        when(repository.getChildren(REALM, ORG, null)).thenReturn(List.of(existing("g1", "engineering")));
        when(repository.getGroup(REALM, ORG, "g1")).thenReturn(full("g1", "engineering", null, null));

        service.importGroups(REALM, ORG, "acme", List.of(group("engineering")));

        verify(repository, never()).getChildren(REALM, ORG, "g1");
    }

    @Test
    void importGroups_shouldSyncMembers() {
        when(repository.getChildren(REALM, ORG, null)).thenReturn(List.of(existing("g1", "engineering")));
        when(repository.getGroup(REALM, ORG, "g1")).thenReturn(full("g1", "engineering", null, null));
        when(repository.getMemberUsernames(REALM, ORG, "g1")).thenReturn(List.of("bob", "carol"));
        when(userRepository.search(REALM, "alice")).thenReturn(Optional.of(user("u-alice")));
        when(userRepository.search(REALM, "carol")).thenReturn(Optional.of(user("u-carol")));

        service.importGroups(REALM, ORG, "acme",
                List.of(new DeclaredGroup("engineering", null, null, null, List.of("alice", "bob"))));

        verify(repository).addMember(REALM, ORG, "g1", "u-alice");
        verify(repository).removeMember(REALM, ORG, "g1", "u-carol");
        verify(repository, never()).addMember(REALM, ORG, "g1", "u-bob");
    }

    private static DeclaredGroup group(String name) {
        return new DeclaredGroup(name, null, null, null, null);
    }

    private static OrganizationGroupRepresentation existing(String id, String name) {
        OrganizationGroupRepresentation group = new OrganizationGroupRepresentation(name, null, null);
        group.setId(id);
        return group;
    }

    private static OrganizationGroupRepresentation full(String id, String name, String description,
                                                        Map<String, List<String>> attributes) {
        OrganizationGroupRepresentation group = new OrganizationGroupRepresentation(name, description,
                attributes == null ? null : new HashMap<>(attributes));
        group.setId(id);
        return group;
    }

    private static UserRepresentation user(String id) {
        UserRepresentation user = new UserRepresentation();
        user.setId(id);
        return user;
    }
}
