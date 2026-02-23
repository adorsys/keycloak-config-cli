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

package de.adorsys.keycloak.config.repository.organization;

import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.repository.RealmRepository;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrganizationRepositoryTest {

    private final RealmRepository realmRepository = mock(RealmRepository.class);
    private final KeycloakProvider keycloakProvider = mock(KeycloakProvider.class);
    private final KeycloakConfigProperties properties = mock(KeycloakConfigProperties.class);
    private final Keycloak keycloak = mock(Keycloak.class);
    private final RealmResource realmResource = mock(RealmResource.class);
    private final OrganizationsResource organizationsResource = mock(OrganizationsResource.class);
    private final OrganizationResource organizationResource = mock(OrganizationResource.class);
    private final OrganizationMembersResource membersResource = mock(OrganizationMembersResource.class);
    private final OrganizationIdentityProvidersResource identityProvidersResource = mock(OrganizationIdentityProvidersResource.class);
    private final OrganizationIdentityProviderResource organizationIdentityProviderResource = mock(OrganizationIdentityProviderResource.class);

    private final OrganizationRepository repository = new OrganizationRepository(realmRepository, keycloakProvider, properties);

    private static final String REALM_NAME = "master";
    private static final String ORG_ID = "org-123";
    private static final String ORG_ALIAS = "test-org";

    @BeforeEach
    void setUp() {
        when(realmRepository.getResource(REALM_NAME)).thenReturn(realmResource);
        when(realmResource.organizations()).thenReturn(organizationsResource);
        when(organizationsResource.get(ORG_ID)).thenReturn(organizationResource);
        when(organizationResource.members()).thenReturn(membersResource);
        when(organizationResource.identityProviders()).thenReturn(identityProvidersResource);

        when(keycloakProvider.getInstance()).thenReturn(keycloak);
    }

    @Test
    void searchShouldReturnOrganizationWhenFound() {
        OrganizationRepresentation org = organization(ORG_ALIAS, ORG_ID);
        when(organizationsResource.getAll()).thenReturn(List.of(org));
        when(organizationResource.toRepresentation()).thenReturn(org);

        Optional<OrganizationRepresentation> result = repository.search(REALM_NAME, ORG_ALIAS);

        assertThat(result).isPresent().get().extracting(OrganizationRepresentation::getAlias).isEqualTo(ORG_ALIAS);
    }

    @Test
    void searchShouldReturnEmptyWhenNotFound() {
        when(organizationsResource.getAll()).thenReturn(Collections.emptyList());

        Optional<OrganizationRepresentation> result = repository.search(REALM_NAME, ORG_ALIAS);

        assertThat(result).isEmpty();
    }

    @Test
    void getByAliasShouldReturnOrganization() {
        OrganizationRepresentation org = organization(ORG_ALIAS, ORG_ID);
        when(organizationsResource.getAll()).thenReturn(List.of(org));
        when(organizationResource.toRepresentation()).thenReturn(org);

        OrganizationRepresentation result = repository.getByAlias(REALM_NAME, ORG_ALIAS);

        assertThat(result.getAlias()).isEqualTo(ORG_ALIAS);
    }

    @Test
    void getAllShouldReturnList() {
        OrganizationRepresentation org = organization(ORG_ALIAS, ORG_ID);
        when(organizationsResource.getAll()).thenReturn(List.of(org));

        List<OrganizationRepresentation> result = repository.getAll(REALM_NAME);

        assertThat(result).hasSize(1).first().extracting(OrganizationRepresentation::getAlias).isEqualTo(ORG_ALIAS);
    }

    @Test
    void createShouldDelegateToResource() {
        OrganizationRepresentation org = organization(ORG_ALIAS, null);
        Response response = mock(Response.class);
        when(response.getStatusInfo()).thenReturn(Response.Status.CREATED);
        when(response.getStatus()).thenReturn(201);
        when(response.getLocation()).thenReturn(java.net.URI.create("http://localhost/org-123"));
        when(response.getHeaderString(eq("Location"))).thenReturn("http://localhost/org-123");
        when(organizationsResource.create(org)).thenReturn(response);

        repository.create(REALM_NAME, org);

        verify(organizationsResource).create(org);
        verify(response).close();
    }

    @Test
    void updateShouldDelegateToResource() {
        OrganizationRepresentation org = organization(ORG_ALIAS, ORG_ID);

        repository.update(REALM_NAME, org);

        verify(organizationResource).update(org);
    }

    @Test
    void deleteShouldDelegateToResource() {
        OrganizationRepresentation org = organization(ORG_ALIAS, ORG_ID);

        repository.delete(REALM_NAME, org);

        verify(organizationResource).delete();
    }

    @Test
    void getIdentityProvidersShouldReturnList() {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias("github");
        when(identityProvidersResource.getIdentityProviders()).thenReturn(List.of(idp));

        List<IdentityProviderRepresentation> result = repository.getIdentityProviders(REALM_NAME, ORG_ID);

        assertThat(result).hasSize(1).first().extracting(IdentityProviderRepresentation::getAlias).isEqualTo("github");
    }

    @Test
    void removeIdentityProviderShouldDelegateToResource() {
        when(identityProvidersResource.get("github")).thenReturn(organizationIdentityProviderResource);

        repository.removeIdentityProvider(REALM_NAME, ORG_ID, "github");

        verify(organizationIdentityProviderResource).delete();
    }

    @Test
    void addMemberShouldDelegateToResource() {
        String userId = "user-123";
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(201);
        when(membersResource.addMember(userId)).thenReturn(response);

        repository.addMember(REALM_NAME, ORG_ID, userId);

        verify(membersResource).addMember(userId);
        verify(response).close();
    }

    @Test
    void removeMemberShouldDelegateToResource() {
        String userId = "user-123";
        OrganizationMemberResource memberResource = mock(OrganizationMemberResource.class);
        when(membersResource.member(userId)).thenReturn(memberResource);

        repository.removeMember(REALM_NAME, ORG_ID, userId);

        verify(memberResource).delete();
    }

    @Test
    void getMembersShouldReturnList() {
        MemberRepresentation member = new MemberRepresentation();
        member.setUsername("testuser");
        when(membersResource.getAll()).thenReturn(List.of(member));

        List<MemberRepresentation> result = repository.getMembers(REALM_NAME, ORG_ID);

        assertThat(result).hasSize(1).first().extracting(MemberRepresentation::getUsername).isEqualTo("testuser");
    }

    private OrganizationRepresentation organization(String alias, String id) {
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setAlias(alias);
        org.setId(id);
        return org;
    }
}
