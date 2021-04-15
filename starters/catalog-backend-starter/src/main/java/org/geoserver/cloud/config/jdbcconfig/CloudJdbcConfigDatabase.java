/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.jdbcconfig;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.geoserver.jdbcconfig.internal.DbUtils.logStatement;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.util.CapabilitiesFilterSplitterFix;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.jdbcconfig.internal.DbMappings;
import org.geoserver.jdbcconfig.internal.Dialect;
import org.geoserver.jdbcconfig.internal.FilterToCatalogSQL;
import org.geoserver.jdbcconfig.internal.PropertyType;
import org.geoserver.jdbcconfig.internal.XStreamInfoSerialBinding;
import org.geoserver.platform.resource.Resource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.Capabilities;
import org.geotools.filter.visitor.CapabilitiesFilterSplitter;
import org.geotools.filter.visitor.ClientTransactionAccessor;
import org.geotools.filter.visitor.LiteralDemultiplyingFilterVisitor;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Overrides {@link #count(Class, Filter)} to use {@link CapabilitiesFilterSplitterFix fixed
 * version} of {@link CapabilitiesFilterSplitter} until
 * https://osgeo-org.atlassian.net/browse/GEOT-6717 is fixed
 */
class CloudJdbcConfigDatabase extends ConfigDatabase {

    private Dialect dialect;
    private DataSource dataSource;
    private NamedParameterJdbcTemplate template;
    private DbMappings dbMappings;
    private ConfigDatabase transactionalConfigDatabase;

