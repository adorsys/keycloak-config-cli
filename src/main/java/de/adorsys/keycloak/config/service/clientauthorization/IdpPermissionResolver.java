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

package de.adorsys.keycloak.config.service.clientauthorization;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.repository.IdentityProviderRepository;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class IdpPermissionResolver implements PermissionResolver {
    private static final Logger logger = LoggerFactory.getLogger(IdpPermissionResolver.class);

    private static final int PAGE_SIZE = 100;

    private final String realmName;
    private final IdentityProviderRepository identityProviderRepository;
    private List<IdentityProviderRepresentation> identityProviders;
    private boolean allIdpPagesLoaded;

    public IdpPermissionResolver(String realmName, IdentityProviderRepository identityProviderRepository) {
        this.realmName = realmName;
        this.identityProviderRepository = identityProviderRepository;
        this.allIdpPagesLoaded = false;
    }

    @Override
    public String resolveObjectId(String alias, String authzName) {
        IdentityProviderRepresentation idpRep = getIdentityProviders()
                .filter(idp -> Objects.equals(idp.getAlias(), alias))
                .findAny()
                .orElse(identityProviderRepository.getByAlias(realmName, alias));

        if (idpRep != null) {
            return idpRep.getInternalId();
        } else {
            throw new ImportProcessingException(
                        "Cannot find identity provider with alias '%s' in realm '%s' for '%s'", alias, realmName, authzName);
        }
    }

    @Override
    public void enablePermissions(String id) {
        Optional<IdentityProviderRepresentation> idpOptional = getIdentityProviders()
                .filter(idp -> Objects.equals(idp.getInternalId(), id))
                .findAny();

        if (idpOptional.isEmpty()) {
            loadAllIdentityProviders();
        }

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

    private void loadAllIdentityProviders() {
        if (allIdpPagesLoaded) {
            return;
        }

        logger.debug("Loading all IDPs with pagination in realm '{}'", realmName);
        int offset = 0;
        identityProviders = new ArrayList<>();
        List<IdentityProviderRepresentation> page;

        do {
            page = identityProviderRepository.getPage(realmName, offset, PAGE_SIZE);
            logger.trace("Successfully read page of size {}", page.size());
            identityProviders.addAll(page);
            offset += PAGE_SIZE;
        } while (page.size() >= PAGE_SIZE || page.isEmpty());

        allIdpPagesLoaded = true;
        logger.trace("All IDP pages read");
    }
}
