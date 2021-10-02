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

import org.apache.commons.lang3.StringUtils;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.representations.info.SystemInfoRepresentation;
import org.mockserver.model.*;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;

import static org.mockserver.model.HttpResponse.response;

public class KeycloakMock {
    static Headers cookieHeader = new Headers(
            new Header("Set-Cookie", "key_expires=value; Expires=Sat, 09 Oct 2021 06:38:53 GMT; Path=/"),
            new Header("Set-Cookie", "key=value")
    );

    public static HttpResponse grantToken(HttpRequest request) throws JsonProcessingException {
        AccessTokenResponse token = new AccessTokenResponse();
        String dummyToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        token.setToken(dummyToken);
        token.setRefreshToken(dummyToken);
        token.setExpiresIn(60);
        token.setRefreshExpiresIn(1800);
        token.setTokenType("Bearer");
        token.setNotBeforePolicy(0);
        token.setSessionState("a04afd4e-d871-4357-b635-1754138ba341");
        token.setScope("profile email");

        String json = new ObjectMapper().writeValueAsString(token);

        return response()
                .withHeaders(cookieHeader)
                .withBody(json, MediaType.APPLICATION_JSON);
    }

    public static HttpResponse serverInfo(HttpRequest request) throws JsonProcessingException {
        ServerInfoRepresentation serverInfo = new ServerInfoRepresentation();
        serverInfo.setSystemInfo(SystemInfoRepresentation.create(0));

        String json = new ObjectMapper().writeValueAsString(serverInfo);
        return response()
                .withHeaders(cookieHeader)
                .withBody(json, MediaType.APPLICATION_JSON);
    }

    public static HttpResponse realm(HttpRequest request) throws JsonProcessingException {
        if (request.matches("PUT")) {
            return noContent(request);
        }

        String realmName = StringUtils.substringAfterLast(request.getPath().toString(), "/");

        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(realmName);
        realm.setId("ef6addd5-7f2d-4781-ad89-99e0ed2e5eb7");
        realm.setEventsEnabled(false);
        realm.setAttributes(Collections.emptyMap());

        String json = new ObjectMapper().writeValueAsString(realm);

        return response()
                .withHeaders(cookieHeader)
                .withBody(json, MediaType.APPLICATION_JSON);
    }

    public static HttpResponse emptyList(HttpRequest request) {
        return response()
                .withHeaders(cookieHeader)
                .withBody("[]", MediaType.APPLICATION_JSON);
    }

    public static HttpResponse noContent(HttpRequest request) {
        return response()
                .withHeaders(cookieHeader)
                .withStatusCode(204);
    }
}
