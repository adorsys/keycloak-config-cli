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
import de.adorsys.keycloak.config.repository.UserProfileRepository;
import de.adorsys.keycloak.config.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class UserProfileImportService {
    private static final Logger logger = LoggerFactory.getLogger(UserProfileImportService.class);

    private final UserProfileRepository userProfileRepository;

    @Autowired
    public UserProfileImportService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public void doImport(RealmImport realmImport) {
        var userProfileEnabledString = realmImport.getAttributesOrEmpty().get(UserProfileRepository.REALM_ATTRIBUTES_USER_PROFILE_ENABLED_STRING);
        if (userProfileEnabledString == null) {
            //if not defined at all, ignore everything else
            logger.trace("UpdateProfile realm-attribute '"
                    + UserProfileRepository.REALM_ATTRIBUTES_USER_PROFILE_ENABLED_STRING
                    + "' not set: skipping profile import.");
            return;
        }

        var userProfileEnabled = Boolean.parseBoolean(userProfileEnabledString);
        var userProfileAttributeString = buildUserProfileConfigurationString(realmImport);

        this.userProfileRepository.updateUserProfile(realmImport.getRealm(), userProfileEnabled, userProfileAttributeString);
    }

    private String buildUserProfileConfigurationString(RealmImport realmImport) {
        var userProfile = realmImport.getUserProfile();
        if (userProfile == null) {
            return null;
        }
        return JsonUtil.toJson(userProfile);
    }

}
