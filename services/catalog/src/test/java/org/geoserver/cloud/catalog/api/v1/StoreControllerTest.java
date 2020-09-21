/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.HTTPStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.cloud.catalog.test.CatalogTestClient;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

@AutoConfigureWebTestClient(timeout = "360000")
public class StoreControllerTest extends AbstractReactiveCatalogControllerTest<StoreInfo> {

    public StoreControllerTest() {
        super(StoreInfo.class);
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

    public @Override @Test void testFindAll() {
        super.testFindAll(
                testData.dataStoreA,
                testData.dataStoreB,
                testData.dataStoreC,
                testData.coverageStoreA,
                testData.wmsStoreA,
                testData.wmtsStoreA);
    }

    public @Override @Test void testFindById() {
        super.testFindById(testData.dataStoreA);
        super.testFindById(testData.coverageStoreA);
        super.testFindById(testData.wmsStoreA);
        super.testFindById(testData.wmtsStoreA);
    }

    public @Override @Test void testFindAllByType() {
        super.testFindAll(
                StoreInfo.class,
                testData.dataStoreA,
                testData.dataStoreB,
                testData.dataStoreC,
                testData.coverageStoreA,
                testData.wmsStoreA,
                testData.wmtsStoreA);

        super.testFindAll(
                DataStoreInfo.class, testData.dataStoreA, testData.dataStoreB, testData.dataStoreC);
        super.testFindAll(CoverageStoreInfo.class, testData.coverageStoreA);
        super.testFindAll(WMSStoreInfo.class, testData.wmsStoreA);
        super.testFindAll(WMTSStoreInfo.class, testData.wmtsStoreA);
    }

    public @Override @Test void testQueryFilter() {
        DataStoreInfo ds1 = catalog.getDataStore(testData.dataStoreA.getId());
        DataStoreInfo ds2 = catalog.getDataStore(testData.dataStoreB.getId());
        DataStoreInfo ds3 = catalog.getDataStore(testData.dataStoreC.getId());
        CoverageStoreInfo cs1 = catalog.getCoverageStore(testData.coverageStoreA.getId());
        WMSStoreInfo wmss1 = catalog.getStore(testData.wmsStoreA.getId(), WMSStoreInfo.class);
        WMTSStoreInfo wmtss1 = catalog.getStore(testData.wmtsStoreA.getId(), WMTSStoreInfo.class);

        super.testQueryFilter(StoreInfo.class, Filter.INCLUDE, ds1, ds2, ds3, cs1, wmss1, wmtss1);
        super.testQueryFilter(StoreInfo.class, Filter.EXCLUDE);
        super.testQueryFilter(DataStoreInfo.class, Filter.INCLUDE, ds1, ds2, ds3);
        super.testQueryFilter(CoverageStoreInfo.class, Filter.INCLUDE, cs1);
        super.testQueryFilter(WMSStoreInfo.class, Filter.INCLUDE, wmss1);
        super.testQueryFilter(WMTSStoreInfo.class, Filter.INCLUDE, wmtss1);

        String ecql = String.format("\"workspace.name\" = '%s'", testData.workspaceA.getName());
        super.testQueryFilter(ecql, ds1, cs1, wmss1, wmtss1);
        super.testQueryFilter(WMSStoreInfo.class, ecql, wmss1);
        super.testQueryFilter(DataStoreInfo.class, ecql, ds1);

        ecql = String.format("\"workspace.id\" = '%s'", testData.workspaceB.getId());
        super.testQueryFilter(ecql, ds2);
    }

    public @Test void testDataStoreInfo_CRUD() throws IOException {
        DataStoreInfo store =
                testData.createDataStore(
                        "dataStoreCRUD-id",
                        testData.workspaceB,
                        "dataStoreCRUD",
                        "dataStoreCRUD description",
                        true);
        crudTest(
                store,
                catalog::getDataStore,
                created -> {
                    created.setEnabled(false);
                    created.setName("modified name");
                    created.setDescription("modified description");
                    created.getConnectionParameters().put("newkey", "new param");
                    return;
                },
                (orig, updated) -> {
                    assertFalse(updated.isEnabled());
                    assertEquals("modified name", updated.getName());
                    assertEquals("modified description", updated.getDescription());
                    assertEquals("new param", updated.getConnectionParameters().get("newkey"));
                });
    }

    public @Test void testCoverageStoreInfo_CRUD() {
        CoverageStoreInfo store =
                testData.createCoverageStore(
                        "coverageStoreCRUD",
                        testData.workspaceC,
                        "coverageStoreCRUD name",
                        "GeoTIFF",
                        "file:/test/coverageStoreCRUD.tiff");
        crudTest(
                store,
                catalog::getCoverageStore,
                created -> {
                    created.setEnabled(false);
                    created.setName("modified name");
                    created.setDescription("modified description");
                    ((CoverageStoreInfo) created)
                            .setURL("file:/test/coverageStoreCRUD_modified.tiff");
                    return;
                },
                (orig, updated) -> {
                    assertFalse(updated.isEnabled());
                    assertEquals("modified name", updated.getName());
                    assertEquals("modified description", updated.getDescription());
                    assertEquals(
                            "file:/test/coverageStoreCRUD_modified.tiff",
                            ((CoverageStoreInfo) updated).getURL());
                });
    }

    public @Test void testWMSStoreInfo_CRUD() {
        WMSStoreInfo store =
                testData.createWebMapServer(
                        "wmsStoreCRUD",
                        testData.workspaceA,
                        "wmsStoreCRUD_name",
                        "http://test.com",
                        true);
        crudTest(
                store,
                id -> catalog.getStore(id, WMSStoreInfo.class),
                created -> {
                    created.setEnabled(false);
                    created.setName("modified name");
                    created.setDescription("modified description");
                    ((WMSStoreInfo) created).setCapabilitiesURL("http://new.caps.url");
                    return;
                },
                (orig, updated) -> {
                    assertFalse(updated.isEnabled());
                    assertEquals("modified name", updated.getName());
                    assertEquals("modified description", updated.getDescription());
                    assertEquals(
                            "http://new.caps.url", ((WMSStoreInfo) updated).getCapabilitiesURL());
                });
    }

    public @Test void testWMTSStoreInfo_CRUD() {
        WMTSStoreInfo store =
                testData.createWebMapTileServer(
                        "wmsStoreCRUD",
                        testData.workspaceA,
                        "wmtsStoreCRUD_name",
                        "http://test.com",
                        true);
        crudTest(
                store,
                id -> catalog.getStore(id, WMTSStoreInfo.class),
                created -> {
                    created.setEnabled(false);
                    created.setName("modified name");
                    created.setDescription("modified description");
                    ((WMTSStoreInfo) created).setCapabilitiesURL("http://new.caps.url");
                    return;
                },
                (orig, updated) -> {
                    assertFalse(updated.isEnabled());
                    assertEquals("modified name", updated.getName());
                    assertEquals("modified description", updated.getDescription());
                    assertEquals(
                            "http://new.caps.url", ((WMTSStoreInfo) updated).getCapabilitiesURL());
                });
    }

    public @Test void testFindStoreById() throws IOException {
        testFindById(testData.coverageStoreA);
        testFindById(testData.dataStoreA);
        testFindById(testData.dataStoreB);
        testFindById(testData.wmsStoreA);
        testFindById(testData.wmtsStoreA);
    }

    public @Test void testFindStoreById_SubtypeMismatch() throws IOException {
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

    public @Test void testFindStoreByName() throws IOException {
        findStoreByName(testData.coverageStoreA);
        findStoreByName(testData.dataStoreA);
        findStoreByName(testData.dataStoreB);
        findStoreByName(testData.wmsStoreA);
        findStoreByName(testData.wmtsStoreA);
    }

    private void findStoreByName(StoreInfo store) {
        StoreInfo responseBody = client().getFirstByName(store.getName());
        StoreInfo resolved = resolveProxies(responseBody);
        assertCatalogInfoEquals(store, resolved);
    }

    public @Test void testFindStoreByWorkspaceAndName() throws IOException {
        testFindStoreByWorkspaceAndName(testData.coverageStoreA, null);
        testFindStoreByWorkspaceAndName(testData.coverageStoreA, ClassMappings.COVERAGESTORE);

        testFindStoreByWorkspaceAndName(testData.dataStoreA, null);
        testFindStoreByWorkspaceAndName(testData.dataStoreA, ClassMappings.DATASTORE);

        testFindStoreByWorkspaceAndName(testData.dataStoreB, null);
        testFindStoreByWorkspaceAndName(testData.dataStoreB, ClassMappings.DATASTORE);

        testFindStoreByWorkspaceAndName(testData.wmsStoreA, null);
        testFindStoreByWorkspaceAndName(testData.wmsStoreA, ClassMappings.WMSSTORE);

        testFindStoreByWorkspaceAndName(testData.wmtsStoreA, null);
        testFindStoreByWorkspaceAndName(testData.wmtsStoreA, ClassMappings.WMTSSTORE);
    }

    private void testFindStoreByWorkspaceAndName(StoreInfo store, @Nullable ClassMappings subType) {
        String workspaceId = store.getWorkspace().getId();
        String name = store.getName();

        StoreInfo found =
                client().getRelative(
                                "/workspaces/{workspaceId}/stores/name/{name}?type={subType}",
                                workspaceId,
                                name,
                                subType)
                        .expectStatus()
                        .isOk()
                        .expectBody(StoreInfo.class)
                        .returnResult()
                        .getResponseBody();
        assertEquals(store.getId(), found.getId());
        assertEquals(store.getName(), found.getName());
    }

    public @Test void testFindStoreByName_WrongWorkspace() throws IOException {
        testFindStoreByName_WrongWorkspace(testData.coverageStoreA, testData.workspaceC.getName());
        testFindStoreByName_WrongWorkspace(testData.dataStoreA, testData.workspaceC.getName());
        testFindStoreByName_WrongWorkspace(testData.dataStoreB, testData.workspaceC.getName());
        testFindStoreByName_WrongWorkspace(testData.wmsStoreA, testData.workspaceC.getName());
        testFindStoreByName_WrongWorkspace(testData.wmtsStoreA, testData.workspaceC.getName());
    }

    private void testFindStoreByName_WrongWorkspace(StoreInfo store, String workspaceId) {
        String name = store.getName();
        ClassMappings subType = null;
        client().getRelative(
                        "/workspaces/{workspaceId}/stores/{name}?type={subType}",
                        workspaceId,
                        name,
                        subType)
                .expectStatus()
                .isNotFound();
    }

    public @Test void testFindStoresByWorkspace() {
        testFindStoresByWorkspace(
                testData.workspaceA,
                testData.dataStoreA,
                testData.coverageStoreA,
                testData.wmsStoreA,
                testData.wmtsStoreA);
        testFindStoresByWorkspace(testData.workspaceB, testData.dataStoreB);
        WorkspaceInfo emptyWs = testData.createWorkspace("emptyws");
        NamespaceInfo emptyNs = testData.createNamespace("emptyns", "http://test.com/emptyns");
        catalog.add(emptyWs);
        catalog.add(emptyNs);
        testFindStoresByWorkspace(emptyWs);
    }

    public void testFindStoresByWorkspace(WorkspaceInfo ws, StoreInfo... expected) {
        List<StoreInfo> stores =
                client().getRelative("/workspaces/{workspaceId}/stores", ws.getId())
                        .expectStatus()
                        .isOk()
                        .expectHeader()
                        .contentType(MediaType.APPLICATION_STREAM_JSON)
                        .expectBodyList(StoreInfo.class)
                        .returnResult()
                        .getResponseBody();

        Set<String> expectedIds =
                Arrays.stream(expected).map(StoreInfo::getId).collect(Collectors.toSet());
        Set<String> actual = stores.stream().map(StoreInfo::getId).collect(Collectors.toSet());

        assertEquals(expectedIds, actual);
    }
}
