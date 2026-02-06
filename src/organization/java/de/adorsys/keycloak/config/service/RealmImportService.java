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
import de.adorsys.keycloak.config.service.organization.OrganizationImporter;
import de.adorsys.keycloak.config.util.CloneUtil;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class RealmImportService {
    private static final Logger logger = LoggerFactory.getLogger(RealmImportService.class);

    private static final String[] ignoredPropertiesForRealmImport = {
        "users", "id", "clientScopeMappings", "clients", "roles", "groups", "defaultGroups", "components", "authenticationFlows",
        "authenticatorConfig", "requiredActions", "identityProviders", "userFederationProviders", "userFederationMappers",
        "scopeMappings", "clientScopeMappings", "browserSecurityHeaders", "oauthClients", "directAccessGrantsEnabled",
        "passwordPolicy", "otpPolicy", "attributes", "eventsEnabled", "eventsExpiration", "eventsListeners",
        "adminEventsDetailsEnabled", "adminEventsEnabled", "internationalizationEnabled", "supportedLocales",
        "defaultLocale", "userProfileEnabled", "userProfile", "organizations"
    };

    private final KeycloakProvider keycloakProvider;
    private final ImportConfigProperties importConfigProperties;
    private final RealmRepository realmRepository;
    private final ClientScopeImportService clientScopeImportService;
    private final ClientPoliciesImportService clientPoliciesImportService;
    private final ClientImportService clientImportService;
    private final RoleImportService roleImportService;
    private final GroupImportService groupImportService;
    private final DefaultGroupsImportService defaultGroupsImportService;
    private final ComponentImportService componentImportService;
    private final UserProfileImportService userProfileImportService;
    private final UserImportService userImportService;
    private final AuthenticationFlowsImportService authenticationFlowsImportService;
    private final IdentityProviderImportService identityProviderImportService;
    private final RequiredActionsImportService requiredActionsImportService;
    private final AuthenticatorConfigImportService authenticatorConfigImportService;
    private final ClientAuthorizationImportService clientAuthorizationImportService;
    private final ScopeMappingImportService scopeMappingImportService;
    private final ClientScopeMappingImportService clientScopeMappingImportService;
    private final OtpPolicyImportService otpPolicyImportService;
    private final ChecksumService checksumService;
    private final StateService stateService;
    private final MessageBundleImportService messageBundleImportService;
    private final Optional<OrganizationImporter> organizationImporter;

    @Autowired
    public RealmImportService(
            KeycloakProvider keycloakProvider,
            ImportConfigProperties importConfigProperties,
            RealmRepository realmRepository,
            ClientScopeImportService clientScopeImportService,
            ClientPoliciesImportService clientPoliciesImportService,
            ClientImportService clientImportService,
            RoleImportService roleImportService,
            GroupImportService groupImportService,
            DefaultGroupsImportService defaultGroupsImportService,
            ComponentImportService componentImportService,
            UserProfileImportService userProfileImportService,
            UserImportService userImportService,
            AuthenticationFlowsImportService authenticationFlowsImportService,
            IdentityProviderImportService identityProviderImportService,
            RequiredActionsImportService requiredActionsImportService,
            AuthenticatorConfigImportService authenticatorConfigImportService,
            ClientAuthorizationImportService clientAuthorizationImportService,
            ScopeMappingImportService scopeMappingImportService,
            ClientScopeMappingImportService clientScopeMappingImportService,
            OtpPolicyImportService otpPolicyImportService,
            ChecksumService checksumService,
            StateService stateService,
            MessageBundleImportService messageBundleImportService,
            Optional<OrganizationImporter> organizationImporter) {

        this.keycloakProvider = keycloakProvider;
        this.importConfigProperties = importConfigProperties;
        this.realmRepository = realmRepository;
        this.clientScopeImportService = clientScopeImportService;
        this.clientPoliciesImportService = clientPoliciesImportService;
        this.clientImportService = clientImportService;
        this.roleImportService = roleImportService;
        this.groupImportService = groupImportService;
        this.defaultGroupsImportService = defaultGroupsImportService;
        this.componentImportService = componentImportService;
        this.userProfileImportService = userProfileImportService;
        this.userImportService = userImportService;
        this.authenticationFlowsImportService = authenticationFlowsImportService;
        this.identityProviderImportService = identityProviderImportService;
        this.requiredActionsImportService = requiredActionsImportService;
        this.authenticatorConfigImportService = authenticatorConfigImportService;
        this.clientAuthorizationImportService = clientAuthorizationImportService;
        this.scopeMappingImportService = scopeMappingImportService;
        this.clientScopeMappingImportService = clientScopeMappingImportService;
        this.otpPolicyImportService = otpPolicyImportService;
        this.checksumService = checksumService;
        this.stateService = stateService;
        this.messageBundleImportService = messageBundleImportService;
        this.organizationImporter = organizationImporter;
    }

    public void doImport(RealmImport realmImport) {
        logger.debug("Importing realm '{}'...", realmImport.getRealm());

        if (realmImport.getRealm() == null) {
            throw new IllegalArgumentException("Realm name is required for import.");
        }

        if (realmRepository.exists(realmImport.getRealm())) {
            updateRealm(realmImport);
        } else {
            createRealm(realmImport);
        }

        checksumService.doImport(realmImport);
    }

    private void createRealm(RealmImport realmImport) {
        logger.debug("Creating realm '{}' ...", realmImport.getRealm());

        RealmRepresentation realm = CloneUtil.deepClone(realmImport, RealmRepresentation.class, ignoredPropertiesForRealmImport);
        realmRepository.create(realm);

        // refresh the access token to update the scopes. See: https://github.com/adorsys/keycloak-config-cli/issues/339
        keycloakProvider.refreshToken();

        stateService.loadState(realmImport);
        configureRealm(realmImport, realm);
    }

    private void updateRealm(RealmImport realmImport) {
        logger.debug("Updating realm '{}'...", realmImport.getRealm());

        RealmRepresentation realm = CloneUtil.deepClone(realmImport, RealmRepresentation.class, ignoredPropertiesForRealmImport);

        RealmRepresentation existingRealm = realmRepository.get(realmImport.getRealm());

        if (realm.getEventsExpiration() == null && existingRealm.getEventsExpiration() != null) {
            realm.setEventsExpiration(existingRealm.getEventsExpiration());
        }

        otpPolicyImportService.updateOtpPolicy(realmImport.getRealm(), realm);
        stateService.loadState(realm);

        realmRepository.update(realm);

        configureRealm(realmImport, realm);
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
        authenticationFlowsImportService.doImport(realmImport);
        identityProviderImportService.doImport(realmImport);
        organizationImporter.ifPresent(importer -> importer.doImport(realmImport));
        requiredActionsImportService.doImport(realmImport);
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

    private void importOtpPolicy(RealmImport realmImport) {
        if (realmImport.getOtpPolicy() != null) {
            otpPolicyImportService.doImport(realmImport);
        }
    }
}
