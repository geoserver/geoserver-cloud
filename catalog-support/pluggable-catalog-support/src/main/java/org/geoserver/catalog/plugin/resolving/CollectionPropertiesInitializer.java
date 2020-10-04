/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import java.util.function.Function;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.ows.util.OwsUtils;

/**
 * {@link ResolvingCatalogFacade#setObjectResolver resolving function} that returns the incoming
 * object decorated with a {@link ModificationProxy}
 *
 * @see OwsUtils#resolveCollections
 */
public class CollectionPropertiesInitializer<T> implements Function<T, T> {

    private static CollectionPropertiesInitializer<?> INSTANCE =
            new CollectionPropertiesInitializer<>();

    public @Override T apply(T value) {
        if (value != null) {
            OwsUtils.resolveCollections(value);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static <T> CollectionPropertiesInitializer<T> instance() {
        return (CollectionPropertiesInitializer<T>) INSTANCE;
    }
}
