/*
 * Copyright 2019-2020 adorsys GmbH & Co. KG @ https://adorsys.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package de.adorsys.keycloak.config.service.state;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.repository.StateRepository;
import de.adorsys.keycloak.config.util.AuthenticationFlowUtil;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StateService {
    private static final Logger logger = LoggerFactory.getLogger(StateService.class);

    private final StateRepository stateRepository;
    private final ImportConfigProperties importConfigProperties;

    @Autowired
    public StateService(StateRepository stateRepository, ImportConfigProperties importConfigProperties) {
        this.stateRepository = stateRepository;
        this.importConfigProperties = importConfigProperties;
    }

    public void loadState(RealmImport realmImport) {
        stateRepository.loadCustomAttributes(realmImport.getRealm());
    }

    public void doImport(RealmImport realmImport) {
        if (!importConfigProperties.isState()) {
            return;
        }

        String realm = realmImport.getRealm();

        setClients(realmImport);
        setRequiredActions(realmImport);
        setComponents(realmImport);
        setTopLevelFlows(realmImport);

        stateRepository.update(realmImport);
        logger.debug("Updated states of realm '{}'", realm);
    }

    private void setClients(RealmImport realmImport) {
        List<ClientRepresentation> clients = realmImport.getClients();
        if (clients == null) return;

        List<Object> state = clients.stream().map(ClientRepresentation::getName).collect(Collectors.toList());

        stateRepository.setState("clients", state);
    }

    public List<RequiredActionProviderRepresentation> getRequiredActions(List<RequiredActionProviderRepresentation> requiredActions) {
        List<Object> requiredActionFromState = stateRepository.getState("required-actions");
        return requiredActions.stream()
                .filter(requiredAction -> requiredActionFromState.contains(requiredAction.getAlias()))
                .collect(Collectors.toList());
    }

    private void setRequiredActions(RealmImport realmImport) {


        List<RequiredActionProviderRepresentation> requiredActions = realmImport.getRequiredActions();
        if (requiredActions == null) return;

        List<Object> state = requiredActions.stream().map(RequiredActionProviderRepresentation::getAlias).collect(Collectors.toList());

        stateRepository.setState("required-actions", state);
    }

    public List<ComponentRepresentation> getComponents(List<ComponentRepresentation> components, String parentComponentName) {
        List<Object> componentsFromState = (parentComponentName != null)
                ? stateRepository.getState("sub-components-" + parentComponentName)
                : stateRepository.getState("components");

        return components.stream()
                .filter(component -> componentsFromState.contains(component.getName()))
                .collect(Collectors.toList());
    }

    private void setComponents(RealmImport realmImport) {
        MultivaluedHashMap<String, ComponentExportRepresentation> components = realmImport.getComponents();
        if (components == null) return;

        List<Object> state = new ArrayList<>();

        for (Map.Entry<String, List<ComponentExportRepresentation>> entry : components.entrySet()) {
            for (ComponentExportRepresentation component : entry.getValue()) {
                String componentName = component.getName();
                state.add(componentName);

                setSubComponents(component);
            }
        }

        stateRepository.setState("components", state);
    }

    private void setSubComponents(ComponentExportRepresentation component) {
        MultivaluedHashMap<String, ComponentExportRepresentation> subComponents = component.getSubComponents();
        if (subComponents == null || subComponents.isEmpty()) {
            return;
        }

        List<Object> state = new ArrayList<>();
        for (Map.Entry<String, List<ComponentExportRepresentation>> subEntry : subComponents.entrySet()) {
            List<String> nameOfSubComponents = subEntry.getValue().stream()
                .map(ComponentExportRepresentation::getName)
                .collect(Collectors.toList());

            state.addAll(nameOfSubComponents);
        }

        stateRepository.setState("sub-components-" + component.getName(), state);
    }

    /*
    public List<AuthenticationFlowRepresentation> getTopLevelFlows(List<AuthenticationFlowRepresentation> topLevelFlows) {
        List<Object> topLevelFlowsFromState = stateRepository.getState("top-flows");

        return topLevelFlows.stream()
                .filter(topLevelFlow -> topLevelFlowsFromState.contains(topLevelFlow.getAlias()))
                .collect(Collectors.toList());
    }
    */

    private void setTopLevelFlows(RealmImport realmImport) {
        List<AuthenticationFlowRepresentation> authenticationFlows = realmImport.getAuthenticationFlows();
        if (authenticationFlows == null) return;

        List<AuthenticationFlowRepresentation> topLevelFlows = AuthenticationFlowUtil.getTopLevelFlows(realmImport);

        List<Object> state = topLevelFlows.stream().map(AuthenticationFlowRepresentation::getAlias).collect(Collectors.toList());

        stateRepository.setState("top-flows", state);
    }
}
