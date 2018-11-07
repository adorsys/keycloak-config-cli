package de.adorsys.keycloak.config;

import java.io.IOException;
import java.util.List;

import de.adorsys.keycloak.config.authenticator.AuthenticationConfig;
import de.adorsys.keycloak.config.client.ClientConfigService;
import de.adorsys.keycloak.config.realm.ComponentDefinition;
import de.adorsys.keycloak.config.user.UserConfigService;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import de.adorsys.keycloak.config.authenticator.AuthenticationConfigSerivce;
import de.adorsys.keycloak.config.realm.RealmConfigService;

@SpringBootApplication
public class ConfigurationApp implements CommandLineRunner {

    private static Logger LOGGER = LoggerFactory.getLogger(ConfigurationApp.class);

    @Autowired
    private AuthenticationConfigSerivce authenticationConfigSerivce;
    
    @Autowired
    private RealmConfigService realmConfigService;

    @Autowired
    private ConfigurationProvider configProvider;

    @Autowired
    private ClientConfigService clientConfigService;

    @Autowired
    private UserConfigService userConfigService;

    @Value("${keycloakCredentialDownloadDir:file://${user.dir}/target}")
    private Resource keycloakCredentialDownloadDir;

    public static void main(String[] args) throws Exception {
        SpringApplication.run(ConfigurationApp.class, args);
    }

    @Bean
    @Qualifier("yaml")
    public ObjectMapper createYamlObjectMapper() {
        final YAMLFactory ymlFactory = new YAMLFactory();
        return new ObjectMapper(ymlFactory);
    }

    @Bean
    public Keycloak createKeycloak(
            @Value("${keycloakUrl}") String serverUrl,
            @Value("${keycloakUser}") String adminLogin,
            @Value("${keycloakPassword}") String adminPassword) {

        Assert.hasLength(serverUrl, "Keycloak server URL cannot be empty, check the start configuration!");
        Assert.hasLength(adminLogin, "Keycloak admin login cannot be empty, check the start configuration!");
        Assert.hasLength(adminPassword, "Keycloak admin password cannot be empty, check the start configuration!");
        LOGGER.debug("Attempt to connect Keycloak using '{}'.", serverUrl);
        return Keycloak.getInstance(serverUrl, "master", adminLogin, adminPassword, "admin-cli");
    }

    @Override
    public void run(final String... args) throws IOException {
        List<RealmRepresentation> realmConfigurations = configProvider.getRealmConfigurations();
        for (RealmRepresentation realm : realmConfigurations) {

            final String realmId = realm.getId();
            realmConfigService.createOrUpdateRealm(realm);

            final List<ComponentDefinition> components = configProvider.getComponents(realmId);
            realmConfigService.createOrUpdateRealmComponents(realmId, components);

            final AuthenticationConfig authenticationConfig = configProvider.getAuthenticationConfig(realmId);
            authenticationConfigSerivce.createAuthenticationConfig(realmId, authenticationConfig);

            final List<ClientRepresentation> clientConfigurations = configProvider.getClients(realmId);
            clientConfigService.handleClients(realmId, clientConfigurations);

            final List<UserRepresentation> userConfigurations = configProvider.getUsers(realmId);
            userConfigService.handleUsers(realmId, userConfigurations);
        }
    }
}