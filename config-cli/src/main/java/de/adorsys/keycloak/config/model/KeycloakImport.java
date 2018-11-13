package de.adorsys.keycloak.config.model;

import java.util.Map;

public class KeycloakImport {

    private final Map<String, RealmImport> realmImports;

    public KeycloakImport(Map<String, RealmImport> realmImports) {
        this.realmImports = realmImports;
    }

    public Map<String, RealmImport> getRealmImports() {
        return realmImports;
    }
}
