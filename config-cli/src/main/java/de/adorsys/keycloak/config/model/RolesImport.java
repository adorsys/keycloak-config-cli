package de.adorsys.keycloak.config.model;

import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RolesImport extends RolesRepresentation {

    @Override
    public List<RoleRepresentation> getRealm() {
        if(realm == null) {
            return Collections.emptyList();
        }

        return realm;
    }

    @Override
    public Map<String, List<RoleRepresentation>> getClient() {
        if(client == null) {
            return Collections.emptyMap();
        }

        return client;
    }
}
