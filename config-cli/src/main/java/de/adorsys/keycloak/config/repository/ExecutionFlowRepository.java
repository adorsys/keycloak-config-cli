package de.adorsys.keycloak.config.repository;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Optional;

@Service
public class ExecutionFlowRepository {

    private final AuthenticationFlowRepository authenticationFlowRepository;

    @Autowired
    public ExecutionFlowRepository(AuthenticationFlowRepository authenticationFlowRepository) {
        this.authenticationFlowRepository = authenticationFlowRepository;
    }

    public AuthenticationExecutionInfoRepresentation getExecutionFlow(String realm, String topLevelFlowAlias, String executionProviderId) {
        Optional<AuthenticationExecutionInfoRepresentation> maybeExecution = tryToGetExecutionFlow(realm, topLevelFlowAlias, executionProviderId);

        if (maybeExecution.isPresent()) {
            return maybeExecution.get();
        }

        throw new RuntimeException("Cannot find stored execution-flow by alias: " + executionProviderId + " in top-level flow: " + topLevelFlowAlias);
    }

    public void createExecutionFlow(String realm, String topLevelFlowAlias, Map<String, String> executionFlowData) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm);
        flowsResource.addExecutionFlow(topLevelFlowAlias, executionFlowData);

    }

    public void updateExecutionFlow(String realm, String flowAlias, AuthenticationExecutionInfoRepresentation executionFlowToUpdate) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm);
        flowsResource.updateExecutions(flowAlias, executionFlowToUpdate);
    }

    public void createExecution(String realm, AuthenticationExecutionRepresentation executionToCreate) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm);

        Response response = flowsResource.addExecution(executionToCreate);
        if (response.getStatus() > 201) {
            throw new RuntimeException(response.getStatusInfo().getReasonPhrase());
        }
    }

    public void createExecution(String realm, String nonTopLevelFlowAlias, Map<String, String> executionData) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm);
        flowsResource.addExecution(nonTopLevelFlowAlias, executionData);
    }

    private Optional<AuthenticationExecutionInfoRepresentation> tryToGetExecutionFlow(String realm, String topLevelFlowAlias, String executionProviderId) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm);

        return flowsResource.getExecutions(topLevelFlowAlias)
                       .stream()
                       .filter(f -> f.getProviderId().equals(executionProviderId))
                       .findFirst();
    }
}
