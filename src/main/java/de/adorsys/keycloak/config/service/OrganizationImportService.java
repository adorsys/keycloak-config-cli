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
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.repository.OrganizationRepository;
import de.adorsys.keycloak.config.repository.UserRepository;
import de.adorsys.keycloak.config.util.CloneUtil;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

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
        List<OrganizationRepresentation> organizations = getOrganizations(realmImport);
        if (organizations == null || organizations.isEmpty()) return;

        String realmName = realmImport.getRealm();

        try {
            createOrUpdateOrDeleteOrganizations(realmName, organizations);
        } catch (RuntimeException e) {
            logger.warn(
                    "Failed to import organizations for realm '{}'. Organizations require Keycloak 26.x or later. Error: {}",
                    realmName,
                    e.getMessage()
            );
        }
    }

    private List<OrganizationRepresentation> getOrganizations(RealmImport realmImport) {
        List<Map<String, Object>> raw = realmImport.getOrganizationsRaw();
        if (raw == null) return null;

        return raw.stream()
                .map(r -> CloneUtil.deepClone(r, OrganizationRepresentation.class))
                .collect(Collectors.toList());
    }

    private void createOrUpdateOrDeleteOrganizations(String realmName, List<OrganizationRepresentation> organizations) {
        List<OrganizationRepresentation> existingOrganizations = organizationRepository.getAll(realmName);

        if (importConfigProperties.getManaged().getOrganization() == ImportManagedPropertiesValues.FULL) {
            deleteOrganizationsMissingInImport(realmName, organizations, existingOrganizations);
        }

        for (OrganizationRepresentation organization : organizations) {
            createOrUpdateOrganization(realmName, organization);
        }
    }

    private void deleteOrganizationsMissingInImport(
            String realmName,
            List<OrganizationRepresentation> organizations,
            List<OrganizationRepresentation> existingOrganizations
    ) {
        for (OrganizationRepresentation existingOrganization : existingOrganizations) {
            if (!hasOrganizationWithAlias(organizations, existingOrganization.getAlias())) {
                logger.debug("Delete organization '{}' in realm '{}'", existingOrganization.getAlias(), realmName);
                organizationRepository.delete(realmName, existingOrganization);
            }
        }
    }

    private void createOrUpdateOrganization(String realmName, OrganizationRepresentation organization) {
        String organizationAlias = organization.getAlias();

        Optional<OrganizationRepresentation> maybeOrganization = organizationRepository.search(realmName, organizationAlias);

        if (maybeOrganization.isPresent()) {
            OrganizationRepresentation existingOrganization = maybeOrganization.get();
            updateOrganizationIfNecessary(realmName, organization, existingOrganization);

            OrganizationRepresentation resolved = organizationRepository.getByAlias(realmName, organizationAlias);
            manageIdentityProviderAssociations(realmName, resolved.getId(), organization);
            manageMemberships(realmName, resolved.getId(), organization);
        } else {
            logger.debug("Create organization '{}' in realm '{}'", organizationAlias, realmName);
            organizationRepository.create(realmName, organization);

            OrganizationRepresentation created = organizationRepository.getByAlias(realmName, organizationAlias);
            manageIdentityProviderAssociations(realmName, created.getId(), organization);
            manageMemberships(realmName, created.getId(), organization);
        }
    }

    private void updateOrganizationIfNecessary(
            String realmName,
            OrganizationRepresentation organization,
            OrganizationRepresentation existingOrganization
    ) {
        OrganizationRepresentation patched = CloneUtil.patch(existingOrganization, organization, "id");
        patched.setId(existingOrganization.getId());

        if (CloneUtil.deepEquals(existingOrganization, patched)) {
            logger.debug("No need to update organization '{}' in realm '{}'", existingOrganization.getAlias(), realmName);
        } else {
            logger.debug("Update organization '{}' in realm '{}'", existingOrganization.getAlias(), realmName);
            organizationRepository.update(realmName, patched);
        }
    }

    private boolean hasOrganizationWithAlias(List<OrganizationRepresentation> organizations, String alias) {
        return organizations.stream().anyMatch(org -> Objects.equals(org.getAlias(), alias));
    }

    private void manageIdentityProviderAssociations(
            String realmName,
            String orgId,
            OrganizationRepresentation organization
    ) {
        List<IdentityProviderRepresentation> idpsToAssociate = organization.getIdentityProviders();
        List<IdentityProviderRepresentation> existingIdps = organizationRepository.getIdentityProviders(realmName, orgId);
        Set<String> existingAliases = existingIdps.stream()
                .map(IdentityProviderRepresentation::getAlias)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (idpsToAssociate == null || idpsToAssociate.isEmpty()) {
            if (importConfigProperties.getManaged().getOrganization() == ImportManagedPropertiesValues.FULL) {
                for (String existingAlias : existingAliases) {
                    try {
                        organizationRepository.removeIdentityProvider(realmName, orgId, existingAlias);
                    } catch (NotFoundException | BadRequestException e) {
                        logger.warn("Failed to remove identity provider '{}' from organization '{}': {}",
                                existingAlias, organization.getAlias(), e.getMessage());
                    }
                }
            }
            return;
        }

        Set<String> configuredAliases = idpsToAssociate.stream()
                .map(IdentityProviderRepresentation::getAlias)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

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

        if (importConfigProperties.getManaged().getOrganization() == ImportManagedPropertiesValues.FULL) {
            for (String existingAlias : existingAliases) {
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
}
