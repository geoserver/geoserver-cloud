/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.integration.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.catalog.plugin.CatalogImpl;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.cloud.catalog.app.CatalogServiceApplication;
import org.geoserver.cloud.catalog.client.impl.CatalogClientConfiguration;
import org.geoserver.cloud.catalog.client.repository.CatalogServiceClientRepository;
import org.geoserver.cloud.test.CatalogConformanceTest;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * {@link Catalog} integration and conformance tests for a {@link CatalogFacade} running off
 * catalog-service's client {@link CatalogServiceClientRepository repositories} hitting a real
 * back-end catalog-service.
 *
 * <p>A {@link Catalog} using the {@code catalog-service} as its back-end is a regular {@link
 * CatalogImpl} with an injected {@link CatalogFacade} whose {@link CatalogInfoRepository
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
        "spring.main.web-application-type=reactive",
        "server.port=15555",
        "geoserver.backend.catalog-service.uri=http://localhost:${server.port}"
    }
)
@RunWith(SpringRunner.class)
@ActiveProfiles("it.catalog-service")
public class CatalogServiceBackendConformanceTest extends CatalogConformanceTest {
    /**
     * WebFlux catalog-service catalog with backend as configured by
     * bootstrap-it.catalog-service.yml
     */
    private @Autowired @Qualifier("catalog") Catalog serverCatalog;

    private @Autowired @Qualifier("rawCatalogServiceFacade") CatalogFacade clientFacade;

    /** Client catalog through which to hit the server catalog */
    private Catalog clientCatalog;

    protected @Override Catalog createCatalog() {
        clientCatalog = new CatalogImpl(clientFacade);
        return clientCatalog;
    }

    /** Override to prune the server catalog instead of the client one */
    @After
    public @Override void deleteAll() {
        Collection<CatalogListener> listeners = new ArrayList<>(serverCatalog.getListeners());
        for (CatalogListener listener : listeners) {
            if (listener instanceof TestListener || listener instanceof ExceptionThrowingListener)
                serverCatalog.removeListener(listener);
        }
        super.data.deleteAll(serverCatalog);
    }

    public @Test void smokeClientServerTest() {
        WorkspaceInfo ws = new WorkspaceInfoImpl();
        ws.setName("testWs");
        clientFacade.add(ws);

        WorkspaceInfo workspace = clientCatalog.getDefaultWorkspace();
        assertNotNull(workspace);
        workspace.setName("changed");
        clientCatalog.save(workspace);
        WorkspaceInfo updated = clientCatalog.getDefaultWorkspace();
        assertEquals("changed", updated.getName());
    }
}
