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

package de.adorsys.keycloak.config.util;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implements a multi-value map to store multiple values for each key
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class MultiValueMap<K, V> {

    /* *****************************************************************************************************************
     * Readonly fields
     **************************************************************************************************************** */

    private final Map<K, List<V>> map = new HashMap<>();

    /* *****************************************************************************************************************
     * Constructors
     **************************************************************************************************************** */

    /**
     * Creates new empty {@link MultiValueMap} instance
     */
    public MultiValueMap() {
    }

    /**
     * Protected copy-constructor used in internal {@link Collector}
     *
     * @param otherMap the collected {@link MultiValueMap} to be copied into the new instance
     */
    private MultiValueMap(MultiValueMap<K, V> otherMap) {
        for (Map.Entry<K, List<V>> entry : otherMap.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
    }

    /* *****************************************************************************************************************
     * Public contract
     **************************************************************************************************************** */

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    public boolean containsValue(V value) {
        Set<Map.Entry<K, List<V>>> entries = map.entrySet();

        for (Map.Entry<K, List<V>> entry : entries) {
            List<V> entryValue = entry.getValue();

            if (entryValue != null && entryValue.contains(value)) {
                return true;
            }
        }

        return false;
    }

    public List<V> get(K key) {
        List<V> values = map.get(key);
        return cloneOf(values);
    }

    public List<V> put(K key, V value) {
        List<V> values = map.computeIfAbsent(key, k -> new ArrayList<>());

        values.add(value);

        return cloneOf(values);
    }

    public List<V> remove(K key) {
        return map.remove(key);
    }

    public void putAll(Map<? extends K, ? extends Collection<V>> map) {
        putEntries(map.entrySet());
    }

    public void putAll(K key, Collection<V> values) {
        for (V value : values) {
            List<V> storedValues = map.computeIfAbsent(key, k -> new ArrayList<>());
            storedValues.add(value);
        }
    }

    public void putAll(MultiValueMap<K, V> map) {
        putEntries(map.entrySet());
    }

    public void clear() {
        map.clear();
    }

    public Set<K> keySet() {
        return map.keySet();
    }

    public Collection<V> values() {
        return map.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public Set<Map.Entry<K, List<V>>> entrySet() {
        return map.entrySet();
    }

    public Map<K, List<V>> toMap() {
        return map.entrySet()
                .stream()
                .collect(
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
                );
    }

    public <R> MultiValueMap<K, R> convert(BiFunction<K, V, R> convertFunction) {
        MultiValueMap<K, R> convertedMap = new MultiValueMap<>();

        for (Map.Entry<K, List<V>> entry : map.entrySet()) {
            K key = entry.getKey();

            for (V value : entry.getValue()) {
                R convertedValue = convertFunction.apply(key, value);
                convertedMap.put(key, convertedValue);
            }
        }

        return convertedMap;
    }

    /* *****************************************************************************************************************
     * Overrides of Object
     **************************************************************************************************************** */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultiValueMap<?, ?> that = (MultiValueMap<?, ?>) o;
        return map.equals(that.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map);
    }

    @Override
    public String toString() {
        return map.toString();
    }

    /* *****************************************************************************************************************
     * Factory method(s)
     **************************************************************************************************************** */

    /**
     * Create {@link MultiValueMap} from two-dimensional {@link Map} containing all elements
     *
     * @param map the two-dimensional {@link Map}
     * @param <K> the key type
     * @param <V> the value type within {@link Collection}s
     * @return a new instance of a {@link MultiValueMap}
     */
    public static <K, V> MultiValueMap<K, V> fromTwoDimMap(Map<K, ? extends Collection<V>> map) {
        MultiValueMap<K, V> multiValueMap = new MultiValueMap<>();

        for (Map.Entry<K, ? extends Collection<V>> entry : map.entrySet()) {
            multiValueMap.putAll(entry.getKey(), entry.getValue());
        }

        return multiValueMap;
    }

    /**
     * Create {@link MultiValueMap} from one-dimensional {@link Map} containing all elements
     *
     * @param map the one-dimensional {@link Map}
     * @param <K> the key type
     * @param <V> the value type
     * @return a new instance of a {@link MultiValueMap}
     */
    public static <K, V> MultiValueMap<K, V> fromOneDimMap(Map<K, V> map) {
        MultiValueMap<K, V> multiValueMap = new MultiValueMap<>();

        for (Map.Entry<K, ? extends V> entry : map.entrySet()) {
            multiValueMap.put(entry.getKey(), entry.getValue());
        }

        return multiValueMap;
    }

    /**
     * Provides a {@link Collector} to collect {@link Stream}s to a {@link MultiValueMap}
     *
     * @param keyMapper   the mapper {@link Function} to get the key for each element
     * @param valueMapper the mapper {@link Function} to get the value for each element
     * @param <T>         the type of the {@link Stream} elements
     * @param <K>         the key type
     * @param <V>         the value type
     * @return a new {@link Collector} instance
     */
    public static <T, K, V> Collector<T, ?, MultiValueMap<K, V>> collector(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper
    ) {
        return new MultiValueMapCollector<>(keyMapper, valueMapper);
    }

    /* *****************************************************************************************************************
     * Private methods
     **************************************************************************************************************** */

    private void putEntries(Set<? extends Map.Entry<? extends K, ? extends Collection<V>>> entries) {
        for (Map.Entry<? extends K, ? extends Collection<V>> entry : entries) {
            Collection<V> entryValues = entry.getValue();

            for (V entryValue : entryValues) {
                putValue(entry.getKey(), entryValue);
            }
        }
    }

    private void putValue(K key, V value) {
        List<V> values = map.computeIfAbsent(key, k -> new ArrayList<>());

        values.add(value);
    }

    private List<V> cloneOf(List<V> values) {
        if (values == null) {
            return null;
        }

        ArrayList<V> clone = new ArrayList<>(values.size());
        clone.addAll(values);

        return clone;
    }

    /* *****************************************************************************************************************
     * Private methods
     **************************************************************************************************************** */

    private static class MultiValueMapCollector<T, K, V> implements Collector<T, MultiValueMap<K, V>, MultiValueMap<K, V>> {

        private final Function<? super T, ? extends K> keyMapper;
        private final Function<? super T, ? extends V> valueMapper;

        private MultiValueMapCollector(
                Function<? super T, ? extends K> keyMapper,
                Function<? super T, ? extends V> valueMapper
        ) {
            this.keyMapper = keyMapper;
            this.valueMapper = valueMapper;
        }

        @Override
        public Supplier<MultiValueMap<K, V>> supplier() {
            return MultiValueMap::new;
        }

        @Override
        public BiConsumer<MultiValueMap<K, V>, T> accumulator() {
            return (map, element) -> {
                K key = keyMapper.apply(element);
                V value = Objects.requireNonNull(valueMapper.apply(element));

                if (value instanceof Collection) {
                    map.putAll(key, (Collection) value);
                } else {
                    map.put(key, value);
                }
            };
        }

        @Override
        public BinaryOperator<MultiValueMap<K, V>> combiner() {
            return (map, otherMap) -> {
                for (Map.Entry<K, List<V>> entry : otherMap.entrySet()) {
                    K key = entry.getKey();
                    List<V> value = Objects.requireNonNull(entry.getValue());
                    map.putAll(key, value);
                }
                return map;
            };
        }

        @Override
        public Function<MultiValueMap<K, V>, MultiValueMap<K, V>> finisher() {
            return MultiValueMap::new;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Collections.emptySet();
        }
    }
}

