/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.forwarding;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Query;
import org.geoserver.catalog.plugin.resolving.ResolvingCatalogFacade;
import org.geoserver.catalog.plugin.resolving.ResolvingFacadeSupport;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;

/**
 * A concrete decorator for {@link ExtendedCatalogFacade} implementing {@link ResolvingCatalogFacade},
 * applying inbound and outbound resolvers to {@link CatalogInfo} objects.
 *
 * <p>This class wraps an existing {@link ExtendedCatalogFacade} and uses {@link ResolvingFacadeSupport}
 * to apply configurable {@link UnaryOperator} functions to objects entering (inbound) and leaving
 * (outbound) the facade. By default, it uses the identity function for both directions, customizable via
 * {@link #setOutboundResolver(UnaryOperator)} and {@link #setInboundResolver(UnaryOperator)}. It simplifies
 * facade implementations by handling resolution logic, allowing the decorated facade to focus on raw data
 * access.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * Catalog catalog = ...;
 * ExtendedCatalogFacade rawFacade = ...;
 * ResolvingCatalogFacadeDecorator facade = new ResolvingCatalogFacadeDecorator(rawFacade);
 * UnaryOperator<CatalogInfo> resolver =
 *     CatalogPropertyResolver.of(catalog)
 *         .andThen(ResolvingProxyResolver.of(catalog))
 *         .andThen(CollectionPropertiesInitializer.instance())
 *         .andThen(ModificationProxyDecorator.wrap());
 * facade.setOutboundResolver(resolver);
 * facade.setInboundResolver(ModificationProxyDecorator.unwrap());
 * }
 * </pre>
 * In this example, outbound objects are resolved for catalog references, proxies, initialized collections,
 * and wrapped in a {@link ModificationProxy}, while inbound objects are unwrapped from proxies.
 *
 * <p>Notes:
 * <ul>
 *   <li>The provided resolvers must be null-safe, as this decorator does not enforce special null handling.</li>
 *   <li>The caller is responsible for ensuring resolvers use the correct {@link Catalog} instance if required.</li>
 * </ul>
 *
 * @since 1.0
 * @see ExtendedCatalogFacade
 * @see ResolvingCatalogFacade
 * @see ResolvingFacadeSupport
 */
public class ResolvingCatalogFacadeDecorator extends ForwardingExtendedCatalogFacade implements ResolvingCatalogFacade {

    private ResolvingFacadeSupport<CatalogInfo> resolver;

    /**
     * Constructs a new resolving decorator wrapping the provided {@link ExtendedCatalogFacade}.
     *
     * <p>Initializes the decorator with default identity resolvers for both inbound and outbound operations.
     * Use {@link #setOutboundResolver(UnaryOperator)} and {@link #setInboundResolver(UnaryOperator)} to
     * configure custom resolution logic.
     *
     * @param facade The underlying {@link ExtendedCatalogFacade} to decorate; must not be null.
     * @throws NullPointerException if {@code facade} is null.
     */
    public ResolvingCatalogFacadeDecorator(ExtendedCatalogFacade facade) {
        super(facade);
        resolver = new ResolvingFacadeSupport<>();
    }

    /**
     * Sets the outbound resolver function applied to objects before they are returned.
     *
     * <p>The resolver transforms {@link CatalogInfo} objects after they are retrieved from the underlying
     * facade but before they are returned to the caller. It must handle null inputs gracefully.
     *
     * @param resolvingFunction The {@link UnaryOperator} to apply to outbound objects; must not be null and
     *                          must accept null arguments.
     * @throws NullPointerException if {@code resolvingFunction} is null.
     * @example Setting an outbound resolver:
     *          <pre>
     *          ResolvingCatalogFacadeDecorator facade = new ResolvingCatalogFacadeDecorator(rawFacade);
     *          UnaryOperator<CatalogInfo> resolver = ModificationProxyDecorator.wrap();
     *          facade.setOutboundResolver(resolver);
     *          </pre>
     */
    @Override
    public void setOutboundResolver(UnaryOperator<CatalogInfo> resolvingFunction) {
        resolver.setOutboundResolver(resolvingFunction);
    }

    /**
     * Retrieves the current outbound resolver function.
     *
     * @return The {@link UnaryOperator} applied to outbound {@link CatalogInfo} objects; never null,
     *         defaults to {@link Function#identity()}.
     */
    @Override
    public UnaryOperator<CatalogInfo> getOutboundResolver() {
        return resolver.getOutboundResolver();
    }

