/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import java.io.Closeable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;

/**
 * {@link CatalogFacade} with additional methods
 *
 * <p>If this were going to be merged into geoserver's main module, the new methods could be added
 * directly to {@link CatalogFacade}
 */
public interface ExtendedCatalogFacade extends CatalogFacade {

    default void forEach(Consumer<? super CatalogInfo> consumer) {
        List<Class<? extends CatalogInfo>> types = List.of(
                WorkspaceInfo.class,
                NamespaceInfo.class,
                StoreInfo.class,
                ResourceInfo.class,
                StyleInfo.class,
                LayerInfo.class,
                LayerGroupInfo.class);

        for (var type : types) {
            try (var stream = query(Query.valueOf(type, Filter.INCLUDE))) {
                stream.forEach(consumer);
            }
        }
    }

    default Optional<CatalogInfo> get(@NonNull String id) {
        CatalogInfo found = getWorkspace(id);
        if (null == found) found = getNamespace(id);
        if (null == found) found = getStore(id, StoreInfo.class);
        if (null == found) found = getResource(id, ResourceInfo.class);
        if (null == found) found = getPublished(id);
        if (null == found) found = getStyle(id);
        if (null == found) found = getMap(id);
        return Optional.ofNullable(found);
    }

    default <T extends CatalogInfo> Optional<T> get(@NonNull String id, @NonNull Class<T> type) {
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Expected an interface type, got " + type);
        }
        if (CatalogInfo.class.equals(type)) {
            return get(id).map(type::cast);
        }

        ClassMappings cm = ClassMappings.fromInterface(type);
        CatalogInfo found =
                switch (cm) {
                    case WORKSPACE -> getWorkspace(id);
                    case NAMESPACE -> getNamespace(id);

                    case DATASTORE, COVERAGESTORE, WMSSTORE, WMTSSTORE, STORE -> getStore(id, StoreInfo.class);

                    case FEATURETYPE, COVERAGE, WMSLAYER, WMTSLAYER, RESOURCE -> getResource(id, ResourceInfo.class);

                    case LAYER -> getLayer(id);
                    case LAYERGROUP -> getLayerGroup(id);
                    case PUBLISHED -> getPublished(id);

                    case STYLE -> getStyle(id);
                    case MAP -> getMap(id);
                    default -> throw new IllegalArgumentException("Unknown CatalogInfo type " + type);
                };

        return Optional.ofNullable(found).filter(type::isInstance).map(type::cast);
    }

    default PublishedInfo getPublished(@NonNull String id) {
        return get(id, LayerInfo.class)
                .map(PublishedInfo.class::cast)
                .or(() -> get(id, LayerGroupInfo.class))
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    default <T extends CatalogInfo> T add(@NonNull T info) {
        return switch (info) {
            case WorkspaceInfo ws -> (T) add(ws);
            case NamespaceInfo ns -> (T) add(ns);
            case StoreInfo st -> (T) add(st);
            case ResourceInfo r -> (T) add(r);
            case LayerInfo l -> (T) add(l);
            case LayerGroupInfo lg -> (T) add(lg);
            case StyleInfo s -> (T) add(s);
            case MapInfo m -> (T) add(m);
            default -> throw new IllegalArgumentException("Unexpected value: %s".formatted(info));
        };
    }

    default void remove(@NonNull CatalogInfo info) {
        switch (info) {
            case WorkspaceInfo ws -> remove(ws);
            case NamespaceInfo ns -> remove(ns);
            case StoreInfo st -> remove(st);
            case ResourceInfo r -> remove(r);
            case LayerInfo l -> remove(l);
            case LayerGroupInfo lg -> remove(lg);
            case StyleInfo s -> remove(s);
            case MapInfo m -> remove(m);
            default -> throw new IllegalArgumentException("Unexpected value: %s".formatted(info));
        }
    }

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
