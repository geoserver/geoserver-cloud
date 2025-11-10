/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin;

import jakarta.annotation.Nullable;
import java.io.Closeable;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
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
 * An enhanced version of {@link CatalogFacade} providing additional methods for querying and manipulating
 * the GeoServer Cloud catalog.
 *
 * <p>This interface extends the standard {@link CatalogFacade} with modern, streamlined operations for
 * interacting with the GeoServer Cloud catalog. It introduces type-safe retrieval by ID, patch-based
 * updates, and stream-based querying, enhancing flexibility and usability over the base facade. These
 * enhancements reduce boilerplate code and align with contemporary Java practices, such as using
 * {@link Optional} and {@link Stream}.
 *
 * <p>The following methods could potentially be upstreamed to {@link CatalogFacade} in GeoServer’s main
 * module:
 * <ul>
 *   <li>{@link #get(String)}: Retrieves a catalog object by ID without type specification.</li>
 *   <li>{@link #get(String, Class)}: Retrieves a typed catalog object by ID with type safety.</li>
 *   <li>{@link #getPublished(String)}: Retrieves a published object (layer or layer group) by ID.</li>
 *   <li>{@link #add(CatalogInfo)}: Adds a new catalog object with type-specific dispatching.</li>
 *   <li>{@link #remove(CatalogInfo)}: Removes a catalog object with type-specific dispatching.</li>
 *   <li>{@link #update(CatalogInfo, Patch)}: Updates an existing object using a patch for precise changes.</li>
 *   <li>{@link #query(Query)}: Queries the catalog with advanced filtering, sorting, and pagination.</li>
 * </ul>
 *
 * <p>All methods are default implementations, delegating to the underlying {@link CatalogFacade} where
 * applicable, allowing implementers to override them for optimized behavior. Deprecated methods like
 * {@link #save(CatalogInfo)} are replaced by {@link #update(CatalogInfo, Patch)}, and {@link #list} is
 * superseded by {@link #query(Query)} for modern stream-based access.
 *
 * @since 1.0
 * @see CatalogFacade
 * @see Query
 * @see Patch
 */
public interface ExtendedCatalogFacade extends CatalogFacade {

    /**
     * Retrieves a catalog object by its ID, searching across all supported types.
     *
     * <p>This method sequentially queries for a match among workspaces, namespaces, stores, resources,
     * published objects (layers or layer groups), styles, and maps, returning the first object found.
     * If no object matches the ID, an empty {@link Optional} is returned.
     *
     * @param id The unique identifier of the catalog object; must not be null.
     * @return An {@link Optional} containing the found {@link CatalogInfo}, or empty if not found.
     * @throws NullPointerException if {@code id} is null.
     * @example Retrieving an object by ID:
     *          <pre>
     *          Optional<CatalogInfo> info = facade.get("ws1");
     *          info.ifPresent(i -> System.out.println("Found: " + i.getClass().getSimpleName()));
     *          </pre>
     */
    default Optional<CatalogInfo> get(@NonNull String id) {
        CatalogInfo found = getWorkspace(id);
        if (null == found) {
            found = getNamespace(id);
        }
        if (null == found) {
            found = getStore(id, StoreInfo.class);
        }
        if (null == found) {
            found = getResource(id, ResourceInfo.class);
        }
        if (null == found) {
            found = getPublished(id);
        }
        if (null == found) {
            found = getStyle(id);
        }
        if (null == found) {
            found = getMap(id);
        }
        return Optional.ofNullable(found);
    }

    /**
     * Retrieves a catalog object by its ID with type safety.
     *
     * <p>This method fetches an object by ID and ensures it matches the specified type (e.g.,
     * {@link LayerInfo}, {@link WorkspaceInfo}). It uses {@link ClassMappings} to dispatch to the
     * appropriate type-specific retrieval method from {@link CatalogFacade}, filtering the result
     * to confirm type compatibility. Returns an empty {@link Optional} if no matching object is found
     * or if the object doesn’t match the type.
     *
     * @param <T>  The expected type of {@link CatalogInfo}.
     * @param id   The unique identifier of the catalog object; must not be null.
     * @param type The interface type to retrieve (e.g., {@link LayerInfo.class}); must not be null.
     * @return An {@link Optional} containing the found object cast to type {@code T}, or empty if not found.
     * @throws NullPointerException if {@code id} or {@code type} is null.
     * @throws IllegalArgumentException if {@code type} is not an interface or is an unknown {@link CatalogInfo} subtype.
     * @example Retrieving a typed object:
     *          <pre>
     *          Optional<LayerInfo> layer = facade.get("layer1", LayerInfo.class);
     *          layer.ifPresent(l -> System.out.println("Layer name: " + l.getName()));
     *          </pre>
     */
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

    /**
     * Retrieves a published catalog object (layer or layer group) by its ID.
     *
     * <p>This method attempts to find a {@link LayerInfo} or {@link LayerGroupInfo} by ID, returning the
     * first match as a {@link PublishedInfo}. If no match is found, returns null.
     *
     * @param id The unique identifier of the published object; must not be null.
     * @return The found {@link PublishedInfo} (either a layer or layer group), or null if not found.
     * @throws NullPointerException if {@code id} is null.
     * @example Retrieving a published object:
     *          <pre>
     *          PublishedInfo pub = facade.getPublished("layer1");
     *          if (pub != null) System.out.println("Published name: " + pub.getName());
     *          </pre>
     */
    default PublishedInfo getPublished(@NonNull String id) {
        return get(id, LayerInfo.class)
                .map(PublishedInfo.class::cast)
                .or(() -> get(id, LayerGroupInfo.class))
                .orElse(null);
    }

    /**
     * Adds a new catalog object to the catalog with type-specific dispatching.
     *
     * <p>This method identifies the type of the provided {@link CatalogInfo} object and delegates to the
     * appropriate {@link CatalogFacade} add method, ensuring type safety via pattern matching. The added
     * object is returned, potentially updated with generated IDs or other properties by the catalog.
     *
     * @param <T>  The type of {@link CatalogInfo} to add.
     * @param info The catalog object to add; must not be null.
     * @return The added object, cast to type {@code T}.
     * @throws NullPointerException if {@code info} is null.
     * @throws IllegalArgumentException if {@code info} is an unrecognized {@link CatalogInfo} subtype.
     * @example Adding a workspace:
     *          <pre>
     *          WorkspaceInfo ws = new WorkspaceInfoImpl();
     *          ws.setName("newWorkspace");
     *          WorkspaceInfo added = facade.add(ws);
     *          System.out.println("Added ID: " + added.getId());
     *          </pre>
     */
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

    /**
     * Removes a catalog object from the catalog with type-specific dispatching.
     *
     * <p>This method identifies the type of the provided {@link CatalogInfo} object and delegates to the
     * appropriate {@link CatalogFacade} remove method, ensuring proper cleanup of associated resources.
     *
     * @param info The catalog object to remove; must not be null.
     * @throws NullPointerException if {@code info} is null.
     * @throws IllegalArgumentException if {@code info} is an unrecognized {@link CatalogInfo} subtype.
     * @example Removing a layer:
     *          <pre>
     *          LayerInfo layer = facade.get("layer1", LayerInfo.class).get();
     *          facade.remove(layer);
     *          System.out.println("Layer removed");
     *          </pre>
     */
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

    /**
     * Updates an existing catalog object with the specified patch of changes.
     *
     * <p>This method applies a {@link Patch} to modify properties of the given {@link CatalogInfo} object,
     * persisting the changes in the catalog. The updated object is returned, reflecting the applied changes.
     * Unlike the deprecated {@code save} methods, this approach allows precise, incremental updates without
     * requiring a full object replacement, supporting efficient storage operations.
     *
     * @param <I>   The type of {@link CatalogInfo} to update.
     * @param info  The catalog object to update; must not be null.
     * @param patch The patch containing property changes to apply; must not be null.
     * @return The updated {@link CatalogInfo} object after applying the patch.
     * @throws NullPointerException if {@code info} or {@code patch} is null.
     * @throws IllegalArgumentException if {@code info} is not found in the catalog or the patch is invalid.
     * @example Updating a layer’s title:
     *          <pre>
     *          LayerInfo layer = facade.get("layer1", LayerInfo.class).get();
     *          Patch patch = new Patch().with("title", "Updated Title");
     *          LayerInfo updated = facade.update(layer, patch);
     *          System.out.println("New title: " + updated.getTitle());
     *          </pre>
     */
    <I extends CatalogInfo> I update(I info, Patch patch);

    /**
     * Queries the catalog for objects matching the specified criteria, returning a stream of results.
     *
     * <p>This method retrieves all {@link CatalogInfo} objects that satisfy the {@link Query}’s type,
     * filter, sorting, and pagination constraints. The returned {@link Stream} must be closed after use
     * to release resources, ideally using a try-with-resources block since {@link Stream} implements
     * {@link AutoCloseable}. This replaces the deprecated {@link #list} method for modern, stream-based
     * access.
     *
     * @param <T>   The type of {@link CatalogInfo} to query.
     * @param query The query defining the criteria (type, filter, sorting, pagination); must not be null.
     * @return A {@link Stream} of matching catalog objects; never null.
     * @throws NullPointerException if {@code query} is null.
     * @example Querying layers with a filter:
     *          <pre>
     *          Query<LayerInfo> query = Query.valueOf(LayerInfo.class, someFilter);
     *          try (Stream<LayerInfo> layers = facade.query(query)) {
     *              layers.forEach(l -> System.out.println(l.getName()));
     *          }
     *          </pre>
     */
    <T extends CatalogInfo> Stream<T> query(Query<T> query);

    /**
     * Retrieves a list of catalog objects matching the specified criteria, using a legacy iterator approach.
     *
     * <p>This method is deprecated in favor of {@link #query(Query)}, which provides a modern
     * {@link Stream}-based API. It constructs a {@link Query} from the parameters and adapts the result
     * to a {@link CloseableIterator} for backward compatibility.
     *
     * @param <T>       The type of {@link CatalogInfo} to list.
     * @param of        The class of catalog objects to retrieve; must not be null.
     * @param filter    The filter to apply; must not be null.
     * @param offset    The number of objects to skip, or null for no offset.
     * @param count     The maximum number of objects to return, or null for no limit.
     * @param sortOrder Variable number of {@link SortBy} directives for ordering (nulls ignored).
     * @return A {@link CloseableIterator} over the matching catalog objects.
     * @throws NullPointerException if {@code of} or {@code filter} is null.
     * @deprecated since 1.0, for removal; use {@link #query(Query)} instead.
     * @example Legacy listing of layers:
     *          <pre>
     *          CloseableIterator<LayerInfo> it = facade.list(LayerInfo.class, Filter.INCLUDE, 0, 10);
     *          try (it) {
     *              while (it.hasNext()) System.out.println(it.next().getName());
     *          }
     *          </pre>
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
     * Throws an exception to enforce use of {@link #update(CatalogInfo, Patch)}.
     * @throws UnsupportedOperationException always, directing users to {@link #update(CatalogInfo, Patch)}.
     * @deprecated since 1.0, for removal; use {@link #update(CatalogInfo, Patch)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    default void save(LayerGroupInfo layerGroup) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    /**
     * Throws an exception to enforce use of {@link #update(CatalogInfo, Patch)}.
     * @throws UnsupportedOperationException always, directing users to {@link #update(CatalogInfo, Patch)}.
     * @deprecated since 1.0, for removal; use {@link #update(CatalogInfo, Patch)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    default void save(LayerInfo layer) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    /**
     * Throws an exception to enforce use of {@link #update(CatalogInfo, Patch)}.
     * @throws UnsupportedOperationException always, directing users to {@link #update(CatalogInfo, Patch)}.
     * @deprecated since 1.0, for removal; use {@link #update(CatalogInfo, Patch)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    default void save(MapInfo map) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    /**
     * Throws an exception to enforce use of {@link #update(CatalogInfo, Patch)}.
     * @throws UnsupportedOperationException always, directing users to {@link #update(CatalogInfo, Patch)}.
     * @deprecated since 1.0, for removal; use {@link #update(CatalogInfo, Patch)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    default void save(NamespaceInfo namespace) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    /**
     * Throws an exception to enforce use of {@link #update(CatalogInfo, Patch)}.
     * @throws UnsupportedOperationException always, directing users to {@link #update(CatalogInfo, Patch)}.
     * @deprecated since 1.0, for removal; use {@link #update(CatalogInfo, Patch)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    default void save(ResourceInfo resource) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    /**
     * Throws an exception to enforce use of {@link #update(CatalogInfo, Patch)}.
     * @throws UnsupportedOperationException always, directing users to {@link #update(CatalogInfo, Patch)}.
     * @deprecated since 1.0, for removal; use {@link #update(CatalogInfo, Patch)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    default void save(StoreInfo store) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    /**
     * Throws an exception to enforce use of {@link #update(CatalogInfo, Patch)}.
     * @throws UnsupportedOperationException always, directing users to {@link #update(CatalogInfo, Patch)}.
     * @deprecated since 1.0, for removal; use {@link #update(CatalogInfo, Patch)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    default void save(WorkspaceInfo workspace) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    /**
     * Throws an exception to enforce use of {@link #update(CatalogInfo, Patch)}.
     * @throws UnsupportedOperationException always, directing users to {@link #update(CatalogInfo, Patch)}.
     * @deprecated since 1.0, for removal; use {@link #update(CatalogInfo, Patch)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    default void save(StyleInfo style) {
        throw new UnsupportedOperationException("Expected use of update(CatalogInfo, Patch)");
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation simply returns the provided object unchanged, assuming no proxying by default.
     */
    @Override
    default WorkspaceInfo detach(WorkspaceInfo info) {
        return info;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation simply returns the provided object unchanged, assuming no proxying by default.
     */
    @Override
    default NamespaceInfo detach(NamespaceInfo info) {
        return info;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation simply returns the provided object unchanged, assuming no proxying by default.
     */
    @Override
    default <T extends StoreInfo> T detach(T store) {
        return store;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation simply returns the provided object unchanged, assuming no proxying by default.
     */
    @Override
    default <T extends ResourceInfo> T detach(T resource) {
        return resource;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation simply returns the provided object unchanged, assuming no proxying by default.
     */
    @Override
    default LayerInfo detach(LayerInfo info) {
        return info;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation simply returns the provided object unchanged, assuming no proxying by default.
     */
    @Override
    default LayerGroupInfo detach(LayerGroupInfo info) {
        return info;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation simply returns the provided object unchanged, assuming no proxying by default.
     */
    @Override
    default StyleInfo detach(StyleInfo info) {
        return info;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation simply returns the provided object unchanged, assuming no proxying by default.
     */
    @Override
    default MapInfo detach(MapInfo info) {
        return info;
    }
}
