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
import de.adorsys.keycloak.config.util.JsonUtil;
import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class UserProfileRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileRepository.class);

    public static final String REALM_ATTRIBUTES_USER_PROFILE_ENABLED_STRING = "userProfileEnabled";

    private final RealmRepository realmRepository;

    @Autowired
    public UserProfileRepository(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    public void updateUserProfile(String realm, boolean newUserProfileEnabled, String newUserProfileConfiguration) {

        var userProfileResource = getResource(realm);
        if (userProfileResource == null) {
            logger.error("Could not retrieve UserProfile resource.");
            return;
        }

        if (!newUserProfileEnabled) {
            logger.trace("UserProfile is explicitly disabled, removing configuration.");
            userProfileResource.update(null);
            logger.trace("UserProfile configuration removed.");
            return;
        }

        var realmAttributes = realmRepository.get(realm).getAttributesOrEmpty();
        var currentUserProfileConfiguration = getUserProfileConfiguration(userProfileResource);
        if (!StringUtils.hasText(currentUserProfileConfiguration)) {
            logger.warn("UserProfile is enabled, but no configuration string provided.");
            return;
        }

        var currentUserProfileEnabled = Boolean.parseBoolean(realmAttributes.getOrDefault(REALM_ATTRIBUTES_USER_PROFILE_ENABLED_STRING, "false"));
        if (!currentUserProfileEnabled) {
            logger.warn("UserProfile enabled attribute in realm differs from configuration. "
                    + "This is strange, because the attribute import should have done that already.");
        }

        var userProfileConfigChanged = hasUserProfileConfigurationChanged(newUserProfileConfiguration, currentUserProfileConfiguration);
        if (!userProfileConfigChanged) {
            logger.trace("UserProfile did not change, skipping update.");
            return;
        }

        try {
            resolveUserProfileUpdate(userProfileResource, newUserProfileConfiguration);
        } catch (Exception ex) {
            throw new KeycloakRepositoryException("Could not update UserProfile Definition", ex);
        }

        logger.trace("UserProfile updated.");
    }

    private boolean hasUserProfileConfigurationChanged(String newUserProfileConfiguration, String currentUserProfileConfiguration) {
        var newValue = JsonUtil.getJsonOrNullNode(newUserProfileConfiguration);
        var currentValue = JsonUtil.getJsonOrNullNode(currentUserProfileConfiguration);
        return !currentValue.equals(newValue);
    }

    private String getUserProfileConfiguration(UserProfileResource userProfileResource) {
        return JsonUtil.toJson(userProfileResource.getConfiguration());
    }

    private void resolveUserProfileUpdate(UserProfileResource userProfileResource, String newUserProfileConfiguration) {
        userProfileResource.update(JsonUtil.readValue(newUserProfileConfiguration, UPConfig.class));
    }

    private UserProfileResource getResource(String realmName) {
        return this.realmRepository.getResource(realmName).users().userProfile();
    }
}
