/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

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

/**
 * Represents a set of property changes (differences) between the old and new states of a catalog object
 * in GeoServer. This class encapsulates a list of {@link Change} objects, each detailing a property
 * name, its old value, and its new value, enabling precise tracking and application of modifications to
 * entities like layers, styles, or workspaces.
 *
 * <p>{@code PropertyDiff} is used within the catalog system to compare states, generate patches (via
 * {@link #toPatch}), and facilitate updates across the distributed microservices architecture. It provides methods to inspect and refine the changes
 * (e.g., {@link #clean} to remove no-op changes).
 *
 * <p>The class is typically constructed using the {@link PropertyDiffBuilder} for incremental building or
 * directly from a {@link ModificationProxy} to capture changes programmatically. It is mutable but provides
 * immutable views of its data where appropriate.
 */
@Slf4j
public @Data class PropertyDiff implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Change> changes;

    /**
     * Creates a new builder for constructing a {@code PropertyDiff} from scratch.
     *
     * @param <T> The type of catalog object ({@link Info}) being diffed.
     * @return A new {@link PropertyDiffBuilder} instance.
     * @example Basic builder usage:
     *          <pre>
     *          PropertyDiff diff = PropertyDiff.builder()
     *              .with("title", "Old Title", "New Title")
     *              .build();
     *          </pre>
     */
    public static <T extends Info> PropertyDiffBuilder<T> builder() {
        return new PropertyDiffBuilder<>();
    }

    /**
     * Creates a builder initialized with an existing catalog object to compare against new values.
     *
     * @param <T>           The type of catalog object ({@link Info}).
     * @param oldValueHolder The original catalog object to use as a baseline for diffs.
     * @return A new {@link PropertyDiffBuilder} instance initialized with the old state.
     * @throws NullPointerException if {@code oldValueHolder} is null.
     * @example Builder with existing object:
     *          <pre>
     *          LayerInfo layer = ...; // existing layer
     *          PropertyDiff diff = PropertyDiff.builder(layer)
     *              .with("title", "New Title")
     *              .build();
     *          </pre>
     */
    public static <T extends Info> PropertyDiffBuilder<T> builder(T oldValueHolder) {
        return new PropertyDiffBuilder<>(oldValueHolder);
    }

    /**
     * Constructs an empty {@code PropertyDiff} with no changes.
     *
     * <p>This is a convenience constructor for creating an empty diff, equivalent to calling
     * {@link #PropertyDiff(List)} with an empty list.
     */
    public PropertyDiff() {
        this(Collections.emptyList());
    }

    /**
     * Constructs a {@code PropertyDiff} with the specified list of changes.
     *
     * <p>The provided list is defensively copied to ensure the internal state remains independent.
     *
     * @param changes The initial list of property changes. Must not be null.
     * @throws NullPointerException if {@code changes} is null.
     */
    public PropertyDiff(@NonNull List<Change> changes) {
        this.changes = new ArrayList<>(changes);
    }

    /**
     * Ensures proper deserialization by initializing the changes list if null.
     *
     * <p>This method is invoked during deserialization to guarantee the object is in a valid state.
     *
     * @return This {@code PropertyDiff} instance.
     */
    protected Object readResolve() {
        if (changes == null) changes = new ArrayList<>();
        return this;
    }

    /**
     * Converts this diff into a {@link Patch} containing only new values.
     *
     * <p>The resulting patch can be applied to update a catalog object, discarding old value
     * information from the diff.
     *
     * @return A new {@link Patch} instance with properties derived from this diff’s new values.
     * @example Converting to a patch:
     *          <pre>
     *          PropertyDiff diff = ...;
     *          Patch patch = diff.toPatch();
     *          </pre>
     */
    public Patch toPatch() {
        Patch patch = new Patch();
        changes.stream()
                .map(c -> new Patch.Property(c.getPropertyName(), c.getNewValue()))
                .forEach(patch::add);
        return patch;
    }

    /**
     * Returns the number of property changes in this diff.
     *
     * @return The size of the changes list.
     */
    public int size() {
        return changes.size();
    }

    /**
     * Retrieves a specific change by its index.
     *
     * @param index The index of the change to retrieve (0-based).
     * @return The {@link Change} at the specified index.
     * @throws IndexOutOfBoundsException if the index is out of range.
     */
    public Change get(int index) {
        return changes.get(index);
    }

    /**
     * Retrieves a change by its property name, if present.
     *
     * @param propertyName The name of the property to find.
     * @return An {@link Optional} containing the {@link Change} if found, or empty if not.
     * @throws NullPointerException if {@code propertyName} is null.
     */
    public Optional<Change> get(@NonNull String propertyName) {
        return changes.stream()
                .filter(c -> propertyName.equals(c.getPropertyName()))
                .findFirst();
    }

    /**
     * Returns the list of property names affected by this diff.
     *
     * @return An unmodifiable list of property names.
     */
    public List<String> getPropertyNames() {
        return getChanges().stream().map(Change::getPropertyName).toList();
    }

    /**
     * Returns the list of old values from this diff’s changes.
     *
     * @return An unmodifiable list of old values.
     */
    public List<Object> getOldValues() {
        return getChanges().stream().map(Change::getOldValue).toList();
    }

    /**
     * Returns the list of new values from this diff’s changes.
     *
     * @return An unmodifiable list of new values.
     */
    public List<Object> getNewValues() {
        return getChanges().stream().map(Change::getNewValue).toList();
    }

    /**
     * Creates a new {@code PropertyDiff} with no-op changes filtered out.
     *
     * <p>No-op changes (where old and new values are effectively equal) are removed using
     * {@link Change#isNotEmpty}. This is useful for optimizing updates by excluding redundant
     * modifications.
     *
     * @return A new {@code PropertyDiff} containing only meaningful changes.
     * @example Cleaning a diff:
     *          <pre>
     *          PropertyDiff diff = ...; // contains some no-op changes
     *          PropertyDiff cleanDiff = diff.clean();
     *          </pre>
     */
    public PropertyDiff clean() {
        return new PropertyDiff(changes.stream().filter(Change::isNotEmpty).toList());
    }

    /**
     * Checks if this diff contains no changes.
     *
     * @return {@code true} if the diff is empty; {@code false} otherwise.
     */
    public boolean isEmpty() {
        return changes.isEmpty();
    }

    /**
     * Represents a single property change within a {@link PropertyDiff}, capturing the property name,
     * old value, and new value.
     *
     * <p>This class includes logic to determine if the change is a no-op (e.g., old and new values are
     * equal or effectively equivalent), supporting special cases like collections, international strings,
     * and coordinate reference systems.
     */
    @NoArgsConstructor
    @AllArgsConstructor
    public static @Data class Change implements Serializable {
        private static final long serialVersionUID = 1L;
        private @NonNull String propertyName;
        private transient Object oldValue;
        private transient Object newValue;

        /**
         * Checks if this change has a meaningful effect.
         *
         * <p>A change is considered meaningful (not empty) if it’s not a no-op as determined by
         * {@link #isNoChange}.
         *
         * @return {@code true} if the change is meaningful; {@code false} if it’s a no-op.
         */
        boolean isNotEmpty() {
            return !isNoChange();
        }

        /**
         * Determines if this change is a no-op (i.e., no effective change).
         *
         * <p>Special cases are handled:
         * <ul>
         * <li>Equal objects (via {@link Objects#equals}).
         * <li>Empty or null collections/maps.
         * <li>Null or empty {@link InternationalString} values.
         * <li>Equivalent {@link CoordinateReferenceSystem} instances.
         * </ul>
         * @return {@code true} if the change is a no-op; {@code false} otherwise.
         */
        public boolean isNoChange() {
            if (Objects.equals(oldValue, newValue)) return true;

            if (isCollectionProperty()) return bothAreNullOrEmpty();

            if (isA(InternationalString.class)) return isNullInternationalStringOp();

            if (isA(CoordinateReferenceSystem.class)) return isSameCrs();

            return false;
        }

        /**
         * Compares two {@link CoordinateReferenceSystem} values for equivalence.
         *
         * <p>Equality is based on CRS identifiers and metadata-ignoring comparison, with fallback
         * logging for errors.
         *
         * @return {@code true} if the CRS values are equivalent; {@code false} otherwise.
         */
        protected boolean isSameCrs() {
            if (neitherIsNull()) {
                try {
                    final boolean fullScan = false; // don't bother
                    String id1 = CRS.lookupIdentifier((CoordinateReferenceSystem) oldValue, fullScan);
                    String id2 = CRS.lookupIdentifier((CoordinateReferenceSystem) newValue, fullScan);
                    boolean sameId = Objects.equals(id1, id2);
                    return sameId && CRS.equalsIgnoreMetadata(oldValue, newValue);
                } catch (Exception e) {
                    log.warn("Failed to compare CRS values in PropertyDiff {}", this, e);
                }
            }
            return bothAreNullOrEmpty();
        }

        /**
         * Checks if an {@link InternationalString} change is effectively a no-op.
         *
         * <p>Considers null or empty string representations as equivalent.
         *
         * @return {@code true} if both values are null or empty; {@code false} otherwise.
         */
        protected boolean isNullInternationalStringOp() {
            return isNullOrEmpty(toStringOrNull(oldValue)) && isNullOrEmpty(toStringOrNull(newValue));
        }

        /**
         * Converts an object to its string representation or null if it’s null.
         */
        private String toStringOrNull(Object o) {
            return o == null ? null : o.toString();
        }

        /**
         * Checks if an object is null, empty, or represents an empty collection/map/string.
         */
        private boolean isNullOrEmpty(Object o) {
            if (o == null) return true;
            if (o instanceof String s) return s.isEmpty();
            if (o instanceof Collection<?> c) return c.isEmpty();
            if (o instanceof Map<?, ?> m) return m.isEmpty();
            return false;
        }

        /**
         * Checks if neither old nor new value is null.
         */
        private boolean neitherIsNull() {
            return oldValue != null && newValue != null;
        }

        /**
         * Checks if both old and new values are null or empty (for collections/maps).
         */
        private boolean bothAreNullOrEmpty() {
            return isNullOrEmpty(oldValue) && isNullOrEmpty(newValue);
        }

        /**
         * Determines if this change involves a collection or map property.
         */
        private boolean isCollectionProperty() {
            return isA(Collection.class, oldValue, newValue) || isA(Map.class, oldValue, newValue);
        }

        /**
         * Checks if either value is an instance of the specified type.
         */
        private boolean isA(@NonNull Class<?> type) {
            return isA(type, oldValue, newValue);
        }

        /**
         * Checks if either of two values is an instance of the specified type.
         */
        private boolean isA(@NonNull Class<?> type, Object value1, Object value2) {
            return isA(type, value1) || isA(type, value2);
        }

        /**
         * Checks if a value is an instance of the specified type.
         */
        private boolean isA(@NonNull Class<?> type, Object value) {
            return type.isInstance(value);
        }

        /**
         * Creates a new {@code Change} instance with the specified values.
         *
         * @param propertyName The name of the property.
         * @param oldValue     The original value.
         * @param newValue     The new value.
         * @return A new {@code Change} instance.
         * @throws NullPointerException if {@code propertyName} is null.
         */
        public static Change valueOf(String propertyName, Object oldValue, Object newValue) {
            return new Change(propertyName, oldValue, newValue);
        }

        /**
         * Returns a string representation of this change for debugging.
         *
         * @return A string in the format "propertyName: {old: oldValue, new: newValue}".
         */
        @Override
        public String toString() {
            return "%s: {old: %s, new: %s}".formatted(propertyName, oldValue, newValue);
        }
    }

    /**
     * Creates a {@code PropertyDiff} from a {@link ModificationProxy} instance.
     *
     * <p>Extracts property names, old values, and new values from the proxy to build the diff.
     *
     * @param proxy The modification proxy containing changes.
     * @return A new {@code PropertyDiff} instance.
     * @throws NullPointerException if {@code proxy} is null.
     */
    public static PropertyDiff valueOf(ModificationProxy proxy) {
        Objects.requireNonNull(proxy);
        List<String> propertyNames = proxy.getPropertyNames();
        List<Object> oldValues = proxy.getOldValues();
        List<Object> newValues = proxy.getNewValues();
        return valueOf(propertyNames, oldValues, newValues);
    }

    /**
     * Creates a {@code PropertyDiff} from lists of property names, old values, and new values.
     *
     * <p>Ensures consistency by handling proxies within the values and building the diff via a
     * {@link PropertyDiffBuilder}.
     *
     * @param propertyNames The list of property names.
     * @param oldValues     The list of old values.
     * @param newValues     The list of new values.
     * @return A new {@code PropertyDiff} instance.
     * @throws NullPointerException if any argument is null.
     * @throws IllegalArgumentException if the lists have different sizes.
     */
    public static PropertyDiff valueOf(
            final @NonNull List<String> propertyNames,
            final @NonNull List<Object> oldValues,
            final @NonNull List<Object> newValues) {

        if (propertyNames.size() != oldValues.size() || propertyNames.size() != newValues.size()) {
            throw new IllegalArgumentException("Inconsistent property names and values list sizes");
        }

        PropertyDiffBuilder<Info> builder = PropertyDiff.builder();
        IntStream.range(0, propertyNames.size()).forEach(i -> {
            String prop = propertyNames.get(i);
            Object oldV = hanldeProxy(oldValues.get(i));
            Object newV = hanldeProxy(newValues.get(i));
            builder.with(prop, oldV, newV);
        });
        return builder.build();
    }

    /**
     * Handles proxy objects by unwrapping and rewrapping them appropriately in order to hold a safe reference that's not affected by
     * external changes to the proxy after this method is called.
     *
     * @param value The value to process.
     * @return The unwrapped or rewrapped value, or the original if not a proxy.
     */
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

    /**
     * Finds the most concrete {@link Info} sub-interface implemented by a class.
     *
     * @param of The class to inspect.
     * @return The most specific {@link Info} interface.
     * @throws IllegalArgumentException if no suitable interface is found.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Info> Class<T> findInfoIterface(Class<?> of) {
        return (Class<T>) Arrays.stream(of.getInterfaces())
                .filter(c -> !IGNORE.contains(c))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unable to find most concrete Info sub-interface of %s".formatted(of.getCanonicalName())));
    }

    /**
     * Returns an empty {@code PropertyDiff} instance.
     */
    public static PropertyDiff empty() {
        return new PropertyDiff();
    }

    /**
     * Builder class for incrementally constructing a {@code PropertyDiff}.
     *
     * <p>Allows adding property changes with old and new values, optionally using an existing
     * {@link Info} object as a baseline. Ensures safe copying of collection/map values and proper
     * case handling for property names.
     *
     * @param <T> The type of catalog object ({@link Info}) being diffed.
     */
    public static class PropertyDiffBuilder<T extends Info> {

        private T info;
        private List<String> propertyNames = new ArrayList<>();
        private List<Object> newValues = new ArrayList<>();
        private List<Object> oldValues = new ArrayList<>();

        PropertyDiffBuilder() {
            this.info = null;
        }

        /**
         * Constructs a builder initialized with an existing catalog object.
         *
         * @param info The original catalog object to use as a baseline.
         * @throws NullPointerException if {@code info} is null.
         * @throws IllegalArgumentException if {@code info} is a proxy or has an unknown class mapping.
         */
        PropertyDiffBuilder(T info) {
            Objects.requireNonNull(info);
            if (Proxy.isProxyClass(info.getClass())) {
                throw new IllegalArgumentException("No proxies allowed");
            }
            this.info = info;
            ClassMappings classMappings = ClassMappings.fromImpl(info.getClass());
            Objects.requireNonNull(
                    classMappings,
                    () -> "Unknown info class: " + info.getClass().getCanonicalName());
        }

        /**
         * Builds the {@code PropertyDiff} from accumulated changes.
         *
         * @return A new {@code PropertyDiff} instance containing all specified changes.
         */
        public PropertyDiff build() {
            if (propertyNames.size() != oldValues.size() || propertyNames.size() != newValues.size()) {
                throw new IllegalStateException("Inconsistent list sizes in PropertyDiffBuilder");
            }
            List<Change> changes = IntStream.range(0, propertyNames.size())
                    .mapToObj(i -> {
                        String name = propertyNames.get(i);
                        Object oldV = oldValues.get(i);
                        Object newV = newValues.get(i);
                        return Change.valueOf(name, oldV, newV);
                    })
                    .toList();
            return new PropertyDiff(changes);
        }

        /**
         * Adds a property change using the old value from the baseline object (if provided).
         *
         * @param property The property name.
         * @param newValue The new value for the property.
         * @return This builder for chaining.
         * @throws IllegalArgumentException if {@code property} doesn’t exist or {@code info} is null.
         */
        public PropertyDiffBuilder<T> with(String property, Object newValue) {
            property = fixCase(property);
            Class<? extends Info> type = info.getClass();
            ClassProperties classProperties = OwsUtils.getClassProperties(type);
            if (null == classProperties.getter(property, newValue == null ? null : newValue.getClass())) {
                throw new IllegalArgumentException("No such property: %s".formatted(property));
            }

            Object oldValue = OwsUtils.get(info, property);
            return with(property, oldValue, newValue);
        }

        /**
         * Adds a property change with explicit old and new values.
         *
         * @param property The property name.
         * @param oldValue The original value.
         * @param newValue The new value.
         * @return This builder for chaining.
         */
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

        /**
         * Safely copies a value, handling collections and maps.
         * @return A safely copied version of the value, or the original if not a collection/map.
         */
        @SuppressWarnings({"unchecked"})
        public static <V> V copySafe(V val) {
            if (val instanceof Collection<?> c) return (V) copyOf(c);
            if (val instanceof Map<?, ?> m) return (V) copyOf(m);
            return val;
        }

        /**
         * Copies a collection with identity mapping.
         * @return A new collection with the same elements.
         */
        public static <V> Collection<V> copyOf(Collection<? extends V> val) {
            return copyOf(val, Function.identity());
        }

        /**
         * Copies a collection with a custom mapping function.
         * @return A new collection with mapped elements.
         */
        public static <V, R> Collection<R> copyOf(Collection<? extends V> val, Function<V, R> mapper) {

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

        /**
         * Copies a map with identity mapping.
         * @return A new map with the same key-value pairs.
         */
        public static <K, V> Map<K, V> copyOf(final Map<K, V> val) {
            return copyOf(val, Function.identity());
        }

        /**
         * Copies a map with a custom value mapping function.
         *
         * @param val         The map to copy.
         * @param valueMapper The function to map values.
         * @return A new map with mapped values.
         */
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
            val.forEach((k, v) -> {
                Object key = copySafe(k);
                V value = copySafe(v);
                R result = valueMapper.apply(value);
                target.put(key, result);
            });
            return target;
        }

        /**
         * Removes a property change from the builder by name.
         *
         * @param property The property name to remove.
         * @return This builder for chaining.
         */
        public PropertyDiffBuilder<T> remove(String property) {
            int i = propertyNames.indexOf(property);
            if (i > -1) {
                propertyNames.remove(i);
                oldValues.remove(i);
                newValues.remove(i);
            }
            return this;
        }

        /**
         * Normalizes property name case to lowercase first letter if needed.
         *
         * @param propertyName The property name to normalize.
         * @return The normalized property name.
         */
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
