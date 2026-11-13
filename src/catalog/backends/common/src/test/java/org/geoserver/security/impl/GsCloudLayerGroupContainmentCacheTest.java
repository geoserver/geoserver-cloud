/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Uninterruptibles;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.impl.CatalogPostModifyEventImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.cloud.event.bus.RemoteEventsResumed;
import org.geoserver.cloud.event.catalog.CatalogInfoAdded;
import org.geoserver.cloud.event.catalog.CatalogInfoModified;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoved;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.impl.LayerGroupContainmentCache.LayerGroupSummary;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.data.property.PropertyDataStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.context.WebApplicationContext;

/**
 * Tests {@link LayerGroupContainmentCache} updates in face of catalog setup and changes
 *
 * <p>copied and adapted from {@link LayerGroupContainmentCacheTest}
 */
class GsCloudLayerGroupContainmentCacheTest {

    private static final String WS = "ws";

    private static final String ANOTHER_WS = "anotherWs";

    private static final String NATURE_GROUP = "nature";

    private static final String CONTAINER_GROUP = "containerGroup";

    private GsCloudLayerGroupContainmentCache cc;

    private LayerGroupInfo nature;

    private LayerGroupInfo container;

    private static CatalogPlugin catalog;

    @TempDir
    static Path tmpDir;

    @BeforeAll
    static void setupBaseCatalog() throws Exception {
        GeoServerExtensionsHelper.setIsSpringContext(false);
        catalog = new CatalogPlugin();
        catalog.setResourceLoader(new GeoServerResourceLoader());

        // the workspace
        addWorkspaceNamespace(WS);
        addWorkspaceNamespace(ANOTHER_WS);

        // the builder
        CatalogBuilder cb = new CatalogBuilder(catalog);
        final WorkspaceInfo defaultWorkspace = catalog.getDefaultWorkspace();
        cb.setWorkspace(defaultWorkspace);

        // setup the store
        String nsURI = catalog.getDefaultNamespace().getURI();

        var resolver = new PathMatchingResourcePatternResolver(MockData.class.getClassLoader());
        Resource[] resources = resolver.getResources("classpath:org/geoserver/data/test/*.properties");
        List<String> propFiles = Stream.of(resources).map(Resource::getFilename).toList();
        propFiles.stream()
                .filter(f -> !f.equals("MarsPoi.properties")) // crs 49900 unknown
                .forEach(GsCloudLayerGroupContainmentCacheTest::unpackTestData);

        File testData = tmpDir.toFile();
        DataStoreInfo storeInfo = cb.buildDataStore("store");
        storeInfo.getConnectionParameters().put("directory", testData);
        storeInfo.getConnectionParameters().put("namespace", nsURI);
        catalog.save(storeInfo);

        // setup all the layers
        PropertyDataStore store = new PropertyDataStore(testData);
        store.setNamespaceURI(nsURI);
        cb.setStore(catalog.getDefaultDataStore(defaultWorkspace));
        for (Name name : store.getNames()) {
            FeatureTypeInfo ft = cb.buildFeatureType(name);
            cb.setupBounds(ft);
            catalog.add(ft);
            LayerInfo layer = cb.buildLayer(ft);
            catalog.add(layer);
        }
    }

