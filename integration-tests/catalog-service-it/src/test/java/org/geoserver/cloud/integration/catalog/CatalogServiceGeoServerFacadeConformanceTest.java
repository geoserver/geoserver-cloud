/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.integration.catalog;

import org.geoserver.catalog.plugin.CatalogImpl;
import org.geoserver.cloud.catalog.app.CatalogServiceApplication;
import org.geoserver.cloud.catalog.client.impl.CatalogClientConfiguration;
import org.geoserver.cloud.catalog.client.impl.CatalogServiceGeoServerFacade;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerConfigConformanceTest;
import org.geoserver.config.plugin.GeoServerImpl;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(
    classes = { //
        CatalogServiceApplication.class, //
        CatalogClientConfiguration.class //
    },
    webEnvironment = WebEnvironment.DEFINED_PORT,
    properties = {
        "spring.main.web-application-type=reactive",
        "server.port=15555",
        "geoserver.backend.catalog-service.uri=http://localhost:${server.port}"
    }
)
@RunWith(SpringRunner.class)
@ActiveProfiles("it.catalog-service")
public class CatalogServiceGeoServerFacadeConformanceTest extends GeoServerConfigConformanceTest {

    private @Autowired CatalogServiceGeoServerFacade facade;

    protected @Override GeoServer createGeoServer() {

        GeoServerImpl gs = new GeoServerImpl();
        gs.setCatalog(new CatalogImpl());
        gs.setFacade(facade);
        return gs;
    }
}
