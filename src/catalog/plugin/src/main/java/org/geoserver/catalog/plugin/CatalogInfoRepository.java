/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.api.filter.Filter;
import org.springframework.lang.Nullable;

/**
 * A raw data access API for {@link CatalogInfo} back-end implementations in GeoServer.
 *
 * <p>This interface defines a null-safe, domain-driven design (DDD)-inspired "repository" API for storing
 * and querying plain {@link CatalogInfo} objects (e.g., workspaces, layers, styles) with precise semantics.
 * <p>
 * It serves as a low-level abstraction for catalog persistence, eschewing null arguments to ensure clear,
 * predictable behavior. Each method has unambiguous intent, avoiding magic or decision-making that is
 * delegated to higher-level abstractions like {@link CatalogFacade} or {@link org.geoserver.catalog.Catalog}.
 *
 * <p>Key features:
 * <ul>
 *   <li><strong>Null Safety:</strong> No method accepts null arguments; violations throw {@link NullPointerException}.</li>
 *   <li><strong>Query Semantics:</strong> Specialized methods (e.g., {@link StyleRepository#findAllByNullWorkspace()})
 *       provide explicit alternatives rather than relying on null checks or overloading.</li>
 *   <li><strong>Return Types:</strong> Single-result queries return {@link Optional}; multi-result queries return
 *       {@link Stream}, which must be closed to release resources.</li>
 *   <li><strong>Resource Management:</strong> Streams implement {@link AutoCloseable}, requiring proper closure
 *       (e.g., via try-with-resources) to avoid resource leaks in implementations using live connections.</li>
 * </ul>
 *
 * <p>Specialized sub-interfaces (e.g., {@link WorkspaceRepository}, {@link LayerRepository}) extend this
 * interface with type-specific operations, tailoring the API to each catalog entity type.
 *
 * @param <T> The specific type of {@link CatalogInfo} this repository manages (e.g., {@link LayerInfo}).
 * @since 1.0
 * @see WorkspaceRepository
 * @see NamespaceRepository
 * @see StoreRepository
 * @see ResourceRepository
 * @see LayerRepository
 * @see LayerGroupRepository
 * @see StyleRepository
 * @see MapRepository
 */
public interface CatalogInfoRepository<T extends CatalogInfo> {

    /**
     * Returns the least specific {@link CatalogInfo} type this repository handles.
     *
     * <p>This method identifies the base type of objects managed by the repository. For example, a
     * {@link ResourceRepository} would return {@link ResourceInfo}, indicating it handles all subtypes
     * like {@link FeatureTypeInfo}, {@link CoverageInfo}, {@link WMSLayerInfo}, and {@link WMTSLayerInfo}.
     *
     * @return The {@link Class} representing the repository’s content type; never null.
     */
    Class<T> getContentType();

    /**
     * Adds a new catalog object to the repository.
     *
     * <p>The object is persisted to the underlying storage, and its state may be updated (e.g., with a
     * generated ID). The added object remains unchanged in memory unless explicitly reassigned.
     *
     * @param value The catalog object to add; must not be null.
     * @throws NullPointerException if {@code value} is null.
     * @example Adding a workspace:
     *          <pre>
     *          WorkspaceInfo ws = new WorkspaceInfoImpl();
     *          ws.setName("myWorkspace");
     *          repository.add(ws);
     */
    void add(@NonNull T value);

    /**
     * Removes a catalog object from the repository.
     *
     * <p>The object is deleted from the underlying storage, and associated resources may be cleaned up
     * depending on the implementation. The provided object remains unchanged in memory.
     *
     * @param value The catalog object to remove; must not be null.
     * @throws NullPointerException if {@code value} is null.
     * @example Removing a layer:
     *          <pre>
     *          LayerInfo layer = ...; // existing layer
     *          repository.remove(layer);
     *          </pre>
     */
    void remove(@NonNull T value);

    /**
     * Updates an existing catalog object in the repository with the specified patch of changes.
     *
     * <p>This method applies the provided {@link Patch} to the repository’s copy of the object identified
     * by {@code value}, returning the updated object. The patch specifies only the properties to change,
     * enabling efficient updates (e.g., translating to an SQL {@code UPDATE} for specific columns) and
     * supporting multi-version concurrency control (MVCC) semantics. The original {@code value} object
     * is not modified; the returned object may be a new instance or the same instance, depending on the
     * implementation.
     *
     * @param <I>   The specific type of {@link CatalogInfo} being updated.
     * @param value The catalog object to update (identifies the target); must not be null.
     * @param patch The patch containing property changes to apply; must not be null.
     * @return The updated catalog object reflecting the applied patch.
     * @throws NullPointerException if {@code value} or {@code patch} is null.
     * @throws IllegalArgumentException if {@code value} is not found in the repository.
     * @example Updating a layer’s title:
     *          <pre>
     *          LayerInfo layer = ...; // existing layer
     *          Patch patch = new Patch().with("title", "New Title");
     *          LayerInfo updated = repository.update(layer, patch);
     *          </pre>
     */
    <I extends T> I update(@NonNull I value, @NonNull Patch patch);

