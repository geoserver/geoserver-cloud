/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.backend.datadir;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.Patch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

class EventuallyConsistentCatalogFacadeTest {

    EventualConsistencyEnforcer tracker;
    EventuallyConsistentCatalogFacade facade;

    @BeforeEach
    void setUp() {
        ExtendedCatalogFacade subject = mock(ExtendedCatalogFacade.class);
        tracker = mock(EventualConsistencyEnforcer.class);
        facade = new EventuallyConsistentCatalogFacade(subject, tracker, new int[] {10, 25, 50});
    }

    @Test
    void testEventuallyConsistentCatalogFacade() {
        assertThrows(NullPointerException.class, () -> new EventualConsistencyEnforcer(null));
    }

    @Test
    void testAddStoreInfo() {
        testAdd(mock(StoreInfo.class), facade::add);
    }

    @Test
    void testRemoveStoreInfo() {
        testRemove(mock(StoreInfo.class), facade::remove);
    }

    @Test
    void testSetDefaultDataStore() {
        WorkspaceInfo ws = mock(WorkspaceInfo.class);

        facade.setDefaultDataStore(ws, null);
        verify(tracker, times(1)).setDefaultDataStore(ws, null);
        verifyNoMoreInteractions(tracker);
        clearInvocations(tracker);

        DataStoreInfo store = mock(DataStoreInfo.class);
        facade.setDefaultDataStore(ws, store);
        verify(tracker, times(1)).setDefaultDataStore(ws, store);
        verifyNoMoreInteractions(tracker);
        clearInvocations(tracker);
    }

    @Test
    void testAddResourceInfo() {
        testAdd(mock(ResourceInfo.class), facade::add);
    }

    @Test
    void testRemoveResourceInfo() {
        testRemove(mock(ResourceInfo.class), facade::remove);
    }

    @Test
    void testAddLayerInfo() {
        testAdd(mock(LayerInfo.class), facade::add);
    }

    @Test
    void testRemoveLayerInfo() {
        testRemove(mock(LayerInfo.class), facade::remove);
    }

    @Test
    void testAddLayerGroupInfo() {
        testAdd(mock(LayerGroupInfo.class), facade::add);
    }

    @Test
    void testRemoveLayerGroupInfo() {
        testRemove(mock(LayerGroupInfo.class), facade::remove);
    }

    @Test
    void testAddNamespaceInfo() {
        testAdd(mock(NamespaceInfo.class), facade::add);
    }

    @Test
    void testRemoveNamespaceInfo() {
        testRemove(mock(NamespaceInfo.class), facade::remove);
    }

    @Test
    void testSetDefaultNamespace() {
        NamespaceInfo ns = mock(NamespaceInfo.class);
        facade.setDefaultNamespace(ns);
        verify(tracker, times(1)).setDefaultNamespace(ns);
        verifyNoMoreInteractions(tracker);
        clearInvocations(tracker);
        facade.setDefaultNamespace(null);
        verify(tracker, times(1)).setDefaultNamespace(null);
        verifyNoMoreInteractions(tracker);
    }

    @Test
    void testAddWorkspaceInfo() {
        testAdd(mock(WorkspaceInfo.class), facade::add);
    }

    @Test
    void testRemoveWorkspaceInfo() {
        testRemove(mock(WorkspaceInfo.class), facade::remove);
    }

    @Test
    void testSetDefaultWorkspace() {
        WorkspaceInfo ws = mock(WorkspaceInfo.class);
        facade.setDefaultWorkspace(ws);
        verify(tracker, times(1)).setDefaultWorkspace(ws);
        verifyNoMoreInteractions(tracker);
        clearInvocations(tracker);
        facade.setDefaultWorkspace(null);
        verify(tracker, times(1)).setDefaultWorkspace(null);
        verifyNoMoreInteractions(tracker);
    }

    @Test
    void testAddStyleInfo() {
        testAdd(mock(StyleInfo.class), facade::add);
    }

    @Test
    void testRemoveStyleInfo() {
        testRemove(mock(StyleInfo.class), facade::remove);
    }

    @Test
    void testUpdate() {
        CatalogInfo info = mock(LayerInfo.class);
        Patch patch = mock(Patch.class);

        assertThrows(NullPointerException.class, () -> facade.update(null, patch));
        assertThrows(NullPointerException.class, () -> facade.update(info, null));

        facade.update(info, patch);
        verify(tracker, times(1)).update(info, patch);
    }

    @Test
    void testAddT() {
        testAddCatalogInfo(mock(WorkspaceInfo.class));
        testAddCatalogInfo(mock(NamespaceInfo.class));
        testAddCatalogInfo(mock(DataStoreInfo.class));
        testAddCatalogInfo(mock(CoverageStoreInfo.class));
        testAddCatalogInfo(mock(WMSStoreInfo.class));
        testAddCatalogInfo(mock(WMTSStoreInfo.class));
        testAddCatalogInfo(mock(FeatureTypeInfo.class));
        testAddCatalogInfo(mock(CoverageInfo.class));
        testAddCatalogInfo(mock(WMSLayerInfo.class));
        testAddCatalogInfo(mock(WMTSLayerInfo.class));
        testAddCatalogInfo(mock(LayerInfo.class));
        testAddCatalogInfo(mock(LayerGroupInfo.class));
        testAddCatalogInfo(mock(StyleInfo.class));
        testAddCatalogInfo(mock(MapInfo.class));
    }

    private void testAddCatalogInfo(CatalogInfo mock) {
        testAdd(mock, facade::add);
    }

    @Test
    void testRemoveCatalogInfo() {
        testRemoveCatalogInfo(mock(WorkspaceInfo.class));
        testRemoveCatalogInfo(mock(NamespaceInfo.class));
        testRemoveCatalogInfo(mock(DataStoreInfo.class));
        testRemoveCatalogInfo(mock(CoverageStoreInfo.class));
        testRemoveCatalogInfo(mock(WMSStoreInfo.class));
        testRemoveCatalogInfo(mock(WMTSStoreInfo.class));
        testRemoveCatalogInfo(mock(FeatureTypeInfo.class));
        testRemoveCatalogInfo(mock(CoverageInfo.class));
        testRemoveCatalogInfo(mock(WMSLayerInfo.class));
        testRemoveCatalogInfo(mock(WMTSLayerInfo.class));
        testRemoveCatalogInfo(mock(LayerInfo.class));
        testRemoveCatalogInfo(mock(LayerGroupInfo.class));
        testRemoveCatalogInfo(mock(StyleInfo.class));
        testRemoveCatalogInfo(mock(MapInfo.class));
    }

    private void testRemoveCatalogInfo(CatalogInfo mock) {
        testRemove(mock, facade::remove);
    }

    private <T extends CatalogInfo> void testAdd(T info, Consumer<T> addop) {
        assertThrows(NullPointerException.class, () -> addop.accept(null));

        addop.accept(info);
        verify(tracker, times(1)).add(info);
        verifyNoMoreInteractions(tracker);
    }

    private <T extends CatalogInfo> void testRemove(T info, Consumer<T> removeop) {

        assertThrows(NullPointerException.class, () -> removeop.accept(null));

        removeop.accept(info);
        verify(tracker, times(1)).remove(info);
        verifyNoMoreInteractions(tracker);
        clearInvocations(tracker);
    }
}
