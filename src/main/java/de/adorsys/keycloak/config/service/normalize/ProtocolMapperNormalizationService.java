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

package de.adorsys.keycloak.config.service.normalize;

import org.javers.core.Javers;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "NORMALIZE")
public class ProtocolMapperNormalizationService {

    private final Javers unOrderedJavers;

    public ProtocolMapperNormalizationService(Javers unOrderedJavers) {
        this.unOrderedJavers = unOrderedJavers;
    }

    public List<ProtocolMapperRepresentation> normalizeProtocolMappers(List<ProtocolMapperRepresentation> exportedMappers,
                                                                       List<ProtocolMapperRepresentation> baselineMappers) {
        List<ProtocolMapperRepresentation> exportedOrEmpty = exportedMappers == null ? List.of() : exportedMappers;
        List<ProtocolMapperRepresentation> baselineOrEmpty = baselineMappers == null ? List.of() : baselineMappers;

        List<ProtocolMapperRepresentation> normalizedMappers = null;
        if (unOrderedJavers.compareCollections(baselineOrEmpty, exportedOrEmpty, ProtocolMapperRepresentation.class).hasChanges()) {
            /*
             * If the mapper lists differ, add all the mappers from the exported list. Otherwise, just return null
             */
            normalizedMappers = new ArrayList<>();
            for (var mapper : exportedOrEmpty) {
                mapper.setId(null);
                normalizedMappers.add(mapper);
            }
        }
        return normalizedMappers;
    }
}
