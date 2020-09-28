/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.integration.datadirectory;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogConformanceTest;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.plugin.CatalogImpl;
import org.geoserver.cloud.testconfiguration.IntegrationTestConfiguration;
import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(
    classes = IntegrationTestConfiguration.class,
    properties = {"geoserver.backend.data-directory.enabled=true"}
)
@RunWith(SpringRunner.class)
public class DataDirectoryCatalogConformanceTest extends CatalogConformanceTest {

    private @Autowired @Qualifier("catalogFacade") CatalogFacade rawCatalogFacade;
    private @Autowired GeoServerResourceLoader resourceLoader;

    @Override
    protected Catalog createCatalog() {
        CatalogImpl catalog = new CatalogImpl(rawCatalogFacade);
        catalog.setResourceLoader(resourceLoader);
        return catalog;
    }
}
