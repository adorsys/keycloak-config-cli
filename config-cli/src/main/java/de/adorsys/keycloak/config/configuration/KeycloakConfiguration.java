package de.adorsys.keycloak.config.configuration;

import de.adorsys.keycloak.config.KeycloakImportProperties;
import org.apache.http.client.utils.URIBuilder;
import org.keycloak.admin.client.Keycloak;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URISyntaxException;

@Configuration
public class KeycloakConfiguration {

    @Bean
    public Keycloak createKeycloak(
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
