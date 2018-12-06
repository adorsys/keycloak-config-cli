package de.adorsys.keycloak.config.model;

import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;

import java.util.Comparator;
import java.util.List;

/**
 * Represents a {@link AuthenticationFlowRepresentation} with authenticationExecutions which are sorted by its priority
 */
public class AuthenticationFlowImport extends AuthenticationFlowRepresentation {
    private static final Comparator<AuthenticationExecutionExportRepresentation> COMPARATOR = new AuthenticationExecutionExportRepresentationComparator();

    @Override
    public List<AuthenticationExecutionExportRepresentation> getAuthenticationExecutions() {
        return authenticationExecutions;
    }

    @Override
    public void setAuthenticationExecutions(List<AuthenticationExecutionExportRepresentation> authenticationExecutions) {
        authenticationExecutions.sort(COMPARATOR);
        super.setAuthenticationExecutions(authenticationExecutions);
    }

    /**
     * Comparator to sort {@link AuthenticationExecutionExportRepresentation} objects by its priority
     */
    private static class AuthenticationExecutionExportRepresentationComparator implements Comparator<AuthenticationExecutionExportRepresentation> {

        @Override
        public int compare(
                AuthenticationExecutionExportRepresentation first,
                AuthenticationExecutionExportRepresentation second
        ) {
            return first.getPriority() - second.getPriority();
        }
    }
}
