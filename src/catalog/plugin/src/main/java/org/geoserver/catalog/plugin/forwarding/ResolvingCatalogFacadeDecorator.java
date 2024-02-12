/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import com.google.common.collect.Lists;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Query;
import org.geoserver.catalog.plugin.resolving.ResolvingCatalogFacade;
import org.geoserver.catalog.plugin.resolving.ResolvingFacadeSupport;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * {@link ExtendedCatalogFacade} decorator that applies a possibly side-effect producing
 * {@link Function} to each {@link CatalogInfo} right before returning it.
 *
 * <p>
 * By default the function applied is the {@link Function#identity() identity} function, use
 * {@link #setOutboundResolver} to establish the function to apply to each object before being
 * returned.
 *
 * <p>
 * The function must accept {@code null} as argument. This {@link CatalogFacade} decorator does not
 * assume any special treatment for {@code null} objects, leaving the supplied resolving function
 * chain the freedom to return {@code null} or resolve to any other object.
 * <p>
 * Use function chaining to compose a resolving pipeline adequate to the {@link CatalogFacade}
 * implementation. For example, the following chain is appropriate for a raw catalog facade that
 * fetches new objects from a remote service, and where all {@link CatalogInfo} object references
 * (e.g. {@link StoreInfo#getWorkspace()}, etc.) are {@link ResolvingProxy} instances:
 *
 * <pre>
 * {@code
 * Catalog catalog = ...
 * Function<CatalogInfo, CatalogInfo> resolvingFunction;
 * resolvingFunction =
 *   CatalogPropertyResolver.of(catalog)
 *   .andThen(ResolvingProxyResolver.of(catalog)
 *   .andThen(CollectionPropertiesInitializer.instance())
 *   .andThen(ModificationProxyDecorator.wrap());
 *
 * ResolvingCatalogFacade facade = ...
 * facade.setOutboundResolver(resolvingFunction);
 * facade.setInboundResolver(ModificationProxyDecorator.unwrap());
 * }
 *
 * Will first set the catalog property if the object type requires it (e.g.
 * {@link ResourceInfo#setCatalog}), then resolve all {@link ResolvingProxy} proxied references,
 * then initialize collection properties that are {@code null} to empty collections, and finally
 * decorate the object with a {@link ModificationProxy}.
 * <p>
 * Note the caller is responsible of supplying a resolving function that utilizes the correct
 * {@link Catalog}, may some of the functions in the chain require one; {@link #setOutboundResolver}
 * is agnostic of such concerns.
 */
public class ResolvingCatalogFacadeDecorator extends ForwardingExtendedCatalogFacade
        implements ResolvingCatalogFacade {

    private ResolvingFacadeSupport<CatalogInfo> resolver;

    public ResolvingCatalogFacadeDecorator(ExtendedCatalogFacade facade) {
        super(facade);
        resolver = new ResolvingFacadeSupport<>();
    }

    @Override
    public void setOutboundResolver(UnaryOperator<CatalogInfo> resolvingFunction) {
        resolver.setOutboundResolver(resolvingFunction);
    }

    @Override
    public UnaryOperator<CatalogInfo> getOutboundResolver() {
        return resolver.getOutboundResolver();
    }

    @Override
    public void setInboundResolver(UnaryOperator<CatalogInfo> resolvingFunction) {
        resolver.setInboundResolver(resolvingFunction);
    }

    @Override
    public UnaryOperator<CatalogInfo> getInboundResolver() {
        return resolver.getInboundResolver();
    }

    @Override
    public <C extends CatalogInfo> C resolveOutbound(C info) {
        return resolver.resolveOutbound(info);
    }

    @Override
    public <C extends CatalogInfo> C resolveInbound(C info) {
        return resolver.resolveInbound(info);
    }

    protected <C extends CatalogInfo> List<C> resolveOutbound(List<C> info) {
        return Lists.transform(info, this::resolveOutbound);
    }

    @Override
    public <I extends CatalogInfo> I update(I info, Patch patch) {
        return resolveOutbound(super.update(resolveInbound(info), patch));
    }

    @Override
    public StoreInfo add(StoreInfo store) {
        return resolveOutbound(super.add(resolveInbound(store)));
    }

    @Override
    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return resolveOutbound(super.getStore(id, clazz));
    }

    @Override
    public <T extends StoreInfo> T getStoreByName(
            WorkspaceInfo workspace, String name, Class<T> clazz) {
        return resolveOutbound(super.getStoreByName(workspace, name, clazz));
    }

    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {
        return resolveOutbound(super.getStoresByWorkspace(workspace, clazz));
    }

    @Override
    public <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return resolveOutbound(super.getStores(clazz));
    }

    @Override
    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return resolveOutbound(super.getDefaultDataStore(workspace));
    }

    @Override
    public ResourceInfo add(ResourceInfo resource) {
        return resolveOutbound(super.add(resolveInbound(resource)));
    }

    @Override
    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return resolveOutbound(super.getResource(id, clazz));
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(
            NamespaceInfo namespace, String name, Class<T> clazz) {
        return resolveOutbound(super.getResourceByName(namespace, name, clazz));
    }

    @Override
    public <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return resolveOutbound(super.getResources(clazz));
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(
            NamespaceInfo namespace, Class<T> clazz) {
        return resolveOutbound(super.getResourcesByNamespace(namespace, clazz));
    }

    @Override
    public <T extends ResourceInfo> T getResourceByStore(
            StoreInfo store, String name, Class<T> clazz) {
        return resolveOutbound(super.getResourceByStore(store, name, clazz));
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz) {
        return resolveOutbound(super.getResourcesByStore(store, clazz));
    }

    @Override
    public LayerInfo add(LayerInfo layer) {
        return resolveOutbound(super.add(resolveInbound(layer)));
    }

    @Override
    public LayerInfo getLayer(String id) {
        return resolveOutbound(super.getLayer(id));
    }

    @Override
    public LayerInfo getLayerByName(String name) {
        return resolveOutbound(super.getLayerByName(name));
    }

    @Override
    public List<LayerInfo> getLayers(ResourceInfo resource) {
        return resolveOutbound(super.getLayers(resource));
    }

    @Override
    public List<LayerInfo> getLayers(StyleInfo style) {
        return resolveOutbound(super.getLayers(style));
    }

    @Override
    public List<LayerInfo> getLayers() {
        return resolveOutbound(super.getLayers());
    }

    @Override
    public MapInfo add(MapInfo map) {
        return resolveOutbound(super.add(resolveInbound(map)));
    }

    @Override
    public MapInfo getMap(String id) {
        return resolveOutbound(super.getMap(id));
    }

    @Override
    public MapInfo getMapByName(String name) {
        return resolveOutbound(super.getMapByName(name));
    }

    @Override
    public List<MapInfo> getMaps() {
        return resolveOutbound(super.getMaps());
    }

    @Override
    public LayerGroupInfo add(LayerGroupInfo layerGroup) {
        return resolveOutbound(super.add(resolveInbound(layerGroup)));
    }

    @Override
    public LayerGroupInfo getLayerGroup(String id) {
        return resolveOutbound(super.getLayerGroup(id));
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(String name) {
        return resolveOutbound(super.getLayerGroupByName(name));
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        return resolveOutbound(super.getLayerGroupByName(workspace, name));
    }

    @Override
    public List<LayerGroupInfo> getLayerGroups() {
        return resolveOutbound(super.getLayerGroups());
    }

    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        return resolveOutbound(super.getLayerGroupsByWorkspace(workspace));
    }

    @Override
    public NamespaceInfo add(NamespaceInfo namespace) {
        return resolveOutbound(super.add(resolveInbound(namespace)));
    }

    @Override
    public NamespaceInfo getDefaultNamespace() {
        return resolveOutbound(super.getDefaultNamespace());
    }

    @Override
    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        super.setDefaultNamespace(resolveInbound(defaultNamespace));
    }

    @Override
    public NamespaceInfo getNamespace(String id) {
        return resolveOutbound(super.getNamespace(id));
    }

    @Override
    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        return resolveOutbound(super.getNamespaceByPrefix(prefix));
    }

    @Override
    public NamespaceInfo getNamespaceByURI(String uri) {
        return resolveOutbound(super.getNamespaceByURI(uri));
    }

    @Override
    public List<NamespaceInfo> getNamespaces() {
        return resolveOutbound(super.getNamespaces());
    }

    @Override
    public WorkspaceInfo add(WorkspaceInfo workspace) {
        return resolveOutbound(super.add(resolveInbound(workspace)));
    }

    @Override
    public WorkspaceInfo getDefaultWorkspace() {
        return resolveOutbound(super.getDefaultWorkspace());
    }

    @Override
    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        super.setDefaultWorkspace(resolveInbound(workspace));
    }

    @Override
    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
        super.setDefaultDataStore(resolveInbound(workspace), resolveInbound(store));
    }

    @Override
    public WorkspaceInfo getWorkspace(String id) {
        return resolveOutbound(super.getWorkspace(id));
    }

    @Override
    public WorkspaceInfo getWorkspaceByName(String name) {
        return resolveOutbound(super.getWorkspaceByName(name));
    }

    @Override
    public List<WorkspaceInfo> getWorkspaces() {
        return resolveOutbound(super.getWorkspaces());
    }

    @Override
    public StyleInfo add(StyleInfo style) {
        return resolveOutbound(super.add(resolveInbound(style)));
    }

    @Override
    public StyleInfo getStyle(String id) {
        return resolveOutbound(super.getStyle(id));
    }

    @Override
    public StyleInfo getStyleByName(String name) {
        return resolveOutbound(super.getStyleByName(name));
    }

    @Override
    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        return resolveOutbound(super.getStyleByName(workspace, name));
    }

    @Override
    public List<StyleInfo> getStyles() {
        return resolveOutbound(super.getStyles());
    }

    @Override
    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        return resolveOutbound(super.getStylesByWorkspace(workspace));
    }

    /**
     * @deprecated as per {@link ExtendedCatalogFacade#save(WorkspaceInfo)} use {@link
     *     #update(CatalogInfo, Patch)} instead
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    public void save(WorkspaceInfo info) {
        super.save(resolveInbound(info));
    }

    /**
     * @deprecated as per {@link ExtendedCatalogFacade#save(NamespaceInfo)} use {@link
     *     #update(CatalogInfo, Patch)} instead
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    public void save(NamespaceInfo info) {
        super.save(resolveInbound(info));
    }

    /**
     * @deprecated as per {@link ExtendedCatalogFacade#save(StoreInfo)} use {@link
     *     #update(CatalogInfo, Patch)} instead
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    public void save(StoreInfo info) {
        super.save(resolveInbound(info));
    }

    /**
     * @deprecated as per {@link ExtendedCatalogFacade#save(ResourceInfo)} use {@link
     *     #update(CatalogInfo, Patch)} instead
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    public void save(ResourceInfo info) {
        super.save(resolveInbound(info));
    }

    /**
     * @deprecated as per {@link ExtendedCatalogFacade#save(LayerInfo)} use {@link
     *     #update(CatalogInfo, Patch)} instead
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    public void save(LayerInfo info) {
        super.save(resolveInbound(info));
    }

    /**
     * @deprecated as per {@link ExtendedCatalogFacade#save(LayerGroupInfo)} use {@link
     *     #update(CatalogInfo, Patch)} instead
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    public void save(LayerGroupInfo info) {
        super.save(resolveInbound(info));
    }

    /**
     * @deprecated as per {@link ExtendedCatalogFacade#save(StyleInfo)} use {@link
     *     #update(CatalogInfo, Patch)} instead
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    public void save(StyleInfo info) {
        super.save(resolveInbound(info));
    }

    /**
     * @deprecated as per {@link ExtendedCatalogFacade#save(MapInfo)} use {@link
     *     #update(CatalogInfo, Patch)} instead
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    public void save(MapInfo info) {
        super.save(resolveInbound(info));
    }

    @Override
    public void remove(WorkspaceInfo info) {
        super.remove(resolveInbound(info));
    }

    @Override
    public void remove(NamespaceInfo info) {
        super.remove(resolveInbound(info));
    }

    @Override
    public void remove(StoreInfo info) {
        super.remove(resolveInbound(info));
    }

    @Override
    public void remove(ResourceInfo info) {
        super.remove(resolveInbound(info));
    }

    @Override
    public void remove(LayerInfo info) {
        super.remove(resolveInbound(info));
    }

    @Override
    public void remove(LayerGroupInfo info) {
        super.remove(resolveInbound(info));
    }

    @Override
    public void remove(StyleInfo info) {
        super.remove(resolveInbound(info));
    }

    @Override
    public void remove(MapInfo info) {
        super.remove(resolveInbound(info));
    }

    /**
     * @deprecated as per {@link ExtendedCatalogFacade#list()} use {@link #query(Query)} instead
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(
            Class<T> of, Filter filter, Integer offset, Integer count, SortBy... sortOrder) {

        final CloseableIterator<T> orig =
                asExtendedFacade().list(of, filter, offset, count, sortOrder);
        return CloseableIteratorAdapter.transform(orig, this::resolveOutbound);
    }

    @Override
    public <T extends CatalogInfo> Stream<T> query(Query<T> query) {
        return super.query(query).map(this::resolveOutbound).filter(Objects::nonNull);
    }
}
