package de.adorsys.keycloak.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RealmImport extends RealmRepresentation {

    private CustomImport customImport;
    private MultivaluedHashMap<String, ComponentImport> components;

    private List<AuthenticationFlowImport> authenticationFlowImports;

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
        return (MultivaluedHashMap) components;
    }

    @JsonSetter("components")
    public void setComponentImports(MultivaluedHashMap<String, ComponentImport> components) {
        this.components = components;
    }

    @Override
    public List<AuthenticationFlowRepresentation> getAuthenticationFlows() {
        return (List)authenticationFlowImports;
    }

    @JsonSetter("authenticationFlows")
    public void setAuthenticationFlowImports(List<AuthenticationFlowImport> authenticationFlowImports) {
        this.authenticationFlowImports = authenticationFlowImports;
    }

    @Override
    public List<AuthenticatorConfigRepresentation> getAuthenticatorConfig() {
        List<AuthenticatorConfigRepresentation> authenticatorConfig = super.getAuthenticatorConfig();

        if(authenticatorConfig == null) {
            authenticatorConfig = new ArrayList<>();
        }

        return authenticatorConfig;
    }

    public List<AuthenticationFlowRepresentation> getTopLevelFlows() {
        return this.getAuthenticationFlows()
                .stream()
                .filter(AuthenticationFlowRepresentation::isTopLevel)
                .collect(Collectors.toList());
    }

    public List<AuthenticationFlowRepresentation> getNonTopLevelFlowsForTopLevelFlow(AuthenticationFlowRepresentation topLevelFlow) {
        return topLevelFlow.getAuthenticationExecutions()
                .stream()
                .filter(AbstractAuthenticationExecutionRepresentation::isAutheticatorFlow)
                .map(AuthenticationExecutionExportRepresentation::getFlowAlias)
                .map(this::getNonTopLevelFlow)
                .collect(Collectors.toList());
    }

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
