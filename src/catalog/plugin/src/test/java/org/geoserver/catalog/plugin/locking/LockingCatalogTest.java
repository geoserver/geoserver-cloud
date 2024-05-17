/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.locking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.faker.CatalogFaker;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.config.GeoServer;
import org.geoserver.config.plugin.GeoServerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @since 1.0
 */
class LockingCatalogTest {

    private GeoServerConfigurationLock configLock;
    private CatalogPlugin catalog;
    private CatalogFaker faker;

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() {
        configLock = createConfigLock();
        catalog = new LockingCatalog(configLock);
        GeoServer geoserver = new GeoServerImpl();
        geoserver.setCatalog(catalog);
        faker = new CatalogFaker(catalog, geoserver);
    }

    protected GeoServerConfigurationLock createConfigLock() {
        return new GeoServerConfigurationLock();
    }

    @Test
    void testConfigLockIsReentrant() {
        configLock.lock(LockType.WRITE);
        assertEquals(LockType.WRITE, configLock.getCurrentLock());
        configLock.unlock();
        assertNull(configLock.getCurrentLock());

        // lock twice
        configLock.lock(LockType.WRITE);
        assertEquals(LockType.WRITE, configLock.getCurrentLock());

        configLock.lock(LockType.WRITE);
        assertEquals(LockType.WRITE, configLock.getCurrentLock());

        // first unlock, shall still hold the lock
        configLock.unlock();
        assertEquals(LockType.WRITE, configLock.getCurrentLock());

        // second unlock, must actually release the lock
        configLock.unlock();
        assertNull(configLock.getCurrentLock());
    }

    @Test
    void simpleMultiThreadTest() {
        List<WorkspaceInfo> workspaces =
                IntStream.range(0, 100).parallel().mapToObj(i -> faker.workspaceInfo()).toList();

        workspaces.stream().forEach(catalog::add);

        List<WorkspaceInfo> catalogs = catalog.getWorkspaces();
        assertEquals(workspaces.size(), catalogs.size());
        assertNotNull(catalog.getDefaultWorkspace());
        assertEquals(Set.copyOf(workspaces), Set.copyOf(catalogs));
    }

    @Test
    void preAcquiredLockMultiThreadTest() {
        List<WorkspaceInfo> workspaces =
                IntStream.range(0, 100)
                        .parallel()
                        .mapToObj(i -> faker.workspaceInfo("ws-" + i))
                        .toList();

        List<List<CatalogInfo>> created =
                workspaces //
                        .stream() //
                        .parallel() //
                        .map(this::batchCreateSubTree)
                        .toList();

        assertNotNull(catalog.getDefaultWorkspace());

        assertThat(catalog.getLayers()).size().isEqualTo(workspaces.size());
        assertThat(catalog.getStyles()).size().isEqualTo(workspaces.size());
        assertThat(catalog.getFeatureTypes()).size().isEqualTo(workspaces.size());
        assertThat(catalog.getDataStores()).size().isEqualTo(workspaces.size());
        assertThat(catalog.getNamespaces()).size().isEqualTo(workspaces.size());
        assertThat(catalog.getWorkspaces()).size().isEqualTo(workspaces.size());

        created //
                .stream() //
                .parallel() //
                .forEach(this::batchUpdate);

        Stream<List<? extends CatalogInfo>> updated =
                created //
                        .stream()
                        .map(
                                list -> {
                                    List<? extends CatalogInfo> sublist =
                                            list.stream()
                                                    .map(CatalogInfo::getId)
                                                    .map(catalog::findById)
                                                    .map(Optional::orElseThrow)
                                                    .toList();
                                    return sublist;
                                });

        updated //
                .parallel() //
                .forEach(this::batchDelete);

        assertThat(catalog.getLayers()).isEmpty();
        assertThat(catalog.getStyles()).isEmpty();
        assertThat(catalog.getFeatureTypes()).isEmpty();
        assertThat(catalog.getDataStores()).isEmpty();
        assertThat(catalog.getNamespaces()).isEmpty();
        assertThat(catalog.getWorkspaces()).isEmpty();
    }

    private void batchUpdate(List<? extends CatalogInfo> infos) {
        configLock.lock(LockType.WRITE);
        try {
            for (CatalogInfo info : infos) {
                if (info instanceof WorkspaceInfo ws) ws.setName(faker.name());
                else if (info instanceof NamespaceInfo ns) ns.setURI(faker.url());
                else if (info instanceof DataStoreInfo ds) {
                    ds.setName(faker.name());
                    ds.getConnectionParameters().put("someparam", "somevalue");
                    ds.getMetadata().put("somekey", "key value");
                } else if (info instanceof FeatureTypeInfo ft) ft.setName(faker.name());
                else if (info instanceof LayerInfo l) l.setAdvertised(false);
                else if (info instanceof StyleInfo s) {
                    s.setDateModified(new Date());
                    s.getMetadata().put("somekey", "key value");
                } else {
                    throw new IllegalStateException("Unexpected catalog info type " + info);
                }
                catalog.save(info);
            }
        } finally {
            assertEquals(
                    LockType.WRITE, configLock.getCurrentLock(), "lock should have been upgraded");
            configLock.unlock();
        }
    }

    private void batchDelete(List<? extends CatalogInfo> infos) {
        configLock.lock(LockType.WRITE);
        try {
            WorkspaceInfo ws =
                    infos.stream()
                            .filter(WorkspaceInfo.class::isInstance)
                            .map(WorkspaceInfo.class::cast)
                            .findFirst()
                            .orElseThrow();

            CascadeDeleteVisitor cascadeDeleteVisitor = new CascadeDeleteVisitor(catalog);
            ws.accept(cascadeDeleteVisitor);

            infos.stream().forEach(catalog::remove);
        } finally {
            assertEquals(
                    LockType.WRITE, configLock.getCurrentLock(), "lock should have been upgraded");
            configLock.unlock();
        }
    }

    private List<CatalogInfo> batchCreateSubTree(WorkspaceInfo workspace) {
        configLock.lock(LockType.WRITE);
        try {
            catalog.add(workspace);
            workspace = catalog.getWorkspace(workspace.getId());
            NamespaceInfo namespace = faker.namespace(workspace.getName());
            catalog.add(namespace);
            namespace = catalog.getNamespace(namespace.getId());

            DataStoreInfo ds = faker.dataStoreInfo(workspace);
            catalog.add(ds);
            ds = catalog.getDataStore(ds.getId());

            FeatureTypeInfo ft = faker.featureTypeInfo(ds);
            catalog.add(ft);
            ft = catalog.getFeatureType(ft.getId());

            StyleInfo style = faker.styleInfo();
            catalog.add(style);
            style = catalog.getStyle(style.getId());

            LayerInfo layer = faker.layerInfo(ft, style);
            catalog.add(layer);
            layer = catalog.getLayer(layer.getId());

            return List.of(workspace, namespace, ds, ft, style, layer);
        } finally {
            assertEquals(
                    LockType.WRITE, configLock.getCurrentLock(), "lock should have been upgraded");
            configLock.unlock();
        }
    }
}
