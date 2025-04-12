/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin;

import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogCapabilities;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LockingCatalogFacade;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ProxyUtils;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.MapRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.ResourceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;
import org.geoserver.function.IsInstanceOf;
import org.geotools.api.filter.And;
import org.geotools.api.filter.BinaryComparisonOperator;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.Or;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.springframework.util.Assert;

/**
 * A concrete implementation of {@link RepositoryCatalogFacade} that manages catalog data using
 * {@link CatalogInfoRepository} instances.
 *
 * <p>This class provides a repository-backed facade for GeoServer Cloud’s catalog, implementing both
 * {@link ExtendedCatalogFacade} and {@link CatalogInfoRepositoryHolder} interfaces. It delegates all
 * catalog operations (e.g., adding, removing, querying) to type-specific repositories (e.g.,
 * {@link WorkspaceRepository}, {@link LayerRepository}) managed by a {@link CatalogInfoRepositoryHolderImpl}.
 * It supports advanced features like stream-based querying ({@link #query(Query)}), patch-based updates
 * ({@link #update(CatalogInfo, Patch)}), and synchronization with other facades ({@link #syncTo(CatalogFacade)}).
 *
 * <p>Key features:
 * <ul>
 *   <li><strong>Repository-Driven:</strong> Uses specialized repositories for persistence and retrieval
 *       of {@link CatalogInfo} objects.</li>
 *   <li><strong>Type Safety:</strong> Ensures consistent handling of catalog info types through generics.</li>
 *   <li><strong>Query Optimization:</strong> Implements efficient querying for {@link PublishedInfo} with
 *       merge-sorted streams.</li>
 *   <li><strong>Capabilities:</strong> Reports catalog capabilities via {@link #getCatalogCapabilities()}.</li>
 * </ul>
 *
 * <p>This implementation is designed to work within GeoServer Cloud’s catalog system, providing a robust
 * and flexible facade that integrates repository-based data access with catalog operations.
 *
 * @since 1.0
 * @see RepositoryCatalogFacade
 * @see ExtendedCatalogFacade
 * @see CatalogInfoRepositoryHolder
 * @see CatalogInfoRepository
 */
public class RepositoryCatalogFacadeImpl implements RepositoryCatalogFacade, CatalogInfoRepositoryHolder {

    protected final CatalogInfoRepositoryHolderImpl repositories;

    protected Catalog catalog;

    protected final CatalogCapabilities capabilities = new CatalogCapabilities();

    /**
     * Constructs a new repository-backed catalog facade with no associated catalog.
     *
     * <p>Initializes the internal {@link CatalogInfoRepositoryHolderImpl} for managing repositories.
     * Use {@link #setCatalog(Catalog)} to associate a catalog instance after construction.
     */
    public RepositoryCatalogFacadeImpl() {
        repositories = new CatalogInfoRepositoryHolderImpl();
    }

    /**
     * Constructs a new repository-backed catalog facade with the specified catalog.
     *
     * <p>Initializes the facade and sets the provided catalog, which is used as the context for all
     * catalog operations.
     *
     * @param catalog The {@link Catalog} instance to associate with this facade; may be null.
     * @see #setCatalog(Catalog)
     */
    public RepositoryCatalogFacadeImpl(Catalog catalog) {
        this();
        setCatalog(catalog);
    }

    /**
     * Returns the catalog capabilities supported by this facade.
     *
     * <p>The capabilities reflect the features available through this repository-backed implementation,
     * such as support for isolated workspaces or specific query operations.
     *
     * @return The {@link CatalogCapabilities} instance; never null.
     */
    @Override
    public CatalogCapabilities getCatalogCapabilities() {
        return capabilities;
    }

    /**
     * Sets the catalog instance associated with this facade.
     *
     * <p>The catalog provides the context for all operations (e.g., default workspaces, namespaces).
     * Setting it to null effectively clears the association.
     *
     * @param catalog The {@link Catalog} to set; may be null.
     */
    @Override
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Retrieves the catalog instance associated with this facade.
     *
     * @return The current {@link Catalog}, or null if none is set.
     */
    @Override
    public Catalog getCatalog() {
        return catalog;
    }

    /**
     * Resolves any internal state or dependencies of the facade.
     *
     * <p>This default implementation is a no-op. Subclasses may override it to perform initialization
     * or consistency checks as needed.
     */
    @Override
    public void resolve() {
        // no-op, override as appropriate
    }

    /**
     * Adds a catalog object to the specified repository and retrieves the persisted instance.
     *
     * <p>This helper method ensures the object is not a proxy, adds it to the repository, and returns
     * the persisted version by looking it up by ID. It’s used internally for type-specific add operations.
     *
     * @param <I>        The type of {@link CatalogInfo} to add.
     * @param info       The catalog object to add; must not be null and not a proxy.
     * @param type       The class of the catalog object; must not be null.
     * @param repository The repository to add the object to; must not be null.
     * @return The persisted {@link CatalogInfo} object, or null if not found after addition.
     * @throws NullPointerException if {@code info}, {@code type}, or {@code repository} is null.
     * @throws IllegalArgumentException if {@code info} is a proxy or lacks an ID.
     */
    protected <I extends CatalogInfo> I add(I info, Class<I> type, CatalogInfoRepository<I> repository) {
        checkNotAProxy(info);
        Objects.requireNonNull(info.getId(), "Object id not provided");
        repository.add(info);
        return repository.findById(info.getId(), type).orElse(null);
    }

    //
    // Stores
    //
    /**
     * Adds a store to the catalog.
     *
     * <p>Persists the store via the {@link StoreRepository} and returns the added instance, which may
     * include updates like a generated ID.
     *
     * @param store The {@link StoreInfo} to add; must not be null.
     * @return The persisted {@link StoreInfo}.
     * @throws NullPointerException if {@code store} is null.
     * @throws IllegalArgumentException if {@code store} is a proxy or lacks an ID.
     * @example Adding a data store:
     *          <pre>
     *          DataStoreInfo store = new DataStoreInfoImpl(catalog);
     *          store.setId("ds1");
     *          facade.add(store);
     *          </pre>
     */
    @Override
    public StoreInfo add(StoreInfo store) {
        return add(store, StoreInfo.class, getStoreRepository());
    }

    /**
     * Removes a store from the catalog.
     *
     * <p>Delegates to the {@link StoreRepository} to delete the store and associated resources.
     *
     * @param store The {@link StoreInfo} to remove; must not be null.
     * @throws NullPointerException if {@code store} is null.
     */
    @Override
    public void remove(StoreInfo store) {
        getStoreRepository().remove(store);
    }

