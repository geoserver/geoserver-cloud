/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import org.geoserver.ows.util.OwsUtils;

public @Value class Patch implements Serializable {
    private static final long serialVersionUID = 1L;

    public static @Value class Property {
        private final String name;
        private final Object value;
    }

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final Map<String, Property> patches = new TreeMap<>();

    public List<Property> getPatches() {
        return new ArrayList<Patch.Property>(patches.values());
    }

    public void add(Property prop) {
        patches.put(prop.getName(), prop);
    }

    public Property add(String name, Object value) {
        Objects.requireNonNull(name, "name");
        Property p = new Property(name, value);
        add(p);
        return p;
    }

    public void applyTo(Object target) {
        patches.values().forEach(p -> apply(target, p));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void apply(Object target, Property change) {
        Method getter = OwsUtils.getter(target.getClass(), change.getName(), null);
        if (getter == null) {
            throw new IllegalArgumentException(
                    "No such property in target object: " + change.getName());
        }
        if (isCollection(getter)) {
            Collection value = (Collection) change.getValue();
            Collection prop = (Collection) OwsUtils.get(target, change.getName());
            if (prop != null) {
                prop.clear();
                if (value != null) {
                    prop.addAll(value);
                }
            }
        } else if (isMap(getter)) {
            Map<Object, Object> value = (Map<Object, Object>) change.getValue();
            Map<Object, Object> prop = (Map<Object, Object>) OwsUtils.get(target, change.getName());
            if (prop != null) {
                prop.clear();
                if (value != null) {
                    prop.putAll(value);
                }
            }
        } else {
            OwsUtils.set(target, change.getName(), change.getValue());
        }
    }

    private static boolean isMap(Method getter) {
        return Map.class.isAssignableFrom(getter.getReturnType());
    }

    private static boolean isCollection(Method getter) {
        return Collection.class.isAssignableFrom(getter.getReturnType());
    }
}
