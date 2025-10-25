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
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;

public class AbstractChecksumServiceIT extends AbstractImportIT {

    private static final Logger logger = LoggerFactory.getLogger(AbstractChecksumServiceIT.class);

    private static final String REALM_NAME = "simple";

    @Autowired
    ChecksumService checksumService;

    @Override
    public void doImport(String fileName) throws IOException {
            super.doImport(fileName);
        
            // For Keycloak 26.0+, grant additional permissions after realm creation
            if (VersionUtil.ge(KEYCLOAK_VERSION, "26.0")) {
                try {
                    grantAdminPermissionsForSimpleRealm();
                } catch (Exception e) {
                    logger.warn("Failed to grant admin permissions for simple realm: {}", e.getMessage());
                    logger.debug("Full stack trace:", e);
                }
            }
        }

        private void grantAdminPermissionsForSimpleRealm() {
            try {
                // Verify the simple realm exists
                keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

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

        // Find the special cross-realm admin client in master realm (e.g. "simple-realm")
        List<ClientRepresentation> masterClients = keycloakProvider.getInstance()
            .realm("master")
            .clients()
            .findByClientId(REALM_NAME + "-realm");

        if (masterClients.size() != 1) {
            logger.warn("Expected exactly one cross-realm client '{}' in master realm, found: {}", REALM_NAME + "-realm", masterClients.size());
            return;
        }

        String masterClientInternalId = masterClients.get(0).getId();

        // Get the manage-realm and view-realm roles from the master client
        RoleRepresentation manageRealmRole = keycloakProvider.getInstance()
            .realm("master")
            .clients()
            .get(masterClientInternalId)
            .roles()
            .get("manage-realm")
            .toRepresentation();

        RoleRepresentation viewRealmRole = null;
        try {
            viewRealmRole = keycloakProvider.getInstance()
                .realm("master")
                .clients()
                .get(masterClientInternalId)
                .roles()
                .get("view-realm")
                .toRepresentation();
        } catch (NotFoundException e) {
            // view-realm might not exist in some Keycloak setups; ignore if missing
            logger.debug("view-realm role not found for client {}, continuing without it", masterClientInternalId);
        }

        // Assign the client-level roles to admin user in master realm
        UserResource userResource = keycloakProvider.getInstance()
            .realm("master")
            .users()
            .get(adminUser.getId());

        if (viewRealmRole != null) {
            userResource.roles()
                .clientLevel(masterClientInternalId)
                .add(Arrays.asList(manageRealmRole, viewRealmRole));
        } else {
            userResource.roles()
                .clientLevel(masterClientInternalId)
                .add(Collections.singletonList(manageRealmRole));
        }

        logger.debug("Successfully granted manage-realm (and optionally view-realm) roles to admin user via master client: {}", masterClientInternalId);
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
