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

package de.adorsys.keycloak.config.service.state;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.repository.StateRepository;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.*;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
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

    /**
     * Loads the realm states and preserves it into the realm representation object
     * to prevent state erasure during realm update
     *
     * @param realmImport the {@link RealmRepresentation} instance which will be synchronized with the Keycloak
     */
    public void loadState(RealmRepresentation realmImport) {
        stateRepository.loadCustomAttributes(realmImport);
    }

    public void doImport(RealmImport realmImport) {
        if (!importConfigProperties.getRemoteState().isEnabled()) {
            return;
        }

        setRealmRoles(realmImport);
        setClientRoles(realmImport);
        setClients(realmImport);
        setRequiredActions(realmImport);
        setComponents(realmImport);
        setClientAuthorizationResources(realmImport);
        setMessageBundles(realmImport);

        stateRepository.update(realmImport);
        logger.debug("Updated states of realm '{}'", realmImport.getRealm());
    }

    public List<String> getRealmRoles() {
        return stateRepository.getState("roles-realm");
    }

    private void setRealmRoles(RealmImport realmImport) {
        RolesRepresentation roles = realmImport.getRoles();
        if (roles == null) return;

        List<RoleRepresentation> rolesRealm = roles.getRealm();
        if (rolesRealm == null) return;

        List<String> state = rolesRealm
                .stream()
                .map(RoleRepresentation::getName)
                .toList();

        stateRepository.setState("roles-realm", state);
    }

    private void setClientRoles(RealmImport realmImport) {
        RolesRepresentation roles = realmImport.getRoles();
        if (roles == null) return;

        Map<String, List<RoleRepresentation>> clientRoles = roles.getClient();
        if (clientRoles == null) return;

        for (Map.Entry<String, List<RoleRepresentation>> client : clientRoles.entrySet()) {
            List<String> state = client.getValue()
                    .stream()
                    .map(RoleRepresentation::getName)
                    .toList();

            stateRepository.setState("roles-client-" + client.getKey(), state);
        }
    }

    private void setClientAuthorizationResources(RealmImport realmImport) {
        List<ClientRepresentation> clients = realmImport.getClients();
        if (clients == null) return;

        for (ClientRepresentation client : clients) {
            if (client.getAuthorizationSettings() == null || client.getAuthorizationSettings().getResources() == null) continue;

            String clientKey = client.getClientId() != null ? client.getClientId() : "name:" + client.getName();

            List<String> resourceNames = client.getAuthorizationSettings().getResources()
                    .stream()
                    .map(ResourceRepresentation::getName)
                    .toList();

            stateRepository.setState("resources-client-" + clientKey, resourceNames);
        }
    }

    public List<String> getClientRoles(String client) {
        return stateRepository.getState("roles-client-" + client);
    }

    public List<String> getClientAuthorizationResources(String client) {
        return stateRepository.getState("resources-client-" + client);
    }

    private void setClients(RealmImport realmImport) {
        List<ClientRepresentation> clients = realmImport.getClients();
        if (clients == null) return;

        List<String> state = new ArrayList<>();
        for (ClientRepresentation client : clients) {
            if (client.getClientId() != null) {
                state.add(client.getClientId());
            } else {
                state.add("name:" + client.getName());
            }
        }

        stateRepository.setState("clients", state);
    }

    public List<String> getRequiredActions() {
        return stateRepository.getState("required-actions");
    }

    public List<String> getClients() {
        return stateRepository.getState("clients");
    }

    private void setRequiredActions(RealmImport realmImport) {
        List<RequiredActionProviderRepresentation> requiredActions = realmImport.getRequiredActions();
        if (requiredActions == null) return;

        List<String> state = requiredActions.stream()
                .map(RequiredActionProviderRepresentation::getAlias)
                .toList();

        stateRepository.setState("required-actions", state);
    }

    public List<ComponentRepresentation> getComponents(List<ComponentRepresentation> components, String parentComponentName) {
        List<String> componentsFromState = (parentComponentName != null)
                ? stateRepository.getState("sub-components-" + parentComponentName)
                : stateRepository.getState("components");

        return components.stream()
                .filter(component -> componentsFromState.contains(component.getName()))
                .toList();
    }

    private void setComponents(RealmImport realmImport) {
        MultivaluedHashMap<String, ComponentExportRepresentation> components = realmImport.getComponents();
        if (components == null) return;

        List<String> state = new ArrayList<>();

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
        if (subComponents.isEmpty()) {
            return;
        }

        List<String> state = new ArrayList<>();
        for (Map.Entry<String, List<ComponentExportRepresentation>> subEntry : subComponents.entrySet()) {
            List<String> nameOfSubComponents = subEntry.getValue().stream()
                    .map(ComponentExportRepresentation::getName)
                    .toList();

            state.addAll(nameOfSubComponents);
        }

        stateRepository.setState("sub-components-" + component.getName(), state);
    }


    private void setMessageBundles(RealmImport realmImport) {
        Map<String, Map<String, String>> messageBundles = realmImport.getMessageBundles();
        if (messageBundles == null) return;

        List<String> state = new ArrayList<>(messageBundles.keySet());

        stateRepository.setState("message-bundles", state);
    }

    public List<String> getMessageBundles() {
        return stateRepository.getState("message-bundles");
    }
}
