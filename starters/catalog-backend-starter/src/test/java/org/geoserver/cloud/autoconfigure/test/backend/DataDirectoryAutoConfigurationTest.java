package org.geoserver.cloud.autoconfigure.test.backend;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.geoserver.cloud.autoconfigure.catalog.DataDirectoryAutoConfiguration;
import org.geoserver.cloud.autoconfigure.testconfiguration.AutoConfigurationTestConfiguration;
import org.geoserver.cloud.config.datadirectory.DataDirectoryBackendConfigurer;
import org.geoserver.cloud.config.datadirectory.DataDirectoryProperties;
import org.geoserver.cloud.config.datadirectory.NoServletContextDataDirectoryResourceStore;
import org.geoserver.config.DefaultGeoServerLoader;
import org.geoserver.platform.GeoServerResourceLoader;
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

    private @Autowired DataDirectoryProperties configProperties;

    public @Test void testProperties() {
        assertNotNull(configProperties);
        assertNotNull(configProperties.getLocation());
        assertEquals(
                "/tmp/data_dir_autoconfiguration_test", configProperties.getLocation().toString());
    }

    public @Test void testCatalog() {
        assertThat(rawCatalog, instanceOf(org.geoserver.catalog.plugin.CatalogImpl.class));
    }

    public @Test void testCatalogFacadeIsRawCatalogFacade() {
        assertSame(
                rawCatalogFacade,
                ((org.geoserver.catalog.plugin.CatalogImpl) rawCatalog).getRawCatalogFacade());
    }

    public @Test void testCatalogFacade() {
        assertThat(
                rawCatalogFacade,
                instanceOf(org.geoserver.catalog.plugin.DefaultCatalogFacade.class));
        assertSame(
                rawCatalogFacade,
                ((org.geoserver.catalog.plugin.CatalogImpl) rawCatalog).getRawCatalogFacade());
    }

    public @Test void testResourceLoader() {
        assertThat(resourceLoader, instanceOf(GeoServerResourceLoader.class));
    }

    public @Test void testGeoserverFacade() {
        assertThat(geoserverFacade, nullValue());
    }

    public @Test void testGeoserverLoader() {
        assertThat(geoserverLoader, instanceOf(DefaultGeoServerLoader.class));
    }

    public @Test void testResourceStoreImpl() {
        assertThat(resourceStoreImpl, instanceOf(NoServletContextDataDirectoryResourceStore.class));
    }
}
