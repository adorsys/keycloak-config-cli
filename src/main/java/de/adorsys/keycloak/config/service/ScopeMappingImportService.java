/*
 * Copyright 2019-2020 adorsys GmbH & Co. KG @ https://adorsys.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.RealmRepository;
import de.adorsys.keycloak.config.repository.ScopeMappingRepository;
import org.jboss.logging.Logger;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Dependent
public class ScopeMappingImportService {
    private static final Logger LOG = Logger.getLogger(ScopeMappingImportService.class);

    @Inject
    RealmRepository realmRepository;

    @Inject
    ScopeMappingRepository scopeMappingRepository;

    public void doImport(RealmImport realmImport) {
        createOrUpdateScopeMappings(realmImport);
    }

    private void createOrUpdateScopeMappings(RealmImport realmImport) {
        String realm = realmImport.getRealm();
        List<ScopeMappingRepresentation> scopeMappingsToImport = realmImport.getScopeMappings();
        RealmRepresentation existingRealm = realmRepository.partialExport(realm);
        List<ScopeMappingRepresentation> existingScopeMappings = existingRealm.getScopeMappings();

        // an omitted scope-mappings array will be ignored - a empty array will delete all scope-mappings
        if (scopeMappingsToImport != null) {
            createOrUpdateRolesInScopeMappings(realm, scopeMappingsToImport, existingScopeMappings);
            cleanupRolesInScopeMappingsIfNecessary(realm, scopeMappingsToImport, existingScopeMappings);
        } else {
            LOG.tracef("Omitting scope-mappings for realm '%s'", realm);
        }
    }

    private void createOrUpdateRolesInScopeMappings(String realm, List<ScopeMappingRepresentation> scopeMappingsToImport, List<ScopeMappingRepresentation> existingScopeMappings) {
        for (ScopeMappingRepresentation scopeMappingToImport : scopeMappingsToImport) {
            Optional<ScopeMappingRepresentation> maybeExistingScopeMapping = tryToFindExistingScopeMapping(existingScopeMappings, scopeMappingToImport);

            if (maybeExistingScopeMapping.isPresent()) {
                updateScopeMappings(realm, scopeMappingToImport, maybeExistingScopeMapping.get());
            } else {
                LOG.debugf("Adding scope-mapping with roles '%s' for %s '%s' in realm '%s'",
                        scopeMappingToImport.getRoles(),
                        scopeMappingToImport.getClient() == null ? "client-scope" : "client",
                        scopeMappingToImport.getClient() == null ? scopeMappingToImport.getClientScope() : scopeMappingToImport.getClient(),
                        realm
                );

                scopeMappingRepository.addScopeMapping(realm, scopeMappingToImport);
            }
        }
    }

    private void cleanupRolesInScopeMappingsIfNecessary(String realm, List<ScopeMappingRepresentation> scopeMappingsToImport, List<ScopeMappingRepresentation> existingScopeMappings) {
        if (existingScopeMappings != null) {
            cleanupRolesInScopeMappings(realm, scopeMappingsToImport, existingScopeMappings);
        }
    }

    private void cleanupRolesInScopeMappings(String realm, List<ScopeMappingRepresentation> scopeMappingsToImport, List<ScopeMappingRepresentation> existingScopeMappings) {
        for (ScopeMappingRepresentation existingScopeMapping : existingScopeMappings) {
            if (hasToBeDeleted(scopeMappingsToImport, existingScopeMapping)) {
                cleanupRolesInScopeMapping(realm, existingScopeMapping);
            }
        }
    }

    private void cleanupRolesInScopeMapping(String realm, ScopeMappingRepresentation existingScopeMapping) {
        String client = existingScopeMapping.getClient();
        String clientScope = existingScopeMapping.getClientScope();

        if (client != null) {
            LOG.debugf("Remove all roles from scope-mapping for client '%s' in realm '%s'", client, realm);
            scopeMappingRepository.removeScopeMappingRolesForClient(realm, client, existingScopeMapping.getRoles());
        } else if (clientScope != null) {
            LOG.debugf("Remove all roles from scope-mapping for client-scope '%s' in realm '%s'", clientScope, realm);
            scopeMappingRepository.removeScopeMappingRolesForClientScope(realm, clientScope, existingScopeMapping.getRoles());
        }
    }

    private boolean hasToBeDeleted(List<ScopeMappingRepresentation> scopeMappingsToImport, ScopeMappingRepresentation existingScopeMapping) {
        return !existingScopeMapping.getRoles().isEmpty()
                && scopeMappingsToImport.stream()
                .filter(scopeMappingToImport -> areScopeMappingsEqual(scopeMappingToImport, existingScopeMapping))
                .count() < 1;
    }

    private void updateScopeMappings(String realm, ScopeMappingRepresentation scopeMappingToImport, ScopeMappingRepresentation existingScopeMapping) {
        Set<String> scopeMappingRolesToImport = scopeMappingToImport.getRoles();

        addRoles(realm, existingScopeMapping, scopeMappingRolesToImport);
        removeRoles(realm, existingScopeMapping, scopeMappingRolesToImport);
    }

    private void removeRoles(String realm, ScopeMappingRepresentation existingScopeMapping, Set<String> scopeMappingRolesToImport) {
        Set<String> existingScopeMappingRoles = existingScopeMapping.getRoles();

        List<String> rolesToBeRemoved = existingScopeMappingRoles.stream()
                .filter(role -> !scopeMappingRolesToImport.contains(role))
                .collect(Collectors.toList());

        String client = existingScopeMapping.getClient();
        String clientScope = existingScopeMapping.getClientScope();

        removeRolesFromScopeMappingIfNecessary(realm, rolesToBeRemoved, client, clientScope);
    }

    private void addRoles(String realm, ScopeMappingRepresentation existingScopeMapping, Set<String> scopeMappingRolesToImport) {
        String client = existingScopeMapping.getClient();
        String clientScope = existingScopeMapping.getClientScope();

        Set<String> existingScopeMappingRoles = existingScopeMapping.getRoles();

        List<String> rolesToBeAdded = scopeMappingRolesToImport.stream()
                .filter(role -> !existingScopeMappingRoles.contains(role))
                .collect(Collectors.toList());

        addRolesToScopeMappingIfNecessary(realm, client, clientScope, rolesToBeAdded);
    }

    private void addRolesToScopeMappingIfNecessary(String realm, String client, String clientScope, List<String> rolesToBeAdded) {
        if (!rolesToBeAdded.isEmpty()) {
            if (client != null) {
                LOG.debugf("Add roles '%s' to scope-mapping for client '%s' in realm '%s'", rolesToBeAdded, client, realm);
                scopeMappingRepository.addScopeMappingRolesForClient(realm, client, rolesToBeAdded);
            } else if (clientScope != null) {
                LOG.debugf("Add roles '%s' to scope-mapping for client-scope '%s' in realm '%s'", rolesToBeAdded, clientScope, realm);
                scopeMappingRepository.addScopeMappingRolesForClientScope(realm, clientScope, rolesToBeAdded);
            }
        } else {
            if (client != null) {
                LOG.tracef("No need to add roles to scope-mapping for client '%s' in realm '%s'", client, realm);
            } else if (clientScope != null) {
                LOG.tracef("No need to add roles to scope-mapping for client-scope '%s' in realm '%s'", clientScope, realm);
            }
        }
    }

    private void removeRolesFromScopeMappingIfNecessary(String realm, List<String> rolesToBeRemoved, String client, String clientScope) {
        if (!rolesToBeRemoved.isEmpty()) {
            if (client != null) {
                LOG.debugf("Remove roles '%s' from scope-mapping for client '%s' in realm '%s'", rolesToBeRemoved, client, realm);
                scopeMappingRepository.removeScopeMappingRolesForClient(realm, client, rolesToBeRemoved);
            } else if (clientScope != null) {
                LOG.debugf("Remove roles '%s' from scope-mapping for client-scope '%s' in realm '%s'", rolesToBeRemoved, clientScope, realm);
                scopeMappingRepository.removeScopeMappingRolesForClientScope(realm, clientScope, rolesToBeRemoved);
            }
        } else {
            if (client != null) {
                LOG.tracef("No need to remove roles to scope-mapping for client '%s' in realm '%s'", client, realm);
            } else if (clientScope != null) {
                LOG.tracef("No need to remove roles to scope-mapping for client-scope '%s' in realm '%s'", clientScope, realm);
            }
        }
    }

    private Optional<ScopeMappingRepresentation> tryToFindExistingScopeMapping(List<ScopeMappingRepresentation> scopeMappings, ScopeMappingRepresentation scopeMappingToBeFound) {
        if (scopeMappings == null) {
            return Optional.empty();
        }

        return scopeMappings.stream()
                .filter(scopeMapping -> areScopeMappingsEqual(scopeMapping, scopeMappingToBeFound))
                .findFirst();
    }

    public boolean areScopeMappingsEqual(ScopeMappingRepresentation first, ScopeMappingRepresentation second) {
        if (first == second) {
            return true;
        }

        if (first == null || second == null) {
            return false;
        }

        String client = first.getClient();
        String clientScope = first.getClientScope();

        return Objects.equals(second.getClient(), client)
                && Objects.equals(second.getClientScope(), clientScope);
    }
}