    public CloudJdbcConfigDatabase(
            final DataSource dataSource, final XStreamInfoSerialBinding binding) {
        super(dataSource, binding);
        this.dataSource = dataSource;
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * Override to dispose the internal cache for both the {@link ModificationProxy} wrapped object
     * (as it may contain identity references to other objects) and the provided {@code info} (which
     * can contain new references to other objects like workspace)
     */
    @Override
    @Transactional(
        transactionManager = "jdbcConfigTransactionManager",
        propagation = Propagation.REQUIRED,
        rollbackFor = Exception.class
    )
    public <T extends Info> T save(T info) {
        clearCache(ModificationProxy.unwrap(info));
        T saved = super.save(info);
        clearCache(saved);
        return saved;
    }

    /**
     * Override to dispose the internal cache for both the {@link ModificationProxy} wrapped object
     */
    @Override
    @Transactional(
        transactionManager = "jdbcConfigTransactionManager",
        propagation = Propagation.REQUIRED,
        rollbackFor = Exception.class
    )
    public void remove(Info info) {
        clearCache(ModificationProxy.unwrap(info));
        super.remove(info);
    }

    /**
     * Overrides to remove the {@link CatalogClearingListener} added by {@code super.setCatalog()},
     * we don't do caching here and the {@link CatalogClearingListener} produces null pointer
     * exceptions
     */
    public @Override void setCatalog(CatalogImpl catalog) {
        super.setCatalog(catalog);
        catalog.removeListeners(CatalogClearingListener.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        super.setApplicationContext(applicationContext);
        this.transactionalConfigDatabase = applicationContext.getBean(ConfigDatabase.class);
    }

    @Transactional(
        transactionManager = "jdbcConfigTransactionManager",
        propagation = Propagation.REQUIRED,
        rollbackFor = Exception.class
    )
    public @Override void initDb(@Nullable Resource resource) throws IOException {
        super.initDb(resource);
        this.dbMappings = new DbMappings(dialect());
        this.dbMappings.initDb(template);
    }

    /**
     * Override to use fixed version of {@link CapabilitiesFilterSplitter} until
     * https://osgeo-org.atlassian.net/browse/GEOT-6717 is fixed
     */
    public @Override <T extends CatalogInfo> int count(final Class<T> of, final Filter filter) {

        QueryBuilder<T> sqlBuilder = QueryBuilder.forCount(dialect, of, dbMappings).filter(filter);

        final StringBuilder sql = sqlBuilder.build();
        final Filter unsupportedFilter = sqlBuilder.getUnsupportedFilter();
        final boolean fullySupported = Filter.INCLUDE.equals(unsupportedFilter);
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Original filter: " + filter);
            LOGGER.finer("Supported filter: " + sqlBuilder.getSupportedFilter());
            LOGGER.finer("Unsupported filter: " + sqlBuilder.getUnsupportedFilter());
        }
        final int count;
        if (fullySupported) {
            final Map<String, Object> namedParameters = sqlBuilder.getNamedParameters();
            logStatement(sql, namedParameters);

            count = template.queryForObject(sql.toString(), namedParameters, Integer.class);
        } else {
            LOGGER.fine(
                    "Filter is not fully supported, doing scan of supported part to return the number of matches");
            // going the expensive route, filtering as much as possible
            CloseableIterator<T> iterator = query(of, filter, null, null, (SortBy) null);
            try {
                return Iterators.size(iterator);
            } finally {
                iterator.close();
            }
        }
        return count;
    }

    /**
     * Override to use fixed version of {@link CapabilitiesFilterSplitter} until
     * https://osgeo-org.atlassian.net/browse/GEOT-6717 is fixed
     */
    @Transactional(
        transactionManager = "jdbcConfigTransactionManager",
        propagation = Propagation.REQUIRED,
        readOnly = true
    )
    public <T extends Info> CloseableIterator<T> query(
            final Class<T> of,
            final Filter filter,
            @Nullable Integer offset,
            @Nullable Integer limit,
            @Nullable SortBy... sortOrder) {

        checkNotNull(of);
        checkNotNull(filter);
        checkArgument(offset == null || offset.intValue() >= 0);
        checkArgument(limit == null || limit.intValue() >= 0);

        QueryBuilder<T> sqlBuilder =
                QueryBuilder.forIds(dialect, of, dbMappings)
                        .filter(filter)
                        .offset(offset)
                        .limit(limit)
                        .sortOrder(sortOrder);
        final StringBuilder sql = sqlBuilder.build();

        List<String> ids = null;

        final SimplifyingFilterVisitor filterSimplifier = new SimplifyingFilterVisitor();
        final Filter simplifiedFilter =
                (Filter) sqlBuilder.getSupportedFilter().accept(filterSimplifier, null);
        if (simplifiedFilter instanceof PropertyIsEqualTo) {
            PropertyIsEqualTo isEqualTo = (PropertyIsEqualTo) simplifiedFilter;
            if (isEqualTo.getExpression1() instanceof PropertyName
                    && isEqualTo.getExpression2() instanceof Literal
                    && ((PropertyName) isEqualTo.getExpression1()).getPropertyName().equals("id")) {
                ids =
                        Collections.singletonList(
                                ((Literal) isEqualTo.getExpression2()).getValue().toString());
            }
            if (isEqualTo.getExpression2() instanceof PropertyName
                    && isEqualTo.getExpression1() instanceof Literal
                    && ((PropertyName) isEqualTo.getExpression2()).getPropertyName().equals("id")) {
                ids =
                        Collections.singletonList(
                                ((Literal) isEqualTo.getExpression1()).getValue().toString());
            }
        }

        final Filter unsupportedFilter = sqlBuilder.getUnsupportedFilter();
        final boolean fullySupported = Filter.INCLUDE.equals(unsupportedFilter);

        if (ids == null) {
            final Map<String, Object> namedParameters = sqlBuilder.getNamedParameters();

            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("Original filter: " + filter);
                LOGGER.finer("Supported filter: " + sqlBuilder.getSupportedFilter());
                LOGGER.finer("Unsupported filter: " + sqlBuilder.getUnsupportedFilter());
            }
            logStatement(sql, namedParameters);

            Stopwatch sw = Stopwatch.createStarted();
            // the oracle offset/limit implementation returns a two column result set
            // with rownum in the 2nd - queryForList will throw an exception
            ids =
                    template.query(
                            sql.toString(),
                            namedParameters,
                            new RowMapper<String>() {
                                @Override
                                public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                                    return rs.getString(1);
                                }
                            });
            sw.stop();
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(
                        Joiner.on("")
                                .join(
                                        "query returned ",
                                        ids.size(),
                                        " records in ",
                                        sw.toString()));
            }
        }

        List<T> lazyTransformed =
                Lists.transform(
                        ids,
                        new Function<String, T>() {
                            @Nullable
                            @Override
                            public T apply(String id) {
                                // tricky bit... transaction management works only if the method
                                // is called from a Spring proxy that processed the annotations,
                                // so we cannot call getId directly, it needs to be done from
                                // "outside"
                                return transactionalConfigDatabase.getById(id, of);
                            }
                        });

        CloseableIterator<T> result;
        Iterator<T> iterator =
                Iterators.filter(
                        lazyTransformed.iterator(), com.google.common.base.Predicates.notNull());

        if (fullySupported) {
            result = new CloseableIteratorAdapter<T>(iterator);
        } else {
            // Apply the filter
            result = CloseableIteratorAdapter.filter(iterator, filter);
            // The offset and limit should not have been applied as part of the query
            assert (!sqlBuilder.isOffsetLimitApplied());
            // Apply offset and limits after filtering
            result = applyOffsetLimit(result, offset, limit);
        }

        return result;
    }

    private <T extends Info> CloseableIterator<T> applyOffsetLimit(
            CloseableIterator<T> iterator, Integer offset, Integer limit) {
        if (offset != null) {
            Iterators.advance(iterator, offset.intValue());
        }
        if (limit != null) {
            iterator = CloseableIteratorAdapter.limit(iterator, limit.intValue());
        }
        return iterator;
    }

    /**
     * Override to use fixed version of {@link CapabilitiesFilterSplitter} until
     * https://osgeo-org.atlassian.net/browse/GEOT-6717 is fixed
     */
    @Transactional(
        transactionManager = "jdbcConfigTransactionManager",
        propagation = Propagation.REQUIRED,
        readOnly = true
    )
    public @Override <T extends Info> CloseableIterator<String> queryIds(
            final Class<T> of, final Filter filter) {

        checkNotNull(of);
        checkNotNull(filter);

        QueryBuilder<T> sqlBuilder = QueryBuilder.forIds(dialect, of, dbMappings).filter(filter);

        final StringBuilder sql = sqlBuilder.build();
        final Map<String, Object> namedParameters = sqlBuilder.getNamedParameters();
        final Filter unsupportedFilter = sqlBuilder.getUnsupportedFilter();
        final boolean fullySupported = Filter.INCLUDE.equals(unsupportedFilter);

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Original filter: " + filter);
            LOGGER.finer("Supported filter: " + sqlBuilder.getSupportedFilter());
            LOGGER.finer("Unsupported filter: " + sqlBuilder.getUnsupportedFilter());
        }
        logStatement(sql, namedParameters);

        Stopwatch sw = Stopwatch.createStarted();
        // the oracle offset/limit implementation returns a two column result set
        // with rownum in the 2nd - queryForList will throw an exception
        List<String> ids =
                template.query(
                        sql.toString(),
                        namedParameters,
                        new RowMapper<String>() {
                            @Override
                            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                                return rs.getString(1);
                            }
                        });
        sw.stop();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("query returned " + ids.size() + " records in " + sw);
        }

        CloseableIterator<String> result;
        Iterator<String> iterator =
                Iterators.filter(ids.iterator(), com.google.common.base.Predicates.notNull());

        if (fullySupported) {
            result = new CloseableIteratorAdapter<String>(iterator);
        } else {
            // Apply the filter
            result = CloseableIteratorAdapter.filter(iterator, filter);
            // The offset and limit should not have been applied as part of the query
            assert (!sqlBuilder.isOffsetLimitApplied());
        }

        return result;
    }

    private Dialect dialect() {
        if (dialect == null) {
            this.dialect = Dialect.detect(dataSource);
        }
        return dialect;
    }

    /**
     * Copy of package private class, can't extend. Uses {@link CapabilitiesFilterSplitterFix fixed
     * version} of {@link CapabilitiesFilterSplitter} until
     * https://osgeo-org.atlassian.net/browse/GEOT-6717 is fixed
     */
    static class QueryBuilder<T extends Info> {

        @SuppressWarnings("unused")
        private static final SortBy DEFAULT_ORDER =
                CommonFactoryFinder.getFilterFactory().sort("id", SortOrder.ASCENDING);

        private Integer offset;

        private Integer limit;

        private SortBy[] sortOrder;

        private final boolean isCountQuery;

        // yuck
        private final Dialect dialect;

        private Class<T> queryType;

        private FilterToCatalogSQL predicateBuilder;

        private DbMappings dbMappings;

        private Filter originalFilter;

        private Filter supportedFilter;

        private Filter unsupportedFilter;

        private boolean offsetLimitApplied = false;

        /** */
        private QueryBuilder(
                Dialect dialect,
                final Class<T> clazz,
                DbMappings dbMappings,
                final boolean isCountQuery) {
            this.dialect = dialect;
            this.queryType = clazz;
            this.dbMappings = dbMappings;
            this.isCountQuery = isCountQuery;
            this.originalFilter = this.supportedFilter = this.unsupportedFilter = Filter.INCLUDE;
        }

        public static <T extends Info> QueryBuilder<T> forCount(
                Dialect dialect, final Class<T> clazz, DbMappings dbMappings) {
            return new QueryBuilder<T>(dialect, clazz, dbMappings, true);
        }

        public static <T extends Info> QueryBuilder<T> forIds(
                Dialect dialect, final Class<T> clazz, DbMappings dbMappings) {
            return new QueryBuilder<T>(dialect, clazz, dbMappings, false);
        }

        public Filter getUnsupportedFilter() {
            return unsupportedFilter;
        }

        public Filter getSupportedFilter() {
            return supportedFilter;
        }

        public Map<String, Object> getNamedParameters() {
            Map<String, Object> params = Collections.emptyMap();
            if (predicateBuilder != null) {
                params = predicateBuilder.getNamedParameters();
            }
            return params;
        }

        public QueryBuilder<T> offset(Integer offset) {
            this.offset = offset;
            return this;
        }

        public QueryBuilder<T> limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public QueryBuilder<T> sortOrder(SortBy order) {
            if (order == null) {
                this.sortOrder();
            } else {
                this.sortOrder(new SortBy[] {order});
            }
            return this;
        }

        public QueryBuilder<T> sortOrder(SortBy... order) {
            if (order == null || order.length == 0) {
                this.sortOrder = null;
            } else {
                this.sortOrder = order;
            }
            return this;
        }

        public QueryBuilder<T> filter(Filter filter) {
            this.originalFilter = filter;
            return this;
        }

        private void querySortBy(StringBuilder query, StringBuilder whereClause, SortBy[] orders) {

            /*
             * Start with the oid and id from the object table selecting for type and the filter.
             *
             * Then left join on oid for each property to sort by to turn it into an attribute.
             *
             * The sort each of the created attribute.
             */

            // Need to put together the ORDER BY clause as we go and then add it at the end
            StringBuilder orderBy = new StringBuilder();
            orderBy.append("ORDER BY ");

            int i = 0;

            query.append("SELECT id FROM ");

            query.append("\n    (SELECT oid, id FROM object WHERE ");
            if (queryType != null) {
                query.append("type_id in (:types) /* ")
                        .append(queryType.getCanonicalName())
                        .append(" */\n      AND ");
            }
            query.append(whereClause).append(") object");

            for (SortBy order : orders) {
                final String sortProperty = order.getPropertyName().getPropertyName();
                final String subSelectName = "subSelect" + i;
                final String attributeName = "prop" + i;
                final String propertyParamName = "sortProperty" + i;

                final Set<Integer> sortPropertyTypeIds;
                sortPropertyTypeIds = dbMappings.getPropertyTypeIds(queryType, sortProperty);

                // Store the property type ID as a named parameter
                Map<String, Object> namedParameters = getNamedParameters();
                namedParameters.put(propertyParamName, sortPropertyTypeIds);

                query.append("\n  LEFT JOIN");
                query.append("\n    (SELECT oid, value ")
                        .append(attributeName)
                        .append(" FROM \n      object_property WHERE property_type IN (:")
                        .append(propertyParamName)
                        .append(")) ")
                        .append(subSelectName);

                query.append("  /* ")
                        .append(order.getPropertyName().getPropertyName())
                        .append(" ")
                        .append(ascDesc(order))
                        .append(" */");

                query.append("\n  ON object.oid = ").append(subSelectName).append(".oid");
                // Update the ORDER BY clause to be added later
                if (i > 0) orderBy.append(", ");
                orderBy.append(attributeName).append(" ").append(ascDesc(order));

                i++;
            }

            query.append("\n  ").append(orderBy);
        }

        private StringBuilder buildWhereClause() {
            final SimplifyingFilterVisitor filterSimplifier = new SimplifyingFilterVisitor();

            this.predicateBuilder = new FilterToCatalogSQL(this.queryType, this.dbMappings);
            Capabilities fcs = new Capabilities(FilterToCatalogSQL.CAPABILITIES);
            FeatureType parent = null;
            // use this to instruct the filter splitter which filters can be encoded depending on
            // whether a db mapping for a given property name exists
            ClientTransactionAccessor transactionAccessor =
                    new ClientTransactionAccessor() {

                        @Override
                        public Filter getUpdateFilter(final String attributePath) {
                            Set<PropertyType> propertyTypes;
                            propertyTypes = dbMappings.getPropertyTypes(queryType, attributePath);

                            final boolean isMappedProp = !propertyTypes.isEmpty();

                            if (isMappedProp) {
                                // continue normally
                                return null;
                            }
                            // tell the caps filter splitter this property name is not encodable
                            return Filter.EXCLUDE;
                        }

                        @Override
                        public Filter getDeleteFilter() {
                            return null;
                        }
                    };

            CapabilitiesFilterSplitterFix filterSplitter;
            filterSplitter = new CapabilitiesFilterSplitterFix(fcs, parent, transactionAccessor);

            final Filter filter = (Filter) this.originalFilter.accept(filterSimplifier, null);
            filter.accept(filterSplitter, null);

            Filter supported = filterSplitter.getFilterPre();
            Filter unsupported = filterSplitter.getFilterPost();
            Filter demultipliedFilter =
                    (Filter) supported.accept(new LiteralDemultiplyingFilterVisitor(), null);
            this.supportedFilter = (Filter) demultipliedFilter.accept(filterSimplifier, null);
            this.unsupportedFilter = (Filter) unsupported.accept(filterSimplifier, null);

            StringBuilder whereClause = new StringBuilder();
            return (StringBuilder) this.supportedFilter.accept(predicateBuilder, whereClause);
        }

        public StringBuilder build() {

            StringBuilder whereClause = buildWhereClause();

            StringBuilder query = new StringBuilder();
            if (isCountQuery) {
                if (Filter.INCLUDE.equals(this.originalFilter)) {
                    query.append("select count(oid) from object where type_id in (:types)");
                } else {
                    query.append("select count(oid) from object where type_id in (:types) AND (\n");
                    query.append(whereClause).append("\n)");
                }
            } else {
                SortBy[] orders = this.sortOrder;
                if (orders == null) {
                    query.append("select id from object where type_id in (:types) AND (\n");
                    query.append(whereClause).append(")\n");
                    query.append(" ORDER BY oid");
                } else {
                    querySortBy(query, whereClause, orders);
                }
                applyOffsetLimit(query);
            }

            return query;
        }

        /** When the query was built, were the offset and limit included. */
        public boolean isOffsetLimitApplied() {
            return offsetLimitApplied;
        }

        private static String ascDesc(SortBy order) {
            return SortOrder.ASCENDING.equals(order.getSortOrder()) ? "ASC" : "DESC";
        }

        protected void applyOffsetLimit(StringBuilder sql) {
            if (unsupportedFilter.equals(Filter.INCLUDE)) {
                dialect.applyOffsetLimit(sql, offset, limit);
                offsetLimitApplied = true;
            } else {
                offsetLimitApplied = false;
            }
        }
    }
}
