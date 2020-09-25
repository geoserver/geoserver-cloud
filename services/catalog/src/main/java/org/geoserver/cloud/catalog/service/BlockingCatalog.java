/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.service;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogCapabilities;
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
import org.geoserver.catalog.impl.AbstractCatalogDecorator;
import org.geoserver.catalog.plugin.CatalogImpl;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Query;
import org.geoserver.catalog.plugin.forwarding.ForwardingCatalogFacade;
import org.geoserver.catalog.util.CloseableIterator;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** */
@Component
@Slf4j
@SuppressWarnings("serial")
public class BlockingCatalog extends AbstractCatalogDecorator {

    private static class AllowAllCatalogFacade extends ForwardingCatalogFacade {
        /**
         * {@link CatalogCapabilities#supportsIsolatedWorkspaces()} is {@code true} to allow
         * bypassing the workspace validation check in {@link CatalogImpl#validate(WorkspaceInfo,
         * boolean)}, that otherwise would throw an {@link IllegalArgumentException} when creating
         * or updating an isolated workspace, but this service has to work as a raw catalog.
         */
        private static final CatalogCapabilities CATALOG_CAPABILITIES = new CatalogCapabilities();

        static {
            CATALOG_CAPABILITIES.setIsolatedWorkspacesSupport(true);
        }

        public AllowAllCatalogFacade(CatalogFacade facade) {
            super(facade);
        }

        public @Override CatalogCapabilities getCatalogCapabilities() {
            return CATALOG_CAPABILITIES;
        }
    }

    public BlockingCatalog(@Qualifier("rawCatalog") Catalog rawCatalog) {
        super(rawCatalog);
        // Make sure isolated workspaces can be created/updated
        if (rawCatalog instanceof org.geoserver.catalog.plugin.CatalogImpl) {
            CatalogImpl impl = ((org.geoserver.catalog.plugin.CatalogImpl) rawCatalog);
            impl.getRawCatalogFacade().getCatalogCapabilities().setIsolatedWorkspacesSupport(true);
        } else if (rawCatalog instanceof org.geoserver.catalog.impl.CatalogImpl) {
            org.geoserver.catalog.impl.CatalogImpl impl =
                    ((org.geoserver.catalog.impl.CatalogImpl) rawCatalog);
            impl.setFacade(new AllowAllCatalogFacade(impl.getFacade()));
        }
    }

    @SuppressWarnings("unchecked")
    public <C extends CatalogInfo> C get(String id, Class<? extends C> type) {
        if (WorkspaceInfo.class.isAssignableFrom(type)) return type.cast(delegate.getWorkspace(id));
        if (NamespaceInfo.class.isAssignableFrom(type)) return type.cast(delegate.getNamespace(id));
        if (StoreInfo.class.isAssignableFrom(type))
            return type.cast(delegate.getStore(id, (Class<? extends StoreInfo>) type));
        if (ResourceInfo.class.isAssignableFrom(type))
            return type.cast(delegate.getResource(id, (Class<? extends ResourceInfo>) type));
        if (LayerInfo.class.isAssignableFrom(type)) return type.cast(delegate.getLayer(id));
        if (LayerGroupInfo.class.isAssignableFrom(type))
            return type.cast(delegate.getLayerGroup(id));
        if (StyleInfo.class.isAssignableFrom(type)) return type.cast(delegate.getStyle(id));
        if (MapInfo.class.isAssignableFrom(type)) return type.cast(delegate.getMap(id));
        throw new IllegalArgumentException("unknown CatalogInfo class: " + type.getCanonicalName());
    }

    public <C extends CatalogInfo> C add(@NonNull C info) {
        Class<? extends CatalogInfo> type = info.getClass();
        if (WorkspaceInfo.class.isAssignableFrom(type)) delegate.add((WorkspaceInfo) info);
        else if (NamespaceInfo.class.isAssignableFrom(type)) delegate.add((NamespaceInfo) info);
        else if (StoreInfo.class.isAssignableFrom(type)) delegate.add((StoreInfo) info);
        else if (ResourceInfo.class.isAssignableFrom(type)) delegate.add((ResourceInfo) info);
        else if (LayerInfo.class.isAssignableFrom(type)) delegate.add((LayerInfo) info);
        else if (LayerGroupInfo.class.isAssignableFrom(type)) delegate.add((LayerGroupInfo) info);
        else if (StyleInfo.class.isAssignableFrom(type)) delegate.add((StyleInfo) info);
        else if (MapInfo.class.isAssignableFrom(type)) delegate.add((MapInfo) info);
        else
            throw new IllegalArgumentException(
                    "Uknown CatalogInfo type: " + type.getCanonicalName());
        return info;
    }

