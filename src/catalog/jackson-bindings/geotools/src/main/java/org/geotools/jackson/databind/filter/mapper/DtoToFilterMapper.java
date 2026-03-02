/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.filter.mapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Generated;
import org.geotools.api.filter.And;
import org.geotools.api.filter.ExcludeFilter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.Id;
import org.geotools.api.filter.IncludeFilter;
import org.geotools.api.filter.MultiValuedFilter.MatchAction;
import org.geotools.api.filter.NativeFilter;
import org.geotools.api.filter.Not;
import org.geotools.api.filter.Or;
import org.geotools.api.filter.PropertyIsBetween;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.PropertyIsGreaterThan;
import org.geotools.api.filter.PropertyIsGreaterThanOrEqualTo;
import org.geotools.api.filter.PropertyIsLessThan;
import org.geotools.api.filter.PropertyIsLessThanOrEqualTo;
import org.geotools.api.filter.PropertyIsLike;
import org.geotools.api.filter.PropertyIsNil;
import org.geotools.api.filter.PropertyIsNotEqualTo;
import org.geotools.api.filter.PropertyIsNull;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.identity.Identifier;
import org.geotools.api.filter.spatial.BBOX;
import org.geotools.api.filter.spatial.Beyond;
import org.geotools.api.filter.spatial.Contains;
import org.geotools.api.filter.spatial.Crosses;
import org.geotools.api.filter.spatial.DWithin;
import org.geotools.api.filter.spatial.Disjoint;
import org.geotools.api.filter.spatial.Equals;
import org.geotools.api.filter.spatial.Intersects;
import org.geotools.api.filter.spatial.Overlaps;
import org.geotools.api.filter.spatial.Touches;
import org.geotools.api.filter.spatial.Within;
import org.geotools.api.filter.temporal.After;
import org.geotools.api.filter.temporal.AnyInteracts;
import org.geotools.api.filter.temporal.Before;
import org.geotools.api.filter.temporal.Begins;
import org.geotools.api.filter.temporal.BegunBy;
import org.geotools.api.filter.temporal.During;
import org.geotools.api.filter.temporal.EndedBy;
import org.geotools.api.filter.temporal.Ends;
import org.geotools.api.filter.temporal.Meets;
import org.geotools.api.filter.temporal.MetBy;
import org.geotools.api.filter.temporal.OverlappedBy;
import org.geotools.api.filter.temporal.TContains;
import org.geotools.api.filter.temporal.TEquals;
import org.geotools.api.filter.temporal.TOverlaps;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.IsEqualsToImpl;
import org.geotools.filter.IsGreaterThanImpl;
import org.geotools.filter.IsGreaterThanOrEqualToImpl;
import org.geotools.filter.IsLessThenImpl;
import org.geotools.filter.IsLessThenOrEqualToImpl;
import org.geotools.filter.IsNotEqualToImpl;
import org.geotools.filter.spatial.BBOXImpl;
import org.geotools.filter.spatial.BeyondImpl;
import org.geotools.filter.spatial.ContainsImpl;
import org.geotools.filter.spatial.CrossesImpl;
import org.geotools.filter.spatial.DWithinImpl;
import org.geotools.filter.spatial.DisjointImpl;
import org.geotools.filter.spatial.EqualsImpl;
import org.geotools.filter.spatial.IntersectsImpl;
import org.geotools.filter.spatial.OverlapsImpl;
import org.geotools.filter.spatial.TouchesImpl;
import org.geotools.filter.spatial.WithinImpl;
import org.geotools.filter.temporal.AfterImpl;
import org.geotools.filter.temporal.AnyInteractsImpl;
import org.geotools.filter.temporal.BeforeImpl;
import org.geotools.filter.temporal.BeginsImpl;
import org.geotools.filter.temporal.BegunByImpl;
import org.geotools.filter.temporal.DuringImpl;
import org.geotools.filter.temporal.EndedByImpl;
import org.geotools.filter.temporal.EndsImpl;
import org.geotools.filter.temporal.MeetsImpl;
import org.geotools.filter.temporal.MetByImpl;
import org.geotools.filter.temporal.OverlappedByImpl;
import org.geotools.filter.temporal.TContainsImpl;
import org.geotools.filter.temporal.TEqualsImpl;
import org.geotools.filter.temporal.TOverlapsImpl;
import org.geotools.jackson.databind.filter.dto.FilterDto;
import org.mapstruct.AnnotateWith;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(config = FilterMapperConfig.class)
@AnnotateWith(value = Generated.class)
abstract class DtoToFilterMapper {

