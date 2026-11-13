/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.backend.pgconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.geoserver.cloud.gwc.backend.pgconfig.PgconfigTileLayerCatalog.LISTING_BATCH_SIZE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.cloud.backend.pgconfig.cache.PutBudget;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.GWCSynchEnv;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.config.GWCConfigPersister;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ows.LocalWorkspace;
import org.geowebcache.config.BaseConfiguration;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** @since 1.7 */
class PgconfigTileLayerCatalogTest {

    private TileLayerInfoRepository repository;
    private ExtendedCatalogFacade facade;
    private PgconfigTileLayerCatalog tlCatalog;
    private TileLayerMocking support;
    private GWCConfigPersister defaultsProvider;
    private GWC gwcMediator;

    /** Keeps no tile layer: each lookup builds its own. */
    private BuiltTileLayerCache builtTileLayers;

    @BeforeEach
    void setUp() {
        CatalogPlugin catalog = new CatalogPlugin();
        support = new TileLayerMocking(catalog, new GeoServerImpl());
        repository = mock(TileLayerInfoRepository.class);
        facade = mock(ExtendedCatalogFacade.class);
        when(facade.getCatalog()).thenReturn(catalog);
        defaultsProvider = mock(GWCConfigPersister.class);
        GWCConfig defaults = new GWCConfig();
        when(defaultsProvider.getConfig()).thenReturn(defaults);
        builtTileLayers = new BuiltTileLayerCache(null, mock(DataSource.class), 0);
        tlCatalog = listingCatalog(PutBudget::unlimited);

        // Install a mock GWC singleton so removeLayer's bridge to GWC.layerRemoved doesn't NPE
        gwcMediator = mock(GWC.class);
        GWC.set(gwcMediator, mock(GWCSynchEnv.class));
    }

    @AfterEach
    void tearDown() {
        GWC.set(null, null);
    }

    @Test
    void testConstructor() {
        GridSetBroker gridsets = support.getGridsets();
        assertThat(tlCatalog.repository).isSameAs(repository);
        assertThat(tlCatalog.gridsetBroker).isSameAs(gridsets);

        Supplier<ExtendedCatalogFacade> catalogFacade = () -> facade;
        GWCConfigPersister defaults = defaultsProvider;
        BuiltTileLayerCache built = builtTileLayers;
        Supplier<PutBudget> budget = PutBudget::unlimited;
        Class<NullPointerException> npe = NullPointerException.class;
        assertThrows(npe, () -> new PgconfigTileLayerCatalog(null, gridsets, catalogFacade, defaults, built, budget));
        assertThrows(npe, () -> new PgconfigTileLayerCatalog(repository, null, catalogFacade, defaults, built, budget));
        assertThrows(npe, () -> new PgconfigTileLayerCatalog(repository, gridsets, null, defaults, built, budget));
        assertThrows(npe, () -> new PgconfigTileLayerCatalog(repository, gridsets, catalogFacade, null, built, budget));
        assertThrows(
                npe, () -> new PgconfigTileLayerCatalog(repository, gridsets, catalogFacade, defaults, null, budget));
        assertThrows(
                npe, () -> new PgconfigTileLayerCatalog(repository, gridsets, catalogFacade, defaults, built, null));
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
        UnsupportedOperationException expected =
                assertThrows(UnsupportedOperationException.class, () -> tlCatalog.setGridSetBroker(gridsets));

        assertThat(expected.getMessage()).contains("use constructor injection instead");
    }

    // each call counts again
    @Test
    void testGetLayerCount() {
        when(repository.count()).thenReturn(10, 11);
        assertThat(tlCatalog.getLayerCount()).isEqualTo(10);
        assertThat(tlCatalog.getLayerCount()).isEqualTo(11);
        verify(repository, times(2)).count();
    }

    // each call reads the names again
    @Test
    void testGetLayerNames() {
        Set<String> expected = Set.of("L1", "ws1:L1");
        Set<String> renamed = Set.of("L2", "ws1:L1");
        when(repository.findAllNames()).thenReturn(expected, renamed);
        assertThat(tlCatalog.getLayerNames()).isEqualTo(expected);
        assertThat(tlCatalog.getLayerNames()).isEqualTo(renamed);
        verify(repository, times(2)).findAllNames();
    }

