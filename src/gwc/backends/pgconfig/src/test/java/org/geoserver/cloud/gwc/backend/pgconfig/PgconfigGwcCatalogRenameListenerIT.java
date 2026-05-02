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

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.faker.CatalogFaker;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.GWCSynchEnv;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.config.GWCConfigPersister;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies the full pgconfig-catalog rename propagation chain ends with a {@link GWC#layerRenamed} (and
 * {@link GWC#layerRemoved}) call against a mocked GWC mediator, without standing up the full GWC stack. The catalog and
 * SQL triggers are real (Postgres testcontainer), so this covers the order-of-operations between the in-process event
 * firing and the database-side {@code publishedinfos_mat} refresh that broke upstream's path.
 *
 * <p>Runs single-threaded because {@link GWC#set(GWC, GWCSynchEnv)} installs a process-wide singleton.
 */
@Testcontainers(disabledWithoutDocker = true)
@Execution(value = ExecutionMode.SAME_THREAD)
class PgconfigGwcCatalogRenameListenerIT {

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    private CatalogPlugin catalog;
    private CatalogFaker faker;
    private PgconfigGwcCatalogRenameListener listener;
    private PgconfigTileLayerCatalog tlCatalog;
    private TileLayerMocking support;
    private GWC mediator;

    @BeforeEach
    void setUp() {
        PgconfigBackendBuilder backendBuilder = new PgconfigBackendBuilder(db.getDataSource());
        catalog = backendBuilder.createCatalog();
        GeoServerImpl geoServer = backendBuilder.createGeoServer(catalog);
        support = new TileLayerMocking(catalog, geoServer);
        faker = support.getFaker();

        listener = new PgconfigGwcCatalogRenameListener(catalog);
        listener.register();

        GWCConfigPersister defaultsProvider = mock(GWCConfigPersister.class);
        when(defaultsProvider.getConfig()).thenReturn(new GWCConfig());
        tlCatalog = new PgconfigTileLayerCatalog(
                db.getDataSource(), support.getGridsets(), () -> catalog, defaultsProvider);

        mediator = mock(GWC.class);
        GWC.set(mediator, mock(GWCSynchEnv.class));
    }

    @AfterEach
    void tearDown() {
        try {
            listener.unregister();
        } finally {
            GWC.set(null, null);
        }
    }

    @Test
    void workspaceRename_firesLayerRenamedForEachLayerInWorkspace() {
        WorkspaceInfo ws = support.workspace("oldWs");
        LayerInfo layer1 = support.layerInfo(ws, "states");
        LayerInfo layer2 = support.layerInfo(ws, "roads");

        assertThat(layer1.prefixedName()).isEqualTo("oldWs:states");
        assertThat(layer2.prefixedName()).isEqualTo("oldWs:roads");

        renameWorkspace(ws, "newWs");

        verify(mediator).layerRenamed("oldWs:states", "newWs:states");
        verify(mediator).layerRenamed("oldWs:roads", "newWs:roads");
    }

    @Test
    void workspaceRename_firesLayerRenamedForLayerGroups() {
        WorkspaceInfo ws = support.workspace("oldWs");
        LayerInfo layer = support.layerInfo(ws, "states");
        LayerGroupInfo group = addLayerGroup(ws, "grp", layer);

        assertThat(group.prefixedName()).isEqualTo("oldWs:grp");

        renameWorkspace(ws, "newWs");

        verify(mediator).layerRenamed("oldWs:grp", "newWs:grp");
    }

    @Test
    void resourceRename_firesLayerRenamed() {
        WorkspaceInfo ws = support.workspace("topp");
        LayerInfo layer = support.layerInfo(ws, "states");

        FeatureTypeInfo resource = catalog.getResource(layer.getResource().getId(), FeatureTypeInfo.class);
        resource.setName("roads");
        catalog.save(resource);

        verify(mediator).layerRenamed("topp:states", "topp:roads");
    }

    @Test
    void layerGroupRename_firesLayerRenamed() {
        WorkspaceInfo ws = support.workspace("topp");
        LayerInfo layer = support.layerInfo(ws, "states");
        LayerGroupInfo group = addLayerGroup(ws, "groupOld", layer);

        group = catalog.getLayerGroup(group.getId());
        group.setName("groupNew");
        catalog.save(group);

        verify(mediator).layerRenamed("topp:groupOld", "topp:groupNew");
    }

    @Test
    void unrelatedModify_doesNotFire() {
        WorkspaceInfo ws = support.workspace("topp");
        support.layerInfo(ws, "states");

        ws = catalog.getWorkspace(ws.getId());
        ws.setIsolated(!ws.isIsolated());
        catalog.save(ws);

        verify(mediator, never())
                .layerRenamed(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void removeLayer_firesLayerRemoved() {
        WorkspaceInfo ws = support.workspace("topp");
        LayerInfo layer = support.layerInfo(ws, "states");
        tlCatalog.addLayer(support.geoServerTileLayer(layer));

        tlCatalog.removeLayer(layer.prefixedName());

        verify(mediator).layerRemoved("topp:states");
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
