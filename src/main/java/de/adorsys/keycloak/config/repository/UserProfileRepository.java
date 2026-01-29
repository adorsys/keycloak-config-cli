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
import de.adorsys.keycloak.config.resource.UserProfileConfigurationResource;
import de.adorsys.keycloak.config.util.JsonUtil;
import de.adorsys.keycloak.config.util.ResponseUtil;
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

        var userProfileResource = getResource();
        if (userProfileResource == null) {
            logger.error("Could not retrieve UserProfile resource.");
            return;
        }

        if (!newUserProfileEnabled) {
            logger.trace("UserProfile is explicitly disabled, removing configuration.");
            userProfileResource.updateUserProfileConfiguration(realm, null);
            logger.trace("UserProfile configuration removed.");
            return;
        }

        var realmAttributes = realmRepository.get(realm).getAttributesOrEmpty();
        var currentUserProfileConfiguration = getUserProfileConfiguration(userProfileResource, realm);
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
            resolveUserProfileUpdate(userProfileResource, realm, newUserProfileConfiguration,
                    currentUserProfileConfiguration);
        } catch (Exception ex) {
            if (ex instanceof jakarta.ws.rs.WebApplicationException webApplicationException) {
                throw new KeycloakRepositoryException(
                        "Could not update UserProfile Definition: " + ResponseUtil.getErrorMessage(webApplicationException),
                        ex);
            }
            throw new KeycloakRepositoryException("Could not update UserProfile Definition", ex);
        }

        logger.trace("UserProfile updated.");
    }

    private boolean hasUserProfileConfigurationChanged(String newUserProfileConfiguration, String currentUserProfileConfiguration) {
        var newValue = JsonUtil.getJsonOrNullNode(newUserProfileConfiguration);
        var currentValue = JsonUtil.getJsonOrNullNode(currentUserProfileConfiguration);
        return !currentValue.equals(newValue);
    }

    private String getUserProfileConfiguration(UserProfileConfigurationResource userProfileResource, String realm) {
        return JsonUtil.toJson(userProfileResource.getUserProfileConfiguration(realm));
    }

    private void resolveUserProfileUpdate(UserProfileConfigurationResource userProfileResource, String realm,
                                          String newUserProfileConfiguration, String currentUserProfileConfiguration) {
        var newValue = JsonUtil.readValue(newUserProfileConfiguration, com.fasterxml.jackson.databind.JsonNode.class);
        var currentValue = JsonUtil.readValue(currentUserProfileConfiguration, com.fasterxml.jackson.databind.JsonNode.class);
        var mergedValue = mergeUserProfileConfiguration(newValue, currentValue);

        if (logger.isTraceEnabled()) {
            logger.trace("Updating UserProfile with configuration: {}", JsonUtil.toJson(mergedValue));
        }

        userProfileResource.updateUserProfileConfiguration(realm, mergedValue);
    }

    private com.fasterxml.jackson.databind.JsonNode mergeUserProfileConfiguration(
            com.fasterxml.jackson.databind.JsonNode newValue,
            com.fasterxml.jackson.databind.JsonNode currentValue) {
        if (!(newValue instanceof com.fasterxml.jackson.databind.node.ObjectNode newObject)
                || !(currentValue instanceof com.fasterxml.jackson.databind.node.ObjectNode currentObject)) {
            return newValue;
        }

        var merged = currentObject.deepCopy();
        merged.setAll(newObject);

        var mergedAttributes = mergeAttributeArrays(
                newObject.get("attributes"),
                currentObject.get("attributes"));
        if (mergedAttributes != null) {
            merged.set("attributes", mergedAttributes);
        }

        return merged;
    }

    private com.fasterxml.jackson.databind.JsonNode mergeAttributeArrays(
            com.fasterxml.jackson.databind.JsonNode newAttributes,
            com.fasterxml.jackson.databind.JsonNode currentAttributes) {
        if (!(currentAttributes instanceof com.fasterxml.jackson.databind.node.ArrayNode currentArray)) {
            return newAttributes;
        }

        if (!(newAttributes instanceof com.fasterxml.jackson.databind.node.ArrayNode newArray)) {
            return currentArray;
        }

        var merged = currentArray.deepCopy();
        for (com.fasterxml.jackson.databind.JsonNode newAttribute : newArray) {
            var newNameNode = newAttribute.get("name");
            var newName = newNameNode == null ? null : newNameNode.asText();
            if (newName == null) {
                merged.add(newAttribute);
                continue;
            }

            var replaced = false;
            for (int index = 0; index < merged.size(); index++) {
                var existingAttribute = merged.get(index);
                var existingNameNode = existingAttribute.get("name");
                var existingName = existingNameNode == null ? null : existingNameNode.asText();
                if (newName.equals(existingName)) {
                    ((com.fasterxml.jackson.databind.node.ArrayNode) merged).set(index, newAttribute);
                    replaced = true;
                    break;
                }
            }

            if (!replaced) {
                merged.add(newAttribute);
            }
        }

        ensureRequiredAttribute("username", currentArray, merged);
        ensureRequiredAttribute("email", currentArray, merged);
        applyDefaultValueFallback(merged);

        return merged;
    }

    private void applyDefaultValueFallback(com.fasterxml.jackson.databind.node.ArrayNode attributes) {
        for (com.fasterxml.jackson.databind.JsonNode attribute : attributes) {
            if (attribute instanceof com.fasterxml.jackson.databind.node.ObjectNode attributeObject
                    && !attributeObject.has("defaultValue")
                    && hasBooleanOptions(attributeObject)) {
                attributeObject.put("defaultValue", "false");
            }
        }
    }

    private boolean hasBooleanOptions(com.fasterxml.jackson.databind.node.ObjectNode attributeObject) {
        var validations = attributeObject.get("validations");
        if (!(validations instanceof com.fasterxml.jackson.databind.node.ObjectNode)) {
            return false;
        }

        var optionsValidation = validations.get("options");
        if (!(optionsValidation instanceof com.fasterxml.jackson.databind.node.ObjectNode)) {
            return false;
        }

        var options = optionsValidation.get("options");
        if (!(options instanceof com.fasterxml.jackson.databind.node.ArrayNode optionsArray)) {
            return false;
        }

        var hasTrue = false;
        var hasFalse = false;
        for (com.fasterxml.jackson.databind.JsonNode option : optionsArray) {
            if (!option.isTextual()) {
                continue;
            }
            var value = option.asText();
            if ("true".equalsIgnoreCase(value)) {
                hasTrue = true;
            } else if ("false".equalsIgnoreCase(value)) {
                hasFalse = true;
            }
        }

        return hasTrue && hasFalse;
    }

    private void ensureRequiredAttribute(String attributeName,
                                         com.fasterxml.jackson.databind.node.ArrayNode currentAttributes,
                                         com.fasterxml.jackson.databind.node.ArrayNode mergedAttributes) {
        if (findAttributeByName(mergedAttributes, attributeName) != null) {
            return;
        }

        var attribute = findAttributeByName(currentAttributes, attributeName);
        if (attribute != null) {
            mergedAttributes.add(attribute);
        }
    }

    private com.fasterxml.jackson.databind.JsonNode findAttributeByName(
            com.fasterxml.jackson.databind.node.ArrayNode attributes,
            String attributeName) {
        for (com.fasterxml.jackson.databind.JsonNode attribute : attributes) {
            var nameNode = attribute.get("name");
            var name = nameNode == null ? null : nameNode.asText();
            if (attributeName.equals(name)) {
                return attribute;
            }
        }

        return null;
    }

    private UserProfileConfigurationResource getResource() {
        return this.realmRepository.getKeycloakProvider().getCustomApiProxy(UserProfileConfigurationResource.class);
    }
}
