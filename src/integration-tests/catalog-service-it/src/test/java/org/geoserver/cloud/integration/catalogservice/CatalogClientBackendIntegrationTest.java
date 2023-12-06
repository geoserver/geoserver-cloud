/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.integration.catalogservice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.Lists;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.cloud.catalog.app.CatalogServiceApplication;
import org.geoserver.cloud.catalog.client.impl.CatalogClientConfiguration;
import org.geoserver.cloud.catalog.client.repository.CatalogClientRepository;
import org.geoserver.cloud.integration.catalog.AbstractCatalogBackendIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.geotools.api.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;

/**
 * {@link Catalog} integration and conformance tests for a {@link CatalogFacade} running off
 * catalog-service's client {@link CatalogClientRepository repositories} hitting a real back-end
 * catalog-service.
 *
 * <p>A {@link Catalog} using the {@code catalog-service} as its back-end is a regular {@link
 * CatalogPlugin} with an injected {@link CatalogFacade} whose {@link CatalogInfoRepository
 * repositories} talk to the {@code catalog-service}, hence this integration test suite verifies the
 * functioning of such {@code CatalogFacade} against a live {@code catalog-service} instance through
 * HTTP.
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
            "server.port=16666",
            "geoserver.backend.catalog-service.uri=http://localhost:${server.port}"
        })
@ActiveProfiles("it.catalog-service")
// REVISIT: fails if run with failsafe "ClassNotFoundException:
// org.geoserver.cloud.catalog.client.reactivefeign.ReactiveCatalogClient"
class CatalogClientBackendIntegrationTest extends AbstractCatalogBackendIT {
    /**
     * WebFlux catalog-service catalog with backend as configured by
     * bootstrap-it.catalog-service.yml
     */
    private @Autowired @Qualifier("catalog") Catalog serverCatalog;

    private @Autowired @Qualifier("rawCatalogServiceFacade") CatalogFacade clientFacade;

    /** Client catalog through which to hit the server catalog */
    private CatalogPlugin clientCatalog;

    protected @Override CatalogPlugin createCatalog() {
        clientCatalog = new CatalogPlugin(clientFacade);
        return clientCatalog;
    }

    public @BeforeEach void purgeServerCatalog() {
        serverCatalog.removeListeners(TestListener.class);
        serverCatalog.removeListeners(ExceptionThrowingListener.class);
        super.data.deleteAll(serverCatalog);
    }

    @Test void testQueryFilterInstanceOf() {
        super.data.addObjects();
        int expected = serverCatalog.getDataStores().size();
        assertThat(expected, greaterThan(0));

        Filter filter = Predicates.isInstanceOf(DataStoreInfo.class);
        ArrayList<StoreInfo> list = Lists.newArrayList(catalog.list(StoreInfo.class, filter));
        assertEquals(3, list.size());

        filter = Predicates.isInstanceOf(CoverageStoreInfo.class);
        list = Lists.newArrayList(catalog.list(StoreInfo.class, filter));
        assertEquals(1, list.size());

        filter = Predicates.isInstanceOf(WMSStoreInfo.class);
        list = Lists.newArrayList(catalog.list(StoreInfo.class, filter));
        assertEquals(1, list.size());

        filter = Predicates.isInstanceOf(WMTSStoreInfo.class);
        list = Lists.newArrayList(catalog.list(StoreInfo.class, filter));
        assertEquals(1, list.size());

        filter = Predicates.isInstanceOf(StoreInfo.class);
        list = Lists.newArrayList(catalog.list(StoreInfo.class, filter));
        assertEquals(6, list.size());
    }
}
