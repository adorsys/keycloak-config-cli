package de.adorsys.keycloak.config.util;

import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;

import javax.ws.rs.core.Response;

public class ResponseUtil {

    private ResponseUtil() {
        throw new UnsupportedOperationException();
    }

    public static void throwOnError(Response response) {
        try {
            if (response.getStatus() > 201) {
                throw new KeycloakRepositoryException(response.getStatusInfo().getReasonPhrase());
            }
        } finally {
            response.close();
        }
    }
}
