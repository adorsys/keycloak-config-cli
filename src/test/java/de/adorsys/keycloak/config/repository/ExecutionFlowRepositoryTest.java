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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExecutionFlowRepositoryTest {
    private static final String REALM = "synthetic-realm";
    private static final String FLOW = "synthetic-flow";
    private static final String AUTHENTICATOR = "synthetic-authenticator";
    private static final String AUTHENTICATOR_CONFIG = "synthetic-config";

    private final AuthenticationFlowRepository authenticationFlowRepository = mock(AuthenticationFlowRepository.class);
    private final AuthenticationManagementResource flowsResource = mock(AuthenticationManagementResource.class);
    private final ExecutionFlowRepository repository = new ExecutionFlowRepository(authenticationFlowRepository);

    @BeforeEach
    void setUp() {
        when(authenticationFlowRepository.getFlowResources(REALM)).thenReturn(flowsResource);
    }

    @Test
    void shouldSelectNewExecutionWhenSameAuthenticatorHasExistingUnconfiguredExecution() {
        AuthenticationExecutionInfoRepresentation existingExecution = execution("existing-id");
        AuthenticationExecutionInfoRepresentation newExecution = execution("new-id");
        when(flowsResource.getExecutions(FLOW)).thenReturn(List.of(existingExecution, newExecution));

        AuthenticationExecutionExportRepresentation requestedExecution = new AuthenticationExecutionExportRepresentation();
        requestedExecution.setAuthenticator(AUTHENTICATOR);
        requestedExecution.setAuthenticatorConfig(AUTHENTICATOR_CONFIG);

        List<AuthenticationExecutionInfoRepresentation> result = repository.getNewExecutionFlowsByAlias(
                REALM, FLOW, requestedExecution, List.of(existingExecution.getId())
        );

        assertThat(result).containsExactly(newExecution);
    }

    private AuthenticationExecutionInfoRepresentation execution(String id) {
        AuthenticationExecutionInfoRepresentation execution = new AuthenticationExecutionInfoRepresentation();
        execution.setId(id);
        execution.setProviderId(AUTHENTICATOR);
        return execution;
    }
}
