/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.caching;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.plugin.CatalogFacadeExtensionAdapter;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enables caching at the {@link CatalogFacade} and {@link GeoServerFacade} level instead of at the
 * {@link Catalog} and {@link GeoServer} level, which would be the natural choice, in order not to
 * interfere with decorators such as {@code SecureCatalogImpl}, which need to hide objects at
 * runtime, and if a caching decorator sits on top of it, those resources might not be hidden for a
 * given user when they should.
 *
 * @see CachingCatalogFacade
 * @see CachingGeoServerFacade
 */
@Configuration(proxyBeanMethods = true)
@EnableCaching(proxyTargetClass = true)
public class GeoServerBackendCacheConfiguration implements BeanPostProcessor {

    @Bean
    CacheConfigurationPostProcessor cacheConfigurationPostProcessor() {
        return new CacheConfigurationPostProcessor();
    }

    public @Bean CachingCatalogFacade cachingCatalogFacade(
            @Qualifier("catalogFacade") CatalogFacade rawCatalogFacade) {
        CatalogFacade raw = rawCatalogFacade;
        ExtendedCatalogFacade facade;
        if (raw instanceof ExtendedCatalogFacade) {
            facade = (ExtendedCatalogFacade) rawCatalogFacade;
        } else {
            facade = new CatalogFacadeExtensionAdapter(raw);
        }
        return new CachingCatalogFacadeImpl(facade);
    }

    public @Bean CachingGeoServerFacade cachingGeoServerFacade(
            @Qualifier("geoserverFacade") GeoServerFacade rawGeoServerFacade) {
        return new CachingGeoServerFacadeImpl(rawGeoServerFacade);
    }
}
