/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.ows.util.ClassProperties;
import org.geoserver.ows.util.OwsUtils;

public @Data class PropertyDiff implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Change> changes;

    public static <T extends Info> PropertyDiffBuilder<T> builder(T info) {
        return new PropertyDiffBuilder<>(info);
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

    /** @return a "clean copy", where no-op changes are ignored */
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
            return (m1 == null && m2.isEmpty()) || (m1.isEmpty() && m2 == null);
        }

        private boolean isNullCollectionOp(Collection<?> c1, Collection<?> c2) {
            return (c1 == null && c2.isEmpty()) || (c1.isEmpty() && c2 == null);
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

    public static PropertyDiff valueOf(
            final @NonNull List<String> propertyNames,
            final @NonNull List<Object> oldValues,
            final @NonNull List<Object> newValues) {

        PropertyDiff changeset = new PropertyDiff();
        IntStream.range(0, propertyNames.size())
                .mapToObj(
                        i ->
                                Change.valueOf(
                                        propertyNames.get(i), oldValues.get(i), newValues.get(i)))
                .forEach(changeset.getChanges()::add);
        return changeset;
    }

    public static PropertyDiff empty() {
        return new PropertyDiff();
    }

    public static class PropertyDiffBuilder<T extends Info> {

        private T info;
        private List<String> propertyNames = new ArrayList<>();
        private List<Object> newValues = new ArrayList<>();
        private List<Object> oldValues = new ArrayList<>();

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
            return PropertyDiff.valueOf(propertyNames, oldValues, newValues).clean();
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

            remove(property);
            Object oldValue = OwsUtils.get(info, property);
            propertyNames.add(property);
            oldValues.add(oldValue);
            newValues.add(newValue);
            return this;
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
