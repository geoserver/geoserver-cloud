/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;

import java.io.Closeable;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.Nullable;

/**
 * {@link CatalogFacade} with additional methods
 *
 * <p>If this were going to be merged into geoserver's main module, the new methods could be added
 * directly to {@link CatalogFacade}
 */
public interface ExtendedCatalogFacade extends CatalogFacade {

    <I extends CatalogInfo> I update(I info, Patch patch);

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

    /**
     * @deprecated use {@link #query(Query)} instead
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    default <T extends CatalogInfo> CloseableIterator<T> list(
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
        return new CloseableIteratorAdapter<>(stream.iterator(), closeable);
    }

    /**
     * @deprecated Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo,
     *     Patch)}
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    default void save(LayerGroupInfo layerGroup) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    /**
     * @deprecated Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo,
     *     Patch)}
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    default void save(LayerInfo layer) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    /**
     * @deprecated Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo,
     *     Patch)}
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    default void save(MapInfo map) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    /**
     * @deprecated Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo,
     *     Patch)}
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    default void save(NamespaceInfo namespace) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    /**
     * @deprecated Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo,
     *     Patch)}
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    default void save(ResourceInfo resource) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    /**
     * @deprecated Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo,
     *     Patch)}
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    default void save(StoreInfo store) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    /**
     * @deprecated Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo,
     *     Patch)}
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    default void save(WorkspaceInfo workspace) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    /**
     * @deprecated Throws {@link UnsupportedOperationException}, use {@link #update(CatalogInfo,
     *     Patch)}
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    default void save(StyleInfo style) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    @Override
    default WorkspaceInfo detach(WorkspaceInfo info) {
        return info;
    }

    @Override
    default NamespaceInfo detach(NamespaceInfo info) {
        return info;
    }

    @Override
    default <T extends StoreInfo> T detach(T store) {
        return store;
    }

    @Override
    default <T extends ResourceInfo> T detach(T resource) {
        return resource;
    }

    @Override
    default LayerInfo detach(LayerInfo info) {
        return info;
    }

    @Override
    default LayerGroupInfo detach(LayerGroupInfo info) {
        return info;
    }

    @Override
    default StyleInfo detach(StyleInfo info) {
        return info;
    }

    @Override
    default MapInfo detach(MapInfo info) {
        return info;
    }
}
