/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.HTTPStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.geotools.api.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import lombok.Getter;
import lombok.experimental.Accessors;

@EnableAutoConfiguration
@Accessors(fluent = true)
public class StoreRepositoryTest
        extends AbstractCatalogServiceClientRepositoryTest<StoreInfo, StoreRepository> {

    private @Autowired @Getter StoreRepository repository;

    public StoreRepositoryTest() {
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
        if (expected instanceof CoverageStoreInfo cov)
            assertEquals(
                    cov.getURL(), ((CoverageStoreInfo) actual).getURL());
        if (expected instanceof HTTPStoreInfo httpStore)
            assertEquals(
            		httpStore.getCapabilitiesURL(),
                    ((HTTPStoreInfo) actual).getCapabilitiesURL());
    }

    @Override public  @Test void testFindAll() {
        super.testFindAll(
                testData.dataStoreA,
                testData.dataStoreB,
                testData.dataStoreC,
                testData.coverageStoreA,
                testData.wmsStoreA,
                testData.wmtsStoreA);
    }

    @Override public  @Test void testFindById() {
        super.testFindById(testData.dataStoreA);
        super.testFindById(testData.coverageStoreA);
        super.testFindById(testData.wmsStoreA);
        super.testFindById(testData.wmtsStoreA);
    }

    @Override public  @Test void testFindAllByType() {
        super.testFindAllIncludeFilter(
                StoreInfo.class,
                testData.dataStoreA,
                testData.dataStoreB,
                testData.dataStoreC,
                testData.coverageStoreA,
                testData.wmsStoreA,
                testData.wmtsStoreA);

        super.testFindAllIncludeFilter(
                DataStoreInfo.class, testData.dataStoreA, testData.dataStoreB, testData.dataStoreC);
        super.testFindAllIncludeFilter(CoverageStoreInfo.class, testData.coverageStoreA);
        super.testFindAllIncludeFilter(WMSStoreInfo.class, testData.wmsStoreA);
        super.testFindAllIncludeFilter(WMTSStoreInfo.class, testData.wmtsStoreA);
    }

    @Test void testFindAllByTypeStoreRepository() {
        testFind(
                () -> repository.findAllByType(StoreInfo.class),
                testData.dataStoreA,
                testData.dataStoreB,
                testData.dataStoreC,
                testData.coverageStoreA,
                testData.wmsStoreA,
                testData.wmtsStoreA);

        testFind(
                () -> repository.findAllByType(DataStoreInfo.class),
                testData.dataStoreA,
                testData.dataStoreB,
                testData.dataStoreC);

        testFind(() -> repository.findAllByType(CoverageStoreInfo.class), testData.coverageStoreA);
        testFind(() -> repository.findAllByType(WMSStoreInfo.class), testData.wmsStoreA);
        testFind(() -> repository.findAllByType(WMTSStoreInfo.class), testData.wmtsStoreA);
    }

    @Test void testSetDefaultDataStore() {
        WorkspaceInfo ws = testData.workspaceA;
        DataStoreInfo ds1 = testData.dataStoreA;
        DataStoreInfo ds2 = testData.faker().dataStoreInfo("wsA-ds2", ws);
        serverCatalog.add(ds2);
        assertEquals(ds1.getId(), repository().getDefaultDataStore(ws).get().getId());

        repository().setDefaultDataStore(ws, ds2);
        assertEquals(ds2.getId(), repository().getDefaultDataStore(ws).get().getId());
        assertEquals(ds2.getId(), serverCatalog.getDefaultDataStore(ws).getId());
    }

    @Test void testUnsetDefaultDataStore() {
        WorkspaceInfo ws = testData.workspaceA;
        DataStoreInfo store = testData.dataStoreA;
        // preflight check
        serverCatalog.setDefaultDataStore(ws, null);
        assertNull(serverCatalog.getDefaultDataStore(ws));
        serverCatalog.setDefaultDataStore(ws, store);
        assertEquals(store, serverCatalog.getDefaultDataStore(ws));

        DataStoreInfo current = serverCatalog.getDefaultDataStore(ws);
        assertNotNull(current);
        assertEquals(store.getId(), current.getId());

        repository.unsetDefaultDataStore(ws);

        assertNull(serverCatalog.getDefaultDataStore(ws));
        assertTrue(repository.getDefaultDataStore(ws).isEmpty());

        // check idempotency
        repository.unsetDefaultDataStore(ws);

        assertNull(serverCatalog.getDefaultDataStore(ws));
        assertTrue(repository.getDefaultDataStore(ws).isEmpty());
    }

    @Test void getDefaultDataStore() {
        WorkspaceInfo wsA = testData.workspaceA;
        WorkspaceInfo wsB = testData.workspaceB;

        DataStoreInfo dsA2 = testData.faker().dataStoreInfo("wsA-ds2", wsA);
        DataStoreInfo dsB2 = testData.faker().dataStoreInfo("wsB-ds2", wsB);
        serverCatalog.add(dsA2);
        serverCatalog.add(dsB2);

        StoreRepository repository = repository();

        assertEquals(
                testData.dataStoreA.getId(), repository.getDefaultDataStore(wsA).get().getId());
        assertEquals(
                testData.dataStoreB.getId(), repository.getDefaultDataStore(wsB).get().getId());

        serverCatalog.setDefaultDataStore(wsA, dsA2);
        serverCatalog.setDefaultDataStore(wsB, dsB2);

        assertEquals(dsA2.getId(), repository.getDefaultDataStore(wsA).get().getId());
        assertEquals(dsB2.getId(), repository.getDefaultDataStore(wsB).get().getId());

        CascadeDeleteVisitor cascadeDeleteVisitor = new CascadeDeleteVisitor(serverCatalog);
        serverCatalog.getDataStore(testData.dataStoreA.getId()).accept(cascadeDeleteVisitor);
        serverCatalog.getDataStore(dsA2.getId()).accept(cascadeDeleteVisitor);
        assertNull(serverCatalog.getDefaultDataStore(wsA));
        assertTrue(repository.getDefaultDataStore(wsA).isEmpty());
    }

    @Test void testGetDefaultDataStores() {
        WorkspaceInfo wsA = testData.workspaceA;
        WorkspaceInfo wsB = testData.workspaceB;
        DataStoreInfo dsA2 = testData.faker().dataStoreInfo("wsA-ds2", wsA);
        DataStoreInfo dsB2 = testData.faker().dataStoreInfo("wsB-ds2", wsB);
        serverCatalog.add(dsA2);
        serverCatalog.add(dsB2);

        StoreRepository repository = repository();
        testFind(
                () -> repository.getDefaultDataStores(),
                testData.dataStoreA,
                testData.dataStoreB,
                testData.dataStoreC);

        serverCatalog.setDefaultDataStore(wsA, dsA2);
        serverCatalog.setDefaultDataStore(wsB, dsB2);
        testFind(() -> repository.getDefaultDataStores(), dsA2, dsB2, testData.dataStoreC);
    }

    @Override public  @Test void testQueryFilter() {
        DataStoreInfo ds1 = serverCatalog.getDataStore(testData.dataStoreA.getId());
        DataStoreInfo ds2 = serverCatalog.getDataStore(testData.dataStoreB.getId());
        DataStoreInfo ds3 = serverCatalog.getDataStore(testData.dataStoreC.getId());
        CoverageStoreInfo cs1 = serverCatalog.getCoverageStore(testData.coverageStoreA.getId());
        WMSStoreInfo wmss1 = serverCatalog.getStore(testData.wmsStoreA.getId(), WMSStoreInfo.class);
        WMTSStoreInfo wmtss1 =
                serverCatalog.getStore(testData.wmtsStoreA.getId(), WMTSStoreInfo.class);

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

    @Test void testDataStoreInfo_CRUD() throws IOException {
        DataStoreInfo store =
                testData.faker().dataStoreInfo(
                        "dataStoreCRUD-id",
                        testData.workspaceB,
                        "dataStoreCRUD",
                        "dataStoreCRUD description",
                        true);
        crudTest(
                store,
                serverCatalog::getDataStore,
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

    @Test void testCoverageStoreInfo_CRUD() {
        CoverageStoreInfo store =
                testData.createCoverageStore(
                        "coverageStoreCRUD",
                        testData.workspaceC,
                        "coverageStoreCRUD name",
                        "GeoTIFF",
                        "file:/test/coverageStoreCRUD.tiff");
        crudTest(
                store,
                serverCatalog::getCoverageStore,
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

    @Test void testWMSStoreInfo_CRUD() {
        WMSStoreInfo store =
                testData.createWebMapServer(
                        "wmsStoreCRUD",
                        testData.workspaceA,
                        "wmsStoreCRUD_name",
                        "http://test.com",
                        true);
        crudTest(
                store,
                id -> serverCatalog.getStore(id, WMSStoreInfo.class),
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

    @Test void testWMTSStoreInfo_CRUD() {
        WMTSStoreInfo store =
                testData.createWebMapTileServer(
                        "wmsStoreCRUD",
                        testData.workspaceA,
                        "wmtsStoreCRUD_name",
                        "http://test.com",
                        true);
        crudTest(
                store,
                id -> serverCatalog.getStore(id, WMTSStoreInfo.class),
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

    @Test void testFindStoreById() throws IOException {
        testFindById(testData.coverageStoreA);
        testFindById(testData.dataStoreA);
        testFindById(testData.dataStoreB);
        testFindById(testData.wmsStoreA);
        testFindById(testData.wmtsStoreA);
    }

    @Test void testFindStoreById_SubtypeMismatch() throws IOException {
        StoreRepository client = repository();
        assertTrue(client.findById(testData.coverageStoreA.getId(), DataStoreInfo.class).isEmpty());
        assertTrue(client.findById(testData.dataStoreA.getId(), CoverageStoreInfo.class).isEmpty());
        assertTrue(client.findById(testData.dataStoreB.getId(), CoverageStoreInfo.class).isEmpty());
    }

    @Test void testFindStoreByName() throws IOException {
        findStoreByName(testData.coverageStoreA);
        findStoreByName(testData.dataStoreA);
        findStoreByName(testData.dataStoreB);
        findStoreByName(testData.wmsStoreA);
        findStoreByName(testData.wmtsStoreA);
    }

    private void findStoreByName(StoreInfo store) {
        StoreInfo responseBody = repository.findFirstByName(store.getName(), StoreInfo.class).get();
        StoreInfo resolved = resolveProxies(responseBody);
        assertCatalogInfoEquals(store, resolved);
    }

    @Test void testFindStoreByWorkspaceAndName() throws IOException {
        testFindStoreByWorkspaceAndName(testData.coverageStoreA, StoreInfo.class);
        testFindStoreByWorkspaceAndName(testData.coverageStoreA, CoverageStoreInfo.class);

        testFindStoreByWorkspaceAndName(testData.dataStoreA, StoreInfo.class);
        testFindStoreByWorkspaceAndName(testData.dataStoreA, DataStoreInfo.class);

        testFindStoreByWorkspaceAndName(testData.dataStoreB, StoreInfo.class);
        testFindStoreByWorkspaceAndName(testData.dataStoreB, DataStoreInfo.class);

        testFindStoreByWorkspaceAndName(testData.wmsStoreA, StoreInfo.class);
        testFindStoreByWorkspaceAndName(testData.wmsStoreA, WMSStoreInfo.class);

        testFindStoreByWorkspaceAndName(testData.wmtsStoreA, StoreInfo.class);
        testFindStoreByWorkspaceAndName(testData.wmtsStoreA, WMTSStoreInfo.class);
    }

    private void testFindStoreByWorkspaceAndName(StoreInfo store, Class<? extends StoreInfo> type) {
        WorkspaceInfo workspace = store.getWorkspace();
        String name = store.getName();

        StoreInfo found = repository().findByNameAndWorkspace(name, workspace, type).get();
        assertNotNull(found);
        assertEquals(store.getId(), found.getId());
        assertEquals(store.getName(), found.getName());
    }

    @Test void testFindStoreByName_WrongWorkspace() throws IOException {
        testFindStoreByName_WrongWorkspace(testData.coverageStoreA, testData.workspaceC);
        testFindStoreByName_WrongWorkspace(testData.dataStoreA, testData.workspaceC);
        testFindStoreByName_WrongWorkspace(testData.dataStoreB, testData.workspaceC);
        testFindStoreByName_WrongWorkspace(testData.wmsStoreA, testData.workspaceC);
        testFindStoreByName_WrongWorkspace(testData.wmtsStoreA, testData.workspaceC);
    }

    private void testFindStoreByName_WrongWorkspace(StoreInfo store, WorkspaceInfo workspace) {
        String name = store.getName();
        assertTrue(repository().findByNameAndWorkspace(name, workspace, StoreInfo.class).isEmpty());
    }

    @Test void testFindStoresByWorkspace() {
        testFindStoresByWorkspace(
                testData.workspaceA,
                testData.dataStoreA,
                testData.coverageStoreA,
                testData.wmsStoreA,
                testData.wmtsStoreA);
        testFindStoresByWorkspace(testData.workspaceB, testData.dataStoreB);
        WorkspaceInfo emptyWs = testData.faker().workspaceInfo("emptyws");
        NamespaceInfo emptyNs = testData.faker().namespace();
        serverCatalog.add(emptyWs);
        serverCatalog.add(emptyNs);
        testFindStoresByWorkspace(emptyWs);
    }

    public void testFindStoresByWorkspace(WorkspaceInfo ws, StoreInfo... expected) {
        Stream<StoreInfo> stores = repository().findAllByWorkspace(ws, StoreInfo.class);
        Set<String> expectedIds =
                Arrays.stream(expected).map(StoreInfo::getId).collect(Collectors.toSet());
        Set<String> actual = stores.map(StoreInfo::getId).collect(Collectors.toSet());

        assertEquals(expectedIds, actual);
    }
}
