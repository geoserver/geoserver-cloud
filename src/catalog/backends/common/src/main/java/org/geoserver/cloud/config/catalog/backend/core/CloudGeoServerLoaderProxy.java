/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.config.catalog.backend.core;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
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

    /**
     * Overrides as no-op, the loaders initialize default styles during load and end
     * up calling this {@link GeoServerInitializer} while the loader proxy's
     * delegate is unset:
     *
     * <pre>{@code java.lang.NullPointerException: Cannot invoke "org.geoserver.config.GeoServerLoader.initializeDefaultStyles(org.geoserver.catalog.Catalog)" because "this.loader" is null
     * at org.geoserver.config.GeoServerLoaderProxy.initialize(GeoServerLoaderProxy.java:91)
     * at org.geoserver.config.GeoServerLoader.loadInitializers(GeoServerLoader.java:342)
     * at org.geoserver.config.GeoServerLoader.postProcessBeforeInitializationGeoServer(GeoServerLoader.java:302)
     * at org.geoserver.config.GeoServerLoader.postProcessBeforeInitialization(GeoServerLoader.java:276)
     * at org.geoserver.cloud.config.catalog.backend.datadirectory.DataDirectoryGeoServerLoader.load(DataDirectoryGeoServerLoader.java:81)
     * at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
     * at java.base/java.lang.reflect.Method.invoke(Method.java:580)
     * at org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor$LifecycleElement.invoke(InitDestroyAnnotationBeanPostProcessor.java:389)
     * ...
     * at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:209)
     * at org.springframework.context.support.AbstractApplicationContext.getBean(AbstractApplicationContext.java:1171)
     * at org.geoserver.platform.GeoServerExtensions.getBean(GeoServerExtensions.java:249)
     * at org.geoserver.platform.GeoServerExtensions.extensions(GeoServerExtensions.java:143)
     * at org.geoserver.platform.GeoServerExtensions.extensions(GeoServerExtensions.java:118)
     * at org.geoserver.platform.GeoServerExtensions.bean(GeoServerExtensions.java:342)
     * at org.geoserver.config.GeoServerLoaderProxy.lookupGeoServerLoader(GeoServerLoaderProxy.java:82)
     * at org.geoserver.config.GeoServerLoaderProxy.setApplicationContext(GeoServerLoaderProxy.java:48)
     * }</pre>
     */
    @Override
    public void initialize(GeoServer geoServer) {
        // no-op
    }
}
