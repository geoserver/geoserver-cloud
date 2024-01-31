/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.backend.pgconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import lombok.NonNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ows.LocalWorkspace;
import org.geowebcache.config.BaseConfiguration;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @since 1.7
 */
class PgsqlTileLayerCatalogTest {

    private PgsqlTileLayerInfoRepository repository;
    private PgsqlTileLayerCatalog tlCatalog;

    private TileLayerMocking support;

    @BeforeEach
    void setUp() {
        support = new TileLayerMocking(new CatalogPlugin(), new GeoServerImpl());
        repository = mock(PgsqlTileLayerInfoRepository.class);
        tlCatalog = new PgsqlTileLayerCatalog(repository, support.getGridsets(), support.catalog());
    }

    @Test
    void testConstructor() {
        GridSetBroker gridsets = support.getGridsets();
        assertThat(tlCatalog.repository).isSameAs(repository);
        assertThat(tlCatalog.gridsetBroker).isSameAs(gridsets);

        Supplier<Catalog> catalog = support.catalog();
        Class<NullPointerException> npe = NullPointerException.class;
        assertThrows(
                npe,
                () ->
                        new PgsqlTileLayerCatalog(
                                (PgsqlTileLayerInfoRepository) null, gridsets, catalog));
        assertThrows(npe, () -> new PgsqlTileLayerCatalog(repository, null, catalog));
        assertThrows(npe, () -> new PgsqlTileLayerCatalog(repository, gridsets, null));
    }

    @Test
    void testGetIdentifier() {
        assertThat(tlCatalog.getIdentifier()).isEqualTo("PostgreSQL Tile Layer Catalog");
    }

    @Test
    void testGetLocation() {
        assertThat(tlCatalog.getLocation()).isEqualTo("PostgreSQL catalog and config database");
    }

    @Test
    void testGetPriority() {
        assertThat(tlCatalog.getPriority()).isEqualTo(BaseConfiguration.BASE_PRIORITY - 1);
    }

    @Test
    void testAfterPropertiesSetIsNoOp() {
        tlCatalog.afterPropertiesSet();
        verifyNoInteractions(repository);
    }

    @Test
    void testDeinitializeIsNoOp() {
        tlCatalog.deinitialize();
        verifyNoInteractions(repository);
    }

    @Test
    void testSetGridSetBroker() {
        GridSetBroker gridsets = support.getGridsets();
        var expected =
                assertThrows(
                        UnsupportedOperationException.class,
                        () -> tlCatalog.setGridSetBroker(gridsets));

        assertThat(expected.getMessage()).contains("use constructor injection instead");
    }

    @Test
    void testGetLayerCount() {
        when(repository.count()).thenReturn(10);
        var actual = tlCatalog.getLayerCount();
        assertThat(actual).isEqualTo(10);
        verify(repository, times(1)).count();
    }

    @Test
    void testGetLayerNames() {
        Set<String> expected = Set.of("L1", "ws1:L1");
        when(repository.findAllNames()).thenReturn(expected);
        var actual = tlCatalog.getLayerNames();
        assertThat(actual).isEqualTo(expected);
        verify(repository, times(1)).findAllNames();
    }

    @Test
    void testGetLayers_PsqlTileLayerInfo_must_have_PublishedInfo() {
        LayerInfo mockLayerInfo = support.layerInfo();
        TileLayerInfo tli = support.pgLayerInfo(mockLayerInfo);
        tli.setPublished(null);
        when(repository.findAll()).thenReturn(Stream.of(tli));

        NullPointerException npe =
                assertThrows(NullPointerException.class, () -> tlCatalog.getLayers());
        assertThat(npe.getMessage()).contains("publishedInfo");
    }

    @Test
    void testGetLayer_name_matching() {
        when(repository.find(any(), any())).thenReturn(Optional.empty());

        tlCatalog.getLayer("globalgroup");
        verify(repository, times(1)).find(null, "globalgroup");
        clearInvocations(repository);

        tlCatalog.getLayer("ws1:layer1");
        verify(repository, times(1)).find("ws1", "layer1");
        clearInvocations(repository);

        WorkspaceInfo localWs = support.workspace("ws2");
        LocalWorkspace.set(localWs);
        try {
            tlCatalog.getLayer("ws1:layer");
            verify(repository, never()).find("ws1", "layer");

            tlCatalog.getLayer("ws2:layer");
            verify(repository, times(1)).find("ws2", "layer");
        } finally {
            LocalWorkspace.remove();
        }
    }

    @Test
    void testGetLayer() {
        assertThrows(NullPointerException.class, () -> tlCatalog.getLayer(null));

        TileLayerInfo l1 = support.pgLayerInfo(support.layerInfo());
        TileLayerInfo l2 = support.pgLayerInfo(support.layerGroupInfo((String) null));
        TileLayerInfo l3 = support.pgLayerInfo(support.layerGroupInfo("testWorkspace"));

        when(repository.find(any(), any())).thenReturn(Optional.empty());
        mockFind(l1);
        mockFind(l2);
        mockFind(l3);

        Optional<TileLayer> result;

        result = tlCatalog.getLayer(l1.getPublished().prefixedName());
        assertTileLayer(l1, result);

        result = tlCatalog.getLayer(l2.getPublished().prefixedName());
        assertTileLayer(l2, result);

        result = tlCatalog.getLayer(l3.getPublished().prefixedName());
        assertTileLayer(l3, result);
    }

