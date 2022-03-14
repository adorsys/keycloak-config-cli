/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2022 adorsys GmbH & Co. KG @ https://adorsys.com
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

import org.keycloak.representations.idm.ManagementPermissionReference;
import org.keycloak.representations.idm.ManagementPermissionRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * Routes which are somehow missing from the official Keycloak client library
 */
public interface ManagementPermissions {

    @PUT
    @Path("/admin/realms/{realm}/identity-provider/instances/{alias}/management/permissions")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    ManagementPermissionReference setIdpPermissions(@PathParam("realm") String realm, @PathParam("alias") String alias,
                                                    ManagementPermissionRepresentation var1);

    @GET
    @Path("/admin/realms/{realm}/identity-provider/instances/{alias}/management/permissions")
    @Produces({"application/json"})
    ManagementPermissionReference getIdpPermissions(@PathParam("realm") String realm, @PathParam("alias") String alias);

    @PUT
    @Path("/admin/realms/{realm}/roles-by-id/{id}/management/permissions")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    ManagementPermissionReference setRealmRolePermissions(@PathParam("realm") String realm, @PathParam("id") String id,
                                                          ManagementPermissionRepresentation var1);

    @GET
    @Path("/admin/realms/{realm}/roles-by-id/{id}/management/permissions")
    @Produces({"application/json"})
    ManagementPermissionReference getRealmRolePermissions(@PathParam("realm") String realm, @PathParam("id") String id);
}
