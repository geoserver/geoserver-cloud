/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.test.backend;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.geoserver.cloud.autoconfigure.catalog.DataDirectoryAutoConfiguration;
import org.geoserver.cloud.autoconfigure.testconfiguration.AutoConfigurationTestConfiguration;
import org.geoserver.cloud.config.catalog.GeoServerBackendProperties;
import org.geoserver.cloud.config.datadirectory.DataDirectoryBackendConfigurer;
import org.geoserver.cloud.config.datadirectory.NoServletContextDataDirectoryResourceStore;
import org.geoserver.config.DefaultGeoServerLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.Assume;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test {@link DataDirectoryBackendConfigurer} through {@link DataDirectoryAutoConfiguration} when
 * {@code geoserver.backend.data-directory.enabled=true}
 */
@SpringBootTest(
    classes = AutoConfigurationTestConfiguration.class,
    properties = {
        "geoserver.backend.data-directory.enabled=true",
        "geoserver.backend.data-directory.location=/tmp/data_dir_autoconfiguration_test"
    }
)
public class DataDirectoryAutoConfigurationTest extends GeoServerBackendConfigurerTest {

    private @Autowired GeoServerBackendProperties configProperties;

    public @Test void testProperties() {
        assertNotNull(configProperties);
        assertNotNull(configProperties.getDataDirectory().getLocation());
        assertEquals(
                "/tmp/data_dir_autoconfiguration_test",
                configProperties.getDataDirectory().getLocation().toString());
    }

    public @Test void testCatalog() {
        Assume.assumeTrue(rawCatalog instanceof org.geoserver.catalog.plugin.CatalogPlugin);
        assertThat(rawCatalog, instanceOf(org.geoserver.catalog.plugin.CatalogPlugin.class));
    }

    public @Test void testCatalogFacadeIsRawCatalogFacade() {
        Assume.assumeTrue(rawCatalog instanceof org.geoserver.catalog.plugin.CatalogPlugin);
        assertSame(
                rawCatalogFacade,
                ((org.geoserver.catalog.plugin.CatalogPlugin) rawCatalog).getRawFacade());
    }

    public @Test void testCatalogFacade() {
        Assume.assumeTrue(rawCatalog instanceof org.geoserver.catalog.plugin.CatalogPlugin);
        assertThat(
                rawCatalogFacade,
                instanceOf(org.geoserver.catalog.plugin.DefaultMemoryCatalogFacade.class));
        assertSame(
                rawCatalogFacade,
                ((org.geoserver.catalog.plugin.CatalogPlugin) rawCatalog).getRawFacade());
    }

    public @Test void testResourceLoader() {
        assertThat(resourceLoader, instanceOf(GeoServerResourceLoader.class));
    }

    public @Test void testGeoserverFacade() {
        assertThat(
                geoserverFacade,
                instanceOf(org.geoserver.config.plugin.RepositoryGeoServerFacade.class));
    }

    public @Test void testGeoserverLoader() {
        assertThat(geoserverLoader, instanceOf(DefaultGeoServerLoader.class));
    }

    public @Test void testResourceStoreImpl() {
        assertThat(resourceStoreImpl, instanceOf(NoServletContextDataDirectoryResourceStore.class));
    }
}
