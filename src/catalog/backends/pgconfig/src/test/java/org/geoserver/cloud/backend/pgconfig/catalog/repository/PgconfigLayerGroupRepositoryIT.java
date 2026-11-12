/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.Query;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.cache.CacheEntry;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey.Kind;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCacheWeight;
import org.geoserver.cloud.backend.pgconfig.cache.NoopCodec;
import org.geoserver.cloud.backend.pgconfig.cache.PutBudget;
import org.geoserver.cloud.backend.pgconfig.catalog.PgconfigCatalogFacade;
import org.geoserver.cloud.backend.pgconfig.support.DecodeCounter;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.cloud.backend.pgconfig.support.StatementCounter;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

@Testcontainers(disabledWithoutDocker = true)
class PgconfigLayerGroupRepositoryIT {

    private static final ObjectMapper JSON = PgconfigObjectMapper.newObjectMapper();

    // Large enough to hold the groups of a test without evicting any
    private static final long SIXTEEN_MIB = 16L * 1024 * 1024;

    // A few groups: the first row of a page brings the members and their references, about 10 KB, the next rows 1 KB
    private static final long SMALL_BUDGET = 16L * 1024;

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    private StatementCounter counter;
    private CatalogPlugin catalog;
    private DecomposingTemplate template;
    private PgconfigLayerGroupRepository repository;
    private String parentId;

    @BeforeEach
    void setUp() {
        counter = new StatementCounter(db.getDataSource());
        catalog = new PgconfigBackendBuilder(counter.dataSource()).createCatalog();
        template = new DecomposingTemplate(counter.dataSource());
        repository = new PgconfigLayerGroupRepository(template, new PgconfigStyleRepository(template));

        createWorkspace("wsa");
        addStyle("s1");
        addStyle("s2");
        addStyle("s3");
        addStyle("g1");
        addStyle("g2");
        LayerInfo l1 = addLayer("l1", "s2");
        LayerInfo l2 = addLayer("l2", "s3");
        LayerGroupInfo nested = addGroup("nested", List.of(addLayer("hidden")));
        LayerGroupInfo parent = addGroup("parent", List.of(l1, l2, nested), "g1", "g2");
        parentId = parent.getId();
    }

    // the aggregate is one level deep: the member of the nested group stays out of the row
    @Test
    void groupRowBringsMembers() {
        repository.findById(parentId);

        assertThat(entityNames(template.entries()))
                .contains("LAYER l1", "LAYER l2", "LAYERGROUP nested")
                .contains("FEATURETYPE l1", "FEATURETYPE l2", "STYLE s1", "STYLE s2", "STYLE s3")
                .doesNotContain("LAYER hidden", "FEATURETYPE hidden");
    }

    @Test
    void groupRowBringsGroupStyles() {
        repository.findById(parentId);

        assertThat(entityNames(template.entries())).contains("STYLE g1", "STYLE g2");
    }

    @Test
    void groupByNameBringsMembers() {
        repository.findByNameAndWorkspace("parent", catalog.getWorkspaceByName("wsa"));

        assertThat(entityNames(template.entries())).contains("LAYER l1", "LAYER l2", "LAYERGROUP nested");
    }

    @Test
    void groupByIdIsOneStatement() {
        counter.reset();
        Optional<LayerGroupInfo> parent = repository.findById(parentId);

        assertThat(counter.count()).isEqualTo(1);
        assertThat(parent).isPresent();
    }

    @Test
    void listingRowHasNoMembers() {
        List<LayerGroupInfo> groups = listGroups();

        assertThat(groups).hasSize(2);
        assertThat(template.columns()).contains("styles").doesNotContain("members");
        assertThat(entityNames(template.entries()))
                .containsOnly("LAYERGROUP parent", "LAYERGROUP nested", "WORKSPACE wsa", "STYLE g1", "STYLE g2");
    }

    @Test
    void findAllByIdHasNoMembers() {
        try (Stream<LayerGroupInfo> groups = repository.findAllById(List.of(parentId), LayerGroupInfo.class)) {
            assertThat(groups).hasSize(1);
        }

        assertThat(template.columns()).contains("styles").doesNotContain("members");
    }

    // the three lookups select different columns: with the members by id and by name, without them in bulk
    @Test
    void groupLookupBringsTileLayer() {
        db.getTemplate()
                .update("UPDATE publishedinfo SET tilelayer = ?::jsonb WHERE id = ?", "{\"enabled\":true}", parentId);
        WorkspaceInfo wsa = catalog.getWorkspaceByName("wsa");
        CacheEntry expected = CacheEntry.tileLayer(parentId, "{\"enabled\": true}".getBytes(UTF_8));

        List<CacheEntry> byId = tileLayersRead(() -> repository.findById(parentId));
        List<CacheEntry> byName = tileLayersRead(() -> repository.findByNameAndWorkspace("parent", wsa));
        List<CacheEntry> inBulk = tileLayersRead(() -> findAllById(parentId));

        assertThat(byId).containsExactly(expected);
        assertThat(byName).containsExactly(expected);
        assertThat(inBulk).containsExactly(expected);
    }

