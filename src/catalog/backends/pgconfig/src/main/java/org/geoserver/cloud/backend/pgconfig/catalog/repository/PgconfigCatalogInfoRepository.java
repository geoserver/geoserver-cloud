/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Query;
import org.geoserver.cloud.backend.pgconfig.cache.CacheAccess;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey.Kind;
import org.geoserver.cloud.backend.pgconfig.cache.CatalogInfoKeys;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.Lookup;
import org.geoserver.cloud.backend.pgconfig.catalog.filter.PgconfigQueryBuilder;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.filter.sort.SortOrder;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.databind.ObjectMapper;

/**
 * Base class of the repositories of the pgconfig catalog.
 *
 * <p>With an {@link EntityCache}, lookups by id, by name and of the default objects read the cache first. On a cache
 * miss, a lookup runs its query, puts the objects of the row in the cache, and decodes the object from its own JSON,
 * like a hit. The references of the object are then proxies holding ids. Listings map their rows as without a cache,
 * and feed it when listing write-through is on. A write evicts the keys made stale by it. The cache side of these
 * operations is in {@link RepositoryCache}.
 *
 * <p>The repositories return their objects with unresolved references. The facade given to the catalog resolves them,
 * below workspace isolation. A listing evaluating part of its filter in memory resolves its objects first, see
 * {@link #setReferenceResolver(UnaryOperator)}.
 *
 * @since 1.4
 */
@Slf4j(topic = "org.geoserver.cloud.backend.pgconfig.catalog.repository")
@SuppressWarnings("java:S1192")
public abstract class PgconfigCatalogInfoRepository<T extends CatalogInfo> implements CatalogInfoRepository<T> {

    protected static final ObjectMapper infoMapper = PgconfigObjectMapper.newObjectMapper();

    protected final @NonNull LoggingTemplate template;

    private Set<String> sortableProperties;

    private final String insertSql;
    private final String updateByIdSql;
    private final String deleteByIdSql;

    private Class<T> contentType;

    /** The types of the objects of this repository, in lookup order. */
    private final RowTypes rowTypes;

    private @Nullable RepositoryCache<T> cache;

    private boolean listingWriteThrough;

    /** See {@link #setReferenceResolver(UnaryOperator)}. */
    private UnaryOperator<CatalogInfo> referenceResolver = UnaryOperator.identity();

    /** @param template */
    protected PgconfigCatalogInfoRepository(@NonNull Class<T> contentType, @NonNull JdbcTemplate template) {
        this(contentType, new LoggingTemplate(template));
    }

    protected PgconfigCatalogInfoRepository(@NonNull Class<T> contentType, @NonNull LoggingTemplate template) {
        this.contentType = contentType;
        this.template = template;
        this.rowTypes = new RowTypes(contentType);

        final String updateTable = getUpdateTable();

        insertSql = """
                INSERT INTO %s (info) VALUES(to_json(?::json))
                """
                .formatted(updateTable);

        updateByIdSql = """
                UPDATE %s SET info = to_json(?::json) WHERE id = ?
                """
                .formatted(updateTable);

        deleteByIdSql = """
                DELETE FROM %s WHERE id = ?
                """.formatted(updateTable);
    }

    public final Class<T> getContentType() {
        return contentType;
    }

    /**
     * Sets the cache of the objects read by this repository, shared with the other repositories of the catalog. Null,
     * the default, reads the objects from the database.
     */
    public void setEntityCache(@Nullable EntityCache entityCache) {
        if (null == entityCache) {
            this.cache = null;
        } else {
            this.cache = new RepositoryCache<>(entityCache, template.getDataSource(), getContentType(), rowTypes);
        }
    }

    /** Sets whether listings add their objects to the cache. Off by default. */
    public void setListingWriteThrough(boolean listingWriteThrough) {
        this.listingWriteThrough = listingWriteThrough;
    }

