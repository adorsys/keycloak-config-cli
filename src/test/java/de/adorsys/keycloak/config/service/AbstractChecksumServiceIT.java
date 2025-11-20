/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2025 adorsys GmbH & Co. KG @ https://adorsys.com
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

import de.adorsys.keycloak.config.AbstractImportIT;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.service.checksum.ChecksumService;
import de.adorsys.keycloak.config.util.VersionUtil;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;

/**
 * Abstract base class for checksum-related integration tests.
 *
 * <p><b>Note on Test Execution:</b> This test class and its subclasses use a shared realm name ("simple")
 * and must be executed sequentially to avoid conflicts. The {@code @Execution(ExecutionMode.SAME_THREAD)}
 * annotation ensures that all tests in subclasses run on the same thread, preventing parallel execution
 * issues when multiple tests try to create/modify the same realm simultaneously.</p>
 */
@Execution(ExecutionMode.SAME_THREAD)
public class AbstractChecksumServiceIT extends AbstractImportIT {

    private static final Logger logger = LoggerFactory.getLogger(AbstractChecksumServiceIT.class);

    private static final String REALM_NAME = "simple";

    @Autowired
    ChecksumService checksumService;

    @BeforeEach
    void setupSimpleRealmWithPermissions() throws Exception {
        // Only execute setup logic for Keycloak 26.0+
        if (!VersionUtil.ge(KEYCLOAK_VERSION, "26.0")) {
            return;
        }

        try {
            // Create the simple realm programmatically
            RealmRepresentation realm = new RealmRepresentation();
            realm.setRealm(REALM_NAME);
            realm.setEnabled(true);

            try {
                keycloakProvider.getInstance().realms().create(realm);
                logger.debug("Created '{}' realm in @BeforeEach", REALM_NAME);
            } catch (WebApplicationException e) {
                if (e.getResponse().getStatus() == 409) {
                    // Realm already exists (from a previous test if cleanup failed)
                    logger.debug("Realm '{}' already exists, continuing with setup", REALM_NAME);
                } else {
                    throw e;
                }
            }

            // Wait briefly for Keycloak to create the cross-realm client asynchronously
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for cross-realm client creation");
                return;
            }

            // Grant permissions using the existing method
            try {
                grantAdminPermissionsForSimpleRealm();
                logger.debug("Successfully granted admin permissions for simple realm in @BeforeEach");
            } catch (Exception e) {
                logger.warn("Failed to grant admin permissions in @BeforeEach: {}", e.getMessage());
                logger.debug("Full stack trace:", e);
            }

            // Refresh the token to include the new permissions
            try {
                keycloakProvider.refreshToken();
                logger.debug("Refreshed admin token after granting permissions in @BeforeEach");
            } catch (Exception e) {
                logger.warn("Token refresh failed in @BeforeEach, attempting full re-authentication: {}", e.getMessage());
                try {
                    keycloakProvider.close();
                    keycloakProvider.getInstance();
                    logger.debug("Successfully re-authenticated in @BeforeEach after token refresh failure");
                } catch (Exception reAuthError) {
                    logger.error("Re-authentication also failed in @BeforeEach: {}", reAuthError.getMessage());
                    logger.debug("Full re-authentication stack trace:", reAuthError);
                }
            }
        } catch (Exception e) {
            logger.warn("Error in @BeforeEach setup: {}", e.getMessage());
            logger.debug("Full stack trace:", e);
            // Don't throw - let tests run and fail with clear errors if setup didn't work
        }
    }

        private void grantAdminPermissionsForSimpleRealm() {
            try {
                // Verify the simple realm exists
                keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();
                logger.debug("Verified '{}' realm exists", REALM_NAME);

                // Find the admin user in master realm
                List<UserRepresentation> users = keycloakProvider.getInstance().realm("master").users().search("admin");
                UserRepresentation adminUser = users.stream()
                        .filter(user -> "admin".equals(user.getUsername()))
                        .findFirst()
                        .orElse(null);

                if (adminUser == null) {
                    logger.warn("Admin user not found in master realm");
                    return;
                }
                logger.debug("Found admin user in master realm: {}", adminUser.getId());

        // Find the special cross-realm admin client in master realm (e.g. "simple-realm")
        String masterClientInternalId = null;
        RoleRepresentation manageRealmRole = null;
        int maxRetries = 6;
        long retryDelayMs = 500;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                List<ClientRepresentation> masterClients = keycloakProvider.getInstance()
                    .realm("master")
                    .clients()
                    .findByClientId(REALM_NAME + "-realm");

                if (masterClients.size() == 1) {
                    masterClientInternalId = masterClients.get(0).getId();
                    logger.debug("Found cross-realm client '{}' in master realm with internal ID: {} (attempt {}/{})",
                        REALM_NAME + "-realm", masterClientInternalId, attempt, maxRetries);

                    // Try to get manage-realm role
                    manageRealmRole = keycloakProvider.getInstance()
                        .realm("master")
                        .clients()
                        .get(masterClientInternalId)
                        .roles()
                        .get("manage-realm")
                        .toRepresentation();

                    logger.debug("Retrieved manage-realm role from client: {} (attempt {}/{})",
                        masterClientInternalId, attempt, maxRetries);
                    break; // Success - exit retry loop
                }

                if (attempt < maxRetries) {
                    logger.debug("Waiting {}ms before retry {}/{} - Found {} clients",
                        retryDelayMs, attempt + 1, maxRetries, masterClients.size());
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.warn("Interrupted while waiting for retry");
                        return;
                    }
                }
            } catch (Exception e) {
                if (attempt < maxRetries) {
                    logger.debug("Attempt {}/{} failed, retrying in {}ms: {}",
                        attempt, maxRetries, retryDelayMs, e.getMessage());
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.warn("Interrupted while waiting for retry after failure");
                        return;
                    }
                }
            }
        }

        if (masterClientInternalId == null || manageRealmRole == null) {
            logger.warn("Failed to find cross-realm client '{}' or manage-realm role after {} attempts",
                REALM_NAME + "-realm", maxRetries);
            return;
        }
        logger.debug("Retrieved manage-realm role from client: {}", masterClientInternalId);

        RoleRepresentation viewRealmRole = null;
        try {
            viewRealmRole = keycloakProvider.getInstance()
                .realm("master")
                .clients()
                .get(masterClientInternalId)
                .roles()
                .get("view-realm")
                .toRepresentation();
            logger.debug("Retrieved view-realm role from client: {}", masterClientInternalId);
        } catch (NotFoundException e) {
            // view-realm might not exist in some Keycloak setups; ignore if missing
            logger.debug("view-realm role not found for client {}, continuing without it", masterClientInternalId);
        }

        // Assign the client-level roles to admin user in master realm
        UserResource userResource = keycloakProvider.getInstance()
            .realm("master")
            .users()
            .get(adminUser.getId());

        // Get current client-level roles
        List<RoleRepresentation> existingRoles = userResource.roles()
            .clientLevel(masterClientInternalId)
            .listAll();

        Set<String> existingRoleNames = existingRoles.stream()
            .map(RoleRepresentation::getName)
            .collect(Collectors.toSet());

        // Determine which roles need to be added
        List<RoleRepresentation> rolesToAdd = new ArrayList<>();
        if (!existingRoleNames.contains("manage-realm")) {
            rolesToAdd.add(manageRealmRole);
        }
        if (viewRealmRole != null && !existingRoleNames.contains("view-realm")) {
            rolesToAdd.add(viewRealmRole);
        }

        // Add missing roles if any
        if (!rolesToAdd.isEmpty()) {
            userResource.roles()
                .clientLevel(masterClientInternalId)
                .add(rolesToAdd);
            logger.debug("Added {} missing role(s) to admin user {}", rolesToAdd.size(), adminUser.getId());
        } else {
            logger.debug("Admin user {} already has all required roles", adminUser.getId());
        }

        // Verify effective roles after assignment
        List<RoleRepresentation> effectiveRoles = userResource.roles()
            .clientLevel(masterClientInternalId)
            .listEffective();

        boolean hasManageRealm = effectiveRoles.stream()
            .anyMatch(role -> "manage-realm".equals(role.getName()));

        logger.debug("Admin user {} effective roles verification - manage-realm present: {}",
            adminUser.getId(), hasManageRealm);

        if (!hasManageRealm) {
            logger.warn("manage-realm role not found in effective roles for admin user {}", adminUser.getId());
        } else {
            logger.debug("Successfully verified manage-realm (and optionally view-realm) roles for admin user {} via master client: {}",
                adminUser.getId(), masterClientInternalId);
        }
            } catch (NotFoundException e) {
                logger.debug("Resource not found while granting permissions: {}", e.getMessage());
            } catch (Exception e) {
                logger.warn("Error granting admin permissions: {}", e.getMessage());
                logger.debug("Full stack trace:", e);
            }
        }

    @AfterEach
    void clearRealms() {
        keycloakProvider.getInstance().realms().findAll().forEach(realm -> {
            if (!Objects.equals(realm.getRealm(), "master")) {
                keycloakProvider.getInstance().realm(realm.getRealm()).remove();
            }
        });
    }

    void verifyChecksum(RealmRepresentation realm, String checksum) {
        Map<String, String> attributes = realm.getAttributes();
        var prefix = ImportConfigProperties.REALM_CHECKSUM_ATTRIBUTE_PREFIX_KEY.substring(0, ImportConfigProperties.REALM_CHECKSUM_ATTRIBUTE_PREFIX_KEY.length() - 3);
        assertThat(attributes, hasEntry(startsWith(prefix), is(checksum)));
    }

    void verifyHasToBeUpdated(RealmImport realmImport, boolean expected) {
        var hasToBe = checksumService.hasToBeUpdated(realmImport);
        assertThat(hasToBe, is(expected));
    }

    void verifyHasToBeUpdated(String fileName, boolean expected) throws IOException {
        var realmImport = getFirstImport(fileName);
        var hasToBe = checksumService.hasToBeUpdated(realmImport);
        assertThat(hasToBe, is(expected));
    }

    void importAndVerifyChecksum(String filename, String checksum) throws Exception {
        doImport(filename);

        var createdRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();
        verifyChecksum(createdRealm, checksum);
    }

    List<RealmImport> importFromDirectory(String location) {
        var keycloakImport = keycloakImportProvider.readFromLocations(location);
        var realmImports = keycloakImport.getRealmImports().values().stream()
                .flatMap(e -> e.values().stream())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        realmImports.forEach(realmImport -> realmImportService.doImport(realmImport));

        return realmImports;
    }
}
