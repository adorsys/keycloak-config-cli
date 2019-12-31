package de.adorsys.keycloak.config;

import de.adorsys.keycloak.config.configuration.TestConfiguration;
import de.adorsys.keycloak.config.model.KeycloakImport;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.service.KeycloakImportProvider;
import de.adorsys.keycloak.config.service.KeycloakProvider;
import de.adorsys.keycloak.config.service.RealmImportService;
import de.adorsys.keycloak.config.util.KeycloakAuthentication;
import de.adorsys.keycloak.config.util.KeycloakRepository;
import de.adorsys.keycloak.config.util.ResourceLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(
        classes = { TestConfiguration.class },
        initializers = { ConfigFileApplicationContextInitializer.class }
)
@ActiveProfiles("IT")
@DirtiesContext
public class ImportGroupsIT {
    private static final String REALM_NAME = "realmWithGroups";

    @Autowired
    RealmImportService realmImportService;

    @Autowired
    KeycloakImportProvider keycloakImportProvider;

    @Autowired
    KeycloakProvider keycloakProvider;

    @Autowired
    KeycloakRepository keycloakRepository;

    @Autowired
    KeycloakAuthentication keycloakAuthentication;

    KeycloakImport keycloakImport;

    @Before
    public void setup() throws Exception {
        File configsFolder = ResourceLoader.loadResource("import-files/groups");
        this.keycloakImport = keycloakImportProvider.readRealmImportsFromDirectory(configsFolder);
    }

    @After
    public void cleanup() throws Exception {
        keycloakProvider.close();
    }

    @Test
    public void shouldReadImports() {
        assertThat(keycloakImport, is(not(nullValue())));
    }

    @Test
    public void integrationTests() throws Exception {
//        shouldCreateRealmWithGroup();
//        shouldUpdateRealmAddGroup();
//        shouldUpdateRealmAddRoleToGroup();
        shouldUpdateRealmRemoveRoleFromGroup();
    }

    private void shouldCreateRealmWithGroup() throws Exception {
        doImport("0_create_realm_with_group.json");

        RealmResource realmResource = keycloakProvider.get().realm(REALM_NAME);

        GroupRepresentation createGroup = realmResource.getGroupByPath("/my_realm_group_user");

        assertThat(createGroup.getName(), is("my_realm_group_user"));
        assertThat(createGroup.getRealmRoles(), contains("my_realm_role_user"));
    }

    private void shouldUpdateRealmAddGroup() throws Exception {
        doImport("1_update_realm__add_group.json");

        RealmResource realmResource = keycloakProvider.get().realm(REALM_NAME);

        GroupRepresentation createGroup = realmResource.getGroupByPath("/my_realm_group_admin");

        assertThat(createGroup.getName(), is("my_realm_group_admin"));
        assertThat(createGroup.getRealmRoles(), contains("my_realm_role_admin"));
    }

    private void shouldUpdateRealmAddRoleToGroup() throws Exception {
        doImport("2_update_realm__add_role_to_group.json");

        RealmResource realmResource = keycloakProvider.get().realm(REALM_NAME);

        GroupRepresentation createGroup = realmResource.getGroupByPath("/my_realm_group_admin");

        assertThat(createGroup.getRealmRoles(), containsInAnyOrder("my_realm_role_admin", "my_realm_role_user"));
    }

    private void shouldUpdateRealmRemoveRoleFromGroup() throws Exception {
        doImport("3_update_realm__remove_role_from_group.json");

        RealmResource realmResource = keycloakProvider.get().realm(REALM_NAME);

        GroupRepresentation createGroup = realmResource.getGroupByPath("/my_realm_group_admin");

        assertThat(createGroup.getRealmRoles(), containsInAnyOrder("my_realm_role_admin"));
        assertThat(createGroup.getRealmRoles(),not(containsInAnyOrder("my_realm_role_user")));
    }

    private void doImport(String realmImport) {
        RealmImport foundImport = getImport(realmImport);
        realmImportService.doImport(foundImport);
    }

    private RealmImport getImport(String importName) {
        Map<String, RealmImport> realmImports = keycloakImport.getRealmImports();

        return realmImports.entrySet()
                .stream()
                .filter(e -> e.getKey().equals(importName))
                .map(Map.Entry::getValue)
                .findFirst()
                .get();
    }
}
