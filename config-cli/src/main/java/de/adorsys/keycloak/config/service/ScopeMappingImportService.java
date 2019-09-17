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

        createOrUpdateRolesInScopeMappings(realm, scopeMappingsToImport, existingScopeMappings);
        cleanupRolesInScopeMappings(realm, scopeMappingsToImport, existingScopeMappings);
    }

    private void createOrUpdateRolesInScopeMappings(String realm, List<ScopeMappingRepresentation> scopeMappingsToImport, List<ScopeMappingRepresentation> existingScopeMappings) {
        for (ScopeMappingRepresentation scopeMappingToImport : scopeMappingsToImport) {
            String scopeMappingClient = scopeMappingToImport.getClient();

            Optional<ScopeMappingRepresentation> maybeExistingScopeMapping = tryToFindScopeMappingByClient(existingScopeMappings, scopeMappingClient);

            if (maybeExistingScopeMapping.isPresent()) {
                updateScopeMappings(realm, scopeMappingToImport, maybeExistingScopeMapping.get());
            } else {
                logger.debug("Adding scope-mapping with roles '{}' for client '{}' in realm '{}'", scopeMappingToImport.getRoles(), scopeMappingToImport.getClient(), realm);

                scopeMappingRepository.addScopeMapping(realm, scopeMappingToImport);
            }
        }
    }

    private void cleanupRolesInScopeMappings(String realm, List<ScopeMappingRepresentation> scopeMappingsToImport, List<ScopeMappingRepresentation> existingScopeMappings) {
        for (ScopeMappingRepresentation existingScopeMapping : existingScopeMappings) {
            if (hasToBeDeleted(scopeMappingsToImport, existingScopeMapping)) {
                String client = existingScopeMapping.getClient();

                logger.debug("Remove all roles from scope-mapping for client '{}' in realm '{}'", client, realm);

                scopeMappingRepository.removeScopeMappingRoles(realm, client, existingScopeMapping.getRoles());
            }
        }
    }

    private boolean hasToBeDeleted(List<ScopeMappingRepresentation> scopeMappingsToImport, ScopeMappingRepresentation existingScopeMapping) {
        return !existingScopeMapping.getRoles().isEmpty()
                && scopeMappingsToImport.stream()
                .filter(scopeMappingRepresentation -> Objects.equals(scopeMappingRepresentation.getClient(), existingScopeMapping.getClient()))
                .count() < 1;
    }

    private void updateScopeMappings(String realm, ScopeMappingRepresentation scopeMappingToImport, ScopeMappingRepresentation existingScopeMapping) {
        String client = existingScopeMapping.getClient();

        Set<String> existingScopeMappingRoles = existingScopeMapping.getRoles();
        Set<String> scopeMappingRolesToImport = scopeMappingToImport.getRoles();

        addRoles(realm, client, existingScopeMappingRoles, scopeMappingRolesToImport);
        removeRoles(realm, client, existingScopeMappingRoles, scopeMappingRolesToImport);
    }

    private void removeRoles(String realm, String client, Set<String> existingScopeMappingRoles, Set<String> scopeMappingRolesToImport) {
        List<String> rolesToBeRemoved = existingScopeMappingRoles.stream()
                .filter(role -> !scopeMappingRolesToImport.contains(role))
                .collect(Collectors.toList());

        if (!rolesToBeRemoved.isEmpty()) {
            logger.debug("Remove roles '{}' from scope-mapping for client '{}' in realm '{}'", rolesToBeRemoved, client, realm);

            scopeMappingRepository.removeScopeMappingRoles(realm, client, rolesToBeRemoved);
        } else {
            logger.debug("No need to remove roles to scope-mapping for client '{}' in realm '{}'", client, realm);
        }
    }

    private void addRoles(String realm, String client, Set<String> existingScopeMappingRoles, Set<String> scopeMappingRolesToImport) {
        List<String> rolesToBeAdded = scopeMappingRolesToImport.stream()
                .filter(role -> !existingScopeMappingRoles.contains(role))
                .collect(Collectors.toList());

        if (!rolesToBeAdded.isEmpty()) {
            logger.debug("Add roles '{}' to scope-mapping for client '{}' in realm '{}'", rolesToBeAdded, client, realm);

            scopeMappingRepository.addScopeMappingRoles(realm, client, rolesToBeAdded);
        } else {
            logger.debug("No need to add roles to scope-mapping for client '{}' in realm '{}'", client, realm);
        }
    }

    private Optional<ScopeMappingRepresentation> tryToFindScopeMappingByClient(List<ScopeMappingRepresentation> scopeMappings, String client) {
        if (scopeMappings == null) {
            return Optional.empty();
        }

        return scopeMappings.stream()
                .filter(scopeMapping -> Objects.equals(scopeMapping.getClient(), client))
                .findFirst();
    }
}
