package de.adorsys.keycloak.config;

import de.adorsys.keycloak.config.configuration.TestConfiguration;
import de.adorsys.keycloak.config.model.KeycloakImport;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.service.KeycloakImportProvider;
import de.adorsys.keycloak.config.service.KeycloakProvider;
import de.adorsys.keycloak.config.service.RealmImportService;
import de.adorsys.keycloak.config.util.ResourceLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
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
public class ImportComponentsIT {
    private static final String REALM_NAME = "realmWithComponents";

    @Autowired
    RealmImportService realmImportService;

    @Autowired
    KeycloakImportProvider keycloakImportProvider;

    @Autowired
    KeycloakProvider keycloakProvider;

    KeycloakImport keycloakImport;

    @Before
    public void setup() throws Exception {
        File configsFolder = ResourceLoader.loadResource("import-files/components");
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
        shouldCreateRealmWithComponent();
        shouldUpdateComponentsConfig();
        shouldUpdateAddComponentsConfig();
        shouldAddComponentForSameProviderType();
    }

    private void shouldCreateRealmWithComponent() throws Exception {
        doImport("0_create_realm_with_component.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        ComponentRepresentation createdComponent = getComponent(
                "org.keycloak.keys.KeyProvider",
                "rsa-generated"
        );

        assertThat(createdComponent.getName(), is("rsa-generated"));
        assertThat(createdComponent.getProviderId(), is("rsa-generated"));
        MultivaluedHashMap<String, String> componentConfig = createdComponent.getConfig();

        System.out.println(componentConfig.keySet());

        List<String> keySize = componentConfig.get("keySize");
        assertThat(keySize, hasSize(1));
        assertThat(keySize.get(0), is("4096"));
    }

    private void shouldUpdateComponentsConfig() throws Exception {
        doImport("1_update_realm__change_component_config.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        ComponentRepresentation createdComponent = getComponent(
                "org.keycloak.keys.KeyProvider",
                "rsa-generated"
        );

        assertThat(createdComponent.getName(), is("rsa-generated"));
        assertThat(createdComponent.getProviderId(), is("rsa-generated"));
        MultivaluedHashMap<String, String> componentConfig = createdComponent.getConfig();

        System.out.println(componentConfig.keySet());

        List<String> keySize = componentConfig.get("keySize");
        assertThat(keySize, hasSize(1));
        assertThat(keySize.get(0), is("2048"));
    }

    private void shouldUpdateAddComponentsConfig() throws Exception {
        doImport("2_update_realm__add_component_with_config.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        ComponentRepresentation createdComponent = getComponent(
                "org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy",
                "Allowed Protocol Mapper Types",
                "authenticated"
        );

        assertThat(createdComponent.getName(), is("Allowed Protocol Mapper Types"));
        assertThat(createdComponent.getProviderId(), is("allowed-protocol-mappers"));
        assertThat(createdComponent.getSubType(), is("authenticated"));
        MultivaluedHashMap<String, String> componentConfig = createdComponent.getConfig();

        System.out.println(componentConfig.keySet());

        List<String> mapperTypes = componentConfig.get("allowed-protocol-mapper-types");
        assertThat(mapperTypes, hasSize(8));
        assertThat(mapperTypes, containsInAnyOrder(
                "oidc-full-name-mapper",
                "oidc-sha256-pairwise-sub-mapper",
                "oidc-address-mapper",
                "saml-user-property-mapper",
                "oidc-usermodel-property-mapper",
                "saml-role-list-mapper",
                "saml-user-attribute-mapper",
                "oidc-usermodel-attribute-mapper"
        ));
    }

    private void shouldAddComponentForSameProviderType() throws Exception {
        doImport("3_update_realm__add_component_for_same_providerType.json");

        RealmRepresentation createdRealm = keycloakProvider.get().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));

        ComponentRepresentation createdComponent = getComponent(
                "org.keycloak.keys.KeyProvider",
                "hmac-generated"
        );

        assertThat(createdComponent.getName(), is("hmac-generated"));
        assertThat(createdComponent.getProviderId(), is("hmac-generated"));
        assertThat(createdComponent.getProviderType(), is("org.keycloak.keys.KeyProvider"));
        MultivaluedHashMap<String, String> componentConfig = createdComponent.getConfig();

        System.out.println(componentConfig.keySet());

        List<String> secretSizeSize = componentConfig.get("secretSize");
        assertThat(secretSizeSize, hasSize(1));
        assertThat(secretSizeSize.get(0), is("32"));
    }

    private ComponentRepresentation getComponent(String providerType, String name, String subType) {
        return tryToGetComponent(providerType, name, subType).get();
    }

    private ComponentRepresentation getComponent(String providerType, String name) {
        return tryToGetComponent(providerType, name).get();
    }

    private Optional<ComponentRepresentation> tryToGetComponent(String providerType, String name, String subType) {
        RealmResource realmResource = keycloakProvider.get()
                .realm(REALM_NAME);

        Optional<ComponentRepresentation> maybeComponent;

        List<ComponentRepresentation> existingComponents = realmResource.components()
                .query().stream()
                .filter(c -> c.getProviderType().equals(providerType))
                .filter(c -> c.getName().equals(name))
                .filter(c -> c.getSubType().equals(subType))
                .collect(Collectors.toList());

        assertThat(existingComponents, hasSize(1));

        if(existingComponents.isEmpty()) {
            maybeComponent = Optional.empty();
        } else {
            maybeComponent = Optional.of(existingComponents.get(0));
        }

        return maybeComponent;
    }

    private Optional<ComponentRepresentation> tryToGetComponent(String providerType, String name) {
        RealmResource realmResource = keycloakProvider.get()
                .realm(REALM_NAME);

        Optional<ComponentRepresentation> maybeComponent;

        List<ComponentRepresentation> existingComponents = realmResource.components()
                .query().stream()
                .filter(c -> c.getProviderType().equals(providerType))
                .filter(c -> c.getName().equals(name))
                .filter(c -> c.getSubType() == null)
                .collect(Collectors.toList());

        assertThat(existingComponents, hasSize(1));

        if(existingComponents.isEmpty()) {
            maybeComponent = Optional.empty();
        } else {
            maybeComponent = Optional.of(existingComponents.get(0));
        }

        return maybeComponent;
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
