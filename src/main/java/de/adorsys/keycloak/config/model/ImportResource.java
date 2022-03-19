package de.adorsys.keycloak.config.model;

import org.apache.commons.lang3.tuple.MutablePair;

public class ImportResource extends MutablePair<String, String> {
    public ImportResource(String key, String value) {
        super(key, value);
    }

    public String getFilename() {
        return getKey();
    }
}
