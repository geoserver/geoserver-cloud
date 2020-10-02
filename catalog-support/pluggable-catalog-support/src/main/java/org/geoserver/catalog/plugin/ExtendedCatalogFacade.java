/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import java.io.Closeable;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * {@link CatalogFacade} with additional methods
 *
 * <p>If this were going to be merged into geoserver's main module, the new methods could be added
 * directly to {@link CatalogFacade}
 */
public interface ExtendedCatalogFacade extends CatalogFacade {

    <I extends CatalogInfo> I update(I info, Patch patch);

    /** @deprecated use {@link #query(Query)} instead */
    @Deprecated
    default @Override <T extends CatalogInfo> CloseableIterator<T> list(
            final Class<T> of,
            final Filter filter,
            @Nullable Integer offset,
            @Nullable Integer count,
            @Nullable SortBy... sortOrder) {

        Objects.requireNonNull(of, "query Info class not provided");
        Objects.requireNonNull(filter, "filter not provided");

        Query<T> query = Query.valueOf(of, filter, offset, count, sortOrder);
        Stream<T> stream = query(query);

        final Closeable closeable = stream::close;
        return new CloseableIteratorAdapter<T>(stream.iterator(), closeable);
    }

    /**
     * Returns all objects in this that satisfy the query criteria (type and filter), and additional
     * restrictions such as paging and order.
     *
     * <p>Be sure to {@link Stream#close} close the returned stream once consumed or before
     * discarding. Since {@link Stream} implements {@link AutoCloseable} it can be used in a
     * try-with-resources block, and eliminates the need to return {@link CloseableIterator}
     *
     * @see Query
     */
    <T extends CatalogInfo> Stream<T> query(Query<T> query);
}
