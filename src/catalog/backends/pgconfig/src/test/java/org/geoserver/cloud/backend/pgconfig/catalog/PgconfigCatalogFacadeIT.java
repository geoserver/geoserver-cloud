/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.resolving.ProxyUtils;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.cloud.backend.pgconfig.support.StatementCounter;
import org.geotools.api.filter.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class PgconfigCatalogFacadeIT {

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    private StatementCounter counter;
    private CatalogPlugin catalog;
    private ExtendedCatalogFacade facade;
    private String layerId;
    private String layer2Id;
    private String groupId;
    private String s1Id;
    private String s2Id;

    @BeforeEach
    void setUp() {
        counter = new StatementCounter(db.getDataSource());
        catalog = new PgconfigBackendBuilder(counter.dataSource()).createCatalog();
        facade = (ExtendedCatalogFacade) catalog.getRawFacade();
        createWorkspace("ws");
        s1Id = addStyle("s1").getId();
        s2Id = addStyle("s2").getId();
        LayerInfo layer = addLayer("l1");
        layerId = layer.getId();
        layer2Id = addLayer("l2").getId();
        groupId = addGroup("g1", layer).getId();
        counter.reset();
    }

    @Test
    // 1 for the group row, 1 for its members
    void getPublishedGroupTwoQueries() {
        PublishedInfo found = facade.getPublished(groupId);

        assertThat(found).isInstanceOf(LayerGroupInfo.class);
        assertThat(counter.count()).isEqualTo(2);
    }

    @Test
    void getPublishedLayerOneQuery() {
        assertThat(facade.getPublished(layerId)).isInstanceOf(LayerInfo.class);
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void getPublishedUnknownIsNull() {
        assertThat(facade.getPublished("nope")).isNull();
    }

    @Test
    // 1 for the ids, 1 for the members of the group
    void publishedByIdTwoQueries() {
        List<String> ids = List.of(layerId, layer2Id, groupId, "nope");
        List<PublishedInfo> found = findAll(ids, PublishedInfo.class);
        assertThat(found).extracting(PublishedInfo::getId).containsExactlyInAnyOrder(layerId, layer2Id, groupId);
        assertThat(counter.count()).isEqualTo(2);
    }

    @Test
    void stylesByIdOneQuery() {
        List<StyleInfo> found = findAll(List.of(s1Id, s2Id), StyleInfo.class);
        assertThat(found).extracting(StyleInfo::getId).containsExactlyInAnyOrder(s1Id, s2Id);
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    // no repository holds all the types, each id is looked up on its own
    void catalogInfoTypeFindsAnyType() {
        List<CatalogInfo> found = findAll(List.of(layerId, s1Id), CatalogInfo.class);
        assertThat(found).extracting(CatalogInfo::getId).containsExactlyInAnyOrder(layerId, s1Id);
    }

    @Test
    // more ids than the 65,535 bind parameters accepted for one statement
    void manyIdsInOneQuery() {
        List<String> unknownIds =
                IntStream.range(0, 70_000).mapToObj("id-%d"::formatted).toList();
        List<String> ids = new ArrayList<>(unknownIds);
        ids.add(layerId);

        assertThat(findAll(ids, PublishedInfo.class))
                .extracting(PublishedInfo::getId)
                .containsExactly(layerId);
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void emptyIdsRunNoQuery() {
        assertThat(findAll(List.of(), PublishedInfo.class)).isEmpty();
        assertThat(counter.count()).isZero();
    }

    @Test
    void listPublishedResolvesMembers() {
        Filter groups = Predicates.isInstanceOf(LayerGroupInfo.class);
        try (CloseableIterator<PublishedInfo> found = catalog.list(PublishedInfo.class, groups)) {
            LayerGroupInfo group = (LayerGroupInfo) found.next();
            PublishedInfo member = group.getLayers().get(0);

            assertThat(ProxyUtils.isResolvingProxy(member)).isFalse();
            assertThat(member).isInstanceOf(LayerInfo.class);
            assertThat(member.getName()).isEqualTo("l1");
        }
    }

    @Test
    void nestedGroupLoadsInEightQueries() {
        String containerId = addNestedGroups();
        counter.reset();

        LayerGroupInfo container = catalog.getLayerGroup(containerId);

        // 1 container row + 1 for its 3 children + 3 x (1 members + 1 styles)
        assertThat(counter.count()).isEqualTo(8);
        LayerGroupInfo child = (LayerGroupInfo) container.getLayers().get(0);
        assertThat(child.getStyles()).extracting(this::nameOrNull).containsExactly("s1", null);
    }

    private <T extends CatalogInfo> List<T> findAll(List<String> ids, Class<T> type) {
        try (Stream<T> found = facade.findAllById(ids, type)) {
            return found.toList();
        }
    }

    private String nameOrNull(StyleInfo style) {
        if (style == null) {
            return null;
        }
        return style.getName();
    }

    private String addNestedGroups() {
        LayerInfo l1 = catalog.getLayer(layerId);
        LayerInfo l2 = catalog.getLayer(layer2Id);
        StyleInfo s1 = catalog.getStyle(s1Id);
        LayerGroupInfo container = catalog.getFactory().createLayerGroup();
        container.setName("container");
        container.setMode(LayerGroupInfo.Mode.CONTAINER);
        for (String name : List.of("n1", "n2", "n3")) {
            LayerGroupInfo child = catalog.getFactory().createLayerGroup();
            child.setName(name);
            child.getLayers().addAll(List.of(l1, l2));
            child.getStyles().addAll(Arrays.asList(s1, null));
            catalog.add(child);
            container.getLayers().add(catalog.getLayerGroupByName(name));
            container.getStyles().add(null);
        }
        catalog.add(container);
        return catalog.getLayerGroupByName("container").getId();
    }

    private void createWorkspace(String name) {
        CatalogFactory factory = catalog.getFactory();
        WorkspaceInfo workspace = factory.createWorkspace();
        workspace.setName(name);
        catalog.add(workspace);

        NamespaceInfo namespace = factory.createNamespace();
        namespace.setPrefix(name);
        namespace.setURI("http://%s.example".formatted(name));
        catalog.add(namespace);
    }

    private StyleInfo addStyle(String name) {
        StyleInfo style = catalog.getFactory().createStyle();
        style.setName(name);
        style.setFilename("%s.sld".formatted(name));
        catalog.add(style);
        return catalog.getStyleByName(name);
    }

    private LayerInfo addLayer(String name) {
        CatalogFactory factory = catalog.getFactory();

        DataStoreInfo store = factory.createDataStore();
        store.setName("%s-store".formatted(name));
        store.setWorkspace(catalog.getWorkspaceByName("ws"));
        store.setEnabled(true);
        catalog.add(store);

        FeatureTypeInfo ft = factory.createFeatureType();
        ft.setName(name);
        ft.setNativeName(name);
        ft.setStore(store);
        ft.setNamespace(catalog.getNamespaceByPrefix("ws"));
        ft.setEnabled(true);
        catalog.add(ft);

        LayerInfo layer = factory.createLayer();
        layer.setResource(ft);
        layer.setDefaultStyle(catalog.getStyleByName("s1"));
        catalog.add(layer);
        return catalog.getLayerByName("ws:" + name);
    }

    private LayerGroupInfo addGroup(String name, LayerInfo member) {
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.setName(name);
        group.getLayers().add(member);
        group.getStyles().add(null);
        catalog.add(group);
        return catalog.getLayerGroupByName(name);
    }
}
