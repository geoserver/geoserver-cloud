/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.catalog.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.geoserver.cloud.catalog.cache.CachingCatalogFacadeContainmentSupport.DEFAULT_NAMESPACE_CACHE_KEY;
import static org.geoserver.cloud.catalog.cache.CachingCatalogFacadeContainmentSupport.DEFAULT_WORKSPACE_CACHE_KEY;
import static org.geoserver.cloud.catalog.cache.CachingCatalogFacadeTest.once;
import static org.geoserver.cloud.event.info.ConfigInfoType.RESOURCE;
import static org.geoserver.cloud.event.info.ConfigInfoType.STORE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import lombok.NonNull;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.ows.util.OwsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache.ValueWrapper;

/**
 * @since 1.7
 */
class CachingCatalogFacadeContainmentSupportTest {

    private CachingCatalogFacadeContainmentSupport support;

    @BeforeEach
    void setUp() {
        support = new CachingCatalogFacadeContainmentSupport();
    }

    @SuppressWarnings("unchecked")
    private <T> Callable<T> loader() {
        return mock(Callable.class);
    }

    @Test
    void testGetDefaultWorkspace() throws Exception {
        assertCaches(
                WorkspaceInfo.class, (supp, loader) -> supp.getDefaultWorkspace(loader), DEFAULT_WORKSPACE_CACHE_KEY);
    }

    @Test
    void testEvictDefaultWorkspace() {
        support.getCache().put(DEFAULT_WORKSPACE_CACHE_KEY, stub(WorkspaceInfo.class));
        support.evictDefaultWorkspace();
        assertNotCached(DEFAULT_WORKSPACE_CACHE_KEY);
    }

    @Test
    void testGetDefaultNamespace() throws Exception {
        assertCaches(
                NamespaceInfo.class, (supp, loader) -> supp.getDefaultNamespace(loader), DEFAULT_NAMESPACE_CACHE_KEY);
    }

    @Test
    void testEvictDefaultNamespace() {
        support.getCache().put(DEFAULT_NAMESPACE_CACHE_KEY, mock(NamespaceInfo.class));
        support.evictDefaultNamespace();
        assertNotCached(DEFAULT_NAMESPACE_CACHE_KEY);
    }

    @Test
    void testGetDefaultDataStore() throws Exception {
        WorkspaceInfo ws = stub(WorkspaceInfo.class);
        Object key = support.generateDefaultDataStoreKey(ws.getId());
        assertCaches(DataStoreInfo.class, (supp, loader) -> supp.getDefaultDataStore(ws, loader), key);
    }

    @Test
    void testEvictDefaultDataStoreWorkspaceInfo() {
        WorkspaceInfo ws = stub(WorkspaceInfo.class);
        Object key = support.generateDefaultDataStoreKey(ws.getId());
        DataStoreInfo ds = stub(DataStoreInfo.class);
        support.getCache().put(key, ds);

        assertCached(key, ds);
        support.evictDefaultDataStore(ws);
        assertNotCached(key);
    }

    @Test
    void testGetDoesNotCacheNullValues() {
        WorkspaceInfo ws = stub(WorkspaceInfo.class);

        InfoIdKey idKey = InfoIdKey.valueOf(ws);
        InfoNameKey nameKey = InfoNameKey.valueOf(ws);

        Callable<WorkspaceInfo> loader = loader();

        assertThat(support.get(idKey, loader)).isNull();
        assertThat(support.get(nameKey, loader)).isNull();
        assertNotCached(idKey);
        assertNotCached(nameKey);
    }

    @Test
    void testGet() throws Exception {
        WorkspaceInfo ws = stub(WorkspaceInfo.class);
        InfoIdKey idKey = InfoIdKey.valueOf(ws);
        InfoNameKey nameKey = InfoNameKey.valueOf(ws);

        Callable<WorkspaceInfo> loader = loader();
        when(loader.call()).thenReturn(ws);

        assertThat(support.get(idKey, loader)).isSameAs(ws);
        assertCached(idKey, ws);
        assertNotCached(nameKey);

        assertThat(support.get(nameKey, loader)).isSameAs(ws);
        assertCached(nameKey, ws);
    }

