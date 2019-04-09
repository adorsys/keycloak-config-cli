package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.util.ResponseUtil;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class ExecutionFlowRepository {
    private static final Logger logger = LoggerFactory.getLogger(ExecutionFlowRepository.class);

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

        throw new KeycloakRepositoryException("Cannot find stored execution-flow by alias '" + executionProviderId + "' in top-level flow '" + topLevelFlowAlias + "' in realm '" + realm + "'");
    }

    public Optional<AuthenticationExecutionInfoRepresentation> tryToGetNonTopLevelFlow(String realm, String topLevelFlowAlias, String nonTopLevelFlowAlias) {
        if(logger.isTraceEnabled()) logger.trace("Try to get non-top-level-flow '{}' from realm '{}' and top-level-flow '{}'", nonTopLevelFlowAlias, realm, topLevelFlowAlias);

        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm);

        return flowsResource.getExecutions(topLevelFlowAlias)
                .stream()
                /* we have to compare the display name with the alias, because the alias property in
                 AuthenticationExecutionInfoRepresentation representations is always set to null. */
                .filter(f -> f.getDisplayName().equals(nonTopLevelFlowAlias))
                .findFirst();
    }

    public void createExecutionFlow(String realm, String topLevelFlowAlias, Map<String, String> executionFlowData) throws WebApplicationException {
        if(logger.isTraceEnabled()) logger.trace("Create non-top-level-flow in realm '{}' and top-level-flow '{}'", realm, topLevelFlowAlias);

        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm);
        flowsResource.addExecutionFlow(topLevelFlowAlias, executionFlowData);
    }

    public void updateExecutionFlow(String realm, String flowAlias, AuthenticationExecutionInfoRepresentation executionFlowToUpdate) throws WebApplicationException {
        if(logger.isTraceEnabled()) logger.trace("Update non-top-level-flow '{}' from realm '{}' and top-level-flow '{}'", executionFlowToUpdate.getAlias(), realm, flowAlias);

        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm);
        flowsResource.updateExecutions(flowAlias, executionFlowToUpdate);
    }

    public void createTopLevelFlowExecution(String realm, AuthenticationExecutionRepresentation executionToCreate) throws KeycloakRepositoryException {
        if(logger.isTraceEnabled()) logger.trace("Create flow-execution '{}' in realm '{}' and top-level-flow '{}'...", executionToCreate.getAuthenticator(), realm, executionToCreate.getParentFlow());

        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm);

        Response response = flowsResource.addExecution(executionToCreate);
        ResponseUtil.throwOnError(response);

        if(logger.isTraceEnabled()) logger.trace("Created flow-execution '{}' in realm '{}' and top-level-flow '{}'", executionToCreate.getAuthenticator(), realm, executionToCreate.getParentFlow());
    }

    public void createNonTopLevelFlowExecution(String realm, String nonTopLevelFlowAlias, Map<String, String> executionData) throws WebApplicationException {
        if(logger.isTraceEnabled()) logger.trace("Create flow-execution in realm '{}' and non-top-level-flow '{}'...", realm, nonTopLevelFlowAlias);

        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm);
        flowsResource.addExecution(nonTopLevelFlowAlias, executionData);

        if(logger.isTraceEnabled()) logger.trace("Created flow-execution in realm '{}' and non-top-level-flow '{}'", realm, nonTopLevelFlowAlias);
    }

    private Optional<AuthenticationExecutionInfoRepresentation> tryToGetExecutionFlow(String realm, String topLevelFlowAlias, String executionProviderId) {
        AuthenticationManagementResource flowsResource = authenticationFlowRepository.getFlows(realm);

        return flowsResource.getExecutions(topLevelFlowAlias)
                .stream()
                .filter(f -> Objects.equals(f.getProviderId(), executionProviderId))
                .findFirst();
    }
}
