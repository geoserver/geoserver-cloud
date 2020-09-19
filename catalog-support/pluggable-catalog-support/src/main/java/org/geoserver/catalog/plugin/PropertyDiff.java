/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import java.io.Serializable;
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

public @Data class PropertyDiff implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Change> changes;

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
}
