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

import de.adorsys.keycloak.config.condition.ConditionalOnKeycloakVersion26OrNewer;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.model.RoutedOrganizationDomainRepresentation;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.repository.OrganizationRepository;
import de.adorsys.keycloak.config.repository.UserRepository;
import de.adorsys.keycloak.config.util.CloneUtil;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import static de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
@ConditionalOnKeycloakVersion26OrNewer
public class OrganizationImportService {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationImportService.class);

    private static final String DOMAINS = "domains";

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ImportConfigProperties importConfigProperties;

    public OrganizationImportService(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            ImportConfigProperties importConfigProperties
    ) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.importConfigProperties = importConfigProperties;
    }

    public void doImport(RealmImport realmImport) {
        List<OrganizationImport> organizations = getOrganizations(realmImport);
        if (organizations == null || organizations.isEmpty()) return;

        String realmName = realmImport.getRealm();

        try {
            createOrUpdateOrDeleteOrganizations(realmName, organizations);
        } catch (RuntimeException e) {
            logger.warn(
                    "Failed to import organizations for realm '{}'. Error: {}",
                    realmName,
                    e.getMessage()
            );
        }
    }

    private List<OrganizationImport> getOrganizations(RealmImport realmImport) {
        List<Map<String, Object>> raw = realmImport.getOrganizationsRaw();
        if (raw == null) return null;

        return raw.stream()
                .map(r -> new OrganizationImport(
                        CloneUtil.deepClone(r, OrganizationRepresentation.class),
                        getDeclaredDomains(r)))
                .toList();
    }

    /**
     * Reads the domains from the raw import, because the admin client's {@link OrganizationDomainRepresentation}
     * drops the identity provider routing and cannot tell a missing {@code verified} from {@code false}.
     */
    private static List<DeclaredDomain> getDeclaredDomains(Map<String, Object> rawOrganization) {
        if (!(rawOrganization.get(DOMAINS) instanceof Collection<?> rawDomains)) return List.of();

        return rawDomains.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .filter(rawDomain -> rawDomain.get("name") != null)
                .map(rawDomain -> new DeclaredDomain(
                        String.valueOf(rawDomain.get("name")),
                        toBoolean(rawDomain.get("verified")).orElse(null),
                        // a declared null or empty alias removes the routing, a missing one keeps it
                        rawDomain.containsKey("identityProviderAlias")
                                ? Objects.toString(rawDomain.get("identityProviderAlias"), "")
                                : null,
                        toBoolean(rawDomain.get("autoRedirect")).orElse(null)))
                .toList();
    }

    private static Optional<Boolean> toBoolean(Object value) {
        if (value == null) return Optional.empty();
        if (value instanceof Boolean bool) return Optional.of(bool);
        return Optional.of(Boolean.parseBoolean(value.toString()));
    }

    private void createOrUpdateOrDeleteOrganizations(String realmName, List<OrganizationImport> organizations) {
        List<OrganizationRepresentation> existingOrganizations = organizationRepository.getAll(realmName);

        if (importConfigProperties.getManaged().getOrganization() == ImportManagedPropertiesValues.FULL) {
            deleteOrganizationsMissingInImport(realmName, organizations, existingOrganizations);
        }

        for (OrganizationImport organization : organizations) {
            createOrUpdateOrganization(realmName, organization);
        }
    }

    private void deleteOrganizationsMissingInImport(
            String realmName,
            List<OrganizationImport> organizations,
            List<OrganizationRepresentation> existingOrganizations
    ) {
        for (OrganizationRepresentation existingOrganization : existingOrganizations) {
            if (!hasOrganizationWithAlias(organizations, existingOrganization.getAlias())) {
                logger.debug("Delete organization '{}' in realm '{}'", existingOrganization.getAlias(), realmName);
                organizationRepository.delete(realmName, existingOrganization);
            }
        }
    }

    /**
     * Keycloak 26.8+ only accepts a domain routed to an identity provider that is linked to the organization.
     * Therefore, identity providers are linked before the organization (and its domain routing) is updated, and
     * unlinked afterwards. A new organization is created without domain routing, which the update then sets.
     */
    private void createOrUpdateOrganization(String realmName, OrganizationImport organizationImport) {
        OrganizationRepresentation organization = organizationImport.organization();
        String organizationAlias = organization.getAlias();

        OrganizationRepresentation existingOrganization;
        if (organizationRepository.search(realmName, organizationAlias).isPresent()) {
            existingOrganization = organizationRepository.getByAlias(realmName, organizationAlias);
        } else {
            logger.debug("Create organization '{}' in realm '{}'", organizationAlias, realmName);
            organizationRepository.create(realmName, withoutDomainRouting(organizationImport));
            existingOrganization = organizationRepository.getByAlias(realmName, organizationAlias);
        }

        String organizationId = existingOrganization.getId();
        addIdentityProviderAssociations(realmName, organizationId, organization);
        updateOrganizationIfNecessary(realmName, organizationImport, existingOrganization);
        removeIdentityProviderAssociations(realmName, organizationId, organization);
        manageMemberships(realmName, organizationId, organization);
    }

    private static OrganizationRepresentation withoutDomainRouting(OrganizationImport organizationImport) {
        // deepClone only returns null for a null input; imported organizations are never null
        OrganizationRepresentation organization = Objects.requireNonNull(
                CloneUtil.deepClone(organizationImport.organization()),
                "organization to create");
        RoutedOrganizationDomainRepresentation.replaceDomains(organization, organizationImport.domains().stream()
                .map(declared -> mergeDomain(declared, null).withoutRouting())
                .toList());
        return organization;
    }

    private void updateOrganizationIfNecessary(
            String realmName,
            OrganizationImport organizationImport,
            OrganizationRepresentation existingOrganization
    ) {
        OrganizationRepresentation patched = CloneUtil.patch(
                existingOrganization, organizationImport.organization(), "id", DOMAINS);
        patched.setId(existingOrganization.getId());

        Collection<RoutedOrganizationDomainRepresentation> domains =
                mergeDomains(existingOrganization.getDomains(), organizationImport.domains());
        RoutedOrganizationDomainRepresentation.replaceDomains(patched, domains);

        if (CloneUtil.deepEquals(existingOrganization, patched, DOMAINS)
                && sameDomains(existingOrganization.getDomains(), domains)) {
            logger.debug("No need to update organization '{}' in realm '{}'", existingOrganization.getAlias(), realmName);
        } else {
            logger.debug("Update organization '{}' in realm '{}'", existingOrganization.getAlias(), realmName);
            organizationRepository.update(realmName, patched);
        }
    }

    /**
     * Existing domains are kept; declared domains are added or updated. Properties a declared domain does not
     * specify keep their current value, so domain routing set outside the import (e.g. by the Keycloak 26.8
     * migration) is not erased.
     */
    private static Collection<RoutedOrganizationDomainRepresentation> mergeDomains(
            Set<OrganizationDomainRepresentation> existingDomains,
            List<DeclaredDomain> declaredDomains
    ) {
        Map<String, RoutedOrganizationDomainRepresentation> merged = new LinkedHashMap<>();
        if (existingDomains != null) {
            existingDomains.forEach(domain -> merged.put(domain.getName(), RoutedOrganizationDomainRepresentation.of(domain)));
        }
        for (DeclaredDomain declared : declaredDomains) {
            merged.put(declared.name(), mergeDomain(declared, merged.get(declared.name())));
        }
        return merged.values();
    }

    private static RoutedOrganizationDomainRepresentation mergeDomain(
            DeclaredDomain declared,
            RoutedOrganizationDomainRepresentation current
    ) {
        RoutedOrganizationDomainRepresentation domain = new RoutedOrganizationDomainRepresentation(declared.name());
        domain.setVerified(declared.verified() != null ? declared.verified() : current != null && current.isVerified());

        if ("".equals(declared.identityProviderAlias())) {
            // routing explicitly removed: without an identity provider there is nothing to redirect to
            domain.setIdentityProviderAlias(null);
            domain.setAutoRedirect(Boolean.FALSE);
            return domain;
        }

        String currentAlias = current != null ? current.getIdentityProviderAlias() : null;
        Boolean currentAutoRedirect = current != null ? current.getAutoRedirect() : null;
        domain.setIdentityProviderAlias(declared.identityProviderAlias() != null ? declared.identityProviderAlias() : currentAlias);
        domain.setAutoRedirect(declared.autoRedirect() != null ? declared.autoRedirect() : currentAutoRedirect);
        return domain;
    }

    private static boolean sameDomains(
            Set<OrganizationDomainRepresentation> existingDomains,
            Collection<RoutedOrganizationDomainRepresentation> domains
    ) {
        return Objects.equals(domainState(existingDomains), domainState(domains));
    }

    private static Map<String, List<Object>> domainState(Collection<? extends OrganizationDomainRepresentation> domains) {
        if (domains == null) return Map.of();
        return domains.stream()
                .map(RoutedOrganizationDomainRepresentation::of)
                .collect(Collectors.toMap(
                        OrganizationDomainRepresentation::getName,
                        domain -> Arrays.asList(
                                domain.isVerified(),
                                domain.getIdentityProviderAlias(),
                                Boolean.TRUE.equals(domain.getAutoRedirect()))));
    }

    private boolean hasOrganizationWithAlias(List<OrganizationImport> organizations, String alias) {
        return organizations.stream().anyMatch(org -> Objects.equals(org.organization().getAlias(), alias));
    }

    private void addIdentityProviderAssociations(
            String realmName,
            String orgId,
            OrganizationRepresentation organization
    ) {
        Set<String> configuredAliases = getConfiguredIdentityProviderAliases(organization);
        if (configuredAliases.isEmpty()) return;

        Set<String> existingAliases = getExistingIdentityProviderAliases(realmName, orgId);
        for (String idpAlias : configuredAliases) {
            if (!existingAliases.contains(idpAlias)) {
                try {
                    organizationRepository.addIdentityProvider(realmName, orgId, idpAlias);
                } catch (NotFoundException | BadRequestException e) {
                    logger.warn("Failed to associate identity provider '{}' with organization '{}': {}",
                            idpAlias, organization.getAlias(), e.getMessage());
                }
            }
        }
    }

    private void removeIdentityProviderAssociations(
            String realmName,
            String orgId,
            OrganizationRepresentation organization
    ) {
        if (importConfigProperties.getManaged().getOrganization() != ImportManagedPropertiesValues.FULL) return;

        Set<String> configuredAliases = getConfiguredIdentityProviderAliases(organization);
        for (String existingAlias : getExistingIdentityProviderAliases(realmName, orgId)) {
            if (!configuredAliases.contains(existingAlias)) {
                try {
                    organizationRepository.removeIdentityProvider(realmName, orgId, existingAlias);
                } catch (NotFoundException | BadRequestException e) {
                    logger.warn("Failed to remove identity provider '{}' from organization '{}': {}",
                            existingAlias, organization.getAlias(), e.getMessage());
                }
            }
        }
    }

    private static Set<String> getConfiguredIdentityProviderAliases(OrganizationRepresentation organization) {
        List<IdentityProviderRepresentation> identityProviders = organization.getIdentityProviders();
        if (identityProviders == null) return Set.of();

        return identityProviders.stream()
                .map(IdentityProviderRepresentation::getAlias)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<String> getExistingIdentityProviderAliases(String realmName, String orgId) {
        return organizationRepository.getIdentityProviders(realmName, orgId).stream()
                .map(IdentityProviderRepresentation::getAlias)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void manageMemberships(
            String realmName,
            String orgId,
            OrganizationRepresentation organization
    ) {
        List<MemberRepresentation> membersToAdd = organization.getMembers();
        List<MemberRepresentation> existingMembers = organizationRepository.getMembers(realmName, orgId);
        Set<String> existingUsernames = existingMembers.stream()
                .map(MemberRepresentation::getUsername)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (membersToAdd == null || membersToAdd.isEmpty()) {
            if (importConfigProperties.getManaged().getOrganization() == ImportManagedPropertiesValues.FULL) {
                for (MemberRepresentation existing : existingMembers) {
                    if (existing == null || existing.getUsername() == null) continue;

                    try {
                        Optional<UserRepresentation> maybeUser = userRepository.search(realmName, existing.getUsername());
                        if (maybeUser.isPresent() && maybeUser.get().getId() != null) {
                            organizationRepository.removeMember(realmName, orgId, maybeUser.get().getId());
                        }
                    } catch (NotFoundException | BadRequestException e) {
                        logger.warn("Failed to remove user '{}' from organization '{}': {}",
                                existing.getUsername(), organization.getAlias(), e.getMessage());
                    }
                }
            }
            return;
        }

        for (MemberRepresentation member : membersToAdd) {
            if (member == null || member.getUsername() == null) continue;

            String username = member.getUsername();

            try {
                Optional<UserRepresentation> maybeUser = userRepository.search(realmName, username);
                if (maybeUser.isEmpty()) {
                    logger.warn("Cannot add user '{}' to organization '{}': user not found in realm '{}'",
                            username, organization.getAlias(), realmName);
                    continue;
                }

                if (!existingUsernames.contains(username)) {
                    organizationRepository.addMember(realmName, orgId, maybeUser.get().getId());
                }
            } catch (NotFoundException | BadRequestException e) {
                logger.warn("Failed to add user '{}' to organization '{}': {}",
                        username, organization.getAlias(), e.getMessage());
            }
        }

        if (importConfigProperties.getManaged().getOrganization() == ImportManagedPropertiesValues.FULL) {
            Set<String> configuredUsernames = membersToAdd.stream()
                    .map(MemberRepresentation::getUsername)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            for (MemberRepresentation existing : existingMembers) {
                if (existing == null || existing.getUsername() == null) continue;

                if (!configuredUsernames.contains(existing.getUsername())) {
                    try {
                        Optional<UserRepresentation> maybeUser = userRepository.search(realmName, existing.getUsername());
                        if (maybeUser.isPresent() && maybeUser.get().getId() != null) {
                            organizationRepository.removeMember(realmName, orgId, maybeUser.get().getId());
                        }
                    } catch (NotFoundException | BadRequestException e) {
                        logger.warn("Failed to remove user '{}' from organization '{}': {}",
                                existing.getUsername(), organization.getAlias(), e.getMessage());
                    }
                }
            }
        }
    }

    private record OrganizationImport(OrganizationRepresentation organization, List<DeclaredDomain> domains) {
    }

    /**
     * A domain as declared in the import; {@code null} means "not declared, keep the current value".
     * An empty {@code identityProviderAlias} removes the routing.
     */
    private record DeclaredDomain(String name, Boolean verified, String identityProviderAlias, Boolean autoRedirect) {
    }
}
