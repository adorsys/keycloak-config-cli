/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2021 adorsys GmbH & Co. KG @ https://adorsys.com
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package de.adorsys.keycloak.config.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.MapType;
import de.adorsys.keycloak.config.exception.ImportProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class CloneUtil {
    private static final Logger logger = LoggerFactory.getLogger(CloneUtil.class);

    private static final ObjectMapper nonNullMapper;
    private static final ObjectMapper nonFailingMapper;

    static {
        nonNullMapper = new ObjectMapper();
        nonNullMapper.setSerializationInclusion(Include.NON_NULL);
        nonNullMapper.setDefaultMergeable(true);

        nonFailingMapper = new ObjectMapper();
        nonFailingMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    CloneUtil() {
        throw new IllegalStateException("Utility class");
    }

    @SuppressWarnings("unchecked")
    public static <T> T deepClone(T object, String... ignoredProperties) {
        if (object == null) return null;

        return (T) deepClone(object, object.getClass(), ignoredProperties);
    }

    public static <T, S> T deepClone(S object, Class<T> targetClass, String... ignoredProperties) {
        if (object == null) return null;

        JsonNode jsonNode = toJsonNode(object, ignoredProperties);

        try {
            return nonFailingMapper.treeToValue(jsonNode, targetClass);
        } catch (IOException e) {
            throw new ImportProcessingException(e);
        }
    }

    /**
     * This patch will not merge list properties
     */

    public static <T, S> S patch(S origin, T patch, String... ignoredProperties) {
        if (origin == null) return null;
        if (patch == null) return origin;

        S _origin = CloneUtil.deepClone(origin);
        T _patch = CloneUtil.deepClone(patch, ignoredProperties);

        ObjectReader objectReader = nonFailingMapper.readerForUpdating(_origin);
        JsonNode patchAsNode = nonNullMapper.valueToTree(_patch);

        try {
            return objectReader.readValue(patchAsNode);
        } catch (IOException e) {
            throw new ImportProcessingException(e);
        }
    }

    public static <T, S> S deepPatch(S origin, T patch, String... ignoredProperties) {
        if (origin == null) return null;

        Map<String, Object> patchAsMap = toMap(patch, ignoredProperties);
        return patchFromMap(origin, patchAsMap);
    }

    public static <S, T> boolean deepEquals(S origin, T other, String... ignoredProperties) {
        Map<String, Object> originAsMap = toMap(origin, ignoredProperties);
        Map<String, Object> otherAsMap = toMap(other, ignoredProperties);

        boolean ret = Objects.equals(originAsMap, otherAsMap);
        logger.trace("objects.deepEquals: ret: {} | origin: {} | other: {} | ignoredProperties: {}", ret, originAsMap, otherAsMap, ignoredProperties);
        return ret;
    }

    private static <S> Map<String, Object> toMap(S object, String... ignoredProperties) {
        JsonNode objectAsNode = toJsonNode(object, ignoredProperties);

        return jsonNodeToMap(objectAsNode);
    }

    private static Map<String, Object> jsonNodeToMap(JsonNode objectAsNode) {
        MapType typeRef = nonFailingMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class);

        try {
            ObjectReader reader = nonFailingMapper.readerFor(typeRef);
            return reader.readValue(objectAsNode);
        } catch (IOException e) {
            throw new ImportProcessingException(e);
        }
    }

    private static <S> JsonNode toJsonNode(S object, String... ignoredProperties) {
        JsonNode objectAsNode = nonNullMapper.valueToTree(object);

        removeIgnoredProperties(objectAsNode, ignoredProperties);

        return objectAsNode;
    }

    @SuppressWarnings("unchecked")
    private static <T> T patchFromMap(T origin, Map<String, Object> patchAsMap) {
        JsonNode patchAsNode = nonNullMapper.valueToTree(patchAsMap);
        JsonNode originAsNode = nonNullMapper.valueToTree(origin);

        try {
            nonNullMapper.readerForUpdating(originAsNode).readValue(patchAsNode);
            return (T) nonFailingMapper.treeToValue(originAsNode, origin.getClass());
        } catch (IOException e) {
            throw new ImportProcessingException(e);
        }
    }

    private static void removeIgnoredProperties(JsonNode jsonNode, String[] ignoredProperties) {
        if (jsonNode.isObject()) {
            removeIgnoredProperties((ObjectNode) jsonNode, ignoredProperties);
        } else if (jsonNode.isArray()) {
            removeIgnoredProperties((ArrayNode) jsonNode, ignoredProperties);
        }
    }

    private static void removeIgnoredProperties(ArrayNode arrayNode, String[] ignoredProperties) {
        for (JsonNode node : arrayNode) {
            removeIgnoredProperties(node, ignoredProperties);
        }
    }

    private static void removeIgnoredProperties(ObjectNode objectNode, String[] ignoredProperties) {
        for (String ignoredProperty : ignoredProperties) {
            objectNode.remove(ignoredProperty);
        }
    }
}
