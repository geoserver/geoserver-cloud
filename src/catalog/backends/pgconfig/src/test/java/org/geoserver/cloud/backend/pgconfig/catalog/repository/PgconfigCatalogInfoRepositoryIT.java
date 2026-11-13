/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Query;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.Lookup;
import org.geoserver.cloud.backend.pgconfig.cache.ZstdCodec;
import org.geoserver.cloud.backend.pgconfig.support.DecodeCounter;
import org.geoserver.cloud.backend.pgconfig.support.LogCapture;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.cloud.backend.pgconfig.support.StatementCounter;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The repositories with an entity cache. The fixtures are written through a catalog without a cache, and the
 * repositories under test share a cache with the zstd codec.
 */
@Testcontainers(disabledWithoutDocker = true)
class PgconfigCatalogInfoRepositoryIT {

    private static final long ONE_MIB = 1024 * 1024;

    private static final String REPOSITORY_LOGGER = "org.geoserver.cloud.backend.pgconfig.catalog.repository";

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    private StatementCounter counter;
    private CatalogPlugin catalog;
    private ZstdCodec codec;
    private EntityCache cache;
    private JdbcTemplate template;
    private PgconfigStyleRepository styles;
    private PgconfigLayerRepository layers;
    private PgconfigWorkspaceRepository workspaces;
    private PgconfigStoreRepository stores;
    private PgconfigResourceRepository resources;
    private String l1Id;
    private String l2Id;
    private String l3Id;
    private String groupId;

    @BeforeEach
    void setUp() {
        counter = new StatementCounter(db.getDataSource());
        catalog = new PgconfigBackendBuilder(counter.dataSource()).createCatalog();
        createWorkspace("wsa");
        addStyle("s1");
        l1Id = addLayer("l1").getId();
        l2Id = addLayer("l2").getId();
        l3Id = addLayer("l3").getId();
        groupId = addGroup("g1", catalog.getLayer(l1Id)).getId();

        codec = ZstdCodec.withBundledDictionary(ZstdCodec.DEFAULT_LEVEL);
        cache = new EntityCache("gs-catalog", ONE_MIB, Duration.ofSeconds(30), codec);
        cache.setInitialized(true);
        template = new JdbcTemplate(counter.dataSource());
        styles = withCache(new PgconfigStyleRepository(template));
        layers = withCache(new PgconfigLayerRepository(template, styles));
        workspaces = withCache(new PgconfigWorkspaceRepository(template));
        stores = withCache(new PgconfigStoreRepository(template));
        resources = withCache(new PgconfigResourceRepository(template, layers));
        counter.reset();
    }

    @AfterEach
    void closeCodec() {
        codec.close();
    }

    @Test
    void secondFindByIdRunsNoQuery() {
        layers.findById(l1Id);
        counter.reset();

        Optional<LayerInfo> found = layers.findById(l1Id);

        assertThat(counter.count()).isZero();
        assertThat(found).map(LayerInfo::getId).contains(l1Id);
    }

    @Test
    void missIsRemembered() {
        layers.findById("LayerInfo-unknown");
        layers.findById("LayerInfo-unknown");

        assertThat(counter.count()).isEqualTo(1);
    }

    // l2 comes from the cache: its path, changed behind the cache, is not read
    @Test
    void findAllByIdFetchesOnlyAbsent() {
        layers.findById(l1Id);
        layers.findById(l2Id);
        setPathBehindCache(l2Id, "/changed");
        counter.reset();

        List<LayerInfo> found = findAllById(List.of(l1Id, l2Id, l3Id));

        assertThat(counter.count()).isEqualTo(1);
        assertThat(found).extracting(LayerInfo::getId).containsExactlyInAnyOrder(l1Id, l2Id, l3Id);
        assertThat(found)
                .filteredOn(layer -> l2Id.equals(layer.getId()))
                .extracting(LayerInfo::getPath)
                .containsOnlyNulls();
    }