    @Test
    void testEvictAndGetCachesIdAndNameKeys() throws Exception {
        WorkspaceInfo ws = stub(WorkspaceInfo.class);
        WorkspaceInfo ws2 = stub(WorkspaceInfo.class);

        InfoIdKey idKey = InfoIdKey.valueOf(ws);
        InfoNameKey nameKey = InfoNameKey.valueOf(ws);
        support.getCache().put(idKey, ws);
        support.getCache().put(nameKey, ws);

        assertCached(idKey, ws);
        assertCached(nameKey, ws);

        Callable<WorkspaceInfo> loader = loader();
        when(loader.call()).thenReturn(ws2);

        WorkspaceInfo loaded = support.evictAndGet(ws, loader);
        assertThat(loaded).isSameAs(ws2);
        assertCached(idKey, ws2);
        assertCached(nameKey, ws2);
    }

    @Test
    void testEvictAndGetDoesNotCacheNullValues() {
        WorkspaceInfo ws = stub(WorkspaceInfo.class);
        InfoIdKey idKey = InfoIdKey.valueOf(ws);
        InfoNameKey nameKey = InfoNameKey.valueOf(ws);

        Callable<WorkspaceInfo> loader = loader();
        WorkspaceInfo loaded = support.evictAndGet(ws, loader);
        assertThat(loaded).isNull();
        assertNotCached(idKey);
        assertNotCached(nameKey);
    }

    @Test
    void testGetLayersByResource() throws Exception {
        Callable<List<LayerInfo>> loader = loader();
        List<LayerInfo> value = List.of(mock(LayerInfo.class));
        when(loader.call()).thenReturn(value);

        InfoIdKey key = support.generateLayersByResourceKey("resource-id");
        List<LayerInfo> ret = support.getLayersByResource("resource-id", loader);
        assertThat(ret).isSameAs(value);

        assertCached(key, value);
    }

    @Test
    @DisplayName("getLayersByResource() does not cache empty lists")
    void testGetLayersByResourceDoesNotCacheEmptyList() throws Exception {
        Callable<List<LayerInfo>> loader = loader();

        when(loader.call()).thenReturn(List.of());

        InfoIdKey key = support.generateLayersByResourceKey("resource-id");
        List<LayerInfo> ret = support.getLayersByResource("resource-id", loader);
        assertThat(ret).isEmpty();
        assertNotCached(key);
    }

    @Test
    void testEvictEvictsIdAndNameKeys() {
        testEvictEvictsIdAndNameKeys(WorkspaceInfo.class);
        testEvictEvictsIdAndNameKeys(NamespaceInfo.class);
        testEvictEvictsIdAndNameKeys(DataStoreInfo.class);
        testEvictEvictsIdAndNameKeys(CoverageStoreInfo.class);
        testEvictEvictsIdAndNameKeys(WMSStoreInfo.class);
        testEvictEvictsIdAndNameKeys(WMTSStoreInfo.class);
        testEvictEvictsIdAndNameKeys(FeatureTypeInfo.class);
        testEvictEvictsIdAndNameKeys(CoverageInfo.class);
        testEvictEvictsIdAndNameKeys(WMSLayerInfo.class);
        testEvictEvictsIdAndNameKeys(WMTSLayerInfo.class);
        testEvictEvictsIdAndNameKeys(LayerInfo.class);
        testEvictEvictsIdAndNameKeys(LayerGroupInfo.class);
        testEvictEvictsIdAndNameKeys(StyleInfo.class);
    }

