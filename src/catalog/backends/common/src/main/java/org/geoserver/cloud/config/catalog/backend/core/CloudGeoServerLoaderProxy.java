/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.core;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerLoaderProxy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;

/**
 * A {@link GeoServerLoaderProxy} that doesn't act as a BeanPostProcessor
 */
public class CloudGeoServerLoaderProxy extends GeoServerLoaderProxy implements InitializingBean {

    private Catalog rawCatalog;
    private GeoServer geoServer;

    public CloudGeoServerLoaderProxy(Catalog rawCatalog, GeoServer geoServer) {
        super(rawCatalog.getResourceLoader());
        this.rawCatalog = rawCatalog;
        this.geoServer = geoServer;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.postProcessBeforeInitialization(rawCatalog, "rawCatalog");
        super.postProcessBeforeInitialization(geoServer, "geoServer");
    }

    /**
     * Override as no-op, the catalog and geoserver beans are created before this
     * bean, hence it'll never be in charge if their pre-post initialization
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * Override as no-op, the catalog and geoserver beans are created before this
     * bean, hence it'll never be in charge if their pre-post initialization
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