    @Test
    void findAllByIdRemembersMisses() {
        findAllById(List.of(l1Id, "LayerInfo-unknown"));
        counter.reset();

        List<LayerInfo> found = findAllById(List.of(l1Id, "LayerInfo-unknown"));

        assertThat(counter.count()).isZero();
        assertThat(found).extracting(LayerInfo::getId).containsExactly(l1Id);
    }

    @Test
    void byNameHitRunsNoQuery() {
        layers.findOneByName("wsa:l1");
        counter.reset();

        Optional<LayerInfo> found = layers.findOneByName("wsa:l1");

        assertThat(counter.count()).isZero();
        assertThat(found).map(LayerInfo::getId).contains(l1Id);
    }

    @Test
    void listingFeedsCache() {
        layers.setListingWriteThrough(true);
        listLayers();
        counter.reset();

        layers.findById(l2Id);

        assertThat(counter.count()).isZero();
    }

    @Test
    void listingSkipsCacheByDefault() {
        listLayers();
        counter.reset();

        layers.findById(l2Id);

        assertThat(counter.count()).isEqualTo(1);
    }

    // the title of a layer is the one of its resource: the path is a property of the layer row
    @Test
    void updateEvictsEntity() {
        LayerInfo l1 = layers.findById(l1Id).orElseThrow();
        layers.update(l1, new Patch().with("path", "/new"));
        counter.reset();

        Optional<LayerInfo> found = layers.findById(l1Id);

        assertThat(counter.count()).isEqualTo(1);
        assertThat(found).map(LayerInfo::getPath).contains("/new");
    }

    @Test
    void removeEvicts() {
        LayerInfo l2 = layers.findById(l2Id).orElseThrow();
        layers.remove(l2);
        counter.reset();

        Optional<LayerInfo> found = layers.findById(l2Id);

        assertThat(counter.count()).isEqualTo(1);
        assertThat(found).isEmpty();
    }

    @Test
    void nullCacheKeepsTodaysBehavior() {
        layers.setEntityCache(null);

        layers.findById(l1Id);
        layers.findById(l1Id);

        assertThat(counter.count()).isEqualTo(2);
    }

    // a name looked up before the layer existed must not hide it
    @Test
    void addEvictsNameMiss() {
        LayerInfo l9 = newLayer("l9");
        assertThat(layers.findOneByName("wsa:l9")).isEmpty();

        layers.add(l9);

        assertThat(layers.findOneByName("wsa:l9")).map(LayerInfo::getId).contains(l9.getId());
    }

    // a lookup as a store misses under the key of each store type, and the add evicts the data store keys only
    @Test
    void storeAddEvictsTypeMisses() {
        WorkspaceInfo wsa = catalog.getWorkspaceByName("wsa");
        DataStoreInfo added = newDataStore("added");
        assertThat(stores.findById(added.getId(), StoreInfo.class)).isEmpty();
        assertThat(stores.findByNameAndWorkspace("added", wsa, StoreInfo.class)).isEmpty();

        stores.add(added);

        assertThat(stores.findById(added.getId(), StoreInfo.class)).isPresent();
        assertThat(stores.findByNameAndWorkspace("added", wsa, StoreInfo.class)).isPresent();
    }

    @Test
    void defaultWorkspaceIsCached() {
        workspaces.getDefaultWorkspace();
        counter.reset();

        Optional<WorkspaceInfo> found = workspaces.getDefaultWorkspace();

        assertThat(counter.count()).isZero();
        assertThat(found).map(WorkspaceInfo::getName).contains("wsa");
    }

    @Test
    void setDefaultEvictsLookup() {
        createWorkspace("wsb");
        workspaces.getDefaultWorkspace();

        workspaces.setDefaultWorkspace(catalog.getWorkspaceByName("wsb"));

        assertThat(workspaces.getDefaultWorkspace()).map(WorkspaceInfo::getName).contains("wsb");
    }

