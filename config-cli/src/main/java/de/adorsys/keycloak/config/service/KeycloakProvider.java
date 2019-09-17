package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.KeycloakImportProperties;
import org.apache.http.client.utils.URIBuilder;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;

/**
 * This class exists cause we need to create a single keycloak instance or to close the keycloak before using a new one
 * to avoid a deadlock.
 */
@Component
public class KeycloakProvider {

    private final KeycloakImportProperties properties;

    private Keycloak keycloak;
    private boolean isClosed = true;

    @Autowired
    public KeycloakProvider(KeycloakImportProperties properties) {
        this.properties = properties;
    }

    public Keycloak get() {
        if(keycloak == null || isClosed) {
            keycloak = createKeycloak(properties);
            isClosed = false;
        }

        return keycloak;
    }

    public void close() {
        if(!isClosed && keycloak != null) {
            keycloak.close();
        }

        isClosed = true;
    }

    private Keycloak createKeycloak(
            KeycloakImportProperties properties
    ) {
        return Keycloak.getInstance(
                buildUri(properties.getUrl()),
                properties.getRealm(),
                properties.getUser(),
                properties.getPassword(),
                properties.getClientId()
        );
    }

    private String buildUri(String baseUri) {
        try {
            return new URIBuilder(baseUri)
                    .setPath("/auth")
                    .build()
                    .toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
