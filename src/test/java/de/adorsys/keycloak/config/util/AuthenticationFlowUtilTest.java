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

package de.adorsys.keycloak.config.util;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.model.AuthenticationFlowImport;
import de.adorsys.keycloak.config.model.RealmImport;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticationFlowUtilTest {

    private static AuthenticationFlowImport flow(String alias, boolean topLevel) {
        AuthenticationFlowImport f = new AuthenticationFlowImport();
        f.setAlias(alias);
        f.setTopLevel(topLevel);
        f.setAuthenticationExecutions(new ArrayList<>());
        return f;
    }

    private static AuthenticationExecutionExportRepresentation exec(boolean authenticatorFlow, String flowAlias) {
        AuthenticationExecutionExportRepresentation e = new AuthenticationExecutionExportRepresentation();
        e.setAuthenticatorFlow(authenticatorFlow);
        e.setFlowAlias(flowAlias);
        return e;
    }

    @Test
    void getTopLevelFlows_shouldFilterByTopLevel() {
        RealmImport realmImport = new RealmImport();
        realmImport.setAuthenticationFlowImports(List.of(
                flow("top", true),
                flow("sub", false)
        ));

        var topLevel = AuthenticationFlowUtil.getTopLevelFlows(realmImport);
        assertThat(topLevel.size(), is(1));
        assertThat(topLevel.getFirst().getAlias(), is("top"));
    }

    @Test
    void getSubFlow_shouldReturnNonTopLevelFlowByAlias() {
        RealmImport realmImport = new RealmImport();
        realmImport.setAuthenticationFlowImports(List.of(
                flow("top", true),
                flow("sub", false)
        ));

        var subFlow = AuthenticationFlowUtil.getSubFlow(realmImport, "sub");
        assertThat(subFlow.getAlias(), is("sub"));
    }

    @Test
    void getSubFlow_shouldThrowWhenNotFound() {
        RealmImport realmImport = new RealmImport();
        realmImport.setAuthenticationFlowImports(List.of(flow("top", true)));

        assertThrows(ImportProcessingException.class, () -> AuthenticationFlowUtil.getSubFlow(realmImport, "missing"));
    }

    @Test
    void getSubFlowsForTopLevelFlow_shouldResolveOnlyAuthenticatorFlows() {
        AuthenticationFlowImport top = flow("top", true);
        top.setAuthenticationExecutions(new ArrayList<>(List.of(
                exec(true, "sub"),
                exec(false, "ignored")
        )));

        RealmImport realmImport = new RealmImport();
        realmImport.setAuthenticationFlowImports(List.of(
                top,
                flow("sub", false)
        ));

        List<AuthenticationFlowRepresentation> subFlows = AuthenticationFlowUtil.getSubFlowsForTopLevelFlow(realmImport, top);
        assertThat(subFlows, contains(AuthenticationFlowUtil.getSubFlow(realmImport, "sub")));
    }

    @Test
    void getSubFlowsForTopLevelFlow_shouldReturnEmptyWhenNoExecutions() {
        AuthenticationFlowImport top = flow("top", true);

        RealmImport realmImport = new RealmImport();
        realmImport.setAuthenticationFlowImports(List.of(top));

        List<AuthenticationFlowRepresentation> subFlows = AuthenticationFlowUtil.getSubFlowsForTopLevelFlow(realmImport, top);
        assertThat(subFlows, is(empty()));
    }
}
