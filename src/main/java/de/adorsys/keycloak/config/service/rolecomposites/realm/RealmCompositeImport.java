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

package de.adorsys.keycloak.config.service.rolecomposites.realm;

import de.adorsys.keycloak.config.repository.RoleCompositeRepository;
import org.jboss.logging.Logger;
import org.keycloak.representations.idm.RoleRepresentation;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Dependent
public class RealmCompositeImport {
    private static final Logger LOG = Logger.getLogger(RealmCompositeImport.class);

    @Inject
    RoleCompositeRepository roleCompositeRepository;

    public void update(String realm, RoleRepresentation realmRole, Set<String> realmComposites) {
        String roleName = realmRole.getName();

        Set<String> existingRealmCompositeNames = findRealmRoleRealmCompositeNames(realm, roleName);

        if (Objects.equals(realmComposites, existingRealmCompositeNames)) {
            LOG.debugf("No need to update realm-level role '%s's composites realm-roles in realm '%s'", roleName, realm);
        } else {
            LOG.debugf("Update realm-level role '%s's composites realm-roles in realm '%s'", roleName, realm);

            updateRealmRoleRealmComposites(realm, roleName, existingRealmCompositeNames, realmComposites);
        }
    }

    private Set<String> findRealmRoleRealmCompositeNames(String realm, String roleName) {
        Set<RoleRepresentation> existingRealmComposites = roleCompositeRepository.findRealmRoleRealmComposites(realm, roleName);

        return existingRealmComposites.stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());
    }

    private void updateRealmRoleRealmComposites(String realm, String roleName, Set<String> existingRealmCompositeNames, Set<String> realmComposites) {
        removeRealmRoleRealmComposites(realm, roleName, existingRealmCompositeNames, realmComposites);
        addRealmRoleRealmComposites(realm, roleName, existingRealmCompositeNames, realmComposites);
    }

    private void removeRealmRoleRealmComposites(String realm, String roleName, Set<String> existingRealmCompositeNames, Set<String> realmComposites) {
        Set<String> realmCompositesToRemove = existingRealmCompositeNames.stream()
                .filter(name -> !realmComposites.contains(name))
                .collect(Collectors.toSet());

        roleCompositeRepository.removeRealmRoleRealmComposites(realm, roleName, realmCompositesToRemove);
    }

    private void addRealmRoleRealmComposites(String realm, String roleName, Set<String> existingRealmCompositeNames, Set<String> realmComposites) {
        Set<String> realmCompositesToAdd = realmComposites.stream()
                .filter(name -> !existingRealmCompositeNames.contains(name))
                .collect(Collectors.toSet());

        roleCompositeRepository.addRealmRoleRealmComposites(
                realm,
                roleName,
                realmCompositesToAdd
        );
    }
}
