package de.adorsys.keycloak.config.service.normalize;

import de.adorsys.keycloak.config.normalize.AbstractNormalizeTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class AuthFlowNormalizationServiceConfigIT extends AbstractNormalizeTest {

    @Autowired
    AuthFlowNormalizationService service;

    @Test
    public void testNormalizeAuthConfigsWithEmptyListsIsNull() {
        var resultingAuthConfig = service.normalizeAuthConfig(new ArrayList<>(), new ArrayList<>());
        Assertions.assertThat(resultingAuthConfig).isNull();
    }

    @Test
    public void testNormalizeAuthConfigsAreRemovedWithoutAliasReference(CapturedOutput output) {
        AuthenticatorConfigRepresentation authenticatorConfigRepresentation = new AuthenticatorConfigRepresentation();
        authenticatorConfigRepresentation.setAlias("config2");

        AuthenticationFlowRepresentation authenticationFlowRepresentation = new AuthenticationFlowRepresentation();

        AuthenticationExecutionExportRepresentation authenticationExecutionExportRepresentation = new AuthenticationExecutionExportRepresentation();
        authenticationExecutionExportRepresentation.setAuthenticatorConfig("config1");
        authenticationFlowRepresentation.setAuthenticationExecutions(List.of(authenticationExecutionExportRepresentation));

        var resultingAuthConfig = service.normalizeAuthConfig(List.of(authenticatorConfigRepresentation), List.of(authenticationFlowRepresentation));

        Assertions.assertThat(resultingAuthConfig).isNull();
        Assertions.assertThat(output.getOut()).contains("Some authenticator configs are unused.");
    }

    @Test
    public void testNormalizeAuthConfigsRemainWithAliasReference() {
        AuthenticatorConfigRepresentation authenticatorConfigRepresentation = new AuthenticatorConfigRepresentation();
        authenticatorConfigRepresentation.setAlias("config1");

        AuthenticationFlowRepresentation authenticationFlowRepresentation = new AuthenticationFlowRepresentation();

        AuthenticationExecutionExportRepresentation authenticationExecutionExportRepresentation = new AuthenticationExecutionExportRepresentation();
        authenticationExecutionExportRepresentation.setAuthenticatorConfig("config1");
        authenticationFlowRepresentation.setAuthenticationExecutions(List.of(authenticationExecutionExportRepresentation));

        var resultingAuthConfig = service.normalizeAuthConfig(List.of(authenticatorConfigRepresentation), List.of(authenticationFlowRepresentation));

        Assertions.assertThat(resultingAuthConfig).containsExactlyInAnyOrder(authenticatorConfigRepresentation);
    }

    @Test
    public void testNormalizeAuthConfigsCheckedForDuplicates(CapturedOutput output) {
        AuthenticatorConfigRepresentation authenticatorConfigRepresentation1 = new AuthenticatorConfigRepresentation();
        authenticatorConfigRepresentation1.setId(UUID.randomUUID().toString());
        authenticatorConfigRepresentation1.setAlias("config1");

        AuthenticatorConfigRepresentation authenticatorConfigRepresentation2 = new AuthenticatorConfigRepresentation();
        authenticatorConfigRepresentation2.setId(UUID.randomUUID().toString());
        authenticatorConfigRepresentation2.setAlias("config1");

        AuthenticationFlowRepresentation authenticationFlowRepresentation = new AuthenticationFlowRepresentation();

        AuthenticationExecutionExportRepresentation authenticationExecutionExportRepresentation = new AuthenticationExecutionExportRepresentation();
        authenticationExecutionExportRepresentation.setAuthenticatorConfig("config1");
        authenticationFlowRepresentation.setAuthenticationExecutions(List.of(authenticationExecutionExportRepresentation));

        var resultingAuthConfig = service.normalizeAuthConfig(List.of(authenticatorConfigRepresentation1, authenticatorConfigRepresentation2), List.of(authenticationFlowRepresentation));

        Assertions.assertThat(resultingAuthConfig).containsExactlyInAnyOrder(authenticatorConfigRepresentation1, authenticatorConfigRepresentation2);
        Assertions.assertThat(output.getOut()).contains("The following authenticator configs are duplicates: [config1]");
    }
}
