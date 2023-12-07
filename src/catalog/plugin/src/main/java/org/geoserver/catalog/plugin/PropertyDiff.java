/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ProxyUtils;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ows.util.ClassProperties;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.util.InternationalString;
import org.geotools.referencing.CRS;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
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

@Slf4j
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

    protected Object readResolve() {
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
        return new PropertyDiff(changes.stream().filter(Change::isNotEmpty).toList());
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static @Data class Change implements Serializable {
        private static final long serialVersionUID = 1L;
        private @NonNull String propertyName;
        private transient Object oldValue;
        private transient Object newValue;

        boolean isNotEmpty() {
            return !isNoChange();
        }

        public boolean isNoChange() {
            if (Objects.equals(oldValue, newValue)) return true;

            if (isCollectionProperty()) return bothAreNullOrEmpty();

            if (isA(InternationalString.class)) return isNullInternationalStringOp();

            if (isA(CoordinateReferenceSystem.class)) return isSameCrs();

            return false;
        }

        protected boolean isSameCrs() {
            if (neitherIsNull()) {
                try {
                    final boolean fullScan = false; // don't bother
                    String id1 =
                            CRS.lookupIdentifier((CoordinateReferenceSystem) oldValue, fullScan);
                    String id2 =
                            CRS.lookupIdentifier((CoordinateReferenceSystem) newValue, fullScan);
                    boolean sameId = Objects.equals(id1, id2);
                    return sameId && CRS.equalsIgnoreMetadata(oldValue, newValue);
                } catch (Exception e) {
                    log.trace("Error comparing PropertyDif CRS values", e);
                }
            }
            return false;
        }

        protected boolean isNullInternationalStringOp() {
            return isNullOrEmpty(toStringOrNull(oldValue))
                    && isNullOrEmpty(toStringOrNull(newValue));
        }

        private String toStringOrNull(Object o) {
            return o == null ? null : o.toString();
        }

        private boolean isNullOrEmpty(Object o) {
            if (o == null) return true;
            if (o instanceof String s) return s.isEmpty();
            if (o instanceof Collection<?> c) return c.isEmpty();
            if (o instanceof Map<?, ?> m) return m.isEmpty();
            return false;
        }

        private boolean neitherIsNull() {
            return oldValue != null && newValue != null;
        }

        private boolean bothAreNullOrEmpty() {
            return isNullOrEmpty(oldValue) && isNullOrEmpty(newValue);
        }

        private boolean isCollectionProperty() {
            return isA(Collection.class, oldValue, newValue) || isA(Map.class, oldValue, newValue);
        }

        private boolean isA(@NonNull Class<?> type) {
            return isA(type, oldValue, newValue);
        }

        private boolean isA(@NonNull Class<?> type, Object value1, Object value2) {
            return isA(type, value1) || isA(type, value2);
        }

        private boolean isA(@NonNull Class<?> type, Object value) {
            return type.isInstance(value);
        }

        public static Change valueOf(String propertyName, Object oldValue, Object newValue) {
            return new Change(propertyName, oldValue, newValue);
        }

        @Override
        public String toString() {
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
                            Object oldV = hanldeProxy(oldValues.get(i));
                            Object newV = hanldeProxy(newValues.get(i));
                            builder.with(prop, oldV, newV);
                        });
        return builder.build();
    }

    private static Object hanldeProxy(Object value) {
        if (value == null) return null;
        ModificationProxy proxy = ProxyUtils.handler(value, ModificationProxy.class);
        if (null != proxy) {
            Class<? extends Object> type = proxy.getProxyObject().getClass();
            Class<Info> infoInterface = findInfoIterface(type);
            return ModificationProxy.rewrap((Info) value, i -> i, infoInterface);
        }
        return value;
    }

    private static final Set<Class<? extends Info>> IGNORE =
            Set.of(Info.class, Catalog.class, ServiceInfo.class, PublishedInfo.class);

    @SuppressWarnings("unchecked")
    private static <T extends Info> Class<T> findInfoIterface(Class<?> of) {
        return (Class<T>)
                Arrays.stream(of.getInterfaces())
                        .filter(c -> !IGNORE.contains(c))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unable to find most concrete Info sub-interface of "
                                                        + of.getCanonicalName()));
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

        @SuppressWarnings({"unchecked"})
        public static <V> V copySafe(V val) {
            if (val instanceof Collection<?> c) return (V) copyOf(c);
            if (val instanceof Map<?, ?> m) return (V) copyOf(m);
            return val;
        }

        public static <V> Collection<V> copyOf(Collection<? extends V> val) {
            return copyOf(val, Function.identity());
        }

        public static <V, R> Collection<R> copyOf(
                Collection<? extends V> val, Function<V, R> mapper) {

            Stream<R> stream = val.stream().map(PropertyDiffBuilder::copySafe).map(mapper);

            if (val instanceof SortedSet<?> set) {
                @SuppressWarnings("unchecked")
                Comparator<Object> comparator = (Comparator<Object>) set.comparator();
                return stream.collect(Collectors.toCollection(() -> new TreeSet<>(comparator)));
            }
            if (val instanceof Set<?>) {
                return stream.collect(Collectors.toCollection(HashSet::new));
            }
            return stream.toList();
        }

        public static <K, V> Map<K, V> copyOf(final Map<K, V> val) {
            return copyOf(val, Function.identity());
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public static <K, V, R> Map<K, R> copyOf(final Map<K, V> val, Function<V, R> valueMapper) {
            Map target;
            if (val instanceof MetadataMap) {
                target = new MetadataMap();
            } else if (val instanceof SortedMap sortedMap) {
                Comparator comparator = sortedMap.comparator();
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
