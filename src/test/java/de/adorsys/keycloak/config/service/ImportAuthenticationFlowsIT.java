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

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.AbstractImportTest;
import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.exception.InvalidImportException;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.util.VersionUtil;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static de.adorsys.keycloak.config.test.util.KeycloakRepository.getAuthenticatorConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"java:S5961", "java:S5976"})
class ImportAuthenticationFlowsIT extends AbstractImportTest {
    private static final String REALM_NAME = "realmWithFlow";

    ImportAuthenticationFlowsIT() {
        this.resourcePath = "import-files/auth-flows";
    }

    @Test
    @Order(0)
    void shouldCreateRealmWithFlows() {
        doImport("00_create_realm_with_flows.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my auth flow");
        assertThat(flow.getDescription(), is("My auth flow for testing"));
        assertThat(flow.getProviderId(), is("basic-flow"));
        assertThat(flow.isBuiltIn(), is(false));
        assertThat(flow.isTopLevel(), is(true));

        List<AuthenticationExecutionExportRepresentation> executions = flow.getAuthenticationExecutions();
        assertThat(executions, hasSize(1));

        List<AuthenticationExecutionExportRepresentation> execution = getExecutionFromFlow(flow, "docker-http-basic-authenticator");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("docker-http-basic-authenticator"));
        assertThat(execution.get(0).getRequirement(), is("DISABLED"));
        assertThat(execution.get(0).getPriority(), is(0));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));
    }

    @Test
    @Order(1)
    void shouldAddExecutionToFlow() {
        doImport("01_update_realm__add_execution_to_flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my auth flow");
        assertThat(flow.getDescription(), is("My auth flow for testing"));
        assertThat(flow.getProviderId(), is("basic-flow"));
        assertThat(flow.isBuiltIn(), is(false));
        assertThat(flow.isTopLevel(), is(true));

        List<AuthenticationExecutionExportRepresentation> executions = flow.getAuthenticationExecutions();
        assertThat(executions, hasSize(2));

        List<AuthenticationExecutionExportRepresentation> execution;
        execution = getExecutionFromFlow(flow, "docker-http-basic-authenticator");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("docker-http-basic-authenticator"));
        assertThat(execution.get(0).getRequirement(), is("DISABLED"));
        assertThat(execution.get(0).getPriority(), is(0));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));

        execution = getExecutionFromFlow(flow, "http-basic-authenticator");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("http-basic-authenticator"));
        assertThat(execution.get(0).getRequirement(), is("DISABLED"));
        assertThat(execution.get(0).getPriority(), is(1));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));
    }

    @Test
    @Order(2)
    void shouldChangeExecutionRequirement() {
        doImport("02_update_realm__change_execution_requirement.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my auth flow");
        assertThat(flow.getDescription(), is("My auth flow for testing"));
        assertThat(flow.getProviderId(), is("basic-flow"));
        assertThat(flow.isBuiltIn(), is(false));
        assertThat(flow.isTopLevel(), is(true));

        List<AuthenticationExecutionExportRepresentation> executions = flow.getAuthenticationExecutions();
        assertThat(executions, hasSize(2));

        List<AuthenticationExecutionExportRepresentation> execution;
        execution = getExecutionFromFlow(flow, "docker-http-basic-authenticator");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("docker-http-basic-authenticator"));
        assertThat(execution.get(0).getRequirement(), is("REQUIRED"));
        assertThat(execution.get(0).getPriority(), is(0));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));

        execution = getExecutionFromFlow(flow, "http-basic-authenticator");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("http-basic-authenticator"));
        assertThat(execution.get(0).getRequirement(), is("DISABLED"));
        assertThat(execution.get(0).getPriority(), is(1));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));
    }

    @Test
    @Order(3)
    void shouldChangeExecutionPriorities() {
        doImport("03_update_realm__change_execution_priorities.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my auth flow");
        assertThat(flow.getDescription(), is("My auth flow for testing"));
        assertThat(flow.getProviderId(), is("basic-flow"));
        assertThat(flow.isBuiltIn(), is(false));
        assertThat(flow.isTopLevel(), is(true));

        List<AuthenticationExecutionExportRepresentation> executions = flow.getAuthenticationExecutions();
        assertThat(executions, hasSize(2));

        List<AuthenticationExecutionExportRepresentation> execution;
        execution = getExecutionFromFlow(flow, "docker-http-basic-authenticator");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("docker-http-basic-authenticator"));
        assertThat(execution.get(0).getRequirement(), is("REQUIRED"));
        assertThat(execution.get(0).getPriority(), is(1));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));

        execution = getExecutionFromFlow(flow, "http-basic-authenticator");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("http-basic-authenticator"));
        assertThat(execution.get(0).getRequirement(), is("DISABLED"));
        assertThat(execution.get(0).getPriority(), is(0));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));
    }

    @Test
    @Order(4)
    void shouldAddFlowWithExecutionFlow() {
        doImport("04_update_realm__add_flow_with_execution_flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my registration");
        assertThat(flow.getDescription(), is("My registration flow"));
        assertThat(flow.getProviderId(), is("basic-flow"));
        assertThat(flow.isBuiltIn(), is(false));
        assertThat(flow.isTopLevel(), is(true));

        List<AuthenticationExecutionExportRepresentation> executionFlows = flow.getAuthenticationExecutions();
        assertThat(executionFlows, hasSize(1));

        List<AuthenticationExecutionExportRepresentation> execution;
        execution = getExecutionFromFlow(flow, "registration-page-form");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("registration-page-form"));
        assertThat(execution.get(0).getRequirement(), is("REQUIRED"));
        assertThat(execution.get(0).getPriority(), is(0));
        assertThat(execution.get(0).isAutheticatorFlow(), is(true));

        AuthenticationFlowRepresentation nonTopLevelFlow = getAuthenticationFlow(realm, "my registration form");
        List<AuthenticationExecutionExportRepresentation> nonTopLevelFlowExecutions = nonTopLevelFlow.getAuthenticationExecutions();
        assertThat(nonTopLevelFlowExecutions, hasSize(2));

        execution = getExecutionFromFlow(nonTopLevelFlow, "registration-user-creation");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("registration-user-creation"));
        assertThat(execution.get(0).getRequirement(), is("REQUIRED"));
        assertThat(execution.get(0).getPriority(), is(0));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));

        execution = getExecutionFromFlow(nonTopLevelFlow, "registration-profile-action");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("registration-profile-action"));
        assertThat(execution.get(0).getRequirement(), is("DISABLED"));
        assertThat(execution.get(0).getPriority(), is(1));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));
    }

    @Test
    @Order(5)
    void shouldFailWhenTryAddFlowWithDefectiveExecutionFlow() {
        RealmImport foundImport = getImport("05_try_to_update_realm__add_flow_with_defective_execution_flow.json");

        ImportProcessingException thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), matchesPattern("Cannot create execution-flow 'my registration form' for top-level-flow 'my registration' in realm 'realmWithFlow':.*"));
    }

    @Test
    @Order(6)
    void shouldChangeFlowRequirementWithExecutionFlow() {
        doImport("10_update_realm__change_requirement_flow_with_execution_flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my registration");
        assertThat(flow.getDescription(), is("My registration flow"));
        assertThat(flow.getProviderId(), is("basic-flow"));
        assertThat(flow.isBuiltIn(), is(false));
        assertThat(flow.isTopLevel(), is(true));

        List<AuthenticationExecutionExportRepresentation> executionFlows = flow.getAuthenticationExecutions();
        assertThat(executionFlows, hasSize(1));

        List<AuthenticationExecutionExportRepresentation> execution;
        execution = getExecutionFromFlow(flow, "registration-page-form");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("registration-page-form"));
        assertThat(execution.get(0).getRequirement(), is("REQUIRED"));
        assertThat(execution.get(0).getPriority(), is(0));
        assertThat(execution.get(0).isAutheticatorFlow(), is(true));

        AuthenticationFlowRepresentation nonTopLevelFlow = getAuthenticationFlow(realm, "my registration form");

        List<AuthenticationExecutionExportRepresentation> nonTopLevelFlowExecutions = nonTopLevelFlow.getAuthenticationExecutions();
        assertThat(nonTopLevelFlowExecutions, hasSize(2));

        execution = getExecutionFromFlow(nonTopLevelFlow, "registration-user-creation");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("registration-user-creation"));
        assertThat(execution.get(0).getRequirement(), is("REQUIRED"));
        assertThat(execution.get(0).getPriority(), is(0));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));

        execution = getExecutionFromFlow(nonTopLevelFlow, "registration-profile-action");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("registration-profile-action"));
        assertThat(execution.get(0).getRequirement(), is("REQUIRED"));
        assertThat(execution.get(0).getPriority(), is(1));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));
    }

    @Test
    @Order(7)
    void shouldFailWhenTryToUpdateDefectiveFlowRequirementWithExecutionFlow() {
        RealmImport foundImport = getImport("06_try_to_update_realm__change_requirement_in_defective_flow_with_execution_flow.json");

        ImportProcessingException thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), matchesPattern("Cannot create execution-flow 'my registration form' for top-level-flow 'my registration' in realm 'realmWithFlow': .*"));
    }

    @Test
    @Order(8)
    void shouldFailWhenTryToUpdateFlowRequirementWithExecutionFlowWithNotExistingExecution() {
        RealmImport foundImport = getImport("07_try_to_update_realm__change_requirement_flow_with_execution_flow_with_not_existing_execution.json");

        ImportProcessingException thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), matchesPattern("Cannot create execution 'not-existing-registration-user-creation' for non-top-level-flow 'my registration form' in realm 'realmWithFlow': .*"));
    }

    @Test
    @Order(9)
    void shouldFailWhenTryToUpdateFlowRequirementWithExecutionFlowWithDefectiveExecution() {
        RealmImport foundImport = getImport("08_try_to_update_realm__change_requirement_flow_with_execution_flow_with_defective_execution.json");

        ImportProcessingException thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), matchesPattern("Cannot update execution-flow 'registration-user-creation' for flow 'my registration form' in realm 'realmWithFlow': .*"));
    }

    @Test
    @Order(10)
    void shouldFailWhenTryToUpdateFlowRequirementWithDefectiveExecutionFlow() {
        RealmImport foundImport = getImport("09_try_to_update_realm__change_requirement_flow_with_defective_execution_flow.json");

        ImportProcessingException thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), is("Cannot create execution-flow 'docker-http-basic-authenticator' for top-level-flow 'my auth flow' in realm 'realmWithFlow'"));
    }

    @Test
    @Order(11)
    void shouldChangeFlowPriorityWithExecutionFlow() {
        doImport("11_update_realm__change_priority_flow_with_execution_flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my registration");
        assertThat(flow.getDescription(), is("My registration flow"));
        assertThat(flow.getProviderId(), is("basic-flow"));
        assertThat(flow.isBuiltIn(), is(false));
        assertThat(flow.isTopLevel(), is(true));

        List<AuthenticationExecutionExportRepresentation> executionFlows = flow.getAuthenticationExecutions();
        assertThat(executionFlows, hasSize(1));

        List<AuthenticationExecutionExportRepresentation> execution;
        execution = getExecutionFromFlow(flow, "registration-page-form");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("registration-page-form"));
        assertThat(execution.get(0).getRequirement(), is("REQUIRED"));
        assertThat(execution.get(0).getPriority(), is(0));
        assertThat(execution.get(0).isAutheticatorFlow(), is(true));

        AuthenticationFlowRepresentation nonTopLevelFlow = getAuthenticationFlow(realm, "my registration form");

        List<AuthenticationExecutionExportRepresentation> nonTopLevelFlowExecutions = nonTopLevelFlow.getAuthenticationExecutions();
        assertThat(nonTopLevelFlowExecutions, hasSize(2));

        execution = getExecutionFromFlow(nonTopLevelFlow, "registration-user-creation");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("registration-user-creation"));
        assertThat(execution.get(0).getRequirement(), is("REQUIRED"));
        assertThat(execution.get(0).getPriority(), is(1));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));

        execution = getExecutionFromFlow(nonTopLevelFlow, "registration-profile-action");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("registration-profile-action"));
        assertThat(execution.get(0).getRequirement(), is("REQUIRED"));
        assertThat(execution.get(0).getPriority(), is(0));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));
    }

    @Test
    @Order(12)
    void shouldSetRegistrationFlow() {
        doImport("12_update_realm__set_registration_flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        assertThat(realm.getRegistrationFlow(), is("my registration"));
    }

    @Test
    @Order(13)
    void shouldChangeRegistrationFlow() {
        doImport("13_update_realm__change_registration_flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        assertThat(realm.getRegistrationFlow(), is("my registration"));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my registration");
        assertThat(flow.getDescription(), is("My changed registration flow"));
    }

    @Test
    @Order(14)
    void shouldAddAndSetResetCredentialsFlow() {
        doImport("14_update_realm__add_and_set_custom_reset-credentials-flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        assertThat(realm.getResetCredentialsFlow(), is("my reset credentials"));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my reset credentials");
        assertThat(flow.getDescription(), is("My reset credentials for a user if they forgot their password or something"));
    }

    @Test
    @Order(15)
    void shouldChangeResetCredentialsFlow() {
        doImport("15_update_realm__change_custom_reset-credentials-flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        assertThat(realm.getResetCredentialsFlow(), is("my reset credentials"));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my reset credentials");
        assertThat(flow.getDescription(), is("My changed reset credentials for a user if they forgot their password or something"));
    }

    @Test
    @Order(16)
    void shouldAddAndSetBrowserFlow() {
        doImport("16_update_realm__add_and_set_custom_browser-flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        assertThat(realm.getBrowserFlow(), is("my browser"));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my browser");
        assertThat(flow.getDescription(), is("My browser based authentication"));
    }

    @Test
    @Order(17)
    void shouldChangeBrowserFlow() {
        doImport("17.1_update_realm__change_custom_browser-flow.json");

        assertThatBrowserFlowIsUpdated(4);

        doImport("17.2_update_realm__change_custom_browser-flow_with_multiple_subflow.json");

        AuthenticationFlowRepresentation flow = assertThatBrowserFlowIsUpdated(5);

        AuthenticationExecutionExportRepresentation myForms2 = getExecutionFlowFromFlow(flow, "my forms 2");
        assertThat(myForms2, notNullValue());
        assertThat(myForms2.getRequirement(), is("ALTERNATIVE"));
        assertThat(myForms2.getPriority(), is(4));
        assertThat(myForms2.isUserSetupAllowed(), is(false));
        assertThat(myForms2.isAutheticatorFlow(), is(true));
    }

    AuthenticationFlowRepresentation assertThatBrowserFlowIsUpdated(int expectedNumberOfExecutionsInFlow) {
        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        assertThat(realm.getBrowserFlow(), is("my browser"));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my browser");
        assertThat(flow.getDescription(), is("My changed browser based authentication"));

        assertThat(flow.getAuthenticationExecutions().size(), is(expectedNumberOfExecutionsInFlow));

        AuthenticationExecutionExportRepresentation myForms = getExecutionFlowFromFlow(flow, "my forms");
        assertThat(myForms, notNullValue());
        assertThat(myForms.getRequirement(), is("ALTERNATIVE"));
        assertThat(myForms.getPriority(), is(3));
        assertThat(myForms.isUserSetupAllowed(), is(false));
        assertThat(myForms.isAutheticatorFlow(), is(true));

        return flow;
    }

    @Test
    @Order(18)
    void shouldAddAndSetDirectGrantFlow() {
        doImport("18_update_realm__add_and_set_custom_direct-grant-flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        assertThat(realm.getDirectGrantFlow(), is("my direct grant"));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my direct grant");
        assertThat(flow.getDescription(), is("My OpenID Connect Resource Owner Grant"));
    }

    @Test
    @Order(19)
    void shouldChangeDirectGrantFlow() {
        doImport("19_update_realm__change_custom_direct-grant-flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        assertThat(realm.getDirectGrantFlow(), is("my direct grant"));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my direct grant");
        assertThat(flow.getDescription(), is("My changed OpenID Connect Resource Owner Grant"));
    }

    @Test
    @Order(20)
    void shouldAddAndSetClientAuthenticationFlow() {
        doImport("20_update_realm__add_and_set_custom_client-authentication-flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        assertThat(realm.getClientAuthenticationFlow(), is("my clients"));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my clients");
        assertThat(flow.getDescription(), is("My Base authentication for clients"));
    }

    @Test
    @Order(21)
    void shouldChangeClientAuthenticationFlow() {
        doImport("21_update_realm__change_custom_client-authentication-flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        assertThat(realm.getClientAuthenticationFlow(), is("my clients"));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my clients");
        assertThat(flow.getDescription(), is("My changed Base authentication for clients"));
    }

    @Test
    @Order(22)
    void shouldAddAndSetDockerAuthenticationFlow() {
        doImport("22_update_realm__add_and_set_custom_docker-authentication-flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        assertThat(realm.getDockerAuthenticationFlow(), is("my docker auth"));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my docker auth");
        assertThat(flow.getDescription(), is("My Used by Docker clients to authenticate against the IDP"));
    }

    @Test
    @Order(23)
    void shouldChangeDockerAuthenticationFlow() {
        doImport("23_update_realm__change_custom_docker-authentication-flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        assertThat(realm.getDockerAuthenticationFlow(), is("my docker auth"));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my docker auth");
        assertThat(flow.getDescription(), is("My changed Used by Docker clients to authenticate against the IDP"));
    }

    @Test
    @Order(24)
    void shouldAddTopLevelFlowWithExecutionFlow() {
        doImport("24_update_realm__add-top-level-flow-with-execution-flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my auth flow with execution-flows");
        assertThat(flow.getDescription(), is("My authentication flow with authentication executions"));
        assertThat(flow.getProviderId(), is("basic-flow"));
        assertThat(flow.isBuiltIn(), is(false));
        assertThat(flow.isTopLevel(), is(true));

        AuthenticationFlowRepresentation nonTopLevelFlow = getAuthenticationFlow(realm, "my execution-flow");

        List<AuthenticationExecutionExportRepresentation> nonTopLevelFlowExecutions = nonTopLevelFlow.getAuthenticationExecutions();
        assertThat(nonTopLevelFlowExecutions, hasSize(2));

        List<AuthenticationExecutionExportRepresentation> execution = getExecutionFromFlow(nonTopLevelFlow, "auth-username-password-form");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("auth-username-password-form"));
        assertThat(execution.get(0).getRequirement(), is("REQUIRED"));
        assertThat(execution.get(0).getPriority(), is(0));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));

        execution = getExecutionFromFlow(nonTopLevelFlow, "auth-otp-form");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("auth-otp-form"));
        assertThat(execution.get(0).getRequirement(), is("CONDITIONAL"));
        assertThat(execution.get(0).getPriority(), is(1));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));
    }

    @Test
    @Order(25)
    void shouldUpdateTopLevelFlowWithPseudoId() {
        doImport("25_update_realm__update-top-level-flow-with-pseudo-id.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my auth flow");
        assertThat(flow.getDescription(), is("My auth flow for testing with pseudo-id"));
    }

    @Test
    @Order(26)
    void shouldUpdateNonTopLevelFlowWithPseudoId() {
        doImport("26_update_realm__update-non-top-level-flow-with-pseudo-id.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        AuthenticationFlowRepresentation nonTopLevelFlow = getAuthenticationFlow(realm, "my registration form");
        assertThat(nonTopLevelFlow.getDescription(), is("My registration form with pseudo-id"));
    }

    @Test
    @Order(27)
    void shouldNotUpdateNonTopLevelFlowWithPseudoId() {
        RealmImport foundImport = getImport("27_update_realm__try-to-update-non-top-level-flow-with-pseudo-id.json");

        ImportProcessingException thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), matchesPattern("Cannot create execution-flow 'my registration form' for top-level-flow 'my registration' in realm 'realmWithFlow': .*"));
    }

    @Test
    @Order(28)
    void shouldUpdateNonTopLevelFlowWithPseudoIdAndReUseTempFlow() {
        doImport("28_update_realm__update-non-top-level-flow-with-pseudo-id.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        assertThat(realm.getRegistrationFlow(), is("my registration"));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my registration");
        assertThat(flow.getDescription(), is("changed registration flow"));

        AuthenticationFlowRepresentation tempFlow = getAuthenticationFlow(realm, "TEMPORARY_CREATED_AUTH_FLOW");
        assertThat(tempFlow, nullValue());
    }

    @Test
    @Order(29)
    void shouldNotUpdateInvalidTopLevelFlow() {
        RealmImport foundImport = getImport("29_update_realm__try-to-update-invalid-top-level-flow.json");

        ImportProcessingException thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), matchesPattern("Cannot create top-level-flow 'my auth flow' in realm 'realmWithFlow': .*"));
    }

    @Test
    @Order(30)
    void shouldCreateMultipleExecutionsWithSameAuthenticator() {
        doImport("30_update_realm__add_multiple_executions_with_same_authenticator.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "with-two-ids");
        assertThat(flow.getDescription(), is("my browser based authentication"));
        assertThat(flow.isBuiltIn(), is(false));
        assertThat(flow.isTopLevel(), is(true));


        List<AuthenticationExecutionExportRepresentation> execution;
        execution = getExecutionFromFlow(flow, "identity-provider-redirector");
        assertThat(execution, hasSize(2));

        List<AuthenticationExecutionExportRepresentation> executionsId1 = execution.stream()
                .filter((config) -> config.getAuthenticatorConfig() != null)
                .filter((config) -> config.getAuthenticatorConfig().equals("id1"))
                .collect(Collectors.toList());

        assertThat(executionsId1, hasSize(1));
        assertThat(executionsId1.get(0).getAuthenticator(), is("identity-provider-redirector"));
        assertThat(executionsId1.get(0).getAuthenticatorConfig(), is("id1"));
        assertThat(executionsId1.get(0).getRequirement(), is("ALTERNATIVE"));

        List<AuthenticationExecutionExportRepresentation> executionsId2 = execution.stream()
                .filter((config) -> config.getAuthenticatorConfig() != null)
                .filter((config) -> config.getAuthenticatorConfig().equals("id2"))
                .collect(Collectors.toList());

        assertThat(executionsId2, hasSize(1));
        assertThat(executionsId2.get(0).getAuthenticator(), is("identity-provider-redirector"));
        assertThat(executionsId2.get(0).getAuthenticatorConfig(), is("id2"));
        assertThat(executionsId2.get(0).getRequirement(), is("ALTERNATIVE"));

        assertThat(executionsId2.get(0).getPriority(), greaterThan(executionsId1.get(0).getPriority()));

        List<AuthenticatorConfigRepresentation> authConfig;
        authConfig = getAuthenticatorConfig(realm, "id1");
        assertThat(authConfig, hasSize(1));
        assertThat(authConfig.get(0).getAlias(), is("id1"));
        assertThat(authConfig.get(0).getConfig(), hasEntry(is("defaultProvider"), is("id1")));

        authConfig = getAuthenticatorConfig(realm, "id2");
        assertThat(authConfig, hasSize(1));
        assertThat(authConfig.get(0).getAlias(), is("id2"));
        assertThat(authConfig.get(0).getConfig(), hasEntry(is("defaultProvider"), is("id2")));
    }

    @Test
    @Order(31)
    void shouldUpdateMultipleExecutionsWithSameAuthenticator() {
        doImport("31_update_realm__update_multiple_executions_with_same_authenticator.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "with-two-ids");
        assertThat(flow.getDescription(), is("my browser based authentication"));
        assertThat(flow.isBuiltIn(), is(false));
        assertThat(flow.isTopLevel(), is(true));


        List<AuthenticationExecutionExportRepresentation> execution;
        execution = getExecutionFromFlow(flow, "identity-provider-redirector");
        assertThat(execution, hasSize(3));

        List<AuthenticatorConfigRepresentation> authConfig;
        authConfig = getAuthenticatorConfig(realm, "id1");
        assertThat(authConfig, hasSize(2));
        assertThat(authConfig.get(0).getAlias(), is("id1"));
        assertThat(authConfig.get(0).getConfig(), hasEntry(is("defaultProvider"), is("id1")));
        assertThat(authConfig.get(1).getAlias(), is("id1"));
        assertThat(authConfig.get(1).getConfig(), hasEntry(is("defaultProvider"), is("id1")));

        authConfig = getAuthenticatorConfig(realm, "id2");
        assertThat(authConfig, hasSize(1));
        assertThat(authConfig.get(0).getAlias(), is("id2"));
        assertThat(authConfig.get(0).getConfig(), hasEntry(is("defaultProvider"), is("id2")));
    }

    @Test
    @Order(32)
    void shouldUpdateMultipleExecutionsWithSameAuthenticatorWithConfig() {
        doImport("32_update_realm__update_multiple_executions_with_same_authenticator_with_config.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "with-two-ids");
        assertThat(flow.getDescription(), is("my browser based authentication"));
        assertThat(flow.isBuiltIn(), is(false));
        assertThat(flow.isTopLevel(), is(true));


        List<AuthenticationExecutionExportRepresentation> execution;
        execution = getExecutionFromFlow(flow, "identity-provider-redirector");
        assertThat(execution, hasSize(3));

        List<AuthenticatorConfigRepresentation> authConfig;
        authConfig = getAuthenticatorConfig(realm, "id1");
        assertThat(authConfig, hasSize(2));
        assertThat(authConfig.get(0).getAlias(), is("id1"));
        assertThat(authConfig.get(0).getConfig(), hasEntry(is("defaultProvider"), is("id2")));
        assertThat(authConfig.get(1).getAlias(), is("id1"));
        assertThat(authConfig.get(1).getConfig(), hasEntry(is("defaultProvider"), is("id2")));

        authConfig = getAuthenticatorConfig(realm, "id2");
        assertThat(authConfig, hasSize(1));
        assertThat(authConfig.get(0).getAlias(), is("id2"));
        assertThat(authConfig.get(0).getConfig(), hasEntry(is("defaultProvider"), is("id4")));
    }

    @Test
    @Order(40)
    void shouldFailWhenTryingToUpdateBuiltInFlow() {
        RealmImport foundImport = getImport("40_update_realm__try-to-update-built-in-flow.json");

        InvalidImportException thrown = assertThrows(InvalidImportException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), is("Unable to update flow 'my auth flow with execution-flows' in realm 'realmWithFlow': Change built-in flag is not possible"));
    }

    @Test
    @Order(41)
    void shouldFailWhenTryingToUpdateWithNonExistingFlow() {
        RealmImport foundImport = getImport("41_update_realm__try-to-update-with-non-existing-flow.json");

        ImportProcessingException thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), is("Non-toplevel flow not found: non existing sub flow"));
    }

    @Test
    @Order(42)
    void shouldUpdateTopLevelBuiltinFLow() {
        doImport("42_update_realm__update_builtin-top-level-flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "saml ecp");
        assertThat(flow.getDescription(), is("SAML ECP Profile Authentication Flow"));
        assertThat(flow.isBuiltIn(), is(true));
        assertThat(flow.isTopLevel(), is(true));

        List<AuthenticationExecutionExportRepresentation> execution = getExecutionFromFlow(flow, "http-basic-authenticator");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("http-basic-authenticator"));
        assertThat(execution.get(0).getRequirement(), is("CONDITIONAL"));
        assertThat(execution.get(0).getPriority(), is(10));
        assertThat(execution.get(0).isUserSetupAllowed(), is(false));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));
    }

    @Test
    @Order(43)
    void shouldUpdateNonTopLevelBuiltinFLow() {
        doImport("43_update_realm__update_builtin-non-top-level-flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "registration form");
        if (VersionUtil.ge(KEYCLOAK_VERSION, "11")) {
            assertThat(flow.getDescription(), is("updated registration form"));
        } else {
            assertThat(flow.getDescription(), is("registration form"));
        }
        assertThat(flow.isBuiltIn(), is(true));
        assertThat(flow.isTopLevel(), is(false));

        List<AuthenticationExecutionExportRepresentation> execution = getExecutionFromFlow(flow, "registration-recaptcha-action");
        assertThat(execution.get(0).getAuthenticator(), is("registration-recaptcha-action"));
        assertThat(execution.get(0).getRequirement(), is("REQUIRED"));
        assertThat(execution.get(0).getPriority(), is(60));
        assertThat(execution.get(0).isUserSetupAllowed(), is(false));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));
    }

    @Test
    @Order(44)
    void shouldNotUpdateFlowWithBuiltInFalse() {
        RealmImport foundImport = getImport("44_update_realm__try-to-update-flow-set-builtin-false.json");

        InvalidImportException thrown = assertThrows(InvalidImportException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), is("Unable to recreate flow 'saml ecp' in realm 'realmWithFlow': Deletion or creation of built-in flows is not possible"));
    }

    @Test
    @Order(45)
    void shouldNotUpdateFlowWithBuiltInTrue() {
        RealmImport foundImport = getImport("45_update_realm__try-to-update-flow-set-builtin-true.json");

        InvalidImportException thrown = assertThrows(InvalidImportException.class, () -> realmImportService.doImport(foundImport));

        assertThat(thrown.getMessage(), is("Unable to update flow 'my auth flow' in realm 'realmWithFlow': Change built-in flag is not possible"));
    }

    @Test
    @Order(46)
    void shouldNotCreateBuiltInFlow() {
        RealmImport foundImport = getImport("46_update_realm__try-to-create-builtin-flow.json");

        if (VersionUtil.ge(KEYCLOAK_VERSION, "11")) {
            ImportProcessingException thrown = assertThrows(ImportProcessingException.class, () -> realmImportService.doImport(foundImport));

            assertThat(thrown.getMessage(), is("Cannot update top-level-flow 'saml ecp' in realm 'realmWithFlow'."));
        }
    }

    @Test
    @Order(47)
    void shouldUpdateRealmUpdateBuiltInFlowWithPseudoId() {
        doImport("47_update_realm__update-builtin-flow-with-pseudo-id.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));
    }

    @Test
    @Order(50)
    void shouldRemoveNonTopLevelFlow() {
        doImport("50_update_realm__update-remove-non-top-level-flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        AuthenticationFlowRepresentation flow;
        flow = getAuthenticationFlow(realm, "my auth flow");
        assertThat(flow.getDescription(), is("My auth flow for testing with pseudo-id"));
        assertThat(flow.getProviderId(), is("basic-flow"));
        assertThat(flow.isBuiltIn(), is(false));
        assertThat(flow.isTopLevel(), is(true));

        List<AuthenticationExecutionExportRepresentation> executions = flow.getAuthenticationExecutions();
        assertThat(executions, hasSize(1));

        List<AuthenticationExecutionExportRepresentation> execution;
        execution = getExecutionFromFlow(flow, "http-basic-authenticator");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("http-basic-authenticator"));
        assertThat(execution.get(0).getRequirement(), is("DISABLED"));
        assertThat(execution.get(0).getPriority(), is(0));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));

        flow = getAuthenticationFlow(realm, "my registration");
        assertThat(flow.getDescription(), is("My registration flow"));
        assertThat(flow.getProviderId(), is("basic-flow"));
        assertThat(flow.isBuiltIn(), is(false));
        assertThat(flow.isTopLevel(), is(true));

        List<AuthenticationExecutionExportRepresentation> executionFlows = flow.getAuthenticationExecutions();
        assertThat(executionFlows, hasSize(1));

        execution = getExecutionFromFlow(flow, "registration-page-form");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("registration-page-form"));
        assertThat(execution.get(0).getRequirement(), is("REQUIRED"));
        assertThat(execution.get(0).getPriority(), is(0));
        assertThat(execution.get(0).isAutheticatorFlow(), is(true));

        AuthenticationFlowRepresentation nonTopLevelFlow = getAuthenticationFlow(realm, "my registration form");

        List<AuthenticationExecutionExportRepresentation> nonTopLevelFlowExecutions = nonTopLevelFlow.getAuthenticationExecutions();
        assertThat(nonTopLevelFlowExecutions, hasSize(2));

        execution = getExecutionFromFlow(nonTopLevelFlow, "registration-profile-action");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("registration-profile-action"));
        assertThat(execution.get(0).getRequirement(), is("REQUIRED"));
        assertThat(execution.get(0).getPriority(), is(0));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));

        execution = getExecutionFromFlow(nonTopLevelFlow, "registration-user-creation");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("registration-user-creation"));
        assertThat(execution.get(0).getRequirement(), is("REQUIRED"));
        assertThat(execution.get(0).getPriority(), is(1));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));
    }

    @Test
    @Order(51)
    void shouldSkipRemoveTopLevelFlow() {
        doImport("51_update_realm__skip-remove-top-level-flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        AuthenticationFlowRepresentation flow;
        flow = getAuthenticationFlow(realm, "my auth flow");
        assertThat(flow.getDescription(), is("My auth flow for testing with pseudo-id"));
        assertThat(flow.getProviderId(), is("basic-flow"));
        assertThat(flow.isBuiltIn(), is(false));
        assertThat(flow.isTopLevel(), is(true));

        List<AuthenticationExecutionExportRepresentation> executions = flow.getAuthenticationExecutions();
        assertThat(executions, hasSize(1));

        List<AuthenticationExecutionExportRepresentation> execution = getExecutionFromFlow(flow, "http-basic-authenticator");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("http-basic-authenticator"));
        assertThat(execution.get(0).getRequirement(), is("DISABLED"));
        assertThat(execution.get(0).getPriority(), is(0));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));

        flow = getAuthenticationFlow(realm, "my registration");
        assertThat(flow.getDescription(), is("My registration flow"));
        assertThat(flow.getProviderId(), is("basic-flow"));
        assertThat(flow.isBuiltIn(), is(false));
        assertThat(flow.isTopLevel(), is(true));

        List<AuthenticationExecutionExportRepresentation> executionFlows = flow.getAuthenticationExecutions();
        assertThat(executionFlows, hasSize(1));

        execution = getExecutionFromFlow(flow, "registration-page-form");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("registration-page-form"));
        assertThat(execution.get(0).getRequirement(), is("REQUIRED"));
        assertThat(execution.get(0).getPriority(), is(0));
        assertThat(execution.get(0).isAutheticatorFlow(), is(true));

        AuthenticationFlowRepresentation nonTopLevelFlow = getAuthenticationFlow(realm, "my registration form");

        List<AuthenticationExecutionExportRepresentation> nonTopLevelFlowExecutions = nonTopLevelFlow.getAuthenticationExecutions();
        assertThat(nonTopLevelFlowExecutions, hasSize(2));

        execution = getExecutionFromFlow(nonTopLevelFlow, "registration-profile-action");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("registration-profile-action"));
        assertThat(execution.get(0).getRequirement(), is("REQUIRED"));
        assertThat(execution.get(0).getPriority(), is(0));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));

        execution = getExecutionFromFlow(nonTopLevelFlow, "registration-user-creation");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("registration-user-creation"));
        assertThat(execution.get(0).getRequirement(), is("REQUIRED"));
        assertThat(execution.get(0).getPriority(), is(1));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));
    }

    @Test
    @Order(52)
    void shouldRemoveTopLevelFlow() {
        doImport("52_update_realm__update-remove-top-level-flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my auth flow");
        assertThat(flow.getDescription(), is("My auth flow for testing with pseudo-id"));
        assertThat(flow.getProviderId(), is("basic-flow"));
        assertThat(flow.isBuiltIn(), is(false));
        assertThat(flow.isTopLevel(), is(true));

        List<AuthenticationExecutionExportRepresentation> executions = flow.getAuthenticationExecutions();
        assertThat(executions, hasSize(1));

        List<AuthenticationExecutionExportRepresentation> execution = getExecutionFromFlow(flow, "http-basic-authenticator");
        assertThat(execution, hasSize(1));
        assertThat(execution.get(0).getAuthenticator(), is("http-basic-authenticator"));
        assertThat(execution.get(0).getRequirement(), is("DISABLED"));
        assertThat(execution.get(0).getPriority(), is(0));
        assertThat(execution.get(0).isAutheticatorFlow(), is(false));

        AuthenticationFlowRepresentation deletedTopLevelFlow = getAuthenticationFlow(realm, "my registration");

        assertThat(deletedTopLevelFlow, is(nullValue()));

        deletedTopLevelFlow = getAuthenticationFlow(realm, "my registration from");
        assertThat(deletedTopLevelFlow, is(nullValue()));
    }

    @Test
    @Order(53)
    void shouldRemoveAllTopLevelFlow() {
        doImport("53_update_realm__update-remove-all-top-level-flow.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        AuthenticationFlowRepresentation deletedTopLevelFlow;
        deletedTopLevelFlow = getAuthenticationFlow(realm, "my auth flow");
        assertThat(deletedTopLevelFlow, is(nullValue()));

        deletedTopLevelFlow = getAuthenticationFlow(realm, "my registration");
        assertThat(deletedTopLevelFlow, is(nullValue()));

        deletedTopLevelFlow = getAuthenticationFlow(realm, "my registration from");
        assertThat(deletedTopLevelFlow, is(nullValue()));

        List<AuthenticationFlowRepresentation> allTopLevelFlow = realm.getAuthenticationFlows()
                .stream().filter(e -> !e.isBuiltIn())
                .collect(Collectors.toList());

        assertThat(allTopLevelFlow, is(empty()));
    }

    @Test
    @Order(61)
    void shouldAddAndSetFirstBrokerLoginFlowForIdentityProvider() {
        doImport("61_update_realm__add_and_set_custom_first-broker-login-flow_for_identity-provider.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        IdentityProviderRepresentation identityProviderRepresentation = realm.getIdentityProviders().stream()
                .filter(idp -> Objects.equals(idp.getAlias(), "keycloak-oidc")).findFirst().orElse(null);

        assertThat(identityProviderRepresentation, is(not(nullValue())));
        assertThat(identityProviderRepresentation.getFirstBrokerLoginFlowAlias(), is("my-first-broker-login"));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my-first-broker-login");
        assertThat(flow.getDescription(), is("custom first broker login"));
    }

    @Test
    @Order(62)
    void shouldChangeFirstBrokerLoginFlowForIdentityProvider() {
        doImport("62_update_realm__change_custom_first-broker-login-flow_for_identity-provider.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).partialExport(true, true);

        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        IdentityProviderRepresentation identityProviderRepresentation = realm.getIdentityProviders().stream()
                .filter(idp -> Objects.equals(idp.getAlias(), "keycloak-oidc")).findFirst().orElse(null);

        assertThat(identityProviderRepresentation, is(not(nullValue())));
        assertThat(identityProviderRepresentation.getFirstBrokerLoginFlowAlias(), is("my-first-broker-login"));

        AuthenticationFlowRepresentation flow = getAuthenticationFlow(realm, "my-first-broker-login");
        assertThat(flow.getDescription(), is("custom changed first broker login"));
    }

    private List<AuthenticationExecutionExportRepresentation> getExecutionFromFlow(AuthenticationFlowRepresentation flow, String executionAuthenticator) {
        List<AuthenticationExecutionExportRepresentation> executions = flow.getAuthenticationExecutions();

        return executions.stream()
                .filter(e -> e.getAuthenticator().equals(executionAuthenticator))
                .collect(Collectors.toList());
    }

    private AuthenticationExecutionExportRepresentation getExecutionFlowFromFlow(AuthenticationFlowRepresentation flow, String subFlow) {
        List<AuthenticationExecutionExportRepresentation> executions = flow.getAuthenticationExecutions();

        return executions.stream()
                .filter(f -> f.getFlowAlias() != null && f.getFlowAlias().equals(subFlow))
                .findFirst()
                .orElse(null);
    }

    private AuthenticationFlowRepresentation getAuthenticationFlow(RealmRepresentation realm, String flowAlias) {
        List<AuthenticationFlowRepresentation> authenticationFlows = realm.getAuthenticationFlows();
        return authenticationFlows.stream()
                .filter(f -> f.getAlias().equals(flowAlias))
                .findFirst()
                .orElse(null);
    }
}
