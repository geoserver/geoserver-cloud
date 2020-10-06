/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.impl;

import java.util.NoSuchElementException;
import java.util.function.Function;
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
import org.geoserver.catalog.plugin.resolving.CollectionPropertiesInitializer;
import org.geoserver.catalog.plugin.resolving.ModificationProxyDecorator;
import org.geoserver.catalog.plugin.resolving.ResolvingProxyResolver;
import org.geoserver.cloud.catalog.client.repository.CatalogServiceClientRepository;

public class CatalogServiceCatalogFacade extends ResolvingCatalogFacadeDecorator {

    public CatalogServiceCatalogFacade(@NonNull RepositoryCatalogFacade rawFacade) {
        super(rawFacade);
    }

    // set up resolving chain
    public @Override void setCatalog(Catalog catalog) {
        super.setCatalog(catalog);

        Function<CatalogInfo, CatalogInfo> resolvingFunction;
        resolvingFunction =
                CatalogPropertyResolver.of(catalog)
                        .andThen(
                                ResolvingProxyResolver.of(
                                        catalog,
                                        (proxiedInfo, proxy) -> {
                                            throw new NoSuchElementException(
                                                    "Object not found: " + proxiedInfo.getId());
                                        }))
                        .andThen(CollectionPropertiesInitializer.instance());

        setOutboundResolver(resolvingFunction);

        setInboundResolver(ModificationProxyDecorator.unwrap());
    }

    public @Override void setOutboundResolver(Function<CatalogInfo, CatalogInfo> resolving) {
        setInnerResolver(repo(WorkspaceInfo.class), cast(resolving));
        setInnerResolver(repo(NamespaceInfo.class), cast(resolving));
        setInnerResolver(repo(StoreInfo.class), cast(resolving));
        setInnerResolver(repo(ResourceInfo.class), cast(resolving));
        setInnerResolver(repo(LayerInfo.class), cast(resolving));
        setInnerResolver(repo(LayerGroupInfo.class), cast(resolving));
        setInnerResolver(repo(StyleInfo.class), cast(resolving));
        setInnerResolver(repo(MapInfo.class), cast(resolving));
    }

    @SuppressWarnings("unchecked")
    private <T extends CatalogInfo> Function<T, T> cast(Function<CatalogInfo, CatalogInfo> f) {
        return (Function<T, T>) f;
    }

    private <T extends CatalogInfo> CatalogServiceClientRepository<T> repo(Class<T> type) {
        return facade().repository(type);
    }

    private <T extends CatalogInfo> void setInnerResolver(
            CatalogServiceClientRepository<T> repo, Function<T, T> proxyResolver) {
        repo.setProxyResolver(proxyResolver);
    }

    protected @Override RepositoryCatalogFacade facade() {
        return (RepositoryCatalogFacade) facade;
    }
}
