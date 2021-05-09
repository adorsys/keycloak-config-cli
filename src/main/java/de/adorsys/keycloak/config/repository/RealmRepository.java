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

package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.util.ResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.WebApplicationException;

@Service
public class RealmRepository {
    private final KeycloakProvider keycloakProvider;

    @Autowired
    public RealmRepository(KeycloakProvider keycloakProvider) {
        this.keycloakProvider = keycloakProvider;
    }

    public boolean exists(String realmName) {
        try {
            get(realmName);
        } catch (javax.ws.rs.NotFoundException e) {
            return false;
        }

        return true;
    }

    public RealmResource getResource(String realmName) {
        return keycloakProvider.getInstance().realms().realm(realmName);
    }

    public RealmRepresentation get(String realmName) {
        return getResource(realmName).toRepresentation();
    }

    public void create(RealmRepresentation realm) {
        Keycloak keycloak = keycloakProvider.getInstance();
        RealmsResource realmsResource = keycloak.realms();

        try {
            realmsResource.create(realm);
        } catch (WebApplicationException error) {
            String errorMessage = ResponseUtil.getErrorMessage(error);
            throw new KeycloakRepositoryException(
                    String.format("Cannot create realm '%s': %s", realm.getRealm(), errorMessage),
                    error
            );
        }
    }

    public void update(RealmRepresentation realm) {
        try {
            getResource(realm.getRealm()).update(realm);
        } catch (WebApplicationException error) {
            String errorMessage = ResponseUtil.getErrorMessage(error);
            throw new KeycloakRepositoryException(
                    String.format("Cannot update realm '%s': %s", realm.getRealm(), errorMessage),
                    error
            );
        }
    }

    public RealmRepresentation partialExport(String realmName, boolean exportGroupsAndRoles, boolean exportClients) {
        return getResource(realmName).partialExport(exportGroupsAndRoles, exportClients);
    }
}
