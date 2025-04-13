/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.backend.pgconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.faker.CatalogFaker;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.config.GWCConfigPersister;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ows.LocalWorkspace;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @since 1.7
 */
@Testcontainers(disabledWithoutDocker = true)
class PgconfigTileLayerCatalogIT {

    @Container
    static PgConfigTestContainer<?> container = new PgConfigTestContainer<>();

    private TileLayerMocking support;

    private PgconfigTileLayerCatalog tlCatalog;

    @BeforeEach
    void setUp() {
        container.setUp();
        PgconfigBackendBuilder backendBuilder = new PgconfigBackendBuilder(container.getDataSource());
        CatalogPlugin catalog = backendBuilder.createCatalog();
        GeoServerImpl geoServer = backendBuilder.createGeoServer(catalog);
        support = new TileLayerMocking(catalog, geoServer);
        GridSetBroker gridsets = support.getGridsets();
        GWCConfigPersister defaultsProvider = mock(GWCConfigPersister.class);
        GWCConfig defaults = new GWCConfig();
        when(defaultsProvider.getConfig()).thenReturn(defaults);
        tlCatalog = new PgconfigTileLayerCatalog(container.getDataSource(), gridsets, () -> catalog, defaultsProvider);
    }

    @AfterEach
    void cleanDb() {
        container.tearDown();
        LocalWorkspace.remove();
    }

    @Test
    void testSimpleLayer() {
        var layerInfo = support.layerInfo();

        assertThat(tlCatalog.getLayer(layerInfo.prefixedName())).isEmpty();

        var tl = support.geoServerTileLayer(layerInfo);
        tlCatalog.addLayer(tl);
        tlCatalog.getLayer(layerInfo.prefixedName());

        Optional<TileLayer> result = tlCatalog.getLayer(layerInfo.prefixedName());
        assertThat(result).isPresent().get().isInstanceOf(GeoServerTileLayer.class);
        GeoServerTileLayer gsl = (GeoServerTileLayer) result.orElseThrow();
        PublishedInfo publishedInfo = gsl.getPublishedInfo();
        assertThat(publishedInfo.getId()).isEqualTo(layerInfo.getId());

        LocalWorkspace.set(support.workspace("local"));
        assertThat(tlCatalog.getLayer(layerInfo.prefixedName())).isEmpty();

        LocalWorkspace.set(layerInfo.getResource().getStore().getWorkspace());
        assertThat(tlCatalog.getLayer(layerInfo.prefixedName())).isPresent();

        String unprefixedName = support.layerName(layerInfo);
        assertThat(tlCatalog.getLayer(unprefixedName)).isPresent();

        LocalWorkspace.remove();
        gsl.setBlobStoreId("newBlobStore");
        gsl.setEnabled(false);
        assertThat(gsl.getGridSubsets()).hasSize(2);
        gsl.removeGridSubset(gsl.getGridSubsets().iterator().next());
        tlCatalog.modifyLayer(gsl);

        GeoServerTileLayer modified = tlCatalog
                .getLayer(layerInfo.prefixedName())
                .map(GeoServerTileLayer.class::cast)
                .orElseThrow();
        assertThat(modified.getBlobStoreId()).isEqualTo("newBlobStore");
        assertThat(modified.isEnabled()).isFalse();
        assertThat(modified.getGridSubsets()).hasSize(1);

        Catalog catalog = support.getFaker().catalog();
        final String oldName = layerInfo.prefixedName();

        ResourceInfo resource = catalog.getResource(layerInfo.getResource().getId(), ResourceInfo.class);
        resource.setName("newName");
        catalog.save(resource);

        final String newName = resource.prefixedName();

        layerInfo = catalog.getLayer(layerInfo.getId());
        assertThat(layerInfo.prefixedName()).isEqualTo(newName);

        assertThat(tlCatalog.containsLayer(oldName)).isFalse();
        assertThat(tlCatalog.containsLayer(newName)).isTrue();

        assertThat(tlCatalog.getLayer(oldName)).isEmpty();
        assertThat(tlCatalog.getLayer(newName)).isPresent();
        tlCatalog.removeLayer(unprefixedName);
    }

    @Test
    void testMultipleLayers() {
        assertThat(tlCatalog.getLayerNames()).isEmpty();

        WorkspaceInfo ws1 = support.workspace("ws1");
        WorkspaceInfo ws2 = support.workspace("ws2");
        WorkspaceInfo ws3 = support.workspace("ws3");

        final int count = 100;
        List<String> ws1Layers = addLayers(ws1, count);
        assertThat(ws1Layers).hasSize(count);
        List<String> ws2Layers = addLayers(ws2, count);
        assertThat(ws2Layers).hasSize(count);
        List<String> ws3Layers = addLayers(ws3, count);
        assertThat(ws3Layers).hasSize(count);

        List<String> allNames = new ArrayList<>(ws1Layers);
        allNames.addAll(ws2Layers);
        allNames.addAll(ws3Layers);

        assertThat(tlCatalog.getLayerCount()).isEqualTo(allNames.size());

        Set<String> layernames = tlCatalog.getLayerNames();
        assertThat(layernames).hasSameSizeAs(allNames).containsAll(allNames);
        for (String name : allNames) {
            assertThat(tlCatalog.containsLayer(name)).isTrue();
        }

        List<GeoServerTileLayer> layers = tlCatalog.getLayers();
        assertThat(layers).hasSameSizeAs(allNames);
    }

    private List<String> addLayers(WorkspaceInfo workspace, int count) {

        CatalogFaker faker = support.getFaker();
        DataStoreInfo ds = faker.dataStoreInfo(workspace);
        faker.catalog().add(ds);

        StyleInfo defaultStyle = faker.styleInfo();
        faker.catalog().add(defaultStyle);

        return IntStream.range(0, count)
                .parallel()
                .mapToObj(i -> {
                    String name = faker.name() + "-" + i;
                    FeatureTypeInfo featureType = faker.featureTypeInfo(ds, name);
                    faker.catalog().add(featureType);

                    LayerInfo layer = faker.layerInfo(featureType, defaultStyle);
                    faker.catalog().add(layer);

                    return support.geoServerTileLayer(layer);
                })
                .peek(tlCatalog::addLayer)
                .map(GeoServerTileLayer::getName)
                .toList();
    }
}
