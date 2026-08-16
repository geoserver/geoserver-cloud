/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.backend.pgconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.GWCSynchEnv;
import org.geotools.api.filter.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PgconfigGwcCatalogRenameListenerTest {

    private Catalog catalog;
    private PgconfigGwcCatalogRenameListener listener;
    private GWC mediator;
    private List<TileLayerEvent> events;

    @BeforeEach
    void setUp() {
        catalog = mock(Catalog.class);
        mediator = mock(GWC.class);
        events = new ArrayList<>();
        listener = new PgconfigGwcCatalogRenameListener(catalog, events::add);
        GWC.set(mediator, mock(GWCSynchEnv.class));
    }

    @AfterEach
    void tearDown() {
        GWC.set(null, null);
    }

    @Test
    void register_addsListenerToCatalog() {
        listener.register();
        verify(catalog).addListener(listener);
    }

    @Test
    void unregister_removesListenerFromCatalog() {
        listener.unregister();
        verify(catalog).removeListener(listener);
    }

    @Test
    void workspaceRename_emitsOneLayerRenamedPerLayerAndLayerGroup() {
        WorkspaceInfo ws = mock(WorkspaceInfo.class);
        when(ws.getName()).thenReturn("oldWs");

        whenListByType(List.of(layerInfoMock("foo"), layerInfoMock("bar")), List.of(layerGroupInfoMock("grp")));

        listener.handleModifyEvent(modifyEvent(ws, List.of("name"), List.of("oldWs"), List.of("newWs")));
        listener.handlePostModifyEvent(postModifyEvent(ws));

        verify(mediator).layerRenamed("oldWs:foo", "newWs:foo");
        verify(mediator).layerRenamed("oldWs:bar", "newWs:bar");
        verify(mediator).layerRenamed("oldWs:grp", "newWs:grp");
        assertThat(events)
                .extracting(TileLayerEvent::getPublishedId, TileLayerEvent::getName, TileLayerEvent::getOldName)
                .containsExactlyInAnyOrder(
                        tuple("foo-id", "newWs:foo", "oldWs:foo"),
                        tuple("bar-id", "newWs:bar", "oldWs:bar"),
                        tuple("grp-id", "newWs:grp", "oldWs:grp"));
    }

    @Test
    void resourceRename_emitsSingleLayerRenamedWithNamespacePrefix() {
        NamespaceInfo ns = mock(NamespaceInfo.class);
        when(ns.getPrefix()).thenReturn("topp");
        FeatureTypeInfo resource = mock(FeatureTypeInfo.class);
        when(resource.getNamespace()).thenReturn(ns);
        when(resource.getName()).thenReturn("states");

        LayerInfo statesLayer = layerInfoMock("states");
        when(catalog.getLayers(resource)).thenReturn(List.of(statesLayer));

        listener.handleModifyEvent(modifyEvent(resource, List.of("name"), List.of("states"), List.of("roads")));
        listener.handlePostModifyEvent(postModifyEvent(resource));

        verify(mediator).layerRenamed("topp:states", "topp:roads");
        assertThat(events)
                .extracting(TileLayerEvent::getPublishedId, TileLayerEvent::getName, TileLayerEvent::getOldName)
                .containsExactly(tuple("states-id", "topp:roads", "topp:states"));
    }

    @Test
    void resourceNamespaceChange_usesOldNamespaceForOldName() {
        NamespaceInfo oldNs = mock(NamespaceInfo.class);
        when(oldNs.getPrefix()).thenReturn("ns1");
        NamespaceInfo newNs = mock(NamespaceInfo.class);
        when(newNs.getPrefix()).thenReturn("ns2");
        FeatureTypeInfo resource = mock(FeatureTypeInfo.class);
        // at pre-modify time the source still exposes the old namespace; the new one
        // is only available through the event's new values
        when(resource.getNamespace()).thenReturn(oldNs);
        when(resource.getName()).thenReturn("ft");

        LayerInfo ftLayer = layerInfoMock("ft");
        when(catalog.getLayers(resource)).thenReturn(List.of(ftLayer));

        listener.handleModifyEvent(modifyEvent(resource, List.of("namespace"), List.of(oldNs), List.of(newNs)));
        listener.handlePostModifyEvent(postModifyEvent(resource));

        verify(mediator).layerRenamed("ns1:ft", "ns2:ft");
        assertThat(events)
                .extracting(TileLayerEvent::getPublishedId, TileLayerEvent::getName, TileLayerEvent::getOldName)
                .containsExactly(tuple("ft-id", "ns2:ft", "ns1:ft"));
    }

    @Test
    void layerGroupRename_workspaceScoped() {
        WorkspaceInfo ws = mock(WorkspaceInfo.class);
        when(ws.getName()).thenReturn("topp");
        LayerGroupInfo group = mock(LayerGroupInfo.class);
        when(group.getWorkspace()).thenReturn(ws);

        listener.handleModifyEvent(modifyEvent(group, List.of("name"), List.of("groupOld"), List.of("groupNew")));
        listener.handlePostModifyEvent(postModifyEvent(group));

        verify(mediator).layerRenamed("topp:groupOld", "topp:groupNew");
    }

    @Test
    void layerGroupRename_global() {
        LayerGroupInfo group = mock(LayerGroupInfo.class);
        when(group.getWorkspace()).thenReturn(null);

        listener.handleModifyEvent(modifyEvent(group, List.of("name"), List.of("globalOld"), List.of("globalNew")));
        listener.handlePostModifyEvent(postModifyEvent(group));

        verify(mediator).layerRenamed("globalOld", "globalNew");
    }

    @Test
    void storeMoveToAnotherWorkspace_renamesAllStoreLayers() {
        WorkspaceInfo oldWs = mock(WorkspaceInfo.class);
        when(oldWs.getName()).thenReturn("sourceWs");
        WorkspaceInfo newWs = mock(WorkspaceInfo.class);
        when(newWs.getName()).thenReturn("targetWs");
        DataStoreInfo store = mock(DataStoreInfo.class);

        NamespaceInfo oldNs = mock(NamespaceInfo.class);
        when(oldNs.getPrefix()).thenReturn("sourceWs");
        FeatureTypeInfo resource = mock(FeatureTypeInfo.class);
        when(resource.getName()).thenReturn("roads");
        when(resource.getNamespace()).thenReturn(oldNs);
        when(catalog.getResourcesByStore(store, ResourceInfo.class)).thenReturn(List.of(resource));
        LayerInfo roadsLayer = layerInfoMock("roads");
        when(catalog.getLayers(resource)).thenReturn(List.of(roadsLayer));

        listener.handleModifyEvent(modifyEvent(store, List.of("workspace"), List.of(oldWs), List.of(newWs)));
        listener.handlePostModifyEvent(postModifyEvent(store));

        verify(mediator).layerRenamed("sourceWs:roads", "targetWs:roads");
        assertThat(events)
                .extracting(TileLayerEvent::getPublishedId, TileLayerEvent::getName, TileLayerEvent::getOldName)
                .containsExactly(tuple("roads-id", "targetWs:roads", "sourceWs:roads"));
    }

    @Test
    void layerRemoved_publishesDeletedEvent() {
        LayerInfo layer = layerInfoMock("roads");
        when(layer.prefixedName()).thenReturn("topp:roads");

        listener.handleRemoveEvent(removeEvent(layer));

        assertThat(events)
                .extracting(TileLayerEvent::getPublishedId, TileLayerEvent::getName)
                .containsExactly(tuple("roads-id", "topp:roads"));
    }

    @Test
    void layerGroupRemoved_publishesDeletedEvent() {
        LayerGroupInfo group = layerGroupInfoMock("basemap");
        when(group.prefixedName()).thenReturn("topp:basemap");

        listener.handleRemoveEvent(removeEvent(group));

        assertThat(events)
                .extracting(TileLayerEvent::getPublishedId, TileLayerEvent::getName)
                .containsExactly(tuple("basemap-id", "topp:basemap"));
    }

    @Test
    void nonPublishedRemoval_publishesNothing() {
        listener.handleRemoveEvent(removeEvent(mock(WorkspaceInfo.class)));
        listener.handleRemoveEvent(removeEvent(mock(FeatureTypeInfo.class)));

        assertThat(events).isEmpty();
    }

    @Test
    void unrelatedModify_doesNothing() {
        listener.handleModifyEvent(
                modifyEvent(mock(org.geoserver.catalog.StyleInfo.class), List.of("name"), List.of("a"), List.of("b")));
        listener.handlePostModifyEvent(postModifyEvent(mock(org.geoserver.catalog.StyleInfo.class)));

        verifyNoInteractions(mediator);
    }

    @Test
    void postModify_drainsThreadLocal_andSecondPostIsNoOp() {
        WorkspaceInfo ws = mock(WorkspaceInfo.class);
        whenListByType(List.of(layerInfoMock("foo")), List.of());

        CatalogModifyEvent pre = modifyEvent(ws, List.of("name"), List.of("oldWs"), List.of("newWs"));
        CatalogPostModifyEvent post = postModifyEvent(ws);

        listener.handleModifyEvent(pre);
        listener.handlePostModifyEvent(post);
        listener.handlePostModifyEvent(post);

        verify(mediator).layerRenamed("oldWs:foo", "newWs:foo");
        verify(mediator, never()).layerRenamed("anything-else", "anything-else");
    }

    @Test
    void noGwcSingleton_isSafe() {
        GWC.set(null, null); // tear down the @BeforeEach setup
        WorkspaceInfo ws = mock(WorkspaceInfo.class);
        whenListByType(List.of(layerInfoMock("foo")), List.of());

        listener.handleModifyEvent(modifyEvent(ws, List.of("name"), List.of("oldWs"), List.of("newWs")));
        listener.handlePostModifyEvent(postModifyEvent(ws));

        verifyNoInteractions(mediator);
        assertThat(events)
                .as("cache eviction events do not depend on the GWC mediator")
                .extracting(TileLayerEvent::getOldName)
                .containsExactly("oldWs:foo");
    }

    private LayerInfo layerInfoMock(String name) {
        LayerInfo info = mock(LayerInfo.class);
        when(info.getId()).thenReturn(name + "-id");
        when(info.getName()).thenReturn(name);
        return info;
    }

    private LayerGroupInfo layerGroupInfoMock(String name) {
        LayerGroupInfo info = mock(LayerGroupInfo.class);
        when(info.getId()).thenReturn(name + "-id");
        when(info.getName()).thenReturn(name);
        return info;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void whenListByType(List<LayerInfo> layers, List<LayerGroupInfo> groups) {
        when(catalog.list(any(Class.class), any(Filter.class))).thenAnswer(invocation -> {
            Class<?> type = invocation.getArgument(0);
            if (LayerInfo.class.equals(type)) {
                return closeableIterator(layers);
            }
            if (LayerGroupInfo.class.equals(type)) {
                return closeableIterator(groups);
            }
            return closeableIterator(List.of());
        });
    }

    private <T> CloseableIterator<T> closeableIterator(List<T> values) {
        return new CloseableIteratorAdapter<>(values.iterator());
    }

    private CatalogModifyEvent modifyEvent(
            Object source, List<String> props, List<Object> oldValues, List<Object> newValues) {
        CatalogModifyEvent event = mock(CatalogModifyEvent.class);
        when(event.getSource()).thenReturn((org.geoserver.catalog.CatalogInfo) source);
        when(event.getPropertyNames()).thenReturn(props);
        when(event.getOldValues()).thenReturn(oldValues);
        when(event.getNewValues()).thenReturn(newValues);
        return event;
    }

    private org.geoserver.catalog.event.CatalogRemoveEvent removeEvent(Object source) {
        org.geoserver.catalog.event.CatalogRemoveEvent event =
                mock(org.geoserver.catalog.event.CatalogRemoveEvent.class);
        when(event.getSource()).thenReturn((org.geoserver.catalog.CatalogInfo) source);
        return event;
    }

    private CatalogPostModifyEvent postModifyEvent(Object source) {
        CatalogPostModifyEvent event = mock(CatalogPostModifyEvent.class);
        when(event.getSource()).thenReturn((org.geoserver.catalog.CatalogInfo) source);
        return event;
    }
}
