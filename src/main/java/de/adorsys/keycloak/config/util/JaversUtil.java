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

package de.adorsys.keycloak.config.util;

import de.adorsys.keycloak.config.exception.NormalizationException;
import org.javers.core.diff.changetype.PropertyChange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "NORMALIZE")
public class JaversUtil {

    public void applyChange(Object object, PropertyChange<?> change) {
        try {
            var field = object.getClass().getDeclaredField(change.getPropertyName());
            field.setAccessible(true);
            field.set(object, change.getRight());
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new NormalizationException(String.format("Failed to set property %s on object of type %s",
                    change.getPropertyName(), object.getClass().getName()), ex);
        }
    }
}
