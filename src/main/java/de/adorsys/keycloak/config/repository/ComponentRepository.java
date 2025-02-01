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

package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.util.ResponseUtil;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.ComponentsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class ComponentRepository {

    private final RealmRepository realmRepository;

    @Autowired
    public ComponentRepository(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    public String create(String realmName, ComponentRepresentation component) {
        try (Response response = getComponentsResource(realmName).add(component)) {
            return CreatedResponseUtil.getCreatedId(response);
        } catch (WebApplicationException error) {
            String errorMessage = ResponseUtil.getErrorMessage(error);

            throw new ImportProcessingException(
                    String.format(
                            "Cannot create component '%s' in realm '%s': %s",
                            component.getName(), realmName, errorMessage
                    ),
                    error
            );
        }
    }

    public void update(String realmName, ComponentRepresentation component) {
        ComponentsResource componentsResource = getComponentsResource(realmName);
        componentsResource.component(component.getId()).update(component);
    }

    public void delete(String realmName, ComponentRepresentation component) {
        ComponentsResource componentsResource = getComponentsResource(realmName);
        componentsResource.component(component.getId()).remove();
    }

    public ComponentRepresentation getById(String realmName, String componentId) {
        ComponentRepresentation component = getComponentsResource(realmName).component(componentId).toRepresentation();

        if (component == null) {
            throw new KeycloakRepositoryException("Cannot find component by id '%s' in realm '%s' ", componentId, realmName);
        }

        return component;
    }

    public List<ComponentRepresentation> getAll(String realmName, String parentId) {
        if (parentId == null) {
            RealmResource realmResource = realmRepository.getResource(realmName);
            parentId = realmResource.toRepresentation().getId();
        }

        RealmResource realmResource = realmRepository.getResource(realmName);

        List<ComponentRepresentation> subComponents = realmResource.components().query(parentId);

        if (subComponents == null) {
            return Collections.emptyList();
        }

        return subComponents;
    }

    public Optional<ComponentRepresentation> search(String realmName, String type, String subType, String name) {
        return search(realmName, type, subType, name, null);
    }

    public Optional<ComponentRepresentation> search(String realmName, String type, String subType, String name, String parentId) {
        List<ComponentRepresentation> component = getComponentsResource(realmName)
                .query(parentId, type, name);

        return component.stream()
                .filter(item -> Objects.equals(subType, item.getSubType()))
                .findFirst();
    }

    private ComponentsResource getComponentsResource(String realmName) {
        return realmRepository.getResource(realmName).components();
    }
}
