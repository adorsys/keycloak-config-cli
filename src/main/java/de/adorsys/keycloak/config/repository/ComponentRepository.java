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

package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.util.ResponseUtil;
import org.keycloak.admin.client.resource.ComponentResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ComponentRepository {

    private final RealmRepository realmRepository;

    @Autowired
    public ComponentRepository(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    public void create(String realm, ComponentRepresentation component) {
        RealmResource realmResource = realmRepository.loadRealm(realm);
        Response response = realmResource.components().add(component);

        ResponseUtil.throwOnError(response);
    }

    public void update(String realm, ComponentRepresentation component) {
        RealmResource realmResource = realmRepository.loadRealm(realm);
        ComponentResource componentResource = realmResource.components().component(component.getId());

        componentResource.update(component);
    }

    public void delete(String realm, ComponentRepresentation component) {
        RealmResource realmResource = realmRepository.loadRealm(realm);
        ComponentResource componentResource = realmResource.components().component(component.getId());

        componentResource.remove();
    }

    public ComponentRepresentation get(String realm, String providerType, String name) {
        RealmResource realmResource = realmRepository.loadRealm(realm);

        List<ComponentRepresentation> realmComponents = realmResource.components().query();

        Optional<ComponentRepresentation> maybeComponent = realmComponents
                .stream()
                .filter(c -> Objects.equals(c.getName(), name))
                .filter(c -> Objects.equals(c.getProviderType(), providerType))
                .findFirst();

        if (maybeComponent.isPresent()) {
            return maybeComponent.get();
        }

        throw new KeycloakRepositoryException("Cannot find component by name '" + name + "' and subtype '" + providerType + "' in realm '" + realm + "' ");
    }

    public List<ComponentRepresentation> getAllComponents(String realm) {
        RealmResource realmResource = realmRepository.loadRealm(realm);
        String realmId = realmResource.toRepresentation().getId();

        List<ComponentRepresentation> subComponents = realmResource.components().query(realmId);

        if (subComponents == null) {
            return Collections.emptyList();
        }

        return subComponents;
    }

    public List<ComponentRepresentation> getAllSubComponentsByParentId(String realm, String parentId) {
        RealmResource realmResource = realmRepository.loadRealm(realm);

        List<ComponentRepresentation> subComponents = realmResource.components().query(parentId);

        if (subComponents == null) {
            return Collections.emptyList();
        }

        return subComponents;
    }

    /**
     * Try to get a component by its properties.
     *
     * @param subType may be null
     */
    public Optional<ComponentRepresentation> tryToGetComponent(String realm, String name, String subType) {
        RealmResource realmResource = realmRepository.loadRealm(realm);

        List<ComponentRepresentation> existingComponents = realmResource.components()
                .query();

        return existingComponents.stream()
                .filter(c -> Objects.equals(c.getName(), name))
                .filter(c -> Objects.equals(c.getSubType(), subType))
                .findFirst();
    }

    public Optional<ComponentRepresentation> tryToGetSubComponent(String realm, String parentId, String subType, String name) {
        RealmResource realmResource = realmRepository.loadRealm(realm);

        Optional<ComponentRepresentation> maybeComponent;
        List<ComponentRepresentation> existingComponents = realmResource.components()
                .query(parentId, subType, name);

        if (existingComponents.isEmpty()) {
            maybeComponent = Optional.empty();
        } else {
            maybeComponent = Optional.of(existingComponents.get(0));
        }

        return maybeComponent;
    }
}
