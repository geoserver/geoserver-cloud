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
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
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

    private Function<CatalogInfo, CatalogInfo> outboundResolver = Function.identity();
    private Function<CatalogInfo, CatalogInfo> inboundResolver = Function.identity();

    public ResolvingCatalogFacadeDecorator(ExtendedCatalogFacade facade) {
        super(facade);
    }

    /**
     * Function applied to all outgoing {@link CatalogInfo} objects returned by the decorated facade
     * before leaving this decorator facade
     */
    public @Override void setOutboundResolver(
            Function<CatalogInfo, CatalogInfo> resolvingFunction) {
        Objects.requireNonNull(resolvingFunction);
        this.outboundResolver = resolvingFunction;
    }

    /**
     * Function applied to all incoming {@link CatalogInfo} objects before deferring to the
     * decorated facade
     *
     * <p>Use {@code facade.setOutboundResolver(facade.getOutboundResolver().andThen(myFunction))}
     * to add traits to the current resolver; for example, a filtering trait could be added this way
     * to filter out objects based on some externally defined conditions, returning {@code null} if
     * an object is to be discarded from the final outcome
     */
    public @Override Function<CatalogInfo, CatalogInfo> getOutboundResolver() {
        return this.outboundResolver;
    }

    /**
     * Function applied to all incoming {@link CatalogInfo} objects before deferring to the
     * decorated facade
     */
    public @Override void setInboundResolver(Function<CatalogInfo, CatalogInfo> resolvingFunction) {
        Objects.requireNonNull(resolvingFunction);
        this.inboundResolver = resolvingFunction;
    }

    /**
     * Function applied to all incoming {@link CatalogInfo} objects before deferring to the
     * decorated facade.
     *
     * <p>Use {@code facade.setInboundResolver(facade.getInboundResolver().andThen(myFunction))} to
     * add traits to the current resolver
     */
    public @Override Function<CatalogInfo, CatalogInfo> getInboundResolver() {
        return this.inboundResolver;
    }

    @SuppressWarnings("unchecked")
    protected <I extends CatalogInfo> Function<I, I> outbound() {
        return (Function<I, I>) outboundResolver;
    }

    @SuppressWarnings("unchecked")
    protected <I extends CatalogInfo> Function<I, I> inbound() {
        return (Function<I, I>) inboundResolver;
    }

    public @Override <C extends CatalogInfo> C resolveOutbound(C info) {
        Function<C, C> outboundResolve = outbound();
        return outboundResolve.apply(info);
    }

    public @Override <C extends CatalogInfo> C resolveInbound(C info) {
        Function<C, C> inboundResolve = inbound();
        return inboundResolve.apply(info);
    }

    protected <C extends CatalogInfo> List<C> resolveOutbound(List<C> info) {
        return Lists.transform(info, this::resolveOutbound);
    }

    public @Override <I extends CatalogInfo> I update(I info, Patch patch) {
        return resolveOutbound(super.update(resolveInbound(info), patch));
    }

    public @Override StoreInfo add(StoreInfo store) {
        return resolveOutbound(super.add(resolveInbound(store)));
    }

    public @Override <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return resolveOutbound(super.getStore(id, clazz));
    }

    public @Override <T extends StoreInfo> T getStoreByName(
            WorkspaceInfo workspace, String name, Class<T> clazz) {
        return resolveOutbound(super.getStoreByName(workspace, name, clazz));
    }

    public @Override <T extends StoreInfo> List<T> getStoresByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {
        return resolveOutbound(super.getStoresByWorkspace(workspace, clazz));
    }

    public @Override <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return resolveOutbound(super.getStores(clazz));
    }

    public @Override DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return resolveOutbound(super.getDefaultDataStore(workspace));
    }

    public @Override ResourceInfo add(ResourceInfo resource) {
        return resolveOutbound(super.add(resolveInbound(resource)));
    }

    public @Override <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return resolveOutbound(super.getResource(id, clazz));
    }

    public @Override <T extends ResourceInfo> T getResourceByName(
            NamespaceInfo namespace, String name, Class<T> clazz) {
        return resolveOutbound(super.getResourceByName(namespace, name, clazz));
    }

    public @Override <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return resolveOutbound(super.getResources(clazz));
    }

    public @Override <T extends ResourceInfo> List<T> getResourcesByNamespace(
            NamespaceInfo namespace, Class<T> clazz) {
        return resolveOutbound(super.getResourcesByNamespace(namespace, clazz));
    }

    public @Override <T extends ResourceInfo> T getResourceByStore(
            StoreInfo store, String name, Class<T> clazz) {
        return resolveOutbound(super.getResourceByStore(store, name, clazz));
    }

    public @Override <T extends ResourceInfo> List<T> getResourcesByStore(
            StoreInfo store, Class<T> clazz) {
        return resolveOutbound(super.getResourcesByStore(store, clazz));
    }

    public @Override LayerInfo add(LayerInfo layer) {
        return resolveOutbound(super.add(resolveInbound(layer)));
    }

    public @Override LayerInfo getLayer(String id) {
        return resolveOutbound(super.getLayer(id));
    }

    public @Override LayerInfo getLayerByName(String name) {
        return resolveOutbound(super.getLayerByName(name));
    }

    public @Override List<LayerInfo> getLayers(ResourceInfo resource) {
        return resolveOutbound(super.getLayers(resource));
    }

    public @Override List<LayerInfo> getLayers(StyleInfo style) {
        return resolveOutbound(super.getLayers(style));
    }

    public @Override List<LayerInfo> getLayers() {
        return resolveOutbound(super.getLayers());
    }

    public @Override MapInfo add(MapInfo map) {
        return resolveOutbound(super.add(resolveInbound(map)));
    }

    public @Override MapInfo getMap(String id) {
        return resolveOutbound(super.getMap(id));
    }

    public @Override MapInfo getMapByName(String name) {
        return resolveOutbound(super.getMapByName(name));
    }

    public @Override List<MapInfo> getMaps() {
        return resolveOutbound(super.getMaps());
    }

    public @Override LayerGroupInfo add(LayerGroupInfo layerGroup) {
        return resolveOutbound(super.add(resolveInbound(layerGroup)));
    }

    public @Override LayerGroupInfo getLayerGroup(String id) {
        return resolveOutbound(super.getLayerGroup(id));
    }

    public @Override LayerGroupInfo getLayerGroupByName(String name) {
        return resolveOutbound(super.getLayerGroupByName(name));
    }

    public @Override LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        return resolveOutbound(super.getLayerGroupByName(workspace, name));
    }

    public @Override List<LayerGroupInfo> getLayerGroups() {
        return resolveOutbound(super.getLayerGroups());
    }

    public @Override List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        return resolveOutbound(super.getLayerGroupsByWorkspace(workspace));
    }

    public @Override NamespaceInfo add(NamespaceInfo namespace) {
        return resolveOutbound(super.add(resolveInbound(namespace)));
    }

    public @Override NamespaceInfo getDefaultNamespace() {
        return resolveOutbound(super.getDefaultNamespace());
    }

    public @Override void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        super.setDefaultNamespace(resolveInbound(defaultNamespace));
    }

    public @Override NamespaceInfo getNamespace(String id) {
        return resolveOutbound(super.getNamespace(id));
    }

    public @Override NamespaceInfo getNamespaceByPrefix(String prefix) {
        return resolveOutbound(super.getNamespaceByPrefix(prefix));
    }

    public @Override NamespaceInfo getNamespaceByURI(String uri) {
        return resolveOutbound(super.getNamespaceByURI(uri));
    }

    public @Override List<NamespaceInfo> getNamespaces() {
        return resolveOutbound(super.getNamespaces());
    }

    public @Override WorkspaceInfo add(WorkspaceInfo workspace) {
        return resolveOutbound(super.add(resolveInbound(workspace)));
    }

    public @Override WorkspaceInfo getDefaultWorkspace() {
        return resolveOutbound(super.getDefaultWorkspace());
    }

    public @Override void setDefaultWorkspace(WorkspaceInfo workspace) {
        super.setDefaultWorkspace(resolveInbound(workspace));
    }

    public @Override WorkspaceInfo getWorkspace(String id) {
        return resolveOutbound(super.getWorkspace(id));
    }

    public @Override WorkspaceInfo getWorkspaceByName(String name) {
        return resolveOutbound(super.getWorkspaceByName(name));
    }

    public @Override List<WorkspaceInfo> getWorkspaces() {
        return resolveOutbound(super.getWorkspaces());
    }

    public @Override StyleInfo add(StyleInfo style) {
        return resolveOutbound(super.add(resolveInbound(style)));
    }

    public @Override StyleInfo getStyle(String id) {
        return resolveOutbound(super.getStyle(id));
    }

    public @Override StyleInfo getStyleByName(String name) {
        return resolveOutbound(super.getStyleByName(name));
    }

    public @Override StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        return resolveOutbound(super.getStyleByName(workspace, name));
    }

    public @Override List<StyleInfo> getStyles() {
        return resolveOutbound(super.getStyles());
    }

    public @Override List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        return resolveOutbound(super.getStylesByWorkspace(workspace));
    }

    public @Override void save(WorkspaceInfo info) {
        super.save(resolveInbound(info));
    }

    public @Override void save(NamespaceInfo info) {
        super.save(resolveInbound(info));
    }

    public @Override void save(StoreInfo info) {
        super.save(resolveInbound(info));
    }

    public @Override void save(ResourceInfo info) {
        super.save(resolveInbound(info));
    }

    public @Override void save(LayerInfo info) {
        super.save(resolveInbound(info));
    }

    public @Override void save(LayerGroupInfo info) {
        super.save(resolveInbound(info));
    }

    public @Override void save(StyleInfo info) {
        super.save(resolveInbound(info));
    }

    public @Override void save(MapInfo info) {
        super.save(resolveInbound(info));
    }

    public @Override void remove(WorkspaceInfo info) {
        super.remove(resolveInbound(info));
    }

    public @Override void remove(NamespaceInfo info) {
        super.remove(resolveInbound(info));
    }

    public @Override void remove(StoreInfo info) {
        super.remove(resolveInbound(info));
    }

    public @Override void remove(ResourceInfo info) {
        super.remove(resolveInbound(info));
    }

    public @Override void remove(LayerInfo info) {
        super.remove(resolveInbound(info));
    }

    public @Override void remove(LayerGroupInfo info) {
        super.remove(resolveInbound(info));
    }

    public @Override void remove(StyleInfo info) {
        super.remove(resolveInbound(info));
    }

    public @Override void remove(MapInfo info) {
        super.remove(resolveInbound(info));
    }

    public @Override <T extends CatalogInfo> CloseableIterator<T> list(
            Class<T> of, Filter filter, Integer offset, Integer count, SortBy... sortOrder) {

        @SuppressWarnings("deprecation")
        final CloseableIterator<T> orig = facade().list(of, filter, offset, count, sortOrder);
        return CloseableIteratorAdapter.transform(orig, this::resolveOutbound);
    }

    public @Override <T extends CatalogInfo> Stream<T> query(Query<T> query) {
        return super.query(query).map(this::resolveOutbound).filter(i -> i != null);
    }
}
