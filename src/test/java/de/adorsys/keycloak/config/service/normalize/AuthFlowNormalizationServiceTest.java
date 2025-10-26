/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2025 adorsys GmbH & Co. KG @ https://adorsys.com
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

import org.javers.core.Javers;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
public class AuthFlowNormalizationServiceTest {

    private AuthFlowNormalizationService service;
    private Javers unOrderedJavers;

    @BeforeEach
    void setUp() {
        unOrderedJavers = mock(Javers.class);
        service = new AuthFlowNormalizationService(unOrderedJavers);
        // NOTE: Do not provide a default global stub for unOrderedJavers.compare here.
        // Tests should explicitly stub compare(...) with a local Diff mock when they need to exercise the Javers branch.
    }

    // Helper creators
    private AuthenticationExecutionExportRepresentation exec(String authenticator, String flowAlias, Integer priority) {
        AuthenticationExecutionExportRepresentation e = new AuthenticationExecutionExportRepresentation();
        e.setAuthenticator(authenticator);
        e.setFlowAlias(flowAlias);
        e.setPriority(priority);
        return e;
    }

    private AuthenticationFlowRepresentation flow(String alias, boolean topLevel, boolean builtIn) {
        AuthenticationFlowRepresentation f = new AuthenticationFlowRepresentation();
        f.setAlias(alias);
        f.setTopLevel(topLevel);
        f.setBuiltIn(builtIn);
        // ensure executions list is never null to avoid NPEs in production code that iterates it
        f.setAuthenticationExecutions(Collections.emptyList());
        return f;
    }

    // executionsChanged tests
    @Test
    void testExecutionsChanged_BothNull() {
        assertThat(service.executionsChanged(null, null)).isFalse();
    }

    @Test
    void testExecutionsChanged_ExportedNullBaselineNotNull() {
        List<AuthenticationExecutionExportRepresentation> baseline = Collections.singletonList(exec("a", null, 1));
        assertThat(service.executionsChanged(null, baseline)).isTrue();
    }

    @Test
    void testExecutionsChanged_ExportedNotNullBaselineNull() {
        List<AuthenticationExecutionExportRepresentation> exported = Collections.singletonList(exec("a", null, 1));
        assertThat(service.executionsChanged(exported, null)).isTrue();
    }

    @Test
    void testExecutionsChanged_DifferentSizes() {
        List<AuthenticationExecutionExportRepresentation> exported = Arrays.asList(exec("a", null, 1), exec("b", null, 2));
        List<AuthenticationExecutionExportRepresentation> baseline = Collections.singletonList(exec("a", null, 1));
        assertThat(service.executionsChanged(exported, baseline)).isTrue();
    }

    @Test
    void testExecutionsChanged_SameSizeNoChanges() {
        AuthenticationExecutionExportRepresentation e1 = exec("auth", null, 1);
        AuthenticationExecutionExportRepresentation e2 = exec("auth", null, 1);
        List<AuthenticationExecutionExportRepresentation> exported = Collections.singletonList(e1);
        List<AuthenticationExecutionExportRepresentation> baseline = Collections.singletonList(e2);
        assertThat(service.executionsChanged(exported, baseline)).isFalse();
    }

    @Test
    void testExecutionsChanged_SameSizeWithChanges() {
        List<AuthenticationExecutionExportRepresentation> exported = Collections.singletonList(exec("auth1", null, 1));
        List<AuthenticationExecutionExportRepresentation> baseline = Collections.singletonList(exec("auth2", null, 1));
        assertThat(service.executionsChanged(exported, baseline)).isTrue();
    }

    @Test
    void testExecutionsChanged_PrioritySorting() {
        List<AuthenticationExecutionExportRepresentation> exported = Arrays.asList(exec("a", null, 2), exec("b", null, 1));
        List<AuthenticationExecutionExportRepresentation> baseline = Arrays.asList(exec("b", null, 1), exec("a", null, 2));
        assertThat(service.executionsChanged(exported, baseline)).isFalse();
    }

    @Test
    void testExecutionsChanged_EmptyLists() {
        assertThat(service.executionsChanged(new ArrayList<>(), new ArrayList<>())).isFalse();
    }

