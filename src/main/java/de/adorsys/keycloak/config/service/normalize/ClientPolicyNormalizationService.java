/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2023 adorsys GmbH & Co. KG @ https://adorsys.com
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

import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "NORMALIZE")
public class ClientPolicyNormalizationService {

    public ClientPoliciesRepresentation normalizePolicies(ClientPoliciesRepresentation exportedPolicies,
                                                          ClientPoliciesRepresentation baselinePolicies) {
        var policies = exportedPolicies.getPolicies();
        if (policies == null || policies.isEmpty()) {
            return null;
        }
        return exportedPolicies;
    }

    public ClientProfilesRepresentation normalizeProfiles(ClientProfilesRepresentation exportedProfiles,
                                                          ClientProfilesRepresentation baselineProfiles) {
        var profiles = exportedProfiles.getProfiles();
        if (profiles == null || profiles.isEmpty()) {
            return null;
        }
        return exportedProfiles;
    }
}
