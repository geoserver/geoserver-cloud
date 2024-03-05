/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.cloud.autoconfigure.gwc.backend.DefaultTileLayerCatalogAutoConfiguration;
import org.geoserver.cloud.autoconfigure.gwc.core.GeoWebCacheAutoConfiguration;
import org.geoserver.cloud.config.catalog.backend.datadirectory.NoServletContextDataDirectoryResourceStore;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.File;

/**
 * @since 1.0
 */
public class GeoWebCacheContextRunner {

    public static WebApplicationContextRunner newMinimalGeoWebCacheContextRunner(File tmpDir) {
        assertThat(tmpDir).isDirectory().isWritable();
        File gwcCacheDir = new File(tmpDir, "gwc");
        gwcCacheDir.mkdir();
        assertThat(gwcCacheDir).isDirectory().isWritable();

        return newMinimalGeoServerContextRunner(tmpDir)
                .withConfiguration(
                        AutoConfigurations.of(
                                GeoWebCacheAutoConfiguration.class,
                                DefaultTileLayerCatalogAutoConfiguration.class))
                .withPropertyValues(
                        "gwc.cache-directory=%s".formatted(gwcCacheDir.getAbsolutePath()));
    }

    public static WebApplicationContextRunner newMinimalGeoServerContextRunner(File tmpDir) {
        return new WebApplicationContextRunner()
                .withAllowBeanDefinitionOverriding(true)
                .withAllowCircularReferences(true)
                // tmpDir for AddGeoServerDependenciesConfiguration
                .withBean("tempDir", File.class, () -> tmpDir)
                .withConfiguration(
                        AutoConfigurations.of(AddGeoServerDependenciesConfiguration.class))
                .withBean("extensions", GeoServerExtensions.class)
                .withBean("environments", GeoServerEnvironment.class)
                .withBean("xstreamPersisterFactory", XStreamPersisterFactory.class)
                .withBean("dispatcher", org.geoserver.ows.Dispatcher.class);
    }

    @AutoConfiguration
    private static class AddGeoServerDependenciesConfiguration {

        @Bean(name = {"rawCatalog", "catalog"})
        @ConditionalOnMissingBean
        CatalogPlugin rawCatalog() {
            return new CatalogPlugin();
        }

        @Bean
        @ConditionalOnMissingBean
        GeoServer geoServer() {
            return new GeoServerImpl();
        }

        @Bean
        @ConditionalOnMissingBean
        GeoServerConfigurationLock configurationLock() {
            return new GeoServerConfigurationLock();
        }

        @Bean
        @Primary
        @ConditionalOnMissingBean(name = "resourceStore")
        ResourceStore resourceStore(@Qualifier("resourceStoreImpl") ResourceStore impl) {
            return impl;
        }

        @Bean
        @ConditionalOnMissingBean(name = "resourceStoreImpl")
        ResourceStore resourceStoreImpl(@Qualifier("tempDir") File tmpDir) {
            return new NoServletContextDataDirectoryResourceStore(tmpDir);
        }

        @Bean
        @ConditionalOnMissingBean
        GeoServerResourceLoader resourceLoader(@Qualifier("tempDir") File tmpDir) {
            return new GeoServerResourceLoader(tmpDir);
        }

        @Bean
        @ConditionalOnMissingBean
        GeoServerDataDirectory geoServerDataDirectory(GeoServerResourceLoader resourceLoader) {
            return new GeoServerDataDirectory(resourceLoader);
        }

        @Bean
        @ConditionalOnMissingBean
        GeoServerSecurityManager geoServerSecurityManager(GeoServerDataDirectory datadir)
                throws Exception {
            return new GeoServerSecurityManager(datadir);
        }
    }
}
