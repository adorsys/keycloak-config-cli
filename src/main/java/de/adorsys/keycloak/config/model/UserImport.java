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

import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UserImport extends UserRepresentation {

    @Override
    public Map<String, List<String>> getClientRoles() {
        if (clientRoles == null) {
            return Collections.emptyMap();
        }

        return clientRoles;
    }

    @Override
    public List<String> getRealmRoles() {
        if (realmRoles == null) {
            return Collections.emptyList();
        }

        return realmRoles;
    }
}
