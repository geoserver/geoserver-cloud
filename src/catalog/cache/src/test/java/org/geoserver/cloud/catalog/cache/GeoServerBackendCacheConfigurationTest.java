/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.cache;

import static org.mockito.Mockito.mock;

import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.plugin.GeoServerImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** */
class GeoServerBackendCacheConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withAllowBeanDefinitionOverriding(true)
                    .withBean("rawCatalog", CatalogPlugin.class)
                    .withBean("geoServer", GeoServerImpl.class)
                    .withBean(
                            "catalogFacade",
                            ExtendedCatalogFacade.class,
                            () -> mock(ExtendedCatalogFacade.class))
                    .withBean(
                            "geoserverFacade",
                            GeoServerFacade.class,
                            () -> mock(GeoServerFacade.class))
                    .withConfiguration(
                            UserConfigurations.of(GeoServerBackendCacheConfiguration.class))
                    .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class));

    @Test
    void testCachingCatalogFacade() {
        contextRunner.run(
                context -> context.isTypeMatch("cachingCatalogFacade", CachingCatalogFacade.class));
    }

    @Test
    void testCachingGeoServerFacade() {
        contextRunner.run(
                context ->
                        context.isTypeMatch(
                                "cachingGeoServerFacade", CachingGeoServerFacade.class));
    }
}