    // a prefixed name is a layer, else a group of the workspace; a bare name is a group without workspace
    @Test
    void testGetLayer_name_matching() {
        WorkspaceInfo ws1 = support.workspace("ws1");
        when(facade.getWorkspaceByName("ws1")).thenReturn(ws1);

        tlCatalog.getLayer("globalgroup");
        verify(facade, times(1)).getLayerGroupByName(CatalogFacade.NO_WORKSPACE, "globalgroup");
        verify(facade, never()).getLayerByName(any());
        clearInvocations(facade);

        tlCatalog.getLayer("ws1:layer1");
        verify(facade, times(1)).getLayerByName("ws1:layer1");
        verify(facade, times(1)).getLayerGroupByName(ws1, "layer1");
        clearInvocations(facade);

        tlCatalog.getLayer("nows:layer1");
        verify(facade, times(1)).getLayerByName("nows:layer1");
        verify(facade, never()).getLayerGroupByName(any(), any());
        clearInvocations(facade);

        WorkspaceInfo localWs = support.workspace("ws2");
        LocalWorkspace.set(localWs);
        try {
            tlCatalog.getLayer("ws1:layer");
            verifyNoInteractions(facade);

            tlCatalog.getLayer("layer");
            verify(facade, times(1)).getLayerByName("ws2:layer");
        } finally {
            LocalWorkspace.remove();
        }
    }

    @Test
    void testGetLayer() {
        assertThrows(NullPointerException.class, () -> tlCatalog.getLayer(null));

        LayerInfo layer = support.layerInfo();
        LayerGroupInfo globalGroup = support.layerGroupInfo((String) null);
        WorkspaceInfo workspace = support.workspace("testWorkspace");
        LayerGroupInfo group = support.layerGroupInfo(workspace);
        when(facade.getLayerByName(layer.prefixedName())).thenReturn(layer);
        when(facade.getLayerGroupByName(same(CatalogFacade.NO_WORKSPACE), eq(globalGroup.getName())))
                .thenReturn(globalGroup);
        when(facade.getWorkspaceByName("testWorkspace")).thenReturn(workspace);
        when(facade.getLayerGroupByName(same(workspace), eq(group.getName()))).thenReturn(group);

        TileLayerInfo l1 = support.pgLayerInfo();
        TileLayerInfo l2 = support.pgLayerInfo().setBlobStoreId("globalGroupBlobStore");
        TileLayerInfo l3 = support.pgLayerInfo().setBlobStoreId("groupBlobStore");
        mockFindSettings(layer, l1);
        mockFindSettings(globalGroup, l2);
        mockFindSettings(group, l3);

        assertTileLayer(layer, l1, tlCatalog.getLayer(layer.prefixedName()));
        assertTileLayer(globalGroup, l2, tlCatalog.getLayer(globalGroup.prefixedName()));
        assertTileLayer(group, l3, tlCatalog.getLayer(group.prefixedName()));
    }

    @Test
    void layerWithoutSettingsIsNone() {
        layerFoundAs("ws1:layer1");
        when(repository.findSettings(any())).thenReturn(Optional.empty());

        assertThat(tlCatalog.getLayer("ws1:layer1")).isEmpty();
        assertThat(tlCatalog.containsLayer("ws1:layer1")).isFalse();
    }

    // each lookup builds its own tile layer from the settings, and never hands the settings out
    @Test
    void lookupBuildsNewTileLayer() {
        LayerInfo layer = support.layerInfo();
        when(facade.getLayerByName(layer.prefixedName())).thenReturn(layer);
        TileLayerInfo settings = support.pgLayerInfo();
        mockFindSettings(layer, settings);

        GeoServerTileLayer first =
                (GeoServerTileLayer) tlCatalog.getLayer(layer.prefixedName()).orElseThrow();
        GeoServerTileLayer second =
                (GeoServerTileLayer) tlCatalog.getLayer(layer.prefixedName()).orElseThrow();
        first.getInfo().setBlobStoreId("changed");

        assertThat(second).isNotSameAs(first);
        assertThat(second.getInfo()).isNotSameAs(first.getInfo());
        assertThat(second.getBlobStoreId()).isEqualTo("defaultBlobStore");
        assertThat(settings.getBlobStoreId()).isEqualTo("defaultBlobStore");
    }

