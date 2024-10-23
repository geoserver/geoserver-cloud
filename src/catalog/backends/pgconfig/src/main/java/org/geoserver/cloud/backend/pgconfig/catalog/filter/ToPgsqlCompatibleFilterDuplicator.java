/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geotools.api.filter.BinaryComparisonOperator;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.MultiValuedFilter.MatchAction;
import org.geotools.api.filter.PropertyIsBetween;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.PropertyIsGreaterThan;
import org.geotools.api.filter.PropertyIsGreaterThanOrEqualTo;
import org.geotools.api.filter.PropertyIsLessThan;
import org.geotools.api.filter.PropertyIsLessThanOrEqualTo;
import org.geotools.api.filter.PropertyIsNotEqualTo;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;

/**
 * Duplicates a supported filter making it directly translatable to SQL taking care of subtleties
 * like {@link MatchAction} and case matching.
 *
 * @since 1.4
 */
@RequiredArgsConstructor
class ToPgsqlCompatibleFilterDuplicator extends DuplicatingFilterVisitor {

    @NonNull
    private final Set<String> supportedPropertyNames;

    /**
     * @param supportedFilter Filter that's already been deemed as supported
     * @param supportedPropertyNames
     * @return
     */
    public static Filter adapt(Filter supportedFilter, Set<String> supportedPropertyNames) {
        var adaptor = new ToPgsqlCompatibleFilterDuplicator(supportedPropertyNames);
        return (Filter) supportedFilter.accept(adaptor, null);
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
    public PropertyIsBetween visit(PropertyIsBetween filter, Object extraData) {
        Expression expr = visit(filter.getExpression(), extraData);
        Expression lower = visit(filter.getLowerBoundary(), extraData);
        Expression upper = visit(filter.getUpperBoundary(), extraData);
        return getFactory(extraData).between(expr, lower, upper, filter.getMatchAction());
    }

    @Override
    public Filter visit(PropertyIsEqualTo filter, Object extraData) {
        return adaptMatchActionForCollectionLiteral(
                (BinaryComparisonOperator) super.visit(filter, filter.isMatchingCase()));
    }

    @Override
    public Filter visit(PropertyIsNotEqualTo filter, Object extraData) {
        return adaptMatchActionForCollectionLiteral(
                (BinaryComparisonOperator) super.visit(filter, filter.isMatchingCase()));
    }

    @Override
    public Filter visit(PropertyIsGreaterThan filter, Object extraData) {
        return adaptMatchActionForCollectionLiteral(
                (BinaryComparisonOperator) super.visit(filter, filter.isMatchingCase()));
    }

    @Override
    public Filter visit(PropertyIsGreaterThanOrEqualTo filter, Object extraData) {
        return adaptMatchActionForCollectionLiteral(
                (BinaryComparisonOperator) super.visit(filter, filter.isMatchingCase()));
    }

    @Override
    public Filter visit(PropertyIsLessThan filter, Object extraData) {
        return adaptMatchActionForCollectionLiteral(
                (BinaryComparisonOperator) super.visit(filter, filter.isMatchingCase()));
    }

    @Override
    public Filter visit(PropertyIsLessThanOrEqualTo filter, Object extraData) {
        return adaptMatchActionForCollectionLiteral(
                (BinaryComparisonOperator) super.visit(filter, filter.isMatchingCase()));
    }

    protected Filter adaptMatchActionForCollectionLiteral(final BinaryComparisonOperator filter) {
        final Expression leftExpr = determineLeftExpression(filter);
        final @Nullable Literal rightExpr = determineRightExpression(filter);
        final List<Object> values = getValueIfCollection(rightExpr);
        if (null == values) {
            return filter;
        }

        final MatchAction matchAction = filter.getMatchAction();

        if (matchAction == MatchAction.ONE) {
            return aggregateXor(filter, leftExpr, values);
        }

        final BiFunction<Expression, Expression, Filter> filterBuilder = filterBuilder(filter);
        Function<List<Filter>, Filter> aggregateBuilder = createOredOrAndedBuilder(matchAction);

        List<Filter> subfilters = new ArrayList<>();
        for (Object v : values) {
            Literal right = ff.literal(v);
            Filter subfilter = filterBuilder.apply(leftExpr, right);
            subfilters.add(subfilter);
        }
        Filter replacement = aggregateBuilder.apply(subfilters);
        replacement.accept(this, null);
        return replacement;
    }

    private Function<List<Filter>, Filter> createOredOrAndedBuilder(final MatchAction matchAction) {
        return switch (matchAction) {
                // if all of the possible combinations match, the result is true (aggregated AND)
            case ALL -> ff::and;
                // if any of the possible combinations match, the result is true (aggregated OR)
            case ANY -> ff::or;
            default -> throw new IllegalStateException();
        };
    }

    /**
     * Only if exactly one of the possible combinations match, the result is true (aggregated XOR)
     */
    private Filter aggregateXor(BinaryComparisonOperator origFilter, Expression leftExpr, final List<Object> values) {

        final BiFunction<Expression, Expression, Filter> filterBuilder = filterBuilder(origFilter);
        List<Filter> xor = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            Filter tomatch = filterBuilder.apply(leftExpr, ff.literal(values.get(i)));
            List<Filter> tomiss = new ArrayList<>();
            for (int j = 0; j < values.size(); j++) {
                if (j == i) continue;
                tomiss.add(filterBuilder.apply(leftExpr, ff.literal(values.get(j))));
            }
            xor.add(ff.and(tomatch, ff.not(ff.or(tomiss))));
        }
        Filter xored = ff.or(xor);
        xored.accept(this, null);
        return xored;
    }

