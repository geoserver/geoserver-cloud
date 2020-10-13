/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.function.Consumer;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogAddEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogModifyEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteCatalogRemoveEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteDefaultDataStoreEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteDefaultNamespaceEvent;
import org.geoserver.cloud.bus.event.catalog.RemoteDefaultWorkspaceEvent;
import org.geoserver.cloud.event.PropertyDiffTestSupport;
import org.geoserver.cloud.test.ApplicationEventCapturingListener;
import org.geoserver.cloud.test.TestConfigurationAutoConfiguration;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = {TestConfigurationAutoConfiguration.class, ApplicationEventCapturingListener.class}
)
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
public class CatalogRemoteApplicationEventsTest extends AbstractRemoteApplicationEventsTest {

    public @Test void testCatalogSetDefaultWorkspace() {
        testDefaultWorkspace(false);
    }

    public @Test void testCatalogSetDefaultWorkspace_Payload() {
        testDefaultWorkspace(true);
    }

    private void testDefaultWorkspace(boolean payload) {
        catalog.add(testData.workspaceA);
        catalog.add(testData.workspaceC);

        Patch expected =
                new PropertyDiffTestSupport()
                        .createTestDiff(
                                "defaultWorkspace", testData.workspaceA, testData.workspaceC)
                        .toPatch();

        enablePayload(payload);
        RemoteDefaultWorkspaceEvent event =
                testCatalogModifiedEvent(
                        catalog,
                        c -> c.setDefaultWorkspace(testData.workspaceC),
                        expected,
                        RemoteDefaultWorkspaceEvent.class);

        assertEquals(Collections.singletonList("defaultWorkspace"), event.getChangedProperties());
        assertEquals(testData.workspaceC.getId(), event.getNewWorkspaceId());
    }

    public @Test void testCatalogSetDefaultNamespace() {
        testSetDefaultNamespace(false);
    }

    public @Test void testCatalogSetDefaultNamespace_Payload() {
        testSetDefaultNamespace(true);
    }

    private void testSetDefaultNamespace(boolean payload) {
        catalog.add(testData.namespaceA);
        catalog.add(testData.namespaceB);

        Patch expected =
                new PropertyDiffTestSupport()
                        .createTestDiff(
                                "defaultNamespace", testData.namespaceA, testData.namespaceB)
                        .toPatch();
        enablePayload();
        RemoteDefaultNamespaceEvent event =
                testCatalogModifiedEvent(
                        catalog,
                        c -> c.setDefaultNamespace(testData.namespaceB),
                        expected,
                        RemoteDefaultNamespaceEvent.class);
        assertEquals(Collections.singletonList("defaultNamespace"), event.getChangedProperties());
        assertEquals(testData.namespaceB.getId(), event.getNewNamespaceId());
    }

    public @Test void testCatalogSetDefaultDataStoreByWorkspace() {
        testSetDefaultDataStoreByWorkspace(false);
    }

    @Ignore("revisit, not all payloads work, possibly a misconfigured ObjectMapper for the bus")
    public @Test void testCatalogSetDefaultDataStoreByWorkspace_Payload() {
        testSetDefaultDataStoreByWorkspace(true);
    }

    private void testSetDefaultDataStoreByWorkspace(boolean payload) {
        WorkspaceInfo workspace = testData.workspaceA;
        DataStoreInfo dataStore = testData.dataStoreA;

        catalog.add(workspace);
        catalog.add(testData.namespaceA);

        enablePayload(payload);
        localRemoteEventsListener.restart();

        Patch expected;
        RemoteDefaultDataStoreEvent event;

        expected =
                new PropertyDiffTestSupport()
                        .createTestDiff("defaultDataStore", null, dataStore)
                        .toPatch();

        event =
                testCatalogModifiedEvent(
                        catalog,
                        c -> c.add(dataStore),
                        expected,
                        RemoteDefaultDataStoreEvent.class);

        assertEquals(Collections.singletonList("defaultDataStore"), event.getChangedProperties());
        assertEquals(workspace.getId(), event.getWorkspaceId());
        assertEquals(dataStore.getId(), event.getDefaultDataStoreId());

        localRemoteEventsListener.restart();

        expected =
                new PropertyDiffTestSupport()
                        .createTestDiff("defaultDataStore", dataStore, null)
                        .toPatch();

        event =
                testCatalogModifiedEvent(
                        catalog,
                        c -> c.remove(dataStore),
                        expected,
                        RemoteDefaultDataStoreEvent.class);

        assertEquals(Collections.singletonList("defaultDataStore"), event.getChangedProperties());
        assertEquals(workspace.getId(), event.getWorkspaceId());
        assertNull(event.getDefaultDataStoreId());
    }

