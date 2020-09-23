/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.impl;

import static org.junit.Assert.assertNotNull;

import org.geoserver.jackson.databind.catalog.GeoServerCatalogModule;
import org.geoserver.jackson.databind.config.GeoServerConfigModule;
import org.geotools.jackson.databind.filter.GeoToolsFilterModule;
import org.geotools.jackson.databind.geojson.GeoToolsGeoJsonModule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = CatalogClientConfiguration.class)
@EnableAutoConfiguration
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class CatalogClientConfigurationTest {

    private @Autowired CatalogServiceCatalogFacade rawCatalogServiceFacade;
    private @Autowired GeoServerCatalogModule catalogJacksonModule;
    private @Autowired GeoServerConfigModule configJacksonModule;
    private @Autowired GeoToolsFilterModule geotoolsFilterModule;
    private @Autowired GeoToolsGeoJsonModule geotoolsGeoJSONModule;

    @Test
    public void smokeTest() {
        assertNotNull(rawCatalogServiceFacade);
        assertNotNull(catalogJacksonModule);
        assertNotNull(configJacksonModule);
        assertNotNull(geotoolsFilterModule);
        assertNotNull(geotoolsGeoJSONModule);
    }
}
