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

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.AbstractImportIT;
import de.adorsys.keycloak.config.model.WorkflowRepresentation;
import de.adorsys.keycloak.config.util.VersionUtil;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ImportWorkflowConcurrencyIT extends AbstractImportIT {
    private static final String REALM = "workflow-concurrency";

    ImportWorkflowConcurrencyIT() {
        resourcePath = "import-files/workflow-concurrency";
    }

    @Test
    void shouldRestartAndCancelImportedWorkflowsOnLogin() throws IOException {
        assumeTrue(VersionUtil.ge(KEYCLOAK_VERSION, "26.6"));
        doImport("00_create_realm.json");

        UserRepresentation user = new UserRepresentation();
        user.setUsername("workflow-user");
        user.setFirstName("Workflow");
        user.setLastName("User");
        user.setEmail("workflow-user@example.test");
        user.setEmailVerified(true);
        user.setEnabled(true);
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue("workflow-password");
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));

        String userId;
        try (Response response = keycloakProvider.getInstance().realm(REALM).users().create(user)) {
            userId = CreatedResponseUtil.getCreatedId(response);
        }
        ScheduledWorkflowsResource resource = keycloakProvider.getCustomApiProxy(ScheduledWorkflowsResource.class);
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(resource.getScheduled(REALM, userId)).hasSize(4));
        Map<String, WorkflowRepresentation> before = scheduledByName(resource, userId);
        assertThat(before.get("legacy-root-restart").getConcurrency().getRestartInProgress()).isEqualTo("user-authenticated");
        assertThat(before.get("canonical-root-restart").getConcurrency().getRestartInProgress()).isEqualTo("user-authenticated");
        assertThat(before.get("nested-restart").getConcurrency().getRestartInProgress()).isEqualTo("user-authenticated");
        assertThat(before.get("root-cancel").getConcurrency().getCancelInProgress()).isEqualTo("user-authenticated");

        keycloakAuthentication.login(REALM, "workflow-login", null, "workflow-user", "workflow-password");

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Map<String, WorkflowRepresentation> after = scheduledByName(resource, userId);
            assertThat(after).hasSize(3).doesNotContainKey("root-cancel");
            for (String name : List.of("legacy-root-restart", "canonical-root-restart", "nested-restart")) {
                assertThat(after.get(name).getSteps().get(0).getScheduledAt())
                        .isGreaterThan(before.get(name).getSteps().get(0).getScheduledAt());
            }
        });
    }

    private Map<String, WorkflowRepresentation> scheduledByName(ScheduledWorkflowsResource resource, String userId) {
        return resource.getScheduled(REALM, userId).stream()
                .collect(Collectors.toMap(WorkflowRepresentation::getName, Function.identity()));
    }

    public interface ScheduledWorkflowsResource {
        @GET
        @Path("/admin/realms/{realm}/workflows/scheduled/{userId}")
        @Produces(MediaType.APPLICATION_JSON)
        List<WorkflowRepresentation> getScheduled(@PathParam("realm") String realm, @PathParam("userId") String userId);
    }
}
