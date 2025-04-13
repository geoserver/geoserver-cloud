/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.ows.util.OwsUtils;

/**
 * Represents a patch of changes to be applied to a catalog object, encapsulating a list of property
 * updates. A patch is used to describe modifications (e.g., adding, updating, or removing properties)
 * to entities like layers, styles, or workspaces in the {@link Catalog}.
 *
 * <p>This class provides a flexible, mutable container for property changes, allowing incremental
 * construction of updates through methods like {@link #add} and {@link #with}. It is typically used
 * by the Catalog to batch updates and apply them to catalog objects, propagating changes
 * efficiently across the distributed system via events.
 *
 * <p>Each patch consists of a collection of {@link Property} objects, where each property specifies
 * a name and its new value. Unlike a diff, this class focuses on the target state rather than tracking
 * old values, enabling straightforward application of changes to objects.
 */
@NoArgsConstructor
public @Data class Patch implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Inner class representing a single property change within a {@link Patch}.
     *
     * <p>A {@code Property} encapsulates the name of the property to be updated and its new value.
     * It supports type-safe value retrieval and custom equality checking for arrays and primitives.
     *
     * @see Patch
     */
    public static @Data class Property {
        private final String name;
        private final Object value;

        /**
         * Retrieves the property value cast to the specified type.
         *
         * <p>This method provides a type-safe way to access the value, suppressing unchecked warnings
         * since the caller is responsible for ensuring type compatibility.
         *
         * @param <V> The expected type of the value.
         * @return The property value cast to type {@code V}.
         * @throws ClassCastException if the value cannot be cast to the requested type.
         * @example Accessing a string value:
         *          <pre>
         *          Property prop = new Property("title", "New Title");
         *          String title = prop.value();
         *          </pre>
         */
        @SuppressWarnings("unchecked")
        public <V> V value() {
            return (V) value;
        }

        /**
         * Compares this property to another object for equality.
         *
         * <p>Equality is based on the property name and value, using custom logic to handle arrays
         * and primitives via {@link #valueEquals}.
         *
         * @param o The object to compare with.
         * @return {@code true} if the objects are equal; {@code false} otherwise.
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Property p && valueEquals(value(), p.value());
        }

        /**
         * Computes the hash code for this property.
         *
         * <p>The hash code is derived from the property value, ensuring consistency with {@link #equals}.
         *
         * @return The hash code value.
         */
        @Override
        public int hashCode() {
            return Objects.hash(Property.class, value);
        }

        /**
         * Compares two values for equality, with special handling for arrays.
         *
         * <p>This utility method supports deep equality checks for primitive and object arrays,
         * ensuring accurate comparison of complex property values.
         * <p>Nonetheless, the values themselves must implement equals()
         *
         * @param v1 The first value to compare.
         * @param v2 The second value to compare.
         * @return {@code true} if the values are equal; {@code false} otherwise.
         * @throws IllegalArgumentException if an unexpected array component type is encountered.
         * @see Arrays#deepEquals
         */
        public static boolean valueEquals(final Object v1, final Object v2) {
            if (Objects.equals(v1, v2)) {
                return true;
            }

            if (v1 != null
                    && v1.getClass().isArray()
                    && v2 != null
                    && v2.getClass().isArray()) {
                if (!v1.getClass().equals(v2.getClass())) {
                    return false;
                }
                final Class<?> componentType = v1.getClass().getComponentType();

                if (componentType.isPrimitive()) {
                    return switch (componentType.getCanonicalName()) {
                        case "byte" -> Arrays.equals((byte[]) v1, (byte[]) v2);
                        case "boolean" -> Arrays.equals((boolean[]) v1, (boolean[]) v2);
                        case "char" -> Arrays.equals((char[]) v1, (char[]) v2);
                        case "short" -> Arrays.equals((short[]) v1, (short[]) v2);
                        case "int" -> Arrays.equals((int[]) v1, (int[]) v2);
                        case "long" -> Arrays.equals((long[]) v1, (long[]) v2);
                        case "float" -> Arrays.equals((float[]) v1, (float[]) v2);
                        case "double" -> Arrays.equals((double[]) v1, (double[]) v2);
                        default -> throw new IllegalArgumentException("Unexpected value: %s".formatted(componentType));
                    };
                } else {
                    Object[] a1 = (Object[]) v1;
                    Object[] a2 = (Object[]) v2;
                    return Arrays.deepEquals(a1, a2);
                }
            }
            return false;
        }
    }

    private final List<Property> patches = new ArrayList<>();

    /**
     * Constructs a new Patch with the given list of property changes.
     *
     * <p>The provided list is iterated and added to the internal collection, allowing further
     * modifications via {@link #add} or {@link #with}. If the input list is null or empty, the
     * patch starts empty, representing no changes initially.
     *
     * @param patches The initial list of property changes to include. May be null or empty.
     * @example Creating a patch to update a layer's title:
     *          <pre>
     *          List<Property> props = new ArrayList<>();
     *          props.add(new Property("title", "New Title"));
     *          Patch patch = new Patch(props);
     *          </pre>
     */
    public Patch(List<Property> patches) {
        patches.forEach(this::add);
    }

    /**
     * Returns the number of property changes in this patch.
     *
     * @return The size of the patches list.
     */
    public int size() {
        return patches.size();
    }
    /**
     * Checks if this patch contains no property changes.
     *
     * <p>An empty patch indicates no modifications will be applied, useful for conditional logic
     * in update workflows.
     *
     * @return {@code true} if the patch is empty; {@code false} otherwise.
     */
    public boolean isEmpty() {
        return patches.isEmpty();
    }

    /**
     * Adds a property change to this patch.
     *
     * <p>The property is appended to the internal list, allowing incremental construction of the
     * patch. This method ensures the property is non-null to maintain consistency.
     *
     * @param prop The property change to add.
     * @throws NullPointerException if the property is null.
     */
    public void add(@NonNull Property prop) {
        patches.add(prop);
    }

    /**
     * Adds a new property change with the specified name and value, returning the created property.
     *
     * <p>This convenience method creates a {@link Property} instance and adds it to the patch,
     * useful for programmatic construction.
     *
     * @param name  The name of the property to update (e.g., "title").
     * @param value The new value for the property.
     * @return The created {@link Property} instance.
     * @throws NullPointerException if the name is null.
     * @example Adding a property:
     *          <pre>
     *          Patch patch = new Patch();
     *          patch.add("enabled", true);
     *          </pre>
     */
    public Property add(String name, Object value) {
        Objects.requireNonNull(name, "name");
        Property p = new Property(name, value);
        add(p);
        return p;
    }

    /**
     * Adds a property change and returns this patch for method chaining.
     */
    public Patch with(String name, Object value) {
        add(name, value);
        return this;
    }

    /**
     * Returns the list of property names in this patch. */
    public List<String> getPropertyNames() {
        return patches.stream().map(Property::getName).toList();
    }

    /**
     * Retrieves a property by its name, if present.
     *
     * @param propertyName The name of the property to find.
     * @return An {@link Optional} containing the {@link Property} if found, or empty if not.
     */
    public Optional<Property> get(String propertyName) {
        return patches.stream().filter(p -> p.getName().equals(propertyName)).findFirst();
    }

    /**
     * Retrieves the value of a property by its name, if present.
     *
     * @param propertyName The name of the property to find.
     * @return An {@link Optional} containing the value if found, or empty if not.
     */
    public Optional<Object> getValue(String propertyName) {
        return get(propertyName).map(Property::getValue);
    }

    /**
     * Applies this patch to the target object, inferring its type.
     *
     * <p>This method attempts to determine the target’s type and applies all property changes.
     * If the target is a proxy, it unwraps it to find the actual type unless nested proxies prevent this.
     *
     * @param <T>   The type of the target object.
     * @param target The object to apply the patch to.
     * @return The modified target object.
     * @throws NullPointerException if the target is null.
     * @throws IllegalArgumentException if the target is a nested proxy and type cannot be inferred.
     */
    public <T> T applyTo(T target) {
        Objects.requireNonNull(target);
        Class<?> targetType = target.getClass();

        if (Proxy.isProxyClass(targetType)) {
            Class<?> subject = ModificationProxy.unwrap(target).getClass();
            if (Proxy.isProxyClass(subject)) {
                throw new IllegalArgumentException(
                        "Argument object is a dynamic proxy and couldn't determine it's surrogate type, use applyTo(Object, Class) instead");
            }
        }
        return applyTo(target, targetType);
    }

    /**
     * Applies this patch to the target object using the specified type.
     *
     * <p>This method applies each property change to the target, handling collections and maps
     * appropriately based on getter method signatures.
     *
     * @param <T>        The type of the target object.
     * @param target     The object to apply the patch to.
     * @param objectType The explicit type of the target object.
     * @return The modified target object.
     * @throws IllegalArgumentException if a property name does not exist in the target type.
     */
    public <T> T applyTo(T target, Class<?> objectType) {
        patches.forEach(p -> apply(target, objectType, p));
        return target;
    }

    /**
     * Applies a single property change to the target object.
     *
     * <p>Handles simple properties, collections, and maps based on the getter’s return type.
     *
     * @param target     The object to modify.
     * @param objectType The type of the target object.
     * @param change     The property change to apply.
     * @throws IllegalArgumentException if the property does not exist or is immutable.
     */
    private static void apply(Object target, Class<?> objectType, final Property change) {
        final Method getter = findGetterOrThrow(objectType, change);
        if (isCollection(getter)) {
            applyCollectionValueChange(target, change);
        } else if (isMap(getter)) {
            applyMapValueChange(target, change);
        } else {
            String propertyName = change.getName();
            Object newValue = change.getValue();
            OwsUtils.set(target, propertyName, newValue);
        }
    }

    /**
     * Applies a collection property change by updating the target object’s collection property.
     *
     * <p>If the new value is null, clears the existing collection if present. If the new value is
     * empty, sets the collection to empty. If the new value is non-empty, replaces the existing
     * collection’s contents or sets a new collection if none exists. Handles immutable collections
     * by throwing an exception.
     *
     * @param target The object whose collection property is being updated.
     * @param change The property change containing the property name and new collection value.
     * @throws IllegalArgumentException if the collection is immutable or cannot be set.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void applyCollectionValueChange(Object target, final Property change) {
        final String propertyName = change.getName();
        final var currentValue = (Collection) OwsUtils.get(target, propertyName);
        final var newValue = (Collection) change.getValue();

        if (newValue == null) {
            if (currentValue != null) {
                try {
                    currentValue.clear();
                } catch (UnsupportedOperationException e) {
                    throw new IllegalArgumentException(
                            "Collection property %s is immutable".formatted(propertyName), e);
                }
            }
            // Optionally: OwsUtils.set(target, propertyName, null); if null is a valid state
        } else if (currentValue != null) {
            try {
                currentValue.clear();
                if (!newValue.isEmpty()) {
                    currentValue.addAll(newValue);
                }
            } catch (UnsupportedOperationException e) {
                throw new IllegalArgumentException("Collection property %s is immutable".formatted(propertyName), e);
            }
        } else if (!newValue.isEmpty()) {
            // Create a new collection and set it, assuming OwsUtils supports this
            try {
                OwsUtils.set(target, propertyName, new ArrayList<>(newValue));
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to set collection property %s".formatted(propertyName), e);
            }
        }
    }
    /**
     * Applies a map property change by clearing and updating the existing map.
     */
    @SuppressWarnings("unchecked")
    private static void applyMapValueChange(Object target, final Property change) {
        final String propertyName = change.getName();
        final var currValue = (Map<Object, Object>) OwsUtils.get(target, propertyName);
        final var newValue = (Map<Object, Object>) change.getValue();
        if (currValue != null) {
            currValue.clear();
            if (newValue != null) {
                currValue.putAll(newValue);
            }
        }
    }

    /**
     * Finds the getter method for a property or throws an exception if not found.
     *
     * @throws IllegalArgumentException if no getter exists for the property name.
     */
    private static Method findGetterOrThrow(Class<?> objectType, Property change) {
        Method getter = OwsUtils.getter(objectType, change.getName(), null);
        if (getter == null) {
            throw new IllegalArgumentException("No such property in target object: %s".formatted(change.getName()));
        }
        return getter;
    }

    /**
     * Checks if the getter returns a map type.
     *
     * @param getter The getter method to inspect.
     * @return {@code true} if the return type is a {@link Map}; {@code false} otherwise.
     */
    private static boolean isMap(Method getter) {
        return Map.class.isAssignableFrom(getter.getReturnType());
    }

    /**
     * Checks if the getter returns a collection type.
     *
     * @param getter The getter method to inspect.
     * @return {@code true} if the return type is a {@link Collection}; {@code false} otherwise.
     */
    private static boolean isCollection(Method getter) {
        return Collection.class.isAssignableFrom(getter.getReturnType());
    }

    /**
     * Returns a string representation of this patch for debugging purposes.
     *
     * <p>Provides a concise summary of all property changes in the format "Patch[name: value, ...]".
     *
     * @return A string summarizing the patch’s contents.
     * @example Output: "Patch[title: New Title, enabled: true]"
     */
    @Override
    public String toString() {
        String props = this.getPatches().stream()
                .map(p -> "(%s: %s)".formatted(p.getName(), p.getValue()))
                .collect(Collectors.joining(","));
        return "%s[%s]".formatted(getClass().getSimpleName(), props);
    }
}
