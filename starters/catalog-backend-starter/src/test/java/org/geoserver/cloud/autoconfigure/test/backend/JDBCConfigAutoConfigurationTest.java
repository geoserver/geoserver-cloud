/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.test.backend;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.IntStream;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.catalog.plugin.CatalogFacadeExtensionAdapter;
import org.geoserver.cloud.autoconfigure.catalog.JDBCConfigAutoConfiguration;
import org.geoserver.cloud.autoconfigure.testconfiguration.AutoConfigurationTestConfiguration;
import org.geoserver.cloud.config.catalog.GeoServerBackendProperties;
import org.geoserver.cloud.config.jdbcconfig.CloudJdbcGeoServerLoader;
import org.geoserver.cloud.config.jdbcconfig.CloudJdbcGeoserverFacade;
import org.geoserver.cloud.config.jdbcconfig.JDBCConfigBackendConfigurer;
import org.geoserver.jdbcconfig.catalog.JDBCCatalogFacade;
import org.geoserver.jdbcstore.JDBCResourceStore;
import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test {@link JDBCConfigBackendConfigurer} through {@link JDBCConfigAutoConfiguration} when {@code
 * geoserver.backend.jdbcconfig.enabled=true}
 */
@SpringBootTest(
    classes = AutoConfigurationTestConfiguration.class,
    properties = {"geoserver.backend.jdbcconfig.enabled=true"}
)
public class JDBCConfigAutoConfigurationTest extends JDBCConfigTest {

    public @Rule CatalogTestData data = CatalogTestData.empty(() -> catalog, () -> geoServer);

    public @Test void testCatalog() {
        assertThat(rawCatalog, instanceOf(CatalogImpl.class));
        GeoServerBackendProperties props = context.getBean(GeoServerBackendProperties.class);
        System.err.println(props);
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

    public @Test void crudTestFeatureTypeMultiThreading() {
        assertNotNull(data.featureTypeA);
        Catalog catalog = super.catalog;
        catalog.add(data.workspaceA);
        catalog.add(data.namespaceA);
        catalog.add(data.dataStoreA);
        catalog.add(data.featureTypeA);

        //        new ConfigDatabase(dataSource, null)
        IntStream.range(0, 100)
                .mapToObj(i -> data.createFeatureType("type-" + i))
                .forEach(catalog::add);

        FeatureTypeInfo featureType = catalog.getFeatureType(data.featureTypeA.getId());
        assertNotNull(featureType);
    }
}