    @Test
    void testGetLayers() {
        TileLayerInfo l1 = support.pgLayerInfo(support.layerInfo());
        TileLayerInfo l2 = support.pgLayerInfo(support.layerGroupInfo((String) null));
        TileLayerInfo l3 = support.pgLayerInfo(support.layerGroupInfo("testWorkspace"));
        var tileLayerInfos = List.of(l1, l2, l3);
        when(repository.findAll()).thenReturn(tileLayerInfos.stream());

        List<GeoServerTileLayer> actual = tlCatalog.getLayers();
        assertThat(actual).hasSameSizeAs(tileLayerInfos);

        assertTileLayer(l1, actual.get(0));
        assertTileLayer(l2, actual.get(1));
        assertTileLayer(l3, actual.get(2));
    }

    private void assertTileLayer(TileLayerInfo expected, Optional<TileLayer> result) {
        assertThat(result).isPresent().get().isInstanceOf(GeoServerTileLayer.class);
        GeoServerTileLayer tileLayer = (GeoServerTileLayer) result.orElseThrow();
        assertTileLayer(expected, tileLayer);
    }

    private void assertTileLayer(TileLayerInfo expected, GeoServerTileLayer tileLayer) {
        TileLayerInfo pgTileLayer = tlCatalog.toInfo(tileLayer);
        assertThat(pgTileLayer).isEqualTo(expected);
    }

    private void mockFind(TileLayerInfo info) {
        PublishedInfo published = info.getPublished();
        String workspace = support.workspaceName(published);
        @NonNull String layer = support.layerName(published);
        when(repository.find(workspace, layer)).thenReturn(Optional.of(info));
    }

    @Test
    void testAddLayer_not_a_GeoSeverTileLayer() {
        assertThrows(
                IllegalArgumentException.class, () -> tlCatalog.addLayer(mock(TileLayer.class)));
    }

    @Test
    void testAddLayer_transient_GeoSeverTileLayer() {
        var gstl = mock(GeoServerTileLayer.class);
        when(gstl.isTransientLayer()).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> tlCatalog.addLayer(gstl));
    }

    @Test
    void testAddLayer() {
        assertThrows(NullPointerException.class, () -> tlCatalog.addLayer(null));
        GeoServerTileLayer li = support.geoServerTileLayer(support.layerInfo());
        var expected = tlCatalog.toInfo(li);
        tlCatalog.addLayer(li);
        verify(repository).add(expected);
    }

    @Test
    void testRemoveLayer() {
        assertThrows(NullPointerException.class, () -> tlCatalog.removeLayer(null));

        tlCatalog.removeLayer("globalgroup");
        verify(repository, times(1)).delete(null, "globalgroup");
        clearInvocations(repository);

        tlCatalog.removeLayer("ws1:layer1");
        verify(repository, times(1)).delete("ws1", "layer1");
        clearInvocations(repository);

        WorkspaceInfo localWs = support.workspace("ws2");
        LocalWorkspace.set(localWs);
        try {
            tlCatalog.removeLayer("ws1:layer");
            verify(repository, never()).delete("ws1", "layer");

            tlCatalog.removeLayer("ws2:layer");
            verify(repository, times(1)).delete("ws2", "layer");
        } finally {
            LocalWorkspace.remove();
        }
    }

    @Test
    void testModifyLayer_not_a_GeoSeverTileLayer() {
        assertThrows(
                IllegalArgumentException.class, () -> tlCatalog.modifyLayer(mock(TileLayer.class)));
    }

    @Test
    void testModifyLayer_transient_GeoSeverTileLayer() {
        var gstl = mock(GeoServerTileLayer.class);
        when(gstl.isTransientLayer()).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> tlCatalog.modifyLayer(gstl));
    }

    @Test
    void testModifyLayer() {
        assertThrows(NullPointerException.class, () -> tlCatalog.modifyLayer(null));

        var gstl = support.geoServerTileLayer(support.layerGroupInfo("workspace1"));
        when(repository.save(any())).thenReturn(false);
        assertThrows(NoSuchElementException.class, () -> tlCatalog.modifyLayer(gstl));

        var expected = tlCatalog.toInfo(gstl);
        verify(repository, times(1)).save(expected);

        clearInvocations(repository);
        when(repository.save(expected)).thenReturn(true);
        tlCatalog.modifyLayer(gstl);

        verify(repository, times(1)).save(expected);
    }

    @Test
    void testRenameLayer() {
        tlCatalog.renameLayer("oldname", "newname");
        verifyNoInteractions(repository);
    }

    @Test
    void testContainsLayer() {
        assertThrows(NullPointerException.class, () -> tlCatalog.containsLayer(null));

        tlCatalog.containsLayer("globalgroup");
        verify(repository, times(1)).exists(null, "globalgroup");
        clearInvocations(repository);

        tlCatalog.containsLayer("ws1:layer1");
        verify(repository, times(1)).exists("ws1", "layer1");
        clearInvocations(repository);

        WorkspaceInfo localWs = support.workspace("ws2");
        LocalWorkspace.set(localWs);
        try {
            tlCatalog.containsLayer("ws1:layer");
            verify(repository, never()).exists("ws1", "layer");

            tlCatalog.containsLayer("ws2:layer");
            verify(repository, times(1)).exists("ws2", "layer");
        } finally {
            LocalWorkspace.remove();
        }
    }

    @Test
    void testCanSave() {
        TileLayer tl = mock(TileLayer.class);
        assertThat(tlCatalog.canSave(tl)).isFalse();

        GeoServerTileLayer gstl = mock(GeoServerTileLayer.class);

        when(gstl.isTransientLayer()).thenReturn(true);
        assertThat(tlCatalog.canSave(gstl)).isFalse();

        clearInvocations(gstl);
        when(gstl.isTransientLayer()).thenReturn(false);
        assertThat(tlCatalog.canSave(gstl)).isTrue();
    }
}
