/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2020 adorsys GmbH & Co. KG @ https://adorsys.de
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

package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.util.ResponseUtil;
import de.adorsys.keycloak.config.util.StreamUtil;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.ClientScopesResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ClientScopeRepository {

    private final RealmRepository realmRepository;

    @Autowired
    public ClientScopeRepository(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    public List<ClientScopeRepresentation> getClientScopes(String realm) {
        ClientScopesResource clientScopeResource = realmRepository.loadRealm(realm).clientScopes();
        return clientScopeResource.findAll();
    }

    public ClientScopeRepresentation getClientScopeByName(String realm, String clientScopeName) {
        ClientScopeResource clientScopeResource = loadClientScopeByName(realm, clientScopeName);
        if (clientScopeResource == null) {
            return null;
        }
        return clientScopeResource.toRepresentation();
    }

    public ClientScopeRepresentation getClientScopeById(String realm, String clientScopeId) {
        ClientScopeResource clientScopeResource = loadClientScopeById(realm, clientScopeId);
        if (clientScopeResource == null) {
            return null;
        }
        return clientScopeResource.toRepresentation();
    }

    public void createClientScope(String realm, ClientScopeRepresentation clientScope) {
        Response response = realmRepository.loadRealm(realm).clientScopes().create(clientScope);
        ResponseUtil.throwOnError(response);
    }

    public void deleteClientScope(String realm, String id) {
        ClientScopeResource clientScopeResource = loadClientScopeById(realm, id);
        clientScopeResource.remove();
    }

    public void updateClientScope(String realm, ClientScopeRepresentation clientScope) {
        ClientScopeResource clientScopeResource = loadClientScopeById(realm, clientScope.getId());
        clientScopeResource.update(clientScope);
    }

    public void addProtocolMappers(String realm, String clientScopeId, List<ProtocolMapperRepresentation> protocolMappers) {
        ClientScopeResource clientScopeResource = loadClientScopeById(realm, clientScopeId);
        ProtocolMappersResource protocolMappersResource = clientScopeResource.getProtocolMappers();

        for (ProtocolMapperRepresentation protocolMapper : protocolMappers) {
            Response response = protocolMappersResource.createMapper(protocolMapper);
            ResponseUtil.throwOnError(response);
        }
    }

    public void removeProtocolMappers(String realm, String clientScopeId, List<ProtocolMapperRepresentation> protocolMappers) {
        ClientScopeResource clientScopeResource = loadClientScopeById(realm, clientScopeId);
        ProtocolMappersResource protocolMappersResource = clientScopeResource.getProtocolMappers();

        List<ProtocolMapperRepresentation> existingProtocolMappers = clientScopeResource.getProtocolMappers().getMappers();
        List<ProtocolMapperRepresentation> protocolMapperToRemove = existingProtocolMappers.stream().filter(em -> protocolMappers.stream().anyMatch(m -> Objects.equals(m.getName(), em.getName()))).collect(Collectors.toList());

        for (ProtocolMapperRepresentation protocolMapper : protocolMapperToRemove) {
            protocolMappersResource.delete(protocolMapper.getId());
        }
    }

    public void updateProtocolMappers(String realm, String clientScopeId, List<ProtocolMapperRepresentation> protocolMappers) {
        ClientScopeResource clientScopeResource = loadClientScopeById(realm, clientScopeId);
        ProtocolMappersResource protocolMappersResource = clientScopeResource.getProtocolMappers();

        for (ProtocolMapperRepresentation protocolMapper : protocolMappers) {
            protocolMappersResource.update(protocolMapper.getId(), protocolMapper);
        }
    }

    private ClientScopeResource loadClientScopeByName(String realm, String clientScopeName) {
        Optional<ClientScopeRepresentation> maybeClientScope = tryToFindClientScopeByName(realm, clientScopeName);

        ClientScopeRepresentation existingClientScope = maybeClientScope.orElse(null);

        if (existingClientScope == null) {
            return null;
        }

        return loadClientScopeById(realm, existingClientScope.getId());
    }

    private ClientScopeResource loadClientScopeById(String realm, String clientScopeId) {
        return realmRepository.loadRealm(realm)
                .clientScopes().get(clientScopeId);
    }

    public Optional<ClientScopeRepresentation> tryToFindClientScopeByName(String realm, String clientScopeName) {
        ClientScopesResource clientScopeResource = realmRepository.loadRealm(realm)
                .clientScopes();

        return clientScopeResource.findAll()
                .stream()
                .filter(s -> Objects.equals(s.getName(), clientScopeName))
                .findFirst();
    }

    public List<ClientScopeRepresentation> getDefaultClientScopes(String realm) {
        List<ClientScopeRepresentation> defaultDefaultScopes = realmRepository.loadRealm(realm)
                .getDefaultDefaultClientScopes();
        List<ClientScopeRepresentation> defaultOptionalScopes = realmRepository.loadRealm(realm)
                .getDefaultOptionalClientScopes();
        return Stream.concat(StreamUtil.collectionAsStream(defaultDefaultScopes), StreamUtil.collectionAsStream(defaultOptionalScopes)).collect(Collectors.toList());
    }

}
