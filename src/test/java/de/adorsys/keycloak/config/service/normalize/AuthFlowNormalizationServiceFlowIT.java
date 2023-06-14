package de.adorsys.keycloak.config.service.normalize;

import de.adorsys.keycloak.config.normalize.AbstractNormalizeTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;

import java.util.ArrayList;
import java.util.List;

class AuthFlowNormalizationServiceFlowIT extends AbstractNormalizeTest {

    @Autowired
    AuthFlowNormalizationService service;


    @Test
    public void testNormalizeAuthFlows() {
       var resultingAuthFlows = service.normalizeAuthFlows(new ArrayList<>(), new ArrayList<>());
       Assertions.assertThat(resultingAuthFlows).isNull();
    }

    @Test
    public void testNormalizeAuthFlowsIgnoreBuiltInTrue() {
        AuthenticationFlowRepresentation authenticationFlowRepresentation = new AuthenticationFlowRepresentation();
        authenticationFlowRepresentation.setBuiltIn(true);

        AuthenticationFlowRepresentation authenticationFlowRepresentationBaseline = new AuthenticationFlowRepresentation();
        authenticationFlowRepresentationBaseline.setBuiltIn(true);

        var resultingAuthFlows = service.normalizeAuthFlows(List.of(authenticationFlowRepresentation), List.of(authenticationFlowRepresentationBaseline));

        Assertions.assertThat(resultingAuthFlows).isNull();
    }

    @Test
    public void testNormalizeAuthFlowsIgnoreBuiltInTrueButBaselineHasEntryCreatesRecreationWarning(CapturedOutput output) {
        AuthenticationFlowRepresentation authenticationFlowRepresentation = new AuthenticationFlowRepresentation();
        authenticationFlowRepresentation.setBuiltIn(true);

        AuthenticationFlowRepresentation authenticationFlowRepresentationBaseline = new AuthenticationFlowRepresentation();
        authenticationFlowRepresentationBaseline.setBuiltIn(false);
        authenticationFlowRepresentationBaseline.setAlias("flow1");

        var resultingAuthFlows = service.normalizeAuthFlows(List.of(authenticationFlowRepresentation), List.of(authenticationFlowRepresentationBaseline));

        Assertions.assertThat(resultingAuthFlows).isNull();
        Assertions.assertThat(output.getOut()).contains("Default realm authentication flow 'flow1' was deleted in exported realm. It may be reintroduced during import");

    }
}
