/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.model;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import lombok.NonNull;

import org.geowebcache.config.DefaultGridsets;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.GridSubsetFactory;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.MimeType;
import org.gwc.tiling.integration.local.GridSubsetInfoAdapter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * @since 1.0
 */
public class TileLayerMockSupport {

    private ConcurrentMap<String, TileLayerInfo> layers = new ConcurrentHashMap<>();
    private ConcurrentMap<String, TileLayer> tileLayers = new ConcurrentHashMap<>();

    public final GridSet worldEpsg3857 = new DefaultGridsets(false, false).worldEpsg3857();
    public final GridSet worldEpsg4326 = new DefaultGridsets(false, false).worldEpsg4326();

    private Map<String, GridSubset> subsets =
            Map.of( //
                    worldEpsg3857.getName(), fullSubset(worldEpsg3857), //
                    worldEpsg4326.getName(), fullSubset(worldEpsg4326) //
                    );

    public final GridSubsetInfo subset3857 =
            new GridSubsetInfoAdapter(subsets.get(worldEpsg3857.getName()));
    public final GridSubsetInfo subset4326 =
            new GridSubsetInfoAdapter(subsets.get(worldEpsg4326.getName()));

    public Collection<TileLayerInfo> getLayers() {
        return layers.values();
    }

    public Collection<TileLayer> getTileLayers() {
        return tileLayers.values();
    }

    public GridSubset fullSubset(@NonNull GridSet gridset) {
        int zoomStart = 0;
        int zoomStop = gridset.getNumLevels() - 1;
        return GridSubsetFactory.createGridSubSet(
                gridset, gridset.getBounds(), zoomStart, zoomStop);
    }

    public TileLayerInfo mockLayer(
            @NonNull String name, @NonNull GridSubsetInfo gridSubset, @NonNull MimeType mimeType) {

        return this.mockLayer(name, gridSubset, mimeType, null);
    }

    public TileLayerInfo mockLayer(
            @NonNull String name,
            @NonNull GridSubsetInfo gridSubset,
            @NonNull MimeType mimeType,
            String parameterIds) {

        return this.mockLayer(
                name,
                Set.of(gridSubset),
                List.of(mimeType),
                parameterIds == null ? List.of() : List.of(parameterIds));
    }

    public TileLayerInfo mockLayer(
            String name,
            Set<GridSubsetInfo> gridSubsets,
            List<MimeType> mimeTypes,
            List<String> parameterIds) {

        Set<String> formats =
                mimeTypes.stream().map(MimeType::getFormat).collect(Collectors.toSet());
        int metaTilingHeight = 4;
        int metaTilingWidth = metaTilingHeight;
        TileLayerInfoImpl l =
                new TileLayerInfoImpl(
                        name, formats, List.copyOf(gridSubsets), metaTilingWidth, metaTilingHeight);
        this.layers.put(name, l);

        Set<GridSubset> realSubsets =
                gridSubsets.stream()
                        .map(GridSubsetInfo::getName)
                        .map(subsets::get)
                        .collect(Collectors.toSet());
        TileLayer tileLayer = mockTileLayer(name, realSubsets, mimeTypes, parameterIds);
        this.tileLayers.put(name, tileLayer);
        return l;
    }

    private TileLayer mockTileLayer(
            String name,
            Set<GridSubset> gridSubsets,
            List<MimeType> mimeTypes,
            List<String> parameterIds) {

        TileLayer l = mock(TileLayer.class);
        when(l.getName()).thenReturn(name);
        when(l.getGridSubsets())
                .thenReturn(
                        gridSubsets.stream().map(GridSubset::getName).collect(Collectors.toSet()));
        when(l.getMimeTypes()).thenReturn(mimeTypes);

        for (GridSubset gs : gridSubsets) {
            when(l.getGridSubset(eq(gs.getName()))).thenReturn(gs);
        }

        when(l.getMetaTilingFactors()).thenReturn(new int[] {4, 4});
        return l;
    }
}
