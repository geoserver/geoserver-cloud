/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.geoserver.jackson.databind.catalog.GeoServerCatalogModule;
import org.geoserver.jackson.databind.config.GeoServerConfigModule;
import org.geotools.jackson.databind.filter.GeoToolsFilterModule;
import org.geotools.jackson.databind.geojson.GeoToolsGeoJsonModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = CatalogClientConfiguration.class)
@EnableAutoConfiguration
@ActiveProfiles("test")
class CatalogClientConfigurationTest {

    private @Autowired CatalogClientCatalogFacade rawCatalogServiceFacade;

    private @Autowired GeoServerCatalogModule catalogJacksonModule;
    private @Autowired GeoServerConfigModule configJacksonModule;
    private @Autowired GeoToolsFilterModule geotoolsFilterModule;
    private @Autowired GeoToolsGeoJsonModule geotoolsGeoJSONModule;

    @Test
    void smokeTest() {
        assertNotNull(rawCatalogServiceFacade);
        assertNotNull(catalogJacksonModule);
        assertNotNull(configJacksonModule);
        assertNotNull(geotoolsFilterModule);
        assertNotNull(geotoolsGeoJSONModule);
    }
}
