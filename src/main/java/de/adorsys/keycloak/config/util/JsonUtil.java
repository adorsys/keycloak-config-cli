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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import de.adorsys.keycloak.config.exception.ImportProcessingException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;

public class JsonUtil {
    private JsonUtil() {
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<String> fromJson(String value) {
        try {
            return objectMapper.readValue(value, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (IOException e) {
            throw new ImportProcessingException(e);
        }
    }

    public static String toJson(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String string) {
            return string;
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new ImportProcessingException(e);
        }
    }

    public static JsonNode getJsonOrNullNode(String value) {
        final JsonNode currentValue;
        if (StringUtils.isEmpty(value)) {
            currentValue = NullNode.getInstance();
        } else {
            currentValue = JsonUtil.fromJsonAsNode(value);
        }
        return currentValue;
    }

    private static JsonNode fromJsonAsNode(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException e) {
            throw new ImportProcessingException(e);
        }
    }

    public static <T> T readValue(String value, Class<T> type) {
        try {
            return value == null ? null : objectMapper.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new ImportProcessingException(e);
        }
    }
}
