package de.adorsys.keycloak.config.service.clientauthorization;

public interface PermissionResolver {
    String resolveObjectId(String placeholder, String authzName);

    void enablePermissions(String id);
}
