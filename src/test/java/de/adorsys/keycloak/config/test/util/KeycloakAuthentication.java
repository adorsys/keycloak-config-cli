/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2020 adorsys GmbH & Co. KG @ https://adorsys.de
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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

@Component
public class KeycloakAuthentication {
    private static final String TOKEN_URL_TEMPLATE = "{0}/auth/realms/{1}/protocol/openid-connect/token";

    private final ObjectMapper objectMapper;

    private final KeycloakConfigProperties keycloakConfigProperties;

    @Autowired
    public KeycloakAuthentication(
            ObjectMapper objectMapper,
            KeycloakConfigProperties keycloakConfigProperties
    ) {
        this.objectMapper = objectMapper;
        this.keycloakConfigProperties = keycloakConfigProperties;
    }

    public AccessTokenResponse login(
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

    public AccessTokenResponse login(
            String url,
            String realm,
            String clientId,
            String clientSecret,
            String username,
            String password
    ) throws AuthenticationException {
        String tokenUrl = MessageFormat.format(TOKEN_URL_TEMPLATE, url, realm);

        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost http = new HttpPost(tokenUrl);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("username", username));
        params.add(new BasicNameValuePair("password", password));
        params.add(new BasicNameValuePair("grant_type", "password"));
        params.add(new BasicNameValuePair("client_id", clientId));
        params.add(new BasicNameValuePair("client_secret", clientSecret));

        AccessTokenResponse token;

        try {
            http.setEntity(new UrlEncodedFormEntity(params));
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String response = client.execute(http, responseHandler);

            token = objectMapper.readValue(response, AccessTokenResponse.class);

            client.close();
        } catch (IOException e) {
            throw new AuthenticationException(e);
        }

        return token;
    }

    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(Throwable cause) {
            super(cause);
        }
    }
}
