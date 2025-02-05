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

import de.adorsys.keycloak.config.model.RealmImport;
import org.keycloak.admin.client.resource.ClientPoliciesPoliciesResource;
import org.keycloak.admin.client.resource.ClientPoliciesProfilesResource;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class ClientPoliciesRepository {

    private static final Logger logger = LoggerFactory.getLogger(ClientPoliciesRepository.class);

    private final RealmRepository realmRepository;

    @Autowired
    public ClientPoliciesRepository(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    private ClientPoliciesPoliciesResource getPoliciesResource(String realmName) {
        return this.realmRepository.getResource(realmName).clientPoliciesPoliciesResource();
    }

    private ClientPoliciesProfilesResource getProfilesResource(String realmName) {
        return this.realmRepository.getResource(realmName).clientPoliciesProfilesResource();
    }

    public void updateClientPoliciesPolicies(RealmImport realmImport, ClientPoliciesRepresentation newClientPolicies) {

        ClientPoliciesPoliciesResource policiesResource = getPoliciesResource(realmImport.getRealm());

        ClientPoliciesRepresentation existingClientPolicies;
        try {
            existingClientPolicies = policiesResource.getPolicies();
        } catch (Exception ex) {
            existingClientPolicies = null;
        }

        if (existingClientPolicies == null && newClientPolicies == null) {
            logger.trace("No client-policy policies configured, skipping update.");
            return;
        }

        if (existingClientPolicies != null && existingClientPolicies.equals(newClientPolicies)) {
            logger.trace("Current client-policy policies match existing policies, skipping update.");
            return;
        }

        if (newClientPolicies == null) {
            logger.trace("New client-policy policies resets existing policies.");
            newClientPolicies = new ClientPoliciesRepresentation();
        }

        policiesResource.updatePolicies(newClientPolicies);
    }

    public void updateClientPoliciesProfiles(RealmImport realmImport, ClientProfilesRepresentation newClientProfiles) {

        ClientPoliciesProfilesResource profilesResource = getProfilesResource(realmImport.getRealm());

        // Note that we deliberately ignore global profiles, to avoid inconsistencies.
        ClientProfilesRepresentation existingClientProfiles;
        try {
            existingClientProfiles = profilesResource.getProfiles(false);
        } catch (Exception ex) {
            existingClientProfiles = null;
        }

        if (existingClientProfiles == null && newClientProfiles == null) {
            logger.trace("No client-policy profiles configured, skipping update.");
            return;
        }

        if (existingClientProfiles != null && existingClientProfiles.equals(newClientProfiles)) {
            logger.trace("Current client-policy profiles match existing profiles, skipping update.");
            return;
        }

        if (newClientProfiles == null) {
            logger.trace("New client-policy profiles resets existing profiles.");
            newClientProfiles = new ClientProfilesRepresentation();
        }

        profilesResource.updateProfiles(newClientProfiles);
    }

}
