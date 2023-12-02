/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.integration.catalogservice;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.cloud.catalog.app.CatalogServiceApplication;
import org.geoserver.cloud.catalog.client.impl.CatalogClientConfiguration;
import org.geoserver.cloud.catalog.client.impl.CatalogClientGeoServerFacade;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveConfigClient;
import org.geoserver.cloud.catalog.client.repository.CatalogClientConfigRepository;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerConfigConformanceTest;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.wms.WMSInfoImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

/**
 * API {@link GeoServerConfigConformanceTest conformance test} for a {@link GeoServer} configuration
 * manager backed by a {@link CatalogClientGeoServerFacade} (which in turn is backed by a {@link
 * CatalogClientConfigRepository}), which in turn is backed by a {@link ReactiveConfigClient},
 * hitting a live {@code catalog-service} instance.
 */
@SpringBootTest(
        classes = { //
            CatalogServiceApplication.class, //
            CatalogClientConfiguration.class //
        },
        webEnvironment = WebEnvironment.DEFINED_PORT,
        properties = {
            "spring.cloud.circuitbreaker.hystrix.enabled=false",
            "spring.main.web-application-type=reactive",
            "server.port=15555",
            "geoserver.backend.catalog-service.uri=http://localhost:${server.port}"
        })
@ActiveProfiles("it.catalog-service")
public class CatalogClientGeoServerFacadeConformanceTest extends GeoServerConfigConformanceTest {

    /**
     * WebFlux catalog-service catalog with backend as configured by
     * bootstrap-it.catalog-service.yml
     */
    private @Autowired @Qualifier("catalog") Catalog serverCatalog;

    private @Autowired @Qualifier("geoServer") GeoServer serverConfig;

    private @Autowired CatalogClientGeoServerFacade facade;
    private @Autowired @Qualifier("rawCatalogServiceFacade") CatalogFacade clientFacade;

    protected @Override GeoServer createGeoServer() {
        Catalog catalog = new CatalogPlugin(clientFacade);

        GeoServerImpl gs = new GeoServerImpl();
        gs.setCatalog(catalog);
        gs.setFacade(facade);
        return gs;
    }

    protected @Override ServiceInfo createService() {
        return new WMSInfoImpl();
    }

    /** prune the server catalog */
    @AfterEach
    public void deleteAll() {
        CatalogTestData.empty(() -> serverCatalog, () -> serverConfig).deleteAll();
    }

    /**
     * Test works if run alone, but not if it runs after other tests cause server config is not null
     */
    @Override public  @Test void testGlobal() throws Exception {
        Assumptions.assumeTrue(serverConfig.getGlobal() == null);
        super.testGlobal();
    }

    /**
     * Test works if run alone, but not if it runs after other tests cause server config is not null
     */
    @Override public  @Test void testModifyGlobal() throws Exception {
        Assumptions.assumeTrue(serverConfig.getGlobal() == null);
        super.testModifyGlobal();
    }
}
