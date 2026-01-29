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
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.repository.OrganizationRepository;
import de.adorsys.keycloak.config.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrganizationImportServiceTest extends AbstractImportTest {

    private final OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ImportConfigProperties importConfigProperties = mock(ImportConfigProperties.class, RETURNS_DEEP_STUBS);

    private final OrganizationImportService organizationImportService =
            new OrganizationImportService(organizationRepository, userRepository, importConfigProperties);

    private RealmImport realmImport;
    private final String realmName = "org-feature-test";

    @BeforeEach
    void setUp() {
        realmImport = new RealmImport();
        realmImport.setRealm(realmName);
        this.resourcePath = "import-files/organizations";
    }

    @Nested
    class ImportOrganizations {

        @Test
        void shouldCreateOrganization() {
            OrganizationRepresentation org = new OrganizationRepresentation();
            org.setAlias("test-org");
            org.setName("Test Org");

            realmImport.setOrganizations(List.of(org));

            when(organizationRepository.search(realmName, "test-org"))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(org));

            organizationImportService.doImport(realmImport);

            verify(organizationRepository).create(eq(realmName), eq(org));
        }

        @Test
        void shouldUpdateOrganization() {
            OrganizationRepresentation org = new OrganizationRepresentation();
            org.setAlias("test-org");
            org.setName("Updated Org");

            OrganizationRepresentation existingOrg = new OrganizationRepresentation();
            existingOrg.setId("org-id");
            existingOrg.setAlias("test-org");
            existingOrg.setName("Old Org");

            realmImport.setOrganizations(List.of(org));

            when(organizationRepository.search(realmName, "test-org")).thenReturn(Optional.of(existingOrg));

            organizationImportService.doImport(realmImport);

            verify(organizationRepository).update(eq(realmName), any(OrganizationRepresentation.class));
        }

        @Test
        void shouldAddIdentityProvider() throws IOException {
            // Load the actual test file
            realmImport = getFirstImport("01_create_organization.json");
            
            // Setup mocks for the tech-startup organization with google IdP
            OrganizationRepresentation techStartup = new OrganizationRepresentation();
            techStartup.setId("tech-startup-id");
            techStartup.setAlias("tech-startup");
            
            when(organizationRepository.search(realmName, "tech-startup")).thenReturn(Optional.of(techStartup));
            when(organizationRepository.getIdentityProviders(realmName, "tech-startup-id")).thenReturn(List.of());

            organizationImportService.doImport(realmImport);

            verify(organizationRepository).addIdentityProvider(realmName, "tech-startup-id", "google");
        }

        @Test
        void shouldAddMembers() throws IOException {
            // Load the actual test file
            realmImport = getFirstImport("01_create_organization.json");
            
            // Setup mocks for the acme organization with members
            OrganizationRepresentation acme = new OrganizationRepresentation();
            acme.setId("acme-id");
            acme.setAlias("acme");
            
            when(organizationRepository.search(realmName, "acme")).thenReturn(Optional.of(acme));
            when(organizationRepository.getMembers(realmName, "acme-id")).thenReturn(List.of());
            
            // Mock that users exist
            UserRepresentation myuser = new UserRepresentation();
            myuser.setId("myuser-id");
            myuser.setUsername("myuser");
            when(userRepository.search(realmName, "myuser")).thenReturn(Optional.of(myuser));

            organizationImportService.doImport(realmImport);

            verify(organizationRepository).addMember(realmName, "acme-id", "myuser-id");
        }
    }
}