    private static void unpackTestData(String propsfileName) {
        Path target = tmpDir.resolve(propsfileName);
        try (InputStream in = MockData.class.getResourceAsStream(propsfileName)) {
            assertNotNull(in);
            Files.copy(in, target);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void addWorkspaceNamespace(String wsName) {
        WorkspaceInfoImpl ws = new WorkspaceInfoImpl();
        ws.setName(wsName);
        catalog.add(ws);
        NamespaceInfo ns = new NamespaceInfoImpl();
        ns.setPrefix(wsName);
        ns.setURI("http://www.geoserver.org/" + wsName);
        catalog.add(ns);
    }

    @BeforeEach
    void setupLayerGrups() {
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerInfo forests = catalog.getLayerByName(getLayerId(MockData.FORESTS));
        LayerInfo roads = catalog.getLayerByName(getLayerId(MockData.ROAD_SEGMENTS));
        WorkspaceInfo ws = catalog.getDefaultWorkspace();

        this.nature = addLayerGroup(NATURE_GROUP, Mode.SINGLE, ws, lakes, forests);
        this.container = addLayerGroup(CONTAINER_GROUP, Mode.CONTAINER, null, nature, roads);

        cc = new GsCloudLayerGroupContainmentCache(catalog);

        /*
         * Call onApplicationEvent(ContextRefreshedEvent), this version of LayerGroupContainmentCache
         * does not create the cache twice, both at the constructor and on context refresh
         */
        ContextRefreshedEvent contextRefreshedEvent = mock(ContextRefreshedEvent.class);
        WebApplicationContext context = mock(WebApplicationContext.class);
        when(contextRefreshedEvent.getApplicationContext()).thenReturn(context);
        cc.setApplicationContext(context);
        cc.onApplicationEvent(contextRefreshedEvent);
        cc.buildTask.orTimeout(2, TimeUnit.SECONDS).join();
    }

    @AfterEach
    void clearLayerGroups() {
        CascadeDeleteVisitor remover = new CascadeDeleteVisitor(catalog);
        for (LayerGroupInfo lg : catalog.getLayerGroups()) {
            if (catalog.getLayerGroup(lg.getId()) != null) {
                remover.visit(lg);
            }
        }
    }

    private LayerGroupInfo addLayerGroup(String name, Mode mode, WorkspaceInfo ws, PublishedInfo... layers) {
        LayerGroupInfo group = newLayerGroup(name, mode, ws, layers);
        catalog.add(group);
        if (ws != null) {
            return catalog.getLayerGroupByName(ws.getName(), name);
        } else {
            return catalog.getLayerGroupByName(name);
        }
    }

    private LayerGroupInfoImpl newLayerGroup(String name, Mode mode, WorkspaceInfo ws, PublishedInfo... layers) {
        CatalogBuilder cb = new CatalogBuilder(catalog);

        LayerGroupInfoImpl group = new LayerGroupInfoImpl();
        group.setName(name);
        group.setMode(mode);
        if (ws != null) {
            group.setWorkspace(ws);
        }
        if (layers != null) {
            for (PublishedInfo layer : layers) {
                group.getLayers().add(layer);
                group.getStyles().add(null);
            }
        }
        try {
            cb.calculateLayerGroupBounds(group);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return group;
    }

    private Set<String> set(String... names) {
        if (names == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(Arrays.asList(names));
    }

    private Set<String> containerNamesForGroup(LayerGroupInfo lg) {
        Collection<LayerGroupSummary> summaries = cc.getContainerGroupsFor(lg);
        return summaries.stream().map(gs -> gs.prefixedName()).collect(Collectors.toSet());
    }

    private Set<String> containerNamesForResource(QName name) {
        Collection<LayerGroupSummary> summaries = cc.getContainerGroupsFor(getResource(name));
        return summaries.stream().map(gs -> gs.prefixedName()).collect(Collectors.toSet());
    }

    private FeatureTypeInfo getResource(QName name) {
        return catalog.getResourceByName(getLayerId(name), FeatureTypeInfo.class);
    }

    private String getLayerId(QName name) {
        return "ws:" + name.getLocalPart();
    }

    @Test
    void buildLayerGroupCaches() {
        GsCloudLayerGroupContainmentCache layerGroupContainmentCache = new GsCloudLayerGroupContainmentCache(catalog);
        ContextRefreshedEvent contextRefreshedEvent = mock(ContextRefreshedEvent.class);
        WebApplicationContext context = mock(WebApplicationContext.class);
        when(contextRefreshedEvent.getApplicationContext()).thenReturn(context);

        layerGroupContainmentCache.setApplicationContext(context);
        layerGroupContainmentCache.onApplicationEvent(contextRefreshedEvent);
        layerGroupContainmentCache.buildTask.orTimeout(2, TimeUnit.SECONDS).join();

        assertEquals(2, layerGroupContainmentCache.groupCache.size());
    }

    // the second request cancels the first build, and builds from a listing of its own after it
    @Test
    void overlappingRebuildFinishes() {
        CountDownLatch firstBuildWaits = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CloseableIterator<LayerGroupInfo> firstListing = blockedListing(firstBuildWaits, release, nature);
        CloseableIterator<LayerGroupInfo> secondListing =
                new CloseableIteratorAdapter<>(List.of(nature, container).iterator());
        Catalog rawCatalog = mock(Catalog.class);
        when(rawCatalog.list(LayerGroupInfo.class, Filter.INCLUDE)).thenReturn(firstListing, secondListing);
        GsCloudLayerGroupContainmentCache cache = new GsCloudLayerGroupContainmentCache(rawCatalog);

        requestBuild(cache);
        assertTrue(Uninterruptibles.awaitUninterruptibly(firstBuildWaits, 2, TimeUnit.SECONDS));
        requestBuild(cache);
        release.countDown();
        cache.buildTask.orTimeout(2, TimeUnit.SECONDS).join();

        assertEquals(Set.of(nature.getId(), container.getId()), cache.groupCache.keySet());
    }

    // the third request cancels the second build before it starts: the second build lists nothing
    @Test
    void cancelledBuildKeepsMaps() {
        CountDownLatch firstBuildWaits = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CloseableIterator<LayerGroupInfo> firstListing = blockedListing(firstBuildWaits, release, nature);
        CloseableIterator<LayerGroupInfo> lastListing =
                new CloseableIteratorAdapter<>(List.of(nature, container).iterator());
        Catalog rawCatalog = mock(Catalog.class);
        when(rawCatalog.list(LayerGroupInfo.class, Filter.INCLUDE)).thenReturn(firstListing, lastListing);
        GsCloudLayerGroupContainmentCache cache = new GsCloudLayerGroupContainmentCache(rawCatalog);

        requestBuild(cache);
        assertTrue(Uninterruptibles.awaitUninterruptibly(firstBuildWaits, 2, TimeUnit.SECONDS));
        requestBuild(cache);
        requestBuild(cache);
        release.countDown();
        cache.buildTask.orTimeout(2, TimeUnit.SECONDS).join();

        assertEquals(Set.of(nature.getId(), container.getId()), cache.groupCache.keySet());
        verify(rawCatalog, times(2)).list(LayerGroupInfo.class, Filter.INCLUDE);
    }

    // a change made through another instance while the bus was away reached no listener
    @Test
    void resumedEventsRebuildCache() {
        LayerInfo neatline = catalog.getLayerByName(getLayerId(MockData.MAP_NEATLINE));
        remoteChange(nature, group -> group.getLayers().set(0, neatline));

        cc.onRemoteEventsResumed(new RemoteEventsResumed("geoserver.anonymous.queue"));
        cc.buildTask.orTimeout(2, TimeUnit.SECONDS).join();

        assertThat(containerNamesForResource(MockData.LAKES), empty());
        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), equalTo(set(CONTAINER_GROUP)));
    }

    // the access checks read the previous maps while a build lists the groups
    @Test
    void rebuildKeepsPreviousMaps() {
        CountDownLatch secondBuildWaits = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CloseableIterator<LayerGroupInfo> firstListing =
                new CloseableIteratorAdapter<>(List.of(nature, container).iterator());
        CloseableIterator<LayerGroupInfo> secondListing = blockedListing(secondBuildWaits, release, nature, container);
        Catalog rawCatalog = mock(Catalog.class);
        when(rawCatalog.list(LayerGroupInfo.class, Filter.INCLUDE)).thenReturn(firstListing, secondListing);
        GsCloudLayerGroupContainmentCache cache = new GsCloudLayerGroupContainmentCache(rawCatalog);
        requestBuild(cache);
        cache.buildTask.orTimeout(2, TimeUnit.SECONDS).join();

        cache.onRemoteEventsResumed(new RemoteEventsResumed("geoserver.anonymous.queue"));
        assertTrue(Uninterruptibles.awaitUninterruptibly(secondBuildWaits, 2, TimeUnit.SECONDS));
        Set<String> groupsDuringBuild = Set.copyOf(cache.groupCache.keySet());
        release.countDown();
        cache.buildTask.orTimeout(2, TimeUnit.SECONDS).join();

        assertEquals(Set.of(nature.getId(), container.getId()), groupsDuringBuild);
    }

    // the listing read the container before its removal: the build applies the removal again
    @Test
    void changeDuringBuildKept() {
        CountDownLatch buildWaits = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CloseableIterator<LayerGroupInfo> staleListing = blockedListing(buildWaits, release, nature, container);
        Catalog rawCatalog = mock(Catalog.class, delegatesTo(catalog));
        doReturn(staleListing).when(rawCatalog).list(LayerGroupInfo.class, Filter.INCLUDE);
        GsCloudLayerGroupContainmentCache cache = new GsCloudLayerGroupContainmentCache(rawCatalog);

        requestBuild(cache);
        assertTrue(Uninterruptibles.awaitUninterruptibly(buildWaits, 2, TimeUnit.SECONDS));
        cache.onRemoteGroupRemoved(remoteRemove(container));
        release.countDown();
        cache.buildTask.orTimeout(2, TimeUnit.SECONDS).join();

        assertThat(cache.getContainerGroupsFor(getResource(MockData.ROAD_SEGMENTS)), empty());
        assertEquals(Set.of(nature.getId()), cache.groupCache.keySet());
    }

    /** Requests a build of the cache, as the refresh of the web application context does. */
    private static void requestBuild(GsCloudLayerGroupContainmentCache cache) {
        WebApplicationContext context = mock(WebApplicationContext.class);
        cache.setApplicationContext(context);
        cache.onApplicationEvent(new ContextRefreshedEvent(context));
    }

    /** Lists the groups once released. The listing tells when a build waits for the release. */
    private static CloseableIterator<LayerGroupInfo> blockedListing(
            CountDownLatch waiting, CountDownLatch release, LayerGroupInfo... groups) {
        Iterator<LayerGroupInfo> listed = List.of(groups).iterator();
        Iterator<LayerGroupInfo> blocked = new Iterator<>() {
            @Override
            public boolean hasNext() {
                waiting.countDown();
                Uninterruptibles.awaitUninterruptibly(release, 2, TimeUnit.SECONDS);
                return listed.hasNext();
            }

            @Override
            public LayerGroupInfo next() {
                return listed.next();
            }
        };
        return new CloseableIteratorAdapter<>(blocked);
    }

    @Test
    void testInitialSetup() {
        // nature
        Collection<LayerGroupSummary> natureContainers = cc.getContainerGroupsFor(nature);
        assertEquals(1, natureContainers.size());
        assertThat(natureContainers, contains(new LayerGroupSummary(container)));
        LayerGroupSummary summary = natureContainers.iterator().next();
        assertNull(summary.getWorkspace());
        assertEquals(CONTAINER_GROUP, summary.getName());
        assertThat(summary.getContainerGroups(), empty());

        // container has no containing groups
        assertThat(cc.getContainerGroupsFor(container), empty());

        // now check the groups containing the layers (nature being SINGLE, not a container)
        assertThat(containerNamesForResource(MockData.LAKES), equalTo(set(CONTAINER_GROUP)));
        assertThat(containerNamesForResource(MockData.FORESTS), equalTo(set(CONTAINER_GROUP)));
        assertThat(containerNamesForResource(MockData.ROAD_SEGMENTS), equalTo(set(CONTAINER_GROUP)));
    }

    @Test
    void testAddLayerToNature() {
        LayerInfo neatline = catalog.getLayerByName(getLayerId(MockData.MAP_NEATLINE));
        nature.getLayers().add(neatline);
        nature.getStyles().add(null);
        catalog.save(nature);

        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), equalTo(set(CONTAINER_GROUP)));
    }

