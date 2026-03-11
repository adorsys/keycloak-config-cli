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
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.util.JsonUtil;
import org.keycloak.admin.client.resource.UserProfileResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Component
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class UserProfileRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileRepository.class);

    public static final String REALM_ATTRIBUTES_USER_PROFILE_ENABLED_STRING = "userProfileEnabled";

    private final RealmRepository realmRepository;
    private final KeycloakProvider keycloakProvider;

    @Autowired
    public UserProfileRepository(RealmRepository realmRepository, KeycloakProvider keycloakProvider) {
        this.realmRepository = realmRepository;
        this.keycloakProvider = keycloakProvider;
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
            resolveUserProfileUpdate(realm, newUserProfileConfiguration);
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

    private void resolveUserProfileUpdate(String realm, String newUserProfileConfiguration) {
        // Use raw HTTP call to send JSON string directly to preserve fields like defaultValue
        // that may not exist in the client library's UPConfig class
        var keycloak = keycloakProvider.getInstance();
        var accessToken = keycloak.tokenManager().getAccessToken().getToken();
        var url = keycloakProvider.getUrl();
        
        try (var client = ClientBuilder.newClient()) {
            var target = client.target(url)
                    .path("/admin/realms/" + realm + "/users/profile");
            
            var response = target.request()
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .put(Entity.entity(newUserProfileConfiguration, MediaType.APPLICATION_JSON));
            
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                var errorEntity = response.readEntity(String.class);
                throw new KeycloakRepositoryException(
                    "Failed to update user profile. Status: " + response.getStatus()
                    + ", Error: " + errorEntity);
            }
        }
    }

    private UserProfileResource getResource(String realmName) {
        return this.realmRepository.getResource(realmName).users().userProfile();
    }
}
