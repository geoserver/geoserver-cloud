/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYER;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.Query;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.Lookup;
import org.geoserver.cloud.backend.pgconfig.cache.NoopCodec;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.cloud.backend.pgconfig.support.StatementCounter;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class PgconfigLayerRepositoryIT {

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    private StatementCounter counter;
    private CatalogPlugin catalog;

    @BeforeEach
    void setUp() {
        counter = new StatementCounter(db.getDataSource());
        catalog = new PgconfigBackendBuilder(counter.dataSource()).createCatalog();
        createWorkspace("wsa");
        createWorkspace("wsb");
        addStyle(null, "s1");
        addStyle(null, "s2");
        addStyle(null, "s3");
        addStyle(null, "s4");
        addStyle("wsb", "w1");
        for (int i = 1; i <= 4; i++) {
            addLayer("l" + i, "s2", "s3", "s4");
        }
        addLayer("l5", "wsb:w1");
        addLayer("l6");
    }

    @Test
    void listsAltStylesInOneQuery() {
        counter.reset();
        List<LayerInfo> layers = catalog.getLayers();
        assertThat(counter.count()).isEqualTo(1);
        assertThat(styleNames(find(layers, "l1"))).containsExactlyInAnyOrder("s2", "s3", "s4");
    }

    @Test
    void getLayerLoadsAltStyles() {
        String id = idOf("l1");
        counter.reset();
        LayerInfo l1 = catalog.getLayer(id);
        assertThat(counter.count()).isEqualTo(1);
        assertThat(styleNames(l1)).containsExactlyInAnyOrder("s2", "s3", "s4");
    }

    @Test
    void altStyleKeepsItsWorkspace() {
        StyleInfo w1 = catalog.getLayerByName("wsa:l5").getStyles().iterator().next();
        assertThat(w1.getWorkspace().getName()).isEqualTo("wsb");
    }

    @Test
    void noAltStylesIsEmptySet() {
        assertThat(catalog.getLayerByName("wsa:l6").getStyles()).isEmpty();
    }

    @Test
    // the row is deleted behind the catalog's back, as a corrupt catalog would have it
    void deletedAltStyleIsSkipped() {
        String s3Id = catalog.getStyleByName("s3").getId();
        db.getTemplate().update("DELETE FROM styleinfo WHERE id = ?", s3Id);

        LayerInfo l1 = catalog.getLayerByName("wsa:l1");

        assertThat(l1.getStyles()).doesNotContainNull();
        assertThat(styleNames(l1)).containsExactlyInAnyOrder("s2", "s4");
    }

    // an empty entry stands for a layer or group without a tile layer
    @Test
    void lookupFollowsTileLayer() {
        String layerId = idOf("l1");
        String groupId = addGroup("g1", catalog.getLayer(layerId)).getId();
        EntityCache cache = newEntityCache();
        PgconfigLayerRepository layers = cachedRepository(cache);
        PgconfigLayerGroupRepository groups = cachedGroupRepository(cache);

        setTileLayer(layerId, "{\"enabled\":true}");
        setTileLayer(groupId, "{\"enabled\":false}");
        layers.findById(layerId);
        groups.findById(groupId);
        String layerSet = cachedTileLayer(cache, layerId);
        String groupSet = cachedTileLayer(cache, groupId);

        cache.clear();
        setTileLayer(layerId, null);
        setTileLayer(groupId, null);
        layers.findById(layerId);
        groups.findById(groupId);

        assertThat(layerSet).isEqualTo("{\"enabled\": true}");
        assertThat(groupSet).isEqualTo("{\"enabled\": false}");
        assertThat(cachedTileLayer(cache, layerId)).isEmpty();
        assertThat(cachedTileLayer(cache, groupId)).isEmpty();
    }

    // a listing leaves the settings out, even when it writes its rows to the cache
    @Test
    void lookupBringsTileLayer() {
        String layerId = idOf("l1");
        setTileLayer(layerId, "{\"enabled\":true}");
        EntityCache cache = newEntityCache();
        PgconfigLayerRepository repository = cachedRepository(cache);

        repository.findById(layerId);
        String byId = cachedTileLayer(cache, layerId);
        cache.clear();
        repository.findOneByName("wsa:l1");
        String byName = cachedTileLayer(cache, layerId);
        cache.clear();
        listLayers(repository);
        String listed = cachedTileLayer(cache, layerId);

        assertThat(byId).isEqualTo("{\"enabled\": true}");
        assertThat(byName).isEqualTo("{\"enabled\": true}");
        assertThat(listed).isNull();
        assertThat(cache.contains(CacheKey.entity(LAYER, layerId)))
                .as("listed layer")
                .isTrue();
    }

    /** A layer repository on the cache, with listing write-through on. */
    private PgconfigLayerRepository cachedRepository(EntityCache cache) {
        JdbcTemplate template = new JdbcTemplate(counter.dataSource());
        PgconfigStyleRepository styles = new PgconfigStyleRepository(template);
        PgconfigLayerRepository repository = new PgconfigLayerRepository(template, styles);
        repository.setEntityCache(cache);
        repository.setListingWriteThrough(true);
        return repository;
    }

    private PgconfigLayerGroupRepository cachedGroupRepository(EntityCache cache) {
        JdbcTemplate template = new JdbcTemplate(counter.dataSource());
        PgconfigStyleRepository styles = new PgconfigStyleRepository(template);
        PgconfigLayerGroupRepository repository = new PgconfigLayerGroupRepository(template, styles);
        repository.setEntityCache(cache);
        return repository;
    }

    private static EntityCache newEntityCache() {
        EntityCache cache = new EntityCache("gs-catalog", 1024 * 1024, Duration.ofSeconds(30), new NoopCodec());
        cache.setInitialized(true);
        return cache;
    }

    /** The JSON of the GWC settings of an object held by the cache, null if the cache does not hold them. */
    private static @Nullable String cachedTileLayer(EntityCache cache, String id) {
        if (cache.lookup(CacheKey.tileLayer(id)) instanceof Lookup.Hit hit) {
            return new String(hit.json(), UTF_8);
        }
        return null;
    }

    private static List<LayerInfo> listLayers(PgconfigLayerRepository repository) {
        try (Stream<LayerInfo> layers = repository.findAll(Query.all(LayerInfo.class))) {
            return layers.toList();
        }
    }

    private void setTileLayer(String id, String json) {
        db.getTemplate().update("UPDATE publishedinfo SET tilelayer = ?::jsonb WHERE id = ?", json, id);
    }

    private LayerGroupInfo addGroup(String name, LayerInfo layer) {
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.setName(name);
        group.getLayers().add(layer);
        group.getStyles().add(null);
        catalog.add(group);
        return group;
    }

    private Set<String> styleNames(LayerInfo layer) {
        return layer.getStyles().stream().map(StyleInfo::getName).collect(Collectors.toSet());
    }

    private LayerInfo find(List<LayerInfo> layers, String name) {
        return layers.stream().filter(l -> name.equals(l.getName())).findFirst().orElseThrow();
    }

    private String idOf(String layerName) {
        return catalog.getLayerByName("wsa:" + layerName).getId();
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

    private void addStyle(String workspaceName, String name) {
        StyleInfo style = catalog.getFactory().createStyle();
        style.setName(name);
        style.setFilename("%s.sld".formatted(name));
        if (workspaceName != null) {
            style.setWorkspace(catalog.getWorkspaceByName(workspaceName));
        }
        catalog.add(style);
    }

    /** Adds a layer in wsa with default style s1; alternate styles are named as "name" or "workspace:name". */
    private void addLayer(String name, String... alternateStyles) {
        CatalogFactory factory = catalog.getFactory();
        WorkspaceInfo workspace = catalog.getWorkspaceByName("wsa");
        NamespaceInfo namespace = catalog.getNamespaceByPrefix("wsa");

        DataStoreInfo store = factory.createDataStore();
        store.setName("%s-store".formatted(name));
        store.setWorkspace(workspace);
        store.setEnabled(true);
        catalog.add(store);

        FeatureTypeInfo ft = factory.createFeatureType();
        ft.setName(name);
        ft.setNativeName(name);
        ft.setStore(store);
        ft.setNamespace(namespace);
        ft.setEnabled(true);
        catalog.add(ft);

        LayerInfo layer = factory.createLayer();
        layer.setResource(ft);
        layer.setDefaultStyle(catalog.getStyleByName("s1"));
        for (String styleName : alternateStyles) {
            layer.getStyles().add(catalog.getStyleByName(styleName));
        }
        catalog.add(layer);
    }
}
