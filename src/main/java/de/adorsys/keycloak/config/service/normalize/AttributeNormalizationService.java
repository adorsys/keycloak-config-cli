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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static de.adorsys.keycloak.config.service.normalize.RealmNormalizationService.*;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "NORMALIZE")
public class AttributeNormalizationService {

    private final Javers unOrderedJavers;

    public AttributeNormalizationService(Javers unOrderedJavers) {
        this.unOrderedJavers = unOrderedJavers;
    }

    public Map<String, String> normalizeStringAttributes(Map<String, String> exportedAttributes, Map<String, String> baselineAttributes) {
        var exportedOrEmpty = getNonNull(exportedAttributes);
        var baselineOrEmpty = getNonNull(baselineAttributes);
        var normalizedAttributes = new HashMap<String, String>();
        for (var entry : baselineOrEmpty.entrySet()) {
            var attributeName = entry.getKey();
            var baselineAttribute = entry.getValue();
            var exportedAttribute = exportedOrEmpty.remove(attributeName);

            if (!Objects.equals(baselineAttribute, exportedAttribute)) {
                normalizedAttributes.put(attributeName, exportedAttribute);
            }
        }
        normalizedAttributes.putAll(exportedOrEmpty);
        return normalizedAttributes.isEmpty() ? null : normalizedAttributes;
    }

    public Map<String, List<String>> normalizeListAttributes(Map<String, List<String>> exportedAttributes,
                                                             Map<String, List<String>> baselineAttributes) {
        var exportedOrEmpty = getNonNull(exportedAttributes);
        var baselineOrEmpty = getNonNull(baselineAttributes);
        var normalizedAttributes = new HashMap<String, List<String>>();
        for (var entry : baselineOrEmpty.entrySet()) {
            var attributeName = entry.getKey();
            var baselineAttribute = entry.getValue();
            var exportedAttribute = exportedOrEmpty.remove(attributeName);

            if (unOrderedJavers.compareCollections(baselineAttribute, exportedAttribute, String.class).hasChanges()) {
                normalizedAttributes.put(attributeName, exportedAttribute);
            }
        }
        normalizedAttributes.putAll(exportedOrEmpty);
        return normalizedAttributes.isEmpty() ? null : normalizedAttributes;
    }

    public boolean listAttributesChanged(Map<String, List<String>> exportedAttributes, Map<String, List<String>> baselineAttributes) {
        var exportedOrEmpty = getNonNull(exportedAttributes);
        var baselineOrEmpty = getNonNull(baselineAttributes);

        if (!Objects.equals(exportedOrEmpty.keySet(), baselineOrEmpty.keySet())) {
            return true;
        }

        for (var entry : baselineOrEmpty.entrySet()) {
            if (unOrderedJavers.compareCollections(entry.getValue(),
                    exportedOrEmpty.get(entry.getKey()), String.class).hasChanges()) {
                return true;
            }
        }
        return false;
    }

}
