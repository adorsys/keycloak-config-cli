/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2024 adorsys GmbH & Co. KG @ https://adorsys.com
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

package de.adorsys.keycloak.config.resource;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

/**
 * Custom endpoint for user profile configuration as raw JSON.
 */
@Path("/admin/realms/{realm}/users/profile")
public interface UserProfileConfigurationResource {

    @GET
    @Produces({"application/json"})
    JsonNode getUserProfileConfiguration(@PathParam("realm") String realm);

    @PUT
    @Consumes({"application/json"})
    void updateUserProfileConfiguration(@PathParam("realm") String realm, JsonNode configuration);
}
