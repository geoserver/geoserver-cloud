/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.jdbcconfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.catalog.plugin.CatalogFacadeExtensionAdapter;
import org.geoserver.cloud.config.catalog.backend.jdbcconfig.CloudJdbcGeoServerLoader;
import org.geoserver.cloud.config.catalog.backend.jdbcconfig.CloudJdbcGeoserverFacade;
import org.geoserver.cloud.config.catalog.backend.jdbcconfig.JDBCConfigBackendConfigurer;
import org.geoserver.cloud.config.catalog.backend.jdbcconfig.JdbcConfigConfigurationProperties;
import org.geoserver.cloud.config.catalog.backend.jdbcconfig.JdbcConfigUpdateSequence;
import org.geoserver.jdbcconfig.catalog.JDBCCatalogFacade;
import org.geoserver.jdbcstore.JDBCResourceStore;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.config.UpdateSequence;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test {@link JDBCConfigBackendConfigurer} through {@link JDBCConfigAutoConfiguration} when {@code
 * geoserver.backend.jdbcconfig.enabled=true}
 */
@SpringBootTest(
        classes = AutoConfigurationTestConfiguration.class,
        properties = "geoserver.backend.jdbcconfig.enabled=true")
@RunWith(SpringRunner.class)
public class JDBCConfigAutoConfigurationTest extends JDBCConfigTest {

    private @Autowired JdbcConfigConfigurationProperties configProperties;

    public @Test void testCatalog() {
        assertThat(rawCatalog, instanceOf(CatalogImpl.class));
    }

    public @Test void testProperties() {
        assertNotNull(configProperties);
        assertNotNull(configProperties.getDatasource());
        assertNotNull(configProperties.getCacheDirectory());
        assertEquals(
                "/tmp/geoserver-jdbcconfig-cache", configProperties.getCacheDirectory().toString());
    }

    public @Test void testCatalogFacade() {
        assertThat(rawCatalogFacade, instanceOf(CatalogFacadeExtensionAdapter.class));
        assertThat(
                ((CatalogFacadeExtensionAdapter) rawCatalogFacade).getSubject(),
                instanceOf(JDBCCatalogFacade.class));
    }

    public @Test void testResourceLoader() {
        assertThat(resourceLoader, instanceOf(GeoServerResourceLoader.class));
    }

    public @Test void testGeoserverFacade() {
        assertThat(geoserverFacade, instanceOf(CloudJdbcGeoserverFacade.class));
    }

    public @Test void testGeoserverLoader() {
        assertThat(geoserverLoader, instanceOf(CloudJdbcGeoServerLoader.class));
    }

    public @Test void testResourceStoreImpl() {
        assertThat(resourceStoreImpl, instanceOf(JDBCResourceStore.class));
    }

    public @Test void crudTest() {
        Catalog catalog = (Catalog) context.getBean("catalog");
        WorkspaceInfoImpl ws = new WorkspaceInfoImpl();
        ws.setName("test-ws");
        catalog.add(ws);
        WorkspaceInfo readback = catalog.getWorkspaceByName("test-ws");
        assertEquals(ws, readback);
        catalog.remove(ws);
        assertNull(catalog.getWorkspaceByName("test-ws"));
    }

    public @Test void testUpdateSequence() {
        UpdateSequence updateSequence = context.getBean(UpdateSequence.class);
        assertThat(updateSequence, instanceOf(JdbcConfigUpdateSequence.class));
    }
}
