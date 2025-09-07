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
import org.apache.commons.lang3.ObjectUtils;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;


@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class RealmRepository {
    private final KeycloakProvider keycloakProvider;

    @Autowired
    public RealmRepository(KeycloakProvider keycloakProvider) {
        this.keycloakProvider = keycloakProvider;
    }

    public boolean exists(String realmName) {
        try {
            get(realmName);
        } catch (NotFoundException e) {
            return false;
        }

        return true;
    }

    public RealmResource getResource(String realmName) {
        return keycloakProvider.getInstance().realms().realm(realmName);
    }

    public RealmRepresentation get(String realmName) {
        final var realm = getResource(realmName).toRepresentation();
        realm.setAttributes(ObjectUtils.firstNonNull(realm.getAttributes(), new HashMap<>()));
        realm.setEventsEnabled(ObjectUtils.firstNonNull(realm.isEventsEnabled(), false));
        return realm;
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

    public void addDefaultDefaultClientScope(String realmName, String scopeId) {
        getResource(realmName).addDefaultDefaultClientScope(scopeId);
    }

    public void addDefaultOptionalClientScope(String realmName, String scopeId) {
        getResource(realmName).addDefaultOptionalClientScope(scopeId);
    }

    public void removeDefaultDefaultClientScope(String realmName, String scopeId) {
        getResource(realmName).removeDefaultDefaultClientScope(scopeId);
    }

    public void removeDefaultOptionalClientScope(String realmName, String scopeId) {
        getResource(realmName).removeDefaultOptionalClientScope(scopeId);
    }

    public List<RealmRepresentation> getRealms() {
        return keycloakProvider.getInstance().realms().findAll();
    }
}
