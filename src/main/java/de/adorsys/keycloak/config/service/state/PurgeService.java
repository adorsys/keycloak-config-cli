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

package de.adorsys.keycloak.config.service.state;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PurgeService {
    private static final Logger logger = LoggerFactory.getLogger(PurgeService.class);
    private final KeycloakProvider keycloakProvider;
    private final ImportConfigProperties importConfigProperties;

    @Autowired
    public PurgeService(KeycloakProvider keycloakProvider, ImportConfigProperties importConfigProperties) {
        this.keycloakProvider = keycloakProvider;
        this.importConfigProperties = importConfigProperties;
    }

    public void purgeResourcesNotInImport(RealmImport realmImport) {
        if (importConfigProperties.getRemoteState().isEnabled()) {
            return;
        }

        String realmName = realmImport.getRealm();
        RealmResource realmResource = keycloakProvider.getInstance().realm(realmName);

        purgeClients(realmResource, realmImport);
        purgeRoles(realmResource, realmImport);
        purgeUsers(realmResource, realmImport);
        purgeGroups(realmResource, realmImport);
        purgeClientScopes(realmResource, realmImport);
        //purgeComponents(realmResource, realmImport);
        //purgeIdentityProviders(realmResource, realmImport);

        // Add other purge methods as needed
    }

    private void purgeClients(RealmResource realmResource, RealmImport realmImport) {
        List<ClientRepresentation> existingClients = realmResource.clients().findAll();
        List<String> importedClients = realmImport.getClients().stream()
                .map(client -> client.getClientId() != null ? client.getClientId() : "name:" + client.getName())
                .toList();

        for (ClientRepresentation client : existingClients) {
            String clientId = client.getClientId() != null ? client.getClientId() : "name:" + client.getName();
            if (!importedClients.contains(clientId)) {
                realmResource.clients().get(client.getId()).remove();
                logger.info("Purged client: {}", clientId);
            }
        }
    }

    private void purgeRoles(RealmResource realmResource, RealmImport realmImport) {
        List<RoleRepresentation> existingRoles = realmResource.roles().list();
        List<String> importedRoleNames = realmImport.getRoles().getRealm().stream()
                .map(RoleRepresentation::getName)
                .toList();

        for (RoleRepresentation role : existingRoles) {
            if (!importedRoleNames.contains(role.getName())) {
                realmResource.roles().deleteRole(role.getName());
                logger.info("Purged role: {}", role.getName());
            }
        }
    }

    private void purgeUsers(RealmResource realmResource, RealmImport realmImport) {
        List<UserRepresentation> existingUsers = realmResource.users().list();
        List<String> importedUserIds = realmImport.getUsers().stream()
                .map(UserRepresentation::getId)
                .toList();

        for (UserRepresentation user : existingUsers) {
            if (!importedUserIds.contains(user.getId())) {
                try {
                    realmResource.users().delete(user.getId());
                    logger.info("Purged user: {}", user.getUsername());
                } catch (Exception e) {
                    logger.error("Failed to purge user: {}. Error: {}", user.getUsername(), e.getMessage());
                }
            }
        }
    }


    private void purgeGroups(RealmResource realmResource, RealmImport realmImport) {
        List<GroupRepresentation> existingGroups = realmResource.groups().groups();
        List<String> importedGroupNames = realmImport.getGroups().stream()
                .map(GroupRepresentation::getName)
                .toList();

        for (GroupRepresentation group : existingGroups) {
            if (!importedGroupNames.contains(group.getName())) {
                realmResource.groups().group(group.getId()).remove();
                logger.info("Purged group: {}", group.getName());
            }
        }
    }

    private void purgeClientScopes(RealmResource realmResource, RealmImport realmImport) {
        List<ClientScopeRepresentation> existingScopes = realmResource.clientScopes().findAll();
        List<String> importedScopeNames = realmImport.getClientScopes().stream()
                .map(ClientScopeRepresentation::getName)
                .toList();

        for (ClientScopeRepresentation scope : existingScopes) {
            if (!importedScopeNames.contains(scope.getName())) {
                realmResource.clientScopes().get(scope.getId()).remove();
                logger.info("Purged client scope: {}", scope.getName());
            }
        }
    }
}