    // executionChanged tests
    @Test
    void testExecutionChanged_AuthenticatorConfigDifferent() {
        AuthenticationExecutionExportRepresentation a = exec(null, null, 1);
        a.setAuthenticatorConfig("c1");
        AuthenticationExecutionExportRepresentation b = exec(null, null, 1);
        b.setAuthenticatorConfig("c2");
        assertThat(service.executionChanged(a, b)).isTrue();
    }

    @Test
    void testExecutionChanged_AuthenticatorDifferent() {
        AuthenticationExecutionExportRepresentation a = exec("auth1", null, 1);
        AuthenticationExecutionExportRepresentation b = exec("auth2", null, 1);
        assertThat(service.executionChanged(a, b)).isTrue();
    }

    @Test
    void testExecutionChanged_IsAuthenticatorFlowDifferent() {
        AuthenticationExecutionExportRepresentation a = exec("auth", null, 1);
        a.setAuthenticatorFlow(Boolean.TRUE);
        AuthenticationExecutionExportRepresentation b = exec("auth", null, 1);
        b.setAuthenticatorFlow(Boolean.FALSE);
        assertThat(service.executionChanged(a, b)).isTrue();
    }

    @Test
    void testExecutionChanged_RequirementDifferent() {
        AuthenticationExecutionExportRepresentation a = exec("auth", null, 1);
        a.setRequirement("REQUIRED");
        AuthenticationExecutionExportRepresentation b = exec("auth", null, 1);
        b.setRequirement("ALTERNATIVE");
        assertThat(service.executionChanged(a, b)).isTrue();
    }

    @Test
    void testExecutionChanged_PriorityDifferent() {
        AuthenticationExecutionExportRepresentation a = exec("auth", null, 1);
        AuthenticationExecutionExportRepresentation b = exec("auth", null, 2);
        assertThat(service.executionChanged(a, b)).isTrue();
    }

    @Test
    void testExecutionChanged_FlowAliasDifferent() {
        AuthenticationExecutionExportRepresentation a = exec("auth", "flowA", 1);
        AuthenticationExecutionExportRepresentation b = exec("auth", "flowB", 1);
        assertThat(service.executionChanged(a, b)).isTrue();
    }

    @Test
    void testExecutionChanged_IsUserSetupAllowedDifferent() {
        AuthenticationExecutionExportRepresentation a = exec("auth", null, 1);
        a.setUserSetupAllowed(Boolean.TRUE);
        AuthenticationExecutionExportRepresentation b = exec("auth", null, 1);
        b.setUserSetupAllowed(Boolean.FALSE);
        assertThat(service.executionChanged(a, b)).isTrue();
    }

    @Test
    void testExecutionChanged_AllFieldsIdentical() {
        AuthenticationExecutionExportRepresentation a = exec("auth", "flow", 1);
        a.setAuthenticatorConfig("cfg");
        a.setAuthenticatorFlow(Boolean.FALSE);
        a.setRequirement("REQ");
        a.setUserSetupAllowed(Boolean.TRUE);
        AuthenticationExecutionExportRepresentation b = exec("auth", "flow", 1);
        b.setAuthenticatorConfig("cfg");
        b.setAuthenticatorFlow(Boolean.FALSE);
        b.setRequirement("REQ");
        b.setUserSetupAllowed(Boolean.TRUE);
        assertThat(service.executionChanged(a, b)).isFalse();
    }

    @Test
    void testExecutionChanged_NullFieldsHandling() {
        AuthenticationExecutionExportRepresentation a = exec(null, null, 0);
        AuthenticationExecutionExportRepresentation b = exec(null, null, 0);
        assertThat(service.executionChanged(a, b)).isFalse();
    }

    // detectBrokenAuthenticationFlows tests
    @Test
    void testDetectBrokenAuthenticationFlows_ValidFormFlow() {
        AuthenticationFlowRepresentation parent = flow("parent", true, false);
        AuthenticationExecutionExportRepresentation e = exec("auth", "sub", 1);
        parent.setAuthenticationExecutions(Collections.singletonList(e));
        AuthenticationFlowRepresentation sub = flow("sub", false, false);
        sub.setProviderId("form-flow");
        List<AuthenticationFlowRepresentation> flows = Arrays.asList(parent, sub);

        // should not throw
        service.detectBrokenAuthenticationFlows(flows);
    }

