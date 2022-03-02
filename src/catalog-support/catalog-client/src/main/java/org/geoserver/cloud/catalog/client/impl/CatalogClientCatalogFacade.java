/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.impl;

import lombok.NonNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
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
import org.geoserver.catalog.plugin.RepositoryCatalogFacade;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;
import org.geoserver.catalog.plugin.resolving.CatalogPropertyResolver;
import org.geoserver.catalog.plugin.resolving.CollectionPropertiesInitializer;
import org.geoserver.catalog.plugin.resolving.ResolvingProxyResolver;
import org.geoserver.cloud.catalog.client.repository.CatalogClientRepository;

import java.lang.reflect.Proxy;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * {@link CatalogFacade} for {@code catalog-service}, being a {@link
 * ResolvingCatalogFacadeDecorator}, wraps the raw {@link RepositoryCatalogFacade} that delegates to
 * the {@link CatalogClientRepository catalog client repositories}, and adds the necessary {@link
 * #setInboundResolver inbound} and {@link #setOutboundResolver outbound} resolvers to ensure valid
 * input and output for the decorated facade.
 *
 * <p>Note these in/out bound resolvers <b>do not</b> deal with {@link ModificationProxy} at all. It
 * is up to the {@link Catalog} implementation itself to do that if it so requires it.
 *
 * <p>This decorator specializes in resolving the objects returned by {@link ReactiveCatalogClient}
 * through {@link CatalogClientRepository}. For instance:
 *
 * <p>
 *
 * <ul>
 *   <li>for in-bound objects, it checks no {@code java.lang.reflect.Proxy} instances get in,
 *       throwing an {@link IllegalArgumentException} if so happens, as it'd be a programming error,
 *       probably the catalog not unwrapping objects before deliering them down to the facade.
 *   <li>for out-bound objects:
 *       <ul>
 *         <li>Resolves {@link ResolvingProxy} proxied object references (see {@link
 *             ResolvingProxyResolver});
 *         <li>then sets the {@link Catalog} property if the object so requires it (e.g. {@link
 *             StoreInfo#getCatalog()} (see {@link CatalogPropertyResolver});
 *         <li>and finally resolves {@code null} collection properties to empty collections (see
 *             {@link CollectionPropertiesInitializer})
 *       </ul>
 * </ul>
 */
public class CatalogClientCatalogFacade extends ResolvingCatalogFacadeDecorator {

    public CatalogClientCatalogFacade(@NonNull RepositoryCatalogFacade rawFacade) {
        super(rawFacade);
    }

    // set up resolving chain
    public @Override void setCatalog(Catalog catalog) {
        super.setCatalog(catalog);

        final ResolvingProxyResolver<CatalogInfo> proxyResolver =
                ResolvingProxyResolver.of(catalog, true);
        final CatalogPropertyResolver<CatalogInfo> catalogPropertyResolver =
                CatalogPropertyResolver.of(catalog);
        final CollectionPropertiesInitializer<CatalogInfo> collectionInitializer =
                CollectionPropertiesInitializer.instance();

        // resolver for single-object returning methods
        Function<CatalogInfo, CatalogInfo> outboundResolver =
                proxyResolver.andThen(catalogPropertyResolver).andThen(collectionInitializer);

        // resolver supplier for Stream<> returning methods, uses memoized proxy resolver that
        // caches resolved references for the lifetime of the stream
        Supplier<Function<CatalogInfo, CatalogInfo>> streamResolver = //
                () ->
                        proxyResolver
                                .<CatalogInfo>memoizing() //
                                .andThen(catalogPropertyResolver) //
                                .andThen(collectionInitializer);

        setInboundResolver(
                o -> {
                    if (o instanceof Proxy) {
                        throw new IllegalArgumentException(
                                "This CatalogFacade does not accept java.lang.reflect.Proxy instances: "
                                        + o);
                    }
                    return o;
                });

        setInnerResolver(repo(WorkspaceInfo.class), cast(outboundResolver), streamResolver);
        setInnerResolver(repo(NamespaceInfo.class), cast(outboundResolver), streamResolver);
        setInnerResolver(repo(StoreInfo.class), cast(outboundResolver), streamResolver);
        setInnerResolver(repo(ResourceInfo.class), cast(outboundResolver), streamResolver);
        setInnerResolver(repo(LayerInfo.class), cast(outboundResolver), streamResolver);
        setInnerResolver(repo(LayerGroupInfo.class), cast(outboundResolver), streamResolver);
        setInnerResolver(repo(StyleInfo.class), cast(outboundResolver), streamResolver);
        setInnerResolver(repo(MapInfo.class), cast(outboundResolver), streamResolver);
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
