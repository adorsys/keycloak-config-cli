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

    private List<AuthenticationFlowImport> authenticationFlowImports;

    private List<UserImport> userImports;

    private RolesImport rolesImport = new RolesImport();

    private String checksum;

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

    /**
     * Override getter to make sure we never get null from import
     */
    @Override
    public MultivaluedHashMap<String, ComponentExportRepresentation> getComponents() {
        MultivaluedHashMap<String, ComponentExportRepresentation> components = super.getComponents();
        if(components == null) {
            return new MultivaluedHashMap<>();
        }

        return components;
    }

    @Override
    public List<AuthenticationFlowRepresentation> getAuthenticationFlows() {
        if(authenticationFlowImports == null) {
            return Collections.emptyList();
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
            authenticatorConfig = Collections.emptyList();
        }

        return authenticatorConfig;
    }

    @Override
    public List<RequiredActionProviderRepresentation> getRequiredActions() {
        if(requiredActions == null) {
            return Collections.emptyList();
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

    @Override
    public RolesRepresentation getRoles() {
        return rolesImport;
    }

    @JsonSetter("roles")
    public void setRolesImport(RolesImport rolesImport) {
        this.rolesImport = rolesImport;
    }

    @Override
    public List<ClientRepresentation> getClients() {
        List<ClientRepresentation> clients = super.getClients();

        if(clients == null) {
            return Collections.emptyList();
        }

        return clients;
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

    @JsonIgnore
    public String getChecksum() {
        return checksum;
    }

    @JsonIgnore
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
}
