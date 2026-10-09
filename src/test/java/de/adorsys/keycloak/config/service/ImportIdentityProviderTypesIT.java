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

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.AbstractImportIT;
import de.adorsys.keycloak.config.repository.IdentityProviderRepository;
import de.adorsys.keycloak.config.util.VersionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// Regression test for https://github.com/adorsys/keycloak-config-cli/issues/1665.
// Do not call getTypes() here. Test sources also compile against client versions without that field.
class ImportIdentityProviderTypesIT extends AbstractImportIT {
    private static final String REALM_NAME = "realmWithIdentityProviderTypes";
    private static final String ALIAS = "oidc-with-types";

    @Autowired
    private IdentityProviderRepository identityProviderRepository;

    ImportIdentityProviderTypesIT() {
        this.resourcePath = "import-files/identity-provider-types";
    }

    @BeforeEach
    void init() {
        // Keycloak 26.5 and later exports "types" for identity providers.
        assumeTrue(VersionUtil.ge(KEYCLOAK_VERSION, "26.5"));
    }

    @Test
    @Order(0)
    void shouldCreateIdentityProviderWithTypes() throws IOException {
        doImport("00_create_realm_with_identity-provider_with_types.json");

        IdentityProviderRepresentation identityProvider = identityProviderRepository.getByAlias(REALM_NAME, ALIAS);

        assertThat(identityProvider.getProviderId(), is("oidc"));
        assertThat(identityProvider.getDisplayName(), is("OIDC with types"));
        assertThat(identityProvider.getConfig().get("syncMode"), is("IMPORT"));
    }

    @Test
    @Order(1)
    void shouldUpdateIdentityProviderWithTypes() throws IOException {
        doImport("01_update_identity-provider_with_types.json");

        IdentityProviderRepresentation identityProvider = identityProviderRepository.getByAlias(REALM_NAME, ALIAS);

        assertThat(identityProvider.getProviderId(), is("oidc"));
        assertThat(identityProvider.getDisplayName(), is("OIDC with types updated"));
        assertThat(identityProvider.getConfig().get("syncMode"), is("FORCE"));
    }
}
