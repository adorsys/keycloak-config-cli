/*
 * Copyright 2019-2020 adorsys GmbH & Co. KG @ https://adorsys.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.ClientRepository;
import org.jboss.logging.Logger;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.representations.idm.ClientRepresentation;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.List;

@Dependent
public class CustomImportService {
    private static final Logger LOG = Logger.getLogger(CustomImportService.class);

    @Inject
    KeycloakProvider keycloakProvider;

    @Inject
    ClientRepository clientRepository;

    public void doImport(RealmImport realmImport) {
        realmImport.getCustomImport().ifPresent(customImport -> setupImpersonation(realmImport, customImport));
    }

    private void setupImpersonation(RealmImport realmImport, RealmImport.CustomImport customImport) {
        if (Boolean.TRUE.equals(customImport.removeImpersonation())) {
            removeImpersonation(realmImport);
        }
    }

    private void removeImpersonation(RealmImport realmImport) {
        RealmResource master = keycloakProvider.get().realm("master");

        String clientId = realmImport.getRealm() + "-realm";
        List<ClientRepresentation> foundClients = master.clients()
                .findByClientId(clientId);

        if (!foundClients.isEmpty()) {
            removeImpersonationRoleFromClient(master, clientId);
        }
    }

    private void removeImpersonationRoleFromClient(RealmResource master, String clientId) {
        ClientRepresentation client = clientRepository.getClient("master", clientId);
        ClientResource clientResource = master.clients()
                .get(client.getId());

        RoleResource impersonationRole = clientResource.roles().get("impersonation");

        try {
            LOG.debugf("Remove role 'impersonation' from client '%s' in realm 'master'", clientId);

            impersonationRole.remove();
        } catch (javax.ws.rs.NotFoundException e) {
            LOG.infof("Cannot remove 'impersonation' role from client '%s' in 'master' realm: Not found", clientId);
        }
    }
}
