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

package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.util.ResponseUtil;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Dependent
public class UserRepository {

    @Inject
    RealmRepository realmRepository;

    public Optional<UserRepresentation> tryToFindUser(String realm, String username) {
        Optional<UserRepresentation> maybeUser;

        try {
            UserRepresentation user = findUser(realm, username);

            maybeUser = Optional.of(user);
        } catch (KeycloakRepositoryException e) {
            maybeUser = Optional.empty();
        }

        return maybeUser;
    }

    final UserResource getUserResource(String realm, String username) {
        UserRepresentation foundUser = findUser(realm, username);
        return realmRepository.loadRealm(realm).users().get(foundUser.getId());
    }

    public UserRepresentation findUser(String realm, String username) {
        List<UserRepresentation> foundUsers = realmRepository.loadRealm(realm).users().search(username);
        //TODO: Remove when https://github.com/keycloak/keycloak/pull/6926 is merged
        List<UserRepresentation> filteredUsers = foundUsers.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username)).collect(Collectors.toList());

        if (filteredUsers.isEmpty()) {
            throw new KeycloakRepositoryException("Cannot find user '" + username + "' in realm '" + realm + "'");
        }

        return filteredUsers.get(0);
    }

    public void create(String realm, UserRepresentation userToCreate) {
        RealmResource realmResource = realmRepository.loadRealm(realm);
        UsersResource usersResource = realmResource.users();

        Response response = usersResource.create(userToCreate);

        ResponseUtil.throwOnError(response);
    }

    public void updateUser(String realm, UserRepresentation user) {
        UserResource userResource = getUserResource(realm, user.getUsername());
        userResource.update(user);
    }
}
