/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.test.backend;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertSame;

import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.cloud.autoconfigure.catalog.CatalogClientBackendAutoConfiguration;
import org.geoserver.cloud.autoconfigure.catalog.GeoServerBackendAutoConfiguration;
import org.geoserver.cloud.autoconfigure.security.GeoServerSecurityAutoConfiguration;
import org.geoserver.cloud.catalog.client.impl.CatalogClientCatalogFacade;
import org.geoserver.cloud.catalog.client.impl.CatalogClientGeoServerFacade;
import org.geoserver.cloud.catalog.client.impl.CatalogClientResourceStore;
import org.geoserver.cloud.config.catalogclient.CatalogClientBackendConfigurer;
import org.geoserver.cloud.config.catalogclient.CatalogClientGeoServerLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactivefeign.spring.config.ReactiveFeignAutoConfiguration;

/**
 * Test {@link CatalogClientBackendConfigurer} through {@link CatalogClientBackendAutoConfiguration}
 * when {@code geoserver.backend.catalog-service.enabled=true}
 */
@Ignore("Make it run without ReactiveCatalogClient trying to connect")
public class CatalogClientBackendAutoConfigurationTest {

    // geoserver.security.enabled=false to avoid calling the catalog during bean initialization,
    // since there's no backend service to connect to
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withAllowBeanDefinitionOverriding(true)
                    .withPropertyValues(
                            "reactive.feign.loadbalancer.enabled=false",
                            "geoserver.backend.catalog-service.enabled=true",
                            "geoserver.security.enabled=false")
                    .withAllowBeanDefinitionOverriding(true)
                    .withConfiguration(
                            AutoConfigurations.of( //
                                    GeoServerBackendAutoConfiguration.class,
                                    GeoServerSecurityAutoConfiguration.class,
                                    ReactiveFeignAutoConfiguration.class,
                                    WebClientAutoConfiguration.class,
                                    CacheAutoConfiguration.class));

    public @Test void testCatalog() {
        contextRunner.run(
                context ->
                        context.isTypeMatch(
                                "rawCatalog", org.geoserver.catalog.plugin.CatalogPlugin.class));
    }

    public @Test void testCatalogFacade() {
        contextRunner.run(
                context -> context.isTypeMatch("catalogFacade", CatalogClientCatalogFacade.class));
    }

    public @Test void testCatalogFacadeIsRawCatalogFacade() {
        contextRunner.run(
                context -> {
                    CatalogPlugin catalog = context.getBean("rawCatalog", CatalogPlugin.class);
                    CatalogFacade rawCatalogFacade =
                            context.getBean("catalogFacade", CatalogFacade.class);
                    assertSame(rawCatalogFacade, catalog.getRawFacade());
                });
    }

    public @Test void testResourceStore() {
        contextRunner.run(
                context ->
                        context.isTypeMatch("resourceStoreImpl", CatalogClientResourceStore.class));
    }

    public @Test void testResourceLoadersResourceStore() {
        contextRunner.run(
                context -> {
                    GeoServerResourceLoader resourceLoader =
                            context.getBean(GeoServerResourceLoader.class);
                    assertThat(
                            resourceLoader.getResourceStore(),
                            instanceOf(CatalogClientResourceStore.class));
                });
    }

    public @Test void testGeoserverFacade() {
        contextRunner.run(
                context ->
                        context.isTypeMatch("geoserverFacade", CatalogClientGeoServerFacade.class));
    }

    public @Test void testGeoserverLoader() {
        contextRunner.run(
                context ->
                        context.isTypeMatch(
                                "geoServerLoaderImpl", CatalogClientGeoServerLoader.class));
    }
}
