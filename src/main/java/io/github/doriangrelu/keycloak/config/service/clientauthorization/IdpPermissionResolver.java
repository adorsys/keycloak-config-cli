/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2022 adorsys GmbH & Co. KG @ https://adorsys.com
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

package io.github.doriangrelu.keycloak.config.service.clientauthorization;

import io.github.doriangrelu.keycloak.config.exception.ImportProcessingException;
import io.github.doriangrelu.keycloak.config.repository.IdentityProviderRepository;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class IdpPermissionResolver implements PermissionResolver {
    private static final Logger logger = LoggerFactory.getLogger(IdpPermissionResolver.class);

    private final String realmName;
    private final IdentityProviderRepository identityProviderRepository;
    private List<IdentityProviderRepresentation> identityProviders;

    public IdpPermissionResolver(String realmName, IdentityProviderRepository identityProviderRepository) {
        this.realmName = realmName;
        this.identityProviderRepository = identityProviderRepository;
    }

    @Override
    public String resolveObjectId(String alias, String authzName) {
        return getIdentityProviders()
                .filter(idp -> Objects.equals(idp.getAlias(), alias))
                .map(IdentityProviderRepresentation::getInternalId)
                .findAny()
                .orElseThrow(() -> new ImportProcessingException(
                        "Cannot find identity provider with alias '%s' in realm '%s' for '%s'", alias, realmName, authzName));
    }

    @Override
    public void enablePermissions(String id) {
        String alias = getIdentityProviders()
                .filter(idp -> Objects.equals(idp.getInternalId(), id))
                .map(IdentityProviderRepresentation::getAlias)
                .findAny()
                .orElseThrow(() -> new ImportProcessingException(
                        "Cannot find identity provider with internal id '%s' in realm '%s'", id, realmName));

        if (!identityProviderRepository.isPermissionEnabled(realmName, alias)) {
            logger.debug("Enable permissions for Identity Provider '{}' in realm '{}'", id, realmName);
            identityProviderRepository.enablePermission(realmName, alias);
        }
    }

    private Stream<IdentityProviderRepresentation> getIdentityProviders() {
        if (identityProviders == null) {
            identityProviders = identityProviderRepository.getAll(realmName);
        }
        return identityProviders.stream();
    }
}
