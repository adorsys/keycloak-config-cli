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

package de.adorsys.keycloak.config.service.normalize;

import de.adorsys.keycloak.config.properties.NormalizationKeycloakConfigProperties;
import de.adorsys.keycloak.config.provider.BaselineProvider;
import de.adorsys.keycloak.config.util.JaversUtil;
import org.javers.core.Javers;
import org.javers.core.diff.changetype.PropertyChange;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "NORMALIZE")
public class RealmNormalizationService {

    private static final Logger logger = LoggerFactory.getLogger(RealmNormalizationService.class);

    private final NormalizationKeycloakConfigProperties keycloakConfigProperties;
    private final Javers javers;
    private final BaselineProvider baselineProvider;
    private final ClientNormalizationService clientNormalizationService;
    private final ScopeMappingNormalizationService scopeMappingNormalizationService;
    private final ProtocolMapperNormalizationService protocolMapperNormalizationService;
    private final ClientScopeNormalizationService clientScopeNormalizationService;
    private final RoleNormalizationService roleNormalizationService;
    private final AttributeNormalizationService attributeNormalizationService;
    private final GroupNormalizationService groupNormalizationService;
    private final AuthFlowNormalizationService authFlowNormalizationService;
    private final IdentityProviderNormalizationService identityProviderNormalizationService;
    private final RequiredActionNormalizationService requiredActionNormalizationService;
    private final UserFederationNormalizationService userFederationNormalizationService;
    private final ClientPolicyNormalizationService clientPolicyNormalizationService;
    private final JaversUtil javersUtil;

    @Autowired
    public RealmNormalizationService(NormalizationKeycloakConfigProperties keycloakConfigProperties,
                                     Javers javers,
                                     BaselineProvider baselineProvider,
                                     ClientNormalizationService clientNormalizationService,
                                     ScopeMappingNormalizationService scopeMappingNormalizationService,
                                     ProtocolMapperNormalizationService protocolMapperNormalizationService,
                                     ClientScopeNormalizationService clientScopeNormalizationService,
                                     RoleNormalizationService roleNormalizationService,
                                     AttributeNormalizationService attributeNormalizationService,
                                     GroupNormalizationService groupNormalizationService,
                                     AuthFlowNormalizationService authFlowNormalizationService,
                                     IdentityProviderNormalizationService identityProviderNormalizationService,
                                     RequiredActionNormalizationService requiredActionNormalizationService,
                                     UserFederationNormalizationService userFederationNormalizationService,
                                     ClientPolicyNormalizationService clientPolicyNormalizationService,
                                     JaversUtil javersUtil) {
        this.keycloakConfigProperties = keycloakConfigProperties;
        this.javers = javers;
        this.baselineProvider = baselineProvider;
        this.clientNormalizationService = clientNormalizationService;
        this.scopeMappingNormalizationService = scopeMappingNormalizationService;
        this.protocolMapperNormalizationService = protocolMapperNormalizationService;
        this.clientScopeNormalizationService = clientScopeNormalizationService;
        this.roleNormalizationService = roleNormalizationService;
        this.attributeNormalizationService = attributeNormalizationService;
        this.groupNormalizationService = groupNormalizationService;
        this.authFlowNormalizationService = authFlowNormalizationService;
        this.identityProviderNormalizationService = identityProviderNormalizationService;
        this.requiredActionNormalizationService = requiredActionNormalizationService;
        this.userFederationNormalizationService = userFederationNormalizationService;
        this.clientPolicyNormalizationService = clientPolicyNormalizationService;
        this.javersUtil = javersUtil;

        // TODO allow extra "default" values to be ignored?

        // TODO Ignore clients by regex
    }