    public @Test void testAdd_Workspace() {
        disablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.workspaceA, catalog::add, eventType);
    }

    public @Test void testAdd_Workspace_Payload() {
        enablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.workspaceA, catalog::add, eventType);
    }

    public @Test void testAdd_Namespace() {
        disablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.namespaceA, catalog::add, eventType);
    }

    public @Test void testAdd_Namespace_Payload() {
        enablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.namespaceA, catalog::add, eventType);
    }

    public @Test void testAdd_CoverageStore() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        disablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.coverageStoreA, catalog::add, eventType);
    }

    public @Test void testAdd_CoverageStore_Payload() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        enablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.coverageStoreA, catalog::add, eventType);
    }

    public @Test void testAdd_DataStore() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        disablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.dataStoreA, catalog::add, eventType);
    }

    public @Test void testAdd_DataStore_Payload() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        enablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.dataStoreA, catalog::add, eventType);
    }

    public @Test void testAdd_Coverage() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.coverageStoreA);
        disablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.coverageA, catalog::add, eventType);
    }

    public @Test void testAdd_Coverage_Payload() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.namespaceB);
        catalog.add(testData.coverageStoreA);
        enablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.coverageA, catalog::add, eventType);
    }

    public @Test void testAdd_FeatureType() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.dataStoreA);
        disablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.featureTypeA, catalog::add, eventType);
    }

    public @Test void testAdd_FeatureType_Payload() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.dataStoreA);
        enablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.featureTypeA, catalog::add, eventType);
    }

    public @Test void testAdd_Layer() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.dataStoreA);
        catalog.add(testData.featureTypeA);
        catalog.add(testData.style1);
        disablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.layerFeatureTypeA, catalog::add, eventType);
    }

    public @Test void testAdd_Layer_Payload() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.dataStoreA);
        catalog.add(testData.featureTypeA);
        catalog.add(testData.style1);
        enablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.layerFeatureTypeA, catalog::add, eventType);
    }

    public @Test void testAdd_LayerGroup() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.dataStoreA);
        catalog.add(testData.featureTypeA);
        catalog.add(testData.layerFeatureTypeA);
        catalog.add(testData.style1);
        disablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.layerGroup1, catalog::add, eventType);
    }

    public @Test void testAdd_LayerGroup_Payload() {
        catalog.add(testData.workspaceA);
        catalog.add(testData.namespaceA);
        catalog.add(testData.dataStoreA);
        catalog.add(testData.featureTypeA);
        catalog.add(testData.layerFeatureTypeA);
        catalog.add(testData.style1);
        enablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.layerGroup1, catalog::add, eventType);
    }

    public @Test void testAdd_Style() {
        disablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.style1, catalog::add, eventType);
    }

    public @Test void testAdd_Style_Payload() {
        enablePayload();
        Class<RemoteCatalogAddEvent> eventType = RemoteCatalogAddEvent.class;
        testRemoteAddEvent(testData.style1, catalog::add, eventType);
    }

    protected void setupNoPayload() {
        setupClean(false);
    }

    protected void setupPayload() {
        setupClean(true);
    }

    protected void setupClean(boolean payload) {
        localRemoteEventsListener.stop();
        testData.deleteAll();
        testData.addObjects();
        enablePayload(payload);
        localRemoteEventsListener.start();
    }

    public @Test void testModifyEventsWorkspace() {
        setupNoPayload();
        modifyEventsWorkspace();
    }

    public @Test void testModifyEventsWorkspace_Payload() {
        setupPayload();
        modifyEventsWorkspace();
    }

    protected void modifyEventsWorkspace() {
        testRemoteModifyEvent(
                testData.workspaceA,
                ws -> {
                    ws.setName("newName");
                },
                catalog::save,
                RemoteCatalogModifyEvent.class);
    }

    @Ignore("implement")
    public @Test void testModifyEventsNamespace() {
        fail("NOT IMPLEMENTED");
    }

    @Ignore("implement")
    public @Test void testModifyEventsDataStore() {
        fail("NOT IMPLEMENTED");
    }

    @Ignore("implement")
    public @Test void testModifyEventsCoverageStore() {
        fail("NOT IMPLEMENTED");
    }

    @Ignore("implement")
    public @Test void testModifyEventsWMSStore() {
        fail("NOT IMPLEMENTED");
    }

    @Ignore("implement")
    public @Test void testModifyEventsWMTSStore() {
        fail("NOT IMPLEMENTED");
    }

    @Ignore("implement")
    public @Test void testModifyEventsFeatureType() {
        fail("NOT IMPLEMENTED");
    }

    @Ignore("implement")
    public @Test void testModifyEventsCoverage() {
        fail("NOT IMPLEMENTED");
    }

    @Ignore("implement")
    public @Test void testModifyEventsWMSLayer() {
        fail("NOT IMPLEMENTED");
    }

    @Ignore("implement")
    public @Test void testModifyEventsWMTSLayer() {
        fail("NOT IMPLEMENTED");
    }

    @Ignore("implement")
    public @Test void testModifyEventsLayer() {
        fail("NOT IMPLEMENTED");
    }

    @Ignore("implement")
    public @Test void testModifyEventsLayerGroup() {
        fail("NOT IMPLEMENTED");
    }

    @Ignore("implement")
    public @Test void testModifyEventsStyle() {
        fail("NOT IMPLEMENTED");
    }

    @Ignore("implement")
    public @Test void testRemoveEvents() {
        testRemoveEvents(false);
    }

    @Ignore("revisit, not all payloads work, possibly a misconfigured ObjectMapper for the bus")
    public @Test void testRemoveEvents_Payload() {
        testRemoveEvents(true);
    }

    private void testRemoveEvents(boolean payload) {
        localRemoteEventsListener.stop();
        testData.addObjects();
        localRemoteEventsListener.start();
        enablePayload(payload);
        Class<RemoteCatalogRemoveEvent> eventType = RemoteCatalogRemoveEvent.class;

        testRemoteRemoveEvent(testData.layerGroup1, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.layerFeatureTypeA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.featureTypeA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.coverageA, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.style1, catalog::remove, eventType);
        testRemoteRemoveEvent(testData.dataStoreA, catalog::remove, eventType);
        // testRemoteRemoveEvent(testData.coverageStoreA, catalog::remove, eventType);
        // testRemoteRemoveEvent(testData.wmsLayerA, catalog::remove, eventType);
        // testRemoteRemoveEvent(testData.wmsStoreA, catalog::remove, eventType);
        // testRemoteRemoveEvent(testData.wmtsLayerA, catalog::remove, eventType);
        // testRemoteRemoveEvent(testData.wmtsStoreA, catalog::remove, eventType);
        // testRemoteRemoveEvent(testData.namespaceA, catalog::remove, eventType);
        // testRemoteRemoveEvent(testData.workspaceA, catalog::remove, eventType);
    }

    private <E extends RemoteCatalogModifyEvent> E testCatalogModifiedEvent(
            Catalog catalog, Consumer<Catalog> modifier, Patch expected, Class<E> eventType) {

        this.localRemoteEventsListener.clear();
        this.localRemoteEventsListener.start();
        this.outBoundEvents.clear();

        modifier.accept(catalog);

        RemoteModifyEvent<Catalog, CatalogInfo> localRemoteEvent;
        E sentEvent;

        localRemoteEvent = localRemoteEventsListener.expectOne(eventType);
        sentEvent = outBoundEvents.expectOne(eventType);

        assertCatalogEvent(catalog, localRemoteEvent, expected);
        assertCatalogEvent(catalog, sentEvent, expected);
        return sentEvent;
    }

    private void assertCatalogEvent(
            Catalog catalog, RemoteModifyEvent<Catalog, CatalogInfo> event, Patch expected) {
        assertNotNull(event.getId());
        assertEquals("**", event.getDestinationService());
        assertEquals("catalog", event.getObjectId());
        assertNotNull(event.getInfoType());
        assertEquals(Catalog.class, event.getInfoType().getType());

        if (this.geoserverBusProperties.isSendDiff()) {
            assertTrue(event.patch().isPresent());
            assertEquals(expected, event.patch().get());
        }
    }
}
