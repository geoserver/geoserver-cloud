/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.geoserver.catalog.Info;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.ows.util.ClassProperties;
import org.geoserver.ows.util.OwsUtils;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public @Data class PropertyDiff implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Change> changes;

    public static <T extends Info> PropertyDiffBuilder<T> builder() {
        return new PropertyDiffBuilder<>();
    }

    public static <T extends Info> PropertyDiffBuilder<T> builder(T oldValueHolder) {
        return new PropertyDiffBuilder<>(oldValueHolder);
    }

    public PropertyDiff() {
        this(Collections.emptyList());
    }

    public PropertyDiff(@NonNull List<Change> changes) {
        this.changes = new ArrayList<>(changes);
    }

    private Object readResolve() {
        if (changes == null) changes = new ArrayList<>();
        return this;
    }

    public Patch toPatch() {
        Patch patch = new Patch();
        changes.stream()
                .map(c -> new Patch.Property(c.getPropertyName(), c.getNewValue()))
                .forEach(patch::add);
        return patch;
    }

    public int size() {
        return changes.size();
    }

    public Change get(int index) {
        return changes.get(index);
    }

    public Optional<Change> get(@NonNull String propertyName) {
        return changes.stream().filter(c -> propertyName.equals(c.getPropertyName())).findFirst();
    }

    public List<String> getPropertyNames() {
        return getChanges().stream().map(Change::getPropertyName).toList();
    }

    public List<Object> getOldValues() {
        return getChanges().stream().map(Change::getOldValue).toList();
    }

    public List<Object> getNewValues() {
        return getChanges().stream().map(Change::getNewValue).toList();
    }

    /**
     * @return a "clean copy", where no-op changes are ignored
     */
    public PropertyDiff clean() {
        return new PropertyDiff(
                changes.stream().filter(c -> !c.isNoChange()).collect(Collectors.toList()));
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static @Data class Change implements Serializable {
        private static final long serialVersionUID = 1L;
        private @NonNull String propertyName;
        private Object oldValue;
        private Object newValue;

        public boolean isNoChange() {
            if (Objects.equals(oldValue, newValue)) return true;
            if (isCollectionProperty())
                return isNullCollectionOp((Collection<?>) newValue, (Collection<?>) oldValue);
            if (isMapProperty()) return isNullMapOp((Map<?, ?>) newValue, (Map<?, ?>) oldValue);
            return false;
        }

        private boolean isNullMapOp(Map<?, ?> m1, Map<?, ?> m2) {
            return m1 == null
                    ? (m2 == null || m2.isEmpty())
                    : (m2 == null ? m1 == null || m1.isEmpty() : false);
        }

        private boolean isNullCollectionOp(Collection<?> c1, Collection<?> c2) {
            if (c1 == null) return c2 == null ? true : c2.isEmpty();
            if (c2 == null) return c1 == null ? true : c1.isEmpty();
            return false;
        }

        public boolean isCollectionProperty() {
            return isCollection(newValue) || isCollection(oldValue);
        }

        public boolean isMapProperty() {
            return isMap(newValue) || isMap(oldValue);
        }

        private boolean isCollection(Object o) {
            return o instanceof Collection;
        }

        private boolean isMap(Object o) {
            return o instanceof Map;
        }

        public static Change valueOf(String propertyName, Object oldValue, Object newValue) {
            return new Change(propertyName, oldValue, newValue);
        }

        public @Override String toString() {
            return String.format("%s: {old: %s, new: %s}", propertyName, oldValue, newValue);
        }
    }

    public static PropertyDiff valueOf(ModificationProxy proxy) {
        Objects.requireNonNull(proxy);
        List<String> propertyNames = proxy.getPropertyNames();
        List<Object> oldValues = proxy.getOldValues();
        List<Object> newValues = proxy.getNewValues();
        return valueOf(propertyNames, oldValues, newValues);
    }

    public static PropertyDiff valueOf(
            final @NonNull List<String> propertyNames,
            final @NonNull List<Object> oldValues,
            final @NonNull List<Object> newValues) {

        PropertyDiffBuilder<Info> builder = PropertyDiff.builder();
        IntStream.range(0, propertyNames.size())
                .forEach(
                        i -> {
                            String prop = propertyNames.get(i);
                            Object oldV = oldValues.get(i);
                            Object newV = newValues.get(i);
                            builder.with(prop, oldV, newV);
                        });
        return builder.build();
    }

    public static PropertyDiff empty() {
        return new PropertyDiff();
    }

    public static class PropertyDiffBuilder<T extends Info> {

        private T info;
        private List<String> propertyNames = new ArrayList<>();
        private List<Object> newValues = new ArrayList<>();
        private List<Object> oldValues = new ArrayList<>();

        PropertyDiffBuilder() {
            this.info = null;
        }

        PropertyDiffBuilder(T info) {
            Objects.requireNonNull(info);
            if (Proxy.isProxyClass(info.getClass())) {
                throw new IllegalArgumentException("No proxies allowed");
            }
            this.info = info;
            ClassMappings classMappings = ClassMappings.fromImpl(info.getClass());
            Objects.requireNonNull(
                    classMappings, "Unknown info class: " + info.getClass().getCanonicalName());
        }

        public PropertyDiff build() {
            List<Change> changes =
                    IntStream.range(0, propertyNames.size())
                            .mapToObj(
                                    i -> {
                                        String name = propertyNames.get(i);
                                        Object oldV = oldValues.get(i);
                                        Object newV = newValues.get(i);
                                        return Change.valueOf(name, oldV, newV);
                                    })
                            .toList();
            return new PropertyDiff(changes);
        }

        public PropertyDiffBuilder<T> with(String property, Object newValue) {
            property = fixCase(property);
            Class<? extends Info> type = info.getClass();
            ClassProperties classProperties = OwsUtils.getClassProperties(type);
            if (null
                    == classProperties.getter(
                            property, newValue == null ? null : newValue.getClass())) {
                throw new IllegalArgumentException("No such property: " + property);
            }

            Object oldValue = OwsUtils.get(info, property);
            return with(property, oldValue, newValue);
        }

        public PropertyDiffBuilder<T> with(String property, Object oldValue, Object newValue) {
            property = fixCase(property);
            oldValue = copySafe(oldValue);
            newValue = copySafe(newValue);
            remove(property);
            propertyNames.add(property);
            oldValues.add(oldValue);
            newValues.add(newValue);
            return this;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public static <V> V copySafe(V val) {
            if (val instanceof Collection) return (V) copyOf((Collection) val);
            if (val instanceof Map) {
                return (V) copyOf((Map<?, ?>) val);
            }
            return val;
        }

        public static <V> Collection<V> copyOf(Collection<? extends V> val) {
            return copyOf(val, Function.identity());
        }

        public static <V, R> Collection<R> copyOf(
                Collection<? extends V> val, Function<V, R> mapper) {

            Stream<R> stream = val.stream().map(PropertyDiffBuilder::copySafe).map(mapper);

            if (val instanceof SortedSet) {
                @SuppressWarnings("unchecked")
                Comparator<Object> comparator = ((SortedSet<Object>) val).comparator();
                return stream.collect(Collectors.toCollection(() -> new TreeSet<>(comparator)));
            }
            if (val instanceof Set) {
                return stream.collect(Collectors.toCollection(HashSet::new));
            }
            return stream.collect(Collectors.toList());
        }

        public static <K, V> Map<K, V> copyOf(final Map<K, V> val) {
            return copyOf(val, Function.identity());
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public static <K, V, R> Map<K, R> copyOf(final Map<K, V> val, Function<V, R> valueMapper) {
            Map target;
            if (val instanceof MetadataMap) {
                target = new MetadataMap();
            } else if (val instanceof SortedMap) {
                Comparator comparator = ((SortedMap) val).comparator();
                target = new TreeMap<>(comparator);
            } else {
                target = new HashMap<>();
            }
            val.forEach(
                    (k, v) -> {
                        Object key = copySafe(k);
                        V value = copySafe(v);
                        R result = valueMapper.apply(value);
                        target.put(key, result);
                    });
            return target;
        }

        public PropertyDiffBuilder<T> remove(String property) {
            int i = propertyNames.indexOf(property);
            if (i > -1) {
                propertyNames.remove(i);
                oldValues.remove(i);
                oldValues.remove(i);
            }
            return this;
        }

        private static String fixCase(String propertyName) {
            if (propertyName.length() > 1) {
                char first = propertyName.charAt(0);
                char second = propertyName.charAt(1);
                if (!Character.isUpperCase(second)) {
                    propertyName = Character.toLowerCase(first) + propertyName.substring(1);
                }
            }
            return propertyName;
        }
    }
}
