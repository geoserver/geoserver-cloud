package org.geoserver.cloud.integration.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogImpl;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.cloud.catalog.app.CatalogServiceApplication;
import org.geoserver.cloud.catalog.client.impl.CatalogClientConfiguration;
import org.geoserver.cloud.catalog.client.impl.CloudCatalogFacade;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveCatalogApiClientConfiguration;
import org.geoserver.cloud.catalog.client.repository.CatalogServiceClientRepository;
import org.geoserver.cloud.test.CatalogTestData;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import reactivefeign.spring.config.EnableReactiveFeignClients;

/**
 * Integration tests for a {@link CatalogFacade} running off catalog-service's client
 * {@link CatalogServiceClientRepository repositories} hitting a real backend service.
 * <p>
 * A {@link Catalog} using the {@code catalog-service} as its backend is a regular
 * {@link CatalogImpl} with an injected {@link CatalogFacade} whose {@link CatalogInfoRepository
 * repositories} talk to the {@code catalog-service}, hence this integration test suite verifies the
 * functioning of such {@code CatalogFacade} against a live {@code catalog-service} instance through HTTP.
 */
@SpringBootTest(classes = { //
        CatalogServiceApplication.class, //
        ReactiveCatalogApiClientConfiguration.class, //
        CatalogClientConfiguration.class //
}, webEnvironment = WebEnvironment.DEFINED_PORT,
        properties = {"spring.main.web-application-type=reactive",
                "geoserver.backend.catalog-service.uri=http://localhost:${server.port}"})
@RunWith(SpringRunner.class)
@ActiveProfiles("it.catalog-service")
@EnableAutoConfiguration
@EnableReactiveFeignClients
@EnableFeignClients
public class CloudCatalogFacadeIntegrationTest {

    /**
     * WebFlux catalog-service catalog with backend as configured by
     * bootstrap-it.catalog-service.yml
     */
    private @Autowired @Qualifier("catalog") Catalog serverCatalog;

    private @Autowired CloudCatalogFacade clientFacade;

    public CatalogTestData data;

    /** Initializes the server catalog with the default test data from {@link CatalogTestData} */
    public @Before void setUp() {
        // delete all server catalog contents and re-create the test data
        data = CatalogTestData.initialized(() -> serverCatalog).initCatalog();
    }

    public @Test void testGetWorkspace() {
        WorkspaceInfo ws1 = data.workspaceA;
        WorkspaceInfo ws2 = data.workspaceB;
        WorkspaceInfo ws3 = data.workspaceC;
        WorkspaceInfo actual;
        actual = clientFacade.getWorkspace(ws1.getId());
        assertEquals(ws1, actual);
        actual = clientFacade.getWorkspace(ws2.getId());
        assertEquals(ws2, actual);
        actual = clientFacade.getWorkspace(ws3.getId());
        assertEquals(ws3, actual);
        assertNull(clientFacade.getWorkspace("non-existent"));
    }

    public @Test void testGetWorkspaces() {
        List<WorkspaceInfo> workspaces = clientFacade.getWorkspaces();
        assertNotNull(workspaces);
        assertEquals(3, workspaces.size());
    }

    public @Ignore("to be implemented") @Test void testAddWorkspaceInfo() {}

    public @Ignore("to be implemented") @Test void testRemoveWorkspaceInfo() {}

    public @Ignore("to be implemented") @Test void testSaveWorkspaceInfo() {}

    public @Ignore("to be implemented") @Test void testGetDefaultWorkspace() {}

    public @Ignore("to be implemented") @Test void testSetDefaultWorkspace() {}

    public @Ignore("to be implemented") @Test void testGetWorkspaceByName() {}

    public @Ignore("to be implemented") @Test void testAddStoreInfo() {}

    public @Ignore("to be implemented") @Test void testRemoveStoreInfo() {}

    public @Ignore("to be implemented") @Test void testSaveStoreInfo() {}

    public @Ignore("to be implemented") @Test void testGetStore() {}

    public @Ignore("to be implemented") @Test void testGetStoreByName() {}

    public @Ignore("to be implemented") @Test void testGetStoresByWorkspace() {}

    public @Ignore("to be implemented") @Test void testGetStores() {}

    public @Ignore("to be implemented") @Test void testGetDefaultDataStore() {}

    public @Ignore("to be implemented") @Test void testSetDefaultDataStore() {}

