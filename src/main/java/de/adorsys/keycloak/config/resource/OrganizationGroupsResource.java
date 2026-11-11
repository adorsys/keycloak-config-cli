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

import de.adorsys.keycloak.config.model.OrganizationGroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

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
 * JAX-RS proxy for the organization groups API of Keycloak 26.6+, which the admin client does not provide.
 */
public interface OrganizationGroupsResource {

    String GROUPS = "/admin/realms/{realm}/organizations/{org}/groups";

    @GET
    @Path(GROUPS)
    @Produces(MediaType.APPLICATION_JSON)
    List<OrganizationGroupRepresentation> getTopLevelGroups(
            @PathParam("realm") String realm,
            @PathParam("org") String org,
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max);

    @GET
    @Path(GROUPS + "/{id}/children")
    @Produces(MediaType.APPLICATION_JSON)
    List<OrganizationGroupRepresentation> getChildren(
            @PathParam("realm") String realm,
            @PathParam("org") String org,
            @PathParam("id") String id,
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max);

    @GET
    @Path(GROUPS + "/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    OrganizationGroupRepresentation getGroup(
            @PathParam("realm") String realm,
            @PathParam("org") String org,
            @PathParam("id") String id);

    @POST
    @Path(GROUPS)
    @Consumes(MediaType.APPLICATION_JSON)
    Response createTopLevelGroup(
            @PathParam("realm") String realm,
            @PathParam("org") String org,
            OrganizationGroupRepresentation group);

    @POST
    @Path(GROUPS + "/{id}/children")
    @Consumes(MediaType.APPLICATION_JSON)
    Response createChild(
            @PathParam("realm") String realm,
            @PathParam("org") String org,
            @PathParam("id") String parentId,
            OrganizationGroupRepresentation group);

    @PUT
    @Path(GROUPS + "/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    void updateGroup(
            @PathParam("realm") String realm,
            @PathParam("org") String org,
            @PathParam("id") String id,
            OrganizationGroupRepresentation group);

    @DELETE
    @Path(GROUPS + "/{id}")
    void deleteGroup(
            @PathParam("realm") String realm,
            @PathParam("org") String org,
            @PathParam("id") String id);

    @GET
    @Path(GROUPS + "/{id}/members")
    @Produces(MediaType.APPLICATION_JSON)
    List<UserRepresentation> getMembers(
            @PathParam("realm") String realm,
            @PathParam("org") String org,
            @PathParam("id") String id,
            @QueryParam("first") Integer first,
            @QueryParam("max") Integer max);

    @PUT
    @Path(GROUPS + "/{id}/members/{userId}")
    void addMember(
            @PathParam("realm") String realm,
            @PathParam("org") String org,
            @PathParam("id") String id,
            @PathParam("userId") String userId);

    @DELETE
    @Path(GROUPS + "/{id}/members/{userId}")
    void removeMember(
            @PathParam("realm") String realm,
            @PathParam("org") String org,
            @PathParam("id") String id,
            @PathParam("userId") String userId);
}
