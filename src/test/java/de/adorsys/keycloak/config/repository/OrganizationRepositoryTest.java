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

import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.OrganizationsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.OrganizationRepresentation;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrganizationRepositoryTest {

    private final RealmRepository realmRepository = mock(RealmRepository.class);
    private final KeycloakProvider keycloakProvider = mock(KeycloakProvider.class);
    private final KeycloakConfigProperties properties = mock(KeycloakConfigProperties.class);
    private final OrganizationRepository organizationRepository = new OrganizationRepository(realmRepository, keycloakProvider, properties);

    private final String realmName = "test-realm";
    private final RealmResource realmResource = mock(RealmResource.class);
    private final OrganizationsResource organizationsResource = mock(OrganizationsResource.class);

    @BeforeEach
    void setUp() {
        when(realmRepository.getResource(realmName)).thenReturn(realmResource);
        when(realmResource.organizations()).thenReturn(organizationsResource);
    }

    @Test
    void shouldSearchOrganization() {
        OrganizationResource organizationResource = mock(OrganizationResource.class);
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setAlias("test-org");
        org.setId("org-id");

        when(organizationsResource.getAll()).thenReturn(java.util.List.of(org));
        when(organizationsResource.get("org-id")).thenReturn(organizationResource);
        when(organizationResource.toRepresentation()).thenReturn(org);

        Optional<OrganizationRepresentation> result = organizationRepository.search(realmName, "test-org");

        assertThat(result).isPresent();
        assertThat(result.get().getAlias()).isEqualTo("test-org");
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        when(organizationsResource.getAll()).thenReturn(java.util.List.of());

        Optional<OrganizationRepresentation> result = organizationRepository.search(realmName, "test-org");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldCreateOrganization() {
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setAlias("test-org");

        organizationRepository.create(realmName, org);

        verify(organizationsResource).create(org);
    }
}