    /**
     * Releases any resources held by this repository.
     *
     * <p>This method should be called when the repository is no longer needed to ensure proper cleanup
     * (e.g., closing database connections). Implementations may use this to free resources explicitly.
     */
    void dispose();

    /**
     * Retrieves all objects managed by this repository without restrictions.
     *
     * <p>This default implementation uses {@link #findAll(Query)} with a query that includes all objects
     * of the repository’s content type ({@link #getContentType()}) and {@link Filter#INCLUDE}. The returned
     * stream must be closed after use to release resources.
     *
     * @return A {@link Stream} of all catalog objects managed by this repository; never null.
     * @example Listing all resources:
     *          <pre>
     *          try (Stream<ResourceInfo> resources = repository.findAll()) {
     *              resources.forEach(r -> System.out.println(r.getName()));
     *          }
     *          </pre>
     */
    default Stream<T> findAll() {
        return findAll(Query.all(getContentType()));
    }

    /**
     * Retrieves all objects matching the specified query criteria.
     *
     * <p>This method returns a stream of objects that satisfy the {@link Query}’s type, filter, sorting,
     * and pagination constraints. The stream must be closed after use to release resources, ideally via
     * a try-with-resources block.
     *
     * @param <U>   The specific type of {@link CatalogInfo} being queried.
     * @param query The query defining the criteria; must not be null.
     * @return A {@link Stream} of matching catalog objects; never null.
     * @throws NullPointerException if {@code query} is null.
     * @example Querying layers by name:
     *          <pre>
     *          Query<LayerInfo> query = Query.valueOf(LayerInfo.class, nameFilter);
     *          try (Stream<LayerInfo> layers = repository.findAll(query)) {
     *              layers.forEach(l -> System.out.println(l.getName()));
     *          }
     *          </pre>
     */
    <U extends T> Stream<U> findAll(Query<U> query);

    /**
     * Counts the number of objects of a specific type matching the given filter.
     *
     * @param <U>    The specific type of {@link CatalogInfo} to count.
     * @param of     The class of objects to count; must not be null.
     * @param filter The filter to apply; must not be null.
     * @return The number of matching objects.
     * @throws NullPointerException if {@code of} or {@code filter} is null.
     * @example Counting layers in a namespace:
     *          <pre>
     *          Filter nsFilter = ...; // filter by namespace
     *          long count = repository.count(LayerInfo.class, nsFilter);
     *          </pre>
     */
    <U extends T> long count(Class<U> of, Filter filter);

    /**
     * Retrieves a catalog object by its ID and type.
     *
     * <p>This method looks up an object with the specified ID, optionally constrained by the given type.
     * If {@code clazz} is null, it returns the first match of any subtype of {@code T}.
     *
     * @param <U>   The specific type of {@link CatalogInfo} to retrieve.
     * @param id    The unique identifier of the object; must not be null.
     * @param clazz The type of object to find, or null for any subtype of {@code T}.
     * @return An {@link Optional} containing the found object, or empty if not found.
     * @throws NullPointerException if {@code id} is null.
     * @example Finding a layer by ID:
     *          <pre>
     *          Optional<LayerInfo> layer = repository.findById("layer1", LayerInfo.class);
     *          layer.ifPresent(l -> System.out.println(l.getName()));
     *          </pre>
     */
    <U extends T> Optional<U> findById(@NonNull String id, @Nullable Class<U> clazz);

    /**
     * Retrieves the first catalog object matching the specified name and type.
     *
     * <p>This method searches for an object by name within the repository’s scope, returning the first
     * match of the given type, or an empty {@link Optional} if none is found.
     *
     * @param <U>   The specific type of {@link CatalogInfo} to retrieve.
     * @param name  The name of the object; must not be null.
     * @param clazz The type of object to find; must not be null.
     * @return An {@link Optional} containing the first matching object, or empty if not found.
     * @throws NullPointerException if {@code name} or {@code clazz} is null.
     * @example Finding a style by name:
     *          <pre>
     *          Optional<StyleInfo> style = repository.findFirstByName("point", StyleInfo.class);
     *          </pre>
     */
    <U extends T> Optional<U> findFirstByName(@NonNull String name, Class<U> clazz);

