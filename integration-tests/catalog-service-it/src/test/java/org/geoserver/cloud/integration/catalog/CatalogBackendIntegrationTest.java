/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.integration.catalog;

import static org.junit.Assert.assertNotNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.cloud.catalog.app.CatalogServiceApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/** Integration tests for the application using a data directory as catalog backend */
@SpringBootTest(
    classes = CatalogServiceApplication.class,
    webEnvironment = WebEnvironment.RANDOM_PORT
)
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class CatalogBackendIntegrationTest {

    private @Autowired @Qualifier("catalog") Catalog serverCatalog;

    private Catalog clientCatalog;

    public @Test void test1() {
        WorkspaceInfo ws = new WorkspaceInfoImpl();
        ws.setName("testws");
        serverCatalog.add(ws);
        assertNotNull(serverCatalog.getWorkspaceByName("testws"));
    }
}