    // the tile layers follow the order of the ids, whatever the order of the layers and groups found
    @Test
    void testGetLayers() {
        LayerInfo layer = support.layerInfo();
        LayerGroupInfo globalGroup = support.layerGroupInfo((String) null);
        LayerGroupInfo group = support.layerGroupInfo("testWorkspace");
        TileLayerInfo l1 = support.pgLayerInfo();
        TileLayerInfo l2 = support.pgLayerInfo().setBlobStoreId("globalGroupBlobStore");
        TileLayerInfo l3 = support.pgLayerInfo().setBlobStoreId("groupBlobStore");
        List<String> ids = List.of(layer.getId(), globalGroup.getId(), group.getId());
        when(repository.findAllIds()).thenReturn(ids);
        when(facade.findAllById(ids, PublishedInfo.class)).thenReturn(Stream.of(group, layer, globalGroup));
        when(repository.findAllSettings(ids))
                .thenReturn(Map.of(layer.getId(), l1, globalGroup.getId(), l2, group.getId(), l3));

        List<GeoServerTileLayer> actual = tlCatalog.getLayers();

        assertThat(actual).extracting(GeoServerTileLayer::getId).containsExactlyElementsOf(ids);
        assertTileLayer(layer, l1, actual.getFirst());
        assertTileLayer(globalGroup, l2, actual.get(1));
        assertTileLayer(group, l3, actual.get(2));
    }

    // the catalog changed after the read of the ids: the settings are read for the layers found only
    @Test
    void listingSkipsUnmatchedIds() {
        LayerInfo layer = support.layerInfo();
        LayerInfo withoutSettings = support.layerInfo();
        List<String> ids = List.of("removed", layer.getId(), withoutSettings.getId());
        List<String> foundIds = List.of(layer.getId(), withoutSettings.getId());
        when(repository.findAllIds()).thenReturn(ids);
        when(facade.findAllById(ids, PublishedInfo.class)).thenReturn(Stream.of(layer, withoutSettings));
        when(repository.findAllSettings(foundIds)).thenReturn(Map.of(layer.getId(), support.pgLayerInfo()));

        List<GeoServerTileLayer> listed = tlCatalog.getLayers();

        assertThat(listed).extracting(GeoServerTileLayer::getId).containsExactly(layer.getId());
        verify(repository).findAllSettings(foundIds);
    }

    // the layers and groups of other workspaces cost no settings read and no build
    @Test
    void virtualServiceSkipsOthers() {
        WorkspaceInfo ws = support.workspace("ws1");
        LayerInfo layer = support.layerInfo(ws);
        LayerInfo otherLayer = support.layerInfo();
        LayerGroupInfo globalGroup = support.layerGroupInfo((String) null);
        List<String> ids = List.of(layer.getId(), otherLayer.getId(), globalGroup.getId());
        List<String> serviceIds = List.of(layer.getId());
        when(repository.findAllIds()).thenReturn(ids);
        when(facade.findAllById(ids, PublishedInfo.class)).thenReturn(Stream.of(layer, otherLayer, globalGroup));
        when(repository.findAllSettings(serviceIds)).thenReturn(Map.of(layer.getId(), support.pgLayerInfo()));

        List<GeoServerTileLayer> listed;
        LocalWorkspace.set(ws);
        try {
            listed = tlCatalog.getLayers();
        } finally {
            LocalWorkspace.remove();
        }

        assertThat(listed).extracting(GeoServerTileLayer::getId).containsExactly(layer.getId());
        verify(repository).findAllSettings(serviceIds);
    }

    // one budget per listing, bound to the thread while each batch reads
    @Test
    void listingReadsInBatches() {
        List<PutBudget> supplied = new ArrayList<>();
        PgconfigTileLayerCatalog listing = listingCatalog(() -> {
            PutBudget budget = PutBudget.unlimited();
            supplied.add(budget);
            return budget;
        });
        List<String> ids = IntStream.rangeClosed(0, LISTING_BATCH_SIZE)
                .mapToObj(i -> "layer" + i)
                .toList();
        when(repository.findAllIds()).thenReturn(ids);
        List<Optional<PutBudget>> bound = new ArrayList<>();
        when(facade.findAllById(anyList(), eq(PublishedInfo.class))).thenAnswer(call -> {
            bound.add(PutBudget.current());
            return Stream.empty();
        });
        when(repository.findAllSettings(anyCollection())).thenAnswer(call -> {
            bound.add(PutBudget.current());
            return Map.of();
        });

        assertThat(listing.getLayers()).isEmpty();

        verify(facade).findAllById(ids.subList(0, LISTING_BATCH_SIZE), PublishedInfo.class);
        verify(facade).findAllById(List.of(ids.getLast()), PublishedInfo.class);
        assertThat(supplied).hasSize(1);
        assertThat(bound).hasSize(4).containsOnly(Optional.of(supplied.getFirst()));
        assertThat(PutBudget.current()).isEmpty();
    }

