/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2021 adorsys GmbH & Co. KG @ https://adorsys.com
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */
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
