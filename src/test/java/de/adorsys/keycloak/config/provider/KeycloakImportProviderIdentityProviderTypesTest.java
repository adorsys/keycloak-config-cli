/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2021 adorsys GmbH & Co. KG @ https://adorsys.com
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package de.adorsys.keycloak.config.provider;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportFilesProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportVarSubstitutionProperties;
import de.adorsys.keycloak.config.util.VersionUtil;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Regression test for https://github.com/adorsys/keycloak-config-cli/issues/1665.
// The import ObjectMapper rejects unknown properties.
// If the bundled IdentityProviderRepresentation has no "types" field, this test fails.
// Do not call getTypes() here. Test sources also compile against client versions without that field.
class KeycloakImportProviderIdentityProviderTypesTest {
    private static final String LOCATION =
            "classpath:import-files/identity-provider-types/00_create_realm_with_identity-provider_with_types.json";

    @Test
    void shouldParseIdentityProviderWithTypes() throws IOException {
        // Keycloak 26.5 and later exports "types" for identity providers.
        assumeTrue(VersionUtil.ge(buildKeycloakVersion(), "26.5"));

        ImportConfigProperties importConfigProperties = mock(ImportConfigProperties.class);
        when(importConfigProperties.getFiles())
                .thenReturn(new ImportFilesProperties(List.of(LOCATION), Collections.emptyList(), false, 104857600));
        when(importConfigProperties.getVarSubstitution())
                .thenReturn(new ImportVarSubstitutionProperties(false, false, true, true, "$(", ")"));

        KeycloakImportProvider importProvider = new KeycloakImportProvider(
                mock(Environment.class),
                new PathMatchingResourcePatternResolver(),
                importConfigProperties
        );

        Map<String, List<RealmImport>> realmImports = importProvider.readFromLocations(LOCATION).getRealmImports().get(LOCATION);
        RealmImport realmImport = realmImports.values().iterator().next().get(0);

        assertThat(realmImport.getIdentityProviders())
                .extracting(IdentityProviderRepresentation::getAlias)
                .containsExactly("oidc-with-types");
    }

    private static String buildKeycloakVersion() throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = KeycloakImportProviderIdentityProviderTypesTest.class
                .getClassLoader().getResourceAsStream("application.properties")) {
            properties.load(inputStream);
        }
        return properties.getProperty("keycloak.version");
    }
}
