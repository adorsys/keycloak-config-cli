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

package de.adorsys.keycloak.config.resource;

import de.adorsys.keycloak.config.model.OrganizationDomainsRepresentation;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * JAX-RS proxy interface reading the domains of an organization including their identity provider routing
 * (Keycloak 26.8+), which the admin client's {@code OrganizationResource} drops.
 */
public interface OrganizationDomainsResource {

    @GET
    @Path("/admin/realms/{realm}/organizations/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    OrganizationDomainsRepresentation getOrganization(@PathParam("realm") String realm, @PathParam("id") String id);
}