    // wsb names a store of wsa as its default: a store is listed as the default of its own workspace only
    @Test
    void defaultDataStoresListed() {
        createWorkspace("wsb");
        WorkspaceInfo wsa = catalog.getWorkspaceByName("wsa");
        WorkspaceInfo wsb = catalog.getWorkspaceByName("wsb");
        stores.setDefaultDataStore(wsa, catalog.getDataStoreByName("wsa", "l2-store"));
        stores.setDefaultDataStore(wsb, catalog.getDataStoreByName("wsa", "l1-store"));

        List<DataStoreInfo> defaults = defaultDataStores();

        assertThat(defaults).extracting(DataStoreInfo::getName).containsExactly("l2-store");
    }

    // the lookup columns lack the styles read by the split: the group is read without the cache, with one warning
    @Test
    void unsplitRowIsNotCached() {
        PgconfigLayerGroupRepository groups = new PgconfigLayerGroupRepository(template, styles) {
            @Override
            protected String getLookupColumns() {
                return "\"@type\", publishedinfo, workspace";
            }
        };
        groups.setEntityCache(cache);
        try (LogCapture log = LogCapture.of(REPOSITORY_LOGGER)) {
            groups.findById(groupId);
            counter.reset();

            Optional<LayerGroupInfo> found = groups.findById(groupId);

            assertThat(found).map(LayerGroupInfo::getName).contains("g1");
            assertThat(counter.count()).isEqualTo(1);
            assertThat(log.messages(Level.WARN)).hasSize(1);
            assertThat(log.messages(Level.DEBUG))
                    .filteredOn(message -> message.contains(groupId))
                    .hasSize(1);
        }
    }

    // the published infos repository marks the group, and the group repository writes it
    @Test
    void writeForgetsUncacheableRow() {
        CacheKey key = CacheKey.entity(ConfigInfoType.LAYERGROUP, groupId);
        PgconfigLayerGroupRepository groups = withCache(new PgconfigLayerGroupRepository(template, styles));
        try (LogCapture _ = LogCapture.of(REPOSITORY_LOGGER)) {
            publishedWithoutStyles().findById(groupId);
            assertThat(cache.isUncacheable(key)).isTrue();

            groups.update(catalog.getLayerGroup(groupId), new Patch().with("title", "written"));

            assertThat(cache.isUncacheable(key)).isFalse();
        }
    }

    @Test
    void evictsAfterCommit() {
        LayerInfo l1 = layers.findById(l1Id).orElseThrow();
        CacheKey key = CacheKey.entity(ConfigInfoType.LAYER, l1Id);
        TransactionTemplate transaction =
                new TransactionTemplate(new DataSourceTransactionManager(counter.dataSource()));

        transaction.executeWithoutResult(status -> {
            layers.update(l1, new Patch().with("path", "/new"));
            assertThat(cache.lookup(key)).isInstanceOf(Lookup.Hit.class);
        });

        assertThat(cache.lookup(key)).isInstanceOf(Lookup.Absent.class);
    }

    // a read within the transaction sees changes not committed yet: they must stay out of the shared cache
    @Test
    void transactionReadSkipsCache() {
        LayerInfo l1 = catalog.getLayer(l1Id);
        CacheKey key = CacheKey.entity(ConfigInfoType.LAYER, l1Id);
        TransactionTemplate transaction =
                new TransactionTemplate(new DataSourceTransactionManager(counter.dataSource()));

        transaction.executeWithoutResult(status -> {
            layers.update(l1, new Patch().with("path", "/uncommitted"));
            layers.findById(l1Id);
            assertThat(cache.lookup(key)).isInstanceOf(Lookup.Absent.class);
            status.setRollbackOnly();
        });
    }

