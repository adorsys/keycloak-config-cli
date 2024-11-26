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

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.repository.RealmRepository;
import de.adorsys.keycloak.config.service.checksum.ChecksumService;
import de.adorsys.keycloak.config.service.state.StateService;
import de.adorsys.keycloak.config.util.CloneUtil;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RealmImportService {
    static final String[] ignoredPropertiesForRealmImport = new String[]{
            "authenticatorConfig",
            "clients",
            "roles",
            "users",
            "groups",
            "defaultGroups",
            "identityProviders",
            "browserFlow",
            "directGrantFlow",
            "clientAuthenticationFlow",
            "dockerAuthenticationFlow",
            "registrationFlow",
            "resetCredentialsFlow",
            "components",
            "authenticationFlows",
            "scopeMappings",
            "clientScopeMappings",
            "clientScopes",
            "requiredActions",
            "defaultDefaultClientScopes",
            "defaultOptionalClientScopes",
            "clientProfiles",
            "clientPolicies",
            "firstBrokerLoginFlow",
    };

    private static final Logger logger = LoggerFactory.getLogger(RealmImportService.class);
    private final KeycloakProvider keycloakProvider;
    private final RealmRepository realmRepository;
    private final OtpPolicyImportService otpPolicyImportService;

    private final UserImportService userImportService;
    private final UserProfileImportService userProfileImportService;

    private final ClientPoliciesImportService clientPoliciesImportService;

    private final RoleImportService roleImportService;
    private final ClientImportService clientImportService;
    private final ClientScopeImportService clientScopeImportService;
    private final GroupImportService groupImportService;
    private final DefaultGroupsImportService defaultGroupsImportService;
    private final ComponentImportService componentImportService;
    private final AuthenticationFlowsImportService authenticationFlowsImportService;
    private final AuthenticatorConfigImportService authenticatorConfigImportService;
    private final RequiredActionsImportService requiredActionsImportService;
    private final ScopeMappingImportService scopeMappingImportService;
    private final ClientAuthorizationImportService clientAuthorizationImportService;
    private final ClientScopeMappingImportService clientScopeMappingImportService;
    private final IdentityProviderImportService identityProviderImportService;
    private final MessageBundleImportService messageBundleImportService;

    private final ImportConfigProperties importProperties;

    private final ChecksumService checksumService;
    private final StateService stateService;

    @Autowired
    public RealmImportService(
            ImportConfigProperties importProperties,
            KeycloakProvider keycloakProvider,
            RealmRepository realmRepository,
            UserImportService userImportService,
            UserProfileImportService userProfileImportService,
            ClientPoliciesImportService clientPoliciesImportService,
            RoleImportService roleImportService,
            ClientImportService clientImportService,
            GroupImportService groupImportService,
            ClientScopeImportService clientScopeImportService,
            DefaultGroupsImportService defaultGroupsImportService,
            ComponentImportService componentImportService,
            AuthenticationFlowsImportService authenticationFlowsImportService,
            AuthenticatorConfigImportService authenticatorConfigImportService,
            RequiredActionsImportService requiredActionsImportService,
            ScopeMappingImportService scopeMappingImportService,
            ClientAuthorizationImportService clientAuthorizationImportService,
            ClientScopeMappingImportService clientScopeMappingImportService,
            IdentityProviderImportService identityProviderImportService,
            MessageBundleImportService messageBundleImportService,
            OtpPolicyImportService otpPolicyImportService,
            ChecksumService checksumService,
            StateService stateService) {
        this.importProperties = importProperties;
        this.keycloakProvider = keycloakProvider;
        this.realmRepository = realmRepository;
        this.userImportService = userImportService;
        this.userProfileImportService = userProfileImportService;
        this.clientPoliciesImportService = clientPoliciesImportService;
        this.roleImportService = roleImportService;
        this.clientImportService = clientImportService;
        this.groupImportService = groupImportService;
        this.clientScopeImportService = clientScopeImportService;
        this.defaultGroupsImportService = defaultGroupsImportService;
        this.componentImportService = componentImportService;
        this.authenticationFlowsImportService = authenticationFlowsImportService;
        this.authenticatorConfigImportService = authenticatorConfigImportService;
        this.requiredActionsImportService = requiredActionsImportService;
        this.scopeMappingImportService = scopeMappingImportService;
        this.clientAuthorizationImportService = clientAuthorizationImportService;
        this.clientScopeMappingImportService = clientScopeMappingImportService;
        this.identityProviderImportService = identityProviderImportService;
        this.messageBundleImportService = messageBundleImportService;
        this.otpPolicyImportService = otpPolicyImportService;
        this.checksumService = checksumService;
        this.stateService = stateService;
    }

    public void doImport(RealmImport realmImport) {
        boolean realmExists = realmRepository.exists(realmImport.getRealm());

        if (realmExists) {
            updateRealmIfNecessary(realmImport);
        } else {
            createRealm(realmImport);
        }
    }

    private void updateRealmIfNecessary(RealmImport realmImport) {
        if (!importProperties.getCache().isEnabled() || checksumService.hasToBeUpdated(realmImport)) {
            setEventsEnabledWorkaround(realmImport);
            updateRealm(realmImport);
        } else {
            logger.debug(
                    "No need to update realm '{}', import checksum same: '{}'",
                    realmImport.getRealm(),
                    realmImport.getChecksum()
            );
        }
    }

    private void setEventsEnabledWorkaround(RealmImport realmImport) {
        // https://github.com/adorsys/keycloak-config-cli/issues/338
        if (realmImport.isEventsEnabled() != null) return;

        Boolean existingEventsEnabled = realmRepository.get(realmImport.getRealm()).isEventsEnabled();
        realmImport.setEventsEnabled(existingEventsEnabled);
    }

    private void createRealm(RealmImport realmImport) {
        logger.info("Starting creation of realm '{}'", realmImport.getRealm());

        RealmRepresentation realm = CloneUtil.deepClone(realmImport, RealmRepresentation.class, ignoredPropertiesForRealmImport);
        logger.debug("RealmRepresentation created: {}", realm);
        logger.info("Creating realm in repository");
        realmRepository.create(realm);
        logger.info("Realm '{}' created successfully", realm.getRealm());

        logger.debug("Refreshing access token to update scopes");
        keycloakProvider.refreshToken();
        logger.debug("Access token refreshed");

        logger.debug("Loading state for realm '{}'", realmImport.getRealm());
        stateService.loadState(realmImport);
        logger.debug("State loaded for realm '{}'", realmImport.getRealm());
        logger.info("Configuring realm '{}'", realmImport.getRealm());
        configureRealm(realmImport, realm);
        logger.info("Configuration completed for realm '{}'", realmImport.getRealm());
        logger.info("Creation of realm '{}' completed", realmImport.getRealm());
    }

    private void updateRealm(RealmImport realmImport) {
        logger.info("Starting update of realm '{}'", realmImport.getRealm());

        logger.debug("Cloning realm import to RealmRepresentation");
        RealmRepresentation realm = CloneUtil.deepClone(realmImport, RealmRepresentation.class, ignoredPropertiesForRealmImport);
        logger.debug("RealmRepresentation created for update: {}", realm);

        logger.info("Fetching existing realm '{}'", realmImport.getRealm());
        RealmRepresentation existingRealm = realmRepository.get(realmImport.getRealm());
        logger.debug("Existing realm retrieved: {}", existingRealm);

        if (existingRealm.getEventsExpiration() != null) {
            logger.debug("Preserving events expiration: {}", existingRealm.getEventsExpiration());
            realm.setEventsExpiration(existingRealm.getEventsExpiration());
        }

        logger.info("Updating OTP policy for realm '{}'", realmImport.getRealm());
        otpPolicyImportService.updateOtpPolicy(realmImport.getRealm(), realm);
        logger.debug("OTP policy updated for realm '{}'", realmImport.getRealm());
        logger.debug("Loading state for updated realm '{}'", realmImport.getRealm());
        stateService.loadState(realm);
        logger.debug("State loaded for updated realm '{}'", realmImport.getRealm());

        logger.info("Updating realm in repository");
        realmRepository.update(realm);
        logger.info("Realm '{}' updated successfully", realm.getRealm());

        logger.info("Configuring updated realm '{}'", realmImport.getRealm());
        configureRealm(realmImport, realm);
        logger.info("Configuration completed for updated realm '{}'", realmImport.getRealm());
        logger.info("Update of realm '{}' completed", realmImport.getRealm());
    }


    private void importOtpPolicy(RealmImport realmImport) {
        RealmRepresentation realmConfig = realmRepository.get(realmImport.getRealm());
        if (realmConfig.getOtpPolicyAlgorithm() != null) {
            otpPolicyImportService.updateOtpPolicy(
                    realmImport.getRealm(),
                    realmConfig
            );
        }
    }

    private void configureRealm(RealmImport realmImport, RealmRepresentation existingRealm) {
        importOtpPolicy(realmImport);
        clientScopeImportService.doImport(realmImport);
        clientScopeImportService.updateDefaultClientScopes(realmImport, existingRealm);
        clientPoliciesImportService.doImport(realmImport);
        clientImportService.doImport(realmImport);
        roleImportService.doImport(realmImport);
        groupImportService.importGroups(realmImport);
        defaultGroupsImportService.doImport(realmImport);
        componentImportService.doImport(realmImport);
        userProfileImportService.doImport(realmImport);
        userImportService.doImport(realmImport);
        requiredActionsImportService.doImport(realmImport);
        authenticationFlowsImportService.doImport(realmImport);
        authenticatorConfigImportService.doImport(realmImport);
        clientImportService.doImportDependencies(realmImport);
        clientScopeImportService.updateDefaultClientScopes(realmImport, existingRealm);
        identityProviderImportService.doImport(realmImport);
        clientAuthorizationImportService.doImport(realmImport);
        scopeMappingImportService.doImport(realmImport);
        clientScopeMappingImportService.doImport(realmImport);
        clientScopeImportService.doRemoveOrphan(realmImport);
        messageBundleImportService.doImport(realmImport);

        stateService.doImport(realmImport);
        checksumService.doImport(realmImport);
    }
}
