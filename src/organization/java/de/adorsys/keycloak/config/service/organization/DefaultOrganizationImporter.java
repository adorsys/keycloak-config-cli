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

package de.adorsys.keycloak.config.service.organization;

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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import static de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues;

public class DefaultOrganizationImporter implements OrganizationImporter {
    private static final Logger logger = LoggerFactory.getLogger(DefaultOrganizationImporter.class);

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ImportConfigProperties importConfigProperties;

    public DefaultOrganizationImporter(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            ImportConfigProperties importConfigProperties
    ) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.importConfigProperties = importConfigProperties;
    }

    @Override
    public void doImport(RealmImport realmImport) {
        List<OrganizationRepresentation> organizations = realmImport.getOrganizations();

        if (organizations == null || organizations.isEmpty()) {
            return;
        }

        try {
            createOrUpdateOrDeleteOrganizations(realmImport);
        } catch (NotFoundException | BadRequestException e) {
            logger.warn(
                    "Failed to import organizations for realm '{}'. "
                            + "Organizations require Keycloak 26.x or later. Error: {}",
                    realmImport.getRealm(),
                    e.getMessage()
            );
        }
    }

    private void createOrUpdateOrDeleteOrganizations(RealmImport realmImport) {
        String realmName = realmImport.getRealm();
        List<OrganizationRepresentation> organizations = realmImport.getOrganizations();
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

            manageIdentityProviderAssociations(realmName, existingOrganization.getId(), organization);
            manageMemberships(realmName, existingOrganization.getId(), organization);
        } else {
            logger.debug("Create organization '{}' in realm '{}'", organizationAlias, realmName);
            organizationRepository.create(realmName, organization);

            Optional<OrganizationRepresentation> createdOrganization = organizationRepository.search(realmName, organizationAlias);
            if (createdOrganization.isPresent()) {
                manageIdentityProviderAssociations(realmName, createdOrganization.get().getId(), organization);
                manageMemberships(realmName, createdOrganization.get().getId(), organization);
            }
        }
    }

    private void updateOrganizationIfNecessary(
            String realmName,
            OrganizationRepresentation organization,
            OrganizationRepresentation existingOrganization
    ) {
        OrganizationRepresentation patchedOrganization = CloneUtil.patch(existingOrganization, organization);
        String organizationAlias = existingOrganization.getAlias();

        if (isOrganizationEqual(existingOrganization, patchedOrganization)) {
            logger.debug("No need to update organization '{}' in realm '{}'", organizationAlias, realmName);
        } else {
            logger.debug("Updating organization '{}' in realm '{}'", organizationAlias, realmName);
            organizationRepository.update(realmName, patchedOrganization);
        }
    }

    private boolean isOrganizationEqual(
            OrganizationRepresentation existingOrganization,
            OrganizationRepresentation patchedOrganization
    ) {
        return CloneUtil.deepEquals(existingOrganization, patchedOrganization);
    }

    private boolean hasOrganizationWithAlias(List<OrganizationRepresentation> organizations, String alias) {
        return organizations.stream().anyMatch(org -> Objects.equals(org.getAlias(), alias));
    }

    private void manageIdentityProviderAssociations(
            String realmName,
            String orgId,
            OrganizationRepresentation organization
    ) {
        List<?> idpsToAssociate = organization.getIdentityProviders();
        if (idpsToAssociate == null || idpsToAssociate.isEmpty()) {
            return;
        }

        List<IdentityProviderRepresentation> existingIdps = organizationRepository.getIdentityProviders(realmName, orgId);
        Set<String> existingIdpAliases = existingIdps.stream()
                .map(IdentityProviderRepresentation::getAlias)
                .collect(Collectors.toSet());

        Set<String> configuredIdpAliases = idpsToAssociate.stream()
                .map(o -> {
                    if (o instanceof IdentityProviderRepresentation) {
                        return ((IdentityProviderRepresentation) o).getAlias();
                    }
                    if (o instanceof OrganizationRepresentation) {
                        return ((OrganizationRepresentation) o).getAlias();
                    }
                    return Objects.toString(o, null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (String idpAlias : configuredIdpAliases) {
            if (!existingIdpAliases.contains(idpAlias)) {
                try {
                    organizationRepository.addIdentityProvider(realmName, orgId, idpAlias);
                } catch (NotFoundException | BadRequestException e) {
                    logger.warn("Failed to associate identity provider '{}' with organization '{}': {}",
                            idpAlias, organization.getAlias(), e.getMessage());
                }
            }
        }

        if (importConfigProperties.getManaged().getOrganization() == ImportManagedPropertiesValues.FULL) {
            for (String existingIdpAlias : existingIdpAliases) {
                if (!configuredIdpAliases.contains(existingIdpAlias)) {
                    try {
                        logger.debug("Remove identity provider '{}' from organization '{}'", existingIdpAlias, orgId);
                        organizationRepository.removeIdentityProvider(realmName, orgId, existingIdpAlias);
                    } catch (NotFoundException | BadRequestException e) {
                        logger.warn("Failed to remove identity provider '{}' from organization '{}': {}",
                                existingIdpAlias, organization.getAlias(), e.getMessage());
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
        List<?> membersToAdd = organization.getMembers();
        if (membersToAdd == null || membersToAdd.isEmpty()) {
            return;
        }
        List<MemberRepresentation> existingMembers = organizationRepository.getMembers(realmName, orgId);
        Set<String> existingMemberUsernames = existingMembers.stream()
                .map(MemberRepresentation::getUsername)
                .collect(Collectors.toSet());

        for (Object memberObj : membersToAdd) {
            String username = null;
            if (memberObj instanceof UserRepresentation) {
                username = ((UserRepresentation) memberObj).getUsername();
            } else if (memberObj instanceof MemberRepresentation) {
                username = ((MemberRepresentation) memberObj).getUsername();
            } else if (memberObj != null) {
                username = Objects.toString(memberObj, null);
            }
            if (username == null) {
                continue;
            }

            try {
                Optional<UserRepresentation> maybeUser = userRepository.search(realmName, username);

                if (maybeUser.isEmpty()) {
                    logger.warn("Cannot add user '{}' to organization '{}': user not found in realm '{}'",
                            username, organization.getAlias(), realmName);
                    continue;
                }

                String userId = maybeUser.get().getId();

                if (!existingMemberUsernames.contains(username)) {
                    logger.debug("Add user '{}' to organization '{}'", username, organization.getAlias());
                    organizationRepository.addMember(realmName, orgId, userId);
                }
            } catch (NotFoundException | BadRequestException e) {
                logger.warn("Failed to add user '{}' to organization '{}': {}",
                        username, organization.getAlias(), e.getMessage());
            }
        }

        if (importConfigProperties.getManaged().getOrganization() == ImportManagedPropertiesValues.FULL) {
            Set<String> configuredUsernames = membersToAdd.stream()
                    .map(o -> {
                        if (o instanceof UserRepresentation) return ((UserRepresentation) o).getUsername();
                        if (o instanceof MemberRepresentation) return ((MemberRepresentation) o).getUsername();
                        return Objects.toString(o, null);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            for (MemberRepresentation existingMember : existingMembers) {
                String existingUsername = existingMember.getUsername();
                if (!configuredUsernames.contains(existingUsername)) {
                    try {
                        logger.debug("Remove user '{}' from organization '{}',",
                                existingUsername, organization.getAlias());
                        Optional<UserRepresentation> maybeUser = userRepository.search(realmName, existingUsername);
                        if (maybeUser.isPresent() && maybeUser.get().getId() != null) {
                            organizationRepository.removeMember(realmName, orgId, maybeUser.get().getId());
                        } else {
                            logger.warn("Cannot remove user '{}' from organization '{}' because user id not found",
                                    existingUsername, organization.getAlias());
                        }
                    } catch (NotFoundException | BadRequestException e) {
                        logger.warn("Failed to remove user '{}' from organization '{}': {}",
                                existingUsername, organization.getAlias(), e.getMessage());
                    }
                }
            }
        }
    }
}
