/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin;

import com.google.common.collect.Iterators;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.geoserver.catalog.CatalogCapabilities;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.forwarding.ForwardingExtendedCatalogFacade;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.LocalWorkspace;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;

/**
 * A catalog facade that enforces workspace isolation in GeoServer Cloud, restricting visibility of
 * catalog objects based on the current local workspace context.
 *
 * <p>This class extends {@link ForwardingExtendedCatalogFacade} to wrap an existing
 * {@link ExtendedCatalogFacade} implementation, adding isolation logic inspired by GeoServer’s
 * package-private {@code org.geoserver.catalog.impl.IsolatedCatalogFacade}. It ensures that catalog
 * objects (e.g., stores, resources, layers) outside the current local workspace—set via
 * {@link LocalWorkspace} during virtual service requests—are filtered out or return null, unless
 * they are non-isolated or globally accessible.
 *
 * <p>Key features:
 * <ul>
 *   <li><strong>Isolation Enforcement:</strong> Methods like {@link #getStore(String, Class)} and
 *       {@link #getResources(Class)} filter results based on workspace visibility, determined by
 *       {@link #canSeeWorkspace(WorkspaceInfo)}.</li>
 *   <li><strong>Type Safety:</strong> Generic methods maintain type safety while applying isolation.</li>
 *   <li><strong>Stream Support:</strong> Modern {@link Stream}-based querying via {@link #query(Query)}.</li>
 *   <li><strong>Legacy Compatibility:</strong> Overrides deprecated {@link #list} for backward compatibility.</li>
 * </ul>
 *
 * <p>Isolation is context-dependent, relying on {@link Dispatcher#REQUEST} and {@link LocalWorkspace}
 * to determine the active workspace during an OWS request. Outside a request context, isolation is
 * bypassed, delegating to the underlying facade.
 *
 * @since 1.0
 * @see ExtendedCatalogFacade
 * @see LocalWorkspace
 * @see Dispatcher
 */
public final class IsolatedCatalogFacade extends ForwardingExtendedCatalogFacade {

    /**
     * Constructs an {@code IsolatedCatalogFacade} wrapping the provided facade.
     *
     * @param facade The underlying {@link ExtendedCatalogFacade} to delegate to; must not be null.
     * @throws NullPointerException if {@code facade} is null.
     */
    IsolatedCatalogFacade(ExtendedCatalogFacade facade) {
        super(facade);
    }

