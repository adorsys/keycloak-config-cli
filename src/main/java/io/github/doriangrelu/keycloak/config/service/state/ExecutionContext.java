/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2026 adorsys GmbH & Co. KG @ https://adorsys.com
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

package io.github.doriangrelu.keycloak.config.service.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Thread-safe container that holds typed collections of objects keyed by realm name and object type.
 *
 * <p>This context is used during the import execution to share state across import services.
 * For example, it allows the {@link io.github.doriangrelu.keycloak.config.service.ClientImportService}
 * to store the list of imported clients for a given realm, so that a later cleanup phase
 * can determine which clients should be removed.</p>
 *
 * <p>Internally, elements are stored in a {@link ConcurrentHashMap} with composite keys
 * built from the realm name and the element type, ensuring thread safety for parallel imports.</p>
 *
 * @author Dorian GRELU
 * @since 02.2026
 */
public class ExecutionContext {

    private static final String KEY_SEPARATOR = ":";

    private final Map<String, Collection<Object>> elements = new ConcurrentHashMap<>();

    /**
     * Stores a collection of elements associated with the given realm name and type.
     *
     * <p>If elements already exist for the same realm and type, the new elements are
     * appended to the existing collection.</p>
     *
     * @param key         the name of the key to associate the elements with
     * @param type        the class type used as part of the composite key
     * @param newElements the collection of elements to store
     * @param <T>         the type of elements
     */
    public <T> void put(final String key, final Class<T> type, final Collection<T> newElements) {
        final String computedKey = computeKey(key, type);
        this.elements.compute(computedKey, (unused, objects) -> computeNewElements(newElements).apply(objects));
    }

    /**
     * Retrieves the collection of elements associated with the given realm name and type.
     *
     * <p>Only elements that are instances of the specified type are returned.
     * If no elements exist for the given key, an empty collection is returned.</p>
     *
     * @param key  the name of the key
     * @param type the class type to filter and cast elements
     * @param <T>  the expected element type
     * @return an unmodifiable list of elements matching the given realm and type, or an empty list if none exist
     */
    public <T> Collection<T> get(final String key, final Class<T> type) {
        final String computedKey = computeKey(key, type);
        return this.elements.getOrDefault(computedKey, List.of())
                .stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }

    private static <T> Function<Collection<Object>, Collection<Object>> computeNewElements(final Collection<T> newElements) {
        return actual -> {
            final Collection<Object> updated = null == actual ? new ArrayList<>() : actual;
            updated.addAll(newElements);
            return updated;
        };
    }

    private static <T> String computeKey(final String realmName, final Class<T> type) {
        return realmName + KEY_SEPARATOR + type;
    }

}
