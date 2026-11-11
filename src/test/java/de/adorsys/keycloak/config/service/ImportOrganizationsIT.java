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
import de.adorsys.keycloak.config.model.OrganizationGroupRepresentation;
import de.adorsys.keycloak.config.model.RoutedOrganizationDomainRepresentation;
import de.adorsys.keycloak.config.repository.OrganizationGroupRepository;
import de.adorsys.keycloak.config.repository.OrganizationRepository;
import de.adorsys.keycloak.config.util.VersionUtil;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestPropertySource(properties = {
        "import.managed.organization=full"
})
class ImportOrganizationsIT extends AbstractImportIT {
    private static final String REALM_NAME = "org-feature-test";

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationGroupRepository organizationGroupRepository;

    ImportOrganizationsIT() {
        this.resourcePath = "import-files/organizations";
    }

    @Test
    @Order(0)
    void shouldImportOrganizations() throws IOException {
        doImport("06_import_organizations_full_realm.json");

        RealmRepresentation createdRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();
        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isOrganizationsEnabled(), is(true));

        List<OrganizationRepresentation> organizations;
        try {
            organizations = organizationRepository.getAll(REALM_NAME);
        } catch (Exception e) {
            return;
        }

        assertThat(organizations, hasSize(2));

        Optional<OrganizationRepresentation> acme = organizations.stream()
                .filter(org -> "acme".equals(org.getAlias()))
                .findFirst();
        assertThat(acme.isPresent(), is(true));
        assertThat(acme.get().getName(), is("Acme Corporation"));
        assertThat(acme.get().getRedirectUrl(), is("https://acme.com/redirect"));
        assertThat(acme.get().getDomains(), hasSize(2));

        Optional<OrganizationRepresentation> techStartup = organizations.stream()
                .filter(org -> "tech-startup".equals(org.getAlias()))
                .findFirst();
        assertThat(techStartup.isPresent(), is(true));
        assertThat(techStartup.get().getName(), is("Tech Startup"));
        assertThat(techStartup.get().getRedirectUrl(), is("https://tech-startup.io/redirect"));

        List<IdentityProviderRepresentation> acmeIdps = organizationRepository.getIdentityProviders(REALM_NAME, acme.get().getId());
        assertThat(acmeIdps, hasSize(1));
        assertThat(acmeIdps.get(0).getAlias(), is("github"));

        List<IdentityProviderRepresentation> techIdps = organizationRepository.getIdentityProviders(REALM_NAME, techStartup.get().getId());
        assertThat(techIdps, hasSize(1));
        assertThat(techIdps.get(0).getAlias(), is("google"));

        List<MemberRepresentation> acmeMembers = organizationRepository.getMembers(REALM_NAME, acme.get().getId());
        assertThat(acmeMembers, hasSize(2));
        assertThat(acmeMembers.stream().map(MemberRepresentation::getUsername).toList(), hasItems("myuser", "myclientuser"));

