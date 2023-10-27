/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql.catalog.filter;

import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.filter.visitor.PostPreProcessFilterSplittingVisitor;

import java.util.Set;

/**
 * Splits a {@link Filter} into supported and unsupported filters for SQL encoding and
 * post-filtering, based on the supported column names provided in the constructor.
 *
 * @since 1.4
 */
class PgsqlCatalogFilterSplitter extends PostPreProcessFilterSplittingVisitor {

    private Set<String> supportedPropertyNames;

    public PgsqlCatalogFilterSplitter(Set<String> supportedPropertyNames) {
        super(PgsqlFilterCapabilities.capabilities(), null, null);
        this.supportedPropertyNames = supportedPropertyNames;
    }

    public static PgsqlCatalogFilterSplitter split(
            Filter filter, Set<String> supportedPropertyNames) {
        PgsqlCatalogFilterSplitter splitter =
                new PgsqlCatalogFilterSplitter(supportedPropertyNames);
        filter.accept(splitter, null);
        return splitter;
    }

    /**
     * If the property name is supported, proceeds with the splitting, otherwise aborts splitting
     * the current filter making it part of the unsupported filter result.
     */
    @Override
    public Object visit(PropertyName expression, Object notUsed) {
        final String propertyName = expression.getPropertyName();
        if (supportedPropertyNames.contains(propertyName)) {
            return super.visit(expression, notUsed);
        }

        postStack.push(expression);
        return null;
    }
}
