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

import com.fasterxml.jackson.databind.JsonNode;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.util.JsonUtil;
import org.keycloak.admin.client.resource.UserProfileResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import javax.ws.rs.core.Response;

@Component
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class UserProfileRepository {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFlowRepository.class);

    public static final String REALM_ATTRIBUTES_USER_PROFILE_ENABLED_STRING = "userProfileEnabled";

    private final RealmRepository realmRepository;

    @Autowired
    public UserProfileRepository(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    public void updateUserProfile(String realm, Boolean newUserProfileEnabled, String newProfileResourceConfiguration) {
        var realmAttributes = realmRepository.get(realm).getAttributesOrEmpty();

        var userProfileResource = getResource(realm);
        if (userProfileResource == null) {
            logger.error("Cannot retrieve userprofile resource");
            return;
        }
        var profileResourceConfiguration = Optional.ofNullable(userProfileResource.getConfiguration()).orElse("");

        if (!newUserProfileEnabled) {
            logger.info("UserProfile explicitly turned off, removing configuration.");
            userProfileResource.update(null);
            return;
        }

        var userProfileEnabled = Boolean.valueOf(realmAttributes.getOrDefault(REALM_ATTRIBUTES_USER_PROFILE_ENABLED_STRING, "false"));
        if (newUserProfileEnabled && profileResourceConfiguration == null) {
            logger.warn("UserProfile is enabled, but no configuration string provided.");
            return;
        }

        if (!userProfileEnabled.equals(newUserProfileEnabled)) {
            logger.warn("UserProfile attribute in realm differs from configuration. "
                    + "This is strange, because the attribute import should have done that already.");
        }

        var profileDefintionHasNotChanged = hasProfileDefinitionChanged(newProfileResourceConfiguration, profileResourceConfiguration);
        if (profileDefintionHasNotChanged) {
            logger.info("UserProfile not changed, so no update.");
            return;
        }

        var updateUserProfileResponse = userProfileResource.update(newProfileResourceConfiguration);
        if (!updateUserProfileResponse.getStatusInfo().equals(Response.Status.OK)) {
            throw new KeycloakRepositoryException("Could not update UserProfile Definition");
        }

        logger.info("UserProfile updated.");
    }

    private boolean hasProfileDefinitionChanged(String newPprofileResourceConfiguration, String profileResourceConfiguration) {
        final JsonNode newValue = JsonUtil.getJsonOrNullNode(newPprofileResourceConfiguration);
        final JsonNode currentValue = JsonUtil.getJsonOrNullNode(profileResourceConfiguration);

        return currentValue.equals(newValue);
    }

    private UserProfileResource getResource(String realmName) {
        return this.realmRepository.getResource(realmName).users().userProfile();
    }
}
