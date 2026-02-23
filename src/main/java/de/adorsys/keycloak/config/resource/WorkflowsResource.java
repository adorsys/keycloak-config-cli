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

import de.adorsys.keycloak.config.model.WorkflowRepresentation;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * JAX-RS proxy interface for the Keycloak Workflows REST API
 * (/admin/realms/{realm}/workflows).
 */
public interface WorkflowsResource {

    @GET
    @Path("/admin/realms/{realm}/workflows")
    @Produces(MediaType.APPLICATION_JSON)
    List<WorkflowRepresentation> listWorkflows(
            @PathParam("realm") String realm,
            @QueryParam("search") String search,
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max,
            @QueryParam("exact") Boolean exact);

    @POST
    @Path("/admin/realms/{realm}/workflows")
    @Consumes(MediaType.APPLICATION_JSON)
    Response createWorkflow(@PathParam("realm") String realm, WorkflowRepresentation workflow);

    @GET
    @Path("/admin/realms/{realm}/workflows/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    WorkflowRepresentation getWorkflow(
            @PathParam("realm") String realm,
            @PathParam("id") String id,
            @QueryParam("includeId") Boolean includeId);

    @PUT
    @Path("/admin/realms/{realm}/workflows/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    void updateWorkflow(
            @PathParam("realm") String realm,
            @PathParam("id") String id,
            WorkflowRepresentation workflow);

    @DELETE
    @Path("/admin/realms/{realm}/workflows/{id}")
    void deleteWorkflow(@PathParam("realm") String realm, @PathParam("id") String id);
}
