/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.config.catalog.backend.core;

import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerLoaderProxy;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.GlobalLockProvider;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.ResourceStoreFactory;
import org.geoserver.security.impl.GsCloudLayerGroupContainmentCache;
import org.geoserver.security.impl.LayerGroupContainmentCache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Base configuration to set up the core catalog and configuration geoserver components
 *
 * <p>A specialized catalog back-end, such as datadir and pgconfig, would provide a <code>@Configuration</code> class
 * that implements the marker interface {@link GeoServerBackendConfigurer}, that must provide at least the following
 * beans:
 *
 * <ul>
 *   <li>GeoServerConfigurationLock configurationLock
 *   <li>UpdateSequence updateSequence
 *   <li>ExtendedCatalogFacade catalogFacade
 *   <li>GeoServerLoader geoServerLoaderImpl
 *   <li>GeoServerFacade geoserverFacade
 *   <li>ResourceStore resourceStoreImpl
 *   <li>GeoServerResourceLoader resourceLoader
 * </ul>
 */
@Configuration(proxyBeanMethods = true)
@Slf4j(topic = "org.geoserver.cloud.config.catalog.backend.core")
public class CoreBackendConfiguration {

    /**
     * {@code GlobalLockProvider lockProvider} is excluded in {@code GeoServerMainConfiguration} because it depends on
     * {@code nullLockProvider}. In GeoServer Cloud, a {@link GeoServerBackendConfigurer} is required to supply a
     * {@link LockProvider} known to perform distributed locking properly.
     */
    @Bean
    GlobalLockProvider lockProvider(LockProvider suppliedLockProvider) {
        GlobalLockProvider globalLockProvider = new GlobalLockProvider();
        globalLockProvider.setDelegate(suppliedLockProvider);
        return globalLockProvider;
    }

    /** A {@link GeoServerLoaderProxy} that doesn't act as a BeanPostProcessor */
    @Lazy
    @Bean
    GeoServerLoaderProxy geoServerLoader(
            @Qualifier("rawCatalog") CatalogImpl catalog, @Qualifier("geoServer") GeoServer geoserver) {
        return new CloudGeoServerLoaderProxy(catalog, geoserver);
    }

    /** Base catalog */
    @Bean
    @ConditionalOnMissingBean(CatalogPlugin.class)
    CatalogPlugin rawCatalog(
            @Qualifier("resourceLoader") GeoServerResourceLoader resourceLoader,
            @Qualifier("catalogFacade") ExtendedCatalogFacade catalogFacade) {

        CatalogPlugin rawCatalog = new CatalogPlugin(catalogFacade);
        rawCatalog.setResourceLoader(resourceLoader);
        return rawCatalog;
    }

    /** Default implementation of GeoServer global and service configuration manager. */
    @ConditionalOnMissingBean(GeoServerImpl.class)
    @Bean(name = "geoServer")
    GeoServerImpl geoServer(
            @Qualifier("catalog") Catalog catalog, @Qualifier("geoserverFacade") GeoServerFacade facade) {
        GeoServerImpl gs = new GeoServerImpl(facade);
        gs.setCatalog(catalog);
        return gs;
    }

    /** Factory for {@link XStreamPersister} instances. */
    @Bean
    XStreamPersisterFactory xstreamPersisterFactory() {
        return new XStreamPersisterFactory();
    }

    /** Utility class uses to process GeoServer extension points. */
    @Bean
    GeoServerExtensions extensions() {
        return new GeoServerExtensions();
    }

    /**
     * A cache for layer group containment, it speeds up looking up layer groups containing a particular layer
     * (recursively).
     *
     * <p>Actual {@link LayerGroupContainmentCache}, matches if the config property
     * {@code geoserver.security.layergroup-containmentcache=true}
     *
     * @see #noOpLayerGroupContainmentCache(Catalog)
     */
    @Bean(name = "layerGroupContainmentCache")
    @ConditionalOnProperty(
            name = "geoserver.security.layergroup-containmentcache",
            havingValue = "true",
            matchIfMissing = false)
    LayerGroupContainmentCache enabledLayerGroupContainmentCache(@Qualifier("rawCatalog") Catalog rawCatalog) {

        log.info("using {}", GsCloudLayerGroupContainmentCache.class.getSimpleName());
        return new GsCloudLayerGroupContainmentCache(rawCatalog);
    }

    /**
     * The default {@link LayerGroupContainmentCache} is a no-op, matches if the config property
     * {@code geoserver.security.layergroup-containmentcache=false} or is not specified
     *
     * @see #enabledLayerGroupContainmentCache(Catalog)
     */
    @Bean(name = "layerGroupContainmentCache")
    @ConditionalOnProperty(
            name = "geoserver.security.layergroup-containmentcache",
            havingValue = "false",
            matchIfMissing = true)
    LayerGroupContainmentCache noOpLayerGroupContainmentCache() {

        log.info("using {}", NoopLayerGroupContainmentCache.class.getSimpleName());
        return new NoopLayerGroupContainmentCache();
    }

    /**
     * File or Resource access to GeoServer data directory. In addition to paths Catalog objects such as workspace or
     * FeatureTypeInfo can be used to locate resources.
     */
    @Bean
    GeoServerDataDirectory dataDirectory(GeoServerResourceLoader resourceLoader) {
        return new GeoServerDataDirectory(resourceLoader);
    }

    /**
     * Main {@link ResourceStore} bean, defaults to the provided {@code resourceStoreImpl}, differently to
     * {@code gs-main.jar!/applicationContext.xml} which provides an indirection through {@link ResourceStoreFactory}
     * and hence a possible early lookup through {@code GeoServerExtensions.bean("resourceStoreImpl",
     * applicationContext)}
     */
    @Bean(name = "resourceStore")
    ResourceStoreFactory resourceStore(@Qualifier("resourceStoreImpl") ResourceStore impl) {
        return new ResourceStoreFactory() {
            @Override
            public ResourceStore getObject() throws Exception {
                return impl;
            }
            /** @return ResourceStore.class, ResourceStoreFactory returns null */
            @Override
            public Class<?> getObjectType() {
                return ResourceStore.class;
            }
        };
    }
}