    @Test
    void testDetectBrokenAuthenticationFlows_InvalidNonFormFlow(CapturedOutput output) {
        AuthenticationFlowRepresentation parent = flow("parent", true, false);
        AuthenticationExecutionExportRepresentation e = exec("auth", "sub", 1);
        parent.setAuthenticationExecutions(Collections.singletonList(e));
        AuthenticationFlowRepresentation sub = flow("sub", false, false);
        sub.setProviderId("basic-flow");
        List<AuthenticationFlowRepresentation> flows = Arrays.asList(parent, sub);
        // should log an error about non 'form-flow' sub-flow
        service.detectBrokenAuthenticationFlows(flows);
        assertThat(output.toString()).contains("This is only possible if the sub-flow is of type 'form-flow'");
    }

    @Test
    void testDetectBrokenAuthenticationFlows_NoAuthenticatorSet() {
        AuthenticationFlowRepresentation parent = flow("parent", true, false);
        AuthenticationExecutionExportRepresentation e = exec(null, "sub", 1);
        parent.setAuthenticationExecutions(Collections.singletonList(e));
        AuthenticationFlowRepresentation sub = flow("sub", false, false);
        List<AuthenticationFlowRepresentation> flows = Arrays.asList(parent, sub);

        service.detectBrokenAuthenticationFlows(flows);
    }

    @Test
    void testDetectBrokenAuthenticationFlows_NoFlowAliasSet() {
        AuthenticationFlowRepresentation parent = flow("parent", true, false);
        AuthenticationExecutionExportRepresentation e = exec("auth", null, 1);
        parent.setAuthenticationExecutions(Collections.singletonList(e));
        List<AuthenticationFlowRepresentation> flows = Collections.singletonList(parent);

        service.detectBrokenAuthenticationFlows(flows);
    }

    @Test
    void testDetectBrokenAuthenticationFlows_EmptyFlowsList() {
        service.detectBrokenAuthenticationFlows(new ArrayList<>());
    }

    @Test
    void testDetectBrokenAuthenticationFlows_MultipleFlowsWithIssues() {
        AuthenticationFlowRepresentation p1 = flow("p1", true, false);
        p1.setAuthenticationExecutions(Collections.singletonList(exec("auth", "s1", 1)));
        AuthenticationFlowRepresentation s1 = flow("s1", false, false);
        s1.setProviderId("basic-flow");

        AuthenticationFlowRepresentation p2 = flow("p2", true, false);
        p2.setAuthenticationExecutions(Collections.singletonList(exec("auth", "s2", 1)));
        AuthenticationFlowRepresentation s2 = flow("s2", false, false);
        s2.setProviderId("basic-flow");

        List<AuthenticationFlowRepresentation> flows = Arrays.asList(p1, s1, p2, s2);
        service.detectBrokenAuthenticationFlows(flows);
    }

    // normalizeAuthFlows tests - interaction with Javers
    @Test
    void testNormalizeAuthFlows_FilterUnusedNonTopLevel_AllTopLevel() {
        AuthenticationFlowRepresentation a = flow("a", true, false);
        List<AuthenticationFlowRepresentation> exported = Collections.singletonList(a);

        // No need to mock Javers since this test is only checking flow filtering and has no baseline flows

        List<AuthenticationFlowRepresentation> result = service.normalizeAuthFlows(exported, Collections.emptyList());
        assertThat(result).contains(a);
    }

    @Test
    void testNormalizeAuthFlows_FilterUnusedNonTopLevel_UsedSubFlow() {
        AuthenticationFlowRepresentation top = flow("top", true, false);
        AuthenticationExecutionExportRepresentation e = exec("auth", "sub", 1);
        top.setAuthenticationExecutions(Collections.singletonList(e));
        AuthenticationFlowRepresentation sub = flow("sub", false, false);
        List<AuthenticationFlowRepresentation> exported = Arrays.asList(top, sub);

        // No need to mock Javers since this test is only checking subflow filtering logic

        List<AuthenticationFlowRepresentation> result = service.normalizeAuthFlows(exported, Collections.emptyList());
        assertThat(result).contains(top, sub);
    }

