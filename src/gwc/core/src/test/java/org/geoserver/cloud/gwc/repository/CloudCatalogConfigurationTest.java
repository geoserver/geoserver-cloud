/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.GWCSynchEnv;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.GeoServerTileLayerInfoImpl;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geowebcache.config.DefaultGridsets;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * In a cluster, a tile layer configuration can be visible through the shared storage or a
 * {@link org.geoserver.cloud.gwc.event.TileLayerEvent} before this node's catalog replicated the {@link LayerInfo} it
 * refers to. {@link CloudCatalogConfiguration} must leave such tile layers out of its listings instead of letting
 * {@link org.geoserver.gwc.layer.GeoServerTileLayer#getPublishedInfo()} throw {@link IllegalStateException} and break
 * whole responses like WMTS GetCapabilities, while its lookups by name keep answering about the stored configuration,
 * which is how a removed layer's tile layer gets deleted.
 */
class CloudCatalogConfigurationTest {

    private static final String LAYER_ID = "LayerInfo-123";
    private static final String LAYER_NAME = "test:roads";

    private Catalog catalog;
    private TileLayerCatalog tileLayerCatalog;
    private CloudCatalogConfiguration config;

    @BeforeEach
    void setUp() {
        catalog = mock(Catalog.class);
        tileLayerCatalog = mock(TileLayerCatalog.class);

        GeoServerTileLayerInfo info = new GeoServerTileLayerInfoImpl();
        info.setId(LAYER_ID);
        info.setName(LAYER_NAME);
        info.getMimeFormats().add("image/png");
        info.setMetaTilingX(4);
        info.setMetaTilingY(4);

        when(tileLayerCatalog.getLayerNames()).thenReturn(Set.of(LAYER_NAME));
        when(tileLayerCatalog.getLayerIds()).thenReturn(Set.of(LAYER_ID));
        when(tileLayerCatalog.getLayerId(LAYER_NAME)).thenReturn(LAYER_ID);
        when(tileLayerCatalog.getLayerById(LAYER_ID)).thenReturn(info);
        when(tileLayerCatalog.getLayerByName(LAYER_NAME)).thenReturn(info);
        when(tileLayerCatalog.exists(LAYER_ID)).thenReturn(true);
        when(tileLayerCatalog.getLayerName(LAYER_ID)).thenReturn(LAYER_NAME);

        GridSetBroker gridSetBroker = new GridSetBroker(List.of(new DefaultGridsets(true, true)));
        config = new CloudCatalogConfiguration(catalog, tileLayerCatalog, gridSetBroker);
        GWC.set(mock(GWC.class), mock(GWCSynchEnv.class));
    }

    @AfterEach
    void tearDown() {
        GWC.set(null, null);
    }

    @Test
    void listingsExcludeTileLayersNotYetInTheLocalCatalog() {
        when(catalog.getLayer(LAYER_ID)).thenReturn(null);
        when(catalog.getLayerGroup(LAYER_ID)).thenReturn(null);

        assertThat(config.getLayers()).isEmpty();
        assertThat(config.getLayerNames()).isEmpty();
        assertThat(config.getLayerCount()).isZero();
    }

    @Test
    void addLayerForStoredTileLayerBehavesAsSave() {
        when(catalog.getLayer(LAYER_ID)).thenReturn(null);
        when(catalog.getLayerGroup(LAYER_ID)).thenReturn(null);

        GeoServerTileLayerInfo info = new GeoServerTileLayerInfoImpl();
        info.setId(LAYER_ID);
        info.setName(LAYER_NAME);
        GeoServerTileLayer tileLayer = new GeoServerTileLayer(
                catalog, LAYER_ID, new GridSetBroker(List.of(new DefaultGridsets(true, true))), info);

        assertThatCode(() -> config.addLayer(tileLayer))
                .as("adding over a tile layer hidden until the catalog catches up must not fail")
                .doesNotThrowAnyException();
    }

    /**
     * Reproduces the stale tile layer left behind by a layer removal: upstream's {@code CatalogLayerEventListener} runs
     * after the catalog dropped the {@code LayerInfo}, and reaches the tile layer catalog through
     * {@code GWC.hasTileLayer} -> {@code TileLayerDispatcher.layerExists} -> {@code getLayer} and
     * {@code TileLayerDispatcher.removeLayer} -> {@code containsLayer}.
     */
    @Test
    void removalFindsTheTileLayerOfAnAlreadyRemovedLayerInfo() {
        when(catalog.getLayer(LAYER_ID)).thenReturn(null);
        when(catalog.getLayerGroup(LAYER_ID)).thenReturn(null);

        assertThat(config.getLayer(LAYER_NAME))
                .as("TileLayerDispatcher.layerExists() must still see the stored configuration")
                .isPresent();
        assertThat(config.containsLayer(LAYER_NAME))
                .as("TileLayerDispatcher.removeLayer() must still find the configuration to remove")
                .isTrue();

        config.removeLayer(LAYER_NAME);
        verify(tileLayerCatalog).delete(LAYER_ID);
    }

    @Test
    void getLayerReturnsTileLayersResolvableAgainstTheLocalCatalog() {
        LayerInfo layer = mock(LayerInfo.class);
        when(layer.getId()).thenReturn(LAYER_ID);
        when(layer.getName()).thenReturn("roads");
        when(layer.prefixedName()).thenReturn(LAYER_NAME);
        when(layer.getStyles()).thenReturn(new HashSet<>());
        when(catalog.getLayer(LAYER_ID)).thenReturn(layer);

        assertThat(config.getLayer(LAYER_NAME)).isPresent();
        assertThat(config.getLayers()).hasSize(1);
        assertThat(config.getLayerNames()).containsExactly(LAYER_NAME);
        assertThat(config.getLayerCount()).isOne();
        assertThat(config.containsLayer(LAYER_NAME)).isTrue();
        assertThatCode(() -> config.getLayers().forEach(TileLayer::getName)).doesNotThrowAnyException();
    }
}
