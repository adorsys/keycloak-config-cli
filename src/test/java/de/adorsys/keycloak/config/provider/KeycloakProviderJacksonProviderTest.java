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

package de.adorsys.keycloak.config.provider;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import jakarta.ws.rs.core.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

class KeycloakProviderJacksonProviderTest {

    @Test
    void shouldKeepNullFlowBindingEntriesForClientDeletion() throws Exception {
        ObjectMapper mapper = new KeycloakProvider.JacksonProvider()
                .locateMapper(ClientRepresentation.class, MediaType.APPLICATION_JSON_TYPE);
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("test");
        Map<String, String> bindings = new HashMap<>();
        bindings.put("browser", null);
        bindings.put("direct_grant", "flow-id");
        client.setAuthenticationFlowBindingOverrides(bindings);

        JsonNode request = mapper.valueToTree(client);
        assertThat(request).isEqualTo(mapper.readTree("""
                {"clientId":"test","authenticationFlowBindingOverrides":{"browser":null,"direct_grant":"flow-id"}}
                """));
    }

    @Test
    void shouldOmitUnsetRealmFieldsForOlderServers() throws Exception {
        ObjectMapper mapper = new KeycloakProvider.JacksonProvider()
                .locateMapper(RealmRepresentation.class, MediaType.APPLICATION_JSON_TYPE);
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm("test");
        realm.setEnabled(true);

        JsonNode request = mapper.valueToTree(realm);
        assertThat(request).isEqualTo(mapper.readTree("""
                {"realm":"test","enabled":true}
                """));
    }

    @Test
    void shouldOmitUnsetUserFieldsButKeepExplicitEmptyValues() throws Exception {
        ObjectMapper mapper = new KeycloakProvider.JacksonProvider()
                .locateMapper(UserRepresentation.class, MediaType.APPLICATION_JSON_TYPE);
        UserRepresentation user = new UserRepresentation();
        user.setUsername("test");
        user.setEnabled(false);
        user.setAttributes(Map.of());
        user.setRequiredActions(List.of());

        JsonNode request = mapper.valueToTree(user);
        assertThat(request).isEqualTo(mapper.readTree("""
                {"username":"test","enabled":false,"attributes":{},"requiredActions":[]}
                """));
    }

    @Test
    void shouldDisableFailOnUnknownProperties() {
        KeycloakProvider.JacksonProvider provider = new KeycloakProvider.JacksonProvider();

        ObjectMapper mapper = provider.locateMapper(Object.class, MediaType.APPLICATION_JSON_TYPE);

        assertFalse(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }
}
