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

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.repository.AuthenticationFlowRepository;
import de.adorsys.keycloak.config.repository.ClientRepository;
import de.adorsys.keycloak.config.repository.ClientScopeRepository;
import de.adorsys.keycloak.config.service.state.StateService;
import de.adorsys.keycloak.config.util.ClientScopeUtil;
import de.adorsys.keycloak.config.util.CloneUtil;
import de.adorsys.keycloak.config.util.KeycloakUtil;
import de.adorsys.keycloak.config.util.ProtocolMapperUtil;
import de.adorsys.keycloak.config.util.ResponseUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.ws.rs.WebApplicationException;

import static de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues.FULL;
import static java.lang.Boolean.TRUE;

@Service
@SuppressWarnings({"java:S1192"})
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class ClientImportService {
    private static final Logger logger = LoggerFactory.getLogger(ClientImportService.class);

    private static final String[] propertiesWithDependencies = new String[]{
            "authenticationFlowBindingOverrides",
            "authorizationSettings",
    };

    public static final String REALM_MANAGEMENT_CLIENT_ID = "realm-management";

    private final ClientRepository clientRepository;
    private final ClientScopeRepository clientScopeRepository;
    private final AuthenticationFlowRepository authenticationFlowRepository;
    private final ImportConfigProperties importConfigProperties;
    private final StateService stateService;

    @Autowired
    public ClientImportService(
            ClientRepository clientRepository,
            ClientScopeRepository clientScopeRepository,
            AuthenticationFlowRepository authenticationFlowRepository,
            ImportConfigProperties importConfigProperties,
            StateService stateService) {
        this.clientRepository = clientRepository;
        this.clientScopeRepository = clientScopeRepository;
        this.authenticationFlowRepository = authenticationFlowRepository;
        this.importConfigProperties = importConfigProperties;
        this.stateService = stateService;
    }

    public void doImport(RealmImport realmImport) {
        List<ClientRepresentation> clients = realmImport.getClients();
        if (clients == null) {
            return;
        }

        if (importConfigProperties.getManaged().getClient() == FULL) {
            deleteClientsMissingInImport(realmImport, clients);
        }
        createOrUpdateClients(realmImport, clients);
    }

    public void doImportDependencies(RealmImport realmImport) {
        List<ClientRepresentation> clients = realmImport.getClients();
        if (clients == null) {
            return;
        }

        updateClientAuthenticationFlowBindingOverrides(realmImport, clients);
    }

    private void createOrUpdateClients(
            RealmImport realmImport,
            List<ClientRepresentation> clients
    ) {
        Consumer<ClientRepresentation> loop = client -> createOrUpdateClient(realmImport, client);
        if (importConfigProperties.isParallel()) {
            clients.parallelStream().forEach(loop);
        } else {
            clients.forEach(loop);
        }
    }

    private void deleteClientsMissingInImport(
            RealmImport realmImport,
            List<ClientRepresentation> clients
    ) {
        Set<String> importedClients = clients.stream()
                .map(ClientRepresentation::getClientId)
                .collect(Collectors.toSet());

        boolean isState = importConfigProperties.getRemoteState().isEnabled();
        final List<String> stateClients = stateService.getClients();

        List<ClientRepresentation> clientsToRemove = clientRepository.getAll(realmImport.getRealm())
                .stream()
                .filter(client -> !KeycloakUtil.isDefaultClient(client)
                        && !importedClients.contains(client.getClientId())
                        && (!isState || stateClients.contains(client.getClientId()))
                        && !(Objects.equals(realmImport.getRealm(), "master")
                        && client.getClientId().endsWith("-realm"))
                )
                .toList();

        for (ClientRepresentation clientToRemove : clientsToRemove) {
            logger.debug("Remove client '{}' in realm '{}'", clientToRemove.getClientId(), realmImport.getRealm());
            clientRepository.remove(realmImport.getRealm(), clientToRemove);
        }
    }

    private void createOrUpdateClient(
            RealmImport realmImport,
            ClientRepresentation client
    ) {
        String realmName = realmImport.getRealm();

        // https://github.com/keycloak/keycloak/blob/74695c02423345dab892a0808bf9203c3f92af7c/server-spi-private/src/main/java/org/keycloak/models/utils/RepresentationToModel.java#L2878-L2881
        if (importConfigProperties.isValidate()
                && client.getAuthorizationSettings() != null && !REALM_MANAGEMENT_CLIENT_ID.equals(client.getClientId())) {
            if (TRUE.equals(client.isBearerOnly()) || TRUE.equals(client.isPublicClient())) {
                throw new ImportProcessingException(
                        "Unsupported authorization settings for client '%s' in realm '%s': client must be confidential.",
                        getClientIdentifier(client), realmName
                );
            }

            if (!TRUE.equals(client.isServiceAccountsEnabled())) {
                throw new ImportProcessingException(
                        "Unsupported authorization settings for client '%s' in realm '%s': serviceAccountsEnabled must be 'true'.",
                        getClientIdentifier(client), realmName
                );
            }
        }

        Optional<ClientRepresentation> existingClient;
        if (client.getClientId() != null) {
            existingClient = clientRepository.searchByClientId(realmName, client.getClientId());
        } else if (client.getName() != null) {
            existingClient = clientRepository.searchByName(realmName, client.getName());
        } else {
            throw new ImportProcessingException("clients require client id or name.");
        }

        if (existingClient.isPresent()) {
            updateClientIfNeeded(realmName, client, existingClient.get());
        } else {
            logger.debug("Create client '{}' in realm '{}'", getClientIdentifier(client), realmName);
            createClient(realmName, client);
        }
    }

    private void updateClientIfNeeded(
            String realmName,
            ClientRepresentation clientToUpdate,
            ClientRepresentation existingClient
    ) {
        String[] propertiesToIgnore = ArrayUtils.addAll(propertiesWithDependencies, "id", "access");
        ClientRepresentation mergedClient = CloneUtil.patch(existingClient, clientToUpdate, propertiesToIgnore);
        String clientIdentifier = getClientIdentifier(clientToUpdate);

        if (!isClientEqual(realmName, existingClient, mergedClient)) {
            logger.debug("Update client '{}' in realm '{}'", clientIdentifier, realmName);
            updateClient(realmName, mergedClient);
            updateClientDefaultOptionalClientScopes(realmName, mergedClient, existingClient);
        } else {
            logger.debug("No need to update client '{}' in realm '{}'", clientIdentifier, realmName);
        }
    }

    private void createClient(String realmName, ClientRepresentation client) {
        ClientRepresentation clientToImport = CloneUtil.deepClone(
                client, ClientRepresentation.class, propertiesWithDependencies
        );
        clientRepository.create(realmName, clientToImport);
    }

    private boolean isClientEqual(
            String realmName,
            ClientRepresentation existingClient,
            ClientRepresentation patchedClient
    ) {
        String[] propertiesToIgnore = ArrayUtils.addAll(
                propertiesWithDependencies, "id", "secret", "access", "protocolMappers", "defaultClientScopes", "optionalClientScopes"
        );

        if (!CloneUtil.deepEquals(existingClient, patchedClient, propertiesToIgnore)) {
            return false;
        }

        if (!CollectionUtil.collectionEquals(patchedClient.getDefaultClientScopes(), existingClient.getDefaultClientScopes())
                || !CollectionUtil.collectionEquals(patchedClient.getOptionalClientScopes(), existingClient.getOptionalClientScopes())) {
            return false;
        }

        boolean areProtocolMapperDifferent = !ProtocolMapperUtil.areProtocolMappersEqual(
                patchedClient.getProtocolMappers(),
                existingClient.getProtocolMappers()
        );

        if (areProtocolMapperDifferent) {
            return false;
        }

        String patchedClientSecret = patchedClient.getSecret();
        if (patchedClientSecret == null) {
            return true;
        }

        String clientSecret = clientRepository.getClientSecret(realmName, patchedClient.getClientId());
        return Objects.equals(clientSecret, patchedClientSecret);
    }

    private void updateClient(
            String realmName,
            ClientRepresentation patchedClient
    ) {
        try {
            clientRepository.update(realmName, patchedClient);
        } catch (WebApplicationException error) {
            String errorMessage = ResponseUtil.getErrorMessage(error);
            throw new ImportProcessingException(
                    String.format("Cannot update client '%s' in realm '%s': %s",
                            getClientIdentifier(patchedClient), realmName, errorMessage
                    ),
                    error
            );
        }
    }

    private void updateClientAuthenticationFlowBindingOverrides(
            RealmImport realmImport,
            List<ClientRepresentation> clients
    ) {
        String realmName = realmImport.getRealm();

        for (ClientRepresentation client : clients) {
            ClientRepresentation existingClient;
            if (client.getClientId() != null) {
                existingClient = clientRepository.getByClientId(realmName, client.getClientId());
            } else if (client.getName() != null) {
                existingClient = clientRepository.getByName(realmName, client.getName());
            } else {
                throw new ImportProcessingException("clients require client id or name.");
            }

            updateAuthenticationFlowBindingOverrides(
                    realmName, existingClient, client.getAuthenticationFlowBindingOverrides()
            );
        }
    }

    private void updateAuthenticationFlowBindingOverrides(
            String realmName,
            ClientRepresentation existingClient,
            Map<String, String> authenticationFlowBindingOverrides
    ) {
        boolean isEqual = Objects.equals(
                authenticationFlowBindingOverrides, existingClient.getAuthenticationFlowBindingOverrides()
        );

        if (isEqual) return;

        Map<String, String> authFlowUpdates = new HashMap<>(existingClient.getAuthenticationFlowBindingOverrides());

        // Be sure that all existing values will be cleared
        // See: https://github.com/keycloak/keycloak/blob/790b549cf99dbbba109e145654ee4a4cd1a047c9/server-spi-private/src/main/java/org/keycloak/models/utils/RepresentationToModel.java#L1516
        authFlowUpdates.replaceAll((k, v) -> v = null);

        // Compute new values
        if (authenticationFlowBindingOverrides != null) {
            for (Map.Entry<String, String> override : authenticationFlowBindingOverrides.entrySet()) {
                if (
                        override.getValue() == null || override.getValue().isEmpty()
                                || authenticationFlowRepository.exists(realmName, override.getValue())
                ) {
                    authFlowUpdates.put(override.getKey(), override.getValue());
                } else {
                    String flowId = authenticationFlowRepository.getByAlias(realmName, override.getValue()).getId();
                    authFlowUpdates.put(override.getKey(), flowId);
                }
            }
        }

        existingClient.setAuthenticationFlowBindingOverrides(authFlowUpdates);
        updateClient(realmName, existingClient);
    }

    private void updateClientDefaultOptionalClientScopes(
            String realmName,
            ClientRepresentation client,
            ClientRepresentation existingClient
    ) {
        final List<String> defaultClientScopeNamesToAdd = ClientScopeUtil
                .estimateClientScopesToAdd(client.getDefaultClientScopes(), existingClient.getDefaultClientScopes());
        final List<String> defaultClientScopeNamesToRemove = ClientScopeUtil
                .estimateClientScopesToRemove(client.getDefaultClientScopes(), existingClient.getDefaultClientScopes());


        final List<String> optionalClientScopeNamesToAdd = ClientScopeUtil
                .estimateClientScopesToAdd(client.getOptionalClientScopes(), existingClient.getOptionalClientScopes());
        final List<String> optionalClientScopeNamesToRemove = ClientScopeUtil
                .estimateClientScopesToRemove(client.getOptionalClientScopes(), existingClient.getOptionalClientScopes());

        if (!defaultClientScopeNamesToRemove.isEmpty()) {
            logger.debug("Remove default client scopes '{}' for client '{}' in realm '{}'",
                    defaultClientScopeNamesToRemove, client.getClientId(), realmName);

            List<ClientScopeRepresentation> defaultClientScopesToRemove = clientScopeRepository
                    .getListByNames(realmName, defaultClientScopeNamesToRemove);

            clientRepository.removeDefaultClientScopes(realmName, client.getClientId(), defaultClientScopesToRemove);
        }

        if (!optionalClientScopeNamesToRemove.isEmpty()) {
            logger.debug("Remove optional client scopes '{}' for client '{}' in realm '{}'",
                    optionalClientScopeNamesToRemove, client.getClientId(), realmName);

            List<ClientScopeRepresentation> optionalClientScopesToRemove = clientScopeRepository
                    .getListByNames(realmName, optionalClientScopeNamesToRemove);

            clientRepository.removeOptionalClientScopes(realmName, client.getClientId(), optionalClientScopesToRemove);
        }


        if (!defaultClientScopeNamesToAdd.isEmpty()) {
            logger.debug("Add default client scopes '{}' for client '{}' in realm '{}'",
                    defaultClientScopeNamesToAdd, client.getClientId(), realmName);

            List<ClientScopeRepresentation> defaultClientScopesToAdd = clientScopeRepository
                    .getListByNames(realmName, defaultClientScopeNamesToAdd);

            clientRepository.addDefaultClientScopes(realmName, client.getClientId(), defaultClientScopesToAdd);
        }

        if (!optionalClientScopeNamesToAdd.isEmpty()) {
            logger.debug("Add optional client scopes '{}' for client '{}' in realm '{}'",
                    optionalClientScopeNamesToAdd, client.getClientId(), realmName);

            List<ClientScopeRepresentation> optionalClientScopesToAdd = clientScopeRepository
                    .getListByNames(realmName, optionalClientScopeNamesToAdd);

            clientRepository.addOptionalClientScopes(realmName, client.getClientId(), optionalClientScopesToAdd);
        }
    }

    private String getClientIdentifier(ClientRepresentation client) {
        return client.getName() != null && !KeycloakUtil.isDefaultClient(client) ? client.getName() : client.getClientId();
    }
}
