/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
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
import org.geoserver.catalog.plugin.resolving.ResolvingCatalogInfoRepository;
import org.geoserver.catalog.plugin.resolving.ResolvingFacade;
import org.geoserver.cloud.backend.pgconfig.catalog.filter.PgconfigQueryBuilder;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.filter.sort.SortOrder;
import org.geotools.factory.CommonFactoryFinder;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * @since 1.4
 */
@Slf4j(topic = "org.geoserver.cloud.backend.pgconfig.catalog.repository")
public abstract class PgconfigCatalogInfoRepository<T extends CatalogInfo> extends ResolvingCatalogInfoRepository<T>
        implements CatalogInfoRepository<T>, ResolvingFacade<T> {

    protected static final ObjectMapper infoMapper = PgconfigObjectMapper.newObjectMapper();

    protected static final FilterFactory FILTER_FACTORY = CommonFactoryFinder.getFilterFactory();

    protected final @NonNull LoggingTemplate template;

    private Set<String> sortableProperties;

    /**
     * @param template
     */
    protected PgconfigCatalogInfoRepository(@NonNull JdbcTemplate template) {
        this(new LoggingTemplate(template));
    }

    protected PgconfigCatalogInfoRepository(@NonNull LoggingTemplate template) {
        super();
        this.template = template;
    }

    protected String getTable() {
        return getContentType().getSimpleName().toLowerCase();
    }

    protected abstract String getQueryTable();

    protected final Set<String> sortableProperties() {
        if (null == sortableProperties) {
            sortableProperties = resolveSortableProperties();
        }
        return sortableProperties;
    }

    private Set<String> resolveSortableProperties() {
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

    protected abstract RowMapper<T> newRowMapper();

    @Override
    public void add(@NonNull T value) {
        String encoded = encode(value);
        template.update(
                """
                INSERT INTO %s (info) VALUES(to_json(?::json))
                """
                        .formatted(getTable()),
                encoded);
    }

    @Override
    public void remove(@NonNull T value) {
        template.update(
                """
                DELETE FROM %s WHERE id = ?
                """.formatted(getTable()),
                value.getId());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <I extends T> I update(@NonNull I value, @NonNull Patch patch) {
        String id = value.getId();
        T patched = findById(value.getId())
                .map(patch::applyTo)
                .orElseThrow(() -> new NoSuchElementException("%s with id %s does not exist"
                        .formatted(getContentType().getSimpleName(), value.getId())));

        String encoded = encode(patched);
        template.update(
                """
                UPDATE %s SET info = to_json(?::json) WHERE id = ?
                """
                        .formatted(getTable()),
                encoded,
                id);
        return (I) patched;
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

        String sql = "SELECT * FROM %s WHERE TRUE".formatted(getQueryTable());

        Object[] prepStatementParams = null;
        if (!Filter.INCLUDE.equals(supportedFilter)) {
            sql = "%s AND %s".formatted(sql, whereClause);
            prepStatementParams = prepareParams(qb);
        }

        sql = applySortOrder(sql, query.getSortBy());

        if (filterFullySupported) {
            sql = applyOffsetLimit(sql, query.getOffset(), query.getCount());
        }

        Stream<U> stream = queryForStream(type, sql, prepStatementParams).map(this::resolveOutbound);
        if (!filterFullySupported) {
            Predicate<U> predicate = toPredicate(unsupportedFilter);
            stream = stream.filter(predicate)
                    .skip(query.offset().orElse(0))
                    .limit(query.count().orElse(Integer.MAX_VALUE));
        }
        return stream;
    }

    protected Stream<T> queryForStream(String sql, Object... prepStatementParams) {
        return queryForStream(getContentType(), sql, prepStatementParams);
    }

    protected <U extends T> Stream<U> queryForStream(Class<U> type, String sql, Object... prepStatementParams) {

        RowMapper<T> rowMapper = newRowMapper();
        Stream<T> stream = template.queryForStream(sql, rowMapper, prepStatementParams);
        return stream.filter(type::isInstance).map(type::cast).map(this::resolveOutbound);
    }

    protected <V> Predicate<V> toPredicate(Filter filter) {
        return info -> null != info && filter.evaluate(info);
    }

    private <S extends Info> Filter applyTypeFilter(Filter filter, Class<S> type) {
        if (!getContentType().equals(type)) {
            filter = Predicates.and(Predicates.isInstanceOf(type), filter);
        }
        return filter;
    }

    protected String applyOffsetLimit(String sql, Integer offset, Integer limit) {
        if (null != offset) sql += " OFFSET %d".formatted(offset);
        if (null != limit) sql += " LIMIT %d".formatted(limit);
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
                    .formatted(property, getTable(), sortableProperties()));
        }
    }

    /**
     * @return {@code -1} if the {@code filter} is not fully supported
     */
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
                sql = sql.formatted(getTable());
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
        if (literalValues.isEmpty()) return null;
        return literalValues.stream().map(this::asPreparedValue).toArray();
    }

    private Object asPreparedValue(Object val) {
        if (val instanceof Enum<?> e) return e.name();
        return val;
    }

    @Override
    public <U extends T> Optional<U> findById(@NonNull String id, Class<U> clazz) {
        String query = """
                SELECT * FROM %s WHERE id = ?
                """.formatted(getQueryTable());
        return findOne(query, clazz, id);
    }

    public Optional<T> findById(@NonNull String id) {
        return findById(id, getContentType());
    }

    @Override
    public <U extends T> Optional<U> findFirstByName(@NonNull String name, Class<U> clazz) {
        String query = """
                SELECT * FROM %s WHERE name = ? ORDER BY id
                """
                .formatted(getQueryTable());
        return findOne(query, clazz, name);
    }

    protected Optional<T> findOne(@NonNull String query, Object... args) {
        return findOne(query, getContentType(), args);
    }

    protected <U extends T> Optional<U> findOne(@NonNull String query, Class<U> clazz, Object... args) {

        return findOne(query, clazz, newRowMapper(), args);
    }

    protected <U extends T> Optional<U> findOne(
            @NonNull String query, Class<U> clazz, RowMapper<T> rowMapper, Object... args) {

        try {
            T object = template.queryForObject(query, rowMapper, args);
            return Optional.ofNullable(object)
                    .filter(clazz::isInstance)
                    .map(clazz::cast)
                    .map(this::resolveOutbound);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
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
        try {
            return infoMapper.writeValueAsString(info);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected String infoType(CatalogInfo value) {
        Class<? extends CatalogInfo> clazz = value.getClass();
        return infoType(clazz);
    }

    protected @NonNull String infoType(Class<? extends CatalogInfo> clazz) {
        ClassMappings cm;
        if (clazz.isInterface()) cm = ClassMappings.fromInterface(clazz);
        else cm = ClassMappings.fromImpl(clazz);

        return cm.getInterface().getSimpleName();
    }
}