    /**
     * Sets the inbound resolver function applied to objects before they are passed to the underlying facade.
     *
     * <p>The resolver transforms {@link CatalogInfo} objects received from the caller before they are
     * processed by the underlying facade (e.g., for add or update operations). It must handle null inputs
     * gracefully.
     *
     * @param resolvingFunction The {@link UnaryOperator} to apply to inbound objects; must not be null and
     *                          must accept null arguments.
     * @throws NullPointerException if {@code resolvingFunction} is null.
     * @example Setting an inbound resolver:
     *          <pre>
     *          ResolvingCatalogFacadeDecorator facade = new ResolvingCatalogFacadeDecorator(rawFacade);
     *          UnaryOperator<CatalogInfo> resolver = ModificationProxyDecorator.unwrap();
     *          facade.setInboundResolver(resolver);
     *          </pre>
     */
    @Override
    public void setInboundResolver(UnaryOperator<CatalogInfo> resolvingFunction) {
        resolver.setInboundResolver(resolvingFunction);
    }

    /**
     * Retrieves the current inbound resolver function.
     *
     * @return The {@link UnaryOperator} applied to inbound {@link CatalogInfo} objects; never null,
     *         defaults to {@link Function#identity()}.
     */
    @Override
    public UnaryOperator<CatalogInfo> getInboundResolver() {
        return resolver.getInboundResolver();
    }

    /**
     * Applies the outbound resolver to a {@link CatalogInfo} object.
     *
     * <p>Processes the object using the configured outbound resolver, which may transform it (e.g.,
     * resolving proxies) or return null.
     *
     * @param <C>  The type of {@link CatalogInfo}.
     * @param info The {@link CatalogInfo} object to resolve; may be null.
     * @return The resolved {@link CatalogInfo}, or null if the resolver returns null.
     */
    @Override
    public <C extends CatalogInfo> C resolveOutbound(C info) {
        return resolver.resolveOutbound(info);
    }

    /**
     * Applies the inbound resolver to a {@link CatalogInfo} object.
     *
     * <p>Processes the object using the configured inbound resolver, which may transform it (e.g.,
     * unwrapping proxies) or return null.
     *
     * @param <C>  The type of {@link CatalogInfo}.
     * @param info The {@link CatalogInfo} object to resolve; may be null.
     * @return The resolved {@link CatalogInfo}, or null if the resolver returns null.
     */
    @Override
    public <C extends CatalogInfo> C resolveInbound(C info) {
        return resolver.resolveInbound(info);
    }

    /**
     * Applies the outbound resolver to a list of {@link CatalogInfo} objects.
     *
     * <p>Transforms each object in the list using {@link #resolveOutbound(CatalogInfo)}, preserving the
     * original order and allowing null results.
     *
     * @param <C>  The type of {@link CatalogInfo}.
     * @param info The list of {@link CatalogInfo} objects to resolve; must not be null.
     * @return A new list with resolved {@link CatalogInfo} objects, potentially including nulls.
     * @throws NullPointerException if {@code info} is null.
     */
    protected <C extends CatalogInfo> List<C> resolveOutbound(List<C> info) {
        return Lists.transform(info, this::resolveOutbound);
    }

    // ExtendedCatalogFacade-specific methods

    /**
     * Adds a catalog object after applying the inbound resolver.
     *
     * <p>Resolves the input object using the inbound resolver before passing it to the underlying facade’s
     * {@link ExtendedCatalogFacade#add(CatalogInfo)} method, then applies the outbound resolver to the
     * result.
     *
     * @param <T>  The type of {@link CatalogInfo} to add.
     * @param info The {@link CatalogInfo} object to add; must not be null.
     * @return The added {@link CatalogInfo} object after outbound resolution.
     * @throws NullPointerException if {@code info} is null.
     * @example Adding a resolved workspace:
     *          <pre>
     *          WorkspaceInfo ws = new WorkspaceInfoImpl();
     *          ws.setName("test");
     *          WorkspaceInfo added = facade.add(ws);
     *          </pre>
     */
    @Override
    public <T extends CatalogInfo> T add(@NonNull T info) {
        return super.add(resolveInbound(info));
    }

    /**
     * Updates a catalog object with a patch, applying both inbound and outbound resolvers.
     *
     * <p>Resolves the input object inbound, applies the patch via the underlying facade’s
     * {@link ExtendedCatalogFacade#update(CatalogInfo, Patch)}, and resolves the result outbound.
     *
     * @param <I>   The type of {@link CatalogInfo} to update.
     * @param info  The {@link CatalogInfo} object to update; must not be null.
     * @param patch The {@link Patch} containing changes; must not be null.
     * @return The updated {@link CatalogInfo} object after outbound resolution.
     * @throws NullPointerException if {@code info} or {@code patch} is null.
     */
    @Override
    public <I extends CatalogInfo> I update(I info, Patch patch) {
        return resolveOutbound(super.update(resolveInbound(info), patch));
    }