    private void testEvictEvictsIdAndNameKeys(Class<? extends CatalogInfo> clazz) {
        var info = stubWithRefs(clazz);
        InfoIdKey idKey = InfoIdKey.valueOf(info);
        InfoNameKey nameKey = InfoNameKey.valueOf(info);

        support.getCache().put(idKey, info);
        support.getCache().put(nameKey, info);
        assertCached(idKey, info);
        assertCached(nameKey, info);

        support.evict(info.getId(), InfoEvent.prefixedName(info), ConfigInfoType.valueOf(info));
        assertNotCached(idKey);
        assertNotCached(nameKey);
    }

    @Test
    @DisplayName("when a CatalogInfo is evicted, both the InfoIdKey and InfoNameKey entries are evicted")
    void testEvictCatalogInfoEvictsIdAndNameKeys() {
        WorkspaceInfo ws = stub(WorkspaceInfo.class);

        InfoIdKey idKey = InfoIdKey.valueOf(ws);
        InfoNameKey nameKey = InfoNameKey.valueOf(ws);

        support.getCache().put(idKey, ws);
        assertCached(idKey, ws);

        support.getCache().put(nameKey, ws);
        assertCached(nameKey, ws);

        support.evict(ws);
        assertNotCached(idKey);
        assertNotCached(nameKey);
    }

    @Test
    @DisplayName("when a LayerInfo is evicted, the layers by resource list for its ResourceInfo is evicted")
    void testEvictCatalogInfoLayerInfoEvictsLayersByResource() {
        LayerInfo layer = stub(LayerInfo.class);
        FeatureTypeInfo resource = stub(FeatureTypeInfo.class);
        when(layer.getResource()).thenReturn(resource);

        InfoIdKey key = support.generateLayersByResourceKey(layer.getResource());
        List<LayerInfo> value = List.of(layer);
        support.getCache().put(key, value);

        assertCached(key, value);
        support.evict(layer);
        assertNotCached(key);
    }

    @Test
    @DisplayName("when a StoreInfo is evicted, the keys for StoreInfo and its concrete type are evicted")
    void testEvictStoreInfoEvictsTheGenericAndConcreteTypeKeys() {
        var store = stubWithRefs(DataStoreInfo.class);
        InfoIdKey id = InfoIdKey.valueOf(store);
        InfoIdKey idGeneric = InfoIdKey.valueOf(id.id(), STORE);
        InfoNameKey name = InfoNameKey.valueOf(store);
        InfoNameKey nameGeneric = InfoNameKey.valueOf(name.prefixedName(), STORE);
        support.getCache().put(id, store);
        support.getCache().put(idGeneric, store);
        support.getCache().put(name, store);
        support.getCache().put(nameGeneric, store);

        support.evict(store);
        assertNotCached(id);
        assertNotCached(idGeneric);
        assertNotCached(name);
        assertNotCached(nameGeneric);
    }

    @Test
    @DisplayName("when a ResourceInfo is evicted, the keys for ResourceInfo and its concrete type are evicted")
    void testEvictResourceInfoEvictsTheGenericAndConcreteTypeKeys() {
        var resource = stubWithRefs(FeatureTypeInfo.class);
        InfoIdKey id = InfoIdKey.valueOf(resource);
        InfoIdKey idGeneric = InfoIdKey.valueOf(id.id(), RESOURCE);
        InfoNameKey name = InfoNameKey.valueOf(resource);
        InfoNameKey nameGeneric = InfoNameKey.valueOf(name.prefixedName(), RESOURCE);
        support.getCache().put(id, resource);
        support.getCache().put(idGeneric, resource);
        support.getCache().put(name, resource);
        support.getCache().put(nameGeneric, resource);

        support.evict(resource);
        assertNotCached(id);
        assertNotCached(idGeneric);
        assertNotCached(name);
        assertNotCached(nameGeneric);
    }

