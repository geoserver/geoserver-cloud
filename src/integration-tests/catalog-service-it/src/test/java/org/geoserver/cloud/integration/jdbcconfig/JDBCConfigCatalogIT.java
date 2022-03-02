/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.integration.jdbcconfig;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.cloud.integration.catalog.AbstractCatalogBackendIT;
import org.geoserver.cloud.integration.catalog.IntegrationTestConfiguration;
import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = IntegrationTestConfiguration.class,
        properties = {
            "geoserver.backend.jdbcconfig.enabled=true",
            "logging.level.org.geoserver.cloud.autoconfigure.bus=ERROR"
        })
public class JDBCConfigCatalogIT extends AbstractCatalogBackendIT {

    private @Autowired @Qualifier("catalogFacade") ExtendedCatalogFacade jdbcCatalogFacade;
    private @Autowired GeoServerResourceLoader resourceLoader;

    @Override
    protected Catalog createCatalog() {
        CatalogPlugin catalog = new CatalogPlugin(jdbcCatalogFacade);
        catalog.setResourceLoader(resourceLoader);
        return catalog;
    }

    public @Before void prepare() {
        data.deleteAll(rawCatalog);
        jdbcCatalogFacade.dispose(); // disposes internal caches
    }
}
