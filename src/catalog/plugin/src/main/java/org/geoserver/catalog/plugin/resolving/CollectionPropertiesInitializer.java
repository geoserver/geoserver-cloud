/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.resolving;

import java.util.function.UnaryOperator;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;
import org.geoserver.ows.util.OwsUtils;

/**
 * A {@link UnaryOperator} that initializes null collection properties of objects to empty collections.
 *
 * <p>This utility class provides a resolver for use with
 * {@link ResolvingCatalogFacadeDecorator#setOutboundResolver(UnaryOperator)}, ensuring that collection
 * properties (e.g., lists, sets) within an object are initialized to empty collections if null. It leverages
 * {@link OwsUtils#resolveCollections(Object)} to perform the initialization, making objects safe for use
 * in contexts where null collections are undesirable. The resolver is stateless and reusable via a
 * singleton instance.
 *
 * <p>Key features:
 * <ul>
 *   <li><strong>Collection Initialization:</strong> Converts null collections to empty ones within the object.</li>
 *   <li><strong>Null Safety:</strong> Returns null for null inputs without modification.</li>
 *   <li><strong>Singleton Design:</strong> Provides a single, reusable instance via {@link #instance()}.</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * ResolvingCatalogFacadeDecorator facade = ...;
 * UnaryOperator<Object> resolver = CollectionPropertiesInitializer.instance();
 * facade.setOutboundResolver(resolver);
 * </pre>
 *
 * @param <T> The type of object to process (typically {@link CatalogInfo} or other {@link ModificationProxy}-compatible types).
 * @since 1.0
 * @see OwsUtils#resolveCollections(Object)
 * @see ResolvingCatalogFacadeDecorator
 */
public class CollectionPropertiesInitializer<T> implements UnaryOperator<T> {

    private static final CollectionPropertiesInitializer<?> INSTANCE = new CollectionPropertiesInitializer<>();

    /**
     * Applies the resolver to initialize null collection properties of an object.
     *
     * <p>Uses {@link OwsUtils#resolveCollections(Object)} to ensure all null collection fields are set to
     * empty collections. If the input is null, returns null without modification.
     *
     * @param value The object to process; may be null.
     * @return The processed object with initialized collections, or null if {@code value} is null.
     */
    @Override
    public T apply(T value) {
        if (value != null) {
            OwsUtils.resolveCollections(value);
        }
        return value;
    }

    /**
     * Returns the singleton instance of this resolver.
     *
     * @param <T> The type of object to process.
     * @return The singleton {@link CollectionPropertiesInitializer}; never null.
     * @example Using the singleton instance:
     *          <pre>
     *          UnaryOperator<Object> resolver = CollectionPropertiesInitializer.instance();
     *          Object obj = ...;
     *          Object resolved = resolver.apply(obj);
     *          </pre>
     */
    @SuppressWarnings("unchecked")
    public static <T> CollectionPropertiesInitializer<T> instance() {
        return (CollectionPropertiesInitializer<T>) INSTANCE;
    }
}
