/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.geoserver.cloud.event.info.ConfigInfoType.COVERAGESTORE;
import static org.geoserver.cloud.event.info.ConfigInfoType.DATASTORE;
import static org.geoserver.cloud.event.info.ConfigInfoType.FEATURETYPE;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYER;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYERGROUP;
import static org.geoserver.cloud.event.info.ConfigInfoType.NAMESPACE;
import static org.geoserver.cloud.event.info.ConfigInfoType.SERVICE;
import static org.geoserver.cloud.event.info.ConfigInfoType.SETTINGS;
import static org.geoserver.cloud.event.info.ConfigInfoType.STYLE;
import static org.geoserver.cloud.event.info.ConfigInfoType.WORKSPACE;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey.Kind;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class CatalogInfoKeysTest {

    // a lookup by the local name of a layer keeps its miss under that name
    @Test
    void layerHasLocalNameKey() {
        List<CacheKey> keys = CatalogInfoKeys.keysOf(LAYER, "layer-1", "topp:roads");

        assertThat(keys)
                .containsExactly(
                        CacheKey.entity(LAYER, "layer-1"),
                        CacheKey.name(LAYER, "topp:roads"),
                        CacheKey.name(LAYER, "roads"),
                        CacheKey.tileLayer("layer-1"));
    }

    @Test
    void bareLayerNameComesOnce() {
        List<CacheKey> keys = CatalogInfoKeys.keysOf(LAYER, "layer-1", "roads");

        assertThat(keys)
                .containsExactly(
                        CacheKey.entity(LAYER, "layer-1"),
                        CacheKey.name(LAYER, "roads"),
                        CacheKey.tileLayer("layer-1"));
    }

    // the getters of a reference not resolved yet answer null: the name of the store is unknown
    @Test
    void unresolvedPrefixHasNoName() {
        DataStoreInfoImpl store = storeInUnresolvedWorkspace("roads");

        assertThat(CatalogInfoKeys.keysOf(store)).containsExactly(CacheKey.entity(DATASTORE, "store-1"));
    }

    @Test
    void namespaceHasUriKey() {
        NamespaceInfoImpl namespace = new NamespaceInfoImpl();
        namespace.setId("ns-1");
        namespace.setPrefix("topp");
        namespace.setURI("http://topp.example");

        assertThat(CatalogInfoKeys.keysOf(namespace))
                .containsExactly(
                        CacheKey.entity(NAMESPACE, "ns-1"),
                        CacheKey.name(NAMESPACE, "topp"),
                        CatalogInfoKeys.namespaceByUri("http://topp.example"));
    }

    @Test
    void workspaceRenameStalesNames() {
        Patch rename = new Patch().with("name", "topp2");

        Predicate<CacheKey> dependents =
                CatalogInfoKeys.dependentKeys(WORKSPACE, rename).orElseThrow();

        assertThat(dependents.test(CacheKey.name(DATASTORE, "topp:roads"))).isTrue();
        assertThat(dependents.test(CacheKey.entity(DATASTORE, "store-1"))).isFalse();
        assertThat(dependents.test(CacheKey.name(LAYER, "topp:roads"))).isFalse();
    }

    // the database moves the resources of the store to the namespace of the new workspace
    @Test
    void storeMoveStalesResources() {
        Patch move = new Patch().with("workspace", ResolvingProxy.create("ws-2", WorkspaceInfo.class));

        Predicate<CacheKey> dependents =
                CatalogInfoKeys.dependentKeys(DATASTORE, move).orElseThrow();

        assertThat(dependents.test(CacheKey.entity(FEATURETYPE, "roads-1"))).isTrue();
        assertThat(dependents.test(CacheKey.name(LAYER, "topp:roads"))).isTrue();
        assertThat(dependents.test(CacheKey.entity(LAYER, "layer-1"))).isFalse();
    }

    @Test
    void otherChangesStaleNothing() {
        Optional<Predicate<CacheKey>> dependents =
                CatalogInfoKeys.dependentKeys(WORKSPACE, new Patch().with("isolated", true));

        assertThat(dependents).isEmpty();
    }

    // a move changes the prefix of a name: the old names are those with the local name, whatever their prefix
    @Test
    void localNameMatchesAnyPrefix() {
        Predicate<CacheKey> roads = CatalogInfoKeys.namesWithLocalName(DATASTORE, "roads");

        assertThat(roads.test(CacheKey.name(DATASTORE, "topp:roads"))).isTrue();
        assertThat(roads.test(CacheKey.name(DATASTORE, "roads"))).isTrue();
        assertThat(roads.test(CacheKey.name(DATASTORE, "topp:rivers"))).isFalse();
        assertThat(roads.test(CacheKey.name(COVERAGESTORE, "topp:roads"))).isFalse();
        assertThat(roads.test(CacheKey.entity(DATASTORE, "roads"))).isFalse();
    }

    @Test
    void namesOfMatchesNameKeys() {
        Predicate<CacheKey> layerNames = CatalogInfoKeys.namesOf(LAYER);

        assertThat(layerNames.test(CacheKey.name(LAYER, "topp:roads"))).isTrue();
        assertThat(layerNames.test(CacheKey.entity(LAYER, "layer-1"))).isFalse();
        assertThat(layerNames.test(CacheKey.name(FEATURETYPE, "topp:roads"))).isFalse();
    }

    @Test
    void lookupsOfMatchesLookupKeys() {
        Predicate<CacheKey> serviceLookups = CatalogInfoKeys.lookupsOf(SERVICE);

        assertThat(serviceLookups.test(CacheKey.lookup(SERVICE, "service-name:WMS")))
                .isTrue();
        assertThat(serviceLookups.test(CacheKey.entity(SERVICE, "wms-1"))).isFalse();
        assertThat(serviceLookups.test(CacheKey.lookup(SETTINGS, "settings:ws-1")))
                .isFalse();
    }

    // the default namespace is a lookup of another kind
    @Test
    void uriLookupsSpareDefault() {
        Predicate<CacheKey> uriLookups = CatalogInfoKeys.namespaceUriLookups();

        assertThat(uriLookups.test(CatalogInfoKeys.namespaceByUri("http://topp.example")))
                .isTrue();
        assertThat(uriLookups.test(CatalogInfoKeys.defaultNamespace())).isFalse();
        assertThat(uriLookups.test(CacheKey.name(NAMESPACE, "topp"))).isFalse();
    }

    // the workspace is a reference not resolved yet: the prefix of the name is not compared
    @Test
    void storeComparesLocalName() {
        DataStoreInfoImpl store = storeInUnresolvedWorkspace("roads");

        assertThat(CatalogInfoKeys.hasName(store, "topp:roads")).isTrue();
        assertThat(CatalogInfoKeys.hasName(store, "topp:rivers")).isFalse();
    }

    // an object of a workspace has a prefixed name, and an object without one a bare name
    @Test
    void prefixFollowsWorkspace() {
        DataStoreInfoImpl store = storeInUnresolvedWorkspace("roads");
        StyleInfoImpl style = new StyleInfoImpl(null);
        style.setName("line");

        assertThat(CatalogInfoKeys.hasName(store, "roads")).isFalse();
        assertThat(CatalogInfoKeys.hasName(style, "topp:line")).isFalse();
        assertThat(CatalogInfoKeys.hasName(style, "line")).isTrue();
    }

    // the name a:b of a group or style without workspace is also the prefixed name of b in the workspace a
    @Test
    void globalNameWithColonHasNoKey() {
        LayerGroupInfoImpl group = group("group-1", "a:b", null);

        assertThat(CatalogInfoKeys.globalName("a:b")).isNull();
        assertThat(CatalogInfoKeys.globalName("basemap")).isEqualTo("basemap");
        assertThat(CatalogInfoKeys.keysOf(group)).doesNotContain(CacheKey.name(LAYERGROUP, "a:b"));
        assertThat(CatalogInfoKeys.hasName(group, "a:b")).isFalse();
    }

    // PostgreSQL cannot store the first character: no stored name, prefixed or not, equals the key
    @Test
    void missKeyIsMarkedNameKey() {
        CacheKey miss = CatalogInfoKeys.globalNameMiss(LAYERGROUP, "a:b");

        assertThat(miss.kind()).isEqualTo(Kind.NAME);
        assertThat(miss).isNotEqualTo(CacheKey.name(LAYERGROUP, "a:b"));
        assertThat(miss.key()).startsWith("\u0000");
    }

    // a write of the group b of the workspace a cannot change whether a group without workspace is named a:b
    @Test
    void globalObjectKeysHoldMiss() {
        StyleInfoImpl style = new StyleInfoImpl(null);
        style.setId("style-1");
        style.setName("a:b");
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setName("a");

        assertThat(CatalogInfoKeys.keysOf(group("group-1", "a:b", null)))
                .contains(CatalogInfoKeys.globalNameMiss(LAYERGROUP, "a:b"));
        assertThat(CatalogInfoKeys.keysOf(style)).contains(CatalogInfoKeys.globalNameMiss(STYLE, "a:b"));
        assertThat(CatalogInfoKeys.keysOf(group("group-2", "basemap", null)))
                .containsExactly(
                        CacheKey.entity(LAYERGROUP, "group-2"),
                        CacheKey.name(LAYERGROUP, "basemap"),
                        CacheKey.tileLayer("group-2"));
        assertThat(CatalogInfoKeys.keysOf(group("group-3", "b", workspace)))
                .containsExactly(
                        CacheKey.entity(LAYERGROUP, "group-3"),
                        CacheKey.name(LAYERGROUP, "a:b"),
                        CacheKey.tileLayer("group-3"));
    }

    // the name a:b of an event may be the name of a group without workspace or the prefixed name of b in a
    @Test
    void eventKeysHoldMiss() {
        assertThat(CatalogInfoKeys.keysOf(LAYERGROUP, "group-1", "a:b"))
                .containsExactly(
                        CacheKey.entity(LAYERGROUP, "group-1"),
                        CacheKey.name(LAYERGROUP, "a:b"),
                        CatalogInfoKeys.globalNameMiss(LAYERGROUP, "a:b"),
                        CacheKey.tileLayer("group-1"));
        assertThat(CatalogInfoKeys.keysOf(STYLE, "style-1", "a:b"))
                .contains(CatalogInfoKeys.globalNameMiss(STYLE, "a:b"));
        assertThat(CatalogInfoKeys.keysOf(LAYERGROUP, "group-2", "basemap"))
                .containsExactly(
                        CacheKey.entity(LAYERGROUP, "group-2"),
                        CacheKey.name(LAYERGROUP, "basemap"),
                        CacheKey.tileLayer("group-2"));
        assertThat(CatalogInfoKeys.keysOf(DATASTORE, "store-1", "topp:roads"))
                .containsExactly(CacheKey.entity(DATASTORE, "store-1"), CacheKey.name(DATASTORE, "topp:roads"));
    }

    // the evictions of a layer or group, after a write or a remote event, include its GWC settings
    @Test
    void publishedKeysHaveTileLayer() {
        LayerInfoImpl layer = new LayerInfoImpl();
        layer.setId("layer-1");
        DataStoreInfoImpl store = storeInUnresolvedWorkspace("roads");

        assertThat(CatalogInfoKeys.keysOf(layer)).contains(CacheKey.tileLayer("layer-1"));
        assertThat(CatalogInfoKeys.keysOf(group("group-1", "basemap", null))).contains(CacheKey.tileLayer("group-1"));
        assertThat(CatalogInfoKeys.keysOf(LAYER, "layer-1", "topp:roads")).contains(CacheKey.tileLayer("layer-1"));
        assertThat(CatalogInfoKeys.keysOf(LAYERGROUP, "group-1", "basemap")).contains(CacheKey.tileLayer("group-1"));
        assertThat(CatalogInfoKeys.keysOf(store)).noneMatch(CatalogInfoKeysTest::isTileLayerKey);
        assertThat(CatalogInfoKeys.keysOf(DATASTORE, "store-1", "topp:roads"))
                .noneMatch(CatalogInfoKeysTest::isTileLayerKey);
    }

    // a resource not resolved yet has no name: the layer repository reads it from the cache
    @Test
    void layerHasNameOfResource() {
        FeatureTypeInfoImpl resource = new FeatureTypeInfoImpl(null);
        resource.setName("roads");
        resource.setNamespace(ResolvingProxy.create("ns-1", NamespaceInfo.class));
        LayerInfoImpl layer = new LayerInfoImpl();
        layer.setResource(resource);
        LayerInfoImpl unresolved = new LayerInfoImpl();
        unresolved.setResource(ResolvingProxy.create("roads-1", ResourceInfo.class));

        assertThat(CatalogInfoKeys.hasName(layer, "topp:roads")).isTrue();
        assertThat(CatalogInfoKeys.hasName(unresolved, "topp:roads")).isFalse();
    }

    private static boolean isTileLayerKey(CacheKey key) {
        return key.kind() == Kind.TILE_LAYER;
    }

    private static LayerGroupInfoImpl group(String id, String name, @Nullable WorkspaceInfo workspace) {
        LayerGroupInfoImpl group = new LayerGroupInfoImpl();
        group.setId(id);
        group.setName(name);
        group.setWorkspace(workspace);
        return group;
    }

    private static DataStoreInfoImpl storeInUnresolvedWorkspace(String name) {
        DataStoreInfoImpl store = new DataStoreInfoImpl(null);
        store.setId("store-1");
        store.setName(name);
        store.setWorkspace(ResolvingProxy.create("ws-1", WorkspaceInfo.class));
        return store;
    }
}
