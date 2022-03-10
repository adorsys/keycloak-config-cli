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
}