    /**
     * Checks if this repository supports sorting by the specified property.
     *
     * <p>This method indicates whether the underlying storage can sort results by the given property name,
     * useful for optimizing queries with {@link SortBy}.
     *
     * @param propertyName The name of the property to check; must not be null. Nested properties are specified with a {@code .}
     * separator, for example: {@code workspace.id}, {@code resource.store.namespace.prefix}, etc.
     * @return {@code true} if sorting by the property is supported; {@code false} otherwise.
     * @throws NullPointerException if {@code propertyName} is null.
     */
    public boolean canSortBy(@NonNull String propertyName);

    /**
     * Synchronizes this repository’s contents to another repository.
     *
     * <p>This method copies all objects from this repository to the target, potentially overwriting
     * existing entries in the target. It is intended for backup or migration purposes. Note: Future
     * enhancements could include a progress listener or cancellation mechanism.
     * <p>
     * Additionally, a {@code synchFrom} method might be more appropriate, as concrete implementations
     * would know better which optimizations to apply.
     *
     * @param target The target repository to sync to; must not be null.
     * @throws NullPointerException if {@code target} is null.
     * @example Syncing to another repository:
     *          <pre>
     *          CatalogInfoRepository<LayerInfo> source = ...;
     *          CatalogInfoRepository<LayerInfo> target = ...;
     *          source.syncTo(target);
     *          </pre>
     */
    void syncTo(@NonNull CatalogInfoRepository<T> target);

    /**
     * Specialized repository for managing {@link NamespaceInfo} objects.
     */
    public interface NamespaceRepository extends CatalogInfoRepository<NamespaceInfo> {
        /**
         * Sets the specified namespace as the default namespace.
         *
         * @param namespace The namespace to set as default; must not be null.
         * @throws NullPointerException if {@code namespace} is null.
         */
        void setDefaultNamespace(@NonNull NamespaceInfo namespace);

        /**
         * Removes the current default namespace designation, leaving no default.
         */
        void unsetDefaultNamespace();

        /**
         * Retrieves the current default namespace.
         *
         * @return An {@link Optional} containing the default {@link NamespaceInfo}, or empty if none is set.
         */
        Optional<NamespaceInfo> getDefaultNamespace();

        /**
         * Finds a namespace by its URI (e.g., "http://example.com").
         *
         * @param uri The URI to search for; must not be null.
         * @return An {@link Optional} containing the matching {@link NamespaceInfo}, or empty if not found.
         * @throws NullPointerException if {@code uri} is null.
         */
        Optional<NamespaceInfo> findOneByURI(@NonNull String uri);

        /**
         * Retrieves all namespaces matching the specified URI.
         *
         * @param uri The URI to search for; must not be null.
         * @return A {@link Stream} of matching {@link NamespaceInfo} objects; never null.
         * @throws NullPointerException if {@code uri} is null.
         */
        Stream<NamespaceInfo> findAllByURI(@NonNull String uri);
    }

    public interface WorkspaceRepository extends CatalogInfoRepository<WorkspaceInfo> {
        /**
         * Sets the specified workspace as the default workspace.
         *
         * @param workspace The workspace to set as default; must not be null.
         * @throws NullPointerException if {@code workspace} is null.
         */
        void setDefaultWorkspace(@NonNull WorkspaceInfo workspace);

        /**
         * Removes the current default workspace designation, leaving no default.
         */
        void unsetDefaultWorkspace();

        /**
         * Retrieves the current default workspace.
         *
         * @return An {@link Optional} containing the default {@link WorkspaceInfo}, or empty if none is set.
         */
        Optional<WorkspaceInfo> getDefaultWorkspace();
    }

    /**
     * Specialized repository for managing {@link StoreInfo} objects.
     */
    public interface StoreRepository extends CatalogInfoRepository<StoreInfo> {
        /**
         * Sets the specified data store as the default for the given workspace.
         *
         * @param workspace The workspace to configure; must not be null.
         * @param dataStore The data store to set as default; must not be null.
         * @throws NullPointerException if {@code workspace} or {@code dataStore} is null.
         */
        void setDefaultDataStore(@NonNull WorkspaceInfo workspace, @NonNull DataStoreInfo dataStore);

        /**
         * Removes the default data store designation for the specified workspace.
         *
         * @param workspace The workspace to modify; must not be null.
         * @throws NullPointerException if {@code workspace} is null.
         */
        void unsetDefaultDataStore(@NonNull WorkspaceInfo workspace);

