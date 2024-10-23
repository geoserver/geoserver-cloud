/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.security.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.impl.LayerGroupContainmentCache.LayerGroupSummary;
import org.geotools.api.feature.type.Name;
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
 * Tests {@link LayerGroupContainmentCache} udpates in face of catalog setup and changes
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

    private static Catalog catalog;

    @TempDir
    static Path tmpDir;

    @BeforeAll
    public static void setupBaseCatalog() throws Exception {
        GeoServerExtensionsHelper.setIsSpringContext(false);
        catalog = new CatalogImpl();
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
    public void setupLayerGrups() {
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
    public void clearLayerGroups() {
        CascadeDeleteVisitor remover = new CascadeDeleteVisitor(catalog);
        for (LayerGroupInfo lg : catalog.getLayerGroups()) {
            if (catalog.getLayerGroup(lg.getId()) != null) {
                remover.visit(lg);
            }
        }
    }

    private LayerGroupInfo addLayerGroup(String name, Mode mode, WorkspaceInfo ws, PublishedInfo... layers) {
        CatalogBuilder cb = new CatalogBuilder(catalog);

        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
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
        catalog.add(group);
        if (ws != null) {
            return catalog.getLayerGroupByName(ws.getName(), name);
        } else {
            return catalog.getLayerGroupByName(name);
        }
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

        // container has no contaning groups
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
        final String NAMED_GROUP = "named";
        LayerInfo neatline = catalog.getLayerByName(getLayerId(MockData.MAP_NEATLINE));
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));

        // add and check containment
        LayerGroupInfo named = addLayerGroup(NAMED_GROUP, Mode.NAMED, null, lakes, neatline);
        assertThat(containerNamesForResource(MockData.LAKES), equalTo(set(CONTAINER_GROUP, NAMED_GROUP)));
        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), equalTo(set(NAMED_GROUP)));
        assertThat(containerNamesForGroup(named), empty());

        // delete and check containment
        catalog.remove(named);
        assertThat(containerNamesForResource(MockData.LAKES), equalTo(set(CONTAINER_GROUP)));
        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), empty());
        assertThat(containerNamesForGroup(named), empty());
    }

    @Test
    void testAddRemoveNestedNamed() {
        final String NESTED_NAMED = "nestedNamed";
        LayerInfo neatline = catalog.getLayerByName(getLayerId(MockData.MAP_NEATLINE));
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));

        // add, nest, and check containment
        LayerGroupInfo nestedNamed = addLayerGroup(NESTED_NAMED, Mode.NAMED, null, lakes, neatline);
        container.getLayers().add(nestedNamed);
        container.getStyles().add(null);
        catalog.save(container);
        assertThat(containerNamesForResource(MockData.LAKES), equalTo(set(CONTAINER_GROUP, NESTED_NAMED)));
        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), equalTo(set(CONTAINER_GROUP, NESTED_NAMED)));
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
}
