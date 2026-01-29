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

import de.adorsys.keycloak.config.AbstractImportIT;
import de.adorsys.keycloak.config.repository.OrganizationRepository;
import de.adorsys.keycloak.config.util.VersionUtil;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ImportOrganizationsIT extends AbstractImportIT {
    private static final String REALM_NAME = "test-realm";

    @Autowired
    private OrganizationRepository organizationRepository;

    ImportOrganizationsIT() {
        this.resourcePath = "import-files/organizations";
    }

    @Test
    @Order(1)
    void shouldCreateOrganizationsWithIdpsAndMembers() throws IOException {
        assumeTrue(VersionUtil.ge(KEYCLOAK_VERSION, "26"), "Organizations require Keycloak 26+");

        doImport("01_create_organization.json");

        List<OrganizationRepresentation> organizations = organizationRepository.getAll(REALM_NAME);
        assertThat(organizations, hasSize(2));

        // Verify acme organization
        OrganizationRepresentation acme = organizations.stream()
                .filter(org -> "acme".equals(org.getAlias()))
                .findFirst()
                .orElseThrow();
        assertThat(acme.getName(), is("Acme Corporation"));
        assertThat(acme.getDomains(), hasSize(2));
        
        // Verify acme has github IdP
        List<IdentityProviderRepresentation> acmeIdps = organizationRepository.getIdentityProviders(REALM_NAME, acme.getId());
        assertThat(acmeIdps, hasSize(1));
        assertThat(acmeIdps.get(0).getAlias(), is("github"));
        
        // Verify acme has members
        List<MemberRepresentation> acmeMembers = organizationRepository.getMembers(REALM_NAME, acme.getId());
        assertThat(acmeMembers, hasSize(2));
        assertThat(acmeMembers, hasItems(hasProperty("username", is("myuser")), hasProperty("username", is("myclientuser"))));

        // Verify tech-startup organization
        OrganizationRepresentation techStartup = organizations.stream()
                .filter(org -> "tech-startup".equals(org.getAlias()))
                .findFirst()
                .orElseThrow();
        assertThat(techStartup.getName(), is("Tech Startup"));
        assertThat(techStartup.getDomains(), hasSize(1));
        
        // Verify tech-startup has google IdP
        List<IdentityProviderRepresentation> techStartupIdps = organizationRepository.getIdentityProviders(REALM_NAME, techStartup.getId());
        assertThat(techStartupIdps, hasSize(1));
        assertThat(techStartupIdps.get(0).getAlias(), is("google"));
        
        // Verify tech-startup has members
        List<MemberRepresentation> techStartupMembers = organizationRepository.getMembers(REALM_NAME, techStartup.getId());
        assertThat(techStartupMembers, hasSize(2));
        assertThat(techStartupMembers, hasItems(hasProperty("username", is("ceo@tech-startup.io")), hasProperty("username", is("cto@tech-startup.io"))));
    }

    @Test
    @Order(2)
    void shouldCreateOrganizationWithDomains() throws IOException {
        assumeTrue(VersionUtil.ge(KEYCLOAK_VERSION, "26"), "Organizations require Keycloak 26+");

        doImport("02_organization_with_domains.json");

        OrganizationRepresentation org = organizationRepository.getByAlias(REALM_NAME, "test-org");
        assertThat(org.getName(), is("Test Organization")); // Verify name preserved from 01
        assertThat(org.getDomains(), hasSize(2));
    }

    @Test
    @Order(3)
    void shouldCreateOrganizationWithIdps() throws IOException {
        assumeTrue(VersionUtil.ge(KEYCLOAK_VERSION, "26"), "Organizations require Keycloak 26+");

        doImport("03_organization_with_idps.json");

        OrganizationRepresentation org = organizationRepository.getByAlias(REALM_NAME, "test-org");
        assertThat(org.getName(), is("Test Organization"));
        assertThat(organizationRepository.getIdentityProviders(REALM_NAME, org.getId()), hasSize(1));
    }

    @Test
    @Order(4)
    void shouldCreateOrganizationWithMembers() throws IOException {
        assumeTrue(VersionUtil.ge(KEYCLOAK_VERSION, "26"), "Organizations require Keycloak 26+");

        doImport("04_organization_with_members.json");

        OrganizationRepresentation org = organizationRepository.getByAlias(REALM_NAME, "test-org");
        assertThat(org.getName(), is("Test Organization"));
        List<MemberRepresentation> members = organizationRepository.getMembers(REALM_NAME, org.getId());
        assertThat(members, hasSize(1));
        assertThat(members.get(0).getUsername(), is("john.doe"));
    }
}