    public RealmRepresentation normalizeRealm(RealmRepresentation exportedRealm) {
        var keycloakConfigVersion = keycloakConfigProperties.getVersion();
        var exportVersion = exportedRealm.getKeycloakVersion();
        if (!exportVersion.equals(keycloakConfigVersion)) {
            logger.warn("Keycloak-Config-CLI keycloak version {} and export keycloak version {} are not equal."
                            + " This may cause problems if the API changed."
                            + " Please compile keycloak-config-cli with a matching keycloak version!",
                    keycloakConfigVersion, exportVersion);
        }
        var exportedRealmRealm = exportedRealm.getRealm();
        logger.info("Exporting realm {}", exportedRealmRealm);
        var baselineRealm = baselineProvider.getRealm(exportVersion, exportedRealmRealm);

        /*
         * Trick javers into thinking this is the "same" object, by setting the ID on the reference realm
         * to the ID of the current realm. That way we only get actual changes, not a full list of changes
         * including the "object removed" and "object added" changes
         */
        baselineRealm.setRealm(exportedRealm.getRealm());
        var minimizedRealm = new RealmRepresentation();

        handleBaseRealm(exportedRealm, baselineRealm, minimizedRealm);

        var clients = clientNormalizationService.normalizeClients(exportedRealm, baselineRealm);
        if (!clients.isEmpty()) {
            minimizedRealm.setClients(clients);
        }

        // No setter for some reason...
        var minimizedScopeMappings = scopeMappingNormalizationService.normalizeScopeMappings(exportedRealm, baselineRealm);
        if (!minimizedScopeMappings.isEmpty()) {
            var scopeMappings = minimizedRealm.getScopeMappings();
            if (scopeMappings == null) {
                minimizedRealm.clientScopeMapping("dummy");
                scopeMappings = minimizedRealm.getScopeMappings();
                scopeMappings.clear();
            }
            scopeMappings.addAll(minimizedScopeMappings);
        }

        var clientScopeMappings = scopeMappingNormalizationService.normalizeClientScopeMappings(exportedRealm, baselineRealm);
        if (!clientScopeMappings.isEmpty()) {
            minimizedRealm.setClientScopeMappings(clientScopeMappings);
        }

        minimizedRealm.setAttributes(attributeNormalizationService.normalizeStringAttributes(exportedRealm.getAttributes(),
                baselineRealm.getAttributes()));

        minimizedRealm.setProtocolMappers(protocolMapperNormalizationService.normalizeProtocolMappers(exportedRealm.getProtocolMappers(),
                baselineRealm.getProtocolMappers()));

        minimizedRealm.setClientScopes(clientScopeNormalizationService.normalizeClientScopes(exportedRealm.getClientScopes(),
                baselineRealm.getClientScopes()));

        minimizedRealm.setRoles(roleNormalizationService.normalizeRoles(exportedRealm.getRoles(), baselineRealm.getRoles()));

        minimizedRealm.setGroups(groupNormalizationService.normalizeGroups(exportedRealm.getGroups(), baselineRealm.getGroups()));

        var authFlows = authFlowNormalizationService.normalizeAuthFlows(exportedRealm.getAuthenticationFlows(),
                baselineRealm.getAuthenticationFlows());
        minimizedRealm.setAuthenticationFlows(authFlows);
        minimizedRealm.setAuthenticatorConfig(authFlowNormalizationService.normalizeAuthConfig(exportedRealm.getAuthenticatorConfig(), authFlows));

        minimizedRealm.setIdentityProviders(identityProviderNormalizationService.normalizeProviders(exportedRealm.getIdentityProviders(),
                baselineRealm.getIdentityProviders()));
        minimizedRealm.setIdentityProviderMappers(identityProviderNormalizationService.normalizeMappers(exportedRealm.getIdentityProviderMappers(),
                baselineRealm.getIdentityProviderMappers()));

        minimizedRealm.setRequiredActions(requiredActionNormalizationService.normalizeRequiredActions(exportedRealm.getRequiredActions(),
                baselineRealm.getRequiredActions()));
        minimizedRealm.setUserFederationProviders(userFederationNormalizationService.normalizeProviders(exportedRealm.getUserFederationProviders(),
                baselineRealm.getUserFederationProviders()));
        minimizedRealm.setUserFederationMappers(userFederationNormalizationService.normalizeMappers(exportedRealm.getUserFederationMappers(),
                baselineRealm.getUserFederationMappers()));

        minimizedRealm.setParsedClientPolicies(clientPolicyNormalizationService.normalizePolicies(exportedRealm.getParsedClientPolicies(),
                baselineRealm.getParsedClientPolicies()));
        minimizedRealm.setParsedClientProfiles(clientPolicyNormalizationService.normalizeProfiles(exportedRealm.getParsedClientProfiles(),
                baselineRealm.getParsedClientProfiles()));
        return minimizedRealm;
    }

    void handleBaseRealm(RealmRepresentation exportedRealm, RealmRepresentation baselineRealm, RealmRepresentation minimizedRealm) {
        var diff = javers.compare(baselineRealm, exportedRealm);
        for (var change : diff.getChangesByType(PropertyChange.class)) {
            javersUtil.applyChange(minimizedRealm, change);
        }

        // Now that Javers is done, clean up a bit afterwards. We always need to set the realm and enabled fields
        minimizedRealm.setRealm(exportedRealm.getRealm());
        minimizedRealm.setEnabled(exportedRealm.isEnabled());

        // If the realm ID diverges from the name, include it in the dump, otherwise remove it
        if (Objects.equals(exportedRealm.getRealm(), exportedRealm.getId())) {
            minimizedRealm.setId(null);
        } else {
            minimizedRealm.setId(exportedRealm.getId());
        }
    }


    public static <K, V> Map<K, V> getNonNull(Map<K, V> in) {
        return in == null ? new HashMap<>() : in;
    }

    public static <E> List<E> getNonNull(List<E> in) {
        return in == null ? new ArrayList<>() : in;
    }
}
