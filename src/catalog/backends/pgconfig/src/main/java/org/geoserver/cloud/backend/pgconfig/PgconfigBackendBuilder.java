/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig;

import java.util.function.UnaryOperator;
import javax.sql.DataSource;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;
import org.geoserver.catalog.plugin.requestcache.RequestCache;
import org.geoserver.catalog.plugin.requestcache.RequestCacheScope;
import org.geoserver.catalog.plugin.requestcache.RequestCachingCatalogFacade;
import org.geoserver.cloud.backend.pgconfig.catalog.PgconfigCatalogFacade;
import org.geoserver.cloud.backend.pgconfig.config.PgconfigGeoServerFacade;
import org.geoserver.config.plugin.GeoServerImpl;
import org.springframework.jdbc.core.JdbcTemplate;

/** @since 1.4 */
@RequiredArgsConstructor
public class PgconfigBackendBuilder {

    private final @NonNull DataSource dataSource;

    public CatalogPlugin createCatalog() {
        return initCatalog(new CatalogPlugin());
    }

    public GeoServerImpl createGeoServer(Catalog catalog) {
        PgconfigGeoServerFacade facade = createGeoServerFacade();
        GeoServerImpl gs = new GeoServerImpl(facade);
        gs.setCatalog(catalog);
        return gs;
    }

    public PgconfigGeoServerFacade createGeoServerFacade() {
        return new PgconfigGeoServerFacade(new JdbcTemplate(dataSource));
    }

    public ExtendedCatalogFacade createCatalogFacade() {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        return resolving(new PgconfigCatalogFacade(template));
    }

    /**
     * Returns the facade to give to the catalog without the cache of the request: a decorator of {@code facade},
     * resolving the references of its objects through it and initializing their null collections, see
     * {@link PgconfigCatalogFacade#referenceResolver}. {@link CatalogPlugin} adds workspace isolation above this
     * decorator. A layer then reaches isolation with its store and workspace resolved, and the lookups of its
     * references do not go through isolation. {@link #requestCaching} builds the chain with the cache of the request.
     */
    public static ResolvingCatalogFacadeDecorator resolving(@NonNull ExtendedCatalogFacade facade) {
        ResolvingCatalogFacadeDecorator resolving = new ResolvingCatalogFacadeDecorator(facade);
        resolving.setOutboundResolver(PgconfigCatalogFacade.referenceResolver(facade));
        return resolving;
    }

    /**
     * Returns the facade to give to the catalog with the cache of the request: a {@link RequestCachingCatalogFacade}
     * over a decorator resolving the references of the objects of {@code facade}, as the one of {@link #resolving}. An
     * object enters the cache of the request once the decorator resolved its references and initialized its
     * collections. A later lookup of the request then returns it as it is, without resolving it again.
     *
     * <p>The decorator, and the repositories of {@code facade} for the part of a filter evaluated in memory, look the
     * references up through the cache of the request, see {@link #lookUpThroughRequestCache}. The request then decodes
     * a store or a style shared by several layers once.
     */
    public static RequestCachingCatalogFacade requestCaching(@NonNull PgconfigCatalogFacade facade) {
        ResolvingCatalogFacadeDecorator resolving = new ResolvingCatalogFacadeDecorator(facade);
        RequestCachingCatalogFacade requestCaching = new RequestCachingCatalogFacade(resolving);
        UnaryOperator<CatalogInfo> references = lookUpThroughRequestCache(requestCaching, facade);
        resolving.setOutboundResolver(references);
        facade.setReferenceResolver(references);
        return requestCaching;
    }

    public <C extends CatalogImpl> C initCatalog(C catalog) {
        ExtendedCatalogFacade facade = createCatalogFacade();
        catalog.setFacade(facade);
        return catalog;
    }

    /**
     * Returns the function resolving the references of the objects of {@code facade} in the chain of
     * {@link #requestCaching}. The function chooses where to look the references up at each call, from the thread of
     * the call: a thread of a request uses the cache of its request, and a thread seeding tiles, outside a listing,
     * shares the lookups within one call.
     *
     * <p>While the thread holds a request cache taking entries, the function looks the references up through
     * {@code requestCaching}, and the objects found enter the cache of the request. An object found below the cache of
     * the request is resolved by a call of its own: the references have no cycle, as the catalog rejects a group
     * containing itself.
     *
     * <p>Otherwise, outside a scope of the request cache, once its cache is full or dead, or once the request caches of
     * the JVM hold their budget of entries, the function looks the references up in {@code facade}, as
     * {@link #resolving} does. Through {@code requestCaching}, each nested lookup would then start a call of its own,
     * and two calls would share nothing. In {@code facade}, one call looks each reference up once.
     */
    private static UnaryOperator<CatalogInfo> lookUpThroughRequestCache(
            RequestCachingCatalogFacade requestCaching, PgconfigCatalogFacade facade) {
        UnaryOperator<CatalogInfo> throughRequestCache = PgconfigCatalogFacade.referenceResolver(requestCaching);
        UnaryOperator<CatalogInfo> throughFacade = PgconfigCatalogFacade.referenceResolver(facade);
        return info -> {
            if (holdsRequestCacheTakingEntries()) {
                return throughRequestCache.apply(info);
            }
            return throughFacade.apply(info);
        };
    }

    /**
     * Whether this thread holds a request cache taking entries, see {@link RequestCache#takesEntries()}: in a request
     * or a worker of a request, in a listing, or in the build of the layer group containment cache, until the cache is
     * full or the budget of the request caches is used up.
     */
    private static boolean holdsRequestCacheTakingEntries() {
        RequestCache cache = RequestCacheScope.current();
        return cache != null && cache.takesEntries();
    }
}
