package de.adorsys.keycloak.config.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RealmImport extends RealmRepresentation {

    private CustomImport customImport;
    private MultivaluedHashMap<String, ComponentImport> components;

    private List<AuthenticationFlowImport> authenticationFlowImports;

    private List<UserImport> userImports;

    public Optional<CustomImport> getCustomImport() {
        return Optional.ofNullable(customImport);
    }

    public static class CustomImport {
        @JsonProperty("removeImpersonation")
        private Boolean removeImpersonation;

        public Boolean removeImpersonation() {
            return removeImpersonation != null && removeImpersonation;
        }
    }

    @Override
    public MultivaluedHashMap<String, ComponentExportRepresentation> getComponents() {
        if(components == null) {
            return new MultivaluedHashMap<>();
        }

        return (MultivaluedHashMap) components;
    }

    @JsonSetter("components")
    public void setComponentImports(MultivaluedHashMap<String, ComponentImport> components) {
        this.components = components;
    }

    @Override
    public List<AuthenticationFlowRepresentation> getAuthenticationFlows() {
        if(authenticationFlowImports == null) {
            return Collections.EMPTY_LIST;
        }

        return (List)authenticationFlowImports;
    }

    @Override
    public List<UserRepresentation> getUsers() {
        return (List) userImports;
    }

    @JsonSetter("users")
    public void setUserImports(List<UserImport> users) {
        this.userImports = users;
    }

    @JsonSetter("authenticationFlows")
    public void setAuthenticationFlowImports(List<AuthenticationFlowImport> authenticationFlowImports) {
        this.authenticationFlowImports = authenticationFlowImports;
    }

    @Override
    public List<AuthenticatorConfigRepresentation> getAuthenticatorConfig() {
        List<AuthenticatorConfigRepresentation> authenticatorConfig = super.getAuthenticatorConfig();

        if(authenticatorConfig == null) {
            authenticatorConfig = Collections.EMPTY_LIST;
        }

        return authenticatorConfig;
    }

    @Override
    public List<RequiredActionProviderRepresentation> getRequiredActions() {
        if(requiredActions == null) {
            return Collections.EMPTY_LIST;
        }

        return requiredActions;
    }

    @JsonIgnore
    public List<AuthenticationFlowRepresentation> getTopLevelFlows() {
        return this.getAuthenticationFlows()
                .stream()
                .filter(AuthenticationFlowRepresentation::isTopLevel)
                .collect(Collectors.toList());
    }

    @JsonIgnore
    public List<AuthenticationFlowRepresentation> getNonTopLevelFlowsForTopLevelFlow(AuthenticationFlowRepresentation topLevelFlow) {
        return topLevelFlow.getAuthenticationExecutions()
                .stream()
                .filter(AbstractAuthenticationExecutionRepresentation::isAutheticatorFlow)
                .map(AuthenticationExecutionExportRepresentation::getFlowAlias)
                .map(this::getNonTopLevelFlow)
                .collect(Collectors.toList());
    }

    @JsonIgnore
    public AuthenticationFlowRepresentation getNonTopLevelFlow(String alias) {
        Optional<AuthenticationFlowRepresentation> maybeNonTopLevelFlow = tryToGetNonTopLevelFlow(alias);

        if(!maybeNonTopLevelFlow.isPresent()) {
            throw new RuntimeException("Non-toplevel flow not found: " + alias);
        }

        return maybeNonTopLevelFlow.get();
    }

    private Optional<AuthenticationFlowRepresentation> tryToGetNonTopLevelFlow(String alias) {
        return this.getNonTopLevelFlows()
                .stream()
                .filter(f -> f.getAlias().equals(alias))
                .findFirst();
    }

    private List<AuthenticationFlowRepresentation> getNonTopLevelFlows() {
        return this.getAuthenticationFlows()
                .stream()
                .filter(f -> !f.isTopLevel())
                .collect(Collectors.toList());
    }
}
