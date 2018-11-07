package de.adorsys.keycloak.config.realm;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.core.Response;

import de.adorsys.keycloak.config.utils.PartialUpdater;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ComponentResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RealmConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(RealmConfigService.class);
    private final Keycloak keycloak;

    @Autowired
    public RealmConfigService(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    public void createOrUpdateRealm(RealmRepresentation realmConfig) throws IOException {
        final String realmId = realmConfig.getId();
        RealmResource realm = getRealm(realmId);
        if (realm == null) {
            LOG.debug("Creating realm '{}'.", realmConfig.getId());
            keycloak.realms().create(realmConfig);
        } else {
            LOG.debug("Updating realm '{}'.", realmConfig.getId());
            RealmRepresentation representation = realm.toRepresentation();
            representation = PartialUpdater.deepPatchObject(realmConfig, representation);
            realm.update(representation);
        }
    }

    public void createOrUpdateRealmComponents(String realmId, List<ComponentDefinition> components) throws IOException {
        RealmResource realm = getRealm(realmId);
        createOrUpdateComponents(components, realm, realmId);
    }

    private void createOrUpdateComponents(List<ComponentDefinition> components, RealmResource realm, String parentId) throws IOException {
        for (ComponentDefinition componentDef : components) {
            ComponentRepresentation src = componentDef.toRepresentation(parentId);

            // Select components name
            List<ComponentRepresentation> comps = realm.components().query(parentId, src.getProviderType(), src.getName());

            if (!comps.isEmpty()) {
                // Update each component found
                for (ComponentRepresentation rep : comps) {
                	ComponentResource proxy = realm.components().component(rep.getId());
                	ComponentRepresentation dest = proxy.toRepresentation();
                	dest = PartialUpdater.deepPatchObject(src, dest);
                	proxy.update(dest);
                	LOG.debug("Updated component '{}'.", dest.getName());
                    if (componentDef.getChildren() != null) {
                        createOrUpdateComponents(componentDef.getChildren(), realm, dest.getId());
                    }
    			}
            } else {
                Response response = realm.components().add(src);

                if (response.getLocation() != null) {
                	ComponentResource proxy = keycloak.proxy(ComponentResource.class, response.getLocation());
                    ComponentRepresentation dest = proxy.toRepresentation();
                    LOG.debug("Created component '{}'.", dest.getName());

                    if (componentDef.getChildren() != null) {
                        createOrUpdateComponents(componentDef.getChildren(), realm, dest.getId());
                    }
                } else {
                    throw new RuntimeException("Unable to create component " + src.getName() + ", Response status: " + response.getStatus() + " " + response.getEntity());
                }
                response.close();
            }
        }
    }

    private RealmResource getRealm(String id) {
        RealmResource realm = keycloak.realms().realm(id);
        try {
            realm.toRepresentation();
            return realm;
        } catch (javax.ws.rs.NotFoundException nnfe) {
            return null;
        }
    }
}