        /**
         * Retrieves the default data store for the specified workspace.
         *
         * @param workspace The workspace to query; must not be null.
         * @return An {@link Optional} containing the default {@link DataStoreInfo}, or empty if none is set.
         * @throws NullPointerException if {@code workspace} is null.
         */
        Optional<DataStoreInfo> getDefaultDataStore(@NonNull WorkspaceInfo workspace);

        /**
         * Retrieves all default data stores across all workspaces.
         *
         * @return A {@link Stream} of default {@link DataStoreInfo} objects; never null.
         */
        Stream<DataStoreInfo> getDefaultDataStores();

        /**
         * Retrieves all stores of a specific type within a workspace.
         *
         * @param workspace The workspace to query; must not be null.
         * @param clazz     The type of {@link StoreInfo} to retrieve; must not be null.
         * @return A {@link Stream} of matching stores; never null.
         * @throws NullPointerException if {@code workspace} or {@code clazz} is null.
         */
        <T extends StoreInfo> Stream<T> findAllByWorkspace(@NonNull WorkspaceInfo workspace, @NonNull Class<T> clazz);

        /**
         * Retrieves all stores of a specific type.
         *
         * @param clazz The type of {@link StoreInfo} to retrieve; must not be null.
         * @return A {@link Stream} of matching stores; never null.
         * @throws NullPointerException if {@code clazz} is null.
         */
        <T extends StoreInfo> Stream<T> findAllByType(@NonNull Class<T> clazz);

        /**
         * Finds a store by name and workspace.
         *
         * @param name      The name of the store; must not be null.
         * @param workspace The workspace to query; must not be null.
         * @param clazz     The type of {@link StoreInfo} to retrieve; must not be null.
         * @return An {@link Optional} containing the matching store, or empty if not found.
         * @throws NullPointerException if {@code name}, {@code workspace}, or {@code clazz} is null.
         */
        <T extends StoreInfo> Optional<T> findByNameAndWorkspace(
                @NonNull String name, @NonNull WorkspaceInfo workspace, @NonNull Class<T> clazz);
    }

    /**
     * Specialized repository for managing {@link ResourceInfo} objects.
     */
    public interface ResourceRepository extends CatalogInfoRepository<ResourceInfo> {
        /**
         * Finds a resource by name and namespace.
         *
         * @param name      The name of the resource; must not be null.
         * @param namespace The namespace to query; must not be null.
         * @param clazz     The type of {@link ResourceInfo} to retrieve; must not be null.
         * @return An {@link Optional} containing the matching resource, or empty if not found.
         * @throws NullPointerException if {@code name}, {@code namespace}, or {@code clazz} is null.
         */
        <T extends ResourceInfo> Optional<T> findByNameAndNamespace(
                @NonNull String name, @NonNull NamespaceInfo namespace, @NonNull Class<T> clazz);

        /**
         * Retrieves all resources of a specific type.
         *
         * @param clazz The type of {@link ResourceInfo} to retrieve; must not be null.
         * @return A {@link Stream} of matching resources; never null.
         * @throws NullPointerException if {@code clazz} is null.
         */
        <T extends ResourceInfo> Stream<T> findAllByType(@NonNull Class<T> clazz);

        /**
         * Retrieves all resources within a namespace.
         *
         * @param ns    The namespace to query; must not be null.
         * @param clazz The type of {@link ResourceInfo} to retrieve; must not be null.
         * @return A {@link Stream} of matching resources; never null.
         * @throws NullPointerException if {@code ns} or {@code clazz} is null.
         */
        <T extends ResourceInfo> Stream<T> findAllByNamespace(@NonNull NamespaceInfo ns, @NonNull Class<T> clazz);

        /**
         * Finds a resource by store and name.
         *
         * @param store The store containing the resource; must not be null.
         * @param name  The name of the resource; must not be null.
         * @param clazz The type of {@link ResourceInfo} to retrieve; must not be null.
         * @return An {@link Optional} containing the matching resource, or empty if not found.
         * @throws NullPointerException if {@code store}, {@code name}, or {@code clazz} is null.
         */
        <T extends ResourceInfo> Optional<T> findByStoreAndName(
                @NonNull StoreInfo store, @NonNull String name, @NonNull Class<T> clazz);

        /**
         * Retrieves all resources associated with a store.
         *
         * @param store The store to query; must not be null.
         * @param clazz The type of {@link ResourceInfo} to retrieve; must not be null.
         * @return A {@link Stream} of matching resources; never null.
         * @throws NullPointerException if {@code store} or {@code clazz} is null.
         */
        <T extends ResourceInfo> Stream<T> findAllByStore(StoreInfo store, Class<T> clazz);
    }