    public <C extends CatalogInfo> C update(@NonNull C info, @NonNull Patch patch) {

        try {
            patch.applyTo(info);
        } catch (RuntimeException e) {
            log.error("Error applying patch to {}: {}", info, patch, e);
            throw e;
        }
        // need to make it work with old catalogs too, jdbcconfig has not been upgraded for example
        Class<? extends CatalogInfo> type = info.getClass();
        try {
            if (WorkspaceInfo.class.isAssignableFrom(type)) delegate.save((WorkspaceInfo) info);
            else if (NamespaceInfo.class.isAssignableFrom(type))
                delegate.save((NamespaceInfo) info);
            else if (StoreInfo.class.isAssignableFrom(type)) delegate.save((StoreInfo) info);
            else if (ResourceInfo.class.isAssignableFrom(type)) delegate.save((ResourceInfo) info);
            else if (LayerInfo.class.isAssignableFrom(type)) delegate.save((LayerInfo) info);
            else if (LayerGroupInfo.class.isAssignableFrom(type))
                delegate.save((LayerGroupInfo) info);
            else if (StyleInfo.class.isAssignableFrom(type)) delegate.save((StyleInfo) info);
            else if (MapInfo.class.isAssignableFrom(type)) delegate.save((MapInfo) info);
            else
                throw new IllegalArgumentException(
                        "Uknown CatalogInfo type: " + type.getCanonicalName());
        } catch (RuntimeException e) {
            log.error("Error saving {} with patch {}", info, patch, e);
            throw e;
        }
        return info;
    }

    public <C extends CatalogInfo> C delete(@NonNull C info) {
        Class<? extends CatalogInfo> type = info.getClass();
        if (WorkspaceInfo.class.isAssignableFrom(type)) delegate.remove((WorkspaceInfo) info);
        else if (NamespaceInfo.class.isAssignableFrom(type)) delegate.remove((NamespaceInfo) info);
        else if (StoreInfo.class.isAssignableFrom(type)) delegate.remove((StoreInfo) info);
        else if (ResourceInfo.class.isAssignableFrom(type)) delegate.remove((ResourceInfo) info);
        else if (LayerInfo.class.isAssignableFrom(type)) delegate.remove((LayerInfo) info);
        else if (LayerGroupInfo.class.isAssignableFrom(type))
            delegate.remove((LayerGroupInfo) info);
        else if (StyleInfo.class.isAssignableFrom(type)) delegate.remove((StyleInfo) info);
        else if (MapInfo.class.isAssignableFrom(type)) delegate.remove((MapInfo) info);
        return info;
    }

    @SuppressWarnings("unchecked")
    public <C extends CatalogInfo> C getByName(
            @NonNull String name, @NonNull Class<? extends C> type) {

        if (WorkspaceInfo.class.isAssignableFrom(type))
            return type.cast(delegate.getWorkspaceByName(name));
        if (NamespaceInfo.class.isAssignableFrom(type))
            return type.cast(delegate.getNamespaceByPrefix(name));
        if (StoreInfo.class.isAssignableFrom(type))
            return type.cast(delegate.getStoreByName(name, (Class<? extends StoreInfo>) type));
        if (ResourceInfo.class.isAssignableFrom(type))
            return type.cast(
                    delegate.getResourceByName(name, (Class<? extends ResourceInfo>) type));
        if (LayerInfo.class.isAssignableFrom(type)) return type.cast(delegate.getLayerByName(name));
        if (LayerGroupInfo.class.isAssignableFrom(type))
            return type.cast(delegate.getLayerGroupByName(name));
        if (StyleInfo.class.isAssignableFrom(type)) return type.cast(delegate.getStyleByName(name));
        if (MapInfo.class.isAssignableFrom(type)) return type.cast(delegate.getMapByName(name));
        throw new IllegalArgumentException("Uknown CatalogInfo type: " + type.getCanonicalName());
    }

    public <C extends org.geoserver.catalog.CatalogInfo> Stream<C> query(@NonNull Query<C> query) {

        Class<C> type = query.getType();
        Filter filter = query.getFilter();
        Integer offset = query.getOffset();
        Integer count = query.getCount();
        SortBy sortBy = query.getSortBy().isEmpty() ? null : query.getSortBy().get(0);

        CloseableIterator<? extends C> iterator =
                delegate.list(type, filter, offset, count, sortBy);
        int characteristics = Spliterator.DISTINCT | Spliterator.NONNULL;
        Spliterator<C> spliterator = Spliterators.spliteratorUnknownSize(iterator, characteristics);
        boolean parallel = false;
        Stream<C> stream = StreamSupport.stream(spliterator, parallel);
        stream.onClose(iterator::close);
        return stream;
    }

    public Stream<NamespaceInfo> getNamespacesByURI(String uri) {
        return getFacade().getNamespacesByURI(uri).stream();
    }

    public Stream<DataStoreInfo> getDefaultDataStores() {
        return query(Query.all(WorkspaceInfo.class))
                .map(delegate::getDefaultDataStore)
                .filter(d -> d != null);
    }
}
