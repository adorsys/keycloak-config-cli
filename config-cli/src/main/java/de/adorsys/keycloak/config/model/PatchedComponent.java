package de.adorsys.keycloak.config.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentRepresentation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a component which is meant to be patched
 * Avoids merging of config properties because we consider the import to be the full truth to be set
 */
public class PatchedComponent extends ComponentRepresentation {

    /**
     * Use a HashMap instead of a MultivaluedHashMap to avoid merging of lists while patching components
     */
    private Map<String, List<String>> config = new HashMap<>();

    @JsonGetter("config")
    @Override
    public MultivaluedHashMap<String, String> getConfig() {
        MultivaluedHashMap configAsMap = new MultivaluedHashMap();
        configAsMap.putAll(config);

        return configAsMap;
    }

    @JsonSetter("config")
    public void setConfig(Map<String, List<String>> config) {
        this.config = config;
    }
}
