/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.cache;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.config.plugin.GeoServerImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;

/** */
@Slf4j(topic = "org.geoserver.cloud.catalog.caching")
class CacheConfigurationPostProcessor implements BeanPostProcessor {

    private @Autowired CachingCatalogFacade cachingCatalogFacade;
    private @Autowired CachingGeoServerFacade cachingGeoServerFacade;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {
        if ("rawCatalog".equals(beanName)) {
            CatalogPlugin rawCatalog = (CatalogPlugin) bean;
            rawCatalog.setFacade(cachingCatalogFacade);
            log.info("Decorated CatalogFacade with CachingCatalogFacade");
        } else if ("geoServer".equals(beanName)) {
            GeoServerImpl gs = (GeoServerImpl) bean;
            gs.setFacade(cachingGeoServerFacade);
            log.info("Decorated GeoServerFacade with CachingGeoServerFacade");
        }
        return bean;
    }
}
