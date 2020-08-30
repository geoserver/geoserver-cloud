/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.clientintegration;

import java.io.IOException;
import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.catalog.app.CatalogServiceApplication;
import org.geoserver.cloud.catalog.test.CatalogTestData;
import org.geoserver.cloud.catalog.test.WebTestClientSupport;
import org.geoserver.cloud.catalog.test.WebTestClientSupportConfiguration;
import org.geoserver.config.GeoServerLoader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

/** Integration tests for the application using a data directory as catalog backend */
@SpringBootTest(
    classes = {CatalogServiceApplication.class, WebTestClientSupportConfiguration.class}
)
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@AutoConfigureWebTestClient(timeout = "360000")
public class CatalogClientIntegrationTest {

    protected @Autowired WebTestClientSupport clientSupport;

    protected @Autowired @Qualifier("catalog") Catalog catalog;
    protected @Autowired GeoServerLoader geoServerLoader;

    public @Rule CatalogTestData testData = CatalogTestData.empty(() -> catalog);

    protected WebTestClient client() {
        return clientSupport.get();
    }

    public @Test void crudTest() throws IOException {
        // CatalogTestClient<WorkspaceInfo> workspaces = clientSupport.workspaces();
        // WorkspaceInfo ws = testData.ws;
        // workspaces.create(ws);
        // WorkspaceInfo created = workspaces.findById(testData.ws).getResponseBody();
        // assertNotNull(created);
        // assertNotSame(ws, created);
        // workspaces.create(ws);
    }
}
