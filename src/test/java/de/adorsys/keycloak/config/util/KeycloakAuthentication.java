/*
 * Copyright 2019-2020 adorsys GmbH & Co. KG @ https://adorsys.de
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package de.adorsys.keycloak.config.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.text.MessageFormat;

@Component
public class KeycloakAuthentication {
    private static final String TOKEN_URL_TEMPLATE = "{0}/auth/realms/{1}/protocol/openid-connect/token";

    private final RestTemplate restTemplate;

    private final KeycloakConfigProperties keycloakConfigProperties;

    @Autowired
    public KeycloakAuthentication(
            RestTemplate restTemplate,
            KeycloakConfigProperties keycloakConfigProperties
    ) {
        this.restTemplate = restTemplate;
        this.keycloakConfigProperties = keycloakConfigProperties;
    }

    public AuthenticationToken login(
            String realm,
            String clientId,
            String clientSecret,
            String username,
            String password
    ) throws AuthenticationException {
        return login(
                keycloakConfigProperties.getUrl(),
                realm,
                clientId,
                clientSecret,
                username,
                password
        );
    }

    public AuthenticationToken login(
            String url,
            String realm,
            String clientId,
            String clientSecret,
            String username,
            String password
    ) throws AuthenticationException {
        String tokenUrl = MessageFormat.format(TOKEN_URL_TEMPLATE, url, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", username);
        body.add("password", password);
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        ResponseEntity<AuthenticationToken> response;
        try {
            response = restTemplate.postForEntity(
                    tokenUrl,
                    new HttpEntity<>(body, headers),
                    AuthenticationToken.class
            );
        } catch (HttpClientErrorException e) {
            throw new AuthenticationException(e);
        }

        return response.getBody();
    }

    public static class AuthenticationToken {

        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("expires_in")
        private Integer expiresIn;

        @JsonProperty("refresh_expires_in")
        private Integer refreshExpiresIn;

        private String scope;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }

        public Integer getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(Integer expiresIn) {
            this.expiresIn = expiresIn;
        }

        public Integer getRefreshExpiresIn() {
            return refreshExpiresIn;
        }

        public void setRefreshExpiresIn(Integer refreshExpiresIn) {
            this.refreshExpiresIn = refreshExpiresIn;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }
    }

    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(Throwable cause) {
            super(cause);
        }
    }
}