    @Test
    @DisplayName("when a PublishedInfo is evicted, the LayerGroups referencing it are evicted")
    void testEvictPublishedInfoEvictsLayerGroupsReferencingIt() {
        LayerInfo l1 = stubLayer("layer1", null, Set.of());
        LayerInfo l2 = stubLayer("layer2", null, Set.of());
        LayerGroupInfo lg1 = stubLayerGroup("lg1", List.of(l1, l2), List.of());
        LayerGroupInfo lg2 = stubLayerGroup("lg2", List.of(l2), List.of());

        put(l1, l2, lg1, lg2);
        assertAllCached(l1, l2, lg1, lg2);

        support.evict(l1);
        assertNotCached(l1);
        // lg1 references l1, evicting l1 should evict also lg1
        assertNotCached(lg1);
        // but should not evict lg2
        assertCached(lg2);

        put(l1, l2, lg1, lg2);
        assertAllCached(l1, l2, lg1, lg2);

        support.evict(l2);
        // both lg1 and lg2 reference l2, evicting l2 should evict both groups
        assertNotCached(lg1);
        assertNotCached(lg2);
    }

    @Test
    @DisplayName("when a style is evicted, all cached layer and groups referencing it are evicted")
    void testEvictStyleEvictsLayersAndLayerGroupsReferencingIt() {
        StyleInfo defStyle1 = stubReal(StyleInfo.class, "s1", "defStyle1");
        StyleInfo style1 = stubReal(StyleInfo.class, "s3", "style1");
        LayerInfo layer1 = stubLayer("layer1", defStyle1, Set.of(defStyle1, style1));

        StyleInfo defStyle2 = stubReal(StyleInfo.class, "s2", "defStyle2");
        StyleInfo style2 = stubReal(StyleInfo.class, "s4", "style2");
        LayerInfo layer2 = stubLayer("layer2", defStyle2, Set.of(defStyle2, style2));

        LayerGroupInfo lg1 = stubLayerGroup("lg1", List.of(layer1), List.of(defStyle1));
        LayerGroupInfo lg2 = stubLayerGroup("lg2", List.of(layer1, layer2), List.of(defStyle1, defStyle2));

        put(defStyle1, defStyle2, style1, style2, layer1, layer2, lg1, lg2);
        assertAllCached(defStyle1, defStyle2, style1, style2, layer1, layer2, lg1, lg2);

        support.evict(defStyle1);
        assertNotCached(defStyle1, layer1, lg1, lg2);
        assertAllCached(defStyle2, style1, style2, layer2);

        put(defStyle1, defStyle2, style1, style2, layer1, layer2, lg1, lg2);
        assertAllCached(defStyle1, defStyle2, style1, style2, layer1, layer2, lg1, lg2);

        support.evict(defStyle2);
        assertNotCached(defStyle2, layer2, lg2);
        assertAllCached(defStyle1, style1, style2, layer1, lg1);

        put(defStyle1, defStyle2, style1, style2, layer1, layer2, lg1, lg2);
        assertAllCached(defStyle1, defStyle2, style1, style2, layer1, layer2, lg1, lg2);

        support.evict(style1);
        assertNotCached(style1, layer1, lg1, lg2);
        assertAllCached(defStyle1, defStyle2, layer2);
    }

