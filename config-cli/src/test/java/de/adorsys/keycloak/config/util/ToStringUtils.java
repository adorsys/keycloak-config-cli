package de.adorsys.keycloak.config.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ToStringUtils {
    private static ObjectMapper om = new ObjectMapper();
    static {
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        om.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    private static String jsonToString(Object obj) {
        try {
            return om.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void prettyPrintAsJson(Object obj) {
        System.out.println(jsonToString(obj));
    }
}