    private List<Object> getValueIfCollection(@Nullable Literal literal) {
        List<Object> values = null;
        if (null != literal) {
            Object value = literal.getValue();
            if (value instanceof Collection) {
                values = new ArrayList<>((Collection<?>) value);
            }
        }
        return values;
    }

    /**
     * The left expression is the one that's not a {@link Literal}. If both are {@literal Literal}s,
     * the order is respected and {@link BinaryComparisonOperator#getExpression1()
     * filter.getExpression1()} is returned
     */
    private Expression determineLeftExpression(BinaryComparisonOperator filter) {
        Expression left = filter.getExpression1();
        Expression right = filter.getExpression2();
        if (left instanceof Literal) {
            return right instanceof Literal ? left : right;
        }
        return left;
    }

    /**
     * The right expression is the one that is a {@link Literal}. If both are {@literal Literal}s,
     * the order is respected and {@link BinaryComparisonOperator#getExpression2()
     * filter.getExpression2()} is returned
     */
    private Literal determineRightExpression(BinaryComparisonOperator filter) {
        Expression left = filter.getExpression1();
        Expression right = filter.getExpression2();

        if (left instanceof Literal l) {
            return right instanceof Literal r ? r : l;
        }
        return right instanceof Literal r ? r : null;
    }

    private BiFunction<Expression, Expression, Filter> filterBuilder(BinaryComparisonOperator orig) {

        if (orig instanceof PropertyIsEqualTo) return (e1, e2) -> ff.equal(e1, e2, orig.isMatchingCase());

        if (orig instanceof PropertyIsGreaterThan) return (e1, e2) -> ff.greater(e1, e2, orig.isMatchingCase());

        if (orig instanceof PropertyIsGreaterThanOrEqualTo)
            return (e1, e2) -> ff.greaterOrEqual(e1, e2, orig.isMatchingCase());

        if (orig instanceof PropertyIsLessThan) return (e1, e2) -> ff.less(e1, e2, orig.isMatchingCase());

        if (orig instanceof PropertyIsLessThanOrEqualTo)
            return (e1, e2) -> ff.lessOrEqual(e1, e2, orig.isMatchingCase());

        if (orig instanceof PropertyIsNotEqualTo) return (e1, e2) -> ff.notEqual(e1, e2, orig.isMatchingCase());

        throw new IllegalArgumentException("Unknown BinaryComparisonOperator: %s".formatted(orig));
    }
}
