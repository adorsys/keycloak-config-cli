package de.adorsys.keycloak.config.repository;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Provides methods to retrieve and store required-actions in your realm
 */
@Service
public class RequiredActionRepository {

    private final AuthenticationFlowRepository authenticationFlowRepository;

    @Autowired
    public RequiredActionRepository(AuthenticationFlowRepository authenticationFlowRepository) {
        this.authenticationFlowRepository = authenticationFlowRepository;
    }

    public List<RequiredActionProviderRepresentation> getRequiredActions(String realm) {
        AuthenticationManagementResource flows = authenticationFlowRepository.getFlows(realm);

        return flows.getRequiredActions();
    }

    public Optional<RequiredActionProviderRepresentation> tryToGetRequiredAction(String realm, String requiredActionAlias) {
        List<RequiredActionProviderRepresentation> requiredActions = getRequiredActions(realm);
        return requiredActions.stream()
                .filter(r -> r.getAlias().equals(requiredActionAlias))
                .map(this::enrichWithProviderId)
                .findFirst();
    }

    private RequiredActionProviderRepresentation enrichWithProviderId(RequiredActionProviderRepresentation r) {
        // keycloak is NOT mapping the field 'providerId' into required-action representations, so we have to enrich
        // the required-action; the provider-id has always the same value like alias
        r.setProviderId(r.getAlias());
        return r;
    }

    public RequiredActionProviderRepresentation getRequiredAction(String realm, String requiredActionAlias) {
        Optional<RequiredActionProviderRepresentation> maybeRequiredAction = tryToGetRequiredAction(realm, requiredActionAlias);

        if(maybeRequiredAction.isPresent()) {
            return maybeRequiredAction.get();
        }

        throw new RuntimeException("Cannot get required action: " + requiredActionAlias);
    }

    public void createRequiredAction(String realm, RequiredActionProviderSimpleRepresentation requiredActionToCreate) {
        AuthenticationManagementResource flows = authenticationFlowRepository.getFlows(realm);
        flows.registerRequiredAction(requiredActionToCreate);
    }

    public void updateRequiredAction(String realm, RequiredActionProviderRepresentation requiredActionToCreate) {
        AuthenticationManagementResource flows = authenticationFlowRepository.getFlows(realm);
        flows.updateRequiredAction(requiredActionToCreate.getAlias(), requiredActionToCreate);
    }
}
