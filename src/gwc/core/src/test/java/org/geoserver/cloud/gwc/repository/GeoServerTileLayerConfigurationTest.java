/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gwc.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ows.LocalPublished;
import org.geoserver.ows.LocalWorkspace;
import org.geowebcache.config.TileLayerConfiguration;
import org.geowebcache.layer.TileLayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeoServerTileLayerConfigurationTest {

    private TileLayerConfiguration delegate;
    private GeoServerTileLayerConfiguration config;

    @BeforeEach
    void setUp() {
        delegate = mock(TileLayerConfiguration.class);
        config = new GeoServerTileLayerConfiguration(delegate, e -> {});
    }

    @AfterEach
    void cleanUp() {
        LocalWorkspace.remove();
        LocalPublished.remove();
    }

    @Test
    void getLayers_noLocalWorkspace_returnsAll() {
        GeoServerTileLayer tl = mockLayerTileLayer("ws1", "layer1");
        doReturn(List.of(tl)).when(delegate).getLayers();

        Collection<TileLayer> result = config.getLayers();
        assertThat(result).containsExactly(tl);
    }

    @Test
    void getLayers_localWorkspace_filtersMatchingLayer() {
        GeoServerTileLayer matching = mockLayerTileLayer("ws1", "layer1");
        GeoServerTileLayer other = mockLayerTileLayer("ws2", "layer2");
        doReturn(List.of(matching, other)).when(delegate).getLayers();

        LocalWorkspace.set(mockWorkspace("ws1"));

        Collection<TileLayer> result = config.getLayers();
        assertThat(result).containsExactly(matching);
    }

    @Test
    void getLayers_localWorkspaceAndPublished_filtersMatchingLayerName() {
        GeoServerTileLayer match = mockLayerTileLayer("ws1", "layer1");
        GeoServerTileLayer samews = mockLayerTileLayer("ws1", "layer2");
        doReturn(List.of(match, samews)).when(delegate).getLayers();

        LocalWorkspace.set(mockWorkspace("ws1"));
        LocalPublished.set(mockPublished("layer1"));

        Collection<TileLayer> result = config.getLayers();
        assertThat(result).containsExactly(match);
    }

    @Test
    void getLayers_localWorkspace_includesLayerGroup() {
        GeoServerTileLayer lg = mockLayerGroupTileLayer("ws1", "group1");
        doReturn(List.of(lg)).when(delegate).getLayers();

        LocalWorkspace.set(mockWorkspace("ws1"));

        Collection<TileLayer> result = config.getLayers();
        assertThat(result).containsExactly(lg);
    }

    @Test
    void getLayers_localWorkspace_excludesLayerGroupWithNullWorkspace() {
        GeoServerTileLayer lg = mockLayerGroupTileLayer(null, "group1");
        doReturn(List.of(lg)).when(delegate).getLayers();

        LocalWorkspace.set(mockWorkspace("ws1"));

        Collection<TileLayer> result = config.getLayers();
        assertThat(result).isEmpty();
    }

    @Test
    void getLayers_localWorkspace_excludesNonGeoServerTileLayer() {
        TileLayer plain = mock(TileLayer.class);
        doReturn(List.of(plain)).when(delegate).getLayers();

        LocalWorkspace.set(mockWorkspace("ws1"));

        Collection<TileLayer> result = config.getLayers();
        assertThat(result).isEmpty();
    }

    @Test
    void getLayers_localWorkspace_excludesUnknownPublishedInfoType() {
        GeoServerTileLayer unknown = mock(GeoServerTileLayer.class);
        when(unknown.getPublishedInfo()).thenReturn(mock(PublishedInfo.class));
        doReturn(List.of(unknown)).when(delegate).getLayers();

        LocalWorkspace.set(mockWorkspace("ws1"));

        Collection<TileLayer> result = config.getLayers();
        assertThat(result).isEmpty();
    }

    @Test
    void getLayer_noLocalWorkspace_returnsDelegateResult() {
        GeoServerTileLayer tl = mockLayerTileLayer("ws1", "layer1");
        when(delegate.getLayer("ws1:layer1")).thenReturn(Optional.of(tl));

        Optional<TileLayer> result = config.getLayer("ws1:layer1");
        assertThat(result).containsSame(tl);
    }

    @Test
    void getLayer_localWorkspace_matchingLayer_returnsIt() {
        GeoServerTileLayer tl = mockLayerTileLayer("ws1", "layer1");
        when(delegate.getLayer("ws1:layer1")).thenReturn(Optional.of(tl));

        LocalWorkspace.set(mockWorkspace("ws1"));

        Optional<TileLayer> result = config.getLayer("ws1:layer1");
        assertThat(result).containsSame(tl);
    }

    @Test
    void getLayer_localWorkspace_wrongWorkspace_returnsEmpty() {
        GeoServerTileLayer tl = mockLayerTileLayer("ws2", "layer1");
        when(delegate.getLayer("ws2:layer1")).thenReturn(Optional.of(tl));

        LocalWorkspace.set(mockWorkspace("ws1"));

        Optional<TileLayer> result = config.getLayer("ws2:layer1");
        assertThat(result).isEmpty();
    }

    @Test
    void getLayer_localWorkspaceAndPublished_nameMismatch_returnsEmpty() {
        GeoServerTileLayer tl = mockLayerTileLayer("ws1", "layer1");
        when(delegate.getLayer("ws1:layer1")).thenReturn(Optional.of(tl));

        LocalWorkspace.set(mockWorkspace("ws1"));
        LocalPublished.set(mockPublished("otherLayer"));

        Optional<TileLayer> result = config.getLayer("ws1:layer1");
        assertThat(result).isEmpty();
    }

    private static GeoServerTileLayer mockLayerTileLayer(String workspaceName, String layerName) {
        WorkspaceInfo ws = mockWorkspace(workspaceName);
        DataStoreInfo store = mock(DataStoreInfo.class);
        when(store.getWorkspace()).thenReturn(ws);
        ResourceInfo resource = mock(ResourceInfo.class);
        when(resource.getStore()).thenReturn(store);
        LayerInfo layerInfo = mock(LayerInfo.class);
        when(layerInfo.getResource()).thenReturn(resource);
        when(layerInfo.getName()).thenReturn(layerName);

        GeoServerTileLayer tl = mock(GeoServerTileLayer.class);
        when(tl.getPublishedInfo()).thenReturn(layerInfo);
        return tl;
    }

    private static GeoServerTileLayer mockLayerGroupTileLayer(String workspaceName, String groupName) {
        WorkspaceInfo ws = workspaceName != null ? mockWorkspace(workspaceName) : null;
        LayerGroupInfo lgi = mock(LayerGroupInfo.class);
        when(lgi.getWorkspace()).thenReturn(ws);
        when(lgi.getName()).thenReturn(groupName);

        GeoServerTileLayer tl = mock(GeoServerTileLayer.class);
        when(tl.getPublishedInfo()).thenReturn(lgi);
        return tl;
    }

    private static WorkspaceInfo mockWorkspace(String name) {
        WorkspaceInfo ws = mock(WorkspaceInfo.class);
        when(ws.getName()).thenReturn(name);
        return ws;
    }

    private static PublishedInfo mockPublished(String name) {
        PublishedInfo pub = mock(PublishedInfo.class);
        when(pub.getName()).thenReturn(name);
        return pub;
    }
}
