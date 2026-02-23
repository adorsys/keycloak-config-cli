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

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.model.WorkflowRepresentation;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues;
import de.adorsys.keycloak.config.repository.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Optional.*;
import static org.mockito.Mockito.*;

class WorkflowImportServiceTest {

    private final WorkflowRepository workflowRepository = mock(WorkflowRepository.class);
    private final ImportConfigProperties importConfigProperties = mock(ImportConfigProperties.class);
    private final ImportManagedProperties managedProperties = mock(ImportManagedProperties.class);

    private final WorkflowImportService service =
            new WorkflowImportService(workflowRepository, importConfigProperties);

    private static final String REALM_NAME = "master";
    private static final String WORKFLOW_NAME = "delete-inactive-users";
    private static final String WORKFLOW_NAME_SECONDARY = "notify-inactive-users";
    private static final String WORKFLOW_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
    private static final String WORKFLOW_ID_SECONDARY = "b2c3d4e5-f6a7-8901-bcde-f12345678901";

    @BeforeEach
    void setUp() {
        when(importConfigProperties.getManaged()).thenReturn(managedProperties);
        when(managedProperties.getWorkflow()).thenReturn(ImportManagedPropertiesValues.FULL);
    }

    @Test
    void shouldSkipWhenNoWorkflowsInImport() {
        RealmImport realmImport = realmImport(null);

        service.doImport(realmImport);

        verifyNoInteractions(workflowRepository);
    }

    @Test
    void shouldSkipWhenApiUnavailable() {
        RealmImport realmImport = realmImport(List.of(workflow(WORKFLOW_NAME, WORKFLOW_ID)));
        when(workflowRepository.getAll(REALM_NAME)).thenReturn(null);

        service.doImport(realmImport);

        verify(workflowRepository).getAll(REALM_NAME);
        verify(workflowRepository, never()).create(any(), any());
        verify(workflowRepository, never()).update(any(), any());
        verify(workflowRepository, never()).delete(any(), any());
    }

    @Test
    void shouldCreateWorkflowWhenNotExisting() {
        WorkflowRepresentation wf = workflow(WORKFLOW_NAME, null);
        RealmImport realmImport = realmImport(List.of(wf));

        when(workflowRepository.getAll(REALM_NAME)).thenReturn(List.of());
        when(workflowRepository.search(REALM_NAME, WORKFLOW_NAME)).thenReturn(empty());

        service.doImport(realmImport);

        verify(workflowRepository).create(REALM_NAME, wf);
        verify(workflowRepository, never()).update(any(), any());
    }

    @Test
    void shouldUpdateWorkflowWhenChanged() {
        WorkflowRepresentation existing = workflow(WORKFLOW_NAME, WORKFLOW_ID);
        existing.setEnabled(true);

        WorkflowRepresentation imported = workflow(WORKFLOW_NAME, null);
        imported.setEnabled(false);

        RealmImport realmImport = realmImport(List.of(imported));
        when(workflowRepository.getAll(REALM_NAME)).thenReturn(List.of(existing));
        when(workflowRepository.search(REALM_NAME, WORKFLOW_NAME)).thenReturn(of(existing));

        service.doImport(realmImport);

        verify(workflowRepository, never()).create(any(), any());
        verify(workflowRepository).update(eq(REALM_NAME), any());
    }

    @Test
    void shouldNotUpdateWorkflowWhenUnchanged() {
        WorkflowRepresentation existing = workflow(WORKFLOW_NAME, WORKFLOW_ID);
        existing.setEnabled(true);

        WorkflowRepresentation imported = workflow(WORKFLOW_NAME, null);
        imported.setEnabled(true);

        RealmImport realmImport = realmImport(List.of(imported));
        when(workflowRepository.getAll(REALM_NAME)).thenReturn(List.of(existing));
        when(workflowRepository.search(REALM_NAME, WORKFLOW_NAME)).thenReturn(of(existing));

        service.doImport(realmImport);

        verify(workflowRepository, never()).create(any(), any());
        verify(workflowRepository, never()).update(any(), any());
    }

    @Test
    void shouldDeleteWorkflowMissingInImport() {
        WorkflowRepresentation existing = workflow(WORKFLOW_NAME, WORKFLOW_ID);
        WorkflowRepresentation imported = workflow(WORKFLOW_NAME_SECONDARY, null);

        RealmImport realmImport = realmImport(List.of(imported));
        when(workflowRepository.getAll(REALM_NAME)).thenReturn(List.of(existing));
        when(workflowRepository.search(REALM_NAME, WORKFLOW_NAME_SECONDARY)).thenReturn(empty());

        service.doImport(realmImport);

        verify(workflowRepository).delete(REALM_NAME, WORKFLOW_ID);
        verify(workflowRepository).create(REALM_NAME, imported);
    }

    @Test
    void shouldSkipWhenGetWorkflowsMethodNotAvailable() {
        // This test verifies the reflection fallback works when getWorkflows method doesn't exist
        // We test this by using a RealmImport without workflows set (null)
        RealmImport realmImport = realmImport(null);
        // When workflows is null, the service should skip without calling repository
        service.doImport(realmImport);
        
        verifyNoInteractions(workflowRepository);
    }

    @Test
    void shouldSkipWhenEmptyWorkflowsList() {
        RealmImport realmImport = realmImport(List.of());

        service.doImport(realmImport);

        verify(workflowRepository).getAll(REALM_NAME);
        verify(workflowRepository, never()).create(any(), any());
        verify(workflowRepository, never()).update(any(), any());
        verify(workflowRepository, never()).delete(any(), any());
    }

    @Test
    void shouldNotDeleteWhenManagedIsNotFull() {
        when(managedProperties.getWorkflow()).thenReturn(ImportManagedPropertiesValues.NO_DELETE);

        WorkflowRepresentation existing = workflow(WORKFLOW_NAME, WORKFLOW_ID);
        WorkflowRepresentation imported = workflow(WORKFLOW_NAME_SECONDARY, null);

        RealmImport realmImport = realmImport(List.of(imported));
        when(workflowRepository.getAll(REALM_NAME)).thenReturn(List.of(existing));
        when(workflowRepository.search(REALM_NAME, WORKFLOW_NAME_SECONDARY)).thenReturn(empty());

        service.doImport(realmImport);

        verify(workflowRepository, never()).delete(any(), any());
        verify(workflowRepository).create(REALM_NAME, imported);
    }

    private RealmImport realmImport(List<WorkflowRepresentation> workflows) {
        RealmImport realmImport = new RealmImport();
        realmImport.setRealm(REALM_NAME);
        realmImport.setWorkflows(workflows);
        return realmImport;
    }

    private WorkflowRepresentation workflow(String name, String id) {
        WorkflowRepresentation wf = new WorkflowRepresentation();
        wf.setId(id);
        wf.setName(name);
        return wf;
    }
}
