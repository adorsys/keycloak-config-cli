package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.RealmRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RealmImportService {
    private static final Logger logger = LoggerFactory.getLogger(RealmImportService.class);

    private static final String REALM_CHECKSUM_ATTRIBUTE_KEY = "de.adorsys.keycloak.config.import-checksum";

    private final String[] ignoredPropertiesForCreation = new String[]{
            "users",
            "browserFlow",
            "directGrantFlow",
            "clientAuthenticationFlow",
            "dockerAuthenticationFlow",
            "registrationFlow",
            "resetCredentialsFlow",
            "components",
            "authenticationFlows"
    };

    private final String[] ignoredPropertiesForUpdate = new String[]{
            "clients",
            "roles",
            "users",
            "browserFlow",
            "directGrantFlow",
            "clientAuthenticationFlow",
            "dockerAuthenticationFlow",
            "registrationFlow",
            "resetCredentialsFlow",
            "components",
            "authenticationFlows",
            "requiredActions"
    };

    private final String[] patchingPropertiesForFlowImport = new String[]{
            "browserFlow",
            "directGrantFlow",
            "clientAuthenticationFlow",
            "dockerAuthenticationFlow",
            "registrationFlow",
            "resetCredentialsFlow",
    };

    private final KeycloakProvider keycloakProvider;
    private final RealmRepository realmRepository;

    private final UserImportService userImportService;
    private final RoleImportService roleImportService;
    private final ClientImportService clientImportService;
    private final ComponentImportService componentImportService;
    private final AuthenticationFlowsImportService authenticationFlowsImportService;
    private final RequiredActionsImportService requiredActionsImportService;
    private final CustomImportService customImportService;
    private final ScopeMappingImportService scopeMappingImportService;

    @Value("${import.force:#{false}}")
    private Boolean forceImport;


    @Autowired
    public RealmImportService(
            KeycloakProvider keycloakProvider,
            RealmRepository realmRepository,
            UserImportService userImportService,
            RoleImportService roleImportService,
            ClientImportService clientImportService,
            ComponentImportService componentImportService,
            AuthenticationFlowsImportService authenticationFlowsImportService,
            RequiredActionsImportService requiredActionsImportService,
            CustomImportService customImportService,
            ScopeMappingImportService scopeMappingImportService
    ) {
        this.keycloakProvider = keycloakProvider;
        this.realmRepository = realmRepository;
        this.userImportService = userImportService;
        this.roleImportService = roleImportService;
        this.clientImportService = clientImportService;
        this.componentImportService = componentImportService;
        this.authenticationFlowsImportService = authenticationFlowsImportService;
        this.requiredActionsImportService = requiredActionsImportService;
        this.customImportService = customImportService;
        this.scopeMappingImportService = scopeMappingImportService;
    }

    public void doImport(RealmImport realmImport) {
        boolean realmExists = realmRepository.exists(realmImport.getRealm());

        if(realmExists) {
            updateRealmIfNecessary(realmImport);
        } else {
            createRealm(realmImport);
        }

        keycloakProvider.close();
    }

    private void createRealm(RealmImport realmImport) {
        logger.debug("Creating realm '{}' ...", realmImport.getRealm());

        RealmRepresentation realmForCreation = CloneUtils.deepClone(realmImport, RealmRepresentation.class, ignoredPropertiesForCreation);
        realmRepository.create(realmForCreation);

        realmRepository.loadRealm(realmImport.getRealm());
        importUsers(realmImport);
        authenticationFlowsImportService.doImport(realmImport);
        setupFlows(realmImport);
        importComponents(realmImport);
        customImportService.doImport(realmImport);
        setupImportChecksum(realmImport);
    }

    private void importComponents(RealmImport realmImport) {
        componentImportService.doImport(realmImport);
    }

    private void updateRealmIfNecessary(RealmImport realmImport) {
        if(forceImport || hasToBeUpdated(realmImport)) {
            updateRealm(realmImport);
        } else {
            logger.debug(
                    "No need to update realm '{}', import checksum same: '{}'",
                    realmImport.getRealm(),
                    realmImport.getChecksum()
            );
        }
    }

    private void updateRealm(RealmImport realmImport) {
        logger.debug("Updating realm '{}'...", realmImport.getRealm());

        RealmRepresentation realmToUpdate = CloneUtils.deepClone(realmImport, RealmRepresentation.class, ignoredPropertiesForUpdate);
        realmRepository.update(realmToUpdate);

        clientImportService.doImport(realmImport);
        roleImportService.doImport(realmImport);
        importUsers(realmImport);
        importRequiredActions(realmImport);
        authenticationFlowsImportService.doImport(realmImport);
        setupFlows(realmImport);
        importComponents(realmImport);
        scopeMappingImportService.doImport(realmImport);
        customImportService.doImport(realmImport);
        setupImportChecksum(realmImport);
    }

    private void importRequiredActions(RealmImport realmImport) {
        requiredActionsImportService.doImport(realmImport);
    }

    private void importUsers(RealmImport realmImport) {
        List<UserRepresentation> users = realmImport.getUsers();

        if(users != null) {
            for(UserRepresentation user : users) {
                userImportService.importUser(realmImport.getRealm(), user);
            }
        }
    }

    private void setupFlows(RealmImport realmImport) {
        RealmRepresentation existingRealm = realmRepository.get(realmImport.getRealm());
        RealmRepresentation realmToUpdate = CloneUtils.deepPatchFieldsOnly(existingRealm, realmImport, patchingPropertiesForFlowImport);

        realmRepository.update(realmToUpdate);
    }

    private boolean hasToBeUpdated(RealmImport realmImport) {
        RealmRepresentation existingRealm = realmRepository.get(realmImport.getRealm());
        Map<String, String> customAttributes = existingRealm.getAttributes();
        String readChecksum = customAttributes.get(REALM_CHECKSUM_ATTRIBUTE_KEY);

        return !realmImport.getChecksum().equals(readChecksum);
    }

    private void setupImportChecksum(RealmImport realmImport) {
        RealmRepresentation existingRealm = realmRepository.get(realmImport.getRealm());
        Map<String, String> customAttributes = existingRealm.getAttributes();

        String importChecksum = realmImport.getChecksum();
        customAttributes.put(REALM_CHECKSUM_ATTRIBUTE_KEY, importChecksum);
        realmRepository.update(existingRealm);

        logger.debug("Updated import checksum of realm '{}' to '{}'", realmImport.getRealm(), importChecksum);
    }
}
