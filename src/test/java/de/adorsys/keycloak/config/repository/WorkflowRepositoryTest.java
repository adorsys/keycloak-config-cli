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

package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.model.WorkflowRepresentation;
import de.adorsys.keycloak.config.model.WorkflowRepresentation.WorkflowConcurrencyRepresentation;
import de.adorsys.keycloak.config.model.WorkflowRepresentation.WorkflowScheduleRepresentation;
import de.adorsys.keycloak.config.model.WorkflowRepresentation.WorkflowStateRepresentation;
import de.adorsys.keycloak.config.model.WorkflowRepresentation.WorkflowStepRepresentation;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.resource.WorkflowsResource;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class WorkflowRepositoryTest {

    private final KeycloakProvider keycloakProvider = mock(KeycloakProvider.class);
    private final WorkflowsResource workflowsResource = mock(WorkflowsResource.class);
    private final WorkflowRepository repository = new WorkflowRepository(keycloakProvider);

    private static final String REALM_NAME = "master";
    private static final String WORKFLOW_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
    private static final String WORKFLOW_NAME = "delete-inactive-users";

    @BeforeEach
    void setUp() {
        when(keycloakProvider.getCustomApiProxy(WorkflowsResource.class)).thenReturn(workflowsResource);
    }

    @Test
    void getAllShouldReturnWorkflows() {
        WorkflowRepresentation wf = workflow(WORKFLOW_NAME, WORKFLOW_ID);
        when(workflowsResource.listWorkflows(REALM_NAME, null, null, null, null)).thenReturn(List.of(wf));

        List<WorkflowRepresentation> result = repository.getAll(REALM_NAME);

        assertThat(result).hasSize(1).first().extracting(WorkflowRepresentation::getName).isEqualTo(WORKFLOW_NAME);
    }

    @Test
    void getAllShouldReturnNullWhenApiNotFound() {
        WebApplicationException notFound = webException(404);
        when(workflowsResource.listWorkflows(REALM_NAME, null, null, null, null)).thenThrow(notFound);

        assertThat(repository.getAll(REALM_NAME)).isNull();
    }

    @Test
    void getAllShouldReturnNullWhenApiNotImplemented() {
        WebApplicationException notImplemented = webException(501);
        when(workflowsResource.listWorkflows(REALM_NAME, null, null, null, null)).thenThrow(notImplemented);

        assertThat(repository.getAll(REALM_NAME)).isNull();
    }

    @Test
    void getAllShouldRethrowOnOtherErrors() {
        WebApplicationException serverError = webException(500);
        when(workflowsResource.listWorkflows(REALM_NAME, null, null, null, null)).thenThrow(serverError);

        assertThatThrownBy(() -> repository.getAll(REALM_NAME)).isInstanceOf(WebApplicationException.class);
    }

    @Test
    void searchShouldReturnMatchingWorkflow() {
        WorkflowRepresentation wf = workflow(WORKFLOW_NAME, WORKFLOW_ID);
        when(workflowsResource.listWorkflows(REALM_NAME, WORKFLOW_NAME, null, null, true)).thenReturn(List.of(wf));

        Optional<WorkflowRepresentation> result = repository.search(REALM_NAME, WORKFLOW_NAME);

        assertThat(result).isPresent().get().extracting(WorkflowRepresentation::getId).isEqualTo(WORKFLOW_ID);
    }

    @Test
    void searchShouldReturnEmptyWhenNullResponse() {
        when(workflowsResource.listWorkflows(REALM_NAME, WORKFLOW_NAME, null, null, true)).thenReturn(null);

        assertThat(repository.search(REALM_NAME, WORKFLOW_NAME)).isEmpty();
    }

    @Test
    void searchShouldReturnEmptyWhenNameNotMatched() {
        when(workflowsResource.listWorkflows(REALM_NAME, WORKFLOW_NAME, null, null, true))
                .thenReturn(List.of(workflow("other-workflow", WORKFLOW_ID)));

        assertThat(repository.search(REALM_NAME, WORKFLOW_NAME)).isEmpty();
    }

    @Test
    void getByIdShouldReturnWorkflow() {
        WorkflowRepresentation wf = workflow(WORKFLOW_NAME, WORKFLOW_ID);
        when(workflowsResource.getWorkflow(REALM_NAME, WORKFLOW_ID, true)).thenReturn(wf);

        assertThat(repository.getById(REALM_NAME, WORKFLOW_ID)).isEqualTo(wf);
    }

    @Test
    void getByIdShouldReturnNullWhenNotFound() {
        when(workflowsResource.getWorkflow(REALM_NAME, WORKFLOW_ID, true)).thenThrow(new NotFoundException());

        assertThat(repository.getById(REALM_NAME, WORKFLOW_ID)).isNull();
    }

    @Test
    void updateShouldDelegateToResource() {
        WorkflowRepresentation wf = workflow(WORKFLOW_NAME, WORKFLOW_ID);

        repository.update(REALM_NAME, wf);

        verify(workflowsResource).updateWorkflow(REALM_NAME, WORKFLOW_ID, wf);
    }

    @Test
    void deleteShouldDelegateToResource() {
        repository.delete(REALM_NAME, WORKFLOW_ID);

        verify(workflowsResource).deleteWorkflow(REALM_NAME, WORKFLOW_ID);
    }

    @Test
    void workflowRepresentationRoundTrip() {
        WorkflowRepresentation wf = new WorkflowRepresentation();
        wf.setOn("user-authenticated");
        wf.setCondition("has-user-attribute(type:user)");
        wf.setWith(Map.of("param", List.of("value")));
        wf.setCancelInProgress("false");
        wf.setRestartInProgress("true");

        WorkflowScheduleRepresentation schedule = new WorkflowScheduleRepresentation();
        wf.setSchedule(schedule);
        WorkflowConcurrencyRepresentation concurrency = new WorkflowConcurrencyRepresentation();
        wf.setConcurrency(concurrency);
        WorkflowStepRepresentation step = new WorkflowStepRepresentation();
        wf.setSteps(List.of(step));
        WorkflowStateRepresentation state = new WorkflowStateRepresentation();
        wf.setState(state);

        assertThat(wf.getOn()).isEqualTo("user-authenticated");
        assertThat(wf.getCondition()).isEqualTo("has-user-attribute(type:user)");
        assertThat(wf.getWith()).containsKey("param");
        assertThat(wf.getCancelInProgress()).isEqualTo("false");
        assertThat(wf.getRestartInProgress()).isEqualTo("true");
        assertThat(wf.getSchedule()).isEqualTo(schedule);
        assertThat(wf.getConcurrency()).isEqualTo(concurrency);
        assertThat(wf.getSteps()).containsExactly(step);
        assertThat(wf.getState()).isEqualTo(state);
    }

    @Test
    void workflowRepresentationNestedClassesRoundTrip() {
        WorkflowScheduleRepresentation schedule = new WorkflowScheduleRepresentation();
        schedule.setAfter("5y");
        schedule.setBatchSize(50);
        assertThat(schedule.getAfter()).isEqualTo("5y");
        assertThat(schedule.getBatchSize()).isEqualTo(50);

        WorkflowConcurrencyRepresentation concurrency = new WorkflowConcurrencyRepresentation();
        concurrency.setCancelInProgress("true");
        concurrency.setRestartInProgress("false");
        assertThat(concurrency.getCancelInProgress()).isEqualTo("true");
        assertThat(concurrency.getRestartInProgress()).isEqualTo("false");

        WorkflowStepRepresentation step = new WorkflowStepRepresentation();
        step.setId("step-1");
        step.setUses("delete-user");
        step.setAfter("1y");
        step.setScheduledAt(1234567890L);
        step.setConfig(Map.of("key", List.of("value")));
        assertThat(step.getId()).isEqualTo("step-1");
        assertThat(step.getUses()).isEqualTo("delete-user");
        assertThat(step.getAfter()).isEqualTo("1y");
        assertThat(step.getScheduledAt()).isEqualTo(1234567890L);
        assertThat(step.getConfig()).containsKey("key");

        WorkflowStateRepresentation state = new WorkflowStateRepresentation();
        state.setErrors(List.of("timeout-error"));
        assertThat(state.getErrors()).containsExactly("timeout-error");
    }

    private WorkflowRepresentation workflow(String name, String id) {
        WorkflowRepresentation wf = new WorkflowRepresentation();
        wf.setId(id);
        wf.setName(name);
        return wf;
    }

    private WebApplicationException webException(int status) {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status);
        WebApplicationException exception = mock(WebApplicationException.class);
        when(exception.getResponse()).thenReturn(response);
        return exception;
    }
}
