/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.integration.jdbcconfig;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.plugin.CatalogImpl;
import org.geoserver.cloud.test.CatalogConformanceTest;
import org.geoserver.cloud.testconfiguration.AutoConfigurationTestConfiguration;
import org.geoserver.jdbcconfig.catalog.JDBCCatalogFacade;
import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(
    classes = AutoConfigurationTestConfiguration.class,
    properties = {"geoserver.backend.jdbcconfig.enabled=true"}
)
@RunWith(SpringRunner.class)
public class JDBCConfigCatalogConformanceTest extends CatalogConformanceTest {

    private @Autowired @Qualifier("catalogFacade") JDBCCatalogFacade jdbcCatalogFacade;
    private @Autowired GeoServerResourceLoader resourceLoader;

    @Override
    protected Catalog createCatalog() {
        CatalogImpl catalog = new CatalogImpl(jdbcCatalogFacade);
        catalog.setResourceLoader(resourceLoader);
        return catalog;
    }

    public @Override void deleteAll() {
        super.deleteAll();
        jdbcCatalogFacade.dispose();
    }
}
