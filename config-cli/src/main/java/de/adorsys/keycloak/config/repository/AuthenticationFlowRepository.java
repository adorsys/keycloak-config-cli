package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.model.RealmImport;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@Service
public class AuthenticationFlowRepository {

    private final RealmRepository realmRepository;

    @Autowired
    public AuthenticationFlowRepository(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    public AuthenticationManagementResource getFlows(String realm) {
        RealmResource realmResource = realmRepository.loadRealm(realm);
        return realmResource.flows();
    }

    public Optional<AuthenticationFlowRepresentation> tryToGetTopLevelFlow(String realm, String alias) {
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
    public void createTopLevelFlow(RealmImport realm, AuthenticationFlowRepresentation topLevelFlowToImport) {
        AuthenticationManagementResource flowsResource = getFlows(realm.getRealm());
        Response response = flowsResource.createFlow(topLevelFlowToImport);

        if (response.getStatus() > 201) {
            throw new RuntimeException(response.getStatusInfo().getReasonPhrase());
        }
    }
}
