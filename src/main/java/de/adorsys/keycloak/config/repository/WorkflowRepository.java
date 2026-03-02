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

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.model.WorkflowRepresentation;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.resource.WorkflowsResource;
import de.adorsys.keycloak.config.util.ResponseUtil;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class WorkflowRepository {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowRepository.class);

    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_NOT_IMPLEMENTED = 501;

    private final KeycloakProvider keycloakProvider;

    @Autowired
    public WorkflowRepository(KeycloakProvider keycloakProvider) {
        this.keycloakProvider = keycloakProvider;
    }

    /**
     * Returns all workflows for the realm, or {@code null} if the Workflows feature
     * is not available on this Keycloak instance (API returns 404/501).
     */
    public List<WorkflowRepresentation> getAll(String realmName) {
        WorkflowsResource resource = keycloakProvider.getCustomApiProxy(WorkflowsResource.class);
        try {
            return resource.listWorkflows(realmName, null, null, null, null);
        } catch (WebApplicationException e) {
            int status = e.getResponse().getStatus();
            if (status == HTTP_NOT_FOUND || status == HTTP_NOT_IMPLEMENTED) {
                logger.debug("Workflows API not available (HTTP {}), skipping", status);
                return null;
            }
            throw e;
        }
    }

    public Optional<WorkflowRepresentation> search(String realmName, String workflowName) {
        WorkflowsResource resource = keycloakProvider.getCustomApiProxy(WorkflowsResource.class);
        List<WorkflowRepresentation> results = resource.listWorkflows(realmName, workflowName, null, null, true);
        return results == null ? Optional.empty() : results.stream()
                .filter(w -> workflowName.equals(w.getName()))
                .findFirst();
    }

    public WorkflowRepresentation getById(String realmName, String workflowId) {
        WorkflowsResource resource = keycloakProvider.getCustomApiProxy(WorkflowsResource.class);
        try {
            return resource.getWorkflow(realmName, workflowId, true);
        } catch (NotFoundException e) {
            return null;
        }
    }

    public void create(String realmName, WorkflowRepresentation workflow) {
        WorkflowsResource resource = keycloakProvider.getCustomApiProxy(WorkflowsResource.class);
        try (Response response = resource.createWorkflow(realmName, workflow)) {
            CreatedResponseUtil.getCreatedId(response);
        } catch (WebApplicationException error) {
            String errorMessage = String.format(
                    "Cannot create workflow '%s' in realm '%s': %s",
                    workflow.getName(), realmName, ResponseUtil.getErrorMessage(error)
            );
            throw new ImportProcessingException(errorMessage, error);
        }
    }

    public void update(String realmName, WorkflowRepresentation workflow) {
        WorkflowsResource resource = keycloakProvider.getCustomApiProxy(WorkflowsResource.class);
        resource.updateWorkflow(realmName, workflow.getId(), workflow);
    }

    public void delete(String realmName, String workflowId) {
        WorkflowsResource resource = keycloakProvider.getCustomApiProxy(WorkflowsResource.class);
        resource.deleteWorkflow(realmName, workflowId);
    }
}
