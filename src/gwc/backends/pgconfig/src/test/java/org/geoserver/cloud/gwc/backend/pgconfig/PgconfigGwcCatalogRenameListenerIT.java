/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.backend.pgconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.faker.CatalogFaker;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.GWCSynchEnv;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.config.GWCConfigPersister;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies the full pgconfig-catalog rename propagation chain ends with a {@link GWC#layerRenamed}
 * (and {@link GWC#layerRemoved}) call against a mocked GWC mediator, without standing up the full
 * GWC stack. The catalog and SQL triggers are real (Postgres testcontainer), so this covers the
 * order-of-operations between the in-process event firing and the database-side {@code
 * publishedinfos_mat} refresh that broke upstream's path.
 *
 * @since 2.28.3.1
 */
@Testcontainers(disabledWithoutDocker = true)
class PgconfigGwcCatalogRenameListenerIT {

    @Container
    static PgConfigTestContainer<?> container = new PgConfigTestContainer<>();

    private CatalogPlugin catalog;
    private CatalogFaker faker;
    private PgconfigGwcCatalogRenameListener listener;
    private PgconfigTileLayerCatalog tlCatalog;
    private GWC mediator;

    @BeforeEach
    void setUp() {
        container.setUp();
        PgconfigBackendBuilder backendBuilder = new PgconfigBackendBuilder(container.getDataSource());
        catalog = backendBuilder.createCatalog();
        GeoServerImpl geoServer = backendBuilder.createGeoServer(catalog);
        faker = new CatalogFaker(catalog, geoServer);

        listener = new PgconfigGwcCatalogRenameListener(catalog);
        listener.register();

        GWCConfigPersister defaultsProvider = mock(GWCConfigPersister.class);
        when(defaultsProvider.getConfig()).thenReturn(new GWCConfig());
        TileLayerMocking support = new TileLayerMocking(catalog, geoServer);
        tlCatalog = new PgconfigTileLayerCatalog(
                container.getDataSource(), support.getGridsets(), () -> catalog, defaultsProvider);

        mediator = mock(GWC.class);
        GWC.set(mediator, mock(GWCSynchEnv.class));
    }

    @AfterEach
    void tearDown() {
        try {
            listener.unregister();
        } finally {
            GWC.set(null, null);
            container.tearDown();
        }
    }

    @Test
    void workspaceRename_firesLayerRenamedForEachLayerInWorkspace() {
        WorkspaceInfo ws = addWorkspace("oldWs");
        LayerInfo layer1 = addLayer(ws, "states");
        LayerInfo layer2 = addLayer(ws, "roads");

        assertThat(layer1.prefixedName()).isEqualTo("oldWs:states");
        assertThat(layer2.prefixedName()).isEqualTo("oldWs:roads");

        renameWorkspace(ws, "newWs");

        verify(mediator).layerRenamed("oldWs:states", "newWs:states");
        verify(mediator).layerRenamed("oldWs:roads", "newWs:roads");
    }

    @Test
    void workspaceRename_firesLayerRenamedForLayerGroups() {
        WorkspaceInfo ws = addWorkspace("oldWs");
        LayerInfo layer = addLayer(ws, "states");
        LayerGroupInfo group = addLayerGroup(ws, "grp", layer);

        assertThat(group.prefixedName()).isEqualTo("oldWs:grp");

        renameWorkspace(ws, "newWs");

        verify(mediator).layerRenamed("oldWs:grp", "newWs:grp");
    }

    @Test
    void resourceRename_firesLayerRenamed() {
        WorkspaceInfo ws = addWorkspace("topp");
        LayerInfo layer = addLayer(ws, "states");

        FeatureTypeInfo resource = (FeatureTypeInfo) layer.getResource();
        resource.setName("roads");
        catalog.save(resource);

        verify(mediator).layerRenamed("topp:states", "topp:roads");
    }

    @Test
    void layerGroupRename_firesLayerRenamed() {
        WorkspaceInfo ws = addWorkspace("topp");
        LayerInfo layer = addLayer(ws, "states");
        LayerGroupInfo group = addLayerGroup(ws, "groupOld", layer);

        group = catalog.getLayerGroup(group.getId());
        group.setName("groupNew");
        catalog.save(group);

        verify(mediator).layerRenamed("topp:groupOld", "topp:groupNew");
    }

    @Test
    void unrelatedModify_doesNotFire() {
        WorkspaceInfo ws = addWorkspace("topp");
        addLayer(ws, "states");

        // bump the workspace's "isolated" attribute (a non-name change)
        ws = catalog.getWorkspace(ws.getId());
        ws.setIsolated(!ws.isIsolated());
        catalog.save(ws);

        verify(mediator, never())
                .layerRenamed(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void removeLayer_firesLayerRemoved() {
        WorkspaceInfo ws = addWorkspace("topp");
        LayerInfo layer = addLayer(ws, "states");
        tlCatalog.addLayer(new TileLayerMocking(catalog, mock(GeoServerImpl.class)).geoServerTileLayer(layer));

        tlCatalog.removeLayer(layer.prefixedName());

        verify(mediator).layerRemoved("topp:states");
    }

    private WorkspaceInfo addWorkspace(String name) {
        WorkspaceInfo ws = faker.workspaceInfo(name);
        NamespaceInfo ns = faker.namespace(name);
        catalog.add(ws);
        catalog.add(ns);
        return catalog.getWorkspaceByName(name);
    }

    private LayerInfo addLayer(WorkspaceInfo ws, String layerName) {
        DataStoreInfo ds = faker.dataStoreInfo(ws);
        catalog.add(ds);

        StyleInfo style = faker.styleInfo();
        catalog.add(style);

        FeatureTypeInfo featureType = faker.featureTypeInfo(ds, layerName);
        catalog.add(featureType);

        LayerInfo layer = faker.layerInfo(featureType, style);
        catalog.add(layer);
        return catalog.getLayer(layer.getId());
    }

    private LayerGroupInfo addLayerGroup(WorkspaceInfo ws, String name, LayerInfo... layers) {
        LayerGroupInfo group = faker.layerGroupInfo(ws);
        group.setName(name);
        for (LayerInfo layer : layers) {
            group.getLayers().add(layer);
        }
        catalog.add(group);
        return catalog.getLayerGroup(group.getId());
    }

    private void renameWorkspace(WorkspaceInfo ws, String newName) {
        WorkspaceInfo persisted = catalog.getWorkspace(ws.getId());
        persisted.setName(newName);
        catalog.save(persisted);
    }
}
