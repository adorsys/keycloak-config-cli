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

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.util.CloneUtil;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class LegacyOrganizationImporter implements OrganizationImporter {
    private static final Logger logger = LoggerFactory.getLogger(LegacyOrganizationImporter.class);

    @Override
    public void doImport(RealmImport realmImport) {
        List<OrganizationRepresentation> organizations = null;
        if (realmImport.getOrganizationsRaw() != null) {
            organizations = realmImport.getOrganizationsRaw().stream()
                    .map(r -> CloneUtil.deepClone(r, OrganizationRepresentation.class))
                    .collect(Collectors.toList());
        }

        if (organizations != null && !organizations.isEmpty()) {
            logger.warn(
                    "Organizations are not supported in Keycloak versions older than 26.x. "
                            + "Skipping import of {} organizations for realm '{}'.",
                    organizations.size(),
                    realmImport.getRealm()
            );
        }
    }
}
