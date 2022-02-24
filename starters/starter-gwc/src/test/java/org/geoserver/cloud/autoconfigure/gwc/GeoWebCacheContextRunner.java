/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.cloud.autoconfigure.gwc.core.GeoWebCacheAutoConfiguration;
import org.geoserver.cloud.config.datadirectory.NoServletContextDataDirectoryResourceStore;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.ResourceStoreFactory;
import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.io.File;

/**
 * @since 1.0
 */
public class GeoWebCacheContextRunner {

    public static WebApplicationContextRunner newMinimalGeoWebCacheContextRunner(File tmpDir) {
        return newMinimalGeoServerContextRunner(tmpDir)
                .withConfiguration(AutoConfigurations.of(GeoWebCacheAutoConfiguration.class))
                .withPropertyValues("gwc.cache-directory=" + tmpDir.toString());
    }

    public static WebApplicationContextRunner newMinimalGeoServerContextRunner(File tmpDir) {
        Catalog catalog = new CatalogPlugin();
        GeoServer geoserver = new GeoServerImpl();
        geoserver.setCatalog(catalog);
        ResourceStore store = new NoServletContextDataDirectoryResourceStore(tmpDir);
        GeoServerResourceLoader gsReourceLoader = new GeoServerResourceLoader(tmpDir);
        GeoServerDataDirectory datadir = new GeoServerDataDirectory(gsReourceLoader);
        GeoServerSecurityManager geoServerSecurityManager;
        try {
            geoServerSecurityManager = new GeoServerSecurityManager(datadir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new WebApplicationContextRunner()
                .withAllowBeanDefinitionOverriding(true)
                .withAllowCircularReferences(true)
                .withBean("rawCatalog", Catalog.class, () -> catalog)
                .withBean("catalog", Catalog.class, () -> catalog)
                .withBean("geoServer", GeoServer.class, () -> geoserver)
                .withBean("resourceStore", ResourceStoreFactory.class)
                .withBean("resourceStoreImpl", ResourceStore.class, () -> store)
                .withBean("resourceLoader", GeoServerResourceLoader.class, () -> gsReourceLoader)
                .withBean("extensions", GeoServerExtensions.class, () -> new GeoServerExtensions())
                .withBean(
                        "geoServerSecurityManager",
                        GeoServerSecurityManager.class,
                        () -> geoServerSecurityManager)
                .withBean("xstreamPersisterFactory", XStreamPersisterFactory.class)
                .withBean("dispatcher", org.geoserver.ows.Dispatcher.class);
    }
}