    // a transaction on another data source holds no connection of the repositories
    @Test
    void otherTransactionUsesCache() {
        layers.findById(l1Id);
        counter.reset();

        otherTransaction().executeWithoutResult(status -> layers.findById(l1Id));

        assertThat(counter.count()).isZero();
    }

    // the update commits at once, outside the transaction on the other data source
    @Test
    void otherTransactionEvictsAtOnce() {
        LayerInfo l1 = layers.findById(l1Id).orElseThrow();
        CacheKey key = CacheKey.entity(ConfigInfoType.LAYER, l1Id);

        otherTransaction().executeWithoutResult(status -> {
            layers.update(l1, new Patch().with("path", "/new"));
            assertThat(cache.lookup(key)).isInstanceOf(Lookup.Absent.class);
        });
    }

    // one decode per object of the row: the layer, its resource, store, workspace, namespace and default style
    @Test
    void rowMappingCountsDecodes() {
        layers.setEntityCache(null);
        DecodeCounter decodes = new DecodeCounter();

        layers.findById(l1Id);

        assertThat(decodes.count()).isEqualTo(6);
    }

    // the query decodes the layer from its own JSON, and the hit decodes it again
    @Test
    void decodesOncePerLookup() {
        DecodeCounter decodes = new DecodeCounter();

        layers.findById(l1Id);
        layers.findById(l1Id);

        assertThat(decodes.count()).isEqualTo(2);
    }

    // the row mapping decodes the six objects of the row, see rowMappingCountsDecodes.
    // The cached path decodes the layer alone.
    @Test
    void suspendedCacheUsesRowMapping() {
        cache.setSuspended(true);
        DecodeCounter decodes = new DecodeCounter();

        layers.findById(l1Id);

        assertThat(counter.count()).isEqualTo(1);
        assertThat(decodes.count()).isEqualTo(6);
    }

    // the statement evaluates the whole filter: no object is resolved for an evaluation in memory
    @Test
    void pushedDownFilterResolvesNothing() {
        AtomicInteger resolved = new AtomicInteger();
        layers.setReferenceResolver(info -> {
            resolved.incrementAndGet();
            return info;
        });
        Query<LayerInfo> byName = Query.valueOf(LayerInfo.class, Predicates.equal("name", "l1"));

        List<LayerInfo> found = listLayers(byName);

        assertThat(found).extracting(LayerInfo::getName).containsExactly("l1");
        assertThat(counter.count()).isEqualTo(1);
        assertThat(resolved).hasValue(0);
    }

    // the database moves the resources of a store to the namespace of its new workspace
    @Test
    void storeMoveEvictsResources() {
        createWorkspace("wsb");
        ResourceInfo resource = catalog.getLayer(l1Id).getResource();
        resources.findById(resource.getId());
        StoreInfo store = stores.findById(resource.getStore().getId()).orElseThrow();

        stores.update(store, new Patch().with("workspace", catalog.getWorkspaceByName("wsb")));

        ResourceInfo moved = resources.findById(resource.getId()).orElseThrow();
        assertThat(moved.getNamespace().getId())
                .isEqualTo(catalog.getNamespaceByPrefix("wsb").getId());
    }

    // a store looked up in a workspace removed later must not stay hidden once another workspace takes its name
    @Test
    void renameEvictsNameMisses() {
        createWorkspace("wsb");
        WorkspaceInfo wsb = catalog.getWorkspaceByName("wsb");
        assertThat(stores.findByNameAndWorkspace("l1-store", wsb, StoreInfo.class))
                .isEmpty();
        catalog.remove(catalog.getNamespaceByPrefix("wsb"));
        catalog.remove(wsb);
        WorkspaceInfo wsa =
                workspaces.findFirstByName("wsa", WorkspaceInfo.class).orElseThrow();

        WorkspaceInfo renamed = workspaces.update(wsa, new Patch().with("name", "wsb"));

        assertThat(stores.findByNameAndWorkspace("l1-store", renamed, StoreInfo.class))
                .isPresent();
    }

