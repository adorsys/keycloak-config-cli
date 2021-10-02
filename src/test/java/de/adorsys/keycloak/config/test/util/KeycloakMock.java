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

package de.adorsys.keycloak.config.test.util;

import org.mockserver.model.*;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import static org.mockserver.model.HttpResponse.response;

public class KeycloakMock {
    static Headers cookieHeader = new Headers(
            new Header("Set-Cookie", "key_expires=value; Expires=Sat, 09 Oct 2021 06:38:53 GMT; Path=/"),
            new Header("Set-Cookie", "key=value")
    );

    public static HttpResponse grantToken(HttpRequest request) throws JsonProcessingException {
        Map<String, Object> map = new HashMap<>();
        map.put("access_token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");
        map.put("expires_in", 60);
        map.put("refresh_expires_in", 1800);
        map.put("refresh_token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");
        map.put("token_type", "Bearer");
        map.put("not-before-policy", 0);
        map.put("session_state", "a04afd4e-d871-4357-b635-1754138ba341");
        map.put("scope", "profile email");

        String json = new ObjectMapper().writeValueAsString(map);

        return response()
                .withHeaders(cookieHeader)
                .withBody(json, MediaType.APPLICATION_JSON);
    }

    public static HttpResponse serverInfo(HttpRequest request) {
        return response()
                .withHeaders(cookieHeader)
                .withBody("{\"systemInfo\":{\"version\":\"15.0.2\"}}", MediaType.APPLICATION_JSON);
    }

    public static HttpResponse realmSimple(HttpRequest request) throws JsonProcessingException {
        if (request.getMethod("GET").equals("PUT")) {
            return response().withStatusCode(204);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("id", "ef6addd5-7f2d-4781-ad89-99e0ed2e5eb7");
        map.put("realm", "simple");
        map.put("eventsEnabled", false);
        map.put("attributes", new HashMap<>());

        String json = new ObjectMapper().writeValueAsString(map);

        return response()
                .withHeaders(cookieHeader)
                .withBody(json, MediaType.APPLICATION_JSON);
    }

    public static HttpResponse emptyList(HttpRequest request) {
        return response()
                .withHeaders(cookieHeader)
                .withBody("[]", MediaType.APPLICATION_JSON);
    }

    public static HttpResponse logout(HttpRequest request) {
        return response()
                .withHeaders(cookieHeader)
                .withStatusCode(204);
    }
}