    /**
     * Specialized repository for managing {@link LayerInfo} objects.
     */
    public interface LayerRepository extends CatalogInfoRepository<LayerInfo> {
        /**
         * Finds a layer by its possibly prefixed name (e.g., "namespace:layer").
         *
         * @param possiblyPrefixedName The name of the layer; must not be null.
         * @return An {@link Optional} containing the matching layer, or empty if not found.
         * @throws NullPointerException if {@code possiblyPrefixedName} is null.
         */
        Optional<LayerInfo> findOneByName(@NonNull String possiblyPrefixedName);

        /**
         * Retrieves all layers using the specified style as default or in their styles list.
         *
         * @param style The style to query; must not be null.
         * @return A {@link Stream} of matching layers; never null.
         * @throws NullPointerException if {@code style} is null.
         */
        Stream<LayerInfo> findAllByDefaultStyleOrStyles(@NonNull StyleInfo style);

        /**
         * Retrieves all layers associated with a resource.
         *
         * @param resource The resource to query; must not be null.
         * @return A {@link Stream} of matching layers; never null.
         * @throws NullPointerException if {@code resource} is null.
         */
        Stream<LayerInfo> findAllByResource(@NonNull ResourceInfo resource);
    }

    /**
     * Specialized repository for managing {@link LayerGroupInfo} objects.
     */
    public interface LayerGroupRepository extends CatalogInfoRepository<LayerGroupInfo> {
        /**
         * Finds a layer group by name with no associated workspace.
         *
         * @param name The name of the layer group; must not be null.
         * @return An {@link Optional} containing the matching layer group, or empty if not found.
         * @throws NullPointerException if {@code name} is null.
         */
        Optional<LayerGroupInfo> findByNameAndWorkspaceIsNull(@NonNull String name);

        /**
         * Finds a layer group by name and workspace.
         *
         * @param name      The name of the layer group; must not be null.
         * @param workspace The workspace to query; must not be null.
         * @return An {@link Optional} containing the matching layer group, or empty if not found.
         * @throws NullPointerException if {@code name} or {@code workspace} is null.
         */
        Optional<LayerGroupInfo> findByNameAndWorkspace(@NonNull String name, @NonNull WorkspaceInfo workspace);

        /**
         * Retrieves all layer groups with no associated workspace.
         *
         * @return A {@link Stream} of matching layer groups; never null.
         */
        Stream<LayerGroupInfo> findAllByWorkspaceIsNull();

        /**
         * Retrieves all layer groups within a workspace.
         *
         * @param workspace The workspace to query; must not be null.
         * @return A {@link Stream} of matching layer groups; never null.
         * @throws NullPointerException if {@code workspace} is null.
         */
        Stream<LayerGroupInfo> findAllByWorkspace(WorkspaceInfo workspace);
    }

    /**
     * Specialized repository for managing {@link StyleInfo} objects.
     */
    public interface StyleRepository extends CatalogInfoRepository<StyleInfo> {
        /**
         * Retrieves all styles with no associated workspace.
         *
         * @return A {@link Stream} of matching styles; never null.
         */
        Stream<StyleInfo> findAllByNullWorkspace();

        /**
         * Retrieves all styles within a workspace.
         *
         * @param ws The workspace to query; must not be null.
         * @return A {@link Stream} of matching styles; never null.
         * @throws NullPointerException if {@code ws} is null.
         */
        Stream<StyleInfo> findAllByWorkspace(@NonNull WorkspaceInfo ws);

        /**
         * Finds a style by name with no associated workspace.
         *
         * @param name The name of the style; must not be null.
         * @return An {@link Optional} containing the matching style, or empty if not found.
         * @throws NullPointerException if {@code name} is null.
         */
        Optional<StyleInfo> findByNameAndWordkspaceNull(@NonNull String name);

        /**
         * Finds a style by name and workspace.
         *
         * @param name      The name of the style; must not be null.
         * @param workspace The workspace to query; must not be null.
         * @return An {@link Optional} containing the matching style, or empty if not found.
         * @throws NullPointerException if {@code name} or {@code workspace} is null.
         */
        Optional<StyleInfo> findByNameAndWorkspace(@NonNull String name, @NonNull WorkspaceInfo workspace);
    }

    /**
     * Specialized repository for managing {@link MapInfo} objects.
     *
     * <p>This interface currently provides no additional methods beyond the base repository operations.
     */
    public interface MapRepository extends CatalogInfoRepository<MapInfo> {}
}