    private <R extends PgconfigCatalogInfoRepository<?>> R withCache(R repository) {
        repository.setEntityCache(cache);
        return repository;
    }

    // The lookup columns lack the styles read by the split of a group row
    private PgconfigPublishedInfoReadOnlyRepository publishedWithoutStyles() {
        return withCache(new PgconfigPublishedInfoReadOnlyRepository(template, styles) {
            @Override
            protected String getLookupColumns() {
                return "\"@type\", publishedinfo, workspace";
            }
        });
    }

    private List<LayerInfo> findAllById(List<String> ids) {
        try (Stream<LayerInfo> found = layers.findAllById(ids, LayerInfo.class)) {
            return found.toList();
        }
    }

    // A data source of its own: the counting one passes equals and hashCode to the wrapped pool, and Spring would take
    // a transaction on that pool for one on the data source of the repositories
    private TransactionTemplate otherTransaction() {
        DataSource other =
                new DriverManagerDataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword());
        return new TransactionTemplate(new DataSourceTransactionManager(other));
    }

    private List<DataStoreInfo> defaultDataStores() {
        try (Stream<DataStoreInfo> found = stores.getDefaultDataStores()) {
            return found.toList();
        }
    }

    private void setPathBehindCache(String layerId, String path) {
        String sql = "UPDATE layerinfo SET info = jsonb_set(info, '{path}', to_jsonb(?::text)) WHERE id = ?";
        db.getTemplate().update(sql, path, layerId);
    }

    private List<LayerInfo> listLayers() {
        return listLayers(Query.all(LayerInfo.class));
    }

    private List<LayerInfo> listLayers(Query<LayerInfo> query) {
        try (Stream<LayerInfo> found = layers.findAll(query)) {
            return found.toList();
        }
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

    private void addStyle(String name) {
        StyleInfo style = catalog.getFactory().createStyle();
        style.setName(name);
        style.setFilename("%s.sld".formatted(name));
        catalog.add(style);
    }

    /** Adds a layer in wsa with default style s1. */
    private LayerInfo addLayer(String name) {
        LayerInfo layer = catalog.getFactory().createLayer();
        layer.setResource(addFeatureType(name));
        layer.setDefaultStyle(catalog.getStyleByName("s1"));
        catalog.add(layer);
        return catalog.getLayerByName("wsa:" + name);
    }

    /** A layer of wsa on a new feature type, with an id and not added. */
    private LayerInfo newLayer(String name) {
        LayerInfoImpl layer = (LayerInfoImpl) catalog.getFactory().createLayer();
        layer.setId("LayerInfo-" + name);
        layer.setResource(addFeatureType(name));
        layer.setDefaultStyle(catalog.getStyleByName("s1"));
        return layer;
    }

    /** A data store of wsa, with an id and not added. */
    private DataStoreInfo newDataStore(String name) {
        DataStoreInfoImpl store = (DataStoreInfoImpl) catalog.getFactory().createDataStore();
        store.setId("DataStoreInfo-" + name);
        store.setName(name);
        store.setWorkspace(catalog.getWorkspaceByName("wsa"));
        store.setEnabled(true);
        return store;
    }

    private FeatureTypeInfo addFeatureType(String name) {
        CatalogFactory factory = catalog.getFactory();
        DataStoreInfo store = factory.createDataStore();
        store.setName("%s-store".formatted(name));
        store.setWorkspace(catalog.getWorkspaceByName("wsa"));
        store.setEnabled(true);
        catalog.add(store);

        FeatureTypeInfo ft = factory.createFeatureType();
        ft.setName(name);
        ft.setNativeName(name);
        ft.setStore(store);
        ft.setNamespace(catalog.getNamespaceByPrefix("wsa"));
        ft.setEnabled(true);
        catalog.add(ft);
        return catalog.getFeatureTypeByName("wsa", name);
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
