/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2022 adorsys GmbH & Co. KG @ https://adorsys.com
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

package de.adorsys.keycloak.config.service.clientauthorization;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.repository.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.NotFoundException;

public class ClientPermissionResolver implements PermissionResolver {
    private static final Logger logger = LoggerFactory.getLogger(ClientPermissionResolver.class);

    private final String realmName;
    private final ClientRepository clientRepository;

    public ClientPermissionResolver(String realmName, ClientRepository clientRepository) {
        this.realmName = realmName;
        this.clientRepository = clientRepository;
    }

    @Override
    public String resolveObjectId(String clientId, String authzName) {
        try {
            return clientRepository.getByClientId(realmName, clientId).getId();
        } catch (NotFoundException | KeycloakRepositoryException e) {
            throw new ImportProcessingException("Cannot find client '%s' in realm '%s' for '%s'", clientId, realmName, authzName);
        }
    }

    @Override
    public void enablePermissions(String id) {
        try {
            if (!clientRepository.isPermissionEnabled(realmName, id)) {
                logger.debug("Enable permissions for client '{}' in realm '{}'", id, realmName);
                clientRepository.enablePermission(realmName, id);
            }
        } catch (NotFoundException e) {
            throw new ImportProcessingException("Cannot find client with id '%s' in realm '%s'", id, realmName);
        }
    }
}
