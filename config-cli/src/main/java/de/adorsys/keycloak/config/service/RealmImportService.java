package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
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

import java.util.List;
import java.util.Optional;

public class RealmImportService {
    private static final Logger logger = LoggerFactory.getLogger(RealmImportService.class);

    private final String[] realmSpecialPropertiesForCreation = new String[]{
            "users",
            "registrationFlow",
            "resetCredentialsFlow"
    };

    private final String[] realmSpecialPropertiesForUpdate = new String[]{
            "clients",
            "roles",
            "users",
            "registrationFlow",
            "resetCredentialsFlow"
    };

    private final String[] realmSpecialPropertiesForFlowHandling = new String[]{
            "clients",
            "roles",
            "users"
    };

    private final Keycloak keycloak;
    private final RealmImport realmImport;
    private RealmResource realmResource;

    public RealmImportService(Keycloak keycloak, RealmImport realmImport) {
        this.keycloak = keycloak;
        this.realmImport = realmImport;
    }

    public void doImport() {
        Optional<RealmResource> maybeRealm = tryToLoadRealm();

        if(maybeRealm.isPresent()) {
            realmResource = maybeRealm.get();
            updateRealm();
        } else {
            createRealm();
        }
    }

    private Optional<RealmResource> tryToLoadRealm() {
        Optional<RealmResource> loadedRealm;

        try {
            RealmResource realm = loadRealm();

            // check here if realm is present, otherwise this method throws an NotFoundException
            realm.toRepresentation();

            loadedRealm = Optional.of(realm);
        } catch (javax.ws.rs.NotFoundException e) {
            loadedRealm = Optional.empty();
        }

        return loadedRealm;
    }

    private RealmResource loadRealm() {
        return keycloak.realms().realm(realmImport.getRealm());
    }

    private void createRealm() {
        if(logger.isDebugEnabled()) logger.debug("Creating realm '{}' ...", realmImport.getId());

        RealmRepresentation realmForCreation = CloneUtils.deepClone(realmImport, RealmRepresentation.class, realmSpecialPropertiesForCreation);
        keycloak.realms().create(realmForCreation);

        realmResource = loadRealm();
        handleUsers();
        handleFlows();
    }

    private void updateRealm() {
        if(logger.isDebugEnabled()) logger.debug("Updating realm '{}'...", realmImport.getId());

        RealmRepresentation realmToUpdate = CloneUtils.deepClone(realmImport, RealmRepresentation.class, realmSpecialPropertiesForUpdate);
        realmResource.update(realmToUpdate);

        handleImpersonation();
        handleClients();
        handleRoles();
        handleUsers();
        handleFlows();
    }

    private void handleUsers() {
        List<UserRepresentation> users = realmImport.getUsers();

        if(users != null) {
            for(UserRepresentation user : users) {
                RealmUserImportService realmUserImportService = new RealmUserImportService(realmResource, realmImport);
                realmUserImportService.importUser(user);
            }
        }
    }

    private void handleRoles() {
        RealmRoleImportService realmRoleImportService = new RealmRoleImportService(realmImport, realmResource);
        realmRoleImportService.doImport();
    }

    private void handleClients() {
        RealmClientImportService realmClientImportService = new RealmClientImportService(realmImport, realmResource);
        realmClientImportService.doImport();
    }

    private void handleFlows() {
        RealmRepresentation realmToUpdate = CloneUtils.deepClone(realmImport, RealmRepresentation.class, realmSpecialPropertiesForFlowHandling);
        realmResource.update(realmToUpdate);
    }

    private void handleImpersonation() {
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
