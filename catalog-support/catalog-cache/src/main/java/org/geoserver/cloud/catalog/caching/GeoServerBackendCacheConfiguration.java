/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.caching;

import javax.annotation.PostConstruct;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.plugin.GeoServerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enables caching at the {@link CatalogFacade} and {@link GeoServerFacade} level instead of at the
 * {@link Catalog} and {@link GeoServer} level, which would be the natural choice, in order not to
 * interfere with decorators such as {@code SecureCatalogImpl}, which need to hide objects at
 * runtime, and if a caching decorator sits on top of it, those resources might not be hidden for a
 * given user when they should.
 */
@Configuration
@EnableCaching
public class GeoServerBackendCacheConfiguration {

    private @Autowired @Qualifier("rawCatalog") CatalogPlugin rawCatalog;
    private @Autowired @Qualifier("geoServer") GeoServerImpl rawGeoServer;

    private @Autowired @Qualifier("catalogFacade") CatalogFacade rawCatalogFacade;
    private @Autowired @Qualifier("geoserverFacade") GeoServerFacade rawGeoServerFacade;

    public @Bean ExtendedCatalogFacade cachingCatalogFacade() {
        ExtendedCatalogFacade facade = (ExtendedCatalogFacade) rawCatalogFacade;
        return new CachingCatalogFacade(facade);
    }

    public @Bean GeoServerFacade cachingGeoServerFacade() {
        return new CachingGeoServerFacade(rawGeoServerFacade);
    }

    public @PostConstruct void decorateFacades() {
        ExtendedCatalogFacade cachingCatalogFacade = cachingCatalogFacade();
        GeoServerFacade cachingGeoServerFacade = cachingGeoServerFacade();

        rawCatalog.setFacade(cachingCatalogFacade);
        rawGeoServer.setFacade(cachingGeoServerFacade);
    }
}
