/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.filter;

import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.api.filter.BinaryComparisonOperator;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.MultiValuedFilter.MatchAction;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.PropertyIsGreaterThan;
import org.geotools.api.filter.PropertyIsGreaterThanOrEqualTo;
import org.geotools.api.filter.PropertyIsLessThan;
import org.geotools.api.filter.PropertyIsLessThanOrEqualTo;
import org.geotools.api.filter.PropertyIsNotEqualTo;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;

/**
 * Converts binary comparison operators using a property name and a {@link CatalogInfo} instance
 * literal as a comparison by id.
 *
 * <p>For example, a filter like {@code workspace = WorkspaceInfo[test]} is translated to {@code
 * workspace.id = WorkspaceInfo[test].id}, where {@code WorkspaceInfo[test]} is an instance of a
 * {@link WorkspaceInfo}.
 *
 * <p>
 *
 * @since 1.8.1
 */
@Slf4j(topic = "org.geoserver.cloud.backend.pgconfig.catalog.filter")
class CatalogInfoLiteralAdaptor extends DuplicatingFilterVisitor {

    private static final FilterFactory FACTORY = CommonFactoryFinder.getFilterFactory(null);

    private Set<String> supportedPropertyNames;

    public CatalogInfoLiteralAdaptor(@NonNull Set<String> supportedPropertyNames) {
        super(FACTORY);
        this.supportedPropertyNames = supportedPropertyNames;
    }

    public static Filter replaceCatalogInfoLiterals(Filter filter, Set<String> supportedPropertyNames) {
        var adaptor = new CatalogInfoLiteralAdaptor(supportedPropertyNames);
        return (Filter) filter.accept(adaptor, null);
    }

    @Override
    public Object visit(PropertyIsEqualTo filter, Object notUsed) {
        return super.visit(adapt(filter, ff::equal), notUsed);
    }

    @Override
    public Object visit(PropertyIsNotEqualTo filter, Object notUsed) {
        return super.visit(adapt(filter, ff::notEqual), notUsed);
    }

    @Override
    public Object visit(PropertyIsGreaterThan filter, Object notUsed) {
        return super.visit(adapt(filter, ff::greater), notUsed);
    }

    @Override
    public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object notUsed) {
        return super.visit(adapt(filter, ff::greaterOrEqual), notUsed);
    }

    @Override
    public Object visit(PropertyIsLessThan filter, Object notUsed) {
        return super.visit(adapt(filter, ff::less), notUsed);
    }

    @Override
    public Object visit(PropertyIsLessThanOrEqualTo filter, Object notUsed) {
        return super.visit(adapt(filter, ff::lessOrEqual), notUsed);
    }

    @FunctionalInterface
    private static interface BinaryComparisonBuilder<F extends BinaryComparisonOperator> {
        F build(Expression left, Expression right, boolean matchCase, MatchAction matchAction);
    }

    /**
     * Converts a binary comparision operator using a property name and a {@link CatalogInfo}
     * instance literal as a comparison by id.
     *
     * <p>For example, a filter like {@code workspace = WorkspaceInfo} is translated to {@code
     * workspace.id = WorkspaceInfo.id}, where {@code WorkspaceInfo} is an instanceo of a {@link
     * WorkspaceInfo}.
     */
    private <F extends BinaryComparisonOperator> F adapt(F filter, BinaryComparisonBuilder<F> builder) {

        PropertyName prop = propertyName(filter);
        Literal literal = literal(filter);
        if (prop != null && literal != null) {
            final Filter orig = filter;
            String propertyName = prop.getPropertyName();
            Object value = literal.getValue();
            if (value instanceof CatalogInfo info) {
                String idProp = "%s.id".formatted(propertyName);
                if (supportedPropertyNames.contains(idProp)) {
                    prop = ff.property(idProp);
                    literal = ff.literal(info.getId());
                    boolean matchingCase = filter.isMatchingCase();
                    MatchAction matchAction = filter.getMatchAction();
                    filter = builder.build(prop, literal, matchingCase, matchAction);
                    log.debug("Fitler with CatalogInfo literal '{}' translated to '{}'", orig, filter);
                }
            }
        }

        return filter;
    }

    private PropertyName propertyName(BinaryComparisonOperator filter) {
        return propertyName(filter.getExpression1())
                .or(() -> propertyName(filter.getExpression2()))
                .orElse(null);
    }

    private Literal literal(BinaryComparisonOperator filter) {
        return literal(filter.getExpression1())
                .or(() -> literal(filter.getExpression2()))
                .orElse(null);
    }

    private Optional<PropertyName> propertyName(Expression e) {
        return Optional.of(e).filter(PropertyName.class::isInstance).map(PropertyName.class::cast);
    }

    private Optional<Literal> literal(Expression e) {
        return Optional.of(e).filter(Literal.class::isInstance).map(Literal.class::cast);
    }
}
