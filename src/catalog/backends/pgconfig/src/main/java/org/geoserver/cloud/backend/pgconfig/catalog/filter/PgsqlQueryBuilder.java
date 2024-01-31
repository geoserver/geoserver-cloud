/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.filter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.geoserver.cloud.backend.pgconfig.catalog.filter.PgsqlFilterToSQL.Result;
import org.geotools.api.filter.Filter;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;

import java.util.List;
import java.util.Set;

/**
 * @since 1.4
 */
@RequiredArgsConstructor
public class PgsqlQueryBuilder {

    private final Filter filter;
    private final Set<String> supportedPropertyNames;

    private @Getter Filter supportedFilter = Filter.INCLUDE;
    private @Getter Filter unsupportedFilter = Filter.INCLUDE;
    private @Getter String whereClause = "TRUE";

    private @Getter List<Object> literalValues;

    @SuppressWarnings("rawtypes")
    private @Getter List<Class> literalTypes;

    public PgsqlQueryBuilder build() {
        if (Filter.INCLUDE.equals(filter)) {
            return this;
        }
        var splitter = PgsqlCatalogFilterSplitter.split(filter, supportedPropertyNames);

        supportedFilter = adaptToSql(splitter.getFilterPre());
        unsupportedFilter = simplify(splitter.getFilterPost());

        Result encodeResult = PgsqlFilterToSQL.evaluate(supportedFilter);
        whereClause = encodeResult.getWhereClause();
        literalValues = encodeResult.getLiteralValues();
        literalTypes = encodeResult.getLiteralTypes();
        return this;
    }

    private Filter adaptToSql(Filter filterPre) {
        Filter supported = ToPgsqlCompatibleFilterDuplicator.adapt(filterPre);
        return simplify(supported);
    }

    private Filter simplify(Filter filter) {
        return SimplifyingFilterVisitor.simplify(filter);
    }
}