    // a layer and no group has the name, and a layer row lacks the columns of a group row
    @Test
    void findFirstByNameSkipsLayers() {
        Optional<LayerGroupInfo> found = repository.findFirstByName("l1", LayerGroupInfo.class);

        assertThat(found).isEmpty();
    }

    // the catalog resolves the members of each group: they come from the cache too
    @Test
    void warmUpMakesLookupsHits() {
        List<String> ids = addGroups(250);
        CachedCatalog cached = cachedCatalog();
        counter.reset();
        DecodeCounter decodes = new DecodeCounter();

        int put = cached.groups().warmUp(() -> false, PutBudget.unlimited());
        int warmUpStatements = counter.count();
        long warmUpDecodes = decodes.count();
        counter.reset();
        List<LayerGroupInfo> found =
                ids.stream().map(cached.catalog()::getLayerGroup).toList();

        // the 250 groups and the two of the fixture, in pages of 100
        assertThat(put).isEqualTo(252);
        assertThat(warmUpStatements).isEqualTo(3);
        assertThat(warmUpDecodes).isZero();
        assertThat(found).hasSize(250).doesNotContainNull();
        assertThat(counter.count()).isZero();
    }

    @Test
    void warmUpStopsAtBudget() {
        addGroups(50);
        List<String> ids = allGroupIds();
        CachedCatalog cached = cachedCatalog();
        long heaviestRow = heaviestRow(cached, ids);

        int put = cached.groups().warmUp(() -> false, new PutBudget(SMALL_BUDGET));
        long weight = EntityCacheWeight.of(cached.cache());
        List<String> absent =
                ids.stream().filter(id -> !isCached(cached.cache(), id)).toList();
        counter.reset();
        absent.forEach(id -> cached.groups().findById(id));

        assertThat(put).isPositive().isLessThan(ids.size());
        assertThat(weight).isLessThanOrEqualTo(SMALL_BUDGET + heaviestRow);
        assertThat(absent).isNotEmpty();
        assertThat(counter.count()).isEqualTo(absent.size());
    }

    // a build cancelled once the first page is read
    @Test
    void warmUpStopsWhenCancelled() {
        addGroups(150);
        CachedCatalog cached = cachedCatalog();
        counter.reset();
        BooleanSupplier cancelledAfterFirstPage = () -> counter.count() > 0;

        int put = cached.groups().warmUp(cancelledAfterFirstPage, PutBudget.unlimited());

        assertThat(put).isEqualTo(PgconfigLayerGroupRepository.WARM_UP_PAGE_SIZE);
        assertThat(counter.count()).isOne();
    }

    // a reload suspends the cache, and a suspended cache ignores puts
    @Test
    void warmUpSkippedWhileSuspended() {
        CachedCatalog cached = cachedCatalog();
        cached.cache().setSuspended(true);
        counter.reset();

        int put = cached.groups().warmUp(() -> false, PutBudget.unlimited());

        assertThat(put).isZero();
        assertThat(counter.count()).isZero();
    }

    /** A catalog with an entity cache on the database of the test, and the group repository of its facade. */
    private record CachedCatalog(CatalogPlugin catalog, PgconfigLayerGroupRepository groups, EntityCache cache) {}

    private CachedCatalog cachedCatalog() {
        EntityCache cache = new EntityCache("gs-catalog", SIXTEEN_MIB, Duration.ofSeconds(30), new NoopCodec());
        cache.setInitialized(true);
        PgconfigCatalogFacade facade = new PgconfigCatalogFacade(new JdbcTemplate(counter.dataSource()), cache);
        CatalogPlugin cachedCatalog = new CatalogPlugin();
        cachedCatalog.setFacade(PgconfigBackendBuilder.resolving(facade));
        PgconfigLayerGroupRepository groups = facade.repository(LayerGroupInfo.class);
        return new CachedCatalog(cachedCatalog, groups, cache);
    }

    /**
     * The most weight put by the lookup of one group by id, each lookup into an emptied cache. A row of the warm-up
     * puts at most the entries of the lookup of its group: the rows of a page share the entries of their references.
     */
    private static long heaviestRow(CachedCatalog cached, List<String> ids) {
        long heaviest = 0;
        for (String id : ids) {
            cached.cache().clear();
            cached.groups().findById(id);
            heaviest = Math.max(heaviest, EntityCacheWeight.of(cached.cache()));
        }
        cached.cache().clear();
        return heaviest;
    }

