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

package de.adorsys.keycloak.config.repository;

import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OtpPolicyRepository {
    private final RealmRepository realmRepository;

    @Autowired
    public OtpPolicyRepository(RealmRepository realmRepository) {
        this.realmRepository = realmRepository;
    }

    public void updateOtpPolicy(String realmName, RealmRepresentation newRealmRepresentation) {
        RealmRepresentation existingRealm = realmRepository.get(realmName);

        existingRealm.setOtpPolicyAlgorithm(newRealmRepresentation.getOtpPolicyAlgorithm());


        realmRepository.update(existingRealm);
    }
}