    /**
     * Removes a catalog object after applying the inbound resolver.
     *
     * <p>Resolves the input object using the inbound resolver before delegating to the underlying facade’s
     * {@link ExtendedCatalogFacade#remove(CatalogInfo)} method.
     *
     * @param info The {@link CatalogInfo} object to remove; must not be null.
     * @throws NullPointerException if {@code info} is null.
     */
    @Override
    public void remove(@NonNull CatalogInfo info) {
        super.remove(resolveInbound(info));
    }

    /**
     * Retrieves a catalog object by ID, applying the outbound resolver.
     *
     * <p>Fetches the object via the underlying facade’s {@link ExtendedCatalogFacade#get(String)} and
     * resolves it outbound.
     *
     * @param id The unique identifier of the object; must not be null.
     * @return An {@link Optional} containing the resolved {@link CatalogInfo}, or empty if not found.
     * @throws NullPointerException if {@code id} is null.
     */
    @Override
    public Optional<CatalogInfo> get(@NonNull String id) {
        return super.get(id).map(this::resolveOutbound);
    }

    /**
     * Retrieves a typed catalog object by ID, applying the outbound resolver.
     *
     * <p>Fetches the object via the underlying facade’s {@link ExtendedCatalogFacade#get(String, Class)}
     * and resolves it outbound.
     *
     * @param <T>  The type of {@link CatalogInfo} to retrieve.
     * @param id   The unique identifier of the object; must not be null.
     * @param type The class of the object to retrieve; must not be null.
     * @return An {@link Optional} containing the resolved {@link CatalogInfo}, or empty if not found.
     * @throws NullPointerException if {@code id} or {@code type} is null.
     */
    @Override
    public <T extends CatalogInfo> Optional<T> get(@NonNull String id, @NonNull Class<T> type) {
        return super.get(id, type).map(this::resolveOutbound);
    }

    /**
     * Retrieves a published object (layer or layer group) by ID, applying the outbound resolver.
     *
     * <p>Fetches the object via the underlying facade’s {@link ExtendedCatalogFacade#getPublished(String)}
     * and resolves it outbound.
     *
     * @param id The unique identifier of the published object; must not be null.
     * @return The resolved {@link PublishedInfo}, or null if not found.
     * @throws NullPointerException if {@code id} is null.
     */
    @Override
    public PublishedInfo getPublished(@NonNull String id) {
        return resolveOutbound(super.getPublished(id));
    }

    /**
     * Queries the catalog with a {@link Query}, applying the outbound resolver to results.
     *
     * <p>Delegates to the underlying facade’s {@link ExtendedCatalogFacade#query(Query)}, resolves each
     * result outbound, and filters out nulls.
     *
     * @param <T>   The type of {@link CatalogInfo} to query.
     * @param query The {@link Query} defining the criteria; must not be null.
     * @return A {@link Stream} of resolved {@link CatalogInfo} objects; never null.
     * @throws NullPointerException if {@code query} is null.
     */
    @Override
    public <T extends CatalogInfo> Stream<T> query(Query<T> query) {
        return super.query(query).map(this::resolveOutbound).filter(Objects::nonNull);
    }

    // CatalogFacade methods

    // <editor-fold desc="CatalogFacade method overrides">

    /**
     * Adds a store after applying the inbound resolver, resolving the result outbound.
     *
     * @param store The {@link StoreInfo} to add; must not be null.
     * @return The added {@link StoreInfo} after outbound resolution.
     * @throws NullPointerException if {@code store} is null.
     */
    @Override
    public StoreInfo add(StoreInfo store) {
        return resolveOutbound(super.add(resolveInbound(store)));
    }

