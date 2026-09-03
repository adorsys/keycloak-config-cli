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

// STUB - no implementation, defines contract and should be deleted
// ProtectedRoleResolver and ImportConfigProperties.ImportProtectedRolesProperties /
// ProtectedRolesMode are production contract types expected to be created by the
// backend-agent. This test file defines their required shape and behaviour.

import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportProtectedRolesProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportProtectedRolesProperties.ProtectedRolesMode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProtectedRoleResolverTest {

    private static ProtectedRoleResolver resolverFor(ImportProtectedRolesProperties protectedRolesProperties) {
        ImportConfigProperties importConfigProperties = mock(ImportConfigProperties.class);
        when(importConfigProperties.getProtectedRoles()).thenReturn(protectedRolesProperties);
        return new ProtectedRoleResolver(importConfigProperties);
    }

    @Nested
    class ZeroConfiguration {
        // Requirement C.9: with zero configuration, defaults are realm roles
        // "admin" and "create-realm", plus all roles of client "realm-management".
        private final ProtectedRoleResolver resolver = resolverFor(
                new ImportProtectedRolesProperties(ProtectedRolesMode.ADD, List.of(), Map.of())
        );

        @Test
        void isRealmRoleProtected_shouldProtectBuiltInDefaultRealmRoles() {
            assertTrue(resolver.isRealmRoleProtected("admin"));
            assertTrue(resolver.isRealmRoleProtected("create-realm"));
        }

        @Test
        void isRealmRoleProtected_shouldNotProtectArbitraryRole() {
            assertFalse(resolver.isRealmRoleProtected("my_custom_role"));
        }

        @Test
        void isClientRoleProtected_shouldProtectAnyRoleOfRealmManagementClient() {
            assertTrue(resolver.isClientRoleProtected("realm-management", "manage-users"));
            assertTrue(resolver.isClientRoleProtected("realm-management", "any-future-role"));
        }

        @Test
        void isClientRoleProtected_shouldNotProtectOtherClients() {
            assertFalse(resolver.isClientRoleProtected("moped-client", "manage-users"));
        }

        @Test
        void isRealmRoleProtected_shouldNotMatchClientRoleNameAsRealmRole() {
            // Requirement B.6: namespace separation - "manage-users" is a protected CLIENT role
            // of realm-management, but must never match a REALM role of the same name.
            assertFalse(resolver.isRealmRoleProtected("manage-users"));
        }
    }

    @Nested
    class AdditiveMode {
        // Requirement C.10: effective protected set is the UNION of built-in defaults
        // and configured roles.
        private final ProtectedRoleResolver resolver = resolverFor(
                new ImportProtectedRolesProperties(
                        ProtectedRolesMode.ADD,
                        List.of("my_custom_protected_role"),
                        Map.of("my-client", List.of("my_protected_client_role"))
                )
        );

        @Test
        void isRealmRoleProtected_shouldStillProtectBuiltInDefaults() {
            assertTrue(resolver.isRealmRoleProtected("admin"));
            assertTrue(resolver.isRealmRoleProtected("create-realm"));
        }

        @Test
        void isRealmRoleProtected_shouldAlsoProtectConfiguredRole() {
            assertTrue(resolver.isRealmRoleProtected("my_custom_protected_role"));
        }

        @Test
        void isClientRoleProtected_shouldStillProtectRealmManagementDefaults() {
            assertTrue(resolver.isClientRoleProtected("realm-management", "manage-users"));
        }

        @Test
        void isClientRoleProtected_shouldAlsoProtectConfiguredClientRole() {
            assertTrue(resolver.isClientRoleProtected("my-client", "my_protected_client_role"));
        }

        @Test
        void isClientRoleProtected_shouldNotProtectUnconfiguredRoleOfConfiguredClient() {
            assertFalse(resolver.isClientRoleProtected("my-client", "some_other_role"));
        }
    }

    @Nested
    class ReplaceModeWithExplicitList {
        // Requirement C.11: EXACTLY the configured roles are protected, built-in defaults
        // do not apply.
        private final ProtectedRoleResolver resolver = resolverFor(
                new ImportProtectedRolesProperties(
                        ProtectedRolesMode.REPLACE,
                        List.of("my_only_protected_role"),
                        Map.of("my-client", List.of("my_only_protected_client_role"))
                )
        );

        @Test
        void isRealmRoleProtected_shouldNotProtectBuiltInDefaults() {
            assertFalse(resolver.isRealmRoleProtected("admin"));
            assertFalse(resolver.isRealmRoleProtected("create-realm"));
        }

        @Test
        void isRealmRoleProtected_shouldProtectOnlyConfiguredRole() {
            assertTrue(resolver.isRealmRoleProtected("my_only_protected_role"));
        }

        @Test
        void isClientRoleProtected_shouldNotProtectRealmManagementDefaults() {
            assertFalse(resolver.isClientRoleProtected("realm-management", "manage-users"));
        }

        @Test
        void isClientRoleProtected_shouldProtectOnlyConfiguredClientRole() {
            assertTrue(resolver.isClientRoleProtected("my-client", "my_only_protected_client_role"));
        }
    }

    @Nested
    class ReplaceModeWithEmptyLists {
        // Requirement C.12: full opt-out - NO role is protected at all.
        private final ProtectedRoleResolver resolver = resolverFor(
                new ImportProtectedRolesProperties(ProtectedRolesMode.REPLACE, List.of(), Map.of())
        );

        @Test
        void isRealmRoleProtected_shouldProtectNothing() {
            assertFalse(resolver.isRealmRoleProtected("admin"));
            assertFalse(resolver.isRealmRoleProtected("create-realm"));
        }

        @Test
        void isClientRoleProtected_shouldProtectNothing() {
            assertFalse(resolver.isClientRoleProtected("realm-management", "manage-users"));
        }
    }

    @Nested
    class Namespaces {
        private final ProtectedRoleResolver resolver = resolverFor(
                new ImportProtectedRolesProperties(ProtectedRolesMode.ADD, List.of(), Map.of())
        );

        @Test
        void isClientRoleProtected_shouldNotMatchRealmRoleNameAsClientRole() {
            // Requirement B.6: "admin" is a protected REALM role, must never match a client role
            // of the same name belonging to some unrelated client.
            assertFalse(resolver.isClientRoleProtected("moped-client", "admin"));
        }

        @Test
        void isClientRoleProtected_shouldMatchRealmManagementRoleRegardlessOfName() {
            // realm-management is wildcard-protected by default, any role name matches
            assertTrue(resolver.isClientRoleProtected("realm-management", "unrelated-role-with-wrong-client"));
        }
    }

    @Nested
    class WildcardClientProtection {
        // Requirement B.7: a wildcard entry for a client protects EVERY role of that client,
        // including roles added later (i.e. any role name matches).
        private final ProtectedRoleResolver resolver = resolverFor(
                new ImportProtectedRolesProperties(
                        ProtectedRolesMode.REPLACE,
                        List.of(),
                        Map.of("wildcard-client", List.of("*"))
                )
        );

        @Test
        void isClientRoleProtected_shouldProtectAnyRoleName() {
            assertTrue(resolver.isClientRoleProtected("wildcard-client", "some-role-not-known-in-advance"));
            assertTrue(resolver.isClientRoleProtected("wildcard-client", "another-future-role"));
        }

        @Test
        void isClientRoleProtected_shouldNotAffectOtherClients() {
            assertFalse(resolver.isClientRoleProtected("other-client", "some-role"));
        }
    }

    @Nested
    class AbsentClientConfiguration {
        // Requirement B.8: protection configured for a client absent from the target realm
        // causes no error at the resolver level - it is a pure lookup, resolver has no
        // knowledge of which clients actually exist in the realm.
        private final ProtectedRoleResolver resolver = resolverFor(
                new ImportProtectedRolesProperties(
                        ProtectedRolesMode.REPLACE,
                        List.of(),
                        Map.of("non-existent-client", List.of("*"))
                )
        );

        @Test
        void isClientRoleProtected_shouldNotThrowForAbsentClient() {
            assertTrue(resolver.isClientRoleProtected("non-existent-client", "whatever"));
            assertFalse(resolver.isClientRoleProtected("other-client", "whatever"));
        }
    }

    @Nested
    class NullSafety {
        private final ProtectedRoleResolver resolver = resolverFor(
                new ImportProtectedRolesProperties(ProtectedRolesMode.ADD, List.of(), Map.of())
        );

        @Test
        void isRealmRoleProtected_shouldReturnFalseForNull() {
            assertFalse(resolver.isRealmRoleProtected(null));
        }

        @Test
        void isClientRoleProtected_shouldReturnFalseForNullClientId() {
            assertFalse(resolver.isClientRoleProtected(null, "manage-users"));
        }

        @Test
        void isClientRoleProtected_shouldReturnFalseForNullRoleName() {
            assertFalse(resolver.isClientRoleProtected("realm-management", null));
        }
    }
}
