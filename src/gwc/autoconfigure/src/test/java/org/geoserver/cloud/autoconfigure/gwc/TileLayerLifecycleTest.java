/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geowebcache.grid.GridSetBroker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * Tile layers must follow the lifecycle of the {@code PublishedInfo} they are configured for: removing the layer must
 * remove its stored tile layer configuration, or a client that recreates a layer by delete-then-create leaves a
 * configuration behind, tied to the id of a layer that no longer exists.
 */
class TileLayerLifecycleTest {

    private static final String WORKSPACE = "lifecycle";
    private static final String LAYER_NAME = "lifecycle:roads";

    private WebApplicationContextRunner runner;

    @BeforeEach
    void setUp(@TempDir File tmpDir) {
        runner = GeoWebCacheContextRunner.newMinimalGeoWebCacheContextRunner(tmpDir);
    }

    @Test
    void removingALayerRemovesItsStoredTileLayerConfiguration() {
        runner.run(context -> {
            GeoServerExtensionsHelper.init(context);

            Catalog catalog = context.getBean("rawCatalog", Catalog.class);
            LayerInfo layer = addCascadedLayer(catalog);
            addTileLayer(context, layer);

            TileLayerCatalog tileLayerCatalog = tileLayerCatalog(context);
            assertThat(tileLayerCatalog.getLayerById(layer.getId())).isNotNull();

            catalog.remove(catalog.getLayer(layer.getId()));

            assertThat(tileLayerCatalog.getLayerById(layer.getId()))
                    .as("the stored tile layer configuration must be gone, not just hidden")
                    .isNull();
            assertThat(tileLayerCatalog.getLayerNames()).doesNotContain(LAYER_NAME);
        });
    }

    /**
     * Mirrors a deployment where automatic tile layer creation is off and clients configure tile layers explicitly, as
     * the automatic creation on layer add would hide a removal that only works for freshly created configurations.
     */
    private void addTileLayer(AssertableWebApplicationContext context, LayerInfo layer) {
        GWC mediator = GWC.get();
        mediator.getConfig().setCacheLayersByDefault(false);
        GridSetBroker gridSetBroker = context.getBean(GridSetBroker.class);
        mediator.add(new GeoServerTileLayer(layer, mediator.getConfig(), gridSetBroker));
    }

    private TileLayerCatalog tileLayerCatalog(AssertableWebApplicationContext context) {
        return context.getBean("GeoSeverTileLayerCatalog", TileLayerCatalog.class);
    }

    /**
     * Adds a cascaded WMTS layer, the kind of layer a delete-then-create client recreates, and the one a tile layer can
     * be configured for without a live store connection.
     */
    private LayerInfo addCascadedLayer(Catalog catalog) {
        CatalogFactory factory = catalog.getFactory();

        WorkspaceInfo workspace = factory.createWorkspace();
        workspace.setName(WORKSPACE);
        catalog.add(workspace);

        NamespaceInfo namespace = factory.createNamespace();
        namespace.setPrefix(WORKSPACE);
        namespace.setURI("http://%s.test".formatted(WORKSPACE));
        catalog.add(namespace);

        WMTSStoreInfo store = factory.createWebMapTileServer();
        store.setWorkspace(catalog.getWorkspace(workspace.getId()));
        store.setName("roadsstore");
        store.setCapabilitiesURL("http://%s.test/wmts?request=GetCapabilities".formatted(WORKSPACE));
        store.setEnabled(true);
        catalog.add(store);

        WMTSLayerInfo resource = factory.createWMTSLayer();
        resource.setStore(catalog.getStore(store.getId(), WMTSStoreInfo.class));
        resource.setNamespace(catalog.getNamespace(namespace.getId()));
        resource.setName("roads");
        resource.setNativeName("roads");
        resource.setSRS("EPSG:4326");
        ReferencedEnvelope world = new ReferencedEnvelope(-180, 180, -90, 90, DefaultGeographicCRS.WGS84);
        resource.setNativeBoundingBox(world);
        resource.setLatLonBoundingBox(world);
        resource.setEnabled(true);
        catalog.add(resource);

        LayerInfo layer = factory.createLayer();
        layer.setResource(catalog.getResource(resource.getId(), WMTSLayerInfo.class));
        layer.setType(PublishedType.WMTS);
        catalog.add(layer);
        return catalog.getLayer(layer.getId());
    }
}
