/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.catalog.backend.datadir;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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

    /**
     * Tests for query retry behavior. These tests verify that queries returning null will retry
     * when the catalog has pending operations (not converged) and there's a web request context.
     */
    @Nested
    class QueryRetryBehavior {

        ExtendedCatalogFacade subject;

        @BeforeEach
        void setUpSubject() {
            subject = mock(ExtendedCatalogFacade.class);
            facade = new EventuallyConsistentCatalogFacade(subject, tracker, new int[] {10, 25, 50});
        }

        @AfterEach
        void tearDown() {
            // Clean up request context
            RequestContextHolder.resetRequestAttributes();
        }

        @Test
        void getWorkspaceByName_NoRetryWhenConverged() {
            // Given: catalog is converged
            when(tracker.isConverged()).thenReturn(true);
            when(subject.getWorkspaceByName("test")).thenReturn(null);

            // Mock web request context
            mockWebRequest();

            // When: query returns null
            WorkspaceInfo result = facade.getWorkspaceByName("test");

            // Then: no retry, returns null immediately
            assertNull(result);
            verify(subject, times(1)).getWorkspaceByName("test");
            verify(tracker, times(1)).isConverged();
            verify(tracker, times(0)).forceResolve(); // Never called
        }

        @Test
        void getWorkspaceByName_NoRetryWithoutWebRequest() {
            // Given: catalog is not converged but no web request
            when(tracker.isConverged()).thenReturn(false);
            when(subject.getWorkspaceByName("test")).thenReturn(null);

            // No web request context set

            // When: query returns null
            WorkspaceInfo result = facade.getWorkspaceByName("test");

            // Then: no retry (not a web request)
            assertNull(result);
            verify(subject, times(1)).getWorkspaceByName("test");
            verify(tracker, times(0)).isConverged(); // Never checked
            verify(tracker, times(0)).forceResolve();
        }

        @Test
        void getWorkspaceByName_RetriesWhenNotConverged() {
            // Given: catalog is not converged
            when(tracker.isConverged()).thenReturn(false);

            // First 3 calls return null, 4th call returns a workspace
            WorkspaceInfo workspace = mock(WorkspaceInfo.class);
            when(subject.getWorkspaceByName("test"))
                    .thenReturn(null) // Initial call
                    .thenReturn(null) // Retry 1
                    .thenReturn(null) // Retry 2
                    .thenReturn(workspace); // Retry 3 - success

            mockWebRequest();

            // When: query initially returns null
            WorkspaceInfo result = facade.getWorkspaceByName("test");

            // Then: retries and eventually finds it
            assertSame(workspace, result);
            verify(subject, times(4)).getWorkspaceByName("test"); // 1 initial + 3 retries
            verify(tracker, times(3)).forceResolve(); // Called on each retry
        }

        @Test
        void getWorkspaceByName_RetriesExhausted() {
            // Given: catalog is not converged
            when(tracker.isConverged()).thenReturn(false);
            when(subject.getWorkspaceByName("test")).thenReturn(null);

            mockWebRequest();

            // When: query returns null on all attempts
            long startTime = System.currentTimeMillis();
            WorkspaceInfo result = facade.getWorkspaceByName("test");
            long elapsed = System.currentTimeMillis() - startTime;

            // Then: exhausts all retries and returns null
            assertNull(result);
            verify(subject, times(4)).getWorkspaceByName("test"); // 1 initial + 3 retries
            verify(tracker, times(3)).forceResolve();

            // Verify timing: should wait 10 + 25 + 50 = 85ms (with some tolerance)
            assertTrue(elapsed >= 80, "Should wait at least 80ms, was: " + elapsed);
            assertTrue(elapsed < 200, "Should complete within 200ms, was: " + elapsed);
        }

        @Test
        void getStyleByName_RetriesWhenNotConverged() {
            // Given: catalog is not converged
            when(tracker.isConverged()).thenReturn(false);

            // First call returns null, second returns a style
            StyleInfo style = mock(StyleInfo.class);
            when(subject.getStyleByName("myStyle")).thenReturn(null).thenReturn(style);

            mockWebRequest();

            // When: query initially returns null
            StyleInfo result = facade.getStyleByName("myStyle");

            // Then: retries once and finds it
            assertSame(style, result);
            verify(subject, times(2)).getStyleByName("myStyle");
            verify(tracker, times(1)).forceResolve();
        }

        @Test
        void getLayerByName_RetriesWhenNotConverged() {
            // Given: catalog is not converged
            when(tracker.isConverged()).thenReturn(false);

            LayerInfo layer = mock(LayerInfo.class);
            when(subject.getLayerByName("myLayer")).thenReturn(null).thenReturn(layer);

            mockWebRequest();

            // When: query initially returns null
            LayerInfo result = facade.getLayerByName("myLayer");

            // Then: retries and finds it
            assertSame(layer, result);
            verify(subject, times(2)).getLayerByName("myLayer");
            verify(tracker, times(1)).forceResolve();
        }

        @Test
        void getResourceByName_RetriesWhenNotConverged() {
            // Given: catalog is not converged
            when(tracker.isConverged()).thenReturn(false);

            NamespaceInfo ns = mock(NamespaceInfo.class);
            ResourceInfo resource = mock(ResourceInfo.class);
            when(subject.getResourceByName(ns, "myResource", ResourceInfo.class))
                    .thenReturn(null)
                    .thenReturn(resource);

            mockWebRequest();

            // When: query initially returns null
            ResourceInfo result = facade.getResourceByName(ns, "myResource", ResourceInfo.class);

            // Then: retries and finds it
            assertSame(resource, result);
            verify(subject, times(2)).getResourceByName(ns, "myResource", ResourceInfo.class);
            verify(tracker, times(1)).forceResolve();
        }

        @Test
        void getNamespaceByPrefix_NoRetryForUriLikePrefix() {
            // Given: prefix looks like a URI (contains ':')
            // Note: calls getNamespace(id) not getNamespaceByPrefix(prefix) - special case
            when(subject.getNamespace("http://example.com")).thenReturn(null);

            mockWebRequest();

            // When: query with URI-like prefix
            NamespaceInfo result = facade.getNamespaceByPrefix("http://example.com");

            // Then: no retry (special case for URI-like prefixes, delegates to getNamespace)
            assertNull(result);
            verify(subject, times(1)).getNamespace("http://example.com");
            verify(subject, times(0)).getNamespaceByPrefix(anyString());
            verify(tracker, times(0)).isConverged(); // Never checked
        }

        @Test
        void getStore_RetriesWhenNotConverged() {
            // Given: catalog is not converged
            when(tracker.isConverged()).thenReturn(false);

            DataStoreInfo store = mock(DataStoreInfo.class);
            when(subject.getStore("store-id", DataStoreInfo.class))
                    .thenReturn(null)
                    .thenReturn(store);

            mockWebRequest();

            // When: query initially returns null
            DataStoreInfo result = facade.getStore("store-id", DataStoreInfo.class);

            // Then: retries and finds it
            assertSame(store, result);
            verify(subject, times(2)).getStore("store-id", DataStoreInfo.class);
            verify(tracker, times(1)).forceResolve();
        }

        @Test
        void getStoreByName_RetriesWhenNotConverged() {
            // Given: catalog is not converged
            when(tracker.isConverged()).thenReturn(false);

            WorkspaceInfo ws = mock(WorkspaceInfo.class);
            CoverageStoreInfo store = mock(CoverageStoreInfo.class);
            when(subject.getStoreByName(ws, "myStore", CoverageStoreInfo.class))
                    .thenReturn(null)
                    .thenReturn(store);

            mockWebRequest();

            // When: query initially returns null
            CoverageStoreInfo result = facade.getStoreByName(ws, "myStore", CoverageStoreInfo.class);

            // Then: retries and finds it
            assertSame(store, result);
            verify(subject, times(2)).getStoreByName(ws, "myStore", CoverageStoreInfo.class);
            verify(tracker, times(1)).forceResolve();
        }

        @Test
        void getResource_RetriesWhenNotConverged() {
            // Given: catalog is not converged
            when(tracker.isConverged()).thenReturn(false);

            FeatureTypeInfo resource = mock(FeatureTypeInfo.class);
            when(subject.getResource("resource-id", FeatureTypeInfo.class))
                    .thenReturn(null)
                    .thenReturn(resource);

            mockWebRequest();

            // When: query initially returns null
            FeatureTypeInfo result = facade.getResource("resource-id", FeatureTypeInfo.class);

            // Then: retries and finds it
            assertSame(resource, result);
            verify(subject, times(2)).getResource("resource-id", FeatureTypeInfo.class);
            verify(tracker, times(1)).forceResolve();
        }

        @Test
        void getResourceByStore_RetriesWhenNotConverged() {
            // Given: catalog is not converged
            when(tracker.isConverged()).thenReturn(false);

            WorkspaceInfo ws = mock(WorkspaceInfo.class);
            when(ws.getName()).thenReturn("testWorkspace");
            DataStoreInfo store = mock(DataStoreInfo.class);
            when(store.getWorkspace()).thenReturn(ws);
            FeatureTypeInfo resource = mock(FeatureTypeInfo.class);
            when(subject.getResourceByStore(store, "myResource", FeatureTypeInfo.class))
                    .thenReturn(null)
                    .thenReturn(resource);

            mockWebRequest();

            // When: query initially returns null
            FeatureTypeInfo result = facade.getResourceByStore(store, "myResource", FeatureTypeInfo.class);

            // Then: retries and finds it
            assertSame(resource, result);
            verify(subject, times(2)).getResourceByStore(store, "myResource", FeatureTypeInfo.class);
            verify(tracker, times(1)).forceResolve();
        }

        @Test
        void getLayer_RetriesWhenNotConverged() {
            // Given: catalog is not converged
            when(tracker.isConverged()).thenReturn(false);

            LayerInfo layer = mock(LayerInfo.class);
            when(subject.getLayer("layer-id")).thenReturn(null).thenReturn(layer);

            mockWebRequest();

            // When: query initially returns null
            LayerInfo result = facade.getLayer("layer-id");

            // Then: retries and finds it
            assertSame(layer, result);
            verify(subject, times(2)).getLayer("layer-id");
            verify(tracker, times(1)).forceResolve();
        }

        @Test
        void getLayerGroup_RetriesWhenNotConverged() {
            // Given: catalog is not converged
            when(tracker.isConverged()).thenReturn(false);

            LayerGroupInfo layerGroup = mock(LayerGroupInfo.class);
            when(subject.getLayerGroup("group-id")).thenReturn(null).thenReturn(layerGroup);

            mockWebRequest();

            // When: query initially returns null
            LayerGroupInfo result = facade.getLayerGroup("group-id");

            // Then: retries and finds it
            assertSame(layerGroup, result);
            verify(subject, times(2)).getLayerGroup("group-id");
            verify(tracker, times(1)).forceResolve();
        }

        @Test
        void getLayerGroupByName_RetriesWhenNotConverged() {
            // Given: catalog is not converged
            when(tracker.isConverged()).thenReturn(false);

            LayerGroupInfo layerGroup = mock(LayerGroupInfo.class);
            when(subject.getLayerGroupByName("myGroup")).thenReturn(null).thenReturn(layerGroup);

            mockWebRequest();

            // When: query initially returns null
            LayerGroupInfo result = facade.getLayerGroupByName("myGroup");

            // Then: retries and finds it
            assertSame(layerGroup, result);
            verify(subject, times(2)).getLayerGroupByName("myGroup");
            verify(tracker, times(1)).forceResolve();
        }

        @Test
        void getLayerGroupByNameWithWorkspace_RetriesWhenNotConverged() {
            // Given: catalog is not converged
            when(tracker.isConverged()).thenReturn(false);

            WorkspaceInfo ws = mock(WorkspaceInfo.class);
            LayerGroupInfo layerGroup = mock(LayerGroupInfo.class);
            when(subject.getLayerGroupByName(ws, "myGroup")).thenReturn(null).thenReturn(layerGroup);

            mockWebRequest();

            // When: query initially returns null
            LayerGroupInfo result = facade.getLayerGroupByName(ws, "myGroup");

            // Then: retries and finds it
            assertSame(layerGroup, result);
            verify(subject, times(2)).getLayerGroupByName(ws, "myGroup");
            verify(tracker, times(1)).forceResolve();
        }

        @Test
        void getNamespace_RetriesWhenNotConverged() {
            // Given: catalog is not converged
            when(tracker.isConverged()).thenReturn(false);

            NamespaceInfo namespace = mock(NamespaceInfo.class);
            when(subject.getNamespace("ns-id")).thenReturn(null).thenReturn(namespace);

            mockWebRequest();

            // When: query initially returns null
            NamespaceInfo result = facade.getNamespace("ns-id");

            // Then: retries and finds it
            assertSame(namespace, result);
            verify(subject, times(2)).getNamespace("ns-id");
            verify(tracker, times(1)).forceResolve();
        }

        @Test
        void getNamespaceByURI_RetriesWhenNotConverged() {
            // Given: catalog is not converged
            when(tracker.isConverged()).thenReturn(false);

            NamespaceInfo namespace = mock(NamespaceInfo.class);
            when(subject.getNamespaceByURI("http://example.com"))
                    .thenReturn(null)
                    .thenReturn(namespace);

            mockWebRequest();

            // When: query initially returns null
            NamespaceInfo result = facade.getNamespaceByURI("http://example.com");

            // Then: retries and finds it
            assertSame(namespace, result);
            verify(subject, times(2)).getNamespaceByURI("http://example.com");
            verify(tracker, times(1)).forceResolve();
        }

        @Test
        void getWorkspace_RetriesWhenNotConverged() {
            // Given: catalog is not converged
            when(tracker.isConverged()).thenReturn(false);

            WorkspaceInfo workspace = mock(WorkspaceInfo.class);
            when(subject.getWorkspace("ws-id")).thenReturn(null).thenReturn(workspace);

            mockWebRequest();

            // When: query initially returns null
            WorkspaceInfo result = facade.getWorkspace("ws-id");

            // Then: retries and finds it
            assertSame(workspace, result);
            verify(subject, times(2)).getWorkspace("ws-id");
            verify(tracker, times(1)).forceResolve();
        }

        @Test
        void getStyle_RetriesWhenNotConverged() {
            // Given: catalog is not converged
            when(tracker.isConverged()).thenReturn(false);

            StyleInfo style = mock(StyleInfo.class);
            when(subject.getStyle("style-id")).thenReturn(null).thenReturn(style);

            mockWebRequest();

            // When: query initially returns null
            StyleInfo result = facade.getStyle("style-id");

            // Then: retries and finds it
            assertSame(style, result);
            verify(subject, times(2)).getStyle("style-id");
            verify(tracker, times(1)).forceResolve();
        }

        @Test
        void getStyleByNameWithWorkspace_RetriesWhenNotConverged() {
            // Given: catalog is not converged
            when(tracker.isConverged()).thenReturn(false);

            WorkspaceInfo ws = mock(WorkspaceInfo.class);
            StyleInfo style = mock(StyleInfo.class);
            when(subject.getStyleByName(ws, "myStyle")).thenReturn(null).thenReturn(style);

            mockWebRequest();

            // When: query initially returns null
            StyleInfo result = facade.getStyleByName(ws, "myStyle");

            // Then: retries and finds it
            assertSame(style, result);
            verify(subject, times(2)).getStyleByName(ws, "myStyle");
            verify(tracker, times(1)).forceResolve();
        }

        private void mockWebRequest() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/geoserver/wms");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        }
    }
}
