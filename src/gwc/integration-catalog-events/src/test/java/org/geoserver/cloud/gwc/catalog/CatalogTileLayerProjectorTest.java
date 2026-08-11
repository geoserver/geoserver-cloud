/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.catalog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.function.Function;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.cloud.event.catalog.CatalogInfoAdded;
import org.geoserver.cloud.event.catalog.CatalogInfoModified;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoved;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogTileLayerProjectorTest {

    private Catalog catalog;
    private GWC gwc;
    private GWCConfig config;
    private Function<PublishedInfo, GeoServerTileLayer> factory;
    private CatalogTileLayerProjector projector;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        catalog = mock(Catalog.class);
        gwc = mock(GWC.class);
        config = mock(GWCConfig.class);
        factory = mock(Function.class);
        projector = new CatalogTileLayerProjector(catalog, gwc, factory);
        when(gwc.getConfig()).thenReturn(config);
    }

    @Test
    void addedLayerCreatesTileLayerFromCurrentCatalogState() {
        LayerInfo layer = layer("layer-id", "test:roads");
        GeoServerTileLayer tileLayer = mock(GeoServerTileLayer.class);
        CatalogInfoAdded event = event(CatalogInfoAdded.class, ConfigInfoType.LAYER, "layer-id", "test:roads");
        when(catalog.getLayer("layer-id")).thenReturn(layer);
        when(config.isSane()).thenReturn(true);
        when(config.isCacheLayersByDefault()).thenReturn(true);
        when(factory.apply(layer)).thenReturn(tileLayer);

        projector.onCatalogInfoAdded(event);

        verify(gwc).add(tileLayer);
    }

    @Test
    void duplicateAddPreservesExistingTileLayerConfiguration() {
        LayerInfo layer = layer("layer-id", "test:roads");
        GeoServerTileLayer existing = tileLayer("test:roads");
        CatalogInfoAdded event = event(CatalogInfoAdded.class, ConfigInfoType.LAYER, "layer-id", "test:roads");
        when(catalog.getLayer("layer-id")).thenReturn(layer);
        when(gwc.hasTileLayer(layer)).thenReturn(true);
        when(gwc.getTileLayer(layer)).thenReturn(existing);

        projector.onCatalogInfoAdded(event);

        verifyNoInteractions(factory);
        verify(gwc, never()).add(any());
        verify(gwc, never()).rename(any(), any());
    }

    @Test
    void automaticCachingDisabledDoesNotCreateTileLayer() {
        LayerGroupInfo group = group("group-id", "test:basemap");
        CatalogInfoAdded event = event(CatalogInfoAdded.class, ConfigInfoType.LAYERGROUP, "group-id", "test:basemap");
        when(catalog.getLayerGroup("group-id")).thenReturn(group);
        when(config.isSane()).thenReturn(true);
        when(config.isCacheLayersByDefault()).thenReturn(false);

        projector.onCatalogInfoAdded(event);

        verifyNoInteractions(factory);
        verify(gwc, never()).add(any());
    }

    @Test
    void modifiedLayerRenamesExistingConfigurationWithoutReplacingIt() {
        LayerInfo layer = layer("layer-id", "test:new-name");
        CatalogInfoModified event = event(CatalogInfoModified.class, ConfigInfoType.LAYER, "layer-id", "test:new-name");
        when(catalog.getLayer("layer-id")).thenReturn(layer);
        when(event.getOldName()).thenReturn("old-name");
        when(gwc.tileLayerExists("test:old-name")).thenReturn(true);

        projector.onCatalogInfoModified(event);

        verify(gwc).rename("test:old-name", "test:new-name");
        verifyNoInteractions(factory);
    }

    @Test
    void duplicateRemoveIsANoOp() {
        CatalogInfoRemoved event = event(CatalogInfoRemoved.class, ConfigInfoType.LAYER, "layer-id", "test:roads");

        projector.onCatalogInfoRemoved(event);

        verify(gwc, never()).removeTileLayers(any());
    }

    @Test
    void removeDeletesExistingTileLayerByRecordedName() {
        CatalogInfoRemoved event = event(CatalogInfoRemoved.class, ConfigInfoType.LAYER, "layer-id", "test:roads");
        when(gwc.tileLayerExists("test:roads")).thenReturn(true);

        projector.onCatalogInfoRemoved(event);

        verify(gwc).removeTileLayers(java.util.List.of("test:roads"));
    }

    @Test
    void ignoresCatalogObjectsThatAreNotPublished() {
        CatalogInfoAdded event = event(CatalogInfoAdded.class, ConfigInfoType.FEATURETYPE, "resource-id", "test:roads");

        projector.onCatalogInfoAdded(event);

        verifyNoInteractions(factory);
        verifyNoInteractions(gwc);
        verifyNoInteractions(catalog);
    }

    private LayerInfo layer(String id, String name) {
        LayerInfo layer = mock(LayerInfo.class);
        ResourceInfo resource = mock(ResourceInfo.class);
        when(layer.getId()).thenReturn(id);
        when(layer.getResource()).thenReturn(resource);
        when(resource.prefixedName()).thenReturn(name);
        return layer;
    }

    private LayerGroupInfo group(String id, String name) {
        LayerGroupInfo group = mock(LayerGroupInfo.class);
        when(group.getId()).thenReturn(id);
        when(group.prefixedName()).thenReturn(name);
        return group;
    }

    private GeoServerTileLayer tileLayer(String name) {
        GeoServerTileLayer layer = mock(GeoServerTileLayer.class);
        GeoServerTileLayerInfo info = mock(GeoServerTileLayerInfo.class);
        when(layer.getInfo()).thenReturn(info);
        when(info.getName()).thenReturn(name);
        return layer;
    }

    private <T> T event(Class<T> eventType, ConfigInfoType objectType, String objectId, String objectName) {
        T event = mock(eventType);
        org.geoserver.cloud.event.info.InfoEvent infoEvent = (org.geoserver.cloud.event.info.InfoEvent) event;
        when(infoEvent.getObjectType()).thenReturn(objectType);
        when(infoEvent.getObjectId()).thenReturn(objectId);
        when(infoEvent.getObjectName()).thenReturn(objectName);
        return event;
    }
}
