package de.adorsys.keycloak.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Component
@ConfigurationProperties(prefix = "keycloak")
@Validated
public class KeycloakImportProperties {

    @NotNull
    @Size(min = 1)
    private String realm;

    @NotNull
    @Size(min = 1)
    private String clientId;

    @NotNull
    @Size(min = 1)
    private String url;

    @NotNull
    @Size(min = 1)
    private String user;

    @NotNull
    @Size(min = 1)
    private String password;


    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