    @Test
    void testAddLayerToContainer() {
        LayerInfo neatline = catalog.getLayerByName(getLayerId(MockData.MAP_NEATLINE));
        container.getLayers().add(neatline);
        container.getStyles().add(null);
        catalog.save(container);

        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), equalTo(set(CONTAINER_GROUP)));
    }

    @Test
    void testRemoveLayerFromNature() {
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        nature.getLayers().remove(lakes);
        nature.getStyles().remove(0);
        catalog.save(nature);

        assertThat(containerNamesForResource(MockData.LAKES), empty());
        assertThat(containerNamesForResource(MockData.FORESTS), equalTo(set(CONTAINER_GROUP)));
        assertThat(containerNamesForResource(MockData.ROAD_SEGMENTS), equalTo(set(CONTAINER_GROUP)));
    }

    @Test
    void testRemoveLayerFromContainer() {
        LayerInfo roads = catalog.getLayerByName(getLayerId(MockData.ROAD_SEGMENTS));
        container.getLayers().remove(roads);
        container.getStyles().remove(0);
        catalog.save(container);

        assertThat(containerNamesForResource(MockData.LAKES), equalTo(set(CONTAINER_GROUP)));
        assertThat(containerNamesForResource(MockData.FORESTS), equalTo(set(CONTAINER_GROUP)));
        assertThat(containerNamesForResource(MockData.ROAD_SEGMENTS), empty());
    }

    @Test
    void testRemoveNatureFromContainer() {
        container.getLayers().remove(nature);
        container.getStyles().remove(0);
        catalog.save(container);

        assertThat(containerNamesForGroup(nature), empty());
        assertThat(containerNamesForResource(MockData.LAKES), empty());
        assertThat(containerNamesForResource(MockData.FORESTS), empty());
        assertThat(containerNamesForResource(MockData.ROAD_SEGMENTS), equalTo(set(CONTAINER_GROUP)));
    }

    @Test
    void testRemoveAllGrups() {
        catalog.remove(container);
        catalog.remove(nature);

        assertThat(containerNamesForGroup(nature), empty());
        assertThat(containerNamesForResource(MockData.LAKES), empty());
        assertThat(containerNamesForResource(MockData.FORESTS), empty());
        assertThat(containerNamesForResource(MockData.ROAD_SEGMENTS), empty());
    }

    @Test
    void testAddRemoveNamed() {
        final String namedGroup = "named";
        LayerInfo neatline = catalog.getLayerByName(getLayerId(MockData.MAP_NEATLINE));
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));

        // add and check containment
        LayerGroupInfo named = addLayerGroup(namedGroup, Mode.NAMED, null, lakes, neatline);
        assertThat(containerNamesForResource(MockData.LAKES), equalTo(set(CONTAINER_GROUP, namedGroup)));
        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), equalTo(set(namedGroup)));
        assertThat(containerNamesForGroup(named), empty());

        // delete and check containment
        catalog.remove(named);
        assertThat(containerNamesForResource(MockData.LAKES), equalTo(set(CONTAINER_GROUP)));
        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), empty());
        assertThat(containerNamesForGroup(named), empty());
    }

    @Test
    void testAddRemoveNestedNamed() {
        final String nestedName = "nestedNamed";
        LayerInfo neatline = catalog.getLayerByName(getLayerId(MockData.MAP_NEATLINE));
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));

        // add, nest, and check containment
        LayerGroupInfo nestedNamed = addLayerGroup(nestedName, Mode.NAMED, null, lakes, neatline);
        container.getLayers().add(nestedNamed);
        container.getStyles().add(null);
        catalog.save(container);
        assertThat(containerNamesForResource(MockData.LAKES), equalTo(set(CONTAINER_GROUP, nestedName)));
        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), equalTo(set(CONTAINER_GROUP, nestedName)));
        assertThat(containerNamesForGroup(nestedNamed), equalTo(set(CONTAINER_GROUP)));

        // delete and check containment
        new CascadeDeleteVisitor(catalog).visit(nestedNamed);
        assertThat(containerNamesForResource(MockData.LAKES), equalTo(set(CONTAINER_GROUP)));
        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), empty());
        assertThat(containerNamesForGroup(nestedNamed), empty());
    }

    @Test
    void testRenameGroup() {
        nature.setName("renamed");
        catalog.save(nature);

        LayerGroupSummary summary = cc.groupCache.get(nature.getId());
        assertEquals("renamed", summary.getName());
        assertEquals(WS, summary.getWorkspace());
    }

    @Test
    void testRenameWorkspace() {
        WorkspaceInfo ws = catalog.getDefaultWorkspace();
        ws.setName("renamed");
        try {
            catalog.save(ws);

            LayerGroupSummary summary = cc.groupCache.get(nature.getId());
            assertEquals(NATURE_GROUP, summary.getName());
            assertEquals("renamed", summary.getWorkspace());
        } finally {
            ws.setName(WS);
            catalog.save(ws);
        }
    }

    @Test
    void remoteWorkspaceRename() {
        WorkspaceInfo ws = catalog.getDefaultWorkspace();
        try {
            cc.onRemoteWorkspaceRenamed(remoteChange(ws, workspace -> workspace.setName("renamed")));

            LayerGroupSummary summary = cc.groupCache.get(nature.getId());
            assertEquals(NATURE_GROUP, summary.getName());
            assertEquals("renamed", summary.getWorkspace());
        } finally {
            WorkspaceInfo renamed = catalog.getWorkspaceByName("renamed");
            renamed.setName(WS);
            catalog.save(renamed);
        }
    }

    @Test
    void testChangeWorkspace() {
        DataStoreInfo store = catalog.getDataStores().get(0);
        try {
            WorkspaceInfo aws = catalog.getWorkspaceByName(ANOTHER_WS);
            store.setWorkspace(aws);
            catalog.save(store);
            nature.setWorkspace(aws);
            catalog.save(nature);

            LayerGroupSummary summary = cc.groupCache.get(nature.getId());
            assertEquals(NATURE_GROUP, summary.getName());
            assertEquals(ANOTHER_WS, summary.getWorkspace());
        } finally {
            WorkspaceInfo ws = catalog.getWorkspaceByName(WS);
            store.setWorkspace(ws);
            catalog.save(store);
        }
    }

    @Test
    void testChangeGroupMode() {
        LayerGroupSummary summary = cc.groupCache.get(nature.getId());
        assertEquals(Mode.SINGLE, summary.getMode());

        nature.setMode(Mode.OPAQUE_CONTAINER);
        catalog.save(nature);

        summary = cc.groupCache.get(nature.getId());
        assertEquals(Mode.OPAQUE_CONTAINER, summary.getMode());
    }

    @Test
    void remoteAddRegistersGroup() {
        LayerInfo neatline = catalog.getLayerByName(getLayerId(MockData.MAP_NEATLINE));
        LayerGroupInfoImpl named = newLayerGroup("named", Mode.NAMED, null, neatline);

        cc.onRemoteGroupAdded(remoteAdd(named));

        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), equalTo(set("named")));
    }

    // the event tells the new members only: lakes leaves nature, and nature stays inside the container group
    @Test
    void remoteModifyUpdatesMembers() {
        LayerInfo neatline = catalog.getLayerByName(getLayerId(MockData.MAP_NEATLINE));

        cc.onRemoteGroupModified(remoteChange(nature, group -> group.getLayers().set(0, neatline)));

        assertThat(containerNamesForResource(MockData.LAKES), empty());
        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), equalTo(set(CONTAINER_GROUP)));
        assertThat(containerNamesForGroup(nature), equalTo(set(CONTAINER_GROUP)));
    }

    @Test
    void remoteRenameUpdatesSummary() {
        cc.onRemoteGroupModified(remoteChange(nature, group -> group.setName("renamed")));

        LayerGroupSummary summary = cc.groupCache.get(nature.getId());
        assertEquals("renamed", summary.getName());
        assertEquals(WS, summary.getWorkspace());
    }

    @Test
    void remoteModeChangeUpdatesSummary() {
        cc.onRemoteGroupModified(remoteChange(nature, group -> group.setMode(Mode.OPAQUE_CONTAINER)));

        LayerGroupSummary summary = cc.groupCache.get(nature.getId());
        assertEquals(Mode.OPAQUE_CONTAINER, summary.getMode());
    }

    // the summary of a group holds neither its title nor its other properties
    @Test
    void remoteTitleChangeIgnored() {
        Catalog recordingCatalog = mock(Catalog.class, delegatesTo(catalog));
        cc = new GsCloudLayerGroupContainmentCache(recordingCatalog);

        cc.onRemoteGroupModified(remoteChange(nature, group -> group.setTitle("Nature")));

        verify(recordingCatalog, never()).getLayerGroup(nature.getId());
    }

    // a failing lookup must not stop the later listeners of the event
    @Test
    void failingLookupKeepsCache() {
        Catalog failingCatalog = mock(Catalog.class, delegatesTo(catalog));
        doThrow(new IllegalStateException("lookup failed")).when(failingCatalog).getLayerGroup(anyString());
        cc = new GsCloudLayerGroupContainmentCache(failingCatalog);
        requestBuild(cc);
        cc.buildTask.orTimeout(2, TimeUnit.SECONDS).join();
        LayerInfo neatline = catalog.getLayerByName(getLayerId(MockData.MAP_NEATLINE));
        CatalogInfoModified event =
                remoteChange(nature, group -> group.getLayers().set(0, neatline));

        assertDoesNotThrow(() -> cc.onRemoteGroupModified(event));

        assertThat(containerNamesForResource(MockData.LAKES), equalTo(set(CONTAINER_GROUP)));
    }

    @Test
    void remoteRemoveClearsGroup() {
        cc.onRemoteGroupRemoved(remoteRemove(container));

        assertThat(containerNamesForResource(MockData.ROAD_SEGMENTS), empty());
        assertThat(containerNamesForResource(MockData.LAKES), empty());
        assertThat(containerNamesForGroup(nature), empty());
    }

    // the catalog listener registers a group added on this instance: the remote listeners ignore its local event
    @Test
    void localEventsStillHandledOnce() {
        Catalog recordingCatalog = mock(Catalog.class, delegatesTo(catalog));
        cc = new GsCloudLayerGroupContainmentCache(recordingCatalog);
        LayerInfo neatline = catalog.getLayerByName(getLayerId(MockData.MAP_NEATLINE));

        LayerGroupInfo named = addLayerGroup("named", Mode.NAMED, null, neatline);
        cc.onRemoteGroupAdded(CatalogInfoAdded.createLocal(1L, named));

        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), equalTo(set("named")));
        verify(recordingCatalog, never()).getLayerGroup(named.getId());
    }

    /**
     * Adds the group as a change made through another service instance arrives: the backend holds the group, the
     * catalog notifies no listener, and the bus delivers the returned remote event. The other instance gives the group
     * its id.
     */
    private static CatalogInfoAdded remoteAdd(LayerGroupInfoImpl group) {
        group.setId("remote-" + group.getName());
        catalog.getFacade().add(group);
        return remote(CatalogInfoAdded.createLocal(1L, group));
    }

    /**
     * Changes the object as a change made through another service instance arrives. The object comes from the catalog,
     * as a proxy recording the change. The event is built from the post-modify event of the change, as the publisher of
     * the catalog events builds it: the patch holds the changed properties only.
     */
    private static <T extends CatalogInfo> CatalogInfoModified remoteChange(T info, Consumer<T> change) {
        change.accept(info);
        ModificationProxy proxy = ModificationProxy.handler(info);
        CatalogPostModifyEventImpl postModify = new CatalogPostModifyEventImpl();
        postModify.setSource(info);
        postModify.setPropertyNames(proxy.getPropertyNames());
        postModify.setOldValues(proxy.getOldValues());
        postModify.setNewValues(proxy.getNewValues());
        CatalogInfoModified event = CatalogInfoModified.createLocal(1L, postModify);
        catalog.getFacade().update(info, event.getPatch());
        return remote(event);
    }

    /** Removes the group as a change made through another service instance arrives. */
    private static CatalogInfoRemoved remoteRemove(LayerGroupInfo group) {
        catalog.getFacade().remove(group);
        return remote(CatalogInfoRemoved.createLocal(1L, group));
    }

    private static <E extends GeoServerEvent> E remote(E event) {
        event.setRemote(true);
        return event;
    }
}