    /**
     * Sets the function resolving the references of an object. A listing applies it to its objects before evaluating
     * the part of its filter not translated to SQL. That part runs in memory, and may run code reading the references,
     * such as the members of a group. By default, the filter sees the references as proxies.
     */
    public void setReferenceResolver(@NonNull UnaryOperator<CatalogInfo> referenceResolver) {
        this.referenceResolver = referenceResolver;
    }

    /** Returns how many catalog objects were decoded from JSON: from the cache, and by the row mappers. For tests. */
    static long decodeCount() {
        return CatalogInfoDecodes.total();
    }

    /**
     * The table name to use for inserts, updates, and deletes. May differ from the table used for
     * {@link #getQueryTable() querying}
     */
    protected final String getUpdateTable() {
        return getContentType().getSimpleName().toLowerCase();
    }

    /** The table or view name used for queries. It may contain denormalized fields for common query attributes */
    protected abstract String getQueryTable();

    /**
     * @return comma separated list of column names required to build the catalog info with the {@link RowMapper}
     * @see #newRowMapper
     */
    protected abstract String getReturnColumns();

    /**
     * The columns selected by the lookups of one object, by id or by name. They default to {@link #getReturnColumns()}.
     * A repository may add columns too costly to select for each object of a listing.
     *
     * @see #selectForLookup(String)
     */
    protected String getLookupColumns() {
        return getReturnColumns();
    }

    /**
     * Creates a new {@link RowMapper}, potentially stateful, so a new one has to be created for each operation (e.g.
     * {@code find*} methods).
     */
    protected CatalogInfoRowMapper<T> newRowMapper() {
        return CatalogInfoRowMapper.newInstance();
    }

    protected final Set<String> sortableProperties() {
        if (null == sortableProperties) {
            sortableProperties = resolveSortableProperties();
        }
        return sortableProperties;
    }

