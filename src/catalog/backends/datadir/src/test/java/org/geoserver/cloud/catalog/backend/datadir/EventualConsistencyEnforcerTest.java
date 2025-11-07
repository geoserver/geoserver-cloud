/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.catalog.backend.datadir;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.config.GeoServer;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventualConsistencyEnforcerTest {

    private Catalog catalog;
    private CatalogTestData data;
    private GeoServer geoserver;

    private EventualConsistencyEnforcer enforcer;

    public static @BeforeAll void oneTimeSetup() {
        // avoid the chatty warning logs due to catalog looking up a bean of type GeoServerConfigurationLock
        GeoServerExtensionsHelper.setIsSpringContext(false);
    }

    public @BeforeEach void before() {
        catalog = new CatalogPlugin();
        geoserver = new GeoServerImpl();
        geoserver.setCatalog(catalog);

        // empty() to not initialize catalog with test data
        data = CatalogTestData.empty(() -> catalog, () -> geoserver).initialize();

        enforcer = new EventualConsistencyEnforcer();
        ExtendedCatalogFacade rawCatalogFacade = (ExtendedCatalogFacade) ((CatalogPlugin) catalog).getRawFacade();
        enforcer.setRawFacade(rawCatalogFacade);
    }

    @SuppressWarnings("unchecked")
    private <T extends CatalogInfo> T resolvingProxy(T info) {
        String id = requireNonNull(info.getId());
        Class<? extends Info> type = ConfigInfoType.valueOf(info).getType();
        return (T) ResolvingProxy.create(id, type);
    }

    @Test
    void testOnDispose() {
        // Create pending operations
        WorkspaceInfo ws = data.workspaceA;
        DataStoreInfo ds = data.dataStoreA;
        WorkspaceInfo unresolved = resolvingProxy(ws);
        ds.setWorkspace(unresolved);

        enforcer.add(ds);
        assertThat(enforcer.isConverged()).isFalse();

        // Dispose should clear pending operations
        enforcer.onDispose();
        assertThat(enforcer.isConverged()).isTrue();

        // Adding the workspace now should not resolve the store (operation was discarded)
        enforcer.add(ws);
        assertThat(catalog.getStore(requireNonNull(ds.getId()), StoreInfo.class))
                .isNull();
    }

    @Test
    void testBeforeReload() {
        // Create pending operations
        WorkspaceInfo ws = data.workspaceA;
        DataStoreInfo ds = data.dataStoreA;
        WorkspaceInfo unresolved = resolvingProxy(ws);
        ds.setWorkspace(unresolved);

        enforcer.add(ds);
        assertThat(enforcer.isConverged()).isFalse();

        // beforeReload should clear pending operations
        enforcer.beforeReload();
        assertThat(enforcer.isConverged()).isTrue();

        // Adding the workspace now should not resolve the store (operation was discarded)
        enforcer.add(ws);
        assertThat(catalog.getStore(requireNonNull(ds.getId()), StoreInfo.class))
                .isNull();
    }

    @Test
    void testForceResolve() {
        WorkspaceInfo ws = data.workspaceA;
        DataStoreInfo ds = data.dataStoreA;
        WorkspaceInfo unresolved = resolvingProxy(ws);
        ds.setWorkspace(unresolved);

        // Add store with missing workspace - will be deferred
        enforcer.add(ds);
        assertThat(enforcer.isConverged()).isFalse();
        assertThat(catalog.getStore(requireNonNull(ds.getId()), StoreInfo.class))
                .isNull();

        // Add workspace directly to catalog (bypassing enforcer)
        catalog.add(ws);

        // Store is still pending because enforcer hasn't been notified
        assertThat(enforcer.isConverged()).isFalse();
        assertThat(catalog.getStore(requireNonNull(ds.getId()), StoreInfo.class))
                .isNull();

        // Force resolve should retry pending operations
        enforcer.forceResolve();
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getStore(requireNonNull(ds.getId()), StoreInfo.class))
                .isNotNull();
    }

    @Test
    void testSetDefaultWorkspace() {
        WorkspaceInfo ws = data.workspaceA;

        // Setting default with unresolved workspace should defer
        WorkspaceInfo unresolved = resolvingProxy(ws);
        enforcer.setDefaultWorkspace(unresolved);
        assertThat(enforcer.isConverged()).isFalse();
        assertThat(catalog.getDefaultWorkspace()).isNull();

        // Adding workspace should resolve the operation
        enforcer.add(ws);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getDefaultWorkspace()).isNotNull();
        assertThat(catalog.getDefaultWorkspace().getId()).isEqualTo(ws.getId());
    }

    @Test
    void testSetDefaultWorkspace_WithNull() {
        WorkspaceInfo ws = data.workspaceA;

        // Set a default workspace first
        enforcer.add(ws);
        enforcer.setDefaultWorkspace(ws);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getDefaultWorkspace()).isNotNull();
        assertThat(catalog.getDefaultWorkspace().getId()).isEqualTo(ws.getId());

        // Clear the default workspace by passing null
        enforcer.setDefaultWorkspace(null);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getDefaultWorkspace()).isNull();
    }

    @Test
    void testAddWorkspace() {
        enforcer.add(data.workspaceA);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getWorkspace(data.workspaceA.getId())).isNotNull();
    }

    @Test
    void testAddNamespace() {
        enforcer.add(data.namespaceA);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getNamespace(data.namespaceA.getId())).isNotNull();
    }

    @Test
    void testAddStoreWithMissingWorkspace() {
        WorkspaceInfo ws = data.workspaceA;
        DataStoreInfo ds = data.dataStoreA;

        WorkspaceInfo unresolved = resolvingProxy(ws);
        ds.setWorkspace(unresolved);

        enforcer.add(ds);
        assertThat(enforcer.isConverged()).isFalse();
        assertThat(catalog.getStore(requireNonNull(ds.getId()), StoreInfo.class))
                .isNull();

        enforcer.add(ws);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getStore(requireNonNull(ds.getId()), StoreInfo.class))
                .isNotNull();
    }

    @Test
    void testUpdate_ObjectToUpdateIsUnresolved() {
        WorkspaceInfo ws = data.workspaceA;

        // Add workspace first
        enforcer.add(ws);
        assertThat(enforcer.isConverged()).isTrue();

        // Create update operation with unresolved proxy for the object
        WorkspaceInfo unresolved = resolvingProxy(ws);
        Patch patch = new Patch();
        patch.add("name", "newName");

        enforcer.update(unresolved, patch);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getWorkspace(ws.getId()).getName()).isEqualTo("newName");
    }

    @Test
    void testUpdate_PatchContainsResolvingProxy() {
        // This test verifies that updates with patches containing ResolvingProxy values are deferred
        // For simplicity, we test with a FeatureType that has a store reference
        WorkspaceInfo ws = data.workspaceA;
        NamespaceInfo ns = data.namespaceA;
        DataStoreInfo ds = data.dataStoreA;
        FeatureTypeInfo ft = data.featureTypeA;

        // Add workspace, namespace, store, and featureType. They're already tied to each other by the test data
        // initialization
        enforcer.add(ws);
        enforcer.add(ns);
        enforcer.add(ds);
        enforcer.add(ft);
        assertThat(enforcer.isConverged()).isTrue();

        // Retrieve the stored featureType
        FeatureTypeInfo storedFt = catalog.getFeatureType(ft.getId());
        assertThat(storedFt.getStore().getId()).isEqualTo(ds.getId());

        // Change the store's workspace to an unresolved one
        WorkspaceInfo ws2 = data.workspaceB;
        WorkspaceInfo unresolvedWs = resolvingProxy(ws2);

        Patch patch = new Patch();
        patch.add("workspace", unresolvedWs);

        // Update should be deferred
        enforcer.update(ds, patch);
        assertThat(enforcer.isConverged()).isFalse();

        enforcer.add(ws2);
        assertThat(enforcer.isConverged()).isTrue();
    }

    @Test
    void testRemove_WithUnresolvedProxy() {
        WorkspaceInfo ws = data.workspaceA;

        // Add workspace
        enforcer.add(ws);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getWorkspace(ws.getId())).isNotNull();

        // Remove with proxy should resolve and remove
        WorkspaceInfo unresolved = resolvingProxy(ws);
        enforcer.remove(unresolved);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getWorkspace(ws.getId())).isNull();
    }

    @Test
    void testRemove_CascadingDependentOperations() {
        WorkspaceInfo ws = data.workspaceA;
        DataStoreInfo ds = data.dataStoreA;

        // Create store with unresolved workspace
        WorkspaceInfo unresolvedWs = resolvingProxy(ws);
        ds.setWorkspace(unresolvedWs);

        // Add store - should be deferred
        enforcer.add(ds);
        assertThat(enforcer.isConverged()).isFalse();

        // Add workspace
        enforcer.add(ws);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getStore(ds.getId(), StoreInfo.class)).isNotNull();

        // Now add another store depending on the same workspace
        DataStoreInfo ds2 = data.dataStoreB;
        WorkspaceInfo unresolvedWs2 = resolvingProxy(ws);
        ds2.setWorkspace(unresolvedWs2);
        enforcer.add(ds2);

        // Remove the workspace - should complete or discard dependent operations
        enforcer.remove(ws);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getWorkspace(ws.getId())).isNull();
    }

    @Test
    void testRemove_DiscardsOperationWithMultipleMissingDependencies() {
        WorkspaceInfo ws = data.workspaceA;
        NamespaceInfo ns = data.namespaceA;
        DataStoreInfo ds = data.dataStoreA;
        FeatureTypeInfo ft = data.featureTypeA;
        StyleInfo style = data.style1;
        LayerInfo layer = data.layerFeatureTypeA;

        // Create layer with unresolved resource and style
        FeatureTypeInfo unresolvedFt = resolvingProxy(ft);
        StyleInfo unresolvedStyle = resolvingProxy(style);
        layer.setResource(unresolvedFt);
        layer.setDefaultStyle(unresolvedStyle);

        // Add layer - should be deferred waiting for both resource and style
        enforcer.add(layer);
        assertThat(enforcer.isConverged()).isFalse();
        assertThat(catalog.getLayer(layer.getId())).isNull();

        // Add the resource's dependencies and the resource itself
        enforcer.add(ws);
        enforcer.add(ns);
        enforcer.add(ds);
        enforcer.add(ft);

        // Layer is still pending (waiting for style)
        assertThat(enforcer.isConverged()).isFalse();
        assertThat(catalog.getLayer(layer.getId())).isNull();

        // Remove the resource before style arrives
        enforcer.remove(ft);
        assertThat(catalog.getFeatureType(ft.getId())).isNull();
        assertThat(catalog.getLayer(layer.getId())).isNull();

        // Adding the style should not add the layer (operation should have been discarded)
        enforcer.add(style);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getLayer(layer.getId())).isNull();
    }

    @Test
    void testSetDefaultNamespace() {
        NamespaceInfo ns = data.namespaceA;

        // Setting default with unresolved namespace should defer
        NamespaceInfo unresolved = resolvingProxy(ns);
        enforcer.setDefaultNamespace(unresolved);
        assertThat(enforcer.isConverged()).isFalse();
        assertThat(catalog.getDefaultNamespace()).isNull();

        // Adding namespace should resolve the operation
        enforcer.add(ns);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getDefaultNamespace()).isNotNull();
        assertThat(catalog.getDefaultNamespace().getId()).isEqualTo(ns.getId());
    }

    @Test
    void testSetDefaultNamespace_WithNull() {
        NamespaceInfo ns = data.namespaceA;

        // Set a default namespace first
        enforcer.add(ns);
        enforcer.setDefaultNamespace(ns);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getDefaultNamespace()).isNotNull();
        assertThat(catalog.getDefaultNamespace().getId()).isEqualTo(ns.getId());

        // Clear the default namespace by passing null
        enforcer.setDefaultNamespace(null);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getDefaultNamespace()).isNull();
    }

    @Test
    void testSetDefaultDataStore() {
        WorkspaceInfo ws = data.workspaceA;
        DataStoreInfo ds = data.dataStoreA;

        // Setting default with both unresolved should defer
        WorkspaceInfo unresolvedWs = resolvingProxy(ws);
        DataStoreInfo unresolvedDs = resolvingProxy(ds);

        enforcer.setDefaultDataStore(unresolvedWs, unresolvedDs);
        assertThat(enforcer.isConverged()).isFalse();
        assertThat(catalog.getDefaultDataStore(ws)).isNull();

        // Add workspace first
        enforcer.add(ws);
        assertThat(enforcer.isConverged()).isFalse();
        assertThat(catalog.getDefaultDataStore(ws)).isNull();

        // Add data store - should resolve
        ds.setWorkspace(ws);
        enforcer.add(ds);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getDefaultDataStore(ws)).isNotNull();
        assertThat(catalog.getDefaultDataStore(ws).getId()).isEqualTo(ds.getId());
    }

    @Test
    void testSetDefaultDataStore_WithNullStore() {
        WorkspaceInfo ws = data.workspaceA;

        // Setting default to null with unresolved workspace should defer
        WorkspaceInfo unresolvedWs = resolvingProxy(ws);
        enforcer.setDefaultDataStore(unresolvedWs, null);
        assertThat(enforcer.isConverged()).isFalse();

        // Adding workspace should resolve and clear default
        enforcer.add(ws);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getDefaultDataStore(ws)).isNull();
    }

    @Test
    void testMultipleDependentOperationsOnTheSameObject() {
        WorkspaceInfo ws = data.workspaceA;
        DataStoreInfo ds1 = data.dataStoreA;
        DataStoreInfo ds2 = data.dataStoreB;

        // Create two stores depending on the same workspace
        WorkspaceInfo unresolvedWs1 = resolvingProxy(ws);
        WorkspaceInfo unresolvedWs2 = resolvingProxy(ws);
        ds1.setWorkspace(unresolvedWs1);
        ds2.setWorkspace(unresolvedWs2);

        // Add both stores - both should be deferred
        enforcer.add(ds1);
        enforcer.add(ds2);
        assertThat(enforcer.isConverged()).isFalse();
        assertThat(catalog.getStore(ds1.getId(), StoreInfo.class)).isNull();
        assertThat(catalog.getStore(ds2.getId(), StoreInfo.class)).isNull();

        // Add workspace - both stores should resolve
        enforcer.add(ws);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getStore(ds1.getId(), StoreInfo.class)).isNotNull();
        assertThat(catalog.getStore(ds2.getId(), StoreInfo.class)).isNotNull();
    }

    @Test
    void testMultipleDependentOperations() {
        WorkspaceInfo ws1 = data.workspaceA;
        DataStoreInfo ds1 = data.dataStoreA;

        WorkspaceInfo ws2 = data.workspaceB;
        DataStoreInfo ds2 = data.dataStoreB;

        assertNotEquals(ws1.getId(), ws2.getId());

        // Create two stores depending on different workspaces
        WorkspaceInfo unresolvedWs1 = resolvingProxy(ws1);
        ds1.setWorkspace(unresolvedWs1);

        WorkspaceInfo unresolvedWs2 = resolvingProxy(ws2);
        ds2.setWorkspace(unresolvedWs2);

        // Add both stores - both should be deferred
        enforcer.add(ds1);
        enforcer.add(ds2);

        assertThat(enforcer.isConverged()).isFalse();
        assertThat(catalog.getStore(ds1.getId(), StoreInfo.class)).isNull();
        assertThat(catalog.getStore(ds2.getId(), StoreInfo.class)).isNull();

        enforcer.add(ws1);
        assertThat(enforcer.isConverged()).isFalse();
        assertThat(catalog.getStore(ds1.getId(), StoreInfo.class)).isNotNull();

        enforcer.add(ws2);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getStore(ds1.getId(), StoreInfo.class)).isNotNull();
        assertThat(catalog.getStore(ds2.getId(), StoreInfo.class)).isNotNull();
    }

    @Test
    void testAddObjectThatAlreadyExists() {
        WorkspaceInfo ws = data.workspaceA;

        // Add workspace normally
        enforcer.add(ws);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getWorkspace(ws.getId())).isNotNull();

        // Add same workspace again - should be silently ignored (idempotency)
        enforcer.add(ws);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getWorkspace(ws.getId())).isNotNull();
    }

    @Test
    void testAddLayerWithMissingStyle() {
        // Setup dependencies
        WorkspaceInfo ws = data.workspaceA;
        NamespaceInfo ns = data.namespaceA;
        DataStoreInfo ds = data.dataStoreA;
        FeatureTypeInfo ft = data.featureTypeA;
        StyleInfo style = data.style1;
        LayerInfo layer = data.layerFeatureTypeA;

        // Set layer's default style to unresolved proxy
        StyleInfo unresolvedStyle = resolvingProxy(style);
        layer.setDefaultStyle(unresolvedStyle);

        // Add all dependencies except the style
        enforcer.add(ws);
        enforcer.add(ns);
        enforcer.add(ds);
        enforcer.add(ft);

        // Add layer with missing style - should be deferred
        enforcer.add(layer);
        assertThat(enforcer.isConverged()).isFalse();
        assertThat(catalog.getLayer(layer.getId())).isNull();

        // Add the style - layer should now be added
        enforcer.add(style);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getLayer(layer.getId())).isNotNull();
        assertThat(catalog.getLayer(layer.getId()).getDefaultStyle().getId()).isEqualTo(style.getId());
    }

    @Test
    void testUpdateLayerWithMissingStyle() {
        // Setup and add all dependencies including the layer
        WorkspaceInfo ws = data.workspaceA;
        NamespaceInfo ns = data.namespaceA;
        DataStoreInfo ds = data.dataStoreA;
        FeatureTypeInfo ft = data.featureTypeA;
        StyleInfo style1 = data.style1;
        StyleInfo style2 = data.style2;
        LayerInfo layer = data.layerFeatureTypeA;

        enforcer.add(ws);
        enforcer.add(ns);
        enforcer.add(ds);
        enforcer.add(ft);
        enforcer.add(style1);
        enforcer.add(layer);
        assertThat(enforcer.isConverged()).isTrue();

        // Verify layer exists with style1
        LayerInfo storedLayer = catalog.getLayer(layer.getId());
        assertThat(storedLayer).isNotNull();
        assertThat(storedLayer.getDefaultStyle().getId()).isEqualTo(style1.getId());

        // Update layer to reference style2 which doesn't exist yet
        StyleInfo unresolvedStyle2 = resolvingProxy(style2);
        Patch patch = new Patch();
        patch.add("defaultStyle", unresolvedStyle2);

        // Update should be deferred
        enforcer.update(layer, patch);
        assertThat(enforcer.isConverged()).isFalse();

        // Add style2 - update should complete
        enforcer.add(style2);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getLayer(layer.getId()).getDefaultStyle().getId()).isEqualTo(style2.getId());
    }

    @Test
    void testAddLayerGroupWithMissingLayers() {
        // Setup dependencies
        WorkspaceInfo ws = data.workspaceA;
        NamespaceInfo ns = data.namespaceA;
        DataStoreInfo ds = data.dataStoreA;
        FeatureTypeInfo ft = data.featureTypeA;
        StyleInfo style = data.style1;
        LayerInfo layer = data.layerFeatureTypeA;
        LayerGroupInfo layerGroup = data.layerGroup1;

        // Set layer group's layer reference to unresolved proxy
        LayerInfo unresolvedLayer = resolvingProxy(layer);
        layerGroup.getLayers().clear();
        layerGroup.getLayers().add(unresolvedLayer);

        // Add workspace, namespace, datastore, and featuretype
        enforcer.add(ws);
        enforcer.add(ns);
        enforcer.add(ds);
        enforcer.add(ft);
        enforcer.add(style);

        // Add layer group with missing layer - should be deferred
        enforcer.add(layerGroup);
        assertThat(enforcer.isConverged()).isFalse();
        assertThat(catalog.getLayerGroup(layerGroup.getId())).isNull();

        // Add the layer - layer group should now be added
        enforcer.add(layer);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getLayerGroup(layerGroup.getId())).isNotNull();
        assertThat(catalog.getLayerGroup(layerGroup.getId()).getLayers().get(0).getId())
                .isEqualTo(layer.getId());
    }

    @Test
    void testAddLayerGroupWithMultipleMissingLayers() {
        // Create a layer group with multiple layers that don't exist yet
        WorkspaceInfo ws = data.workspaceA;
        NamespaceInfo ns = data.namespaceA;
        DataStoreInfo ds = data.dataStoreA;
        FeatureTypeInfo ft = data.featureTypeA;
        StyleInfo style = data.style1;
        LayerInfo layer1 = data.layerFeatureTypeA;

        // Create a second layer using test data
        FeatureTypeInfo ft2 = data.createFeatureType(
                "ft2", data.dataStoreB, data.namespaceB, "ftName2", "ftAbstract2", "ftDescription2", true);
        LayerInfo layer2 = data.createLayer("layer2", ft2, "Layer2", true, data.style2);

        LayerGroupInfo layerGroup = data.layerGroup1;

        // Set both layers as unresolved proxies
        LayerInfo unresolvedLayer1 = resolvingProxy(layer1);
        LayerInfo unresolvedLayer2 = resolvingProxy(layer2);
        layerGroup.getLayers().clear();
        layerGroup.getLayers().add(unresolvedLayer1);
        layerGroup.getLayers().add(unresolvedLayer2);

        // Add basic dependencies
        enforcer.add(ws);
        enforcer.add(ns);
        enforcer.add(ds);
        enforcer.add(ft);
        enforcer.add(style);

        // Add layer group - should be deferred due to missing layers
        enforcer.add(layerGroup);
        assertThat(enforcer.isConverged()).isFalse();
        assertThat(catalog.getLayerGroup(layerGroup.getId())).isNull();

        // Add first layer - still not converged (layer2 missing)
        enforcer.add(layer1);
        assertThat(enforcer.isConverged()).isFalse();
        assertThat(catalog.getLayerGroup(layerGroup.getId())).isNull();

        // Add dependencies for second layer
        enforcer.add(data.workspaceB);
        enforcer.add(data.namespaceB);
        enforcer.add(data.dataStoreB);
        enforcer.add(ft2);
        enforcer.add(data.style2);

        // Add second layer - now everything should resolve
        enforcer.add(layer2);
        assertThat(enforcer.isConverged()).isTrue();
        assertThat(catalog.getLayerGroup(layerGroup.getId())).isNotNull();
        assertThat(catalog.getLayerGroup(layerGroup.getId()).getLayers()).hasSize(2);
        assertThat(catalog.getLayerGroup(layerGroup.getId()).getLayers().get(0).getId())
                .isEqualTo(layer1.getId());
        assertThat(catalog.getLayerGroup(layerGroup.getId()).getLayers().get(1).getId())
                .isEqualTo(layer2.getId());
    }
}
