package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.RealmRepository;
import de.adorsys.keycloak.config.repository.ScopeMappingRepository;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ScopeMappingImportService {
    private static final Logger logger = LoggerFactory.getLogger(ScopeMappingImportService.class);

    private final RealmRepository realmRepository;
    private final ScopeMappingRepository scopeMappingRepository;

    @Autowired
    public ScopeMappingImportService(
            RealmRepository realmRepository,
            ScopeMappingRepository scopeMappingRepository
    ) {
        this.realmRepository = realmRepository;
        this.scopeMappingRepository = scopeMappingRepository;
    }

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
            logger.trace("Omitting scope-mappings for realm '{}'", realm);
        }
    }

    private void createOrUpdateRolesInScopeMappings(String realm, List<ScopeMappingRepresentation> scopeMappingsToImport, List<ScopeMappingRepresentation> existingScopeMappings) {
        for (ScopeMappingRepresentation scopeMappingToImport : scopeMappingsToImport) {
            Optional<ScopeMappingRepresentation> maybeExistingScopeMapping = tryToFindExistingScopeMapping(existingScopeMappings, scopeMappingToImport);

            if (maybeExistingScopeMapping.isPresent()) {
                updateScopeMappings(realm, scopeMappingToImport, maybeExistingScopeMapping.get());
            } else {
                logger.debug("Adding scope-mapping with roles '{}' for client '{}' in realm '{}'", scopeMappingToImport.getRoles(), scopeMappingToImport.getClient(), realm);

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
            logger.debug("Remove all roles from scope-mapping for client '{}' in realm '{}'", client, realm);
            scopeMappingRepository.removeScopeMappingRolesForClient(realm, client, existingScopeMapping.getRoles());
        } else if (clientScope != null) {
            logger.debug("Remove all roles from scope-mapping for client-scope '{}' in realm '{}'", clientScope, realm);
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
                logger.debug("Add roles '{}' to scope-mapping for client '{}' in realm '{}'", rolesToBeAdded, client, realm);
                scopeMappingRepository.addScopeMappingRolesForClient(realm, client, rolesToBeAdded);
            } else if (clientScope != null) {
                logger.debug("Add roles '{}' to scope-mapping for client-scope '{}' in realm '{}'", rolesToBeAdded, clientScope, realm);
                scopeMappingRepository.addScopeMappingRolesForClientScope(realm, clientScope, rolesToBeAdded);
            }
        } else {
            if (client != null) {
                logger.trace("No need to add roles to scope-mapping for client '{}' in realm '{}'", client, realm);
            } else if (clientScope != null) {
                logger.trace("No need to add roles to scope-mapping for client-scope '{}' in realm '{}'", clientScope, realm);
            }
        }
    }

    private void removeRolesFromScopeMappingIfNecessary(String realm, List<String> rolesToBeRemoved, String client, String clientScope) {
        if (!rolesToBeRemoved.isEmpty()) {
            if (client != null) {
                logger.debug("Remove roles '{}' from scope-mapping for client '{}' in realm '{}'", rolesToBeRemoved, client, realm);
                scopeMappingRepository.removeScopeMappingRolesForClient(realm, client, rolesToBeRemoved);
            } else if (clientScope != null) {
                logger.debug("Remove roles '{}' from scope-mapping for client-scope '{}' in realm '{}'", rolesToBeRemoved, clientScope, realm);
                scopeMappingRepository.removeScopeMappingRolesForClientScope(realm, clientScope, rolesToBeRemoved);
            }
        } else {
            if (client != null) {
                logger.trace("No need to remove roles to scope-mapping for client '{}' in realm '{}'", client, realm);
            } else if (clientScope != null) {
                logger.trace("No need to remove roles to scope-mapping for client-scope '{}' in realm '{}'", clientScope, realm);
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
