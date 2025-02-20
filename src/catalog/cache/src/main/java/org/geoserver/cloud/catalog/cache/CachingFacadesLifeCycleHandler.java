/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.cache;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.GeoServerReinitializer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.config.plugin.GeoServerImpl;

/**
 * {@link CachingCatalogFacade} and {@link CachingGeoServerFacade} initialization and life cycle
 * handler, to enable caching during normal operation and disable it during maintenance operations
 * such as configuration initialization and reloading.
 *
 * <p>Some methods in the caching decorators may cache {@code null} return values (for instance,
 * {@link CachingGeoServerFacade#getService(Class)} and others), which is very valuable during
 * normal GeoServer operation as such methods may be called several times per request. Yet, it can
 * be inconvenient during catalog and config maintenance operations, when, for example, the {@link
 * GeoServerLoader} checks for the existence of default or required objects. If a cached {@code
 * null} value is returned, multiple GeoServer instances may attempt to create the same default
 * objects (like {@link ServiceInfo}s).
 *
 * <p>For that reason, this {@link GeoServerReinitializer reinitializer} and {@link
 * GeoServerLifecycleHandler life cycle} listener disables caching during configuration {@link
 * #beforeReinitialize initialization} and {@link #beforeReload reload}, and enables caching after
 * {@link #initialize(GeoServer) initialization} and {@link #onReload() reload}.
 */
@RequiredArgsConstructor
@Slf4j(topic = "org.geoserver.cloud.catalog.caching")
class CachingFacadesLifeCycleHandler implements GeoServerReinitializer, GeoServerLifecycleHandler {

    private final @NonNull CachingCatalogFacade cachingCatalogFacade;
    private final @NonNull CachingGeoServerFacade cachingGeoServerFacade;

    /**
     * held to replace its {@link CatalogImpl#setFacade(CatalogFacade) facade} with the caching or
     * original one when enabling or disabling caching
     */
    private final @NonNull CatalogPlugin rawCatalog;

    /**
     * held to replace its {@link GeoServerImpl#setFacade(GeoServerFacade) facade} with the caching
     * or original one when enabling or disabling caching
     */
    private final @NonNull GeoServerImpl rawGeoServer;

    /**
     * {@link GeoServerReinitializer} method called before reloading the configuration, overridden to
     * disable caching
     */
    @Override
    public void beforeReinitialize(GeoServer geoServer) {
        disableCaching("beforeReinitialize");
    }

    /**
     * {@link GeoServerReinitializer} method called once the configuration has been loaded,
     * overridden to enable caching by replacing the {@link Catalog} facade with the {@link
     * CachingCatalogFacade} and the {@link GeoServer} facade with the {@link
     * CachingGeoServerFacade} decorators.
     */
    @Override
    public void initialize(GeoServer geoServer) {
        enableCaching("initialize");
    }

    /**
     * {@link GeoServerLifecycleHandler} method called by {@link GeoServer#reset()} to clear up all
     * of the caches inside GeoServer, evicts the {@link CachingCatalogFacade} and {@link
     * CachingGeoServerFacade} caches completely
     */
    @Override
    public void onReset() {
        evictAll();
    }

    /**
     * {@link GeoServerLifecycleHandler} method called by {@link GeoServer#reload()}, disables
     * caching by removing the {@link CachingCatalogFacade} and {@link CachingGeoServerFacade} from
     * the {@link Catalog} and {@link GeoServer} respectively.
     */
    @Override
    public void beforeReload() {
        disableCaching("beforeReload");
    }

    /**
     * {@link GeoServerLifecycleHandler} called by {@link GeoServer#reload()} once the config has
     * been reloaded, overridden to re-enable caching by replacing the {@link Catalog} facade with
     * the {@link CachingCatalogFacade} and the {@link GeoServer} facade with the {@link
     * CachingGeoServerFacade} decorators.
     */
    @Override
    public void onReload() {
        enableCaching("onReload");
    }

    /** {@link GeoServerLifecycleHandler} called by {@link GeoServer#dispose()} */
    @Override
    public void onDispose() {
        evictAll();
    }

    private void enableCaching(String method) {
        rawCatalog.setFacade(cachingCatalogFacade);
        log.info("{}: Decorated CatalogFacade with CachingCatalogFacade", method);
        rawGeoServer.setFacade(cachingGeoServerFacade);
        log.info("{}: Decorated GeoServerFacade with CachingGeoServerFacade", method);
    }

    private void disableCaching(String method) {
        CatalogFacade origCatalogFacade = cachingCatalogFacade.getSubject();
        GeoServerFacade origGeoServerFacade = cachingGeoServerFacade.getSubject();

        rawCatalog.setFacade(origCatalogFacade);
        log.info("{}: removed CachingCatalogFacade", method);
        rawGeoServer.setFacade(origGeoServerFacade);
        log.info("{}: removed CachingGeoServerFacade", method);

        evictAll();
    }

    private void evictAll() {
        cachingCatalogFacade.evictAll();
        cachingGeoServerFacade.evictAll();
        log.info("Evicted all catalog and config facade caches");
    }
}
