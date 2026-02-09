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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.repository.organization.OrganizationRepository;
import de.adorsys.keycloak.config.service.organization.DefaultOrganizationImporter;
import de.adorsys.keycloak.config.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class OrganizationFeatureTest {

    private DefaultOrganizationImporter organizationImporter;
    private ObjectMapper objectMapper;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ImportConfigProperties importConfigProperties;

    @BeforeEach
    void setUp() {
        organizationImporter = new DefaultOrganizationImporter(
                organizationRepository,
                userRepository,
                importConfigProperties
        );
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldParseOrganizationJsonFile() throws IOException {
        File jsonFile = new File("src/test/resources/import-files/organizations/01_import_organizations_with_idp.json");
        RealmImport realmImport = objectMapper.readValue(jsonFile, RealmImport.class);

        assertThat(realmImport.getRealm(), is("org-feature-test"));
        assertThat(realmImport.getOrganizations(), hasSize(2));

        OrganizationRepresentation acme = realmImport.getOrganizations().get(0);
        assertThat(acme.getName(), is("Acme Corporation"));
        assertThat(acme.getAlias(), is("acme"));
        assertThat(acme.getRedirectUrl(), is("https://acme.com/redirect"));
        assertThat(acme.getDomains(), hasSize(2));
        assertThat(acme.getAttributes().get("industry"), hasItem("Technology"));

        OrganizationRepresentation techStartup = realmImport.getOrganizations().get(1);
        assertThat(techStartup.getName(), is("Tech Startup"));
        assertThat(techStartup.getAlias(), is("tech-startup"));
    }

    @Test
    void shouldNotImportWhenOrganizationsListIsNull() {
        RealmImport realmImport = new RealmImport();
        realmImport.setOrganizations(null);

        organizationImporter.doImport(realmImport);

        verify(organizationRepository, never()).getAll(anyString());
        verify(organizationRepository, never()).create(anyString(), any(OrganizationRepresentation.class));
    }

    @Test
    void shouldNotImportWhenOrganizationsListIsEmpty() {
        RealmImport realmImport = new RealmImport();
        realmImport.setOrganizations(Collections.emptyList());

        organizationImporter.doImport(realmImport);

        verify(organizationRepository, never()).getAll(anyString());
        verify(organizationRepository, never()).create(anyString(), any(OrganizationRepresentation.class));
    }

    @Test
    void shouldCreateNewOrganizationWhenNotFound() {
        RealmImport realmImport = new RealmImport();
        realmImport.setRealm("test-realm");
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setAlias("test-org");
        org.setName("Test Organization");
        realmImport.setOrganizations(Collections.singletonList(org));

        when(organizationRepository.search("test-realm", "test-org")).thenReturn(Optional.empty());
        when(organizationRepository.getAll("test-realm")).thenReturn(Collections.emptyList());

        ImportConfigProperties.ImportManagedProperties managedProperties = mock(ImportConfigProperties.ImportManagedProperties.class);
        when(importConfigProperties.getManaged()).thenReturn(managedProperties);
        when(managedProperties.getOrganization()).thenReturn(ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues.NO_DELETE);

        organizationImporter.doImport(realmImport);

        verify(organizationRepository).create(eq("test-realm"), eq(org));
        verify(organizationRepository, never()).update(anyString(), any(OrganizationRepresentation.class));
    }

    @Test
    void shouldUpdateExistingOrganization() {
        RealmImport realmImport = new RealmImport();
        realmImport.setRealm("test-realm");
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setAlias("test-org");
        org.setName("Updated Organization");
        realmImport.setOrganizations(Collections.singletonList(org));

        OrganizationRepresentation existingOrg = new OrganizationRepresentation();
        existingOrg.setAlias("test-org");
        existingOrg.setName("Old Organization");

        when(organizationRepository.search("test-realm", "test-org")).thenReturn(Optional.of(existingOrg));
        when(organizationRepository.getAll("test-realm")).thenReturn(Collections.singletonList(existingOrg));

        ImportConfigProperties.ImportManagedProperties managedProperties = mock(ImportConfigProperties.ImportManagedProperties.class);
        when(importConfigProperties.getManaged()).thenReturn(managedProperties);
        when(managedProperties.getOrganization()).thenReturn(ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues.NO_DELETE);

        organizationImporter.doImport(realmImport);

        verify(organizationRepository, never()).create(anyString(), any(OrganizationRepresentation.class));
        verify(organizationRepository).update(eq("test-realm"), any(OrganizationRepresentation.class));
    }

    @Test
    void shouldHandleIdentityProviderAssociations() {
        RealmImport realmImport = new RealmImport();
        realmImport.setRealm("test-realm");
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setAlias("test-org");
        
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias("github");
        org.setIdentityProviders(Collections.singletonList(idp));
        
        realmImport.setOrganizations(Collections.singletonList(org));

        OrganizationRepresentation existingOrg = new OrganizationRepresentation();
        existingOrg.setId("org-123");
        existingOrg.setAlias("test-org");

        when(organizationRepository.search("test-realm", "test-org")).thenReturn(Optional.of(existingOrg));
        when(organizationRepository.getAll("test-realm")).thenReturn(Collections.singletonList(existingOrg));
        when(organizationRepository.getIdentityProviders("test-realm", "org-123")).thenReturn(Collections.emptyList());

        ImportConfigProperties.ImportManagedProperties managedProperties = mock(ImportConfigProperties.ImportManagedProperties.class);
        when(importConfigProperties.getManaged()).thenReturn(managedProperties);
        when(managedProperties.getOrganization()).thenReturn(ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues.NO_DELETE);

        organizationImporter.doImport(realmImport);

        verify(organizationRepository).addIdentityProvider("test-realm", "org-123", "github");
    }

    @Test
    void shouldHandleMemberAssociations() {
        RealmImport realmImport = new RealmImport();
        realmImport.setRealm("test-realm");
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setAlias("test-org");
        
        MemberRepresentation member = new MemberRepresentation();
        member.setUsername("testuser");
        org.setMembers(Collections.singletonList(member));
        
        realmImport.setOrganizations(Collections.singletonList(org));

        OrganizationRepresentation existingOrg = new OrganizationRepresentation();
        existingOrg.setId("org-123");
        existingOrg.setAlias("test-org");

        UserRepresentation foundUser = new UserRepresentation();
        foundUser.setId("user-123");
        foundUser.setUsername("testuser");

        when(organizationRepository.search("test-realm", "test-org")).thenReturn(Optional.of(existingOrg));
        when(organizationRepository.getAll("test-realm")).thenReturn(Collections.singletonList(existingOrg));
        when(organizationRepository.getMembers("test-realm", "org-123")).thenReturn(Collections.emptyList());
        when(userRepository.search("test-realm", "testuser")).thenReturn(Optional.of(foundUser));

        ImportConfigProperties.ImportManagedProperties managedProperties = mock(ImportConfigProperties.ImportManagedProperties.class);
        when(importConfigProperties.getManaged()).thenReturn(managedProperties);
        when(managedProperties.getOrganization()).thenReturn(ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues.NO_DELETE);

        organizationImporter.doImport(realmImport);

        verify(organizationRepository).addMember("test-realm", "org-123", "user-123");
    }

    @Test
    void shouldDeleteOrganizationsInFullManagedMode() {
        RealmImport realmImport = new RealmImport();
        realmImport.setRealm("test-realm");
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setAlias("test-org");
        realmImport.setOrganizations(Collections.singletonList(org));

        OrganizationRepresentation existingOrg = new OrganizationRepresentation();
        existingOrg.setAlias("old-org");
        existingOrg.setId("old-org-id");

        when(organizationRepository.search("test-realm", "test-org")).thenReturn(Optional.empty());
        when(organizationRepository.getAll("test-realm")).thenReturn(Collections.singletonList(existingOrg));

        ImportConfigProperties.ImportManagedProperties managedProperties = mock(ImportConfigProperties.ImportManagedProperties.class);
        when(importConfigProperties.getManaged()).thenReturn(managedProperties);
        when(managedProperties.getOrganization()).thenReturn(ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues.FULL);

        organizationImporter.doImport(realmImport);

        verify(organizationRepository).delete("test-realm", existingOrg);
        verify(organizationRepository).create(eq("test-realm"), any(OrganizationRepresentation.class));
    }

    @Test
    void shouldGracefullyHandleKeycloakVersionIncompatibility() {
        RealmImport realmImport = new RealmImport();
        realmImport.setRealm("test-realm");
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setAlias("test-org");
        realmImport.setOrganizations(Collections.singletonList(org));

        when(organizationRepository.getAll("test-realm")).thenThrow(new RuntimeException("Organizations require Keycloak 26.x or later"));

        ImportConfigProperties.ImportManagedProperties managedProperties = mock(ImportConfigProperties.ImportManagedProperties.class);
        when(importConfigProperties.getManaged()).thenReturn(managedProperties);
        when(managedProperties.getOrganization()).thenReturn(ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues.NO_DELETE);

        assertDoesNotThrow(() -> organizationImporter.doImport(realmImport));

        verify(organizationRepository).getAll("test-realm");
    }
}