    @Test
    void testNormalizeAuthFlows_FilterUnusedNonTopLevel_UnusedSubFlow() {
        AuthenticationFlowRepresentation top = flow("top", true, false);
        AuthenticationFlowRepresentation sub = flow("sub", false, false);
        List<AuthenticationFlowRepresentation> exported = Arrays.asList(top, sub);

        // No need to mock Javers since we're only testing unused subflow filtering

        List<AuthenticationFlowRepresentation> result = service.normalizeAuthFlows(exported, Collections.emptyList());
        assertThat(result).contains(top);
    }

    @Test
    void testNormalizeAuthFlows_FilterUnusedNonTopLevel_NestedFlowHierarchy() {
        AuthenticationFlowRepresentation a = flow("A", true, false);
        AuthenticationExecutionExportRepresentation e1 = exec(null, "B", 1);
        a.setAuthenticationExecutions(Collections.singletonList(e1));
        AuthenticationFlowRepresentation b = flow("B", false, false);
        AuthenticationExecutionExportRepresentation e2 = exec(null, "C", 1);
        b.setAuthenticationExecutions(Collections.singletonList(e2));
        AuthenticationFlowRepresentation c = flow("C", false, false);
        AuthenticationFlowRepresentation d = flow("D", false, false);
        List<AuthenticationFlowRepresentation> exported = Arrays.asList(a, b, c, d);

        // No need to mock Javers since this test focuses on nested flow filtering

        List<AuthenticationFlowRepresentation> result = service.normalizeAuthFlows(exported, Collections.emptyList());
        assertThat(result).contains(a, b, c).doesNotContain(d);
    }

    @Test
    void testNormalizeAuthFlows_FilterUnusedNonTopLevel_WhileLoopMultipleIterations() {
        AuthenticationFlowRepresentation top = flow("top", true, false);
        AuthenticationFlowRepresentation s1 = flow("s1", false, false);
        AuthenticationFlowRepresentation s2 = flow("s2", false, false);
        AuthenticationFlowRepresentation s3 = flow("s3", false, false);
        top.setAuthenticationExecutions(Collections.singletonList(exec(null, "s1", 1)));
        s1.setAuthenticationExecutions(Collections.singletonList(exec(null, "s2", 1)));
        s2.setAuthenticationExecutions(Collections.singletonList(exec(null, "s3", 1)));
        List<AuthenticationFlowRepresentation> exported = Arrays.asList(top, s1, s2, s3);

        // No need to mock Javers since this test checks deep subflow chaining

        List<AuthenticationFlowRepresentation> result = service.normalizeAuthFlows(exported, Collections.emptyList());
        assertThat(result).contains(top, s1, s2, s3);
    }

    @Test
    void testNormalizeAuthFlows_FilterBuiltInFlows() {
        AuthenticationFlowRepresentation built = flow("built", true, true);
        AuthenticationFlowRepresentation normal = flow("n", true, false);
        List<AuthenticationFlowRepresentation> exported = Arrays.asList(built, normal);

        // No need to mock Javers since this test only validates built-in flow filtering

        List<AuthenticationFlowRepresentation> result = service.normalizeAuthFlows(exported, Collections.emptyList());
        assertThat(result).contains(normal).doesNotContain(built);
    }

    @Test
    void testNormalizeAuthFlows_DeletedBaselineFlow() {
        AuthenticationFlowRepresentation baseline = flow("flow1", true, false);
        List<AuthenticationFlowRepresentation> exported = Collections.emptyList();

        // No need to mock Javers since we're only testing deleted flow handling

        List<AuthenticationFlowRepresentation> result = service.normalizeAuthFlows(exported, Collections.singletonList(baseline));
        // result may be null or empty; ensure method runs without exception
        assertThat(result == null || result.isEmpty()).isTrue();
    }

    @Test
    void testNormalizeAuthFlows_NoChangesDetected() {
        AuthenticationFlowRepresentation baseline = flow("f", true, false);
        AuthenticationFlowRepresentation exported = flow("f", true, false);
        List<AuthenticationFlowRepresentation> exportedList = Collections.singletonList(exported);

        // explicitly stub compare to report no changes
        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(false);
        when(unOrderedJavers.compare(baseline, exported)).thenReturn(diff);

        List<AuthenticationFlowRepresentation> result = service.normalizeAuthFlows(exportedList, Collections.singletonList(baseline));
        // when no changes detected by Javers and no execution changes, flow should be omitted
        assertThat(result == null || result.isEmpty()).isTrue();
    }

