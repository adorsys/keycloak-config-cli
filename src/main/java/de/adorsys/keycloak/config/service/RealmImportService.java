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
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.repository.RealmRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.jboss.logging.Logger;
import org.keycloak.representations.idm.RealmRepresentation;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Map;

@Dependent
public class RealmImportService {
    private static final Logger LOG = Logger.getLogger(RealmImportService.class);

    private static final String REALM_CHECKSUM_ATTRIBUTE_PREFIX_KEY = "de.adorsys.keycloak.config.import-checksum-";

    private final String[] ignoredPropertiesForCreation = new String[]{
            "users",
            "groups",
            "browserFlow",
            "directGrantFlow",
            "clientAuthenticationFlow",
            "dockerAuthenticationFlow",
            "registrationFlow",
            "resetCredentialsFlow",
            "components",
            "authenticationFlows"
    };

    private final String[] ignoredPropertiesForUpdate = new String[]{
            "clients",
            "roles",
            "users",
            "groups",
            "identityProviders",
            "browserFlow",
            "directGrantFlow",
            "clientAuthenticationFlow",
            "dockerAuthenticationFlow",
            "registrationFlow",
            "resetCredentialsFlow",
            "components",
            "authenticationFlows",
            "requiredActions"
    };

    private final String[] patchingPropertiesForFlowImport = new String[]{
            "browserFlow",
            "directGrantFlow",
            "clientAuthenticationFlow",
            "dockerAuthenticationFlow",
            "registrationFlow",
            "resetCredentialsFlow",
    };

    @Inject
    KeycloakProvider keycloakProvider;

    @Inject
    RealmRepository realmRepository;

    @Inject
    UserImportService userImportService;

    @Inject
    RoleImportService roleImportService;

    @Inject
    ClientImportService clientImportService;

    @Inject
    GroupImportService groupImportService;

    @Inject
    ComponentImportService componentImportService;

    @Inject
    AuthenticationFlowsImportService authenticationFlowsImportService;

    @Inject
    AuthenticatorConfigImportService authenticatorConfigImportService;

    @Inject
    RequiredActionsImportService requiredActionsImportService;

    @Inject
    CustomImportService customImportService;

    @Inject
    ScopeMappingImportService scopeMappingImportService;

    @Inject
    IdentityProviderImportService identityProviderImportService;

    @Inject
    ImportConfigProperties importProperties;

    public void doImport(RealmImport realmImport) {
        boolean realmExists = realmRepository.exists(realmImport.getRealm());

        if (realmExists) {
            updateRealmIfNecessary(realmImport);
        } else {
            createRealm(realmImport);
        }

        keycloakProvider.close();
    }

    private void createRealm(RealmImport realmImport) {
        LOG.debugf("Creating realm '%s' ...", realmImport.getRealm());

        RealmRepresentation realmForCreation = CloneUtils.deepClone(realmImport, RealmRepresentation.class, ignoredPropertiesForCreation);
        realmRepository.create(realmForCreation);

        userImportService.doImport(realmImport);
        groupImportService.importGroups(realmImport);
        authenticationFlowsImportService.doImport(realmImport);
        setupFlows(realmImport);
        componentImportService.doImport(realmImport);
        customImportService.doImport(realmImport);
        setupImportChecksum(realmImport);
    }

    private void updateRealmIfNecessary(RealmImport realmImport) {
        if (Boolean.TRUE.equals(importProperties.getForce()) || hasToBeUpdated(realmImport)) {
            updateRealm(realmImport);
        } else {
            LOG.debugf(
                    "No need to update realm '%s', import checksum same: '%s'",
                    realmImport.getRealm(),
                    realmImport.getChecksum()
            );
        }
    }

    private void updateRealm(RealmImport realmImport) {
        LOG.debugf("Updating realm '%s'...", realmImport.getRealm());

        RealmRepresentation realmToUpdate = CloneUtils.deepClone(realmImport, RealmRepresentation.class, ignoredPropertiesForUpdate);
        realmRepository.update(realmToUpdate);

        clientImportService.doImport(realmImport);
        roleImportService.doImport(realmImport);
        groupImportService.importGroups(realmImport);
        userImportService.doImport(realmImport);
        importRequiredActions(realmImport);
        authenticationFlowsImportService.doImport(realmImport);
        authenticatorConfigImportService.doImport(realmImport);
        setupFlows(realmImport);
        componentImportService.doImport(realmImport);
        scopeMappingImportService.doImport(realmImport);
        identityProviderImportService.doImport(realmImport);
        customImportService.doImport(realmImport);
        setupImportChecksum(realmImport);
    }

    private void importRequiredActions(RealmImport realmImport) {
        requiredActionsImportService.doImport(realmImport);
    }

    private void setupFlows(RealmImport realmImport) {
        RealmRepresentation existingRealm = realmRepository.get(realmImport.getRealm());
        RealmRepresentation realmToUpdate = CloneUtils.deepPatchFieldsOnly(existingRealm, realmImport, patchingPropertiesForFlowImport);

        realmRepository.update(realmToUpdate);
    }

    private boolean hasToBeUpdated(RealmImport realmImport) {
        RealmRepresentation existingRealm = realmRepository.get(realmImport.getRealm());
        Map<String, String> customAttributes = existingRealm.getAttributes();
        String readChecksum = customAttributes.get(REALM_CHECKSUM_ATTRIBUTE_PREFIX_KEY + importProperties.getKey());

        return !realmImport.getChecksum().equals(readChecksum);
    }

    private void setupImportChecksum(RealmImport realmImport) {
        RealmRepresentation existingRealm = realmRepository.get(realmImport.getRealm());
        Map<String, String> customAttributes = existingRealm.getAttributes();

        String importChecksum = realmImport.getChecksum();
        customAttributes.put(REALM_CHECKSUM_ATTRIBUTE_PREFIX_KEY + importProperties.getKey(), importChecksum);
        realmRepository.update(existingRealm);

        LOG.debugf("Updated import checksum of realm '%s' to '%s'", realmImport.getRealm(), importChecksum);
    }
}
