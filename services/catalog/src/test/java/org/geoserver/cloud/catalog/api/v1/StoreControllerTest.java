/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.HTTPStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.cloud.catalog.test.CatalogTestClient;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;

@AutoConfigureWebTestClient(timeout = "360000")
public class StoreControllerTest extends AbstractCatalogInfoControllerTest<StoreInfo> {

    public StoreControllerTest() {
        super(StoreController.BASE_URI, StoreInfo.class);
    }

    protected @Override void assertPropertriesEqual(StoreInfo expected, StoreInfo actual) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        // connection params may have been serialized as string
        final Map<String, Serializable> cm1 =
                expected.getConnectionParameters() == null
                        ? new HashMap<>()
                        : expected.getConnectionParameters();
        final Map<String, Serializable> cm2 =
                actual.getConnectionParameters() == null
                        ? new HashMap<>()
                        : actual.getConnectionParameters();
        assertEquals(cm1.size(), cm2.size());
        cm1.forEach((k, v) -> assertEquals(String.valueOf(v), String.valueOf(cm2.get(k))));
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getWorkspace(), actual.getWorkspace());
        assertEquals(expected.isEnabled(), actual.isEnabled());
        if (expected instanceof CoverageStoreInfo)
            assertEquals(
                    ((CoverageStoreInfo) expected).getURL(), ((CoverageStoreInfo) actual).getURL());
        if (expected instanceof HTTPStoreInfo)
            assertEquals(
                    ((HTTPStoreInfo) expected).getCapabilitiesURL(),
                    ((HTTPStoreInfo) actual).getCapabilitiesURL());
    }

    public @Test void dataStoreCRUD() throws IOException {
        DataStoreInfo store =
                testData.createDataStore(
                        "dataStoreCRUD-id",
                        testData.workspaceB,
                        "dataStoreCRUD",
                        "dataStoreCRUD description",
                        true);
        crudTest(
                store,
                s -> {
                    s.setEnabled(false);
                    s.setName("modified name");
                    s.setDescription("modified description");
                    s.getConnectionParameters().put("newkey", "new param");
                    return;
                },
                catalog::getDataStore);
    }

    public @Test void coverageStoreCRUD() {
        CoverageStoreInfo store =
                testData.createCoverageStore(
                        "coverageStoreCRUD",
                        testData.workspaceC,
                        "coverageStoreCRUD name",
                        "GeoTIFF",
                        "file:/test/coverageStoreCRUD.tiff");
        crudTest(
                store,
                s -> {
                    s.setEnabled(false);
                    s.setName("modified name");
                    s.setDescription("modified description");
                    ((CoverageStoreInfo) s).setURL("file:/test/coverageStoreCRUD_modified.tiff");
                    return;
                },
                catalog::getCoverageStore);
    }

    public @Test void wmsStoreCRUD() {
        WMSStoreInfo store =
                testData.createWebMapServer(
                        "wmsStoreCRUD",
                        testData.workspaceA,
                        "wmsStoreCRUD_name",
                        "http://test.com",
                        true);
        crudTest(
                store,
                s -> {
                    s.setEnabled(false);
                    s.setName("modified name");
                    s.setDescription("modified description");
                    ((WMSStoreInfo) s).setCapabilitiesURL("http://new.caps.url");
                    return;
                },
                id -> catalog.getStore(id, WMSStoreInfo.class));
    }

    public @Test void wmtsStoreCRUD() {
        WMTSStoreInfo store =
                testData.createWebMapTileServer(
                        "wmsStoreCRUD",
                        testData.workspaceA,
                        "wmtsStoreCRUD_name",
                        "http://test.com",
                        true);
        crudTest(
                store,
                s -> {
                    s.setEnabled(false);
                    s.setName("modified name");
                    s.setDescription("modified description");
                    ((WMTSStoreInfo) s).setCapabilitiesURL("http://new.caps.url");
                    return;
                },
                id -> catalog.getStore(id, WMTSStoreInfo.class));
    }

    public @Test void findStoreById() throws IOException {
        testFindById(testData.coverageStoreA);
        testFindById(testData.dataStoreA);
        testFindById(testData.dataStoreB);
        testFindById(testData.wmsStoreA);
        testFindById(testData.wmtsStoreA);
    }

    public @Test void findStoreById_SubtypeMismatch() throws IOException {
        CatalogTestClient<StoreInfo> client = client();
        client.findById(testData.coverageStoreA.getId(), DataStoreInfo.class)
                .expectStatus()
                .isNotFound();
        client.findById(testData.dataStoreA.getId(), CoverageStoreInfo.class)
                .expectStatus()
                .isNotFound();
        client.findById(testData.dataStoreB.getId(), CoverageStoreInfo.class)
                .expectStatus()
                .isNotFound();
    }

    public @Test void findStoreByName() throws IOException {
        findStoreByName(testData.coverageStoreA);
        findStoreByName(testData.dataStoreA);
        findStoreByName(testData.dataStoreB);
        findStoreByName(testData.wmsStoreA);
        findStoreByName(testData.wmtsStoreA);
    }

    private void findStoreByName(StoreInfo store) {
        StoreInfo responseBody =
                client().findByName(store.getName(), store)
                        .expectStatus()
                        .isOk()
                        .expectBody(infoType)
                        .returnResult()
                        .getResponseBody();
        assertCatalogInfoEquals(store, responseBody);
    }

    public @Test void findStoreByWorkspaceAndName() throws IOException {
        findStoreByWorkspaceAndName(testData.coverageStoreA);
        findStoreByWorkspaceAndName(testData.dataStoreA);
        findStoreByWorkspaceAndName(testData.dataStoreB);
        findStoreByWorkspaceAndName(testData.wmsStoreA);
        findStoreByWorkspaceAndName(testData.wmtsStoreA);
    }

    private void findStoreByWorkspaceAndName(StoreInfo store) {
        String workspaceName = store.getWorkspace().getName();
        StoreInfo responseBody =
                client().findByName(store.getName(), workspaceName, store, infoType)
                        .expectStatus()
                        .isOk()
                        .expectBody(infoType)
                        .returnResult()
                        .getResponseBody();
        ;
        assertCatalogInfoEquals(store, responseBody);
    }

    public @Test void findStoreByName_WrongWorkspace() throws IOException {
        findStoreByName_WrongWorkspace(testData.coverageStoreA, testData.workspaceC.getName());
        findStoreByName_WrongWorkspace(testData.dataStoreA, testData.workspaceC.getName());
        findStoreByName_WrongWorkspace(testData.dataStoreB, testData.workspaceC.getName());
        findStoreByName_WrongWorkspace(testData.wmsStoreA, testData.workspaceC.getName());
        findStoreByName_WrongWorkspace(testData.wmtsStoreA, testData.workspaceC.getName());
    }

    private void findStoreByName_WrongWorkspace(StoreInfo store, String workspaceName) {
        assertNotEquals(store.getWorkspace().getName(), workspaceName);
        client().findByName(store.getName(), workspaceName, null, infoType)
                .expectStatus()
                .isNotFound();
    }
}