    @Test
    @DisplayName("when a WorkspaceInfo is evicted, all objects referencing it directly or indirectly are evicted")
    void testEvictWorkspaceInfoEvictsAllInfosReferencingIt() {
        WorkspaceInfo ws1 = stubReal(WorkspaceInfo.class, "ws1", "workspace1");
        NamespaceInfo ns1 = stubReal(NamespaceInfo.class, "ns1", "workspace1");
        DataStoreInfo store1 = stubReal(DataStoreInfo.class, "ds1", "ds1");
        store1.setWorkspace(ws1);
        StyleInfo style1 = stubReal(StyleInfo.class, "s1", "style1");
        style1.setWorkspace(ws1);
        LayerInfo layer1 = stubLayer("layer1", style1, Set.of(style1));
        layer1.getResource().setNamespace(ns1);
        layer1.getResource().setStore(store1);
        LayerGroupInfo lg1 = stubLayerGroup("lg1", List.of(layer1), List.of());
        lg1.setWorkspace(ws1);

        WorkspaceInfo ws2 = stubReal(WorkspaceInfo.class, "ws2", "workspace2");
        NamespaceInfo ns2 = stubReal(NamespaceInfo.class, "ns2", "workspace2");
        DataStoreInfo store2 = stubReal(DataStoreInfo.class, "ds2", "ds2");
        store2.setWorkspace(ws2);
        StyleInfo style2 = stubReal(StyleInfo.class, "s2", "style2");
        style2.setWorkspace(ws2);
        LayerInfo layer2 = stubLayer("layer2", style2, Set.of(style2));
        layer2.getResource().setNamespace(ns2);
        layer2.getResource().setStore(store2);
        LayerGroupInfo lg2 = stubLayerGroup("lg2", List.of(layer2), List.of());
        lg2.setWorkspace(ws2);

        put(ws1, ns1, store1, style1, layer1, lg1);
        put(ws2, ns2, store2, style2, layer2, lg2);

        assertAllCached(ws1, ns1, store1, style1, layer1, lg1);
        assertAllCached(ws2, ns2, store2, style2, layer2, lg2);

        support.evict(ws1);
        assertNotCached(ws1, store1, style1, layer1, lg1);
        assertAllCached(ws2, store2, style2, layer2, lg2);

        put(ws1, store1, style1, layer1, lg1);
        support.evict(ws2);
        assertAllCached(ws1, store1, style1, layer1, lg1);
        assertNotCached(ws2, store2, style2, layer2, lg2);
    }

    <T> void assertCaches(
            Class<T> type, BiFunction<CachingCatalogFacadeContainmentSupport, Callable<T>, T> getter, Object... keys)
            throws Exception {

        Callable<T> loader = loader();
        assertThat(getter.apply(support, loader)).isNull();
        verify(loader, once()).call();

        for (Object key : keys) {
            assertCachedNull(key);
            support.getCache().evict(key);
        }

        T value = mock(type);
        when(loader.call()).thenReturn(value);
        assertThat(loader.call()).isSameAs(value);
        assertThat(getter.apply(support, loader)).isSameAs(value);

        for (Object key : keys) {
            assertCached(key, value);
        }
    }

    private void assertCachedNull(Object key) {
        assertThat(support.getCache().get(key))
                .as("Expected non-null ValueWrapper for key %s".formatted(key))
                .isNotNull()
                .extracting(ValueWrapper::get)
                .as("Expected cached value null for key %s".formatted(key))
                .isNull();
    }

    private void assertNotCached(CatalogInfo... infos) {
        for (var info : infos) {
            assertNotCached(info);
        }
    }

    private void assertNotCached(CatalogInfo info) {
        assertNotCached(InfoIdKey.valueOf(info));
        assertNotCached(InfoNameKey.valueOf(info));
    }

    private void assertNotCached(Object key) {
        assertThat(support.getCache().get(key))
                .as("Expected null ValueWrapper for key %s".formatted(key))
                .isNull();
    }

    private void assertAllCached(CatalogInfo... infos) {
        for (var info : infos) {
            assertCached(info);
        }
    }

    private void assertCached(CatalogInfo info) {
        assertCached(InfoIdKey.valueOf(info), info);
        assertCached(InfoNameKey.valueOf(info), info);
    }

    private void assertCached(Object key, Object value) {
        assertThat(support.getCache().get(key))
                .as("Expected non-null ValueWrapper for key %s".formatted(key))
                .isNotNull()
                .extracting(ValueWrapper::get)
                .as("Expected cached value for key %s".formatted(key))
                .isSameAs(value);
    }

