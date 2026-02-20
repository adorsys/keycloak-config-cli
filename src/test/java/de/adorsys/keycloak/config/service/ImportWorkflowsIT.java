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
import de.adorsys.keycloak.config.repository.WorkflowRepository;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SuppressWarnings("java:S5961")
@TestPropertySource(properties = {
        "import.managed.workflow=full"
})
class ImportWorkflowsIT extends AbstractImportIT {
    private static final String REALM_NAME = "realmWithWorkflows";

    @Autowired
    private WorkflowRepository workflowRepository;

    ImportWorkflowsIT() {
        this.resourcePath = "import-files/workflows";
    }

    @Test
    @Order(0)
    void shouldCreateRealmWithWorkflow() throws IOException {
        doImport("00_create_realm_with_workflow.json");

        RealmRepresentation createdRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();
        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        List<WorkflowRepresentation> workflows = workflowRepository.getAll(REALM_NAME);
        assumeTrue(workflows != null, "Workflows API not available on this Keycloak version");

        assertThat(workflows, hasSize(1));

        WorkflowRepresentation workflow = workflows.get(0);
        assertThat(workflow.getName(), is("Delete inactive users"));
        assertThat(workflow.getEnabled(), is(true));
        assertThat(workflow.getOn(), is("user-authenticated"));
        assertThat(workflow.getCondition(), is("has-user-attribute(type:user)"));
        assertThat(workflow.getSchedule().getAfter(), is("5y"));
        assertThat(workflow.getSchedule().getBatchSize(), is(50));
        assertThat(workflow.getConcurrency().getRestartInProgress(), is("true"));
        assertThat(workflow.getSteps(), hasSize(1));
        assertThat(workflow.getSteps().get(0).getUses(), is("delete-user"));
        assertThat(workflow.getSteps().get(0).getAfter(), is("5y"));
    }

    @Test
    @Order(1)
    void shouldNotUpdateUnchangedWorkflow() throws IOException {
        doImport("00_create_realm_with_workflow.json");

        List<WorkflowRepresentation> workflows = workflowRepository.getAll(REALM_NAME);
        assumeTrue(workflows != null, "Workflows API not available on this Keycloak version");

        assertThat(workflows, hasSize(1));
        assertThat(workflows.get(0).getName(), is("Delete inactive users"));
        assertThat(workflows.get(0).getEnabled(), is(true));
    }

    @Test
    @Order(2)
    void shouldUpdateWorkflow() throws IOException {
        doImport("01_update_workflow.json");

        List<WorkflowRepresentation> workflows = workflowRepository.getAll(REALM_NAME);
        assumeTrue(workflows != null, "Workflows API not available on this Keycloak version");

        assertThat(workflows, hasSize(1));

        WorkflowRepresentation workflow = workflows.get(0);
        assertThat(workflow.getName(), is("Delete inactive users"));
        assertThat(workflow.getEnabled(), is(false));
        assertThat(workflow.getSchedule().getAfter(), is("1y"));
        assertThat(workflow.getSchedule().getBatchSize(), is(100));
    }

    @Test
    @Order(3)
    void shouldDeleteRemovedWorkflow() throws IOException {
        doImport("02_delete_workflow.json");

        List<WorkflowRepresentation> workflows = workflowRepository.getAll(REALM_NAME);
        assumeTrue(workflows != null, "Workflows API not available on this Keycloak version");

        assertThat(workflows, hasSize(0));
    }
}
