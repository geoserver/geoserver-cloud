/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import java.util.function.UnaryOperator;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;
import org.geoserver.ows.util.OwsUtils;

/**
 * {@link ResolvingCatalogFacadeDecorator#setObjectResolver resolving function} that returns the
 * incoming object decorated with a {@link ModificationProxy}
 *
 * @see OwsUtils#resolveCollections
 */
public class CollectionPropertiesInitializer<T> implements UnaryOperator<T> {

    private static final CollectionPropertiesInitializer<?> INSTANCE = new CollectionPropertiesInitializer<>();

    @Override
    public T apply(T value) {
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
