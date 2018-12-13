package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.util.ResponseUtil;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@Service
public class AuthenticationFlowRepository {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFlowRepository.class);

    private final RealmRepository realmRepository;

    @Autowired
    public AuthenticationFlowRepository(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    public AuthenticationManagementResource getFlows(String realm) {
        if(logger.isTraceEnabled()) logger.trace("Get flows-resource for realm '{}'...", realm);

        RealmResource realmResource = realmRepository.loadRealm(realm);
        AuthenticationManagementResource flows = realmResource.flows();

        if(logger.isTraceEnabled()) logger.trace("Got flows-resource for realm '{}'", realm);

        return flows;
    }

    public Optional<AuthenticationFlowRepresentation> tryToGetTopLevelFlow(String realm, String alias) {
        if(logger.isTraceEnabled()) logger.trace("Try to get top-level-flow '{}' from realm '{}'", alias, realm);

        AuthenticationManagementResource flowsResource = getFlows(realm);

        // keycloak is returning here only so-called toplevel-flows
        List<AuthenticationFlowRepresentation> existingTopLevelFlows = flowsResource.getFlows();

        return existingTopLevelFlows.stream()
                .filter(f -> f.getAlias().equals(alias))
                .findFirst();
    }

    public AuthenticationFlowRepresentation getTopLevelFlow(String realm, String alias) {
        Optional<AuthenticationFlowRepresentation> maybeTopLevelFlow = tryToGetTopLevelFlow(realm, alias);

        if(maybeTopLevelFlow.isPresent()) {
            return maybeTopLevelFlow.get();
        }

        throw new RuntimeException("Cannot find top-level flow: " + alias);
    }

    /**
     * creates only the top-level flow WITHOUT its executions or execution-flows
     */
    public void createTopLevelFlow(String realm, AuthenticationFlowRepresentation topLevelFlowToImport) {
        if(logger.isTraceEnabled()) logger.trace("Create top-level-flow '{}' in realm '{}'", topLevelFlowToImport.getAlias(), realm);

        AuthenticationManagementResource flowsResource = getFlows(realm);
        Response response = flowsResource.createFlow(topLevelFlowToImport);

        ResponseUtil.throwOnError(response);
    }

    public AuthenticationFlowRepresentation getFlowById(String realm, String id) {
        if(logger.isTraceEnabled()) logger.trace("Get flow by id '{}' in realm '{}'", id, realm);

        AuthenticationManagementResource flowsResource = getFlows(realm);
        return flowsResource.getFlow(id);
    }
}