    static <I extends CatalogInfo> I stub(Class<I> type) {
        String id = type.getSimpleName() + "-id";
        String name = type.getSimpleName() + "-name";
        return stub(type, id, name);
    }

    static <I extends CatalogInfo> I stub(Class<I> type, String id, String name) {
        I info = mock(type);
        when(info.getId()).thenReturn(id);
        if (info instanceof WorkspaceInfo i) {
            when(i.getName()).thenReturn(name);
        } else if (info instanceof NamespaceInfo i) {
            when(i.getPrefix()).thenReturn(name);
        } else if (info instanceof StoreInfo i) {
            when(i.getName()).thenReturn(name);
        } else if (info instanceof ResourceInfo i) {
            when(i.getName()).thenReturn(name);
        } else if (info instanceof PublishedInfo i) {
            when(i.getName()).thenReturn(name);
        } else if (info instanceof StyleInfo i) {
            when(i.getName()).thenReturn(name);
        }
        return info;
    }

    private void put(CatalogInfo... infos) {
        for (var info : infos) {
            support.put(() -> info);
        }
    }

    private <C extends CatalogInfo> C stubReal(Class<C> clazz, String id, String name) {

        C info = create(ConfigInfoType.valueOf(clazz));
        OwsUtils.set(info, "id", id);
        if (info instanceof LayerInfo l) {
            ResourceInfo resource = stubReal(FeatureTypeInfo.class, id + "-resource", name);
            l.setResource(resource);
            resource.setNamespace(stubReal(NamespaceInfo.class, id + "-ns", name + "-ns"));
        } else if (info instanceof NamespaceInfo ns) {
            ns.setPrefix(name);
        } else {
            OwsUtils.set(info, "name", name);
        }
        return info;
    }

    private <C extends CatalogInfo> C create(@NonNull ConfigInfoType type) {
        Class<C> clazz = type.type();
        return clazz.cast(
                switch (type) {
                    case LAYER -> new LayerInfoImpl();
                    case LAYERGROUP -> new LayerGroupInfoImpl();
                    case STYLE -> new StyleInfoImpl(null);
                    case FEATURETYPE -> new FeatureTypeInfoImpl(null);
                    case NAMESPACE -> new NamespaceInfoImpl();
                    case WORKSPACE -> new WorkspaceInfoImpl();
                    case DATASTORE -> new DataStoreInfoImpl(null);
                    default -> throw new UnsupportedOperationException("not configured to create " + type);
                });
    }

    private <C extends CatalogInfo> C stubWithRefs(Class<C> clazz) {
        C info = stub(clazz);
        if (info instanceof StoreInfo s) {
            var ws = stub(WorkspaceInfo.class);
            when(s.getWorkspace()).thenReturn(ws);
        } else if (info instanceof ResourceInfo r) {
            var n = stub(NamespaceInfo.class);
            when(r.getNamespace()).thenReturn(n);
        } else if (info instanceof LayerInfo l) {
            var r = stub(CoverageInfo.class);
            var n = stub(NamespaceInfo.class);
            when(r.getNamespace()).thenReturn(n);
            when(l.getResource()).thenReturn(r);
        } else if (info instanceof LayerGroupInfo lg) {
            var ws = stub(WorkspaceInfo.class);
            when(lg.getWorkspace()).thenReturn(ws);
        }
        return info;
    }

    private LayerInfo stubLayer(String name, StyleInfo defaultStyle, Set<StyleInfo> styles) {
        var layer = stubReal(LayerInfo.class, name + "-id", name);
        layer.setDefaultStyle(defaultStyle);
        layer.getStyles().addAll(styles);
        return layer;
    }

    private LayerGroupInfo stubLayerGroup(String name, List<PublishedInfo> layers, List<StyleInfo> styles) {

        var lg = stubReal(LayerGroupInfo.class, name + "-id", name);
        lg.getLayers().addAll(layers);
        lg.getStyles().addAll(styles);
        return lg;
    }
}
