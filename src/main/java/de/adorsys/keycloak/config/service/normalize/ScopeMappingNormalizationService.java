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
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.adorsys.keycloak.config.service.normalize.RealmNormalizationService.getNonNull;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "NORMALIZE")
public class ScopeMappingNormalizationService {

    private static final Logger logger = LoggerFactory.getLogger(ScopeMappingNormalizationService.class);

    private final Javers javers;

    public ScopeMappingNormalizationService(Javers javers) {
        this.javers = javers;
    }

    public List<ScopeMappingRepresentation> normalizeScopeMappings(RealmRepresentation exportedRealm, RealmRepresentation baselineRealm) {
        /*
         * TODO: are the mappings in scopeMappings always clientScope/role? If not, this breaks
         */
        // First handle the "default" scopeMappings present in the
        var exportedMappingsMap = new HashMap<String, ScopeMappingRepresentation>();
        for (var exportedMapping : exportedRealm.getScopeMappings()) {
            exportedMappingsMap.put(exportedMapping.getClientScope(), exportedMapping);
        }

        var baselineMappingsMap = new HashMap<String, ScopeMappingRepresentation>();

        var mappings = new ArrayList<ScopeMappingRepresentation>();
        for (var baselineRealmMapping : baselineRealm.getScopeMappings()) {
            var clientScope = baselineRealmMapping.getClientScope();
            baselineMappingsMap.put(clientScope, baselineRealmMapping);
            var exportedMapping = exportedMappingsMap.get(clientScope);
            if (exportedMapping == null) {
                logger.warn("Default realm scopeMapping '{}' was deleted in exported realm. It may be reintroduced during import!", clientScope);
                continue;
            }
            // If the exported scopeMapping is different from the one that is present in the baseline realm, export it in the yml
            if (scopeMappingChanged(exportedMapping, baselineRealmMapping)) {
                mappings.add(exportedMapping);
            }
        }

        for (Map.Entry<String, ScopeMappingRepresentation> e : exportedMappingsMap.entrySet()) {
            var clientScope = e.getKey();
            if (!baselineMappingsMap.containsKey(clientScope)) {
                mappings.add(e.getValue());
            }
        }
        return mappings;
    }

    public Map<String, List<ScopeMappingRepresentation>> normalizeClientScopeMappings(RealmRepresentation exportedRealm,
                                                                                      RealmRepresentation baselineRealm) {
        var baselineOrEmpty = getNonNull(baselineRealm.getClientScopeMappings());
        var exportedOrEmpty = getNonNull(exportedRealm.getClientScopeMappings());

        var mappings = new HashMap<String, List<ScopeMappingRepresentation>>();
        for (var e : baselineOrEmpty.entrySet()) {
            var key = e.getKey();
            if (!exportedOrEmpty.containsKey(key)) {
                logger.warn("Default realm clientScopeMapping '{}' was deleted in exported realm. It may be reintroduced during import!", key);
                continue;
            }
            var scopeMappings = exportedOrEmpty.get(key);
            if (javers.compareCollections(e.getValue(), scopeMappings, ScopeMappingRepresentation.class).hasChanges()) {
                mappings.put(key, scopeMappings);
            }
        }

        for (var e : exportedOrEmpty.entrySet()) {
            var key = e.getKey();
            if (!baselineOrEmpty.containsKey(key)) {
                mappings.put(key, e.getValue());
            }
        }
        return mappings;
    }

    public boolean scopeMappingChanged(ScopeMappingRepresentation exportedMapping, ScopeMappingRepresentation baselineRealmMapping) {
        return javers.compare(baselineRealmMapping, exportedMapping).hasChanges();
    }

}
