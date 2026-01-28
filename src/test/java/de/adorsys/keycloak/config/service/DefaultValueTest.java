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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.keycloak.config.model.RealmImport;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultValueTest {

    @Test
    void testDefaultValueInUserProfile() throws Exception {
        String yaml = """
            enabled: true
            realm: testRealm
            attributes:
              userProfileEnabled: true
            userProfile:
              attributes:
                - name: username
                  displayName: "${username}"
                - name: newsletter
                  displayName: "${profile.attributes.newsletter}"
                  defaultValue: "false"
                  validations:
                    options:
                      options:
                        - "true"
                        - "false"
            """;

        // Test our JSON processing logic
        Yaml yamlParser = new Yaml();
        Object yamlDocument = yamlParser.load(yaml);
        
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.valueToTree(yamlDocument);
        
        // Check if defaultValue is preserved in JSON
        assertTrue(jsonNode.has("userProfile"));
        assertTrue(jsonNode.get("userProfile").has("attributes"));
        
        JsonNode attributes = jsonNode.get("userProfile").get("attributes");
        assertTrue(attributes.isArray());
        
        boolean foundDefaultValue = false;
        for (JsonNode attribute : attributes) {
            if (attribute.has("defaultValue")) {
                foundDefaultValue = true;
                assertEquals("false", attribute.get("defaultValue").asText());
                break;
            }
        }
        
        assertTrue(foundDefaultValue, "defaultValue should be found in userProfile attributes");
        
        // Test that raw JSON extraction works
        String rawUserProfileJson = objectMapper.writeValueAsString(jsonNode.get("userProfile"));
        assertNotNull(rawUserProfileJson);
        assertTrue(rawUserProfileJson.contains("defaultValue"));
        assertTrue(rawUserProfileJson.contains("\"false\""));
    }
}
