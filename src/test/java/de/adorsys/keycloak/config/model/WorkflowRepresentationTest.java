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

package de.adorsys.keycloak.config.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowRepresentationTest {
    private final ObjectMapper importMapper = new ObjectMapper();
    private final ObjectMapper requestMapper = new KeycloakProvider.JacksonProvider()
            .locateMapper(WorkflowRepresentation.class, MediaType.APPLICATION_JSON_TYPE);

    @ParameterizedTest
    @CsvSource({
            "restartInProgress, cancelInProgress, user-authenticated, true",
            "restart-in-progress, cancel-in-progress, user-authenticated, true",
            "restartInProgress, cancelInProgress, true, false",
            "restart-in-progress, cancel-in-progress, true, false"
    })
    void shouldImportRootConcurrencyAndSendCanonicalKeys(String restartKey, String cancelKey,
            String restartValue, String cancelValue) throws Exception {
        String json = """
                {"realm":"test", "workflows":[{
                  "%s":"%s", "%s":"%s"
                }]}
                """.formatted(restartKey, restartValue, cancelKey, cancelValue);
        WorkflowRepresentation workflow = importMapper.readValue(json, RealmImport.class).getWorkflows().get(0);

        JsonNode request = requestMapper.valueToTree(workflow);

        assertThat(workflow.getRestartInProgress()).isEqualTo(restartValue);
        assertThat(workflow.getCancelInProgress()).isEqualTo(cancelValue);
        assertThat(request.path("restart-in-progress").asText()).isEqualTo(restartValue);
        assertThat(request.path("cancel-in-progress").asText()).isEqualTo(cancelValue);
        assertThat(request.has("restartInProgress")).isFalse();
        assertThat(request.has("cancelInProgress")).isFalse();
    }

    @Test
    void shouldNotSendUnsetRootConcurrencyAlongsideNestedConfiguration() throws Exception {
        WorkflowRepresentation workflow = importMapper.readValue("""
                {"concurrency":{"restart-in-progress":"user-authenticated", "cancel-in-progress":"false"}}
                """, WorkflowRepresentation.class);

        JsonNode request = requestMapper.valueToTree(workflow);

        assertThat(request.path("concurrency").path("restart-in-progress").asText()).isEqualTo("user-authenticated");
        assertThat(request.path("concurrency").path("cancel-in-progress").asText()).isEqualTo("false");
        assertNoRootConcurrency(request);
    }

    @ParameterizedTest
    @CsvSource({
            "restartInProgress, cancelInProgress",
            "restart-in-progress, cancel-in-progress"
    })
    void shouldOmitExplicitlyNullRootConcurrency(String restartKey, String cancelKey) throws Exception {
        WorkflowRepresentation workflow = importMapper.readValue(
                "{\"%s\":null,\"%s\":null}".formatted(restartKey, cancelKey), WorkflowRepresentation.class);

        assertNoRootConcurrency(requestMapper.valueToTree(workflow));
    }

    @Test
    void shouldOmitAbsentRootConcurrency() {
        assertNoRootConcurrency(requestMapper.valueToTree(new WorkflowRepresentation()));
    }

    @Test
    void shouldReadSingleValueWorkflowStepParameter() throws Exception {
        WorkflowRepresentation workflow = importMapper.readValue("""
                {"name":"workflow","steps":[{"uses":"do-something","with":{"message":"We are here"}}]}
                """, WorkflowRepresentation.class);

        assertThat(workflow.getSteps().get(0).getWith().get("message")).containsExactly("We are here");
    }

    @Test
    void shouldReadArrayWorkflowStepParameter() throws Exception {
        WorkflowRepresentation workflow = importMapper.readValue("""
                {"steps":[{"uses":"do-something","with":{"message":["first","second"]}}]}
                """, WorkflowRepresentation.class);

        assertThat(workflow.getSteps().get(0).getWith().get("message")).containsExactly("first", "second");
    }

    private void assertNoRootConcurrency(JsonNode request) {
        assertThat(request.has("restart-in-progress")).isFalse();
        assertThat(request.has("cancel-in-progress")).isFalse();
        assertThat(request.has("restartInProgress")).isFalse();
        assertThat(request.has("cancelInProgress")).isFalse();
    }
}
