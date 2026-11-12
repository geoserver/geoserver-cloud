/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.requestcache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.geoserver.catalog.CatalogFacade.NO_WORKSPACE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Query;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RequestCachingCatalogFacadeTest {

    private final ExtendedCatalogFacade subject = mock(ExtendedCatalogFacade.class);
    private final RequestCachingCatalogFacade facade = new RequestCachingCatalogFacade(subject);

    private final WorkspaceInfo topp = workspace("topp");
    private final WorkspaceInfo sf = workspace("sf");

    @AfterEach
    void closeScope() {
        RequestCacheScope.close();
    }

    @Test
    void secondGetLayerSkipsDelegate() {
        RequestCacheScope.open(100);
        LayerInfo roads = layer("roads");
        when(subject.getLayer("roads")).thenReturn(roads);

        LayerInfo first = facade.getLayer("roads");
        LayerInfo second = facade.getLayer("roads");

        assertThat(first).isSameAs(roads);
        assertThat(second).isSameAs(roads);
        verify(subject, times(1)).getLayer("roads");
    }

    @Test
    void missSkipsDelegate() {
        RequestCacheScope.open(100);

        LayerInfo first = facade.getLayer("missing");
        LayerInfo second = facade.getLayer("missing");

        assertThat(first).isNull();
        assertThat(second).isNull();
        verify(subject, times(1)).getLayer("missing");
    }

    // the subject answers null: the second lookup finds the cached miss
    @ParameterizedTest
    @MethodSource("lookups")
    void lookupCachedPerRequest(Consumer<ExtendedCatalogFacade> lookup) {
        RequestCacheScope.open(100);

        lookup.accept(facade);
        lookup.accept(facade);

        lookup.accept(verify(subject, times(1)));
    }

    static Stream<Named<Consumer<ExtendedCatalogFacade>>> lookups() {
        WorkspaceInfo topp = workspace("topp");
        NamespaceInfo toppNamespace = namespace("topp");
        return Stream.of(
                lookup("getWorkspace", target -> target.getWorkspace("id")),
                lookup("getNamespace", target -> target.getNamespace("id")),
                lookup("getStore", target -> target.getStore("id", DataStoreInfo.class)),
                lookup("getResource", target -> target.getResource("id", FeatureTypeInfo.class)),
                lookup("getLayer", target -> target.getLayer("id")),
                lookup("getLayerGroup", target -> target.getLayerGroup("id")),
                lookup("getStyle", target -> target.getStyle("id")),
                lookup("getPublished", target -> target.getPublished("id")),
                lookup("getWorkspaceByName", target -> target.getWorkspaceByName("topp")),
                lookup("getNamespaceByPrefix", target -> target.getNamespaceByPrefix("topp")),
                lookup("getStoreByName", target -> target.getStoreByName(topp, "roads", StoreInfo.class)),
                lookup(
                        "getResourceByName",
                        target -> target.getResourceByName(toppNamespace, "roads", ResourceInfo.class)),
                lookup("getLayerByName", target -> target.getLayerByName("topp:roads")),
                lookup("getLayerGroupByName", target -> target.getLayerGroupByName("basemap")),
                lookup("getLayerGroupByName in workspace", target -> target.getLayerGroupByName(topp, "basemap")),
                lookup("getStyleByName without workspace", target -> target.getStyleByName(NO_WORKSPACE, "point")),
                lookup("getStyleByName in workspace", target -> target.getStyleByName(topp, "point")),
                lookup("getDefaultWorkspace", ExtendedCatalogFacade::getDefaultWorkspace),
                lookup("getDefaultNamespace", ExtendedCatalogFacade::getDefaultNamespace),
                lookup("getDefaultDataStore", target -> target.getDefaultDataStore(topp)));
    }

    // the subject answers null: the reads before the write find the cached miss
    @ParameterizedTest(name = "{0}")
    @MethodSource("writes")
    void writeEvictsLookup(Consumer<ExtendedCatalogFacade> read, Consumer<ExtendedCatalogFacade> write) {
        RequestCacheScope.open(100);
        read.accept(facade);
        read.accept(facade);

        write.accept(facade);
        read.accept(facade);

        read.accept(verify(subject, times(2)));
    }

    static Stream<Arguments> writes() {
        WorkspaceInfo topp = workspace("topp");
        WorkspaceInfo sf = workspace("sf");
        NamespaceInfo toppNamespace = namespace("topp");
        DataStoreInfo roadsStore = dataStore(topp, "roads");
        FeatureTypeInfo roadsType = featureType(toppNamespace, "roads");
        LayerInfo roadsLayer = layer(roadsType);
        StyleInfo point = style(null, "point");
        return Stream.of(
                write(
                        "setDefaultNamespace",
                        ExtendedCatalogFacade::getDefaultNamespace,
                        target -> target.setDefaultNamespace(toppNamespace)),
                write(
                        "setDefaultDataStore",
                        target -> target.getDefaultDataStore(topp),
                        target -> target.setDefaultDataStore(topp, roadsStore)),
                write(
                        "namespace prefix",
                        target -> target.getNamespaceByPrefix("states"),
                        target -> target.update(toppNamespace, new Patch().with("prefix", "states"))),
                write(
                        "findAllById by PublishedInfo",
                        target -> target.findAllById(List.of("roads-layer-id"), PublishedInfo.class),
                        target -> target.update(roadsLayer, new Patch().with("enabled", false))),
                write(
                        "store move",
                        target -> target.getResourceByName(toppNamespace, "roads", FeatureTypeInfo.class),
                        target -> target.update(roadsStore, new Patch().with("workspace", sf))),
                write(
                        "workspace removal",
                        target -> target.getStoreByName(topp, "roads", DataStoreInfo.class),
                        target -> target.remove(topp)),
                write(
                        "namespace removal",
                        target -> target.getResourceByName(toppNamespace, "roads", FeatureTypeInfo.class),
                        target -> target.remove(toppNamespace)),
                write(
                        "resource rename",
                        target -> target.getLayer("roads-layer-id"),
                        target -> target.update(roadsType, new Patch().with("name", "streets"))),
                write(
                        "style rename",
                        target -> target.getLayerGroup("basemap-id"),
                        target -> target.update(point, new Patch().with("name", "dot"))),
                write(
                        "layer removal",
                        target -> target.getLayerGroup("basemap-id"),
                        target -> target.remove(roadsLayer)));
    }

    // the name key topp:roads of a layer holds the old name of its workspace
    @Test
    void workspaceRenameEvictsLayers() {
        RequestCacheScope.open(100);
        LayerInfo roads = layer(featureType(namespace("topp"), "roads"));
        when(subject.getLayerByName("topp:roads")).thenReturn(roads);
        facade.getLayerByName("topp:roads");
        facade.getLayerByName("topp:roads");

        facade.update(topp, new Patch().with("name", "states"));
        facade.getLayerByName("topp:roads");

        verify(subject, times(2)).getLayerByName("topp:roads");
    }

    // a layer cached under its own key answers a lookup of its id as a published object
    @Test
    void publishedIdsReadLayerKeys() {
        RequestCacheScope.open(100);
        LayerInfo roads = layer("roads");
        when(subject.getLayer("roads")).thenReturn(roads);
        facade.getLayer("roads");

        List<PublishedInfo> found =
                facade.findAllById(List.of("roads"), PublishedInfo.class).toList();

        assertThat(found).containsExactly(roads);
        verify(subject, never()).findAllById(List.of("roads"), PublishedInfo.class);
    }

    // a cached miss leaves its id out, and an id absent from the answer below is cached as a miss
    @Test
    void findAllByIdFetchesOnlyUncached() {
        RequestCacheScope.open(100);
        LayerInfo roads = layer("roads");
        LayerInfo rivers = layer("rivers");
        List<String> uncached = List.of("rivers", "lakes");
        when(subject.getLayer("roads")).thenReturn(roads);
        when(subject.findAllById(uncached, LayerInfo.class)).thenReturn(Stream.of(rivers));
        facade.getLayer("roads");
        facade.getLayer("missing");

        List<String> ids = List.of("roads", "missing", "rivers", "lakes");
        List<LayerInfo> found = facade.findAllById(ids, LayerInfo.class).toList();
        LayerInfo riversAgain = facade.getLayer("rivers");
        LayerInfo lakes = facade.getLayer("lakes");

        assertThat(found).containsExactlyInAnyOrder(roads, rivers);
        assertThat(riversAgain).isSameAs(rivers);
        assertThat(lakes).isNull();
        verify(subject).findAllById(uncached, LayerInfo.class);
        verify(subject, never()).getLayer("rivers");
        verify(subject, never()).getLayer("lakes");
    }

    // the facade below resolves the group before it gives the layer, and the group looks the layer up meanwhile
    @Test
    void bulkKeepsInstanceCachedFirst() {
        RequestCacheScope.open(100);
        LayerGroupInfo basemap = group(null, "basemap");
        LayerInfo roadsFoundFirst = layer("roads");
        LayerInfo roadsFetched = layer("roads");
        List<String> ids = List.of("basemap-id", "roads");
        when(subject.getLayer("roads")).thenReturn(roadsFoundFirst);
        when(subject.findAllById(ids, PublishedInfo.class))
                .thenAnswer(invocation -> groupLooksRoadsUp(Stream.of(basemap, roadsFetched)));

        List<PublishedInfo> found = facade.findAllById(ids, PublishedInfo.class).toList();
        LayerInfo roadsAgain = facade.getLayer("roads");

        assertThat(found).hasSize(2);
        assertThat(found.get(1)).isSameAs(roadsFoundFirst);
        assertThat(roadsAgain).isSameAs(roadsFoundFirst);
    }

    /** Looks roads up through the facade under test when the stream gives the group, as a resolver would. */
    private Stream<PublishedInfo> groupLooksRoadsUp(Stream<PublishedInfo> fetched) {
        return fetched.peek(info -> {
            if (info instanceof LayerGroupInfo) {
                facade.getLayer("roads");
            }
        });
    }

    // the lookup after the listing goes below once, and the second lookup finds it cached
    @Test
    void listingCachesNoObjects() {
        RequestCacheScope.open(100);
        LayerInfo roads = layer("roads");
        Query<LayerInfo> allLayers = Query.all(LayerInfo.class);
        when(subject.query(allLayers)).thenReturn(Stream.of(roads));
        when(subject.getLayer("roads")).thenReturn(roads);

        List<LayerInfo> listed = facade.query(allLayers).toList();
        LayerInfo first = facade.getLayer("roads");
        LayerInfo second = facade.getLayer("roads");

        assertThat(listed).containsExactly(roads);
        assertThat(first).isSameAs(roads);
        assertThat(second).isSameAs(roads);
        verify(subject, times(1)).getLayer("roads");
    }

    @Test
    void updateEvictsLayer() {
        RequestCacheScope.open(100);
        LayerInfo roads = layer("roads");
        when(subject.getLayer("roads")).thenReturn(roads);
        facade.getLayer("roads");
        facade.getLayer("roads");

        facade.update(roads, new Patch().with("enabled", false));
        facade.getLayer("roads");

        verify(subject, times(2)).getLayer("roads");
    }

    @Test
    void noScopePassesThrough() {
        facade.getLayer("roads");
        facade.getLayer("roads");

        verify(subject, times(2)).getLayer("roads");
    }

    // a worker thread may still hold the cache of a finished request
    @Test
    void deadScopePassesThrough() {
        RequestCacheScope.open(100);
        RequestCache cache = RequestCacheScope.current();
        cache.kill();

        facade.getLayer("roads");
        facade.getLayer("roads");

        verify(subject, times(2)).getLayer("roads");
    }

    // the lookup by StoreInfo caches under STORE, and a write of the data store evicts that key too
    @Test
    void abstractLookupEvictedByWrite() {
        RequestCacheScope.open(100);
        DataStoreInfo roads = dataStore(topp, "roads");
        when(subject.getStore("roads-id", StoreInfo.class)).thenReturn(roads);
        facade.getStore("roads-id", StoreInfo.class);
        facade.getStore("roads-id", StoreInfo.class);

        facade.update(roads, new Patch().with("description", "main roads"));
        facade.getStore("roads-id", StoreInfo.class);

        verify(subject, times(2)).getStore("roads-id", StoreInfo.class);
    }

    @Test
    void publishedChecksLayerThenGroup() {
        RequestCacheScope.open(100);
        LayerInfo roads = layer("roads");
        LayerGroupInfo basemap = group(null, "basemap");
        when(subject.getLayer("roads")).thenReturn(roads);
        when(subject.getLayerGroup("basemap-id")).thenReturn(basemap);
        facade.getLayer("roads");
        facade.getLayerGroup("basemap-id");

        PublishedInfo layer = facade.getPublished("roads");
        PublishedInfo group = facade.getPublished("basemap-id");

        assertThat(layer).isSameAs(roads);
        assertThat(group).isSameAs(basemap);
        verify(subject, never()).getPublished(anyString());
    }

    @Test
    void publishedCachedAsLayer() {
        RequestCacheScope.open(100);
        LayerInfo roads = layer("roads");
        when(subject.getPublished("roads")).thenReturn(roads);
        facade.getPublished("roads");

        LayerInfo layer = facade.getLayer("roads");
        PublishedInfo published = facade.getPublished("roads");

        assertThat(layer).isSameAs(roads);
        assertThat(published).isSameAs(roads);
        verify(subject, never()).getLayer("roads");
        verify(subject, times(1)).getPublished("roads");
    }

    // the catalog looks topp:basemap up without workspace first, then in topp
    @Test
    void globalMissKeepsWorkspaceGroup() {
        RequestCacheScope.open(100);
        LayerGroupInfo basemap = group(topp, "basemap");
        when(subject.getLayerGroupByName(topp, "basemap")).thenReturn(basemap);

        LayerGroupInfo global = facade.getLayerGroupByName(NO_WORKSPACE, "topp:basemap");
        LayerGroupInfo inTopp = facade.getLayerGroupByName(topp, "basemap");

        assertThat(global).isNull();
        assertThat(inTopp).isSameAs(basemap);
    }

    @Test
    void globalGroupCachedByName() {
        RequestCacheScope.open(100);
        LayerGroupInfo basemap = group(null, "basemap");
        when(subject.getLayerGroupByName(NO_WORKSPACE, "basemap")).thenReturn(basemap);
        facade.getLayerGroupByName(NO_WORKSPACE, "basemap");

        LayerGroupInfo found = facade.getLayerGroupByName("basemap");

        assertThat(found).isSameAs(basemap);
        verify(subject, never()).getLayerGroupByName("basemap");
    }

    @Test
    void globalStyleCachedByName() {
        RequestCacheScope.open(100);
        StyleInfo point = style(null, "point");
        when(subject.getStyleByName("point")).thenReturn(point);
        facade.getStyleByName("point");

        StyleInfo byName = facade.getStyleByName("point");
        StyleInfo global = facade.getStyleByName(NO_WORKSPACE, "point");

        assertThat(byName).isSameAs(point);
        assertThat(global).isSameAs(point);
        verify(subject, times(1)).getStyleByName("point");
        verify(subject, never()).getStyleByName(NO_WORKSPACE, "point");
    }

    // without a global style of that name, the bare name finds the style of a workspace
    @Test
    void workspaceStyleNotGlobal() {
        RequestCacheScope.open(100);
        StyleInfo point = style(topp, "point");
        when(subject.getStyleByName("point")).thenReturn(point);

        StyleInfo byName = facade.getStyleByName("point");
        StyleInfo global = facade.getStyleByName(NO_WORKSPACE, "point");

        assertThat(byName).isSameAs(point);
        assertThat(global).isNull();
        verify(subject).getStyleByName(NO_WORKSPACE, "point");
    }

    @Test
    void renameEvictsBothNames() {
        RequestCacheScope.open(100);
        when(subject.getWorkspaceByName("topp")).thenReturn(topp);
        facade.getWorkspaceByName("topp");
        facade.getWorkspaceByName("topp");
        facade.getWorkspaceByName("states");
        facade.getWorkspaceByName("states");

        facade.update(topp, new Patch().with("name", "states"));
        facade.getWorkspaceByName("topp");
        facade.getWorkspaceByName("states");

        verify(subject, times(2)).getWorkspaceByName("topp");
        verify(subject, times(2)).getWorkspaceByName("states");
    }

    // a store moved to another workspace changes the prefix of its name
    @Test
    void moveEvictsPrefixedNames() {
        RequestCacheScope.open(100);
        DataStoreInfo roads = dataStore(topp, "roads");
        when(subject.getStoreByName(topp, "roads", DataStoreInfo.class)).thenReturn(roads);
        facade.getStoreByName(topp, "roads", DataStoreInfo.class);
        facade.getStoreByName(topp, "roads", DataStoreInfo.class);
        facade.getStoreByName(sf, "roads", DataStoreInfo.class);
        facade.getStoreByName(sf, "roads", DataStoreInfo.class);

        facade.update(roads, new Patch().with("workspace", sf));
        facade.getStoreByName(topp, "roads", DataStoreInfo.class);
        facade.getStoreByName(sf, "roads", DataStoreInfo.class);

        verify(subject, times(2)).getStoreByName(topp, "roads", DataStoreInfo.class);
        verify(subject, times(2)).getStoreByName(sf, "roads", DataStoreInfo.class);
    }

    // a layer has the name of its resource
    @Test
    void resourceRenameEvictsLayer() {
        RequestCacheScope.open(100);
        FeatureTypeInfo roadsType = featureType(namespace("topp"), "roads");
        LayerInfo roads = layer(roadsType);
        when(subject.getLayerByName("topp:roads")).thenReturn(roads);
        facade.getLayerByName("topp:roads");
        facade.getLayerByName("topp:roads");

        facade.update(roadsType, new Patch().with("name", "streets"));
        facade.getLayerByName("topp:roads");

        verify(subject, times(2)).getLayerByName("topp:roads");
    }

    @Test
    void addEvictsCachedMiss() {
        RequestCacheScope.open(100);
        StyleInfo point = style(null, "point");
        facade.getStyle("point-id");
        facade.getStyle("point-id");
        facade.getStyleByName(NO_WORKSPACE, "point");
        facade.getStyleByName(NO_WORKSPACE, "point");

        facade.add(point);
        facade.getStyle("point-id");
        facade.getStyleByName(NO_WORKSPACE, "point");

        verify(subject, times(2)).getStyle("point-id");
        verify(subject, times(2)).getStyleByName(NO_WORKSPACE, "point");
    }

    @Test
    void removeEvictsKeys() {
        RequestCacheScope.open(100);
        StyleInfo point = style(null, "point");
        when(subject.getStyle("point-id")).thenReturn(point);
        when(subject.getStyleByName(NO_WORKSPACE, "point")).thenReturn(point);
        facade.getStyle("point-id");
        facade.getStyle("point-id");
        facade.getStyleByName(NO_WORKSPACE, "point");
        facade.getStyleByName(NO_WORKSPACE, "point");

        facade.remove(point);
        facade.getStyle("point-id");
        facade.getStyleByName(NO_WORKSPACE, "point");

        verify(subject, times(2)).getStyle("point-id");
        verify(subject, times(2)).getStyleByName(NO_WORKSPACE, "point");
    }

    @Test
    void setDefaultEvictsLookup() {
        RequestCacheScope.open(100);
        when(subject.getDefaultWorkspace()).thenReturn(topp, sf);
        facade.getDefaultWorkspace();
        facade.getDefaultWorkspace();

        facade.setDefaultWorkspace(sf);
        WorkspaceInfo found = facade.getDefaultWorkspace();

        assertThat(found).isSameAs(sf);
        verify(subject, times(2)).getDefaultWorkspace();
    }

    // the lookup of the default workspace holds the workspace too
    @Test
    void workspaceWriteEvictsDefault() {
        RequestCacheScope.open(100);
        when(subject.getDefaultWorkspace()).thenReturn(topp);
        facade.getDefaultWorkspace();
        facade.getDefaultWorkspace();

        facade.update(topp, new Patch().with("name", "states"));
        facade.getDefaultWorkspace();

        verify(subject, times(2)).getDefaultWorkspace();
    }

    // CatalogInfo has no ClassMappings constant
    @Test
    void unmappedTypePassesThrough() {
        RequestCacheScope.open(100);
        List<String> ids = List.of("roads");
        when(subject.findAllById(ids, CatalogInfo.class)).thenAnswer(invocation -> Stream.empty());

        facade.findAllById(ids, CatalogInfo.class).toList();
        facade.findAllById(ids, CatalogInfo.class).toList();

        verify(subject, times(2)).findAllById(ids, CatalogInfo.class);
    }

    private static Named<Consumer<ExtendedCatalogFacade>> lookup(String name, Consumer<ExtendedCatalogFacade> call) {
        return Named.of(name, call);
    }

    private static Arguments write(
            String name, Consumer<ExtendedCatalogFacade> read, Consumer<ExtendedCatalogFacade> write) {
        return Arguments.of(Named.of(name, read), write);
    }

    private static WorkspaceInfo workspace(String name) {
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId(name + "-id");
        workspace.setName(name);
        return workspace;
    }

    private static NamespaceInfo namespace(String prefix) {
        NamespaceInfoImpl namespace = new NamespaceInfoImpl();
        namespace.setId(prefix + "-id");
        namespace.setPrefix(prefix);
        return namespace;
    }

    private static DataStoreInfo dataStore(WorkspaceInfo workspace, String name) {
        DataStoreInfoImpl store = new DataStoreInfoImpl(null);
        store.setId(name + "-id");
        store.setWorkspace(workspace);
        store.setName(name);
        return store;
    }

    private static FeatureTypeInfo featureType(NamespaceInfo namespace, String name) {
        FeatureTypeInfoImpl featureType = new FeatureTypeInfoImpl(null);
        featureType.setId(name + "-type-id");
        featureType.setNamespace(namespace);
        featureType.setName(name);
        return featureType;
    }

    private static LayerInfo layer(String id) {
        LayerInfoImpl layer = new LayerInfoImpl();
        layer.setId(id);
        return layer;
    }

    private static LayerInfo layer(ResourceInfo resource) {
        LayerInfoImpl layer = new LayerInfoImpl();
        layer.setId(resource.getName() + "-layer-id");
        layer.setResource(resource);
        return layer;
    }

    private static LayerGroupInfo group(@Nullable WorkspaceInfo workspace, String name) {
        LayerGroupInfoImpl group = new LayerGroupInfoImpl();
        group.setId(name + "-id");
        group.setWorkspace(workspace);
        group.setName(name);
        return group;
    }

    private static StyleInfo style(@Nullable WorkspaceInfo workspace, String name) {
        StyleInfoImpl style = new StyleInfoImpl(null);
        style.setId(name + "-id");
        style.setWorkspace(workspace);
        style.setName(name);
        return style;
    }
}
