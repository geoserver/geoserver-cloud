/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.datadir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.cloud.config.catalog.backend.datadirectory.DataDirectoryBackendConfiguration;
import org.geoserver.cloud.config.catalog.backend.datadirectory.DataDirectoryProperties;
import org.geoserver.cloud.config.catalog.backend.datadirectory.DataDirectoryUpdateSequence;
import org.geoserver.cloud.config.catalog.backend.datadirectory.NoServletContextDataDirectoryResourceStore;
import org.geoserver.config.DefaultGeoServerLoader;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.platform.resource.ResourceStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test {@link DataDirectoryBackendConfiguration} through {@link DataDirectoryAutoConfiguration}
 * when {@code geoserver.backend.data-directory.enabled=true}
 */
@SpringBootTest(
        classes = DataDirectoryTestConfiguration.class, //
        properties = {
            "geoserver.backend.dataDirectory.enabled=true",
            "geoserver.backend.dataDirectory.location=/tmp/data_dir_autoconfiguration_test"
        })
@ActiveProfiles("test")
public class DataDirectoryAutoConfigurationTest {

    private @Autowired DataDirectoryProperties configProperties;
    protected @Autowired ApplicationContext context;

    protected @Autowired @Qualifier("catalog") Catalog catalog;
    protected @Autowired @Qualifier("rawCatalog") Catalog rawCatalog;
    protected @Autowired @Qualifier("catalogFacade") CatalogFacade rawCatalogFacade;
    protected @Autowired @Qualifier("geoServer") GeoServer geoServer;
    protected @Autowired @Qualifier("resourceLoader") GeoServerResourceLoader resourceLoader;
    protected @Autowired(required = false) @Qualifier("geoserverFacade") GeoServerFacade
            geoserverFacade;
    protected @Autowired @Qualifier("geoServerLoaderImpl") GeoServerLoader geoserverLoader;
    protected @Autowired @Qualifier("resourceStoreImpl") ResourceStore resourceStoreImpl;

    public @Test void testProperties() {
        assertNotNull(configProperties);
        assertNotNull(configProperties.getLocation());
        assertEquals(
                "/tmp/data_dir_autoconfiguration_test", configProperties.getLocation().toString());
    }

    public @Test void testCatalog() {
        assertThat(rawCatalog, instanceOf(org.geoserver.catalog.plugin.CatalogPlugin.class));
    }

    public @Test void testCatalogFacadeIsRawCatalogFacade() {
        assertSame(
                rawCatalogFacade,
                ((org.geoserver.catalog.plugin.CatalogPlugin) rawCatalog).getRawFacade());
    }

    public @Test void testCatalogFacade() {
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

    public @Test void testUpdateSequence() {
        assertThat(
                context.getBean(UpdateSequence.class),
                instanceOf(DataDirectoryUpdateSequence.class));
    }
}
