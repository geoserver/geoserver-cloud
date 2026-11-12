/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.geoserver.cloud.event.info.ConfigInfoType.FEATURETYPE;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYER;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.NamespaceWorkspaceConsistencyListener;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.Lookup;
import org.geoserver.cloud.backend.pgconfig.cache.NoopCodec;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.cloud.backend.pgconfig.support.StatementCounter;
import org.geotools.api.filter.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Renames through a catalog with an entity cache: a lookup by id or by name must not answer with a name made stale by a
 * rename.
 *
 * <p>Production users met stale names after renaming a workspace in the web UI. The page adding a cached layer listed
 * the old prefixed layer names until the resource cache was cleared. It calls {@code Catalog.getLayerByName}, which
 * looks up the namespace, then the resource, then the layers of the resource.
 *
 * <p>The catalog keeps workspaces and namespaces paired, as in production: renaming one renames the other. The fixture
 * is added through the catalog under test: adding it warms the cache, as in production.
 */
@Testcontainers(disabledWithoutDocker = true)
class PgconfigEntityCacheRenameIT {

    private static final long ONE_MIB = 1024 * 1024;

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    private StatementCounter counter;
    private EntityCache cache;
    private CatalogPlugin catalog;
    private String workspaceId;
    private String namespaceId;
    private String resourceId;
    private String layerId;

    @BeforeEach
    void setUp() {
        counter = new StatementCounter(db.getDataSource());
        cache = new EntityCache("gs-catalog", ONE_MIB, Duration.ofSeconds(30), new NoopCodec());
        cache.setInitialized(true);
        catalog = new CatalogPlugin();
        PgconfigCatalogFacade facade = new PgconfigCatalogFacade(new JdbcTemplate(counter.dataSource()), cache);
        catalog.setFacade(PgconfigBackendBuilder.resolving(facade));
        new NamespaceWorkspaceConsistencyListener(catalog);
        addFixture();
    }

    @Test
    void layerByIdTakesNewPrefix() {
        warmCache();

        renameWorkspace("ws2");

        LayerInfo layer = catalog.getLayer(layerId);
        assertThat(layer.getResource().prefixedName()).isEqualTo("ws2:roads");
    }

    // the new name is looked up before the rename: its misses must not hide the renamed layer
    @Test
    void oldPrefixedNameNoLongerFound() {
        warmCache();
        assertThat(catalog.getLayerByName("ws2:roads")).isNull();

        renameWorkspace("ws2");

        assertThat(catalog.getLayerByName("ws:roads")).isNull();
        assertThat(catalog.getLayerByName("ws2:roads"))
                .extracting(layer -> layer.getResource().prefixedName())
                .isEqualTo("ws2:roads");
    }

    // adding the fixture warms the cache as well
    @Test
    void newNameFoundWithoutWarming() {
        renameWorkspace("ws2");

        assertThat(catalog.getLayerByName("ws2:roads"))
                .extracting(layer -> layer.getResource().prefixedName())
                .isEqualTo("ws2:roads");
    }

    @Test
    void namespaceTakesNewPrefix() {
        warmCache();

        renameWorkspace("ws2");

        assertThat(catalog.getNamespace(namespaceId).getPrefix()).isEqualTo("ws2");
        assertThat(catalog.getNamespaceByPrefix("ws")).isNull();
        assertThat(catalog.getNamespaceByPrefix("ws2")).isNotNull();
    }

    @Test
    void listedLayersTakeNewPrefix() {
        warmCache();

        renameWorkspace("ws2");

        assertThat(listedLayerNames()).containsExactly("ws2:roads");
    }

    // the listener renames the workspace with the namespace
    @Test
    void namespaceRenameUpdatesLayerNames() {
        warmCache();

        renameNamespace("ws2");

        assertThat(catalog.getLayerByName("ws:roads")).isNull();
        assertThat(catalog.getLayerByName("ws2:roads"))
                .extracting(LayerInfo::getId)
                .isEqualTo(layerId);
        LayerInfo byId = catalog.getLayer(layerId);
        assertThat(byId.getResource().prefixedName()).isEqualTo("ws2:roads");
    }

    // the new name is looked up before the rename: its misses must not hide the layer
    @Test
    void resourceRenameUpdatesLayerName() {
        warmCache();
        assertThat(catalog.getLayerByName("ws:streets")).isNull();

        renameResource("streets");

        assertThat(catalog.getLayerByName("ws:roads")).isNull();
        assertThat(catalog.getLayerByName("ws:streets"))
                .extracting(LayerInfo::getId)
                .isEqualTo(layerId);
    }

    // the key holds the id of a layer with another name
    @Test
    void staleNameHintIsDropped() {
        CacheKey lakes = CacheKey.name(LAYER, "ws:lakes");
        cache.putId(lakes, layerId, cache.epoch());

        LayerInfo found = catalog.getLayerByName("ws:lakes");

        assertThat(found).isNull();
        assertThat(cache.lookup(lakes)).isInstanceOf(Lookup.Miss.class);
        assertThat(catalog.getLayerByName("ws:roads"))
                .extracting(LayerInfo::getId)
                .isEqualTo(layerId);
    }

