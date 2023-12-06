/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.integration.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.IOException;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogConformanceTest;
import org.geotools.ows.wmts.WebMapTileServer;
import org.geotools.ows.wmts.model.WMTSCapabilities;
import org.geotools.ows.wmts.model.WMTSLayer;
import org.junit.jupiter.api.Test;
import org.geotools.api.util.ProgressListener;

public abstract class AbstractCatalogBackendIT extends CatalogConformanceTest {

    final String LIVE_WMTS_GETCAPS_URL =
            "https://wmts.geo.admin.ch/EPSG/3857/1.0.0/WMTSCapabilities.xml";

    @Test
    void onlineWMTS_addStore() throws IOException {
        final WorkspaceInfo ws = addWorkspace();
        WMTSStoreInfo store = addWMTSStore(ws, LIVE_WMTS_GETCAPS_URL);
        assertNotNull(store);
        WebMapTileServer wmts = store.getWebMapTileServer((ProgressListener) null);
        assertNotNull(wmts);
        WMTSCapabilities capabilities = wmts.getCapabilities();
        assertNotNull(capabilities);
        List<WMTSLayer> layers = capabilities.getLayerList();
        assertNotNull(layers);
        assertFalse(layers.isEmpty());
    }

    @Test
    void onlineWMTS_addResource() throws IOException {
        final WorkspaceInfo ws = addWorkspace();
        final NamespaceInfo ns = addNamespace();
        final WMTSStoreInfo store = addWMTSStore(ws, LIVE_WMTS_GETCAPS_URL);
        final WebMapTileServer wmts = store.getWebMapTileServer((ProgressListener) null);
        final WMTSCapabilities capabilities = wmts.getCapabilities();
        final List<WMTSLayer> layers = capabilities.getLayerList();
        final WMTSLayer wmtsLayer = layers.get(0);

        final String layerName = wmtsLayer.getName();
        final WMTSLayerInfo resource = addWMTSLayer(ns, store, layerName);
        assertNotNull(resource);
        assertNotNull(resource.getCatalog());
        assertEquals(ns, resource.getNamespace());
        assertEquals(layerName, resource.getName());
        assertEquals(layerName, resource.getNativeName());

        final Catalog catalog = super.catalog;
        WMTSLayerInfo resourceByName =
                catalog.getResourceByName(ns, layerName, WMTSLayerInfo.class);
        assertEquals(resource, resourceByName);
        WMTSLayerInfo resourceByStore =
                catalog.getResourceByStore(store, layerName, WMTSLayerInfo.class);
        assertEquals(resource, resourceByStore);
    }

    @Test
    void onlineWMTS_addLayer() throws IOException {
        final WorkspaceInfo ws = addWorkspace();
        final NamespaceInfo ns = addNamespace();
        final WMTSStoreInfo store = addWMTSStore(ws, LIVE_WMTS_GETCAPS_URL);
        final WebMapTileServer wmts = store.getWebMapTileServer((ProgressListener) null);
        final WMTSCapabilities capabilities = wmts.getCapabilities();
        final List<WMTSLayer> layers = capabilities.getLayerList();
        final WMTSLayer wmtsLayer = layers.get(0);

        final String layerName = wmtsLayer.getName();
        final WMTSLayerInfo resource = addWMTSLayer(ns, store, layerName);

        final Catalog catalog = super.catalog;
        LayerInfo layer = catalog.getFactory().createLayer();
        layer.setResource(resource);
        layer.setName(layerName);

        LayerInfo layerInfo = add(layer, catalog::add, catalog::getLayer);
        assertNotNull(layerInfo);
        assertEquals(resource, layerInfo.getResource());
    }

    protected WMTSStoreInfo addWMTSStore(WorkspaceInfo workspace, String getCapsUrl) {
        WMTSStoreInfo store = catalog.getFactory().createWebMapTileServer();
        store.setWorkspace(workspace);
        store.setName("wmtsstore");
        store.setCapabilitiesURL(getCapsUrl);
        store.setUseConnectionPooling(true);
        return add(store, catalog::add, id -> catalog.getStore(id, WMTSStoreInfo.class));
    }

    protected WMTSLayerInfo addWMTSLayer(NamespaceInfo ns, WMTSStoreInfo wmts, String layerName) {
        WMTSLayerInfo resource = catalog.getFactory().createWMTSLayer();
        resource.setName(layerName);
        resource.setStore(wmts);
        resource.setNamespace(ns);
        resource.setEnabled(true);
        return add(
                resource, catalog::add, id -> catalog.getResource(id, WMTSLayerInfo.class));
    }
}
