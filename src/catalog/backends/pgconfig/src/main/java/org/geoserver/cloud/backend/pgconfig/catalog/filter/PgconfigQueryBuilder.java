/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.catalog.filter;

import static org.geoserver.cloud.backend.pgconfig.catalog.filter.CatalogInfoLiteralAdaptor.replaceCatalogInfoLiterals;
import static org.geoserver.cloud.backend.pgconfig.catalog.filter.PgconfigCatalogFilterSplitter.split;

import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.geoserver.cloud.backend.pgconfig.catalog.filter.PgconfigFilterToSQL.Result;
import org.geotools.api.filter.Filter;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;

/**
 * @since 1.4
 */
@RequiredArgsConstructor
public class PgconfigQueryBuilder {

    private final Filter origFilter;
    private final Set<String> supportedPropertyNames;

    private @Getter Filter supportedFilter = Filter.INCLUDE;
    private @Getter Filter unsupportedFilter = Filter.INCLUDE;
    private @Getter String whereClause = "TRUE";

    private @Getter List<Object> literalValues;

    @SuppressWarnings("rawtypes")
    private @Getter List<Class> literalTypes;

    public PgconfigQueryBuilder build() {
        if (Filter.INCLUDE.equals(origFilter)) {
            return this;
        }

        Filter filter = replaceCatalogInfoLiterals(origFilter, supportedPropertyNames);
        filter = simplify(filter);

        var splitter = split(filter, supportedPropertyNames);

        supportedFilter = adaptToSql(splitter.getFilterPre());

        unsupportedFilter = simplify(splitter.getFilterPost());

        Result encodeResult = PgconfigFilterToSQL.evaluate(supportedFilter);
        whereClause = encodeResult.getWhereClause();
        literalValues = encodeResult.getLiteralValues();
        literalTypes = encodeResult.getLiteralTypes();
        return this;
    }

    private Filter adaptToSql(Filter filterPre) {
        Filter supported = ToPgsqlCompatibleFilterDuplicator.adapt(filterPre, supportedPropertyNames);
        return simplify(supported);
    }

    private Filter simplify(Filter filter) {
        return SimplifyingFilterVisitor.simplify(filter);
    }
}