    private static boolean isCached(EntityCache cache, String groupId) {
        return cache.contains(CacheKey.entity(ConfigInfoType.LAYERGROUP, groupId));
    }

    private List<String> allGroupIds() {
        return catalog.getLayerGroups().stream().map(LayerGroupInfo::getId).toList();
    }

    /**
     * Adds groups named group-000 and up in wsa, each holding l1 and l2. Every tenth group also holds the group added
     * before it.
     */
    private List<String> addGroups(int count) {
        LayerInfo l1 = catalog.getLayerByName("wsa:l1");
        LayerInfo l2 = catalog.getLayerByName("wsa:l2");
        List<String> ids = new ArrayList<>();
        LayerGroupInfo previous = null;
        for (int i = 0; i < count; i++) {
            List<PublishedInfo> members = new ArrayList<>(List.of(l1, l2));
            if (i % 10 == 9) {
                members.add(previous);
            }
            LayerGroupInfo group = addGroup("group-%03d".formatted(i), members);
            ids.add(group.getId());
            previous = group;
        }
        return ids;
    }

    /** The tile layer entries of the rows read by a lookup. */
    private List<CacheEntry> tileLayersRead(Runnable lookup) {
        int before = template.entries().size();
        lookup.run();
        List<CacheEntry> read =
                template.entries().subList(before, template.entries().size());
        return read.stream()
                .filter(entry -> entry.key().kind() == Kind.TILE_LAYER)
                .toList();
    }

    private List<LayerGroupInfo> findAllById(String id) {
        try (Stream<LayerGroupInfo> groups = repository.findAllById(List.of(id), LayerGroupInfo.class)) {
            return groups.toList();
        }
    }

    private List<LayerGroupInfo> listGroups() {
        try (Stream<LayerGroupInfo> groups = repository.findAll(Query.all(LayerGroupInfo.class))) {
            return groups.toList();
        }
    }

    /** The ENTITY entries as their type and the name read from their JSON, such as "LAYER l1". */
    private static List<String> entityNames(List<CacheEntry> entries) {
        return entries.stream()
                .filter(entry -> entry.key().kind() == Kind.ENTITY)
                .map(PgconfigLayerGroupRepositoryIT::entityName)
                .toList();
    }

    private static String entityName(CacheEntry entry) {
        String name = JSON.readTree(entry.json()).path("name").asString();
        return entry.key().type() + " " + name;
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

    /** Adds a layer in wsa with default style s1 and the given alternate styles. */
    private LayerInfo addLayer(String name, String... alternateStyles) {
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
        return layer;
    }

    /** Adds a group in wsa. The explicit styles go to the first members in order, and the other members have none. */
    private LayerGroupInfo addGroup(String name, List<PublishedInfo> members, String... styleNames) {
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.setName(name);
        group.setWorkspace(catalog.getWorkspaceByName("wsa"));
        for (int i = 0; i < members.size(); i++) {
            group.getLayers().add(members.get(i));
            group.getStyles().add(explicitStyle(styleNames, i));
        }
        catalog.add(group);
        return group;
    }

    private @Nullable StyleInfo explicitStyle(String[] styleNames, int index) {
        if (index < styleNames.length) {
            return catalog.getStyleByName(styleNames[index]);
        }
        return null;
    }

    /**
     * Runs the queries of the repository and decomposes each row with the row mapper of the repository, before the
     * mapper builds the object of the row. Keeps the entries and the column labels of the rows.
     */
    private static class DecomposingTemplate extends JdbcTemplate {

        private final List<CacheEntry> entries = new ArrayList<>();

        private final Set<String> columns = new TreeSet<>();

        DecomposingTemplate(DataSource dataSource) {
            super(dataSource);
        }

        List<CacheEntry> entries() {
            return entries;
        }

        Set<String> columns() {
            return columns;
        }

        @Override
        public <T> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper, @Nullable Object... args) {
            RowMapper<T> decomposing = (rs, rowNum) -> decomposeAndMap(rowMapper, rs, rowNum);
            return super.queryForStream(sql, decomposing, args);
        }

        private <T> T decomposeAndMap(RowMapper<T> rowMapper, ResultSet rs, int rowNum) throws SQLException {
            CatalogInfoRowMapper<?> mapper = (CatalogInfoRowMapper<?>) rowMapper;
            entries.addAll(mapper.decompose(rs));
            columns.addAll(columnLabels(rs.getMetaData()));
            return rowMapper.mapRow(rs, rowNum);
        }

        private static List<String> columnLabels(ResultSetMetaData metadata) throws SQLException {
            List<String> labels = new ArrayList<>();
            for (int column = 1; column <= metadata.getColumnCount(); column++) {
                labels.add(metadata.getColumnLabel(column));
            }
            return labels;
        }
    }
}
