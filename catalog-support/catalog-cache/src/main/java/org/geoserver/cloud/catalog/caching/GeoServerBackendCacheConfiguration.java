/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.caching;

import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.plugin.CatalogFacadeExtensionAdapter;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.plugin.GeoServerImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
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
@Slf4j(topic = "org.geoserver.cloud.catalog.caching")
public class GeoServerBackendCacheConfiguration implements BeanPostProcessor {

    private @Autowired @Qualifier("catalogFacade") CatalogFacade rawCatalogFacade;
    private @Autowired @Qualifier("geoserverFacade") GeoServerFacade rawGeoServerFacade;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {
        if ("rawCatalog".equals(beanName)) {
            CatalogPlugin rawCatalog = (CatalogPlugin) bean;
            CachingCatalogFacade cachingFacade = cachingCatalogFacade();
            rawCatalog.setFacade(cachingFacade);
            log.info("Decorated CatalogFacade with CachingCatalogFacade");
        } else if ("geoServer".equals(beanName)) {
            GeoServerImpl gs = (GeoServerImpl) bean;
            CachingGeoServerFacade cachingFacade = cachingGeoServerFacade();
            gs.setFacade(cachingFacade);
            log.info("Decorated GeoServerFacade with CachingGeoServerFacade");
        }
        return bean;
    }

    public @Bean CachingCatalogFacade cachingCatalogFacade() {
        CatalogFacade raw = rawCatalogFacade;
        ExtendedCatalogFacade facade;
        if (raw instanceof ExtendedCatalogFacade) {
            facade = (ExtendedCatalogFacade) rawCatalogFacade;
        } else {
            facade = new CatalogFacadeExtensionAdapter(raw);
        }
        return new CachingCatalogFacadeImpl(facade);
    }

    public @Bean CachingGeoServerFacade cachingGeoServerFacade() {
        return new CachingGeoServerFacadeImpl(rawGeoServerFacade);
    }
}