    private PgconfigTileLayerCatalog listingCatalog(Supplier<PutBudget> listingBudget) {
        return new PgconfigTileLayerCatalog(
                repository, support.getGridsets(), () -> facade, defaultsProvider, builtTileLayers, listingBudget);
    }

    private void assertTileLayer(PublishedInfo published, TileLayerInfo settings, Optional<TileLayer> result) {
        assertThat(result).isPresent().get().isInstanceOf(GeoServerTileLayer.class);
        GeoServerTileLayer tileLayer = (GeoServerTileLayer) result.orElseThrow();
        assertTileLayer(published, settings, tileLayer);
    }

    /** Checks that the tile layer has the layer or group, and the settings completed with the defaults of GWC. */
    private void assertTileLayer(PublishedInfo published, TileLayerInfo settings, GeoServerTileLayer tileLayer) {
        GeoServerTileLayer expectedTileLayer = tlCatalog.toLayer(settings, published);
        TileLayerInfo expected = tlCatalog.toInfo(expectedTileLayer);

        tlCatalog.completeWithDefaults(tileLayer.getInfo(), tileLayer.getPublishedInfo());

        TileLayerInfo pgTileLayer = tlCatalog.toInfo(tileLayer);
        assertThat(pgTileLayer).isEqualTo(expected);
        assertThat(tileLayer.getPublishedInfo().getId()).isEqualTo(published.getId());
    }

    private void mockFindSettings(PublishedInfo published, TileLayerInfo settings) {
        when(repository.findSettings(published.getId())).thenReturn(Optional.of(settings));
    }

    /** Makes the facade find a layer by its prefixed name, and returns the id of the layer. */
    private String layerFoundAs(String prefixedName) {
        String id = "layer-" + prefixedName;
        LayerInfo layer = mock(LayerInfo.class);
        when(layer.getId()).thenReturn(id);
        when(facade.getLayerByName(prefixedName)).thenReturn(layer);
        return id;
    }

    /**
     * Makes the facade find a group without workspace by its name, and returns the id of the group. The stub matches
     * {@code NO_WORKSPACE} by identity: the constant is a proxy answering null to {@code equals}.
     */
    private String globalGroupFoundAs(String name) {
        String id = "group-" + name;
        LayerGroupInfo group = mock(LayerGroupInfo.class);
        when(group.getId()).thenReturn(id);
        when(facade.getLayerGroupByName(same(CatalogFacade.NO_WORKSPACE), eq(name)))
                .thenReturn(group);
        return id;
    }

    @Test
    void testAddLayer_not_a_GeoSeverTileLayer() {
        assertThrows(IllegalArgumentException.class, () -> tlCatalog.addLayer(mock(TileLayer.class)));
    }

