/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.impl;

import java.util.function.Function;
import java.util.function.Supplier;
import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.RepositoryCatalogFacade;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;
import org.geoserver.catalog.plugin.resolving.CatalogPropertyResolver;
import org.geoserver.catalog.plugin.resolving.ModificationProxyDecorator;
import org.geoserver.catalog.plugin.resolving.ResolvingProxyResolver;
import org.geoserver.cloud.catalog.client.repository.CatalogClientRepository;

public class CatalogClientCatalogFacade extends ResolvingCatalogFacadeDecorator {

    public CatalogClientCatalogFacade(@NonNull RepositoryCatalogFacade rawFacade) {
        super(rawFacade);
    }

    // set up resolving chain
    public @Override void setCatalog(Catalog catalog) {
        super.setCatalog(catalog);

        final ResolvingProxyResolver proxyResolver = ResolvingProxyResolver.of(catalog, true);
        final CatalogPropertyResolver catalogPropertyResolver = CatalogPropertyResolver.of(catalog);
        final CatalogPropertyResolver collectionInitializer = CatalogPropertyResolver.of(catalog);

        // resolver for single-object returning methods
        Function<CatalogInfo, CatalogInfo> resolving =
                proxyResolver.andThen(catalogPropertyResolver).andThen(collectionInitializer);

        // resolver supplier for Stream<> returning methods, uses memoized proxy resolver that
        // caches resolved references for the lifetime of the stream
        Supplier<Function<CatalogInfo, CatalogInfo>> streamResolve = //
                () ->
                        proxyResolver
                                .memoizing() //
                                .andThen(catalogPropertyResolver) //
                                .andThen(collectionInitializer);

        setInboundResolver(ModificationProxyDecorator.unwrap());

        setInnerResolver(repo(WorkspaceInfo.class), cast(resolving), streamResolve);
        setInnerResolver(repo(NamespaceInfo.class), cast(resolving), streamResolve);
        setInnerResolver(repo(StoreInfo.class), cast(resolving), streamResolve);
        setInnerResolver(repo(ResourceInfo.class), cast(resolving), streamResolve);
        setInnerResolver(repo(LayerInfo.class), cast(resolving), streamResolve);
        setInnerResolver(repo(LayerGroupInfo.class), cast(resolving), streamResolve);
        setInnerResolver(repo(StyleInfo.class), cast(resolving), streamResolve);
        setInnerResolver(repo(MapInfo.class), cast(resolving), streamResolve);
    }

    @SuppressWarnings("unchecked")
    private <T extends CatalogInfo> Function<T, T> cast(Function<CatalogInfo, CatalogInfo> f) {
        return (Function<T, T>) f;
    }

    private <T extends CatalogInfo> CatalogClientRepository<T> repo(Class<T> type) {
        return facade().repository(type);
    }

    private <T extends CatalogInfo> void setInnerResolver( //
            CatalogClientRepository<T> catalogClientRepository, //
            Function<T, T> objectResolver, //
            Supplier<Function<CatalogInfo, CatalogInfo>> memoizingResolver) {

        catalogClientRepository.setObjectResolver(objectResolver);
        catalogClientRepository.setStreamResolver(memoizingResolver);
    }

    protected @Override RepositoryCatalogFacade facade() {
        return (RepositoryCatalogFacade) facade;
    }
}