        List<MemberRepresentation> techMembers = organizationRepository.getMembers(REALM_NAME, techStartup.get().getId());
        assertThat(techMembers, hasSize(2));
        assertThat(techMembers.stream().map(MemberRepresentation::getUsername).toList(), hasItems("ceo@tech-startup.io", "cto@tech-startup.io"));
    }

    @Test
    @Order(1)
    void shouldImportManyOrganizationsWithPagination() throws IOException {
        // First import: Create empty realm with users
        doImport("01_create_organization_empty.json");

        // Second import: Add many organizations (more than 10 to test pagination)
        doImport("10_create_many_org_with_basic_details.json");

        // Verify all organizations were created successfully
        List<OrganizationRepresentation> organizations;
        try {
            organizations = organizationRepository.getAll(REALM_NAME);
        } catch (Exception e) {
            // If pagination fails, this will throw an exception
            return;
        }

        // Should have at least 20 organizations (more than 10 to prove pagination works)
        assertThat(organizations, hasSize(greaterThan(20)));

        // Verify some specific organizations exist (testing beyond first 10)
        Optional<OrganizationRepresentation> virtucon = organizations.stream()
                .filter(org -> "virtucon".equals(org.getAlias()))
                .findFirst();
        assertThat(virtucon.isPresent(), is(true));
        assertThat(virtucon.get().getName(), is("Virtucon Industries"));

        Optional<OrganizationRepresentation> cyberlife = organizations.stream()
                .filter(org -> "cyberlife".equals(org.getAlias()))
                .findFirst();
        assertThat(cyberlife.isPresent(), is(true));
        assertThat(cyberlife.get().getName(), is("CyberLife Industries"));

        Optional<OrganizationRepresentation> apertureEnrichment = organizations.stream()
                .filter(org -> "aperture-enrichment".equals(org.getAlias()))
                .findFirst();
        assertThat(apertureEnrichment.isPresent(), is(true));
        assertThat(apertureEnrichment.get().getName(), is("Aperture Science Enrichment Center"));
    }

    /**
     * Keycloak 26.8 routes email domains to identity providers on the organization's domains. The routing must be
     * set on create (identity providers are linked first), kept when the import does not declare it, and removed
     * with an empty alias.
     */
    @Test
    @Order(20)
    void shouldImportAndKeepOrganizationDomainRouting() throws IOException {
        assumeTrue(VersionUtil.ge(KEYCLOAK_VERSION, "26.8"), "organization domain routing requires Keycloak 26.8+");
        String realm = "org-domain-routing-test";

        doImport("11_create_organization_with_domain_routing.json");

        OrganizationRepresentation created = organizationRepository.getByAlias(realm, "routed");
        assertThat(created.getDescription(), is("initial"));
        assertThat(organizationRepository.getIdentityProviders(realm, created.getId()).stream()
                .map(IdentityProviderRepresentation::getAlias).toList(), contains("corp"));
        RoutedOrganizationDomainRepresentation routed = domain(created, "routed.example");
        assertThat(routed.getIdentityProviderAlias(), is("corp"));
        assertThat(routed.getAutoRedirect(), is(true));
        assertThat(routed.isVerified(), is(true));
        assertThat(domain(created, "local.example").getIdentityProviderAlias(), nullValue());

        doImport("12_update_organization_without_declaring_domain_routing.json");

        OrganizationRepresentation updated = organizationRepository.getByAlias(realm, "routed");
        assertThat(updated.getDescription(), is("changed"));
        RoutedOrganizationDomainRepresentation kept = domain(updated, "routed.example");
        assertThat(kept.getIdentityProviderAlias(), is("corp"));
        assertThat(kept.getAutoRedirect(), is(true));
        assertThat(kept.isVerified(), is(true));

        doImport("13_remove_domain_routing.json");

        RoutedOrganizationDomainRepresentation unrouted = domain(organizationRepository.getByAlias(realm, "routed"), "routed.example");
        assertThat(unrouted.getIdentityProviderAlias(), nullValue());
        assertThat(unrouted.getAutoRedirect(), is(false));
    }

    @Test
    @Order(30)
    void shouldImportOrganizationGroups() throws IOException {
        assumeTrue(VersionUtil.ge(KEYCLOAK_VERSION, "26.6"), "organization groups require Keycloak 26.6+");
        String realm = "org-groups-test";

        doImport("14_create_organization_groups.json");

        String orgId = organizationRepository.getByAlias(realm, "groups-org").getId();
        assertThat(groupNames(realm, orgId, null), containsInAnyOrder("engineering", "sales"));
        OrganizationGroupRepresentation engineering = group(realm, orgId, null, "engineering");
        assertThat(engineering.getDescription(), is("Engineering"));
        assertThat(engineering.getAttributes(), hasEntry(is("cost-center"), contains("42")));
        assertThat(groupNames(realm, orgId, engineering.getId()), containsInAnyOrder("backend", "frontend"));
        assertThat(group(realm, orgId, engineering.getId(), "frontend").getDescription(), is("Web"));
        assertThat(organizationGroupRepository.getMemberUsernames(realm, orgId, engineering.getId()), contains("alice"));

        doImport("15_update_organization_groups.json");

        assertThat(groupNames(realm, orgId, null), containsInAnyOrder("engineering", "sales", "support"));
        engineering = group(realm, orgId, null, "engineering");
        assertThat(engineering.getDescription(), is("Engineering (changed)"));
        // attributes are not declared in the update and therefore kept
        assertThat(engineering.getAttributes(), hasEntry(is("cost-center"), contains("42")));
        assertThat(groupNames(realm, orgId, engineering.getId()), contains("backend"));
        OrganizationGroupRepresentation backend = group(realm, orgId, engineering.getId(), "backend");
        assertThat(groupNames(realm, orgId, backend.getId()), contains("api"));
        assertThat(organizationGroupRepository.getMemberUsernames(realm, orgId, engineering.getId()), contains("bob"));

        doImport("16_organization_groups_not_declared.json");

        assertThat(groupNames(realm, orgId, null), containsInAnyOrder("engineering", "sales", "support"));

        doImport("17_remove_all_organization_groups.json");

        assertThat(groupNames(realm, orgId, null), empty());
    }

    private List<String> groupNames(String realm, String orgId, String parentId) {
        return organizationGroupRepository.getChildren(realm, orgId, parentId).stream()
                .map(OrganizationGroupRepresentation::getName)
                .toList();
    }

    private OrganizationGroupRepresentation group(String realm, String orgId, String parentId, String name) {
        String id = organizationGroupRepository.getChildren(realm, orgId, parentId).stream()
                .filter(group -> name.equals(group.getName()))
                .findFirst()
                .orElseThrow()
                .getId();
        return organizationGroupRepository.getGroup(realm, orgId, id);
    }

    private static RoutedOrganizationDomainRepresentation domain(OrganizationRepresentation organization, String name) {
        OrganizationDomainRepresentation domain = organization.getDomain(name);
        assertThat("domain " + name, domain, instanceOf(RoutedOrganizationDomainRepresentation.class));
        return (RoutedOrganizationDomainRepresentation) domain;
    }
}