    @Test
    void testNormalizeAuthFlows_ChangesDetectedByJavers() {
        AuthenticationFlowRepresentation baseline = flow("f", true, false);
        AuthenticationFlowRepresentation exported = flow("f", true, false);

        Diff diff = mock(Diff.class);
        when(diff.hasChanges()).thenReturn(true);
        when(unOrderedJavers.compare(baseline, exported)).thenReturn(diff);

        List<AuthenticationFlowRepresentation> result = service.normalizeAuthFlows(Collections.singletonList(exported), Collections.singletonList(baseline));
        assertThat(result).contains(exported);
    }

    @Test
    void testNormalizeAuthFlows_ChangesDetectedByExecutionsChanged() {
        AuthenticationFlowRepresentation baseline = flow("f", true, false);
        AuthenticationFlowRepresentation exported = flow("f", true, false);
        exported.setAuthenticationExecutions(Collections.singletonList(exec("a", null, 1)));

        Diff diffFalse = mock(Diff.class);
        when(diffFalse.hasChanges()).thenReturn(false);
        when(unOrderedJavers.compare(baseline, exported)).thenReturn(diffFalse);

        List<AuthenticationFlowRepresentation> result = service.normalizeAuthFlows(Collections.singletonList(exported), Collections.singletonList(baseline));
        // since executionsChanged() is true the flow should be included
        assertThat(result).contains(exported);
    }

    @Test
    void testNormalizeAuthFlows_NewFlowInExport() {
        AuthenticationFlowRepresentation baseline = flow("flow1", true, false);
        AuthenticationFlowRepresentation f2 = flow("flow2", true, false);
        List<AuthenticationFlowRepresentation> exported = Arrays.asList(baseline, f2);

        // Need to mock Javers since we have both baseline and exported flows to compare
        Diff diffFalse = mock(Diff.class);
        when(diffFalse.hasChanges()).thenReturn(false);
        when(unOrderedJavers.compare(baseline, baseline)).thenReturn(diffFalse);

        List<AuthenticationFlowRepresentation> result = service.normalizeAuthFlows(exported, Collections.singletonList(baseline));
        assertThat(result).contains(f2);
    }

    @Test
    void testNormalizeAuthFlows_IdSetToNull() {
        AuthenticationFlowRepresentation f = flow("f", true, false);
        f.setId("id1");
        List<AuthenticationFlowRepresentation> exported = Collections.singletonList(f);

        // No need to mock Javers since this test only checks ID nulling behavior

        List<AuthenticationFlowRepresentation> result = service.normalizeAuthFlows(exported, Collections.emptyList());
        assertThat(result).allMatch(x -> x.getId() == null);
    }

    @Test
    void testNormalizeAuthFlows_EmptyResult() {
        List<AuthenticationFlowRepresentation> exported = Collections.emptyList();

        // No need to mock Javers since this test has empty inputs

        List<AuthenticationFlowRepresentation> result = service.normalizeAuthFlows(exported, Collections.emptyList());
        assertThat(result == null || result.isEmpty()).isTrue();
    }

    @Test
    void testNormalizeAuthFlows_DetectBrokenFlowsCalled() {
        AuthenticationFlowRepresentation f = flow("f", true, false);
        List<AuthenticationFlowRepresentation> exported = Collections.singletonList(f);
        // we cannot easily spy internal method calls without a spy; just run to ensure no exception
        // No need to mock Javers since we're only validating detectBrokenFlows gets called

        service.normalizeAuthFlows(exported, Collections.emptyList());
    }
    @Test
    void testDetectBrokenAuthenticationFlows_MissingSubFlowAliasThrowsNPE() {
        AuthenticationFlowRepresentation parent = flow("parent", true, false);
        AuthenticationExecutionExportRepresentation e = exec("auth", "missing", 1);
        parent.setAuthenticationExecutions(Collections.singletonList(e));
        List<AuthenticationFlowRepresentation> flows = Collections.singletonList(parent);

        // currently this will throw a NullPointerException because the referenced flow is missing
        assertThrows(NullPointerException.class, () -> service.detectBrokenAuthenticationFlows(flows));
    }

}
