/*
 * Copyright 2019-2020 adorsys GmbH & Co. KG @ https://adorsys.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package de.adorsys.keycloak.config.model;

import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class RolesImport extends RolesRepresentation {

    @Override
    public List<RoleRepresentation> getRealm() {
        if (realm == null) {
            return Collections.emptyList();
        }

        return realm;
    }

    @Override
    public Map<String, List<RoleRepresentation>> getClient() {
        if (client == null) {
            return Collections.emptyMap();
        }

        return client;
    }
}
