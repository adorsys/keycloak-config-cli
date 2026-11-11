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

package de.adorsys.keycloak.config.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * A group of an organization (Keycloak 26.6+), as sent to and read from
 * {@code /admin/realms/{realm}/organizations/{id}/groups}.
 * <p>
 * keycloak-config-cli uses its own class instead of the admin client's {@code GroupRepresentation}, because the
 * admin client versions used for older Keycloak builds do not know every property (for example
 * {@code description}). Realm and client roles are not part of it: Keycloak does not support role mappings on
 * organization groups.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganizationGroupRepresentation {

    private String id;
    private String name;
    private String description;
    private String path;
    private Map<String, List<String>> attributes;

    public OrganizationGroupRepresentation() {
    }

    public OrganizationGroupRepresentation(String name, String description, Map<String, List<String>> attributes) {
        this.name = name;
        this.description = description;
        this.attributes = attributes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }
}
