/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql.catalog.filter;

import org.geotools.api.filter.BinaryComparisonOperator;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.MultiValuedFilter.MatchAction;
import org.geotools.api.filter.PropertyIsBetween;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.PropertyIsGreaterThan;
import org.geotools.api.filter.PropertyIsGreaterThanOrEqualTo;
import org.geotools.api.filter.PropertyIsLessThan;
import org.geotools.api.filter.PropertyIsLessThanOrEqualTo;
import org.geotools.api.filter.PropertyIsLike;
import org.geotools.api.filter.PropertyIsNotEqualTo;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Duplicates a supported filter making it directly translatable to SQL taking care of subtleties
 * like {@link MatchAction} and case matching.
 *
 * @since 1.4
 */
class ToPgsqlCompatibleFilterDuplicator extends DuplicatingFilterVisitor {

    public static Filter adapt(Filter filter) {
        ToPgsqlCompatibleFilterDuplicator adaptor = new ToPgsqlCompatibleFilterDuplicator();
        return (Filter) filter.accept(adaptor, null);
    }

    @Override
    public Object visit(PropertyName expression, Object extraData) {
        boolean matchCase = true;
        if (extraData instanceof Boolean match) matchCase = match;
        if (!matchCase) {
            return getFactory(null).function("strToLowerCase", expression);
        }
        return super.visit(expression, extraData);
    }

    @Override
    public Object visit(Literal expression, Object extraData) {
        boolean matchCase = true;
        if (extraData instanceof Boolean match) matchCase = match;
        if (!matchCase) {
            return getFactory(null).function("strToLowerCase", expression);
        }
        return super.visit(expression, extraData);
    }

    @Override
    public Object visit(PropertyIsBetween filter, Object extraData) {
        Expression expr = visit(filter.getExpression(), extraData);
        Expression lower = visit(filter.getLowerBoundary(), extraData);
        Expression upper = visit(filter.getUpperBoundary(), extraData);
        return getFactory(extraData).between(expr, lower, upper, filter.getMatchAction());
    }

    @Override
    public Object visit(PropertyIsEqualTo filter, Object extraData) {
        BinaryComparisonOperator dup =
                (BinaryComparisonOperator) super.visit(filter, filter.isMatchingCase());
        return visitBinaryComparisonOperator(dup);
    }

    @Override
    public Object visit(PropertyIsNotEqualTo filter, Object extraData) {
        BinaryComparisonOperator dup =
                (BinaryComparisonOperator) super.visit(filter, filter.isMatchingCase());
        return visitBinaryComparisonOperator(dup);
    }

    @Override
    public Object visit(PropertyIsGreaterThan filter, Object extraData) {
        BinaryComparisonOperator dup =
                (BinaryComparisonOperator) super.visit(filter, filter.isMatchingCase());
        return visitBinaryComparisonOperator(dup);
    }

    @Override
    public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object extraData) {
        BinaryComparisonOperator dup =
                (BinaryComparisonOperator) super.visit(filter, filter.isMatchingCase());
        return visitBinaryComparisonOperator(dup);
    }

    @Override
    public Object visit(PropertyIsLessThan filter, Object extraData) {
        BinaryComparisonOperator dup =
                (BinaryComparisonOperator) super.visit(filter, filter.isMatchingCase());
        return visitBinaryComparisonOperator(dup);
    }

    @Override
    public Object visit(PropertyIsLessThanOrEqualTo filter, Object extraData) {
        BinaryComparisonOperator dup =
                (BinaryComparisonOperator) super.visit(filter, filter.isMatchingCase());
        return visitBinaryComparisonOperator(dup);
    }

    @Override
    public Object visit(PropertyIsLike filter, Object extraData) {
        return super.visit(filter, extraData);
    }

    protected Filter visitBinaryComparisonOperator(BinaryComparisonOperator filter) {
        Expression e1 = filter.getExpression1();
        Expression e2 = filter.getExpression2();

        Literal literal = e1 instanceof Literal l ? l : (e2 instanceof Literal l ? l : null);

        Object value = null == literal ? null : literal.getValue();
        if (!(value instanceof Collection)) {
            return filter;
        }

        List<Object> values = new ArrayList<>((Collection<?>) value);
        final MatchAction matchAction = filter.getMatchAction();
        final BiFunction<Expression, Expression, Filter> filterBuilder = filterBuilder(filter);
        Function<List<Filter>, Filter> aggregateBuilder;
        final Expression left = literal == e1 ? e2 : e1;
        switch (matchAction) {
            case ALL:
                // only if all of the possible combinations match, the result is true (aggregated
                // AND)
                aggregateBuilder = ff::and;
                break;
            case ANY:
                // if any of the possible combinations match, the result is true (aggregated OR)
                aggregateBuilder = ff::or;
                break;
            case ONE:
                // only if exactly one of the possible combinations match, the result is true
                // (aggregated XOR)
                aggregateBuilder = ff::or;
                List<Filter> xor = new ArrayList<>();
                for (int i = 0; i < values.size(); i++) {
                    Filter tomatch = filterBuilder.apply(left, ff.literal(values.get(i)));
                    List<Filter> tomiss = new ArrayList<>();
                    for (int j = 0; j < values.size(); j++) {
                        if (j == i) continue;
                        tomiss.add(filterBuilder.apply(left, ff.literal(values.get(j))));
                    }
                    xor.add(ff.and(tomatch, ff.not(ff.or(tomiss))));
                }
                Filter xored = ff.or(xor);
                xored.accept(this, null);
                return xored;
            default:
                throw new IllegalStateException();
        }

        List<Filter> subfilters = new ArrayList<>();
        for (Object v : values) {
            Literal right = ff.literal(v);
            Filter subfilter = filterBuilder.apply(left, right);
            subfilters.add(subfilter);
        }
        Filter replacement = aggregateBuilder.apply(subfilters);
        replacement.accept(this, null);
        return replacement;
    }

    private BiFunction<Expression, Expression, Filter> filterBuilder(
            BinaryComparisonOperator orig) {

        if (orig instanceof PropertyIsEqualTo)
            return (e1, e2) -> ff.equal(e1, e2, orig.isMatchingCase());

        if (orig instanceof PropertyIsGreaterThan)
            return (e1, e2) -> ff.greater(e1, e2, orig.isMatchingCase());

        if (orig instanceof PropertyIsGreaterThanOrEqualTo)
            return (e1, e2) -> ff.greaterOrEqual(e1, e2, orig.isMatchingCase());

        if (orig instanceof PropertyIsLessThan)
            return (e1, e2) -> ff.less(e1, e2, orig.isMatchingCase());

        if (orig instanceof PropertyIsLessThanOrEqualTo)
            return (e1, e2) -> ff.lessOrEqual(e1, e2, orig.isMatchingCase());

        if (orig instanceof PropertyIsNotEqualTo)
            return (e1, e2) -> ff.notEqual(e1, e2, orig.isMatchingCase());

        throw new IllegalArgumentException("Unknown BinaryComparisonOperator: " + orig);
    }
}
