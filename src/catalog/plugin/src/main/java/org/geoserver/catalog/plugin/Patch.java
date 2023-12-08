/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.ows.util.OwsUtils;

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

@NoArgsConstructor
public @Data class Patch implements Serializable {
    private static final long serialVersionUID = 1L;

    public static @Data class Property {
        private final String name;
        private final Object value;

        @SuppressWarnings("unchecked")
        public <V> V value() {
            return (V) value;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Property p && valueEquals(value(), p.value());
        }

        @Override
        public int hashCode() {
            return Objects.hash(Property.class, value);
        }

        public static boolean valueEquals(final Object v1, final Object v2) {
            if (Objects.equals(v1, v2)) {
                return true;
            }

            if (v1 != null && v1.getClass().isArray() && v2 != null && v2.getClass().isArray()) {
                if (!v1.getClass().equals(v2.getClass())) return false;
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
                        default -> throw new IllegalArgumentException(
                                "Unexpected value: " + componentType);
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

    public Patch(List<Property> patches) {
        patches.forEach(this::add);
    }

    public int size() {
        return patches.size();
    }

    public boolean isEmpty() {
        return patches.isEmpty();
    }

    public void add(@NonNull Property prop) {
        patches.add(prop);
    }

    public Property add(String name, Object value) {
        Objects.requireNonNull(name, "name");
        Property p = new Property(name, value);
        add(p);
        return p;
    }

    public Patch with(String name, Object value) {
        add(name, value);
        return this;
    }

    public List<String> getPropertyNames() {
        return patches.stream().map(Property::getName).toList();
    }

    public Optional<Property> get(String propertyName) {
        return patches.stream().filter(p -> p.getName().equals(propertyName)).findFirst();
    }

    public Optional<Object> getValue(String propertyName) {
        return get(propertyName).map(Property::getValue);
    }

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

    public <T> T applyTo(T target, Class<?> objectType) {
        patches.forEach(p -> apply(target, objectType, p));
        return target;
    }

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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void applyCollectionValueChange(Object target, final Property change) {
        final String propertyName = change.getName();
        final var currentValue = (Collection) OwsUtils.get(target, propertyName);
        final var newValue = (Collection) change.getValue();
        if (currentValue != null) {
            try {
                currentValue.clear();
            } catch (UnsupportedOperationException e) {
                throw new IllegalArgumentException(
                        "Collection property " + propertyName + " is immutable", e);
            }
            if (newValue != null) {
                currentValue.addAll(newValue);
            }
        }
    }

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

    private static Method findGetterOrThrow(Class<?> objectType, Property change) {
        Method getter = OwsUtils.getter(objectType, change.getName(), null);
        if (getter == null) {
            throw new IllegalArgumentException(
                    "No such property in target object: " + change.getName());
        }
        return getter;
    }

    private static boolean isMap(Method getter) {
        return Map.class.isAssignableFrom(getter.getReturnType());
    }

    private static boolean isCollection(Method getter) {
        return Collection.class.isAssignableFrom(getter.getReturnType());
    }

    @Override
    public String toString() {
        String props =
                this.getPatches().stream()
                        .map(p -> String.format("(%s: %s)", p.getName(), p.getValue()))
                        .collect(Collectors.joining(","));
        return String.format("%s[%s]", getClass().getSimpleName(), props);
    }
}
