package de.adorsys.keycloak.config.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CloneUtils {
    private static ObjectMapper nonNullMapper;
    private static ObjectMapper nonFailingMapper;

    static {
        nonNullMapper = new ObjectMapper();
        nonNullMapper.setSerializationInclusion(Include.NON_NULL);
        nonNullMapper.setDefaultMergeable(true);

        nonFailingMapper = new ObjectMapper();
        nonFailingMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static <T, S> T deepClone(S object, Class<T> targetClass, String... ignoredProperties) {
        if (object == null) return null;

        Map<String, Object> objectAsMap = toMap(object, ignoredProperties);
        return fromMap(objectAsMap, targetClass);
    }

    public static <T> T deepClone(T object, String... ignoredProperties) {
        if (object == null) return null;

        Map<String, Object> objectAsMap = toMap(object, ignoredProperties);
        return (T) fromMap(objectAsMap, object.getClass());
    }

    public static <T, S> S deepPatch(S origin, T patch, String... ignoredProperties) {
        if (origin == null) return null;

        Map<String, Object> patchAsMap = toMap(patch, ignoredProperties);
        return patchFromMap(origin, patchAsMap);
    }

    public static <T, S, C> C deepPatch(S origin, T patch, Class<C> targetClass, String... ignoredProperties) {
        if (origin == null) return null;

        Map<String, Object> patchAsMap = toMap(patch, ignoredProperties);
        return patchFromMap(origin, patchAsMap, targetClass);
    }

    public static <T, S> S deepPatchFieldsOnly(S origin, T patch, String... onlyThisFields) {
        if (origin == null) return null;

        Map<String, Object> patchAsMap = toMapFilteredBy(patch, onlyThisFields);
        return patchFromMap(origin, patchAsMap);
    }

    public static <S, T> boolean deepEquals(S origin, T other, String... ignoredProperties) {
        Map<String, Object> originAsMap = toMap(origin, ignoredProperties);
        Map<String, Object> otherAsMap = toMap(other, ignoredProperties);

        return Objects.equals(originAsMap, otherAsMap);
    }

    private static <S> Map<String, Object> toMap(S object, String... ignoredProperties) {
        JsonNode objectAsNode = nonNullMapper.valueToTree(object);
        Map objectAsMap;

        try {
            objectAsMap = nonFailingMapper.treeToValue(objectAsNode, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        for (String ignoredProperty : ignoredProperties) {
            if (objectAsMap.containsKey(ignoredProperty)) {
                objectAsMap.remove(ignoredProperty);
            }
        }

        return objectAsMap;
    }

    private static <S> Map<String, Object> toMapFilteredBy(S object, String... allowedKeys) {
        JsonNode objectAsNode = nonNullMapper.valueToTree(object);
        Map<String, Object> objectAsMap;

        try {
            objectAsMap = nonFailingMapper.treeToValue(objectAsNode, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Map<String, Object> filteredMap = new HashMap<>();
        for (String allowedKey : allowedKeys) {
            Object allowedValue = objectAsMap.get(allowedKey);
            filteredMap.put(allowedKey, allowedValue);
        }

        return filteredMap;
    }

    private static <T> T fromMap(Map<String, Object> map, Class<T> targetClass) {
        JsonNode mapAsNode = nonNullMapper.valueToTree(map);
        try {
            return nonFailingMapper.treeToValue(mapAsNode, targetClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T patchFromMap(T origin, Map<String, Object> patchAsMap) {
        JsonNode patchAsNode = nonNullMapper.valueToTree(patchAsMap);
        JsonNode originAsNode = nonNullMapper.valueToTree(origin);

        try {
            nonNullMapper.readerForUpdating(originAsNode).readValue(patchAsNode);
            return (T) nonFailingMapper.treeToValue(originAsNode, origin.getClass());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T, C> C patchFromMap(T origin, Map<String, Object> patchAsMap, Class<C> targetClass) {
        JsonNode patchAsNode = nonNullMapper.valueToTree(patchAsMap);
        JsonNode originAsNode = nonNullMapper.valueToTree(origin);

        try {
            nonNullMapper.readerForUpdating(originAsNode).readValue(patchAsNode);
            return nonFailingMapper.treeToValue(originAsNode, targetClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