    // the resource of the layer is not cached: nothing confirms the name held by the key
    @Test
    void hintNeedsCachedResource() {
        catalog.getLayer(layerId);
        cache.evict(CacheKey.entity(FEATURETYPE, resourceId));
        CacheKey lakes = CacheKey.name(LAYER, "ws:lakes");
        cache.putId(lakes, layerId, cache.epoch());

        assertThat(catalog.getLayerByName("ws:lakes")).isNull();
    }

    // the key holds the id of a resource removed since: the query finds the resource, and puts its entries
    @Test
    void goneObjectHintIsDropped() {
        CacheKey roads = CacheKey.name(FEATURETYPE, "ws:roads");
        cache.putId(roads, "FeatureTypeInfo-gone", cache.epoch());

        FeatureTypeInfo found = catalog.getFeatureTypeByName("ws", "roads");
        counter.reset();
        catalog.getFeatureTypeByName("ws", "roads");

        assertThat(found).extracting(FeatureTypeInfo::getId).isEqualTo(resourceId);
        assertThat(counter.count()).isZero();
    }

    /** Reads the layer and its namespace by id and by name, as the pages of the web UI do. */
    private void warmCache() {
        catalog.getLayer(layerId);
        catalog.getLayerByName("ws:roads");
        catalog.getNamespace(namespaceId);
        catalog.getNamespaceByPrefix("ws");
        FeatureTypeInfo resource = catalog.getResource(resourceId, FeatureTypeInfo.class);
        catalog.getLayers(resource);
    }

    private void renameWorkspace(String name) {
        WorkspaceInfo workspace = catalog.getWorkspace(workspaceId);
        workspace.setName(name);
        catalog.save(workspace);
    }

    private void renameNamespace(String prefix) {
        NamespaceInfo namespace = catalog.getNamespace(namespaceId);
        namespace.setPrefix(prefix);
        catalog.save(namespace);
    }

    private void renameResource(String name) {
        FeatureTypeInfo resource = catalog.getFeatureType(resourceId);
        resource.setName(name);
        catalog.save(resource);
    }

    private List<String> listedLayerNames() {
        List<String> names = new ArrayList<>();
        try (CloseableIterator<LayerInfo> layers = catalog.list(LayerInfo.class, Filter.INCLUDE)) {
            while (layers.hasNext()) {
                LayerInfo layer = layers.next();
                names.add(layer.getResource().prefixedName());
            }
        }
        return names;
    }

    /** Adds the layer {@code ws:roads}, on a feature type of the data store {@code ws:ds}. */
    private void addFixture() {
        WorkspaceInfo workspace = addWorkspace("ws");
        NamespaceInfo namespace = addNamespace("ws");
        DataStoreInfo store = addDataStore("ds", workspace);
        FeatureTypeInfo resource = addFeatureType("roads", store, namespace);
        addStyle("line");
        addLayer(resource, "line");
        workspaceId = workspace.getId();
        namespaceId = namespace.getId();
        resourceId = resource.getId();
        layerId = catalog.getLayerByName("ws:roads").getId();
    }

    private WorkspaceInfo addWorkspace(String name) {
        WorkspaceInfo workspace = factory().createWorkspace();
        workspace.setName(name);
        catalog.add(workspace);
        return workspace;
    }

    private NamespaceInfo addNamespace(String prefix) {
        NamespaceInfo namespace = factory().createNamespace();
        namespace.setPrefix(prefix);
        namespace.setURI("http://%s.example".formatted(prefix));
        catalog.add(namespace);
        return namespace;
    }

    private DataStoreInfo addDataStore(String name, WorkspaceInfo workspace) {
        DataStoreInfo store = factory().createDataStore();
        store.setName(name);
        store.setWorkspace(workspace);
        store.setEnabled(true);
        catalog.add(store);
        return store;
    }

    private FeatureTypeInfo addFeatureType(String name, DataStoreInfo store, NamespaceInfo namespace) {
        FeatureTypeInfo resource = factory().createFeatureType();
        resource.setName(name);
        resource.setNativeName(name);
        resource.setStore(store);
        resource.setNamespace(namespace);
        resource.setEnabled(true);
        catalog.add(resource);
        return resource;
    }

    private void addStyle(String name) {
        StyleInfo style = factory().createStyle();
        style.setName(name);
        style.setFilename("%s.sld".formatted(name));
        catalog.add(style);
    }

    private void addLayer(FeatureTypeInfo resource, String styleName) {
        LayerInfo layer = factory().createLayer();
        layer.setResource(catalog.getResource(resource.getId(), FeatureTypeInfo.class));
        layer.setDefaultStyle(catalog.getStyleByName(styleName));
        layer.setEnabled(true);
        catalog.add(layer);
    }

    private CatalogFactory factory() {
        return catalog.getFactory();
    }
}
