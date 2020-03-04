package com.github.borisskert.keycloak.config.model;

import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RolesImport extends RolesRepresentation {

    @Override
    public List<RoleRepresentation> getRealm() {
        if (realm == null) {
            return Collections.EMPTY_LIST;
        }

        return realm;
    }

    @Override
    public Map<String, List<RoleRepresentation>> getClient() {
        if (client == null) {
            return Collections.EMPTY_MAP;
        }

        return client;
    }
}
