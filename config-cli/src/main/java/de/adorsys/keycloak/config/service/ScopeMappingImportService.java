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

        for (ScopeMappingRepresentation scopeMappingToImport : scopeMappingsToImport) {
            String scopeMappingClient = scopeMappingToImport.getClient();

            Optional<ScopeMappingRepresentation> maybeExistingScopeMapping = tryToFindScopeMappingByClient(existingScopeMappings, scopeMappingClient);

            if(maybeExistingScopeMapping.isPresent()) {
                updateScopeMappings(realm, scopeMappingToImport, maybeExistingScopeMapping.get());
            } else {
                logger.debug("Adding scope-mapping for client '{}' in realm '{}'", scopeMappingToImport.getClient(), realm);
            }
        }
    }

    private void updateScopeMappings(String realm, ScopeMappingRepresentation scopeMappingToImport, ScopeMappingRepresentation existingScopeMapping) {
        String client = existingScopeMapping.getClient();

        Set<String> existingScopeMappingRoles = existingScopeMapping.getRoles();
        Set<String> scopeMappingRolesToImport = scopeMappingToImport.getRoles();

        List<String> rolesToBeAdded = scopeMappingRolesToImport.stream()
                .filter(role -> !existingScopeMappingRoles.contains(role))
                .collect(Collectors.toList());

        if(!rolesToBeAdded.isEmpty()) {
            logger.debug("Add roles '{}' to scope-mapping for client '{}' in realm '{}'", rolesToBeAdded, client, realm);
            scopeMappingRepository.addScopeMappingRole(realm, client, rolesToBeAdded);
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