    private FilterFactory ff = CommonFactoryFinder.getFilterFactory();
    private ExpressionMapper expm = Mappers.getMapper(ExpressionMapper.class);
    private final GeoToolsValueMappers valueMappers = Mappers.getMapper(GeoToolsValueMappers.class);

    private Expression exp(org.geotools.jackson.databind.filter.dto.ExpressionDto e) {
        return expm.map(e);
    }

    public org.geotools.api.filter.Filter map(org.geotools.jackson.databind.filter.dto.FilterDto dto) {
        if (dto == null) {
            return null;
        }
        final Class<? extends FilterDto> dtoFilterType = dto.getClass();
        Method mapperMethod;
        try {
            mapperMethod = getClass().getMethod("toFilter", dtoFilterType);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException(e);
        }
        org.geotools.api.filter.Filter filter;
        try {
            filter = (org.geotools.api.filter.Filter) mapperMethod.invoke(this, dto);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
        return filter;
    }

    public abstract IncludeFilter toFilter(FilterDto.IncludeFilterDto dto);

    public abstract ExcludeFilter toFilter(FilterDto.ExcludeFilterDto dto);

    public PropertyIsNil toFilter(FilterDto.PropertyIsNilDto dto) {
        return ff.isNil(exp(dto.getExpression()), dto.getNilReason());
    }

    public PropertyIsNull toFilter(FilterDto.PropertyIsNullDto dto) {
        return ff.isNull(exp(dto.getExpression()));
    }

    public NativeFilter toFilter(FilterDto.NativeFilterDto dto) {
        return ff.nativeFilter(dto.getNative());
    }

    public PropertyIsLike toFilter(FilterDto.PropertyIsLikeDto dto) {
        MatchAction matchAction = valueMappers.matchAction(dto.getMatchAction());
        return ff.like(
                exp(dto.getExpression()),
                dto.getLiteral(),
                dto.getWildCard(),
                dto.getSingleChar(),
                dto.getEscape(),
                dto.isMatchingCase(),
                matchAction);
    }

    public PropertyIsBetween toFilter(FilterDto.PropertyIsBetweenDto dto) {
        Expression expression = exp(dto.getExpression());
        Expression lower = exp(dto.getLowerBoundary());
        Expression upper = exp(dto.getUpperBoundary());
        MatchAction matchAction = valueMappers.matchAction(dto.getMatchAction());
        return ff.between(expression, lower, upper, matchAction);
    }

    @IterableMapping(elementTargetType = org.geotools.api.filter.Filter.class)
    protected abstract List<org.geotools.api.filter.Filter> list(List<FilterDto> dtos);

    public And toFilter(FilterDto.BinaryLogicOperatorDto.And dto) {
        return ff.and(list(dto.getChildren()));
    }

    public Or toFilter(FilterDto.BinaryLogicOperatorDto.Or dto) {
        return ff.or(list(dto.getChildren()));
    }

    public Not toFilter(FilterDto.NotDto dto) {
        return ff.not(map(dto.getFilter()));
    }

    public Id toFilter(FilterDto.IdDto dto) {
        Set<FilterDto.IdDto.FeatureId> identifiers =
                dto.getIdentifiers() == null ? Collections.emptySet() : dto.getIdentifiers();

        Set<? extends Identifier> ids =
                identifiers.stream().map(this::toIdentifier).collect(Collectors.toSet());

        return ff.id(ids);
    }

    Identifier toIdentifier(FilterDto.IdDto.FeatureId dto) {
        if (dto == null) {
            return null;
        }
        if (dto instanceof FilterDto.IdDto.ResourceId rid) {
            if (rid.getStartTime() != null || rid.getEndTime() != null) {
                return ff.resourceId(rid.getId(), rid.getStartTime(), rid.getEndTime());
            }
            throw new UnsupportedOperationException();
        }
        if (dto instanceof FilterDto.IdDto.FeatureId) {
            String id = dto.getId();
            String featureVersion = dto.getFeatureVersion();
            if (featureVersion == null) {
                return ff.featureId(id);
            }
            return ff.featureId(id, featureVersion);
        }
        throw new IllegalArgumentException(
                "Unsupported identifier type: %s".formatted(dto.getClass().getCanonicalName()));
    }

    private static @FunctionalInterface interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    private static @FunctionalInterface interface QuadFunction<A, B, C, D, R> {
        R apply(A a, B b, C c, D d);
    }

    private <T extends org.geotools.api.filter.Filter> T toBinaryOperator(
            FilterDto.BinaryOperatorDto dto, TriFunction<Expression, Expression, MatchAction, T> factory) {

        Expression e1 = exp(dto.getExpression1());
        Expression e2 = exp(dto.getExpression2());
        MatchAction matchAction = valueMappers.matchAction(dto.getMatchAction());
        return factory.apply(e1, e2, matchAction);
    }

    private <T extends org.geotools.api.filter.BinaryComparisonOperator> T toBinaryComparisonOperator(
            FilterDto.BinaryComparisonOperatorDto dto,
            QuadFunction<Expression, Expression, Boolean, MatchAction, T> factory) {

        Expression e1 = exp(dto.getExpression1());
        Expression e2 = exp(dto.getExpression2());
        MatchAction matchAction = valueMappers.matchAction(dto.getMatchAction());
        Boolean matchCase = Boolean.valueOf(dto.isMatchingCase());

        return factory.apply(e1, e2, matchCase, matchAction);
    }

    public Within toFilter(FilterDto.BinarySpatialOperatorDto.WithinDto dto) {
        return toBinaryOperator(dto, WithinImpl::new);
    }

    public Touches toFilter(FilterDto.BinarySpatialOperatorDto.TouchesDto dto) {
        return toBinaryOperator(dto, TouchesImpl::new);
    }

    public Overlaps toFilter(FilterDto.BinarySpatialOperatorDto.OverlapsDto dto) {
        return toBinaryOperator(dto, OverlapsImpl::new);
    }

    public Intersects toFilter(FilterDto.BinarySpatialOperatorDto.IntersectsDto dto) {
        return toBinaryOperator(dto, IntersectsImpl::new);
    }

    public Equals toFilter(FilterDto.BinarySpatialOperatorDto.EqualsDto dto) {
        return toBinaryOperator(dto, EqualsImpl::new);
    }

    public Disjoint toFilter(FilterDto.BinarySpatialOperatorDto.DisjointDto dto) {
        return toBinaryOperator(dto, DisjointImpl::new);
    }

    public Crosses toFilter(FilterDto.BinarySpatialOperatorDto.CrossesDto dto) {
        return toBinaryOperator(dto, CrossesImpl::new);
    }

    public Contains toFilter(FilterDto.BinarySpatialOperatorDto.ContainsDto dto) {
        return toBinaryOperator(dto, ContainsImpl::new);
    }

    public BBOX toFilter(FilterDto.BinarySpatialOperatorDto.BBOXDto dto) {
        return toBinaryOperator(dto, BBOXImpl::new);
    }

    public Beyond toFilter(FilterDto.BinarySpatialOperatorDto.BeyondDto dto) {
        BeyondImpl impl = toBinaryOperator(dto, BeyondImpl::new);
        impl.setDistance(dto.getDistance());
        impl.setUnits(dto.getDistanceUnits());
        return impl;
    }

    public DWithin toFilter(FilterDto.BinarySpatialOperatorDto.DWithinDto dto) {
        DWithinImpl impl = toBinaryOperator(dto, DWithinImpl::new);
        impl.setDistance(dto.getDistance());
        impl.setUnits(dto.getDistanceUnits());
        return impl;
    }

    private static class PropertyEquals extends IsEqualsToImpl {
        public PropertyEquals(
                Expression expression1, Expression expression2, boolean matchCase, MatchAction matchAction) {
            super(expression1, expression2, matchCase, matchAction);
        }
    }

    private static class PropertyNotEquals extends IsNotEqualToImpl {
        public PropertyNotEquals(
                Expression expression1, Expression expression2, boolean matchCase, MatchAction matchAction) {
            super(expression1, expression2, matchCase, matchAction);
        }
    }

    private static class PropertyLessThan extends IsLessThenImpl {
        public PropertyLessThan(
                Expression expression1, Expression expression2, boolean matchCase, MatchAction matchAction) {
            super(expression1, expression2, matchCase, matchAction);
        }
    }

    private static class PropertyLessThanOrEquals extends IsLessThenOrEqualToImpl {
        public PropertyLessThanOrEquals(
                Expression expression1, Expression expression2, boolean matchCase, MatchAction matchAction) {
            super(expression1, expression2, matchCase, matchAction);
        }
    }

    private static class PropertyGreaterThan extends IsGreaterThanImpl {
        public PropertyGreaterThan(
                Expression expression1, Expression expression2, boolean matchCase, MatchAction matchAction) {
            super(expression1, expression2, matchAction);
        }
    }

    private static class PropertyGreaterThanOrEqual extends IsGreaterThanOrEqualToImpl {
        public PropertyGreaterThanOrEqual(
                Expression expression1, Expression expression2, boolean matchCase, MatchAction matchAction) {
            super(expression1, expression2, matchCase, matchAction);
        }
    }

    public PropertyIsEqualTo toFilter(FilterDto.BinaryComparisonOperatorDto.PropertyIsEqualToDto dto) {
        return toBinaryComparisonOperator(dto, PropertyEquals::new);
    }

    public PropertyIsNotEqualTo toFilter(FilterDto.BinaryComparisonOperatorDto.PropertyIsNotEqualToDto dto) {
        return toBinaryComparisonOperator(dto, PropertyNotEquals::new);
    }

    public PropertyIsLessThanOrEqualTo toFilter(
            FilterDto.BinaryComparisonOperatorDto.PropertyIsLessThanOrEqualToDto dto) {
        return toBinaryComparisonOperator(dto, PropertyLessThanOrEquals::new);
    }

    public PropertyIsLessThan toFilter(FilterDto.BinaryComparisonOperatorDto.PropertyIsLessThanDto dto) {
        return toBinaryComparisonOperator(dto, PropertyLessThan::new);
    }

    public PropertyIsGreaterThanOrEqualTo toFilter(
            FilterDto.BinaryComparisonOperatorDto.PropertyIsGreaterThanOrEqualToDto dto) {
        return toBinaryComparisonOperator(dto, PropertyGreaterThanOrEqual::new);
    }

    public PropertyIsGreaterThan toFilter(FilterDto.BinaryComparisonOperatorDto.PropertyIsGreaterThanDto dto) {
        return toBinaryComparisonOperator(dto, PropertyGreaterThan::new);
    }

    public After toFilter(FilterDto.BinaryTemporalOperatorDto.AfterDto dto) {
        return toBinaryOperator(dto, AfterImpl::new);
    }

    public TEquals toFilter(FilterDto.BinaryTemporalOperatorDto.TEqualsDto dto) {
        return toBinaryOperator(dto, TEqualsImpl::new);
    }

    public TContains toFilter(FilterDto.BinaryTemporalOperatorDto.TContainsDto dto) {
        return toBinaryOperator(dto, TContainsImpl::new);
    }

    public OverlappedBy toFilter(FilterDto.BinaryTemporalOperatorDto.OverlappedByDto dto) {
        return toBinaryOperator(dto, OverlappedByImpl::new);
    }

    public MetBy toFilter(FilterDto.BinaryTemporalOperatorDto.MetByDto dto) {
        return toBinaryOperator(dto, MetByImpl::new);
    }

    public Meets toFilter(FilterDto.BinaryTemporalOperatorDto.MeetsDto dto) {
        return toBinaryOperator(dto, MeetsImpl::new);
    }

    public Ends toFilter(FilterDto.BinaryTemporalOperatorDto.EndsDto dto) {
        return toBinaryOperator(dto, EndsImpl::new);
    }

    public EndedBy toFilter(FilterDto.BinaryTemporalOperatorDto.EndedByDto dto) {
        return toBinaryOperator(dto, EndedByImpl::new);
    }

    public During toFilter(FilterDto.BinaryTemporalOperatorDto.DuringDto dto) {
        return toBinaryOperator(dto, DuringImpl::new);
    }

    public BegunBy toFilter(FilterDto.BinaryTemporalOperatorDto.BegunByDto dto) {
        return toBinaryOperator(dto, BegunByImpl::new);
    }

    public Begins toFilter(FilterDto.BinaryTemporalOperatorDto.BeginsDto dto) {
        return toBinaryOperator(dto, BeginsImpl::new);
    }

    public Before toFilter(FilterDto.BinaryTemporalOperatorDto.BeforeDto dto) {
        return toBinaryOperator(dto, BeforeImpl::new);
    }

    public AnyInteracts toFilter(FilterDto.BinaryTemporalOperatorDto.AnyInteractsDto dto) {
        return toBinaryOperator(dto, AnyInteractsImpl::new);
    }

    public TOverlaps toFilter(FilterDto.BinaryTemporalOperatorDto.TOverlapsDto dto) {
        return toBinaryOperator(dto, TOverlapsImpl::new);
    }
}