    public @Ignore("to be implemented") @Test void testAddResourceInfo() {}

    public @Ignore("to be implemented") @Test void testRemoveResourceInfo() {}

    public @Ignore("to be implemented") @Test void testSaveResourceInfo() {}

    public @Ignore("to be implemented") @Test void testGetResource() {}

    public @Ignore("to be implemented") @Test void testGetResourceByName() {}

    public @Ignore("to be implemented") @Test void testGetResources() {}

    public @Ignore("to be implemented") @Test void testGetResourcesByNamespace() {}

    public @Ignore("to be implemented") @Test void testGetResourceByStore() {}

    public @Ignore("to be implemented") @Test void testGetResourcesByStore() {}

    public @Ignore("to be implemented") @Test void testAddLayerInfo() {}

    public @Ignore("to be implemented") @Test void testRemoveLayerInfo() {}

    public @Ignore("to be implemented") @Test void testSaveLayerInfo() {}

    public @Ignore("to be implemented") @Test void testGetLayer() {}

    public @Ignore("to be implemented") @Test void testGetLayerByName() {}

    public @Ignore("to be implemented") @Test void testGetLayersResourceInfo() {}

    public @Ignore("to be implemented") @Test void testGetLayersStyleInfo() {}

    public @Ignore("to be implemented") @Test void testGetLayers() {}

    public @Ignore("to be implemented") @Test void testAddMapInfo() {}

    public @Ignore("to be implemented") @Test void testRemoveMapInfo() {}

    public @Ignore("to be implemented") @Test void testSaveMapInfo() {}

    public @Ignore("to be implemented") @Test void testGetMap() {}

    public @Ignore("to be implemented") @Test void testGetMapByName() {}

    public @Ignore("to be implemented") @Test void testGetMaps() {}

    public @Ignore("to be implemented") @Test void testAddLayerGroupInfo() {}

    public @Ignore("to be implemented") @Test void testRemoveLayerGroupInfo() {}

    public @Ignore("to be implemented") @Test void testSaveLayerGroupInfo() {}

    public @Ignore("to be implemented") @Test void testGetLayerGroups() {}

    public @Ignore("to be implemented") @Test void testGetLayerGroupsByWorkspace() {}

    public @Ignore("to be implemented") @Test void testGetLayerGroup() {}

    public @Ignore("to be implemented") @Test void testGetLayerGroupByNameString() {}

    public @Ignore("to be implemented") @Test void testGetLayerGroupByNameWorkspaceInfoString() {}

    public @Ignore("to be implemented") @Test void testAddNamespaceInfo() {}

    public @Ignore("to be implemented") @Test void testRemoveNamespaceInfo() {}

    public @Ignore("to be implemented") @Test void testSaveNamespaceInfo() {}

    public @Ignore("to be implemented") @Test void testGetDefaultNamespace() {}

    public @Ignore("to be implemented") @Test void testSetDefaultNamespace() {}

    public @Ignore("to be implemented") @Test void testGetNamespace() {}

    public @Ignore("to be implemented") @Test void testGetNamespaceByPrefix() {}

    public @Ignore("to be implemented") @Test void testGetNamespaceByURI() {}

    public @Ignore("to be implemented") @Test void testGetNamespacesByURI() {}

    public @Ignore("to be implemented") @Test void testGetNamespaces() {}

    public @Ignore("to be implemented") @Test void testAddStyleInfo() {}

    public @Ignore("to be implemented") @Test void testRemoveStyleInfo() {}

    public @Ignore("to be implemented") @Test void testSaveStyleInfo() {}

    public @Ignore("to be implemented") @Test void testGetStyle() {}

    public @Ignore("to be implemented") @Test void testGetStyleByNameString() {}

    public @Ignore("to be implemented") @Test void testGetStyleByNameWorkspaceInfoString() {}

    public @Ignore("to be implemented") @Test void testGetStyles() {}

    public @Ignore("to be implemented") @Test void testGetStylesByWorkspace() {}

    public @Ignore("to be implemented") @Test void testDispose() {}

    public @Ignore("to be implemented") @Test void testSyncTo() {}

    public @Ignore("to be implemented") @Test void testCount() {}

    public @Ignore("to be implemented") @Test void testCanSort() {}

    public @Ignore("to be implemented") @Test void testList() {}
}
