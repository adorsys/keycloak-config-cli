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

package de.adorsys.keycloak.config.service.normalize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;



import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientPolicyNormalizationServiceIT {

    private ClientPolicyNormalizationService service;

    @BeforeEach
    public void setUp() {
        service = new ClientPolicyNormalizationService();
    }

    @Test
    public void testNormalizePoliciesWithNullPolicies() {
        ClientPoliciesRepresentation exportedPolicies = new ClientPoliciesRepresentation();
        exportedPolicies.setPolicies(null);

        ClientPoliciesRepresentation result = service.normalizePolicies(exportedPolicies, null);

        assertThat(result).isNull();
    }

    @Test
    public void testNormalizePoliciesWithEmptyPolicies() {
        ClientPoliciesRepresentation exportedPolicies = new ClientPoliciesRepresentation();
        exportedPolicies.setPolicies(Collections.emptyList());

        ClientPoliciesRepresentation result = service.normalizePolicies(exportedPolicies, null);

        assertThat(result).isNull();
    }

    @Test
    public void testNormalizePoliciesWithNonEmptyPolicies() {
        ClientPoliciesRepresentation exportedPolicies = new ClientPoliciesRepresentation();
        exportedPolicies.setPolicies(Collections.singletonList(new ClientPolicyRepresentation()));

        ClientPoliciesRepresentation result = service.normalizePolicies(exportedPolicies, null);

        assertThat(result).isEqualTo(exportedPolicies);
    }

    @Test
    public void testNormalizeProfilesWithNullProfiles() {
        ClientProfilesRepresentation exportedProfiles = new ClientProfilesRepresentation();
        exportedProfiles.setProfiles(null);

        ClientProfilesRepresentation result = service.normalizeProfiles(exportedProfiles, null);

        assertThat(result).isNull();
    }

    @Test
    public void testNormalizeProfilesWithEmptyProfiles() {
        ClientProfilesRepresentation exportedProfiles = new ClientProfilesRepresentation();
        exportedProfiles.setProfiles(Collections.emptyList());

        ClientProfilesRepresentation result = service.normalizeProfiles(exportedProfiles, null);

        assertThat(result).isNull();
    }

    @Test
    public void testNormalizeProfilesWithNonEmptyProfiles() {
        ClientProfilesRepresentation exportedProfiles = new ClientProfilesRepresentation();
        exportedProfiles.setProfiles(Collections.singletonList(new ClientProfileRepresentation()));
        ClientProfilesRepresentation result = service.normalizeProfiles(exportedProfiles, null);

        assertThat(result).isEqualTo(exportedProfiles);
    }
}
