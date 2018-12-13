package de.adorsys.keycloak.config.util;

import javax.ws.rs.core.Response;

public class ResponseUtil {

    private ResponseUtil() {
        throw new UnsupportedOperationException();
    }

    public static void throwOnError(Response response) {
        try {
            if (response.getStatus() > 201) {
                throw new RuntimeException(response.getStatusInfo().getReasonPhrase());
            }
        } finally {
            response.close();
        }
    }
}