    /**
     * Resolves which column names can be used for queries. Column names match {@link CatalogInfo} property names that
     * are commonly used in filters. For example, {@code name, title, workspace.id, resource.store.name, etc.}
     *
     * <p>This method assumes any column from the {@link #getQueryTable() query table} that's not of type {@code jsonb}
     * can be used for filters.
     *
     * @return an immutable {@link Set} with the {@link #getQueryTable()} column names to use for filters.
     */
    protected Set<String> resolveSortableProperties() {
        Set<String> queryableColumns = new TreeSet<>();
        final String queryTable = getQueryTable();
        try (Connection c = template.getDataSource().getConnection()) {
            DatabaseMetaData metaData = c.getMetaData();
            try (ResultSet columns = metaData.getColumns(null, null, queryTable, null)) {
                while (columns.next()) {
                    String name = columns.getString("COLUMN_NAME");
                    String type = columns.getString("TYPE_NAME");
                    if (!"jsonb".equals(type)) {
                        queryableColumns.add(name);
                    }
                }
            }
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
        log.debug("resolved queryable/sortable properties for {}: {}", queryTable, queryableColumns);
        return Set.copyOf(queryableColumns);
    }

    @Override
    public void add(@NonNull T value) {
        String encoded = encode(value);
        template.update(insertSql, encoded);
        // a miss stored for the id or a name of the object would hide it
        evictKeysOf(value);
    }

    @Override
    public void remove(@NonNull T value) {
        template.update(deleteByIdSql, value.getId());
        evictKeysOf(value);
    }

    private void evictKeysOf(T info) {
        if (null != cache) {
            cache.evictAfterWrite(CatalogInfoKeys.keysOf(info));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <I extends T> I update(@NonNull I value, @NonNull Patch patch) {
        String id = value.getId();
        T current = findCurrent(id);
        List<CacheKey> staleKeys = new ArrayList<>(cacheKeysOf(current));
        T patched = patch.applyTo(current);
        staleKeys.addAll(cacheKeysOf(patched));

        String encoded = encode(patched);
        template.update(updateByIdSql, encoded, id);
        evictAfterUpdate(staleKeys, patched, patch);
        return (I) patched;
    }

    /**
     * Reads an object from the database, without the cache. An update applies its patch to this copy and writes it
     * back: a copy from the cache could miss a change made by another service instance, and the update would undo it.
     */
    protected final T findCurrent(String id) {
        return findMapped(select("WHERE id = ?"), getContentType(), id)
                .orElseThrow(() -> new NoSuchElementException("%s with id %s does not exist"
                        .formatted(getContentType().getSimpleName(), id)));
    }

    /** The keys of an object in the cache, empty without a cache. */
    private List<CacheKey> cacheKeysOf(T info) {
        if (null == cache) {
            return List.of();
        }
        return CatalogInfoKeys.keysOf(info);
    }

    private void evictAfterUpdate(List<CacheKey> staleKeys, T patched, Patch patch) {
        if (null == cache) {
            return;
        }
        ConfigInfoType type = ConfigInfoType.valueOf(patched);
        Optional<Predicate<CacheKey>> dependentKeys = CatalogInfoKeys.dependentKeys(type, patch);
        if (dependentKeys.isPresent()) {
            cache.evictAfterWrite(staleKeys, dependentKeys.get());
        } else {
            cache.evictAfterWrite(staleKeys);
        }
    }

    /** Evicts keys made stale by a write of this repository, such as the key of a default object. */
    protected void evictAfterWrite(CacheKey... keys) {
        if (null != cache) {
            cache.evictAfterWrite(List.of(keys));
        }
    }

    @Override
    public <U extends T> Stream<U> findAll(Query<U> query) {
        final Filter filter = applyTypeFilter(query.getFilter(), query.getType());
        final Class<U> type = query.getType();

        final PgconfigQueryBuilder qb = new PgconfigQueryBuilder(filter, sortableProperties()).build();
        final Filter supportedFilter = qb.getSupportedFilter();
        final Filter unsupportedFilter = qb.getUnsupportedFilter();
        final String whereClause = qb.getWhereClause();

        log.trace(
                "supported filter {} translated to {}, unsupported: {}",
                supportedFilter,
                whereClause,
                unsupportedFilter);

        final boolean filterFullySupported = Filter.INCLUDE.equals(unsupportedFilter);

        String sql = select("WHERE TRUE");

        Object[] prepStatementParams = null;
        if (!Filter.INCLUDE.equals(supportedFilter)) {
            sql = "%s AND %s".formatted(sql, whereClause);
            prepStatementParams = prepareParams(qb);
        }

        sql = applySortOrder(sql, query.getSortBy());

        if (filterFullySupported) {
            sql = applyOffsetLimit(sql, query.getOffset(), query.getCount());
        }

        Stream<U> stream = queryForStream(type, sql, prepStatementParams);
        if (!filterFullySupported) {
            stream = filterInMemory(stream, unsupportedFilter, query);
        }
        return stream;
    }

    /**
     * Evaluates the part of the filter not translated to SQL on the objects of a listing, then applies the offset and
     * limit of the query. The references of the objects are resolved first: that part may run code reading them, as the
     * access checks of a GetCapabilities request read the members of a group.
     */
    private <U extends T> Stream<U> filterInMemory(Stream<U> listed, Filter filter, Query<U> query) {
        Stream<U> resolved = listed.map(this::resolveReferences);
        Stream<U> matching = resolved.filter(toPredicate(filter));
        int offset = query.offset().orElse(0);
        int limit = query.count().orElse(Integer.MAX_VALUE);
        return matching.skip(offset).limit(limit);
    }

    @SuppressWarnings("unchecked")
    private <U extends T> U resolveReferences(U info) {
        return (U) referenceResolver.apply(info);
    }

    protected Stream<T> queryForStream(String sql, Object... prepStatementParams) {
        return queryForStream(getContentType(), sql, prepStatementParams);
    }

    protected <U extends T> Stream<U> queryForStream(Class<U> type, String sql, Object... prepStatementParams) {

        RowMapper<T> rowMapper = listingRowMapper();
        Stream<T> stream = template.queryForStream(sql, rowMapper, prepStatementParams);
        return stream.filter(type::isInstance).map(type::cast);
    }

    /**
     * The row mapper of a listing. With listing write-through on, it also puts the entries of the rows in the cache.
     */
    private RowMapper<T> listingRowMapper() {
        CatalogInfoRowMapper<T> rowMapper = newRowMapper();
        if (!cacheInUse() || !listingWriteThrough) {
            return rowMapper;
        }
        long epochSeen = cache.epoch();
        return (rs, rowNum) -> {
            T object = rowMapper.mapRow(rs, rowNum);
            cache.putListedRow(rowMapper, rs, object, epochSeen);
            return object;
        };
    }

    protected <V> Predicate<V> toPredicate(Filter filter) {
        return info -> null != info && filter.evaluate(info);
    }

    protected <S extends Info> Filter applyTypeFilter(Filter filter, Class<S> type) {
        if (!getContentType().equals(type)) {
            filter = Predicates.and(Predicates.isInstanceOf(type), filter);
        }
        return filter;
    }

    protected String applyOffsetLimit(String sql, Integer offset, Integer limit) {
        if (null != offset) {
            sql += " OFFSET %d".formatted(offset);
        }
        if (null != limit) {
            sql += " LIMIT %d".formatted(limit);
        }
        return sql;
    }

    protected String applySortOrder(String sql, @NonNull List<SortBy> sortBy) {
        if (!sortBy.isEmpty()) {
            StringBuilder builder = new StringBuilder(sql).append(" ORDER BY");
            for (SortBy sort : sortBy) {
                String property = sort.getPropertyName().getPropertyName();
                checkCanSortBy(property);
                SortOrder sortOrder = sort.getSortOrder();
                builder.append(" \"%s\" %s".formatted(property, sortOrder.toSQL()));
            }
            return builder.toString();
        }
        return sql;
    }

    protected void checkCanSortBy(String property) {
        if (!canSortBy(property)) {
            throw new IllegalArgumentException("Unsupported sort property %s on %s. Supported properties: %s"
                    .formatted(property, getUpdateTable(), sortableProperties()));
        }
    }

    /** @return {@code -1} if the {@code filter} is not fully supported */
    @Override
    public <U extends T> long count(Class<U> of, Filter filter) {
        filter = applyTypeFilter(filter, of);
        final PgconfigQueryBuilder qb = new PgconfigQueryBuilder(filter, sortableProperties()).build();
        final Filter supportedFilter = qb.getSupportedFilter();
        final Filter unsupportedFilter = qb.getUnsupportedFilter();
        final String whereClause = qb.getWhereClause();

        log.trace(
                "supported filter {} translated to {}, unsupported: {}",
                supportedFilter,
                whereClause,
                unsupportedFilter);

        final boolean filterFullySupported = Filter.INCLUDE.equals(unsupportedFilter);
        if (filterFullySupported) {
            String sql = "SELECT count(*) FROM %s WHERE TRUE";
            Object[] prepStatementParams = null;
            if (Filter.INCLUDE.equals(supportedFilter)) {
                sql = sql.formatted(getUpdateTable());
            } else {
                sql = sql.formatted(getQueryTable());
                sql = "%s AND %s".formatted(sql, whereClause);
                prepStatementParams = prepareParams(qb);
            }
            return template.queryForObject(sql, Long.class, prepStatementParams);
        }

        try (Stream<U> stream = findAll(Query.valueOf(of, filter))) {
            return stream.count();
        }
    }

    @SuppressWarnings("java:S1168")
    private Object[] prepareParams(PgconfigQueryBuilder qb) {
        List<Object> literalValues = qb.getLiteralValues();
        if (literalValues.isEmpty()) {
            return null;
        }
        return literalValues.stream().map(this::asPreparedValue).toArray();
    }

    private Object asPreparedValue(Object val) {
        if (val instanceof Enum<?> e) {
            return e.name();
        }
        return val;
    }

    @Override
    public <U extends T> Optional<U> findById(@NonNull String id, Class<U> clazz) {
        if (!cacheInUse()) {
            return findMapped(selectForLookup("WHERE id = ?"), clazz, id);
        }
        return findThroughCache(id, clazz);
    }

    public Optional<T> findById(@NonNull String id) {
        return findById(id, getContentType());
    }

    /**
     * Finds an object by id through the cache: from its JSON in the cache, or with a query. The query feeds the cache,
     * and an empty answer is remembered as a miss for the id.
     */
    private <U extends T> Optional<U> findThroughCache(String id, Class<U> clazz) {
        List<CacheKey> keys = cache.entityKeys(id, clazz);
        return switch (cache.lookup(keys)) {
            case Lookup.Hit hit -> instanceOf(clazz, cache.decode(hit.json()));
            case Lookup.Miss _ -> Optional.empty();
            case Lookup.Absent _ -> cache.queryRemembering(keys, () -> readById(id, clazz));
        };
    }

    private static <U> Optional<U> instanceOf(Class<U> clazz, Object info) {
        return Optional.of(info).filter(clazz::isInstance).map(clazz::cast);
    }

    private <U extends T> Optional<U> readById(String id, Class<U> clazz) {
        return readOne(selectForLookup("WHERE id = ?"), clazz, id);
    }

    /**
     * Finds the first object with the name, by id. The name is a bare name, and objects of several workspaces or
     * namespaces may share it: the lookup reads the database, and the row feeds the cache.
     */
    @Override
    public <U extends T> Optional<U> findFirstByName(@NonNull String name, Class<U> clazz) {
        if (!filtersByType(clazz)) {
            return findOne(selectForLookup("WHERE name = ? ORDER BY id LIMIT 1"), clazz, name);
        }
        String predicate = "WHERE \"@type\" = ANY(?::infotype[]) AND name = ? ORDER BY id LIMIT 1";
        return findOne(selectForLookup(predicate), clazz, infoTypes(clazz), name);
    }

    /**
     * Whether a query for objects of the class must select their type, as the query table also holds objects of other
     * types.
     */
    protected boolean filtersByType(Class<?> clazz) {
        return !clazz.isAssignableFrom(getContentType());
    }

    /** The {@code @type} values of the types of this repository assignable to the class. */
    private String[] infoTypes(Class<?> clazz) {
        return rowTypes.assignableTo(clazz).stream()
                .map(type -> type.getType().getSimpleName())
                .toArray(String[]::new);
    }

    /**
     * Finds the objects of the given type with the given ids in one query, and runs no query for empty ids.
     *
     * <p>The ids go as one array parameter. The SQL text is then the same for any number of ids, and the limit on the
     * number of bind parameters of a statement does not apply. The caller must close the returned stream.
     *
     * <p>With a cache, the query asks only for the ids absent from the cache, and the ids missing from its result are
     * remembered as misses.
     */
    public <U extends T> Stream<U> findAllById(@NonNull List<String> ids, @NonNull Class<U> type) {
        if (ids.isEmpty()) {
            return Stream.empty();
        }
        if (cacheInUse()) {
            return findAllCached(ids, type);
        }
        String query = select("WHERE id = ANY(?)");
        // cast to Object because varargs would spread the array into one parameter per id
        return queryForStream(type, query, (Object) ids.toArray(String[]::new));
    }

    private <U extends T> Stream<U> findAllCached(List<String> ids, Class<U> type) {
        List<T> found = new ArrayList<>();
        List<String> absent = new ArrayList<>();
        for (String id : new LinkedHashSet<>(ids)) {
            switch (cache.lookup(cache.entityKeys(id, type))) {
                case Lookup.Hit hit -> found.add(cache.decode(hit.json()));
                case Lookup.Miss _ -> {
                    // the id was not found earlier
                }
                case Lookup.Absent _ -> absent.add(id);
            }
        }
        found.addAll(fetchAll(absent, type));
        return found.stream().filter(type::isInstance).map(type::cast);
    }

    /** Queries the ids in one statement, and remembers a miss for the ids missing from the result. */
    private List<T> fetchAll(List<String> ids, Class<?> type) {
        if (ids.isEmpty()) {
            return List.of();
        }
        long epochSeen = cache.epoch();
        RowMapper<T> rowMapper = cachingRowMapper(epochSeen);
        List<T> fetched;
        // cast to Object because varargs would spread the array into one parameter per id
        Object idsParameter = ids.toArray(String[]::new);
        try (Stream<T> rows = template.queryForStream(select("WHERE id = ANY(?)"), rowMapper, idsParameter)) {
            fetched = rows.filter(Objects::nonNull).toList();
        }
        cache.rememberMissing(ids, fetched, type, epochSeen);
        return fetched;
    }

    /**
     * Finds an object by name, or by a lookup such as the default workspace. With a cache, each key holds the id of the
     * object for one of its possible types, and an object found by the query feeds the cache. Without keys, as when a
     * name cannot be computed, the query runs.
     *
     * <p>The id held by a key is a hint. Since the key was stored, the object may be gone, or a rename may have given
     * it another name than the one of a {@code NAME} key. The lookup then evicts the keys, and the query runs and
     * stores what it finds.
     */
    protected <U extends T> Optional<U> findByKeys(List<CacheKey> keys, Class<U> clazz, Supplier<Optional<U>> query) {
        if (!cacheInUse() || keys.isEmpty()) {
            return query.get();
        }
        return switch (cache.lookup(keys)) {
            case Lookup.Hit hit -> findByHint(keys, new String(hit.json(), UTF_8), clazz, query);
            case Lookup.Miss _ -> Optional.empty();
            case Lookup.Absent _ -> cache.queryRemembering(keys, query);
        };
    }

    /** Finds the object with the id held by the keys if it still answers them, and runs the query otherwise. */
    private <U extends T> Optional<U> findByHint(
            List<CacheKey> keys, String id, Class<U> clazz, Supplier<Optional<U>> query) {
        Optional<U> found = findThroughCache(id, clazz);
        if (found.isPresent() && answers(found.get(), keys.getFirst())) {
            return found;
        }
        cache.evict(keys);
        return cache.queryRemembering(keys, query);
    }

    // The NAME keys of a lookup hold the same name, one key per type. A LOOKUP key holds no name to compare.
    private boolean answers(T found, CacheKey key) {
        if (key.kind() == Kind.NAME) {
            return hasName(found, key.key());
        }
        return true;
    }

    /**
     * Whether an object read through the cache has the name of a {@code NAME} key, see
     * {@link CatalogInfoKeys#hasName(CatalogInfo, String)}. The references of the object are not resolved yet: they are
     * proxies holding ids.
     */
    protected boolean hasName(T info, String prefixedName) {
        return CatalogInfoKeys.hasName(info, prefixedName);
    }

    /**
     * Returns an object of any type held by the cache, with its references not resolved. Empty if the cache does not
     * hold it, and if no cache is in use: no query runs.
     */
    protected <R extends CatalogInfo> Optional<R> findCached(String id, Class<R> clazz) {
        if (!cacheInUse()) {
            return Optional.empty();
        }
        return cache.cachedObject(id, clazz);
    }

    /**
     * The {@code NAME} keys of a prefixed name, one per type of the class. Empty if the name is null, and without a
     * cache.
     */
    protected List<CacheKey> nameKeys(@Nullable String prefixedName, Class<?> clazz) {
        if (null == cache || null == prefixedName) {
            return List.of();
        }
        return cache.nameKeys(prefixedName, clazz);
    }

    /**
     * The keys of a lookup by name of a group or style without workspace, one per type of the class. Empty without a
     * cache. A name holding a colon is also the prefixed name of an object of a workspace: its lookup without workspace
     * keeps its misses under the keys of {@link CatalogInfoKeys#globalNameMiss(ConfigInfoType, String)}.
     */
    protected List<CacheKey> globalNameKeys(String name, Class<?> clazz) {
        if (null == cache) {
            return List.of();
        }
        String globalName = CatalogInfoKeys.globalName(name);
        if (null == globalName) {
            return cache.globalNameMissKeys(name, clazz);
        }
        return cache.nameKeys(globalName, clazz);
    }

    protected Optional<T> findOne(@NonNull String query, Object... args) {
        return findOne(query, getContentType(), args);
    }

    /**
     * Returns the first row matching {@code query} that is an instance of {@code clazz}, or empty if none matches.
     *
     * <p>Never throws when the query matches multiple rows, mirroring the first-match semantics of upstream GeoServer's
     * {@code DefaultCatalogFacade} for ambiguous lookups (e.g. an unqualified name present in several workspaces).
     * Queries that can legitimately match several rows must include an {@code ORDER BY} clause; without it the returned
     * row depends on the database's execution plan.
     *
     * <p>With a cache, the rows feed the cache, and the object is decoded from its own JSON in the row.
     */
    protected <U extends T> Optional<U> findOne(@NonNull String query, Class<U> clazz, Object... args) {
        if (!cacheInUse()) {
            return findMapped(query, clazz, args);
        }
        return readOne(query, clazz, args);
    }

    /**
     * Returns the first instance of the class in the rows of the query, and puts the entries of the rows in the cache.
     * The object is decoded from its own JSON, with its references not resolved.
     */
    private <U extends T> Optional<U> readOne(String query, Class<U> clazz, Object... args) {
        RowMapper<T> rowMapper = cachingRowMapper(cache.epoch());
        try (Stream<T> objects = template.queryForStream(query, rowMapper, args)) {
            return objects.filter(clazz::isInstance).map(clazz::cast).findFirst();
        }
    }

    /**
     * A row mapper that puts the entries of the rows in the cache, and decodes the object of a row from its own JSON. A
     * row that cannot be split into entries is mapped as without a cache, and left out of the cache.
     */
    private RowMapper<T> cachingRowMapper(long epochSeen) {
        CatalogInfoRowMapper<T> rowMapper = newRowMapper();
        return (rs, rowNum) -> cache.readRow(rowMapper, rs, rowNum, epochSeen);
    }

    /** Maps the rows of the query as without a cache, and returns the first instance of the class. */
    private <U extends T> Optional<U> findMapped(String query, Class<U> clazz, Object... args) {
        RowMapper<T> rowMapper = newRowMapper();
        try (Stream<U> stream = template.queryForStream(query, rowMapper, args)
                .filter(clazz::isInstance)
                .map(clazz::cast)) {
            return stream.findFirst();
        }
    }

    /**
     * Whether lookups go through the cache: there is one, it is not suspended, and no transaction runs on the data
     * source, see {@link CacheAccess#inUse()}. A lookup through a suspended cache would read each reference of the
     * object with a query of its own, while the mapping of rows without the cache reads them with the object.
     */
    private boolean cacheInUse() {
        return null != cache && cache.inUse();
    }

    @Override
    public boolean canSortBy(@NonNull String propertyName) {
        return sortableProperties().contains(propertyName);
    }

    @Override
    public void syncTo(@NonNull CatalogInfoRepository<T> target) {
        throw new UnsupportedOperationException("implement");
    }

    @Override
    public void dispose() {}

    protected String encode(T info) {
        return infoMapper.writeValueAsString(info);
    }

    protected String infoType(CatalogInfo value) {
        Class<? extends CatalogInfo> clazz = value.getClass();
        return infoType(clazz);
    }

    protected @NonNull String infoType(Class<? extends CatalogInfo> clazz) {
        ClassMappings cm;
        if (clazz.isInterface()) {
            cm = ClassMappings.fromInterface(clazz);
        } else {
            cm = ClassMappings.fromImpl(clazz);
        }

        return cm.getInterface().getSimpleName();
    }

    protected String select(String predicate) {
        return selectColumns(getReturnColumns(), predicate);
    }

    /** Like {@link #select(String)}, with the columns of {@link #getLookupColumns()}. */
    protected String selectForLookup(String predicate) {
        return selectColumns(getLookupColumns(), predicate);
    }

    private String selectColumns(String columns, String predicate) {
        String queryTable = getQueryTable();
        if (null == predicate) {
            predicate = "";
        }
        return "SELECT %s FROM %s %s".formatted(columns, queryTable, predicate);
    }
}
