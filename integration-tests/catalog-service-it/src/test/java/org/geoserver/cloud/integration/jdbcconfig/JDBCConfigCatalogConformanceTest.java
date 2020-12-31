/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.integration.jdbcconfig;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogConformanceTest;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.cloud.testconfiguration.IntegrationTestConfiguration;
import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(
    classes = IntegrationTestConfiguration.class,
    properties = {"geoserver.backend.jdbcconfig.enabled=true"}
)
@RunWith(SpringRunner.class)
public class JDBCConfigCatalogConformanceTest extends CatalogConformanceTest {

    private @Autowired @Qualifier("catalogFacade") ExtendedCatalogFacade jdbcCatalogFacade;
    private @Autowired GeoServerResourceLoader resourceLoader;

    @Override
    protected Catalog createCatalog() {
        CatalogPlugin catalog = new CatalogPlugin(jdbcCatalogFacade);
        catalog.setResourceLoader(resourceLoader);
        return catalog;
    }

    public @After void deleteAll() {
        data.deleteAll(rawCatalog);
        jdbcCatalogFacade.dispose();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testDataStoreEvents() {
        super.testDataStoreEvents();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testGetLayerGroupByNameWithWorkspace() {
        super.testGetLayerGroupByNameWithWorkspace();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testFullTextSearch() {
        super.testFullTextSearch();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testModifyMetadata() {
        super.testModifyMetadata();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testGetFeatureTypeByName() {
        super.testGetFeatureTypeByName();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testGetLayerById() {
        super.testGetLayerById();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testAddIsolatedWorkspace() {
        super.testAddIsolatedWorkspace();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testAddIsolatedNamespace() {
        super.testAddIsolatedNamespace();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testAutoSetDefaultStore() {
        super.testAutoSetDefaultStore();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testFullTextSearchLayerGroupAbstract() {
        super.testFullTextSearchLayerGroupAbstract();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testGetLayerByNameWithoutColon() {
        super.testGetLayerByNameWithoutColon();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testFullTextSearchKeywords() {
        super.testFullTextSearchKeywords();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testGetDataStoreByName() {
        super.testGetDataStoreByName();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testGetStoreByName() {
        super.testGetStoreByName();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testGetLayerByName() {
        super.testGetLayerByName();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testGetLayerByResource() {
        super.testGetLayerByResource();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testRemoveLayerGroupInLayerGroup() throws Exception {
        super.testRemoveLayerGroupInLayerGroup();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testFullTextSearchLayerGroupTitle() {
        super.testFullTextSearchLayerGroupTitle();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testFeatureTypeEvents() {
        super.testFeatureTypeEvents();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testIterablesHaveCatalogSet() {
        super.testIterablesHaveCatalogSet();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testGetLayerGroupByNameWithColon() {
        super.testGetLayerGroupByNameWithColon();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testAddWMSLayer() {
        super.testAddWMSLayer();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testAddWMTSStore() {
        super.testAddWMTSStore();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testLayerEvents() {
        super.testLayerEvents();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testLayerGroupRootLayer() {
        super.testLayerGroupRootLayer();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testFullTextSearchLayerGroupName() {
        super.testFullTextSearchLayerGroupName();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testGetFeatureTypeById() {
        super.testGetFeatureTypeById();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testGetDataStoreById() {
        super.testGetDataStoreById();
    }

    // @Ignore("equals fails with jdbcfacade, not worth fixing right now")
    public @Override @Test void testAddWMTSLayer() {
        super.testAddWMTSLayer();
    }

    // @Ignore
    public @Override @Test void testCountIncludeFilter() {
        super.testCountIncludeFilter();
    }
}
