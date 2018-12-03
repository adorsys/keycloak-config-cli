package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.RealmRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RealmImportService {
    private static final Logger logger = LoggerFactory.getLogger(RealmImportService.class);

    private final String[] ignoredPropertiesForCreation = new String[]{
            "users",
            "browserFlow",
            "directGrantFlow",
            "registrationFlow",
            "resetCredentialsFlow",
            "components"
    };

    private final String[] ignoredPropertiesForUpdate = new String[]{
            "clients",
            "roles",
            "users",
            "browserFlow",
            "directGrantFlow",
            "registrationFlow",
            "resetCredentialsFlow",
            "components",
            "authenticationFlows"
    };

    private final String[] patchingPropertiesForFlowImport = new String[]{
            "browserFlow",
            "directGrantFlow",
            "registrationFlow",
            "resetCredentialsFlow",
    };

    private final Keycloak keycloak;
    private final RealmRepository realmRepository;

    private final RealmUserImportService realmUserImportService;
    private final RealmRoleImportService realmRoleImportService;
    private final RealmClientImportService realmClientImportService;
    private final RealmComponentImportService realmComponentImportService;
    private final AuthenticationFlowsImportService authenticationFlowsImportService;

    @Autowired
    public RealmImportService(
            Keycloak keycloak,
            RealmRepository realmRepository,
            RealmUserImportService realmUserImportService,
            RealmRoleImportService realmRoleImportService,
            RealmClientImportService realmClientImportService,
            RealmComponentImportService realmComponentImportService,
            AuthenticationFlowsImportService authenticationFlowsImportService
    ) {
        this.keycloak = keycloak;
        this.realmRepository = realmRepository;
        this.realmUserImportService = realmUserImportService;
        this.realmRoleImportService = realmRoleImportService;
        this.realmClientImportService = realmClientImportService;
        this.realmComponentImportService = realmComponentImportService;
        this.authenticationFlowsImportService = authenticationFlowsImportService;
    }

    public void doImport(RealmImport realmImport) {
        Optional<RealmResource> maybeRealm = realmRepository.tryToLoadRealm(realmImport.getRealm());

        if(maybeRealm.isPresent()) {
            updateRealm(realmImport);
        } else {
            createRealm(realmImport);
        }
    }

    private void createRealm(RealmImport realmImport) {
        if(logger.isDebugEnabled()) logger.debug("Creating realm '{}' ...", realmImport.getId());

        RealmRepresentation realmForCreation = CloneUtils.deepClone(realmImport, RealmRepresentation.class, ignoredPropertiesForCreation);
        keycloak.realms().create(realmForCreation);

        realmRepository.loadRealm(realmImport.getRealm());
        importUsers(realmImport);
        importFlows(realmImport);
        importComponents(realmImport);
    }

    private void importComponents(RealmImport realmImport) {
        realmComponentImportService.doImport(realmImport);
    }

    private void updateRealm(RealmImport realmImport) {
        if(logger.isDebugEnabled()) logger.debug("Updating realm '{}'...", realmImport.getId());

        RealmRepresentation realmToUpdate = CloneUtils.deepClone(realmImport, RealmRepresentation.class, ignoredPropertiesForUpdate);
        realmRepository.loadRealm(realmImport.getRealm()).update(realmToUpdate);

        setupImpersonation(realmImport);
        importClients(realmImport);
        importRoles(realmImport);
        importUsers(realmImport);
        importAuthenticationFlows(realmImport);
        importFlows(realmImport);
        importComponents(realmImport);
    }

    private void importAuthenticationFlows(RealmImport realmImport) {
        authenticationFlowsImportService.doImport(realmImport);
    }

    private void importUsers(RealmImport realmImport) {
        List<UserRepresentation> users = realmImport.getUsers();

        if(users != null) {
            for(UserRepresentation user : users) {
                realmUserImportService.importUser(realmImport.getRealm(), user);
            }
        }
    }

    private void importRoles(RealmImport realmImport) {
        realmRoleImportService.doImport(realmImport);
    }

    private void importClients(RealmImport realmImport) {
        realmClientImportService.doImport(realmImport);
    }

    private void importFlows(RealmImport realmImport) {
        RealmResource realmResource = realmRepository.loadRealm(realmImport.getRealm());
        RealmRepresentation existingRealm = realmResource.toRepresentation();

        RealmRepresentation realmToUpdate = CloneUtils.deepPatchFieldsOnly(existingRealm, realmImport, patchingPropertiesForFlowImport);
        realmResource.update(realmToUpdate);
    }

    private void setupImpersonation(RealmImport realmImport) {
        realmImport.getCustomImport().ifPresent(customImport -> {
            if(customImport.removeImpersonation()) {
                RealmResource master = keycloak.realm("master");

                String clientId = realmImport.getRealm() + "-realm";
                List<ClientRepresentation> foundClients = master.clients()
                        .findByClientId(clientId);

                if(!foundClients.isEmpty()) {
                    ClientRepresentation client = foundClients.get(0);
                    ClientResource clientResource = master.clients()
                            .get(client.getId());

                    RoleResource impersonationRole = clientResource.roles().get("impersonation");

                    try {
                        impersonationRole.remove();
                    } catch(javax.ws.rs.NotFoundException e) {
                        if(logger.isInfoEnabled()) logger.info("Cannot remove 'impersonation' role from client '{}' in 'master' realm: Not found", clientId);
                    }
                }
            }
        });
    }
}
