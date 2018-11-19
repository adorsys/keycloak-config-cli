package de.adorsys.keycloak.config.model;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentExportRepresentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ComponentImport extends ComponentExportRepresentation {

    private MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();

    @Override
    public MultivaluedHashMap<String, String> getConfig() {
        return reduceByDuplicates();
    }

    private MultivaluedHashMap<String, String> reduceByDuplicates() {
        MultivaluedHashMap<String, String> reducedMap = new MultivaluedHashMap<String, String>();

        for (Map.Entry<String, List<String>> entry : config.entrySet()) {

            List<String> reducedList = new ArrayList<>();
            for (String value : entry.getValue()) {
                if(!reducedList.contains(value)) {
                    reducedList.add(value);
                }
            }

            reducedMap.addAll(entry.getKey(), reducedList);
        }

        return reducedMap;
    }
}