    /**
     * Retrieves a store by ID, enforcing workspace isolation.
     *
     * <p>If the store’s workspace is not visible in the current context (per
     * {@link #canSeeWorkspace(WorkspaceInfo)}), returns null.
     *
     * @param <T>   The specific type of {@link StoreInfo} to retrieve.
     * @param id    The unique identifier of the store; must not be null.
     * @param clazz The class of the store to retrieve; must not be null.
     * @return The matching {@link StoreInfo} if visible, or null if not found or isolated.
     * @throws NullPointerException if {@code id} or {@code clazz} is null.
     * @example Retrieving a store:
     *          <pre>
     *          DataStoreInfo store = facade.getStore("store1", DataStoreInfo.class);
     *          </pre>
     */
    @Override
    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return enforceStoreIsolation(facade.getStore(id, clazz));
    }

    /**
     * Retrieves a store by name and workspace, enforcing isolation.
     *
     * <p>If the workspace is not visible (per {@link #canSeeWorkspace(WorkspaceInfo)}), returns null.
     *
     * @param <T>       The specific type of {@link StoreInfo} to retrieve.
     * @param workspace The workspace containing the store; may be null.
     * @param name      The name of the store; must not be null.
     * @param clazz     The class of the store to retrieve; must not be null.
     * @return The matching {@link StoreInfo} if visible, or null if not found or isolated.
     * @throws NullPointerException if {@code name} or {@code clazz} is null.
     */
    @Override
    public <T extends StoreInfo> T getStoreByName(WorkspaceInfo workspace, String name, Class<T> clazz) {
        return canSeeWorkspace(workspace) ? facade.getStoreByName(workspace, name, clazz) : null;
    }

    /**
     * Retrieves all stores in a workspace, enforcing isolation.
     *
     * <p>If the workspace is not visible, returns an empty list.
     *
     * @param <T>       The specific type of {@link StoreInfo} to retrieve.
     * @param workspace The workspace containing the stores; may be null.
     * @param clazz     The class of the stores to retrieve; must not be null.
     * @return A list of visible {@link StoreInfo} objects, or empty if workspace is isolated.
     * @throws NullPointerException if {@code clazz} is null.
     */
    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(WorkspaceInfo workspace, Class<T> clazz) {
        return canSeeWorkspace(workspace) ? facade.getStoresByWorkspace(workspace, clazz) : Collections.emptyList();
    }

    /**
     * Retrieves all stores of a specific type, filtering out isolated ones.
     *
     * @param <T>   The specific type of {@link StoreInfo} to retrieve.
     * @param clazz The class of the stores to retrieve; must not be null.
     * @return A list of visible {@link StoreInfo} objects.
     * @throws NullPointerException if {@code clazz} is null.
     */
    @Override
    public <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return filterIsolated(facade.getStores(clazz), clazz, this::filter);
    }

    /**
     * Retrieves the default data store for a workspace, enforcing isolation.
     *
     * @param workspace The workspace to query; may be null.
     * @return The default {@link DataStoreInfo} if visible, or null if not found or isolated.
     */
    @Override
    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return enforceStoreIsolation(facade.getDefaultDataStore(workspace));
    }

    /**
     * Retrieves a resource by ID, enforcing isolation.
     *
     * @param <T>   The specific type of {@link ResourceInfo} to retrieve.
     * @param id    The unique identifier of the resource; must not be null.
     * @param clazz The class of the resource to retrieve; must not be null.
     * @return The matching {@link ResourceInfo} if visible, or null if not found or isolated.
     * @throws NullPointerException if {@code id} or {@code clazz} is null.
     */
    @Override
    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return enforceResourceIsolation(facade.getResource(id, clazz));
    }

    /**
     * Retrieves a resource by name and namespace, enforcing isolation with namespace matching.
     *
     * <p>If a local workspace is active and its namespace URI matches the provided namespace’s URI,
     * uses the local namespace; otherwise, applies isolation rules.
     *
     * @param <T>       The specific type of {@link ResourceInfo} to retrieve.
     * @param namespace The namespace containing the resource; may be null.
     * @param name      The name of the resource; must not be null.
     * @param clazz     The class of the resource to retrieve; must not be null.
     * @return The matching {@link ResourceInfo} if visible, or null if not found or isolated.
     * @throws NullPointerException if {@code name} or {@code clazz} is null.
     */
    @Override
    public <T extends ResourceInfo> T getResourceByName(NamespaceInfo namespace, String name, Class<T> clazz) {
        NamespaceInfo localNamespace = tryMatchLocalNamespace(namespace);
        if (localNamespace != null) {
            return facade.getResourceByName(localNamespace, name, clazz);
        }
        return enforceResourceIsolation(facade.getResourceByName(namespace, name, clazz));
    }

    /**
     * Retrieves all resources of a specific type, filtering out isolated ones.
     *
     * @param <T>   The specific type of {@link ResourceInfo} to retrieve.
     * @param clazz The class of the resources to retrieve; must not be null.
     * @return A list of visible {@link ResourceInfo} objects.
     * @throws NullPointerException if {@code clazz} is null.
     */
    @Override
    public <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return filterIsolated(facade.getResources(clazz), clazz, this::filter);
    }

    /**
     * Retrieves all resources in a namespace, enforcing isolation with namespace matching.
     *
     * @param <T>       The specific type of {@link ResourceInfo} to retrieve.
     * @param namespace The namespace containing the resources; may be null.
     * @param clazz     The class of the resources to retrieve; must not be null.
     * @return A list of visible {@link ResourceInfo} objects.
     * @throws NullPointerException if {@code clazz} is null.
     */
    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(NamespaceInfo namespace, Class<T> clazz) {
        NamespaceInfo localNamespace = tryMatchLocalNamespace(namespace);
        if (localNamespace != null) {
            return facade.getResourcesByNamespace(localNamespace, clazz);
        }
        return filterIsolated(facade.getResourcesByNamespace(namespace, clazz), clazz, this::filter);
    }

    /**
     * Retrieves a resource by store and name, enforcing isolation.
     *
     * @param <T>   The specific type of {@link ResourceInfo} to retrieve.
     * @param store The store containing the resource; may be null.
     * @param name  The name of the resource; must not be null.
     * @param clazz The class of the resource to retrieve; must not be null.
     * @return The matching {@link ResourceInfo} if visible, or null if not found or isolated.
     * @throws NullPointerException if {@code name} or {@code clazz} is null.
     */
    @Override
    public <T extends ResourceInfo> T getResourceByStore(StoreInfo store, String name, Class<T> clazz) {
        return enforceResourceIsolation(facade.getResourceByStore(store, name, clazz));
    }

    /**
     * Retrieves all resources in a store, filtering out isolated ones.
     *
     * @param <T>   The specific type of {@link ResourceInfo} to retrieve.
     * @param store The store containing the resources; may be null.
     * @param clazz The class of the resources to retrieve; must not be null.
     * @return A list of visible {@link ResourceInfo} objects.
     * @throws NullPointerException if {@code clazz} is null.
     */
    @Override
    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz) {
        return filterIsolated(facade.getResourcesByStore(store, clazz), clazz, this::filter);
    }

    /**
     * Retrieves a layer by ID, enforcing isolation.
     *
     * @param id The unique identifier of the layer; must not be null.
     * @return The matching {@link LayerInfo} if visible, or null if not found or isolated.
     * @throws NullPointerException if {@code id} is null.
     */
    @Override
    public LayerInfo getLayer(String id) {
        return enforceLayerIsolation(facade.getLayer(id));
    }

    /**
     * Retrieves a layer by name, enforcing isolation.
     *
     * @param name The name of the layer; must not be null.
     * @return The matching {@link LayerInfo} if visible, or null if not found or isolated.
     * @throws NullPointerException if {@code name} is null.
     */
    @Override
    public LayerInfo getLayerByName(String name) {
        return enforceLayerIsolation(facade.getLayerByName(name));
    }

    /**
     * Retrieves all layers associated with a resource, filtering out isolated ones.
     *
     * @param resource The resource linked to the layers; may be null.
     * @return A list of visible {@link LayerInfo} objects.
     */
    @Override
    public List<LayerInfo> getLayers(ResourceInfo resource) {
        return filterIsolated(facade.getLayers(resource), LayerInfo.class, this::filter);
    }

    /**
     * Retrieves all layers using a style, filtering out isolated ones.
     *
     * @param style The style linked to the layers; may be null.
     * @return A list of visible {@link LayerInfo} objects.
     */
    @Override
    public List<LayerInfo> getLayers(StyleInfo style) {
        return filterIsolated(facade.getLayers(style), LayerInfo.class, this::filter);
    }

    /**
     * Retrieves all layers, filtering out isolated ones.
     *
     * @return A list of visible {@link LayerInfo} objects.
     */
    @Override
    public List<LayerInfo> getLayers() {
        return filterIsolated(facade.getLayers(), LayerInfo.class, this::filter);
    }

    /**
     * Retrieves a layer group by ID, enforcing isolation.
     *
     * @param id The unique identifier of the layer group; must not be null.
     * @return The matching {@link LayerGroupInfo} if visible, or null if not found or isolated.
     * @throws NullPointerException if {@code id} is null.
     */
    @Override
    public LayerGroupInfo getLayerGroup(String id) {
        return enforceLayerGroupIsolation(facade.getLayerGroup(id));
    }

    /**
     * Retrieves a layer group by name, enforcing isolation.
     *
     * @param name The name of the layer group; must not be null.
     * @return The matching {@link LayerGroupInfo} if visible, or null if not found or isolated.
     * @throws NullPointerException if {@code name} is null.
     */
    @Override
    public LayerGroupInfo getLayerGroupByName(String name) {
        return enforceLayerGroupIsolation(facade.getLayerGroupByName(name));
    }

    /**
     * Retrieves a layer group by name and workspace, enforcing isolation.
     *
     * @param workspace The workspace containing the layer group; may be null.
     * @param name      The name of the layer group; must not be null.
     * @return The matching {@link LayerGroupInfo} if visible, or null if not found or isolated.
     * @throws NullPointerException if {@code name} is null.
     */
    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        return enforceLayerGroupIsolation(facade.getLayerGroupByName(workspace, name));
    }

    /**
     * Retrieves all layer groups, filtering out isolated ones.
     *
     * @return A list of visible {@link LayerGroupInfo} objects.
     */
    @Override
    public List<LayerGroupInfo> getLayerGroups() {
        return filterIsolated(facade.getLayerGroups(), LayerGroupInfo.class, this::filter);
    }

    /**
     * Retrieves all layer groups in a workspace, filtering out isolated ones.
     *
     * @param workspace The workspace containing the layer groups; may be null.
     * @return A list of visible {@link LayerGroupInfo} objects.
     */
    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        return filterIsolated(facade.getLayerGroupsByWorkspace(workspace), LayerGroupInfo.class, this::filter);
    }

    /**
     * Retrieves a namespace by URI, enforcing isolation with local workspace matching.
     *
     * <p>Prioritizes the local workspace’s namespace if its URI matches; otherwise, returns a global
     * non-isolated namespace or null.
     *
     * @param uri The URI of the namespace; must not be null.
     * @return The matching {@link NamespaceInfo} if visible, or null if not found or isolated.
     * @throws NullPointerException if {@code uri} is null.
     */
    @Override
    public NamespaceInfo getNamespaceByURI(String uri) {
        NamespaceInfo localNamespace = getLocalNamespace();
        if (localNamespace != null && Objects.equals(localNamespace.getURI(), uri)) {
            return localNamespace;
        }
        for (NamespaceInfo namespace : facade.getNamespacesByURI(uri)) {
            if (!namespace.isIsolated()) {
                return namespace;
            }
        }
        return null;
    }

    /**
     * Retrieves a style by ID, enforcing isolation.
     *
     * @param id The unique identifier of the style; must not be null.
     * @return The matching {@link StyleInfo} if visible, or null if not found or isolated.
     * @throws NullPointerException if {@code id} is null.
     */
    @Override
    public StyleInfo getStyle(String id) {
        return enforceStyleIsolation(facade.getStyle(id));
    }

    /**
     * Retrieves a style by name, enforcing isolation.
     *
     * @param name The name of the style; must not be null.
     * @return The matching {@link StyleInfo} if visible, or null if not found or isolated.
     * @throws NullPointerException if {@code name} is null.
     */
    @Override
    public StyleInfo getStyleByName(String name) {
        return enforceStyleIsolation(facade.getStyleByName(name));
    }

    /**
     * Retrieves a style by name and workspace, enforcing isolation.
     *
     * @param workspace The workspace containing the style; may be null.
     * @param name      The name of the style; must not be null.
     * @return The matching {@link StyleInfo} if visible, or null if not found or isolated.
     * @throws NullPointerException if {@code name} is null.
     */
    @Override
    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        return enforceStyleIsolation(facade.getStyleByName(workspace, name));
    }

    /**
     * Retrieves all styles, filtering out isolated ones.
     *
     * @return A list of visible {@link StyleInfo} objects.
     */
    @Override
    public List<StyleInfo> getStyles() {
        return filterIsolated(facade.getStyles(), StyleInfo.class, this::filter);
    }

    /**
     * Retrieves all styles in a workspace, filtering out isolated ones.
     *
     * @param workspace The workspace containing the styles; may be null.
     * @return A list of visible {@link StyleInfo} objects.
     */
    @Override
    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        return filterIsolated(facade.getStylesByWorkspace(workspace), StyleInfo.class, this::filter);
    }

    /**
     * Counts catalog objects matching the type and filter, enforcing isolation during requests.
     *
     * <p>Within an OWS request context, uses {@link #query(Query)} to filter results; otherwise,
     * delegates to the underlying facade.
     *
     * @param <T>    The type of {@link CatalogInfo} to count.
     * @param of     The class of objects to count; must not be null.
     * @param filter The filter to apply; must not be null.
     * @return The number of visible matching objects.
     * @throws NullPointerException if {@code of} or {@code filter} is null.
     */
    @Override
    public <T extends CatalogInfo> int count(Class<T> of, Filter filter) {
        if (Dispatcher.REQUEST.get() == null) {
            return super.count(of, filter);
        }
        try (Stream<T> result = query(Query.valueOf(of, filter))) {
            return (int) result.count();
        }
    }

    /**
     * Retrieves a list of catalog objects matching the criteria, enforcing isolation, using a legacy iterator.
     *
     * <p>This method is deprecated in favor of {@link #query(Query)}. It adapts the underlying facade’s
     * results, filtering out isolated objects within a request context, ignoring offset/count for
     * accuracy.
     *
     * @param <T>       The type of {@link CatalogInfo} to list.
     * @param of        The class of objects to list; must not be null.
     * @param filter    The filter to apply; must not be null.
     * @param offset    The number of objects to skip, or null for no offset.
     * @param count     The maximum number of objects to return, or null for no limit.
     * @param sortOrder Variable number of {@link SortBy} directives (nulls ignored).
     * @return A {@link CloseableIterator} of visible objects.
     * @throws NullPointerException if {@code of} or {@code filter} is null.
     * @deprecated since 1.0, for removal; use {@link #query(Query)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(
            Class<T> of,
            Filter filter,
            @Nullable Integer offset,
            @Nullable Integer count,
            @Nullable SortBy... sortOrder) {

        if (Dispatcher.REQUEST.get() == null) {
            return facade.list(of, filter, offset, count, sortOrder);
        }
        final CloseableIterator<T> all = facade.list(of, filter, null, null, sortOrder);
        Iterator<T> filtered = Iterators.filter(all, this::filter);
        if (offset != null) {
            Iterators.advance(filtered, offset.intValue());
        }
        if (count != null) {
            filtered = Iterators.limit(filtered, count.intValue());
        }
        return new CloseableIteratorAdapter<>(filtered, all);
    }

    /**
     * Queries the catalog with isolation applied, filtering out non-visible objects.
     *
     * <p>Delegates to the underlying facade’s {@link ExtendedCatalogFacade#query(Query)}, then applies
     * isolation rules to the results.
     *
     * @param <T>   The type of {@link CatalogInfo} to query.
     * @param query The query specifying criteria; must not be null.
     * @return A {@link Stream} of visible objects; never null.
     * @throws NullPointerException if {@code query} is null.
     * @example Querying visible layers:
     *          <pre>
     *          Query<LayerInfo> query = Query.valueOf(LayerInfo.class, someFilter);
     *          try (Stream<LayerInfo> layers = facade.query(query)) {
     *              layers.forEach(l -> System.out.println(l.getName()));
     *          }
     *          </pre>
     */
    @Override
    public <T extends CatalogInfo> Stream<T> query(Query<T> query) {
        return ((ExtendedCatalogFacade) facade)
                .query(query)
                .map(this::enforceIsolation)
                .filter(i -> i != null);
    }

    /**
     * Returns the catalog capabilities with isolation support enabled.
     *
     * <p>Extends the underlying facade’s capabilities by setting isolated workspace support to true.
     *
     * @return The {@link CatalogCapabilities} with isolation support indicated.
     */
    @Override
    public CatalogCapabilities getCatalogCapabilities() {
        CatalogCapabilities capabilities = facade.getCatalogCapabilities();
        capabilities.setIsolatedWorkspacesSupport(true);
        return capabilities;
    }

    /**
     * Retrieves the current local workspace, if set.
     *
     * @return The current {@link WorkspaceInfo} from {@link LocalWorkspace}, or null if not set.
     */
    private WorkspaceInfo getLocalWorkspace() {
        return LocalWorkspace.get();
    }

    /**
     * Enforces isolation on a catalog object based on its type.
     *
     * <p>Delegates to type-specific isolation methods (e.g., {@link #enforceStoreIsolation}).
     *
     * @param <T>  The type of {@link CatalogInfo}.
     * @param info The catalog object to check; may be null.
     * @return The object if visible, or null if isolated.
     */
    @SuppressWarnings("unchecked")
    private <T extends CatalogInfo> T enforceIsolation(T info) {
        if (info instanceof StoreInfo store) {
            return (T) enforceStoreIsolation(store);
        }
        if (info instanceof ResourceInfo resource) {
            return (T) enforceResourceIsolation(resource);
        }
        if (info instanceof LayerInfo layer) {
            return (T) enforceLayerIsolation(layer);
        }
        if (info instanceof LayerGroupInfo lg) {
            return (T) enforceLayerGroupIsolation(lg);
        }
        if (info instanceof StyleInfo style) {
            return (T) enforceStyleIsolation(style);
        }
        return info;
    }

    /**
     * Filters a catalog object for visibility.
     *
     * @param <T>  The type of {@link CatalogInfo}.
     * @param info The catalog object to filter; may be null.
     * @return {@code true} if visible, {@code false} if isolated or null.
     */
    private <T extends CatalogInfo> boolean filter(T info) {
        if (info != null) {
            return enforceIsolation(info) != null;
        }
        return false;
    }

    /**
     * Enforces isolation on a store, checking its workspace visibility.
     *
     * @param <T>   The type of {@link StoreInfo}.
     * @param store The store to check; may be null.
     * @return The store if visible, or null if isolated.
     */
    private <T extends StoreInfo> T enforceStoreIsolation(T store) {
        if (store == null) {
            return null;
        }
        WorkspaceInfo workspace = store.getWorkspace();
        return canSeeWorkspace(workspace) ? store : null;
    }

    /**
     * Enforces isolation on a resource, checking its store’s workspace visibility.
     *
     * @param <T>      The type of {@link ResourceInfo}.
     * @param resource The resource to check; may be null.
     * @return The resource if visible, or null if isolated.
     */
    private <T extends ResourceInfo> T enforceResourceIsolation(T resource) {
        if (resource == null) {
            return null;
        }
        StoreInfo store = resource.getStore();
        if (store == null) {
            return resource;
        }
        WorkspaceInfo workspace = store.getWorkspace();
        return canSeeWorkspace(workspace) ? resource : null;
    }

    /**
     * Enforces isolation on a layer, checking its resource’s store workspace visibility.
     *
     * @param <T>   The type of {@link LayerInfo}.
     * @param layer The layer to check; may be null.
     * @return The layer if visible, or null if isolated.
     */
    private <T extends LayerInfo> T enforceLayerIsolation(T layer) {
        if (layer == null) {
            return null;
        }
        ResourceInfo resource = layer.getResource();
        if (resource == null) {
            return layer;
        }
        StoreInfo store = resource.getStore();
        if (store == null) {
            return layer;
        }
        WorkspaceInfo workspace = store.getWorkspace();
        return canSeeWorkspace(workspace) ? layer : null;
    }

    /**
     * Enforces isolation on a style, checking its workspace visibility.
     *
     * @param <T>   The type of {@link StyleInfo}.
     * @param style The style to check; may be null.
     * @return The style if visible, or null if isolated.
     */
    private <T extends StyleInfo> T enforceStyleIsolation(T style) {
        if (style == null) {
            return null;
        }
        WorkspaceInfo workspace = style.getWorkspace();
        return canSeeWorkspace(workspace) ? style : null;
    }

    /**
     * Enforces isolation on a layer group, checking its workspace visibility.
     *
     * <p>Note: Nested layer groups within the result are not filtered for isolation.
     *
     * @param <T>       The type of {@link LayerGroupInfo}.
     * @param layerGroup The layer group to check; may be null.
     * @return The layer group if visible, or null if isolated.
     */
    private <T extends LayerGroupInfo> T enforceLayerGroupIsolation(T layerGroup) {
        if (layerGroup == null) {
            return null;
        }
        WorkspaceInfo workspace = layerGroup.getWorkspace();
        return canSeeWorkspace(workspace) ? layerGroup : null;
    }

    /**
     * Determines if a workspace is visible in the current context.
     *
     * <p>A workspace is visible if:
     * <ul>
     *   <li>It is a special workspace ({@link CatalogFacade#NO_WORKSPACE}, {@link CatalogFacade#ANY_WORKSPACE}).</li>
     *   <li>It is null or non-isolated.</li>
     *   <li>No request context exists (outside OWS).</li>
     *   <li>It matches the current local workspace (via {@link LocalWorkspace}).</li>
     * </ul>
     *
     * @param workspace The workspace to check; may be null.
     * @return {@code true} if visible, {@code false} if isolated.
     */
    private boolean canSeeWorkspace(WorkspaceInfo workspace) {
        if (workspace == CatalogFacade.NO_WORKSPACE
                || workspace == CatalogFacade.ANY_WORKSPACE
                || workspace == null
                || !workspace.isIsolated()
                || Dispatcher.REQUEST.get() == null) {
            return true;
        }
        WorkspaceInfo localWorkspace = getLocalWorkspace();
        return localWorkspace != null && Objects.equals(localWorkspace.getName(), workspace.getName());
    }

    /**
     * Filters a list of catalog objects, removing those not visible in the current context.
     *
     * <p>Handles {@link ModificationProxy} unwrapping and rewrapping for consistency.
     *
     * @param <T>     The type of {@link CatalogInfo}.
     * @param objects The list of objects to filter; may be null or empty.
     * @param type    The class of the objects; must not be null.
     * @param filter  The predicate to apply; must not be null.
     * @return A filtered list wrapped with {@link ModificationProxy}.
     * @throws NullPointerException if {@code type} or {@code filter} is null.
     */
    private <T extends CatalogInfo> List<T> filterIsolated(List<T> objects, Class<T> type, Predicate<T> filter) {
        List<T> unwrapped = ModificationProxy.unwrap(objects);
        return ModificationProxy.createList(unwrapped.stream().filter(filter).toList(), type);
    }

    /**
     * Attempts to match a namespace to the local workspace’s namespace by URI.
     *
     * @param namespace The namespace to match; may be null.
     * @return The local workspace’s {@link NamespaceInfo} if URIs match, or null if no match.
     */
    private NamespaceInfo tryMatchLocalNamespace(NamespaceInfo namespace) {
        WorkspaceInfo localWorkspace = getLocalWorkspace();
        if (localWorkspace != null) {
            NamespaceInfo localNamespace = facade.getNamespaceByPrefix(localWorkspace.getName());
            if (localNamespace != null && Objects.equals(localNamespace.getURI(), namespace.getURI())) {
                return localNamespace;
            }
        }
        return null;
    }

    /**
     * Retrieves the namespace associated with the current local workspace.
     *
     * @return The local {@link NamespaceInfo}, or null if no local workspace is set.
     */
    private NamespaceInfo getLocalNamespace() {
        WorkspaceInfo localWorkspace = getLocalWorkspace();
        if (localWorkspace != null) {
            return facade.getNamespaceByPrefix(localWorkspace.getName());
        }
        return null;
    }
}
