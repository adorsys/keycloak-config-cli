package de.adorsys.keycloak.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.Optional;

public class RealmImport extends RealmRepresentation {

    private CustomImport customImport;

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
}