    /**
     * Retrieves a store by its ID.
     *
     * <p>Queries the {@link StoreRepository} for a store matching the given ID and type.
     *
     * @param <T>   The specific type of {@link StoreInfo} to retrieve (e.g., {@link DataStoreInfo}).
     * @param id    The unique identifier of the store; must not be null.
     * @param clazz The class of the store to retrieve; must not be null.
     * @return The matching {@link StoreInfo} if found, or null if not.
     * @throws NullPointerException if {@code id} or {@code clazz} is null.
     */
    @Override
    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return getStoreRepository().findById(id, clazz).orElse(null);
    }

    /**
     * Retrieves a store by name and workspace.
     *
     * <p>If the workspace is {@link CatalogFacade#ANY_WORKSPACE} or null, searches for the first store
     * matching the name across all workspaces. Otherwise, queries the {@link StoreRepository} for a store
     * with the specified name within the given workspace.
     *
     * @param <T>       The specific type of {@link StoreInfo} to retrieve.
     * @param workspace The workspace containing the store, or {@link CatalogFacade#ANY_WORKSPACE}; may be null.
     * @param name      The name of the store; must not be null.
     * @param clazz     The class of the store to retrieve; must not be null.
     * @return The matching {@link StoreInfo} if found, or null if not.
     * @throws NullPointerException if {@code name} or {@code clazz} is null.
     */
    @Override
    public <T extends StoreInfo> T getStoreByName(WorkspaceInfo workspace, String name, Class<T> clazz) {
        Optional<T> result;
        if (workspace == ANY_WORKSPACE || workspace == null) {
            result = getStoreRepository().findFirstByName(name, clazz);
        } else {
            result = getStoreRepository().findByNameAndWorkspace(name, workspace, clazz);
        }
        return result.orElse(null);
    }

    /**
     * Retrieves all stores within a workspace.
     *
     * <p>If the workspace is null, defaults to the current default workspace. Delegates to the
     * {@link StoreRepository} to fetch stores of the specified type within the workspace.
     *
     * @param <T>       The specific type of {@link StoreInfo} to retrieve.
     * @param workspace The workspace containing the stores; may be null to use the default.
     * @param clazz     The class of the stores to retrieve; must not be null.
     * @return A list of matching {@link StoreInfo} objects.
     * @throws NullPointerException if {@code clazz} is null.
     */
    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(WorkspaceInfo workspace, Class<T> clazz) {
        final WorkspaceInfo ws = (workspace == null) ? getDefaultWorkspace() : workspace;
        return toList(() -> getStoreRepository().findAllByWorkspace(ws, clazz));
    }

    /**
     * Retrieves all stores of a specific type.
     *
     * <p>Queries the {@link StoreRepository} for all stores matching the given class.
     *
     * @param <T>   The specific type of {@link StoreInfo} to retrieve.
     * @param clazz The class of the stores to retrieve; must not be null.
     * @return A list of matching {@link StoreInfo} objects.
     * @throws NullPointerException if {@code clazz} is null.
     */
    @Override
    public <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return toList(() -> getStoreRepository().findAllByType(clazz));
    }

    /**
     * Retrieves the default data store for a workspace.
     *
     * <p>Queries the {@link StoreRepository} for the default {@link DataStoreInfo} associated with the
     * specified workspace.
     *
     * @param workspace The workspace to query; may be null.
     * @return The default {@link DataStoreInfo} if set, or null if not found.
     */
    @Override
    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return getStoreRepository().getDefaultDataStore(workspace).orElse(null);
    }

    /**
     * Sets or unsets the default data store for a workspace.
     *
     * <p>If {@code store} is null, unsets the default data store for the workspace. Otherwise, sets the
     * specified store as the default, ensuring it belongs to the given workspace.
     *
     * @param workspace The workspace to configure; must not be null.
     * @param store     The {@link DataStoreInfo} to set as default, or null to unset.
     * @throws NullPointerException if {@code workspace} is null.
     * @throws IllegalArgumentException if {@code store} is non-null and its workspace does not match {@code workspace}.
     */
    @Override
    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
        if (store != null) {
            Objects.requireNonNull(store.getWorkspace(), "Store workspace must not be null");
            Assert.isTrue(workspace.getId().equals(store.getWorkspace().getId()), "Store workspace mismatch");
        }
        if (store == null) {
            getStoreRepository().unsetDefaultDataStore(workspace);
        } else {
            getStoreRepository().setDefaultDataStore(workspace, store);
        }
    }

    //
    // Resources
    //
    /**
     * Adds a resource to the catalog.
     *
     * <p>Persists the resource via the {@link ResourceRepository} and returns the added instance.
     *
     * @param resource The {@link ResourceInfo} to add; must not be null.
     * @return The persisted {@link ResourceInfo}.
     * @throws NullPointerException if {@code resource} is null.
     * @throws IllegalArgumentException if {@code resource} is a proxy or lacks an ID.
     */
    @Override
    public ResourceInfo add(ResourceInfo resource) {
        return add(resource, ResourceInfo.class, getResourceRepository());
    }

    /**
     * Removes a resource from the catalog.
     *
     * <p>Delegates to the {@link ResourceRepository} to delete the resource and associated data.
     *
     * @param resource The {@link ResourceInfo} to remove; must not be null.
     * @throws NullPointerException if {@code resource} is null.
     */
    @Override
    public void remove(ResourceInfo resource) {
        getResourceRepository().remove(resource);
    }

    /**
     * Retrieves a resource by its ID.
     *
     * <p>Queries the {@link ResourceRepository} for a resource matching the given ID and type.
     *
     * @param <T>   The specific type of {@link ResourceInfo} to retrieve (e.g., {@link org.geoserver.catalog.FeatureTypeInfo}).
     * @param id    The unique identifier of the resource; must not be null.
     * @param clazz The class of the resource to retrieve; must not be null.
     * @return The matching {@link ResourceInfo} if found, or null if not.
     * @throws NullPointerException if {@code id} or {@code clazz} is null.
     */
    @Override
    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return getResourceRepository().findById(id, clazz).orElse(null);
    }

    /**
     * Retrieves a resource by name and namespace.
     *
     * <p>If the namespace is {@link CatalogFacade#ANY_NAMESPACE}, searches for the first resource matching
     * the name across all namespaces. Otherwise, queries the {@link ResourceRepository} for a resource
     * with the specified name within the given namespace.
     *
     * @param <T>       The specific type of {@link ResourceInfo} to retrieve.
     * @param namespace The namespace containing the resource, or {@link CatalogFacade#ANY_NAMESPACE}; may be null (returns null).
     * @param name      The name of the resource; must not be null.
     * @param clazz     The class of the resource to retrieve; must not be null.
     * @return The matching {@link ResourceInfo} if found, or null if not.
     * @throws NullPointerException if {@code name} or {@code clazz} is null.
     */
    @Override
    public <T extends ResourceInfo> T getResourceByName(NamespaceInfo namespace, String name, Class<T> clazz) {
        if (namespace == null) {
            return null;
        }
        Optional<T> result;
        if (namespace == ANY_NAMESPACE) {
            result = getResourceRepository().findFirstByName(name, clazz);
        } else {
            result = getResourceRepository().findByNameAndNamespace(name, namespace, clazz);
        }
        return result.orElse(null);
    }

    /**
     * Retrieves all resources of a specific type.
     *
     * <p>Queries the {@link ResourceRepository} for all resources matching the given class.
     *
     * @param <T>   The specific type of {@link ResourceInfo} to retrieve.
     * @param clazz The class of the resources to retrieve; must not be null.
     * @return A list of matching {@link ResourceInfo} objects.
     * @throws NullPointerException if {@code clazz} is null.
     */
    @Override
    public <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return toList(() -> getResourceRepository().findAllByType(clazz));
    }

    /**
     * Retrieves all resources within a namespace.
     *
     * <p>If the namespace is null, defaults to the current default namespace. Queries the
     * {@link ResourceRepository} for resources of the specified type within the namespace.
     *
     * @param <T>       The specific type of {@link ResourceInfo} to retrieve.
     * @param namespace The namespace containing the resources; may be null to use the default.
     * @param clazz     The class of the resources to retrieve; must not be null.
     * @return A list of matching {@link ResourceInfo} objects.
     * @throws NullPointerException if {@code clazz} is null.
     */
    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(NamespaceInfo namespace, Class<T> clazz) {
        NamespaceInfo ns = namespace == null ? getDefaultNamespace() : namespace;
        return toList(() -> getResourceRepository().findAllByNamespace(ns, clazz));
    }

    /**
     * Retrieves a resource by store and name, ensuring namespace consistency.
     *
     * <p>Queries the {@link ResourceRepository} for a resource with the specified name in the store’s
     * namespace, falling back to a store-specific search if namespace lookup fails (e.g., in test scenarios).
     * Verifies the store matches the resource’s store to ensure consistency.
     *
     * @param <T>   The specific type of {@link ResourceInfo} to retrieve.
     * @param store The store containing the resource; must not be null.
     * @param name  The name of the resource; must not be null.
     * @param clazz The class of the resource to retrieve; must not be null.
     * @return The matching {@link ResourceInfo} if found and consistent with the store, or null if not.
     * @throws NullPointerException if {@code store}, {@code name}, or {@code clazz} is null.
     */
    @Override
    public <T extends ResourceInfo> T getResourceByStore(StoreInfo store, String name, Class<T> clazz) {
        T resource = null;

        if (store.getWorkspace() != null && store.getWorkspace().getName() != null) {
            NamespaceInfo ns = getNamespaceByPrefix(store.getWorkspace().getName());
            if (ns != null) {
                ResourceRepository resourceRepository = getResourceRepository();
                resource = resourceRepository
                        .findByNameAndNamespace(name, ns, clazz)
                        .orElse(null);
                if (resource != null
                        && !(store.getId().equals(resource.getStore().getId()))) {
                    resource = null;
                }
            }
        } else {
            resource = getResourceRepository()
                    .findByStoreAndName(store, name, clazz)
                    .orElse(null);
        }
        return resource;
    }

    /**
     * Retrieves all resources associated with a store.
     *
     * <p>Queries the {@link ResourceRepository} for resources of the specified type linked to the store.
     *
     * @param <T>   The specific type of {@link ResourceInfo} to retrieve.
     * @param store The store containing the resources; must not be null.
     * @param clazz The class of the resources to retrieve; must not be null.
     * @return A list of matching {@link ResourceInfo} objects.
     * @throws NullPointerException if {@code store} or {@code clazz} is null.
     */
    @Override
    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz) {
        return toList(() -> getResourceRepository().findAllByStore(store, clazz));
    }

    //
    // Layers
    //
    /**
     * Adds a layer to the catalog.
     *
     * <p>Persists the layer via the {@link LayerRepository} and returns the added instance.
     *
     * @param layer The {@link LayerInfo} to add; must not be null.
     * @return The persisted {@link LayerInfo}.
     * @throws NullPointerException if {@code layer} is null.
     * @throws IllegalArgumentException if {@code layer} is a proxy or lacks an ID.
     */
    @Override
    public LayerInfo add(LayerInfo layer) {
        return add(layer, LayerInfo.class, getLayerRepository());
    }

    /**
     * Removes a layer from the catalog.
     *
     * <p>Delegates to the {@link LayerRepository} to delete the layer and associated data.
     *
     * @param layer The {@link LayerInfo} to remove; must not be null.
     * @throws NullPointerException if {@code layer} is null.
     */
    @Override
    public void remove(LayerInfo layer) {
        getLayerRepository().remove(layer);
    }

    /**
     * Retrieves a layer by its ID.
     *
     * <p>Queries the {@link LayerRepository} for a layer matching the given ID.
     *
     * @param id The unique identifier of the layer; must not be null.
     * @return The matching {@link LayerInfo} if found, or null if not.
     * @throws NullPointerException if {@code id} is null.
     */
    @Override
    public LayerInfo getLayer(String id) {
        return getLayerRepository().findById(id, LayerInfo.class).orElse(null);
    }

    /**
     * Retrieves a layer by its name.
     *
     * <p>Queries the {@link LayerRepository} for a layer matching the possibly prefixed name (e.g.,
     * "namespace:layer").
     *
     * @param name The name of the layer (possibly prefixed); must not be null.
     * @return The matching {@link LayerInfo} if found, or null if not.
     * @throws NullPointerException if {@code name} is null.
     */
    @Override
    public LayerInfo getLayerByName(String name) {
        return getLayerRepository().findOneByName(name).orElse(null);
    }

    /**
     * Retrieves all layers associated with a resource.
     *
     * <p>Queries the {@link LayerRepository} for layers linked to the specified resource.
     *
     * @param resource The {@link ResourceInfo} linked to the layers; must not be null.
     * @return A list of matching {@link LayerInfo} objects.
     * @throws NullPointerException if {@code resource} is null.
     */
    @Override
    public List<LayerInfo> getLayers(ResourceInfo resource) {
        return toList(() -> getLayerRepository().findAllByResource(resource));
    }

    /**
     * Retrieves all layers using a specific style.
     *
     * <p>Queries the {@link LayerRepository} for layers where the style is either the default or included
     * in their styles list.
     *
     * @param style The {@link StyleInfo} linked to the layers; must not be null.
     * @return A list of matching {@link LayerInfo} objects.
     * @throws NullPointerException if {@code style} is null.
     */
    @Override
    public List<LayerInfo> getLayers(StyleInfo style) {
        return toList(() -> getLayerRepository().findAllByDefaultStyleOrStyles(style));
    }

    /**
     * Retrieves all layers in the catalog.
     *
     * <p>Queries the {@link LayerRepository} for all layers without restrictions.
     *
     * @return A list of all {@link LayerInfo} objects.
     */
    @Override
    public List<LayerInfo> getLayers() {
        return toList(getLayerRepository()::findAll);
    }

    //
    // Maps
    //
    /**
     * Adds a map to the catalog.
     *
     * <p>Persists the map via the {@link MapRepository} and returns the added instance.
     *
     * @param map The {@link MapInfo} to add; must not be null.
     * @return The persisted {@link MapInfo}.
     * @throws NullPointerException if {@code map} is null.
     * @throws IllegalArgumentException if {@code map} is a proxy or lacks an ID.
     */
    @Override
    public MapInfo add(MapInfo map) {
        return add(map, MapInfo.class, getMapRepository());
    }

    /**
     * Removes a map from the catalog.
     *
     * <p>Delegates to the {@link MapRepository} to delete the map and associated data.
     *
     * @param map The {@link MapInfo} to remove; must not be null.
     * @throws NullPointerException if {@code map} is null.
     */
    @Override
    public void remove(MapInfo map) {
        getMapRepository().remove(map);
    }

    /**
     * Retrieves a map by its ID.
     *
     * <p>Queries the {@link MapRepository} for a map matching the given ID.
     *
     * @param id The unique identifier of the map; must not be null.
     * @return The matching {@link MapInfo} if found, or null if not.
     * @throws NullPointerException if {@code id} is null.
     */
    @Override
    public MapInfo getMap(String id) {
        return getMapRepository().findById(id, MapInfo.class).orElse(null);
    }

    /**
     * Retrieves a map by its name.
     *
     * <p>Queries the {@link MapRepository} for the first map matching the given name.
     *
     * @param name The name of the map; must not be null.
     * @return The matching {@link MapInfo} if found, or null if not.
     * @throws NullPointerException if {@code name} is null.
     */
    @Override
    public MapInfo getMapByName(String name) {
        return getMapRepository().findFirstByName(name, MapInfo.class).orElse(null);
    }

    /**
     * Retrieves all maps in the catalog.
     *
     * <p>Queries the {@link MapRepository} for all maps without restrictions.
     *
     * @return A list of all {@link MapInfo} objects.
     */
    @Override
    public List<MapInfo> getMaps() {
        return toList(getMapRepository()::findAll);
    }

    //
    // Layer Groups
    //
    /**
     * Adds a layer group to the catalog.
     *
     * <p>Persists the layer group via the {@link LayerGroupRepository} and returns the added instance.
     *
     * @param layerGroup The {@link LayerGroupInfo} to add; must not be null.
     * @return The persisted {@link LayerGroupInfo}.
     * @throws NullPointerException if {@code layerGroup} is null.
     * @throws IllegalArgumentException if {@code layerGroup} is a proxy or lacks an ID.
     */
    @Override
    public LayerGroupInfo add(LayerGroupInfo layerGroup) {
        return add(layerGroup, LayerGroupInfo.class, getLayerGroupRepository());
    }

    /**
     * Removes a layer group from the catalog.
     *
     * <p>Delegates to the {@link LayerGroupRepository} to delete the layer group and associated data.
     *
     * @param layerGroup The {@link LayerGroupInfo} to remove; must not be null.
     * @throws NullPointerException if {@code layerGroup} is null.
     */
    @Override
    public void remove(LayerGroupInfo layerGroup) {
        getLayerGroupRepository().remove(layerGroup);
    }

    /**
     * Retrieves all layer groups in the catalog.
     *
     * <p>Queries the {@link LayerGroupRepository} for all layer groups without restrictions.
     *
     * @return A list of all {@link LayerGroupInfo} objects.
     */
    @Override
    public List<LayerGroupInfo> getLayerGroups() {
        return toList(getLayerGroupRepository()::findAll);
    }

    /**
     * Retrieves all layer groups within a workspace.
     *
     * <p>If the workspace is null, defaults to the current default workspace. Handles special cases:
     * {@link CatalogFacade#NO_WORKSPACE} retrieves global layer groups (no workspace), while others fetch
     * workspace-specific groups via the {@link LayerGroupRepository}.
     *
     * @param workspace The workspace containing the layer groups; may be null to use the default.
     * @return A list of matching {@link LayerGroupInfo} objects.
     */
    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        WorkspaceInfo ws = (workspace == null) ? getDefaultWorkspace() : workspace;
        Stream<LayerGroupInfo> matches = (workspace == NO_WORKSPACE)
                ? getLayerGroupRepository().findAllByWorkspaceIsNull()
                : getLayerGroupRepository().findAllByWorkspace(ws);
        return toList(() -> matches);
    }

    /**
     * Retrieves a layer group by its ID.
     *
     * <p>Queries the {@link LayerGroupRepository} for a layer group matching the given ID.
     *
     * @param id The unique identifier of the layer group; must not be null.
     * @return The matching {@link LayerGroupInfo} if found, or null if not.
     * @throws NullPointerException if {@code id} is null.
     */
    @Override
    public LayerGroupInfo getLayerGroup(String id) {
        return getLayerGroupRepository().findById(id, LayerGroupInfo.class).orElse(null);
    }

    /**
     * Retrieves a layer group by its name, assuming no workspace context.
     *
     * <p>Delegates to {@link #getLayerGroupByName(WorkspaceInfo, String)} with
     * {@link CatalogFacade#NO_WORKSPACE}.
     *
     * @param name The name of the layer group; must not be null.
     * @return The matching {@link LayerGroupInfo} if found, or null if not.
     * @throws NullPointerException if {@code name} is null.
     */
    @Override
    public LayerGroupInfo getLayerGroupByName(String name) {
        return getLayerGroupByName(NO_WORKSPACE, name);
    }

    /**
     * Retrieves a layer group by name and workspace.
     *
     * <p>Handles special cases: {@link CatalogFacade#NO_WORKSPACE} queries global layer groups,
     * {@link CatalogFacade#ANY_WORKSPACE} searches across all workspaces, and specific workspaces restrict
     * the search to that context.
     *
     * @param workspace The workspace containing the layer group, or special values; must not be null.
     * @param name      The name of the layer group; must not be null.
     * @return The matching {@link LayerGroupInfo} if found, or null if not.
     * @throws NullPointerException if {@code workspace} or {@code name} is null.
     */
    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        Objects.requireNonNull(workspace, "workspace");
        Optional<LayerGroupInfo> match;
        if (workspace == NO_WORKSPACE) {
            match = getLayerGroupRepository().findByNameAndWorkspaceIsNull(name);
        } else if (ANY_WORKSPACE == workspace) {
            match = getLayerGroupRepository().findFirstByName(name, LayerGroupInfo.class);
        } else {
            match = getLayerGroupRepository().findByNameAndWorkspace(name, workspace);
        }
        return match.orElse(null);
    }

    //
    // Namespaces
    //
    /**
     * Adds a namespace to the catalog.
     *
     * <p>Persists the namespace via the {@link NamespaceRepository} and returns the added instance.
     *
     * @param namespace The {@link NamespaceInfo} to add; must not be null.
     * @return The persisted {@link NamespaceInfo}.
     * @throws NullPointerException if {@code namespace} is null.
     * @throws IllegalArgumentException if {@code namespace} is a proxy or lacks an ID.
     */
    @Override
    public NamespaceInfo add(NamespaceInfo namespace) {
        return add(namespace, NamespaceInfo.class, getNamespaceRepository());
    }

    /**
     * Removes a namespace from the catalog.
     *
     * <p>If the namespace is the default, unsets it before removal. Delegates to the
     * {@link NamespaceRepository} to delete the namespace.
     *
     * @param namespace The {@link NamespaceInfo} to remove; must not be null.
     * @throws NullPointerException if {@code namespace} is null.
     */
    @Override
    public void remove(NamespaceInfo namespace) {
        NamespaceInfo defaultNamespace = getDefaultNamespace();
        if (defaultNamespace != null && namespace.getId().equals(defaultNamespace.getId())) {
            setDefaultNamespace(null);
        }
        getNamespaceRepository().remove(namespace);
    }

    /**
     * Retrieves the default namespace.
     *
     * <p>Queries the {@link NamespaceRepository} for the current default namespace.
     *
     * @return The default {@link NamespaceInfo} if set, or null if not.
     */
    @Override
    public NamespaceInfo getDefaultNamespace() {
        return getNamespaceRepository().getDefaultNamespace().orElse(null);
    }

    /**
     * Sets or unsets the default namespace.
     *
     * <p>If {@code defaultNamespace} is null, unsets the default namespace. Otherwise, sets the specified
     * namespace as the default via the {@link NamespaceRepository}.
     *
     * @param defaultNamespace The {@link NamespaceInfo} to set as default, or null to unset.
     */
    @Override
    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        if (defaultNamespace == null) {
            getNamespaceRepository().unsetDefaultNamespace();
        } else {
            getNamespaceRepository().setDefaultNamespace(defaultNamespace);
        }
    }

    /**
     * Retrieves a namespace by its ID.
     *
     * <p>Queries the {@link NamespaceRepository} for a namespace matching the given ID.
     *
     * @param id The unique identifier of the namespace; must not be null.
     * @return The matching {@link NamespaceInfo} if found, or null if not.
     * @throws NullPointerException if {@code id} is null.
     */
    @Override
    public NamespaceInfo getNamespace(String id) {
        return getNamespaceRepository().findById(id, NamespaceInfo.class).orElse(null);
    }

    /**
     * Retrieves a namespace by its prefix.
     *
     * <p>Queries the {@link NamespaceRepository} for the first namespace matching the given prefix.
     *
     * @param prefix The prefix of the namespace; must not be null.
     * @return The matching {@link NamespaceInfo} if found, or null if not.
     * @throws NullPointerException if {@code prefix} is null.
     */
    @Override
    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        return getNamespaceRepository()
                .findFirstByName(prefix, NamespaceInfo.class)
                .orElse(null);
    }

    /**
     * Retrieves a namespace by its URI.
     *
     * <p>Queries the {@link NamespaceRepository} for a namespace matching the given URI.
     *
     * @param uri The URI of the namespace; must not be null.
     * @return The matching {@link NamespaceInfo} if found, or null if not.
     * @throws NullPointerException if {@code uri} is null.
     */
    @Override
    public NamespaceInfo getNamespaceByURI(String uri) {
        return getNamespaceRepository().findOneByURI(uri).orElse(null);
    }

    /**
     * Retrieves all namespaces matching a URI.
     *
     * <p>Queries the {@link NamespaceRepository} for namespaces with the specified URI.
     *
     * @param uri The URI to match; must not be null.
     * @return A list of matching {@link NamespaceInfo} objects.
     * @throws NullPointerException if {@code uri} is null.
     */
    @Override
    public List<NamespaceInfo> getNamespacesByURI(String uri) {
        return toList(() -> getNamespaceRepository().findAllByURI(uri));
    }

    /**
     * Retrieves all namespaces in the catalog.
     *
     * <p>Queries the {@link NamespaceRepository} for all namespaces without restrictions.
     *
     * @return A list of all {@link NamespaceInfo} objects.
     */
    @Override
    public List<NamespaceInfo> getNamespaces() {
        return toList(getNamespaceRepository()::findAll);
    }

    //
    // Workspaces
    //
    /**
     * Adds a workspace to the catalog.
     *
     * <p>Persists the workspace via the {@link WorkspaceRepository} and returns the added instance.
     *
     * @param workspace The {@link WorkspaceInfo} to add; must not be null.
     * @return The persisted {@link WorkspaceInfo}.
     * @throws NullPointerException if {@code workspace} is null.
     * @throws IllegalArgumentException if {@code workspace} is a proxy or lacks an ID.
     */
    @Override
    public WorkspaceInfo add(WorkspaceInfo workspace) {
        return add(workspace, WorkspaceInfo.class, getWorkspaceRepository());
    }

    /**
     * Removes a workspace from the catalog.
     *
     * <p>If the workspace is the default, unsets it before removal. Delegates to the
     * {@link WorkspaceRepository} to delete the workspace.
     *
     * @param workspace The {@link WorkspaceInfo} to remove; must not be null.
     * @throws NullPointerException if {@code workspace} is null.
     */
    @Override
    public void remove(WorkspaceInfo workspace) {
        WorkspaceInfo defaultWorkspace = getDefaultWorkspace();
        if (defaultWorkspace != null && workspace.getId().equals(defaultWorkspace.getId())) {
            getWorkspaceRepository().unsetDefaultWorkspace();
        }
        getWorkspaceRepository().remove(workspace);
    }

    /**
     * Retrieves the default workspace.
     *
     * <p>Queries the {@link WorkspaceRepository} for the current default workspace.
     *
     * @return The default {@link WorkspaceInfo} if set, or null if not.
     */
    @Override
    public WorkspaceInfo getDefaultWorkspace() {
        return getWorkspaceRepository().getDefaultWorkspace().orElse(null);
    }

    /**
     * Sets or unsets the default workspace.
     *
     * <p>If {@code workspace} is null, unsets the default workspace. Otherwise, sets the specified
     * workspace as the default via the {@link WorkspaceRepository}.
     *
     * @param workspace The {@link WorkspaceInfo} to set as default, or null to unset.
     */
    @Override
    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        if (workspace == null) {
            getWorkspaceRepository().unsetDefaultWorkspace();
        } else {
            getWorkspaceRepository().setDefaultWorkspace(workspace);
        }
    }

    /**
     * Retrieves all workspaces in the catalog.
     *
     * <p>Queries the {@link WorkspaceRepository} for all workspaces without restrictions.
     *
     * @return A list of all {@link WorkspaceInfo} objects.
     */
    @Override
    public List<WorkspaceInfo> getWorkspaces() {
        return toList(getWorkspaceRepository()::findAll);
    }

    /**
     * Retrieves a workspace by its ID.
     *
     * <p>Queries the {@link WorkspaceRepository} for a workspace matching the given ID.
     *
     * @param id The unique identifier of the workspace; must not be null.
     * @return The matching {@link WorkspaceInfo} if found, or null if not.
     * @throws NullPointerException if {@code id} is null.
     */
    @Override
    public WorkspaceInfo getWorkspace(String id) {
        return getWorkspaceRepository().findById(id, WorkspaceInfo.class).orElse(null);
    }

    /**
     * Retrieves a workspace by its name.
     *
     * <p>Queries the {@link WorkspaceRepository} for the first workspace matching the given name.
     *
     * @param name The name of the workspace; must not be null.
     * @return The matching {@link WorkspaceInfo} if found, or null if not.
     * @throws NullPointerException if {@code name} is null.
     */
    @Override
    public WorkspaceInfo getWorkspaceByName(String name) {
        return getWorkspaceRepository()
                .findFirstByName(name, WorkspaceInfo.class)
                .orElse(null);
    }

    //
    // Styles
    //
    /**
     * Adds a style to the catalog.
     *
     * <p>Persists the style via the {@link StyleRepository} and returns the added instance.
     *
     * @param style The {@link StyleInfo} to add; must not be null.
     * @return The persisted {@link StyleInfo}.
     * @throws NullPointerException if {@code style} is null.
     * @throws IllegalArgumentException if {@code style} is a proxy or lacks an ID.
     */
    @Override
    public StyleInfo add(StyleInfo style) {
        return add(style, StyleInfo.class, getStyleRepository());
    }

    /**
     * Removes a style from the catalog.
     *
     * <p>Delegates to the {@link StyleRepository} to delete the style and associated data.
     *
     * @param style The {@link StyleInfo} to remove; must not be null.
     * @throws NullPointerException if {@code style} is null.
     */
    @Override
    public void remove(StyleInfo style) {
        getStyleRepository().remove(style);
    }

    /**
     * Retrieves a style by its ID.
     *
     * <p>Queries the {@link StyleRepository} for a style matching the given ID.
     *
     * @param id The unique identifier of the style; must not be null.
     * @return The matching {@link StyleInfo} if found, or null if not.
     * @throws NullPointerException if {@code id} is null.
     */
    @Override
    public StyleInfo getStyle(String id) {
        return getStyleRepository().findById(id, StyleInfo.class).orElse(null);
    }

    /**
     * Retrieves a style by its name, preferring global styles.
     *
     * <p>Queries the {@link StyleRepository} first for a global style (no workspace) matching the name,
     * falling back to the first workspace-specific match if no global style is found.
     *
     * @param name The name of the style; must not be null.
     * @return The matching {@link StyleInfo} if found, or null if not.
     * @throws NullPointerException if {@code name} is null.
     */
    @Override
    public StyleInfo getStyleByName(String name) {
        Optional<StyleInfo> match = getStyleRepository().findByNameAndWordkspaceNull(name);
        if (match.isEmpty()) {
            match = getStyleRepository().findFirstByName(name, StyleInfo.class);
        }
        return match.orElse(null);
    }

    /**
     * Retrieves a style by name and workspace.
     *
     * <p>Handles special cases: {@link CatalogFacade#NO_WORKSPACE} queries global styles,
     * {@link CatalogFacade#ANY_WORKSPACE} searches across all workspaces, and specific workspaces restrict
     * the search to that context.
     *
     * @param workspace The workspace containing the style, or special values; must not be null.
     * @param name      The name of the style; must not be null.
     * @return The matching {@link StyleInfo} if found, or null if not.
     * @throws NullPointerException if {@code workspace} or {@code name} is null.
     */
    @Override
    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        Objects.requireNonNull(workspace, "workspace");
        Objects.requireNonNull(name, "name");

        if (workspace == ANY_WORKSPACE) {
            return getStyleByName(name);
        }
        Optional<StyleInfo> match = (workspace == NO_WORKSPACE)
                ? getStyleRepository().findByNameAndWordkspaceNull(name)
                : getStyleRepository().findByNameAndWorkspace(name, workspace);
        return match.orElse(null);
    }

    /**
     * Retrieves all styles in the catalog.
     *
     * <p>Queries the {@link StyleRepository} for all styles without restrictions.
     *
     * @return A list of all {@link StyleInfo} objects.
     */
    @Override
    public List<StyleInfo> getStyles() {
        return toList(getStyleRepository()::findAll);
    }

    /**
     * Retrieves all styles within a workspace.
     *
     * <p>If the workspace is null, defaults to the current default workspace. Handles
     * {@link CatalogFacade#NO_WORKSPACE} for global styles via the {@link StyleRepository}.
     *
     * @param workspace The workspace containing the styles; may be null to use the default.
     * @return A list of matching {@link StyleInfo} objects.
     */
    @Override
    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        Stream<StyleInfo> matches;
        if (workspace == NO_WORKSPACE) {
            matches = getStyleRepository().findAllByNullWorkspace();
        } else {
            WorkspaceInfo ws = (workspace == null) ? getDefaultWorkspace() : workspace;
            matches = getStyleRepository().findAllByWorkspace(ws);
        }
        return toList(() -> matches);
    }

    /**
     * Converts a stream supplier to a list, ensuring proper stream closure.
     *
     * <p>This utility method safely collects a stream into a list, closing the stream after use to
     * prevent resource leaks.
     *
     * @param <T>      The type of objects in the stream.
     * @param supplier A supplier providing the stream; must not be null.
     * @return A list of objects from the stream.
     * @throws NullPointerException if {@code supplier} is null.
     */
    protected <T extends CatalogInfo> List<T> toList(Supplier<Stream<T>> supplier) {
        try (Stream<T> stream = supplier.get()) {
            return stream.toList();
        }
    }

    /**
     * Disposes of all resources held by this facade.
     *
     * <p>Delegates to the internal repository holder to release all repository resources (e.g., database
     * connections).
     */
    @Override
    public void dispose() {
        repositories.dispose();
    }

    /**
     * Synchronizes this facade’s catalog data to another facade.
     *
     * <p>If the target facade is a {@link CatalogInfoRepositoryHolder}, performs an optimized sync by
     * delegating to each repository’s {@code syncTo} method. Otherwise, manually imports all objects using
     * the target’s {@code add} methods. Also syncs default settings (workspace, namespace, data stores).
     *
     * @param to The target {@link CatalogFacade} to sync to; must not be null.
     * @throws NullPointerException if {@code to} is null.
     * @example Syncing to another facade:
     *          <pre>
     *          RepositoryCatalogFacadeImpl source = ...;
     *          CatalogFacade target = new DefaultCatalogFacade();
     *          source.syncTo(target);
     *          </pre>
     */
    @Override
    public void syncTo(CatalogFacade to) {
        final CatalogFacade dao = ProxyUtils.unwrap(to, LockingCatalogFacade.class);
        if (dao instanceof CatalogInfoRepositoryHolder other) {
            this.getWorkspaceRepository().syncTo(other.getWorkspaceRepository());
            this.getNamespaceRepository().syncTo(other.getNamespaceRepository());
            this.getStoreRepository().syncTo(other.getStoreRepository());
            this.getResourceRepository().syncTo(other.getResourceRepository());
            this.getLayerRepository().syncTo(other.getLayerRepository());
            this.getLayerGroupRepository().syncTo(other.getLayerGroupRepository());
            this.getStyleRepository().syncTo(other.getStyleRepository());
            this.getMapRepository().syncTo(other.getMapRepository());
            dao.setCatalog(catalog);
        } else {
            sync(getWorkspaceRepository()::findAll, dao::add);
            sync(getNamespaceRepository()::findAll, dao::add);
            sync(getStoreRepository()::findAll, dao::add);
            sync(getResourceRepository()::findAll, dao::add);
            sync(getStyleRepository()::findAll, dao::add);
            sync(getLayerRepository()::findAll, dao::add);
            sync(getLayerGroupRepository()::findAll, dao::add);
            sync(getMapRepository()::findAll, dao::add);
        }
        dao.setDefaultWorkspace(getDefaultWorkspace());
        dao.setDefaultNamespace(getDefaultNamespace());
        try (Stream<DataStoreInfo> defaultDataStores = getStoreRepository().getDefaultDataStores()) {
            defaultDataStores.forEach(d -> dao.setDefaultDataStore(d.getWorkspace(), d));
        }
    }

    /**
     * Synchronizes a stream of catalog objects to a consumer, ensuring proper closure.
     *
     * <p>This helper method streams objects from a supplier and applies a consumer (e.g., {@code add}),
     * closing the stream afterward.
     *
     * @param <T>      The type of {@link CatalogInfo}.
     * @param from     A supplier providing the stream of objects; must not be null.
     * @param to       The consumer to apply to each object; must not be null.
     * @throws NullPointerException if {@code from} or {@code to} is null.
     */
    private <T extends CatalogInfo> void sync(Supplier<Stream<T>> from, Consumer<T> to) {
        try (Stream<T> all = from.get()) {
            all.forEach(to::accept);
        }
    }

    /**
     * Counts catalog objects of a specific type matching a filter.
     *
     * <p>For {@link PublishedInfo}, splits the filter to count layers and layer groups separately, summing
     * the results. Otherwise, delegates to the appropriate repository’s {@code count} method, capping at
     * {@link Integer#MAX_VALUE}.
     *
     * @param <T>    The type of {@link CatalogInfo} to count.
     * @param of     The class of objects to count; must not be null.
     * @param filter The filter to apply; must not be null.
     * @return The number of matching objects, capped at {@link Integer#MAX_VALUE}.
     * @throws NullPointerException if {@code of} or {@code filter} is null.
     * @throws CatalogException if counting fails due to repository errors.
     */
    @Override
    public <T extends CatalogInfo> int count(final Class<T> of, Filter filter) {
        filter = SimplifyingFilterVisitor.simplify(filter);
        if (PublishedInfo.class.equals(of)) {
            return countPublishedInfo(filter);
        }
        return countInternal(of, filter);
    }

    protected <T extends CatalogInfo> int countInternal(Class<T> of, Filter filter) {
        try {
            return (int) repository(of).count(of, filter);
        } catch (RuntimeException e) {
            throw new CatalogException(
                    "Error obtaining count of %s with filter %s".formatted(of.getSimpleName(), filter), e);
        }
    }

    /**
     * Queries {@link LayerRepository} and {@link LayerGroupRepository} for
     * individual counts returns the aggregate.
     *
     * <p>
     * Splits the query filter to handle layers and layer groups separately.
     * <p>
     * Subclasses are free to override this method if they can deal better with
     * PublishedInfo queries.
     *
     * @throws NullPointerException if {@code filter} is null.
     * @see #queryPublishedInfo(Query)
     */
    protected int countPublishedInfo(@NonNull Filter filter) {
        Map<Class<?>, Filter> filters = splitOredInstanceOf(filter);
        Filter layerFilter = filters.getOrDefault(LayerInfo.class, filter);
        Filter lgFilter = filters.getOrDefault(LayerGroupInfo.class, filter);
        int layers = count(LayerInfo.class, layerFilter);
        int groups = count(LayerGroupInfo.class, lgFilter);
        return layers + groups;
    }

    /**
     * Splits an OR filter into type-specific filters for {@link PublishedInfo} queries.
     *
     * <p>Analyzes the filter for {@code IsInstanceOf} conditions to separate {@link LayerInfo} and
     * {@link LayerGroupInfo} constraints, returning a map of type-to-filter pairs.
     *
     * @param filter The filter to split; must not be null.
     * @return A map of {@link Class} to {@link Filter} for each type, or an empty map if not splittable.
     */
    Map<Class<?>, Filter> splitOredInstanceOf(Filter filter) {
        Map<Class<?>, Filter> split = new HashMap<>();
        if (filter instanceof Or or) {
            for (Filter subFilter : or.getChildren()) {
                IsInstanceOf instanceOf = findInstanceOf(subFilter);
                if (instanceOf == null) {
                    return Map.of();
                }
                List<Expression> parameters = instanceOf.getParameters();
                if (parameters.isEmpty()) {
                    return Map.of();
                }
                Class<?> clazz = parameters.get(0).evaluate(null, Class.class);
                split.put(clazz, subFilter);
            }
        }
        return split;
    }

    /**
     * Finds an {@code IsInstanceOf} filter within a compound filter.
     *
     * <p>Recursively searches AND filters or checks binary comparisons for an {@code IsInstanceOf}
     * expression.
     *
     * @param subFilter The filter to search; must not be null.
     * @return The found {@link IsInstanceOf} filter, or null if none exists.
     */
    private IsInstanceOf findInstanceOf(Filter subFilter) {
        if (subFilter instanceof And and) {
            for (Filter f : and.getChildren()) {
                var i = findInstanceOf(f);
                if (i != null) {
                    return i;
                }
            }
        }
        if (subFilter instanceof BinaryComparisonOperator b) {
            IsInstanceOf instanceOf = extractInstanceOf(b);
            if (instanceOf != null) {
                return instanceOf;
            }
        }
        return null;
    }

    /**
     * Extracts an {@code IsInstanceOf} expression from a binary comparison.
     *
     * @param f The binary comparison filter; must not be null.
     * @return The {@link IsInstanceOf} expression if present, or null if not.
     */
    private IsInstanceOf extractInstanceOf(BinaryComparisonOperator f) {
        if (f.getExpression1() instanceof IsInstanceOf i) {
            return i;
        }
        if (f.getExpression2() instanceof IsInstanceOf i) {
            return i;
        }
        return null;
    }

    /**
     * Checks if sorting by a property is supported for a given type.
     *
     * <p>Supports sorting on properties (including nested ones) that are primitive or implement
     * {@link Comparable}. For {@link PublishedInfo}, checks both {@link LayerInfo} and
     * {@link LayerGroupInfo} capabilities.
     *
     * @param type         The type of {@link CatalogInfo} to sort; must not be null.
     * @param propertyName The property name to sort by; must not be null.
     * @return {@code true} if sorting is supported, {@code false} otherwise.
     * @throws NullPointerException if {@code type} or {@code propertyName} is null.
     * @see CatalogInfoRepository#canSortBy(String)
     */
    @Override
    public boolean canSort(final Class<? extends CatalogInfo> type, final String propertyName) {
        if (PublishedInfo.class.equals(type)) {
            return canSort(LayerInfo.class, propertyName) || canSort(LayerGroupInfo.class, propertyName);
        }
        return repository(type).canSortBy(propertyName);
    }

    /**
     * Validates that a query’s sort order is supported for its type.
     *
     * @param <T>  The type of {@link CatalogInfo}.
     * @param query The query to validate; must not be null.
     * @throws IllegalArgumentException if any sort property is unsupported for the query’s type.
     */
    private <T extends CatalogInfo> void checkCanSort(final Query<T> query) {
        query.getSortBy().forEach(sb -> checkCanSort(query.getType(), sb));
    }

    /**
     * Validates that a specific sort order is supported for a type.
     *
     * @param <T>  The type of {@link CatalogInfo}.
     * @param type The class of objects to sort; must not be null.
     * @param order The {@link SortBy} order to check; must not be null.
     * @throws IllegalArgumentException if the sort property is unsupported.
     */
    private <T extends CatalogInfo> void checkCanSort(final Class<T> type, SortBy order) {
        if (!canSort(type, order.getPropertyName().getPropertyName())) {
            throw new IllegalArgumentException(
                    "Can't sort objects of type %s by %s".formatted(type.getName(), order.getPropertyName()));
        }
    }

    /**
     * Queries the catalog for objects matching the specified criteria.
     *
     * <p>For {@link PublishedInfo}, calls {@link #queryPublishedInfo(Query)},
     * which queries {@link LayerRepository} and {@link LayerGroupRepository} for {@link PublishedInfo}, returning
     * a merge-sorted stream. Subclasses are free to override it if they can deal better with PublishedInfo queries.
     * <p>
     * Otherwise, delegates to the appropriate repository’s {@code findAll} method after validating sort
     * order. Returns an empty stream if the filter is {@link Filter#EXCLUDE}.
     *
     * @param <T>   The type of {@link CatalogInfo} to query.
     * @param query The query defining type, filter, sorting, and pagination; must not be null.
     * @return A {@link Stream} of matching objects; never null.
     * @throws NullPointerException if {@code query} is null.
     * @throws CatalogException if querying fails due to repository errors.
     * @throws IllegalArgumentException if sort order is unsupported.
     * @example Querying layers:
     *          <pre>
     *          Query<LayerInfo> query = Query.valueOf(LayerInfo.class, someFilter);
     *          try (Stream<LayerInfo> layers = facade.query(query)) {
     *              layers.forEach(l -> System.out.println(l.getName()));
     *          }
     *          </pre>
     */
    @Override
    public <T extends CatalogInfo> Stream<T> query(Query<T> query) {
        if (Filter.EXCLUDE.equals(query.getFilter())) {
            return Stream.empty();
        }
        final Class<T> type = query.getType();
        if (PublishedInfo.class.equals(type)) {
            return queryPublishedInfo(query.as()).map(query.getType()::cast);
        }
        return queryInternal(query);
    }

    protected <T extends CatalogInfo> Stream<T> queryInternal(Query<T> query) {
        try {
            checkCanSort(query);
            return repository(query.getType()).findAll(query);
        } catch (RuntimeException e) {
            throw new CatalogException("Error obtaining stream: %s".formatted(query), e);
        }
    }

    /**
     * Queries {@link LayerRepository} and {@link LayerGroupRepository} for {@link PublishedInfo}, returning
     * a merge-sorted stream.
     *
     * <p>Splits the query filter to handle layers and layer groups separately, applying offset and limit
     * in-memory if needed. Ensures predictable order with a default "id" sort if none is specified.
     * Closes underlying streams when the result stream is closed.
     *<p>Subclasses are free to override this method if they can deal better with PublishedInfo queries.
     * @param query The query defining criteria for {@link PublishedInfo}; must not be null.
     * @return A {@link Stream} of {@link PublishedInfo} objects (layers and layer groups); never null.
     * @throws NullPointerException if {@code query} is null.
     * @see #countPublishedInfo(Filter)
     */
    protected Stream<PublishedInfo> queryPublishedInfo(Query<PublishedInfo> query) {
        final Filter filter = SimplifyingFilterVisitor.simplify(query.getFilter());
        final Map<Class<?>, Filter> filters = splitOredInstanceOf(filter);
        final Filter layerFilter = filters.getOrDefault(LayerInfo.class, filter);
        final Filter lgFilter = filters.getOrDefault(LayerGroupInfo.class, filter);

        var layerQuery = new Query<>(LayerInfo.class, query).setFilter(layerFilter);
        var lgQuery = new Query<>(LayerGroupInfo.class, query).setFilter(lgFilter);

        if (query.getSortBy().isEmpty()) {
            List<SortBy> sortBy = List.of(Predicates.sortBy("id", true));
            layerQuery.setSortBy(sortBy);
            lgQuery.setSortBy(sortBy);
        }
        final Integer offset = query.getOffset();
        final Integer limit = query.getCount();
        final boolean applyOffsetLimit = shallApplyOffsetLimit(offset, limit, layerQuery, lgQuery);

        Stream<LayerInfo> layers = Stream.empty();
        Stream<LayerGroupInfo> groups = Stream.empty();
        try {
            layers = query(layerQuery);
            groups = query(lgQuery);

            Iterator<PublishedInfo> layersit =
                    layers.map(PublishedInfo.class::cast).iterator();
            Iterator<PublishedInfo> lgit = groups.map(PublishedInfo.class::cast).iterator();
            Comparator<PublishedInfo> comparator = CatalogInfoLookup.toComparator(query);

            var stream = Streams.stream(Iterators.mergeSorted(List.of(layersit, lgit), comparator));
            if (applyOffsetLimit) {
                if (offset != null) {
                    stream = stream.skip(offset);
                }
                if (limit != null) {
                    stream = stream.limit(limit);
                }
            }
            stream = closing(stream, layers, groups);
            return stream;
        } catch (RuntimeException e) {
            layers.close();
            groups.close();
            throw e;
        }
    }

    /**
     * Determines if offset and limit should be applied in-memory for a {@link PublishedInfo} query.
     *
     * <p>Checks if either layer or layer group results are non-zero, resetting query offsets/limits to
     * fetch all results for in-memory sorting if needed.
     *
     * @param offset     The offset from the original query; may be null.
     * @param limit      The limit from the original query; may be null.
     * @param layerQuery The query for layers; must not be null.
     * @param lgQuery    The query for layer groups; must not be null.
     * @return {@code true} if in-memory offset/limit is required, {@code false} otherwise.
     */
    protected boolean shallApplyOffsetLimit(
            final Integer offset, final Integer limit, Query<LayerInfo> layerQuery, Query<LayerGroupInfo> lgQuery) {
        if (null == offset && null == limit) {
            return false;
        }
        int lgCount = count(LayerGroupInfo.class, lgQuery.getFilter());
        if (0 == lgCount) {
            lgQuery.setFilter(Filter.EXCLUDE);
            return false;
        }
        int lcount = count(LayerInfo.class, layerQuery.getFilter());
        if (0 == lcount) {
            layerQuery.setFilter(Filter.EXCLUDE);
            return false;
        }
        layerQuery.setOffset(null);
        layerQuery.setCount(null);
        lgQuery.setOffset(null);
        lgQuery.setCount(null);
        return true;
    }

    /**
     * Adds closure logic to a stream to close additional streams when done.
     *
     * <p>Ensures that auxiliary streams (e.g., layers and groups) are closed when the main stream is closed.
     *
     * @param <T>        The type of objects in the stream.
     * @param stream     The main stream to extend; must not be null.
     * @param closeables Additional streams to close; must not be null.
     * @return The extended {@link Stream} with closure logic.
     */
    <T> Stream<T> closing(Stream<T> stream, Stream<?>... closeables) {
        return stream.onClose(() -> {
            for (var s : closeables) {
                s.close();
            }
        });
    }

    /**
     * Updates a catalog object with a patch.
     *
     * <p>Ensures the object is not a proxy, then delegates to the appropriate repository’s
     * {@code update} method to apply the patch and return the updated instance.
     *
     * @param <I>   The type of {@link CatalogInfo} to update.
     * @param info  The catalog object to update; must not be null and not a proxy.
     * @param patch The {@link Patch} containing changes; must not be null.
     * @return The updated {@link CatalogInfo} object.
     * @throws NullPointerException if {@code info} or {@code patch} is null.
     * @throws IllegalArgumentException if {@code info} is a proxy or not found in the repository.
     */
    @Override
    public <I extends CatalogInfo> I update(I info, Patch patch) {
        checkNotAProxy(info);
        CatalogInfoRepository<I> repo = repositoryFor(info);
        return repo.update(info, patch);
    }

    /**
     * Validates that a catalog object is not a proxy.
     *
     * @param value The {@link CatalogInfo} object to check; must not be null.
     * @throws IllegalArgumentException if {@code value} is a proxy.
     */
    private static void checkNotAProxy(CatalogInfo value) {
        if (Proxy.isProxyClass(value.getClass())) {
            throw new IllegalArgumentException("Proxy values shall not be passed to CatalogInfoLookup");
        }
    }

    /**
     * Retrieves the repository for a specific {@link CatalogInfo} type.
     *
     * <p>Delegates to the internal repository holder’s {@link CatalogInfoRepositoryHolder#repository(Class)}.
     *
     * @param <T> The type of {@link CatalogInfo}.
     * @param <R> The corresponding repository type.
     * @param of  The class of catalog info objects; must not be null.
     * @return The repository for type {@code T}; never null.
     * @throws NullPointerException if {@code of} is null.
     * @throws IllegalArgumentException if no repository is configured for the type.
     */
    @Override
    public <T extends CatalogInfo, R extends CatalogInfoRepository<T>> R repository(Class<T> of) {
        return repositories.repository(of);
    }

    /**
     * Retrieves the repository for the type of a given {@link CatalogInfo} instance.
     *
     * <p>Delegates to the internal repository holder’s {@link CatalogInfoRepositoryHolder#repositoryFor(CatalogInfo)}.
     *
     * @param <T>  The type of {@link CatalogInfo}.
     * @param <R>  The corresponding repository type.
     * @param info The catalog info object; must not be null.
     * @return The repository for the object’s type; never null.
     * @throws NullPointerException if {@code info} is null.
     * @throws IllegalArgumentException if no repository is configured for the object’s type.
     */
    @Override
    public <T extends CatalogInfo, R extends CatalogInfoRepository<T>> R repositoryFor(T info) {
        return repositories.repositoryFor(info);
    }

    /**
     * Sets the repository for managing {@link NamespaceInfo} objects.
     *
     * <p>Delegates to the internal repository holder’s {@link CatalogInfoRepositoryHolder#setNamespaceRepository(NamespaceRepository)}.
     *
     * @param namespaces The {@link NamespaceRepository} to set; must not be null.
     * @throws NullPointerException if {@code namespaces} is null.
     */
    @Override
    public void setNamespaceRepository(NamespaceRepository namespaces) {
        repositories.setNamespaceRepository(namespaces);
    }

    /**
     * Retrieves the repository for managing {@link NamespaceInfo} objects.
     *
     * @return The configured {@link NamespaceRepository}; never null.
     * @throws IllegalStateException if no namespace repository has been set.
     */
    @Override
    public NamespaceRepository getNamespaceRepository() {
        return repositories.getNamespaceRepository();
    }

    /**
     * Sets the repository for managing {@link WorkspaceInfo} objects.
     *
     * <p>Delegates to the internal repository holder’s {@link CatalogInfoRepositoryHolder#setWorkspaceRepository(WorkspaceRepository)}.
     *
     * @param workspaces The {@link WorkspaceRepository} to set; must not be null.
     * @throws NullPointerException if {@code workspaces} is null.
     */
    @Override
    public void setWorkspaceRepository(WorkspaceRepository workspaces) {
        repositories.setWorkspaceRepository(workspaces);
    }

    /**
     * Retrieves the repository for managing {@link WorkspaceInfo} objects.
     *
     * @return The configured {@link WorkspaceRepository}; never null.
     * @throws IllegalStateException if no workspace repository has been set.
     */
    @Override
    public WorkspaceRepository getWorkspaceRepository() {
        return repositories.getWorkspaceRepository();
    }

    /**
     * Sets the repository for managing {@link StoreInfo} objects.
     *
     * <p>Delegates to the internal repository holder’s {@link CatalogInfoRepositoryHolder#setStoreRepository(StoreRepository)}.
     *
     * @param stores The {@link StoreRepository} to set; must not be null.
     * @throws NullPointerException if {@code stores} is null.
     */
    @Override
    public void setStoreRepository(StoreRepository stores) {
        repositories.setStoreRepository(stores);
    }

    /**
     * Retrieves the repository for managing {@link StoreInfo} objects.
     *
     * @return The configured {@link StoreRepository}; never null.
     * @throws IllegalStateException if no store repository has been set.
     */
    @Override
    public StoreRepository getStoreRepository() {
        return repositories.getStoreRepository();
    }

    /**
     * Sets the repository for managing {@link ResourceInfo} objects.
     *
     * <p>Delegates to the internal repository holder’s {@link CatalogInfoRepositoryHolder#setResourceRepository(ResourceRepository)}.
     *
     * @param resources The {@link ResourceRepository} to set; must not be null.
     * @throws NullPointerException if {@code resources} is null.
     */
    @Override
    public void setResourceRepository(ResourceRepository resources) {
        repositories.setResourceRepository(resources);
    }

    /**
     * Retrieves the repository for managing {@link ResourceInfo} objects.
     *
     * @return The configured {@link ResourceRepository}; never null.
     * @throws IllegalStateException if no resource repository has been set.
     */
    @Override
    public ResourceRepository getResourceRepository() {
        return repositories.getResourceRepository();
    }

    /**
     * Sets the repository for managing {@link LayerInfo} objects.
     *
     * <p>Delegates to the internal repository holder’s {@link CatalogInfoRepositoryHolder#setLayerRepository(LayerRepository)}.
     *
     * @param layers The {@link LayerRepository} to set; must not be null.
     * @throws NullPointerException if {@code layers} is null.
     */
    @Override
    public void setLayerRepository(LayerRepository layers) {
        repositories.setLayerRepository(layers);
    }

    /**
     * Retrieves the repository for managing {@link LayerInfo} objects.
     *
     * @return The configured {@link LayerRepository}; never null.
     * @throws IllegalStateException if no layer repository has been set.
     */
    @Override
    public LayerRepository getLayerRepository() {
        return repositories.getLayerRepository();
    }

    /**
     * Sets the repository for managing {@link LayerGroupInfo} objects.
     *
     * <p>Delegates to the internal repository holder’s {@link CatalogInfoRepositoryHolder#setLayerGroupRepository(LayerGroupRepository)}.
     *
     * @param layerGroups The {@link LayerGroupRepository} to set; must not be null.
     * @throws NullPointerException if {@code layerGroups} is null.
     */
    @Override
    public void setLayerGroupRepository(LayerGroupRepository layerGroups) {
        repositories.setLayerGroupRepository(layerGroups);
    }

    /**
     * Retrieves the repository for managing {@link LayerGroupInfo} objects.
     *
     * @return The configured {@link LayerGroupRepository}; never null.
     * @throws IllegalStateException if no layer group repository has been set.
     */
    @Override
    public LayerGroupRepository getLayerGroupRepository() {
        return repositories.getLayerGroupRepository();
    }

    /**
     * Sets the repository for managing {@link StyleInfo} objects.
     *
     * <p>Delegates to the internal repository holder’s {@link CatalogInfoRepositoryHolder#setStyleRepository(StyleRepository)}.
     *
     * @param styles The {@link StyleRepository} to set; must not be null.
     * @throws NullPointerException if {@code styles} is null.
     */
    @Override
    public void setStyleRepository(StyleRepository styles) {
        repositories.setStyleRepository(styles);
    }

    /**
     * Retrieves the repository for managing {@link StyleInfo} objects.
     *
     * @return The configured {@link StyleRepository}; never null.
     * @throws IllegalStateException if no style repository has been set.
     */
    @Override
    public StyleRepository getStyleRepository() {
        return repositories.getStyleRepository();
    }

    /**
     * Sets the repository for managing {@link MapInfo} objects.
     *
     * <p>Delegates to the internal repository holder’s {@link CatalogInfoRepositoryHolder#setMapRepository(MapRepository)}.
     *
     * @param maps The {@link MapRepository} to set; must not be null.
     * @throws NullPointerException if {@code maps} is null.
     */
    @Override
    public void setMapRepository(MapRepository maps) {
        repositories.setMapRepository(maps);
    }

    /**
     * Retrieves the repository for managing {@link MapInfo} objects.
     *
     * @return The configured {@link MapRepository}; never null.
     * @throws IllegalStateException if no map repository has been set.
     */
    @Override
    public MapRepository getMapRepository() {
        return repositories.getMapRepository();
    }
}
