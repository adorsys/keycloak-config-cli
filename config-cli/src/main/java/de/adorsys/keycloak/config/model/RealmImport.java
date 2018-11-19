package de.adorsys.keycloak.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RealmImport extends RealmRepresentation {

    private CustomImport customImport;
    private MultivaluedHashMap<String, ComponentImport> components;

    public Optional<CustomImport> getCustomImport() {
        return Optional.ofNullable(customImport);
    }

    public static class CustomImport {
        @JsonProperty("removeImpersonation")
        private Boolean removeImpersonation;

        public Boolean removeImpersonation() {
            return removeImpersonation != null && removeImpersonation;
        }
    }

    @Override
    public MultivaluedHashMap<String, ComponentExportRepresentation> getComponents() {
        return (MultivaluedHashMap) components;
    }

    @JsonSetter("components")
    public void setComponentImports(MultivaluedHashMap<String, ComponentImport> components) {
        this.components = components;
    }

    @Override
    public List<AuthenticatorConfigRepresentation> getAuthenticatorConfig() {
        List<AuthenticatorConfigRepresentation> authenticatorConfig = super.getAuthenticatorConfig();

        if(authenticatorConfig == null) {
            authenticatorConfig = new ArrayList<>();
        }

        return authenticatorConfig;
    }
}