    /**
     * Retrieves a store by ID and type, applying the outbound resolver.
     *
     * @param <T>   The type of {@link StoreInfo}.
     * @param id    The unique identifier; must not be null.
     * @param clazz The class of the store; must not be null.
     * @return The resolved {@link StoreInfo}, or null if not found.
     * @throws NullPointerException if {@code id} or {@code clazz} is null.
     */
    @Override
    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return resolveOutbound(super.getStore(id, clazz));
    }

    /**
     * Retrieves a store by name, workspace, and type, applying the outbound resolver.
     *
     * @param <T>       The type of {@link StoreInfo}.
     * @param workspace The workspace; may be null.
     * @param name      The name; must not be null.
     * @param clazz     The class of the store; must not be null.
     * @return The resolved {@link StoreInfo}, or null if not found.
     * @throws NullPointerException if {@code name} or {@code clazz} is null.
     */
    @Override
    public <T extends StoreInfo> T getStoreByName(WorkspaceInfo workspace, String name, Class<T> clazz) {
        return resolveOutbound(super.getStoreByName(workspace, name, clazz));
    }

    /**
     * Retrieves stores by workspace and type, applying the outbound resolver.
     *
     * @param <T>       The type of {@link StoreInfo}.
     * @param workspace The workspace; may be null.
     * @param clazz     The class of the stores; must not be null.
     * @return A list of resolved {@link StoreInfo} objects.
     * @throws NullPointerException if {@code clazz} is null.
     */
    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(WorkspaceInfo workspace, Class<T> clazz) {
        return resolveOutbound(super.getStoresByWorkspace(workspace, clazz));
    }

    /**
     * Retrieves all stores of a type, applying the outbound resolver.
     *
     * @param <T>   The type of {@link StoreInfo}.
     * @param clazz The class of the stores; must not be null.
     * @return A list of resolved {@link StoreInfo} objects.
     * @throws NullPointerException if {@code clazz} is null.
     */
    @Override
    public <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return resolveOutbound(super.getStores(clazz));
    }

    /**
     * Retrieves the default data store for a workspace, applying the outbound resolver.
     *
     * @param workspace The workspace; may be null.
     * @return The resolved {@link DataStoreInfo}, or null if not set.
     */
    @Override
    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return resolveOutbound(super.getDefaultDataStore(workspace));
    }

    /**
     * Adds a resource after applying the inbound resolver, resolving the result outbound.
     *
     * @param resource The {@link ResourceInfo} to add; must not be null.
     * @return The added {@link ResourceInfo} after outbound resolution.
     * @throws NullPointerException if {@code resource} is null.
     */
    @Override
    public ResourceInfo add(ResourceInfo resource) {
        return resolveOutbound(super.add(resolveInbound(resource)));
    }

    /**
     * Retrieves a resource by ID and type, applying the outbound resolver.
     *
     * @param <T>   The type of {@link ResourceInfo}.
     * @param id    The unique identifier; must not be null.
     * @param clazz The class of the resource; must not be null.
     * @return The resolved {@link ResourceInfo}, or null if not found.
     * @throws NullPointerException if {@code id} or {@code clazz} is null.
     */
    @Override
    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return resolveOutbound(super.getResource(id, clazz));
    }

    /**
     * Retrieves a resource by name, namespace, and type, applying the outbound resolver.
     *
     * @param <T>       The type of {@link ResourceInfo}.
     * @param namespace The namespace; may be null.
     * @param name      The name; must not be null.
     * @param clazz     The class of the resource; must not be null.
     * @return The resolved {@link ResourceInfo}, or null if not found.
     * @throws NullPointerException if {@code name} or {@code clazz} is null.
     */
    @Override
    public <T extends ResourceInfo> T getResourceByName(NamespaceInfo namespace, String name, Class<T> clazz) {
        return resolveOutbound(super.getResourceByName(namespace, name, clazz));
    }

    /**
     * Retrieves all resources of a type, applying the outbound resolver.
     *
     * @param <T>   The type of {@link ResourceInfo}.
     * @param clazz The class of the resources; must not be null.
     * @return A list of resolved {@link ResourceInfo} objects.
     * @throws NullPointerException if {@code clazz} is null.
     */
    @Override
    public <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return resolveOutbound(super.getResources(clazz));
    }

    /**
     * Retrieves resources by namespace and type, applying the outbound resolver.
     *
     * @param <T>       The type of {@link ResourceInfo}.
     * @param namespace The namespace; may be null.
     * @param clazz     The class of the resources; must not be null.
     * @return A list of resolved {@link ResourceInfo} objects.
     * @throws NullPointerException if {@code clazz} is null.
     */
    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(NamespaceInfo namespace, Class<T> clazz) {
        return resolveOutbound(super.getResourcesByNamespace(namespace, clazz));
    }

    /**
     * Retrieves a resource by store, name, and type, applying the outbound resolver.
     *
     * @param <T>   The type of {@link ResourceInfo}.
     * @param store The store; must not be null.
     * @param name  The name; must not be null.
     * @param clazz The class of the resource; must not be null.
     * @return The resolved {@link ResourceInfo}, or null if not found.
     * @throws NullPointerException if {@code store}, {@code name}, or {@code clazz} is null.
     */
    @Override
    public <T extends ResourceInfo> T getResourceByStore(StoreInfo store, String name, Class<T> clazz) {
        return resolveOutbound(super.getResourceByStore(store, name, clazz));
    }

    /**
     * Retrieves resources by store and type, applying the outbound resolver.
     *
     * @param <T>   The type of {@link ResourceInfo}.
     * @param store The store; must not be null.
     * @param clazz The class of the resources; must not be null.
     * @return A list of resolved {@link ResourceInfo} objects.
     * @throws NullPointerException if {@code store} or {@code clazz} is null.
     */
    @Override
    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz) {
        return resolveOutbound(super.getResourcesByStore(store, clazz));
    }

    /**
     * Adds a layer after applying the inbound resolver, resolving the result outbound.
     *
     * @param layer The {@link LayerInfo} to add; must not be null.
     * @return The added {@link LayerInfo} after outbound resolution.
     * @throws NullPointerException if {@code layer} is null.
     */
    @Override
    public LayerInfo add(LayerInfo layer) {
        return resolveOutbound(super.add(resolveInbound(layer)));
    }

    /**
     * Retrieves a layer by ID, applying the outbound resolver.
     *
     * @param id The unique identifier; must not be null.
     * @return The resolved {@link LayerInfo}, or null if not found.
     * @throws NullPointerException if {@code id} is null.
     */
    @Override
    public LayerInfo getLayer(String id) {
        return resolveOutbound(super.getLayer(id));
    }

    /**
     * Retrieves a layer by name, applying the outbound resolver.
     *
     * @param name The name; must not be null.
     * @return The resolved {@link LayerInfo}, or null if not found.
     * @throws NullPointerException if {@code name} is null.
     */
    @Override
    public LayerInfo getLayerByName(String name) {
        return resolveOutbound(super.getLayerByName(name));
    }

    /**
     * Retrieves layers by resource, applying the outbound resolver.
     *
     * @param resource The resource; must not be null.
     * @return A list of resolved {@link LayerInfo} objects.
     * @throws NullPointerException if {@code resource} is null.
     */
    @Override
    public List<LayerInfo> getLayers(ResourceInfo resource) {
        return resolveOutbound(super.getLayers(resource));
    }

    /**
     * Retrieves layers by style, applying the outbound resolver.
     *
     * @param style The style; must not be null.
     * @return A list of resolved {@link LayerInfo} objects.
     * @throws NullPointerException if {@code style} is null.
     */
    @Override
    public List<LayerInfo> getLayers(StyleInfo style) {
        return resolveOutbound(super.getLayers(style));
    }

    /**
     * Retrieves all layers, applying the outbound resolver.
     *
     * @return A list of resolved {@link LayerInfo} objects.
     */
    @Override
    public List<LayerInfo> getLayers() {
        return resolveOutbound(super.getLayers());
    }

    /**
     * Adds a map after applying the inbound resolver, resolving the result outbound.
     *
     * @param map The {@link MapInfo} to add; must not be null.
     * @return The added {@link MapInfo} after outbound resolution.
     * @throws NullPointerException if {@code map} is null.
     */
    @Override
    public MapInfo add(MapInfo map) {
        return resolveOutbound(super.add(resolveInbound(map)));
    }

    /**
     * Retrieves a map by ID, applying the outbound resolver.
     *
     * @param id The unique identifier; must not be null.
     * @return The resolved {@link MapInfo}, or null if not found.
     * @throws NullPointerException if {@code id} is null.
     */
    @Override
    public MapInfo getMap(String id) {
        return resolveOutbound(super.getMap(id));
    }

    /**
     * Retrieves a map by name, applying the outbound resolver.
     *
     * @param name The name; must not be null.
     * @return The resolved {@link MapInfo}, or null if not found.
     * @throws NullPointerException if {@code name} is null.
     */
    @Override
    public MapInfo getMapByName(String name) {
        return resolveOutbound(super.getMapByName(name));
    }

    /**
     * Retrieves all maps, applying the outbound resolver.
     *
     * @return A list of resolved {@link MapInfo} objects.
     */
    @Override
    public List<MapInfo> getMaps() {
        return resolveOutbound(super.getMaps());
    }

    /**
     * Adds a layer group after applying the inbound resolver, resolving the result outbound.
     *
     * @param layerGroup The {@link LayerGroupInfo} to add; must not be null.
     * @return The added {@link LayerGroupInfo} after outbound resolution.
     * @throws NullPointerException if {@code layerGroup} is null.
     */
    @Override
    public LayerGroupInfo add(LayerGroupInfo layerGroup) {
        return resolveOutbound(super.add(resolveInbound(layerGroup)));
    }

    /**
     * Retrieves a layer group by ID, applying the outbound resolver.
     *
     * @param id The unique identifier; must not be null.
     * @return The resolved {@link LayerGroupInfo}, or null if not found.
     * @throws NullPointerException if {@code id} is null.
     */
    @Override
    public LayerGroupInfo getLayerGroup(String id) {
        return resolveOutbound(super.getLayerGroup(id));
    }

    /**
     * Retrieves a layer group by name, applying the outbound resolver.
     *
     * @param name The name; must not be null.
     * @return The resolved {@link LayerGroupInfo}, or null if not found.
     * @throws NullPointerException if {@code name} is null.
     */
    @Override
    public LayerGroupInfo getLayerGroupByName(String name) {
        return resolveOutbound(super.getLayerGroupByName(name));
    }

    /**
     * Retrieves a layer group by name and workspace, applying the outbound resolver.
     *
     * @param workspace The workspace; may be null.
     * @param name      The name; must not be null.
     * @return The resolved {@link LayerGroupInfo}, or null if not found.
     * @throws NullPointerException if {@code name} is null.
     */
    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        return resolveOutbound(super.getLayerGroupByName(workspace, name));
    }

    /**
     * Retrieves all layer groups, applying the outbound resolver.
     *
     * @return A list of resolved {@link LayerGroupInfo} objects.
     */
    @Override
    public List<LayerGroupInfo> getLayerGroups() {
        return resolveOutbound(super.getLayerGroups());
    }

    /**
     * Retrieves layer groups by workspace, applying the outbound resolver.
     *
     * @param workspace The workspace; may be null.
     * @return A list of resolved {@link LayerGroupInfo} objects.
     */
    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        return resolveOutbound(super.getLayerGroupsByWorkspace(workspace));
    }

    /**
     * Adds a namespace after applying the inbound resolver, resolving the result outbound.
     *
     * @param namespace The {@link NamespaceInfo} to add; must not be null.
     * @return The added {@link NamespaceInfo} after outbound resolution.
     * @throws NullPointerException if {@code namespace} is null.
     */
    @Override
    public NamespaceInfo add(NamespaceInfo namespace) {
        return resolveOutbound(super.add(resolveInbound(namespace)));
    }

    /**
     * Retrieves the default namespace, applying the outbound resolver.
     *
     * @return The resolved {@link NamespaceInfo}, or null if not set.
     */
    @Override
    public NamespaceInfo getDefaultNamespace() {
        return resolveOutbound(super.getDefaultNamespace());
    }

    /**
     * Sets the default namespace after applying the inbound resolver.
     *
     * @param defaultNamespace The {@link NamespaceInfo} to set as default; may be null.
     */
    @Override
    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        super.setDefaultNamespace(resolveInbound(defaultNamespace));
    }

    /**
     * Retrieves a namespace by ID, applying the outbound resolver.
     *
     * @param id The unique identifier; must not be null.
     * @return The resolved {@link NamespaceInfo}, or null if not found.
     * @throws NullPointerException if {@code id} is null.
     */
    @Override
    public NamespaceInfo getNamespace(String id) {
        return resolveOutbound(super.getNamespace(id));
    }

    /**
     * Retrieves a namespace by prefix, applying the outbound resolver.
     *
     * @param prefix The prefix; must not be null.
     * @return The resolved {@link NamespaceInfo}, or null if not found.
     * @throws NullPointerException if {@code prefix} is null.
     */
    @Override
    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        return resolveOutbound(super.getNamespaceByPrefix(prefix));
    }

    /**
     * Retrieves a namespace by URI, applying the outbound resolver.
     *
     * @param uri The URI; must not be null.
     * @return The resolved {@link NamespaceInfo}, or null if not found.
     * @throws NullPointerException if {@code uri} is null.
     */
    @Override
    public NamespaceInfo getNamespaceByURI(String uri) {
        return resolveOutbound(super.getNamespaceByURI(uri));
    }

    /**
     * Retrieves all namespaces, applying the outbound resolver.
     *
     * @return A list of resolved {@link NamespaceInfo} objects.
     */
    @Override
    public List<NamespaceInfo> getNamespaces() {
        return resolveOutbound(super.getNamespaces());
    }

    /**
     * Adds a workspace after applying the inbound resolver, resolving the result outbound.
     *
     * @param workspace The {@link WorkspaceInfo} to add; must not be null.
     * @return The added {@link WorkspaceInfo} after outbound resolution.
     * @throws NullPointerException if {@code workspace} is null.
     */
    @Override
    public WorkspaceInfo add(WorkspaceInfo workspace) {
        return resolveOutbound(super.add(resolveInbound(workspace)));
    }

    /**
     * Retrieves the default workspace, applying the outbound resolver.
     *
     * @return The resolved {@link WorkspaceInfo}, or null if not set.
     */
    @Override
    public WorkspaceInfo getDefaultWorkspace() {
        return resolveOutbound(super.getDefaultWorkspace());
    }

    /**
     * Sets the default workspace after applying the inbound resolver.
     *
     * @param workspace The {@link WorkspaceInfo} to set as default; may be null.
     */
    @Override
    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        super.setDefaultWorkspace(resolveInbound(workspace));
    }

    /**
     * Sets the default data store for a workspace, applying the inbound resolver to inputs.
     *
     * @param workspace The workspace; must not be null.
     * @param store     The {@link DataStoreInfo} to set; may be null.
     * @throws NullPointerException if {@code workspace} is null.
     */
    @Override
    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
        super.setDefaultDataStore(resolveInbound(workspace), resolveInbound(store));
    }

    /**
     * Retrieves a workspace by ID, applying the outbound resolver.
     *
     * @param id The unique identifier; must not be null.
     * @return The resolved {@link WorkspaceInfo}, or null if not found.
     * @throws NullPointerException if {@code id} is null.
     */
    @Override
    public WorkspaceInfo getWorkspace(String id) {
        return resolveOutbound(super.getWorkspace(id));
    }

    /**
     * Retrieves a workspace by name, applying the outbound resolver.
     *
     * @param name The name; must not be null.
     * @return The resolved {@link WorkspaceInfo}, or null if not found.
     * @throws NullPointerException if {@code name} is null.
     */
    @Override
    public WorkspaceInfo getWorkspaceByName(String name) {
        return resolveOutbound(super.getWorkspaceByName(name));
    }

    /**
     * Retrieves all workspaces, applying the outbound resolver.
     *
     * @return A list of resolved {@link WorkspaceInfo} objects.
     */
    @Override
    public List<WorkspaceInfo> getWorkspaces() {
        return resolveOutbound(super.getWorkspaces());
    }

    /**
     * Adds a style after applying the inbound resolver, resolving the result outbound.
     *
     * @param style The {@link StyleInfo} to add; must not be null.
     * @return The added {@link StyleInfo} after outbound resolution.
     * @throws NullPointerException if {@code style} is null.
     */
    @Override
    public StyleInfo add(StyleInfo style) {
        return resolveOutbound(super.add(resolveInbound(style)));
    }

    /**
     * Retrieves a style by ID, applying the outbound resolver.
     *
     * @param id The unique identifier; must not be null.
     * @return The resolved {@link StyleInfo}, or null if not found.
     * @throws NullPointerException if {@code id} is null.
     */
    @Override
    public StyleInfo getStyle(String id) {
        return resolveOutbound(super.getStyle(id));
    }

    /**
     * Retrieves a style by name, applying the outbound resolver.
     *
     * @param name The name; must not be null.
     * @return The resolved {@link StyleInfo}, or null if not found.
     * @throws NullPointerException if {@code name} is null.
     */
    @Override
    public StyleInfo getStyleByName(String name) {
        return resolveOutbound(super.getStyleByName(name));
    }

    /**
     * Retrieves a style by name and workspace, applying the outbound resolver.
     *
     * @param workspace The workspace; may be null.
     * @param name      The name; must not be null.
     * @return The resolved {@link StyleInfo}, or null if not found.
     * @throws NullPointerException if {@code name} is null.
     */
    @Override
    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        return resolveOutbound(super.getStyleByName(workspace, name));
    }

    /**
     * Retrieves all styles, applying the outbound resolver.
     *
     * @return A list of resolved {@link StyleInfo} objects.
     */
    @Override
    public List<StyleInfo> getStyles() {
        return resolveOutbound(super.getStyles());
    }

    /**
     * Retrieves styles by workspace, applying the outbound resolver.
     *
     * @param workspace The workspace; may be null.
     * @return A list of resolved {@link StyleInfo} objects.
     */
    @Override
    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        return resolveOutbound(super.getStylesByWorkspace(workspace));
    }

    /**
     * Saves a workspace after applying the inbound resolver (deprecated).
     *
     * <p>Resolves the input workspace inbound before delegating to the underlying facade’s deprecated
     * {@link ExtendedCatalogFacade#save(WorkspaceInfo)} method.
     *
     * @param info The {@link WorkspaceInfo} to save; must not be null.
     * @throws NullPointerException if {@code info} is null.
     * @deprecated since 1.0, for removal; use {@link #update(CatalogInfo, Patch)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    public void save(WorkspaceInfo info) {
        super.save(resolveInbound(info));
    }

    /**
     * Saves a namespace after applying the inbound resolver (deprecated).
     *
     * @param info The {@link NamespaceInfo} to save; must not be null.
     * @throws NullPointerException if {@code info} is null.
     * @deprecated since 1.0, for removal; use {@link #update(CatalogInfo, Patch)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    public void save(NamespaceInfo info) {
        super.save(resolveInbound(info));
    }

    /**
     * Saves a store after applying the inbound resolver (deprecated).
     *
     * @param info The {@link StoreInfo} to save; must not be null.
     * @throws NullPointerException if {@code info} is null.
     * @deprecated since 1.0, for removal; use {@link #update(CatalogInfo, Patch)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    public void save(StoreInfo info) {
        super.save(resolveInbound(info));
    }

    /**
     * Saves a resource after applying the inbound resolver (deprecated).
     *
     * @param info The {@link ResourceInfo} to save; must not be null.
     * @throws NullPointerException if {@code info} is null.
     * @deprecated since 1.0, for removal; use {@link #update(CatalogInfo, Patch)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    public void save(ResourceInfo info) {
        super.save(resolveInbound(info));
    }

    /**
     * Saves a layer after applying the inbound resolver (deprecated).
     *
     * @param info The {@link LayerInfo} to save; must not be null.
     * @throws NullPointerException if {@code info} is null.
     * @deprecated since 1.0, for removal; use {@link #update(CatalogInfo, Patch)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    public void save(LayerInfo info) {
        super.save(resolveInbound(info));
    }

    /**
     * Saves a layer group after applying the inbound resolver (deprecated).
     *
     * @param info The {@link LayerGroupInfo} to save; must not be null.
     * @throws NullPointerException if {@code info} is null.
     * @deprecated since 1.0, for removal; use {@link #update(CatalogInfo, Patch)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    public void save(LayerGroupInfo info) {
        super.save(resolveInbound(info));
    }

    /**
     * Saves a style after applying the inbound resolver (deprecated).
     *
     * @param info The {@link StyleInfo} to save; must not be null.
     * @throws NullPointerException if {@code info} is null.
     * @deprecated since 1.0, for removal; use {@link #update(CatalogInfo, Patch)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    public void save(StyleInfo info) {
        super.save(resolveInbound(info));
    }

    /**
     * Saves a map after applying the inbound resolver (deprecated).
     *
     * @param info The {@link MapInfo} to save; must not be null.
     * @throws NullPointerException if {@code info} is null.
     * @deprecated since 1.0, for removal; use {@link #update(CatalogInfo, Patch)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    public void save(MapInfo info) {
        super.save(resolveInbound(info));
    }

    /**
     * Removes a workspace after applying the inbound resolver.
     *
     * @param info The {@link WorkspaceInfo} to remove; must not be null.
     * @throws NullPointerException if {@code info} is null.
     */
    @Override
    public void remove(WorkspaceInfo info) {
        super.remove(resolveInbound(info));
    }

    /**
     * Removes a namespace after applying the inbound resolver.
     *
     * @param info The {@link NamespaceInfo} to remove; must not be null.
     * @throws NullPointerException if {@code info} is null.
     */
    @Override
    public void remove(NamespaceInfo info) {
        super.remove(resolveInbound(info));
    }

    /**
     * Removes a store after applying the inbound resolver.
     *
     * @param info The {@link StoreInfo} to remove; must not be null.
     * @throws NullPointerException if {@code info} is null.
     */
    @Override
    public void remove(StoreInfo info) {
        super.remove(resolveInbound(info));
    }

    /**
     * Removes a resource after applying the inbound resolver.
     *
     * @param info The {@link ResourceInfo} to remove; must not be null.
     * @throws NullPointerException if {@code info} is null.
     */
    @Override
    public void remove(ResourceInfo info) {
        super.remove(resolveInbound(info));
    }

    /**
     * Removes a layer after applying the inbound resolver.
     *
     * @param info The {@link LayerInfo} to remove; must not be null.
     * @throws NullPointerException if {@code info} is null.
     */
    @Override
    public void remove(LayerInfo info) {
        super.remove(resolveInbound(info));
    }

    /**
     * Removes a layer group after applying the inbound resolver.
     *
     * @param info The {@link LayerGroupInfo} to remove; must not be null.
     * @throws NullPointerException if {@code info} is null.
     */
    @Override
    public void remove(LayerGroupInfo info) {
        super.remove(resolveInbound(info));
    }

    /**
     * Removes a style after applying the inbound resolver.
     *
     * @param info The {@link StyleInfo} to remove; must not be null.
     * @throws NullPointerException if {@code info} is null.
     */
    @Override
    public void remove(StyleInfo info) {
        super.remove(resolveInbound(info));
    }

    /**
     * Removes a map after applying the inbound resolver.
     *
     * @param info The {@link MapInfo} to remove; must not be null.
     * @throws NullPointerException if {@code info} is null.
     */
    @Override
    public void remove(MapInfo info) {
        super.remove(resolveInbound(info));
    }

    /**
     * Retrieves a list of catalog objects matching criteria, applying the outbound resolver (deprecated).
     *
     * <p>Delegates to the underlying facade’s deprecated {@link ExtendedCatalogFacade#list(Class, Filter, Integer, Integer, SortBy...)}
     * method and transforms the result iterator with the outbound resolver.
     *
     * @param <T>       The type of {@link CatalogInfo}.
     * @param of        The class of objects; must not be null.
     * @param filter    The filter; must not be null.
     * @param offset    The offset; may be null.
     * @param count     The count; may be null.
     * @param sortOrder The sort order; may be null.
     * @return A {@link CloseableIterator} of resolved objects.
     * @throws NullPointerException if {@code of} or {@code filter} is null.
     * @deprecated since 1.0, for removal; use {@link #query(Query)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(
            Class<T> of, Filter filter, Integer offset, Integer count, SortBy... sortOrder) {

        final CloseableIterator<T> orig = asExtendedFacade().list(of, filter, offset, count, sortOrder);
        return CloseableIteratorAdapter.transform(orig, this::resolveOutbound);
    }

    // </editor-fold>
}