    @Test
    void testAddLayer_transient_GeoSeverTileLayer() {
        GeoServerTileLayer gstl = mock(GeoServerTileLayer.class);
        when(gstl.isTransientLayer()).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> tlCatalog.addLayer(gstl));
    }

    @Test
    void testAddLayer() {
        assertThrows(NullPointerException.class, () -> tlCatalog.addLayer(null));
        GeoServerTileLayer li = support.geoServerTileLayer(support.layerInfo());
        TileLayerInfo expected = tlCatalog.toInfo(li);
        tlCatalog.addLayer(li);
        verify(repository).save(li.getPublishedInfo().getId(), expected);
    }

    @Test
    void testRemoveLayer() {
        assertThrows(NullPointerException.class, () -> tlCatalog.removeLayer(null));
        String globalGroupId = globalGroupFoundAs("globalgroup");
        String layerId = layerFoundAs("ws1:layer1");
        String localLayerId = layerFoundAs("ws2:layer");

        tlCatalog.removeLayer("globalgroup");
        verify(repository, times(1)).delete(globalGroupId);
        clearInvocations(repository);

        tlCatalog.removeLayer("ws1:layer1");
        verify(repository, times(1)).delete(layerId);
        clearInvocations(repository);

        WorkspaceInfo localWs = support.workspace("ws2");
        LocalWorkspace.set(localWs);
        try {
            tlCatalog.removeLayer("ws1:layer1");
            verify(repository, never()).delete(any());

            tlCatalog.removeLayer("ws2:layer");
            verify(repository, times(1)).delete(localLayerId);
        } finally {
            LocalWorkspace.remove();
        }
    }

    @Test
    void testRemoveLayer_bridges_to_GWC_layerRemoved() {
        String layerId = layerFoundAs("ws1:layer1");
        tlCatalog.removeLayer("ws1:layer1");
        verify(repository, times(1)).delete(layerId);
        verify(gwcMediator, times(1)).layerRemoved("ws1:layer1");

        clearInvocations(repository, gwcMediator);
        String globalGroupId = globalGroupFoundAs("globalgroup");
        tlCatalog.removeLayer("globalgroup");
        verify(repository, times(1)).delete(globalGroupId);
        verify(gwcMediator, times(1)).layerRemoved("globalgroup");

        clearInvocations(repository, gwcMediator);
        String namedId = layerFoundAs("ws3:named");
        WorkspaceInfo localWs = support.workspace("ws3");
        LocalWorkspace.set(localWs);
        try {
            tlCatalog.removeLayer("named");
            verify(repository, times(1)).delete(namedId);
            verify(gwcMediator, times(1)).layerRemoved("ws3:named");
        } finally {
            LocalWorkspace.remove();
        }
    }

    @Test
    void testRemoveLayer_no_GWC_singleton_is_safe() {
        GWC.set(null, null);
        String layerId = layerFoundAs("ws1:layer1");
        tlCatalog.removeLayer("ws1:layer1");
        verify(repository, times(1)).delete(layerId);
    }

    @Test
    void testModifyLayer_not_a_GeoSeverTileLayer() {
        assertThrows(IllegalArgumentException.class, () -> tlCatalog.modifyLayer(mock(TileLayer.class)));
    }

    @Test
    void testModifyLayer_transient_GeoSeverTileLayer() {
        GeoServerTileLayer gstl = mock(GeoServerTileLayer.class);
        when(gstl.isTransientLayer()).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> tlCatalog.modifyLayer(gstl));
    }

    @Test
    void testModifyLayer() {
        assertThrows(NullPointerException.class, () -> tlCatalog.modifyLayer(null));

        GeoServerTileLayer gstl = support.geoServerTileLayer(support.layerGroupInfo("workspace1"));
        String publishedId = gstl.getPublishedInfo().getId();
        when(repository.save(any(), any())).thenReturn(false);
        assertThrows(NoSuchElementException.class, () -> tlCatalog.modifyLayer(gstl));

        TileLayerInfo expected = tlCatalog.toInfo(gstl);
        verify(repository, times(1)).save(publishedId, expected);

        clearInvocations(repository);
        when(repository.save(publishedId, expected)).thenReturn(true);
        tlCatalog.modifyLayer(gstl);

        verify(repository, times(1)).save(publishedId, expected);
    }

    @Test
    void testRenameLayer() {
        tlCatalog.renameLayer("oldname", "newname");
        verifyNoInteractions(repository);
    }

    // containsLayer builds the tile layer, as getLayer does
    @Test
    void testContainsLayer() {
        assertThrows(NullPointerException.class, () -> tlCatalog.containsLayer(null));
        LayerGroupInfo globalGroup = support.layerGroupInfo((String) null);
        WorkspaceInfo ws1 = support.workspace("ws1");
        WorkspaceInfo ws2 = support.workspace("ws2");
        LayerInfo layer = support.layerInfo(ws1, "layer1");
        LayerInfo localLayer = support.layerInfo(ws2, "layer");
        when(facade.getLayerGroupByName(same(CatalogFacade.NO_WORKSPACE), eq(globalGroup.getName())))
                .thenReturn(globalGroup);
        when(facade.getLayerByName("ws1:layer1")).thenReturn(layer);
        when(facade.getLayerByName("ws2:layer")).thenReturn(localLayer);
        mockFindSettings(globalGroup, support.pgLayerInfo());
        mockFindSettings(layer, support.pgLayerInfo());
        mockFindSettings(localLayer, support.pgLayerInfo());

        assertThat(tlCatalog.containsLayer(globalGroup.getName())).isTrue();
        verify(repository, times(1)).findSettings(globalGroup.getId());
        clearInvocations(repository);

        assertThat(tlCatalog.containsLayer("ws1:layer1")).isTrue();
        verify(repository, times(1)).findSettings(layer.getId());
        clearInvocations(repository);

        LocalWorkspace.set(ws2);
        try {
            assertThat(tlCatalog.containsLayer("ws1:layer1")).isFalse();
            verify(repository, never()).findSettings(any());

            assertThat(tlCatalog.containsLayer("ws2:layer")).isTrue();
            verify(repository, times(1)).findSettings(localLayer.getId());
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
