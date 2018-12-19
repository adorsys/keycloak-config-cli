package de.adorsys.keycloak.config.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.springframework.validation.annotation.Validated;

import java.util.*;

/**
 * Represents a component which is meant to be patched
 * Avoids merging of config properties because we consider the import to be the full truth to be set
 */
public class PatchedComponent extends ComponentRepresentation {

    /**
     * Use a custom class instead of a MultivaluedHashMap to avoid merging of lists while patching components
     */
    private Config config = new Config();

    @JsonGetter("config")
    @Override
    public MultivaluedHashMap<String, String> getConfig() {
        MultivaluedHashMap configAsMap = new MultivaluedHashMap();

        for (Map.Entry<String, Config.Value> entry : config.entrySet()) {
            for (String value : entry.getValue()) {
                configAsMap.add(entry.getKey(), value);
            }
        }

        return configAsMap;
    }

    @JsonSetter("config")
    public void setConfig(Config config) {
        this.config = config;
    }

    /**
     * Component config representation to avoid merging of lists while patching components
     */
    public static class Config implements Map<String, Config.Value> {

        private final Map<String, Config.Value> map = new HashMap<>();

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return map.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return map.containsValue(value);
        }

        @Override
        public Config.Value get(Object key) {
            return map.get(key);
        }

        @Override
        public Config.Value put(String key, Config.Value value) {
            return map.put(key, value);
        }

        @Override
        public Config.Value remove(Object key) {
            return map.remove(key);
        }

        @Override
        public void putAll(Map<? extends String, ? extends Config.Value> m) {
            map.putAll(m);
            return;
        }

        @Override
        public void clear() {
            map.clear();
            return;
        }

        @Override
        public Set<String> keySet() {
            return map.keySet();
        }

        @Override
        public Collection<Config.Value> values() {
            return map.values();
        }

        @Override
        public Set<Entry<String, Config.Value>> entrySet() {
            return map.entrySet();
        }

        /**
         * Use a custom class to represent a List of Strings.
         * This class avoids merging of values
         */
        public static class Value implements List<String> {

            private final List<String> values;

            public Value() {
                values = new ArrayList<>();
            }

            public Value(List<String> values) {
                this.values = values;
            }

            @Override
            public int size() {
                return values.size();
            }

            @Override
            public boolean isEmpty() {
                return values.isEmpty();
            }

            @Override
            public boolean contains(Object o) {
                return values.contains(o);
            }

            @Override
            public Iterator<String> iterator() {
                return values.iterator();
            }

            @Override
            public Object[] toArray() {
                return values.toArray();
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return values.toArray(a);
            }

            @Override
            public boolean add(String s) {
                if(!values.isEmpty()) {
                    values.clear();
                }

                return values.add(s);
            }

            @Override
            public boolean remove(Object o) {
                return values.remove(o);
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return values.containsAll(c);
            }

            @Override
            public boolean addAll(Collection<? extends String> c) {
                return values.addAll(c);
            }

            @Override
            public boolean addAll(int index, Collection<? extends String> c) {
                return values.addAll(index, c);
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return values.removeAll(c);
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return values.retainAll(c);
            }

            @Override
            public void clear() {
                values.clear();
                return;
            }

            @Override
            public String get(int index) {
                return values.get(index);
            }

            @Override
            public String set(int index, String element) {
                return values.set(index, element);
            }

            @Override
            public void add(int index, String element) {
                values.add(index, element);
                return;
            }

            @Override
            public String remove(int index) {
                return values.remove(index);
            }

            @Override
            public int indexOf(Object o) {
                return values.indexOf(o);
            }

            @Override
            public int lastIndexOf(Object o) {
                return values.lastIndexOf(o);
            }

            @Override
            public ListIterator<String> listIterator() {
                return values.listIterator();
            }

            @Override
            public ListIterator<String> listIterator(int index) {
                return values.listIterator(index);
            }

            @Override
            public List<String> subList(int fromIndex, int toIndex) {
                return values.subList(fromIndex, toIndex);
            }
        }
    }
}
