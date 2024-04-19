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

package de.adorsys.keycloak.config.test.util;

import org.apache.commons.collections.CollectionUtils;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.GroupRepresentation;

import java.util.List;

public class SubGroupUtil {

    private SubGroupUtil() {
    }

    public static List<GroupRepresentation> getSubGroups(
            GroupRepresentation groupRepresentation,
            RealmResource realmResource
    ) {
        if (CollectionUtils.isNotEmpty(groupRepresentation.getSubGroups())) {
            return groupRepresentation.getSubGroups();
        }

        return realmResource.groups()
                .group(groupRepresentation.getId())
                .getSubGroups(null, null, false);
    }
}
