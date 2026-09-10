/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2025 adorsys GmbH & Co. KG @ https://adorsys.com
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

/*
 * Copyright 2017-2024 adorsys GmbH & Co. KG @ https://adorsys.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.adorsys.keycloak.config.util;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.adorsys.keycloak.config.exception.ImportProcessingException;

class JsonUtilTest {

    @Test
    void fromJson_success() {
        String json = "[\"value1\", \"value2\"]";
        List<String> result = JsonUtil.fromJson(json);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("value1"));
        assertTrue(result.contains("value2"));
    }

    @Test
    void fromJson_invalidJson_throwsImportProcessingException() {
        String invalidJson = "invalid json";
        assertThrows(ImportProcessingException.class, () -> JsonUtil.fromJson(invalidJson));
    }

    @Test
    void toJson_object_success() {
        Map<String, String> map = Map.of("key", "value");
        String json = JsonUtil.toJson(map);
        assertNotNull(json);
        assertTrue(json.contains("\"key\":\"value\""));
    }

    @Test
    void toJson_string_success() {
        String value = "{\"key\":\"value\"}";
        String json = JsonUtil.toJson(value);
        assertEquals(value, json);
    }

    @Test
    void toJson_null_returnsNull() {
        assertNull(JsonUtil.toJson(null));
    }

    @Test
    void getJsonOrNullNode_emptyString_returnsNullNode() {
        JsonNode node = JsonUtil.getJsonOrNullNode("");
        assertTrue(node.isNull());
    }

    @Test
    void getJsonOrNullNode_nullString_returnsNullNode() {
        JsonNode node = JsonUtil.getJsonOrNullNode(null);
        assertTrue(node.isNull());
    }

    @Test
    void getJsonOrNullNode_validJson_returnsJsonNode() {
        String json = "{\"key\":\"value\"}";
        JsonNode node = JsonUtil.getJsonOrNullNode(json);
        assertNotNull(node);
        assertFalse(node.isNull());
        assertEquals("value", node.get("key").asText());
    }

    @Test
    void readValue_success() {
        String json = "{\"name\":\"test\"}";
        TestClass result = JsonUtil.readValue(json, TestClass.class);
        assertNotNull(result);
        assertEquals("test", result.name);
    }

    @Test
    void readValue_nullValue_returnsNull() {
        TestClass result = JsonUtil.readValue(null, TestClass.class);
        assertNull(result);
    }

    @Test
    void readValue_invalidJson_throwsImportProcessingException() {
        String invalidJson = "invalid json";
        assertThrows(ImportProcessingException.class, () -> JsonUtil.readValue(invalidJson, TestClass.class));
    }

    static class TestClass {
        public String name;
    }
}
