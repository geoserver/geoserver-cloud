/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.UnaryOperator;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerGroupStyleImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.impl.ResourceInfoImpl;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.config.plugin.GeoServerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogPropertyResolverTest {

    CatalogTestData testData;

    GeoServerImpl geoServer;
    Catalog catalog;

    UnaryOperator<Info> resolver;

    @BeforeEach
    void setUp() {
        catalog = new CatalogPlugin();
        geoServer = new GeoServerImpl();
        geoServer.setCatalog(catalog);
        testData = CatalogTestData.initialized(() -> catalog, () -> geoServer).initCatalog();
        resolver = CatalogPropertyResolver.of(catalog);
    }

    @Test
    void testReturnsSame() {
        assertThat(resolver.apply(null)).isNull();
        testReturnsSame(testData.coverageA);
        testReturnsSame(testData.dataStoreA);
        testReturnsSame(testData.featureTypeA);
        testReturnsSame(testData.layerFeatureTypeA);
        testReturnsSame(testData.layerGroup1);
        testReturnsSame(testData.namespaceA);
        testReturnsSame(testData.workspaceA);
    }

    @Test
    void testReturnsSameResolvingProxy() {
        testReturnsSame(ResolvingProxy.create("test", StyleInfo.class));
        testReturnsSame(ResolvingProxy.create("test", DataStoreInfo.class));
        testReturnsSame(ResolvingProxy.create("test", CoverageStoreInfo.class));
        testReturnsSame(ResolvingProxy.create("test", FeatureTypeInfo.class));
        testReturnsSame(ResolvingProxy.create("test", CoverageInfo.class));
        testReturnsSame(ResolvingProxy.create("test", LayerInfo.class));
        testReturnsSame(ResolvingProxy.create("test", LayerGroupInfo.class));
        testReturnsSame(ResolvingProxy.create("test", WorkspaceInfo.class));
        testReturnsSame(ResolvingProxy.create("test", NamespaceInfo.class));
    }

    private void testReturnsSame(CatalogInfo info) {
        CatalogInfo actual = ModificationProxy.unwrap(info);
        CatalogInfo proxied = ModificationProxy.create(actual, CatalogInfo.class);
        assertThat(resolver.apply(actual)).isSameAs(actual);
        assertThat(resolver.apply(proxied)).isSameAs(proxied);
    }

    @Test
    void storeInfo() {
        StoreInfoImpl store = new DataStoreInfoImpl(null);
        resolver.apply(store);
        assertThat(store.getCatalog()).isSameAs(catalog);
        store.setCatalog(null);
        resolver.apply(ModificationProxy.create(store, StoreInfo.class));
        assertThat(store.getCatalog()).isSameAs(catalog);
    }

    @Test
    void resourceInfo() {
        StoreInfoImpl store = new DataStoreInfoImpl(null);
        ResourceInfoImpl resource = new FeatureTypeInfoImpl(null);
        resource.setStore(store);
        resolver.apply(resource);

        assertThat(resource.getCatalog()).isSameAs(catalog);
        assertThat(store.getCatalog()).isSameAs(catalog);

        resource.setCatalog(null);
        store.setCatalog(null);
        resource.setStore(ModificationProxy.create(store, DataStoreInfo.class));

        resolver.apply(ModificationProxy.create(resource, FeatureTypeInfo.class));
        assertThat(resource.getCatalog()).isSameAs(catalog);
        assertThat(store.getCatalog()).isSameAs(catalog);
    }

    @Test
    void styleInfo() {
        StyleInfoImpl style = new StyleInfoImpl(null);
        style.setId("sid");

        resolver.apply(style);
        assertThat(style.getCatalog()).isSameAs(catalog);

        style.setCatalog(null);
        resolver.apply(ModificationProxy.create(style, StyleInfo.class));
        assertThat(style.getCatalog()).isSameAs(catalog);
    }

    @Test
    void styleInfoRemoveStyle() {
        StyleInfoImpl style = new StyleInfoImpl(null);
        style.setId(null); // this means it's a remote style

        resolver.apply(style);
        assertThat(style.getCatalog()).isNull();

        resolver.apply(ModificationProxy.create(style, StyleInfo.class));
        assertThat(style.getCatalog()).isNull();
    }

    @Test
    void layerInfo() {
        LayerInfoImpl layer = new LayerInfoImpl();
        ResourceInfoImpl resource = new FeatureTypeInfoImpl(null);
        StoreInfoImpl store = new DataStoreInfoImpl(null);
        StyleInfoImpl defaultStyle = new StyleInfoImpl(null);
        defaultStyle.setId("defaultStyle");
        StyleInfoImpl s1 = new StyleInfoImpl(null);
        s1.setId("s1");
        StyleInfoImpl s2 = new StyleInfoImpl(null);
        s2.setId("s2");

        resource.setStore(store);
        layer.setResource(resource);
        layer.setDefaultStyle(defaultStyle);
        layer.getStyles().add(s1);
        layer.getStyles().add(s2);

        resolver.apply(layer);
        assertThat(resource.getCatalog()).isSameAs(catalog);
        assertThat(store.getCatalog()).isSameAs(catalog);
        assertThat(defaultStyle.getCatalog()).isSameAs(catalog);
        assertThat(s1.getCatalog()).isSameAs(catalog);
        assertThat(s2.getCatalog()).isSameAs(catalog);

        resource.setStore(ModificationProxy.create(store, DataStoreInfo.class));
        layer.setResource(ModificationProxy.create(resource, FeatureTypeInfo.class));
        layer.setDefaultStyle(ModificationProxy.create(defaultStyle, StyleInfo.class));
        layer.getStyles().add(ModificationProxy.create(s1, StyleInfo.class));
        layer.getStyles().add(ModificationProxy.create(s2, StyleInfo.class));

        resolver.apply(ModificationProxy.create(layer, LayerInfo.class));
        assertThat(resource.getCatalog()).isSameAs(catalog);
        assertThat(store.getCatalog()).isSameAs(catalog);
        assertThat(defaultStyle.getCatalog()).isSameAs(catalog);
        assertThat(s1.getCatalog()).isSameAs(catalog);
        assertThat(s2.getCatalog()).isSameAs(catalog);
    }

    @Test
    void layerGroupInfoLayers() {
        LayerGroupInfoImpl layerGroup = new LayerGroupInfoImpl();
        LayerInfoImpl layer = new LayerInfoImpl();
        ResourceInfoImpl resource = new FeatureTypeInfoImpl(null);
        StoreInfoImpl store = new DataStoreInfoImpl(null);
        StyleInfoImpl defaultStyle = new StyleInfoImpl(null);
        defaultStyle.setId("defaultStyle");
        StyleInfoImpl s1 = new StyleInfoImpl(null);
        s1.setId("s1");
        StyleInfoImpl s2 = new StyleInfoImpl(null);
        s2.setId("s2");

        resource.setStore(store);
        layer.setResource(resource);
        layer.setDefaultStyle(defaultStyle);
        layer.getStyles().add(s1);
        layer.getStyles().add(s2);
        layerGroup.getLayers().add(layer);

        resolver.apply(layerGroup);
        assertThat(resource.getCatalog()).isSameAs(catalog);
        assertThat(store.getCatalog()).isSameAs(catalog);
        assertThat(defaultStyle.getCatalog()).isSameAs(catalog);
        assertThat(s1.getCatalog()).isSameAs(catalog);
        assertThat(s2.getCatalog()).isSameAs(catalog);

        resource.setStore(ModificationProxy.create(store, DataStoreInfo.class));
        layer.setResource(ModificationProxy.create(resource, FeatureTypeInfo.class));
        layer.setDefaultStyle(ModificationProxy.create(defaultStyle, StyleInfo.class));
        layer.getStyles().add(ModificationProxy.create(s1, StyleInfo.class));
        layer.getStyles().add(ModificationProxy.create(s2, StyleInfo.class));

        resolver.apply(ModificationProxy.create(layerGroup, LayerGroupInfo.class));
        assertThat(resource.getCatalog()).isSameAs(catalog);
        assertThat(store.getCatalog()).isSameAs(catalog);
        assertThat(defaultStyle.getCatalog()).isSameAs(catalog);
        assertThat(s1.getCatalog()).isSameAs(catalog);
        assertThat(s2.getCatalog()).isSameAs(catalog);
    }

    @Test
    void layerGroupInfoStyles() {
        LayerGroupInfoImpl layerGroup = new LayerGroupInfoImpl();
        StyleInfoImpl s1 = new StyleInfoImpl(null);
        s1.setId("s1");
        StyleInfoImpl remoteStyle = new StyleInfoImpl(null);
        remoteStyle.setId(null);
        StyleInfoImpl s2 = new StyleInfoImpl(null);
        s2.setId("s2");

        layerGroup.getStyles().add(s1);
        layerGroup.getStyles().add(remoteStyle);
        layerGroup.getStyles().add(s2);
        layerGroup.getStyles().add(null);

        resolver.apply(layerGroup);
        assertThat(s1.getCatalog()).isSameAs(catalog);
        assertThat(remoteStyle.getCatalog()).isNull();
        assertThat(s1.getCatalog()).isSameAs(catalog);
        assertThat(layerGroup.getStyles().get(3)).isNull();

        s1.setCatalog(null);
        s2.setCatalog(null);
        layerGroup.getStyles().clear();
        layerGroup.getStyles().add(null);
        layerGroup.getStyles().add(ModificationProxy.create(s1, StyleInfo.class));
        layerGroup.getStyles().add(ModificationProxy.create(remoteStyle, StyleInfo.class));
        layerGroup.getStyles().add(ModificationProxy.create(s2, StyleInfo.class));

        resolver.apply(ModificationProxy.create(layerGroup, LayerGroupInfo.class));
        assertThat(s1.getCatalog()).isSameAs(catalog);
        assertThat(remoteStyle.getCatalog()).isNull();
        assertThat(s1.getCatalog()).isSameAs(catalog);
        assertThat(layerGroup.getStyles().get(0)).isNull();
    }

    @Test
    void layerGroupInfoRootLayer() {
        LayerGroupInfoImpl layerGroup = new LayerGroupInfoImpl();
        LayerInfoImpl layer = new LayerInfoImpl();
        ResourceInfoImpl resource = new FeatureTypeInfoImpl(null);
        StoreInfoImpl store = new DataStoreInfoImpl(null);
        StyleInfoImpl defaultStyle = new StyleInfoImpl(null);
        defaultStyle.setId("defaultStyle");
        StyleInfoImpl s1 = new StyleInfoImpl(null);
        s1.setId("s1");
        StyleInfoImpl s2 = new StyleInfoImpl(null);
        s2.setId("s2");

        resource.setStore(store);
        layer.setResource(resource);
        layer.setDefaultStyle(defaultStyle);
        layer.getStyles().add(s1);
        layer.getStyles().add(s2);
        layerGroup.setRootLayer(layer);

        resolver.apply(layerGroup);
        assertThat(resource.getCatalog()).isSameAs(catalog);
        assertThat(store.getCatalog()).isSameAs(catalog);
        assertThat(defaultStyle.getCatalog()).isSameAs(catalog);
        assertThat(s1.getCatalog()).isSameAs(catalog);
        assertThat(s2.getCatalog()).isSameAs(catalog);

        resource.setStore(ModificationProxy.create(store, DataStoreInfo.class));
        layer.setResource(ModificationProxy.create(resource, FeatureTypeInfo.class));
        layer.setDefaultStyle(ModificationProxy.create(defaultStyle, StyleInfo.class));
        layer.getStyles().add(ModificationProxy.create(s1, StyleInfo.class));
        layer.getStyles().add(ModificationProxy.create(s2, StyleInfo.class));

        resolver.apply(ModificationProxy.create(layerGroup, LayerGroupInfo.class));
        assertThat(resource.getCatalog()).isSameAs(catalog);
        assertThat(store.getCatalog()).isSameAs(catalog);
        assertThat(defaultStyle.getCatalog()).isSameAs(catalog);
        assertThat(s1.getCatalog()).isSameAs(catalog);
        assertThat(s2.getCatalog()).isSameAs(catalog);
    }

    @Test
    void layerGroupInfoRootLayerStyle() {
        LayerGroupInfoImpl layerGroup = new LayerGroupInfoImpl();
        StyleInfoImpl s1 = new StyleInfoImpl(null);
        s1.setId("s1");
        StyleInfoImpl remoteStyle = new StyleInfoImpl(null);
        remoteStyle.setId(null);

        layerGroup.setRootLayerStyle(s1);
        resolver.apply(layerGroup);
        assertThat(s1.getCatalog()).isSameAs(catalog);

        s1.setCatalog(null);
        resolver.apply(ModificationProxy.create(layerGroup, LayerGroupInfo.class));
        assertThat(s1.getCatalog()).isSameAs(catalog);

        layerGroup.setRootLayerStyle(remoteStyle);
        resolver.apply(layerGroup);
        assertThat(remoteStyle.getCatalog()).isNull();
    }

    @Test
    void layerGroupLayerGroupStyles() {
        LayerInfoImpl layer = new LayerInfoImpl();
        ResourceInfoImpl resource = new FeatureTypeInfoImpl(null);
        StoreInfoImpl store = new DataStoreInfoImpl(null);
        StyleInfoImpl defaultStyle = new StyleInfoImpl(null);
        defaultStyle.setId("defaultStyle");
        StyleInfoImpl s1 = new StyleInfoImpl(null);
        s1.setId("s1");
        StyleInfoImpl remoteStyle = new StyleInfoImpl(null);
        remoteStyle.setId(null);

        resource.setStore(store);
        layer.setResource(resource);
        layer.setDefaultStyle(defaultStyle);

        LayerGroupStyleImpl lgStyle = new LayerGroupStyleImpl();
        lgStyle.getStyles().add(remoteStyle);
        lgStyle.getLayers().add(null);

        lgStyle.getLayers().add(layer);
        lgStyle.getStyles().add(s1);

        lgStyle.getLayers().add(null);
        lgStyle.getStyles().add(null);

        LayerGroupInfoImpl layerGroup = new LayerGroupInfoImpl();
        layerGroup.getLayerGroupStyles().add(null);
        layerGroup.getLayerGroupStyles().add(lgStyle);

        resolver.apply(layerGroup);
        assertThat(resource.getCatalog()).isSameAs(catalog);
        assertThat(store.getCatalog()).isSameAs(catalog);
        assertThat(defaultStyle.getCatalog()).isSameAs(catalog);
        assertThat(s1.getCatalog()).isSameAs(catalog);
        assertThat(remoteStyle.getCatalog()).isNull();
    }
}
