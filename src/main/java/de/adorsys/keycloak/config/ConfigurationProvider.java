package de.adorsys.keycloak.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.adorsys.keycloak.config.authenticator.AuthenticationConfig;
import de.adorsys.keycloak.config.realm.ComponentDefinition;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
public class ConfigurationProvider {

	private final ObjectMapper ymlObjectMapper;

	private static final String REALM_CONFIG_YML = "realm.yml";
	private static final String AUTHENTICATION_CONFIG_YML = "authentication.yml";
	private static final String COMPONENTS_CONFIG_YML = "components.yml";
	private static final String CLIENT_CONFIG_YML = "clients.yml";
	private static final String USER_CONFIG_YML = "users.yml";
	private static final Logger LOG = LoggerFactory.getLogger(ConfigurationProvider.class);

	@Value("${keycloakConfigDir}")
	String keycloakConfigDir;

	@Autowired
	public ConfigurationProvider(@Qualifier("yaml") ObjectMapper ymlOm) throws IOException {
		this.ymlObjectMapper = ymlOm;
	}

	public List<RealmRepresentation> getRealmConfigurations() {
		try {
			Stream<Path> realmConfFiles = Files.find(Paths.get(keycloakConfigDir), Integer.MAX_VALUE,
					((path, basicFileAttributes) -> basicFileAttributes.isRegularFile()
							&& path.getFileName().toString().equals(REALM_CONFIG_YML)));

			return realmConfFiles.map(realmConfFile -> {
				try {
					final File realConfigFile = realmConfFile.toFile();
					LOG.debug("Loading realm config from '{}'.", realmConfFile.toAbsolutePath());

					RealmRepresentation realmRepresentation = ymlObjectMapper.readValue(realConfigFile,
							RealmRepresentation.class);
					LOG.debug("RealmRepresentation created for '{}'.", realmRepresentation.getId());
					return realmRepresentation;

				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}).collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public AuthenticationConfig getAuthenticationConfig(String realmId) throws IOException {
		AuthenticationConfig authenticationConfig = null;
		final Path configPath = Paths.get(keycloakConfigDir, realmId, AUTHENTICATION_CONFIG_YML);
		final boolean authenticationConfigExists = configPath.toFile().exists();
		if (authenticationConfigExists) {
			LOG.debug("Loading realm authentication from '{}'.", configPath.toAbsolutePath());
			authenticationConfig = ymlObjectMapper.readValue(configPath.toFile(), AuthenticationConfig.class);
		}
		return authenticationConfig;
	}

	public List<ComponentDefinition> getComponents(String realmId) throws IOException {
		Path componentsConfigPath = Paths.get(keycloakConfigDir, realmId, COMPONENTS_CONFIG_YML);
		LOG.debug("Loading realm components from '{}'.", componentsConfigPath.toAbsolutePath());

		return ymlObjectMapper.readValue(componentsConfigPath.toFile(),new TypeReference<List<ComponentDefinition>>() {});
	}

	public List<ClientRepresentation> getClients(String realmId) throws IOException {
		Path clientConfFilePath = Paths.get(keycloakConfigDir, realmId, CLIENT_CONFIG_YML);
		LOG.debug("Loading realm clients from '{}'.", clientConfFilePath.toAbsolutePath());

		return ymlObjectMapper.readValue(clientConfFilePath.toFile(),new TypeReference<List<ClientRepresentation>>() {});
	}

	public List<UserRepresentation> getUsers(String realmId) throws IOException {
		Path clientConfFilePath = Paths.get(keycloakConfigDir, realmId, USER_CONFIG_YML);
		LOG.debug("Loading realm users from '{}'.", clientConfFilePath.toAbsolutePath());

		return ymlObjectMapper.readValue(clientConfFilePath.toFile(),new TypeReference<List<UserRepresentation>>() {});
	}
}
