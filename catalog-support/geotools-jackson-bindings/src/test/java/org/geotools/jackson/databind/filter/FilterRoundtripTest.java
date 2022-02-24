/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter;

import org.geotools.jackson.databind.filter.dto.Expression;
import org.geotools.jackson.databind.filter.dto.Expression.Literal;
import org.geotools.jackson.databind.filter.dto.Filter;
import org.geotools.jackson.databind.filter.dto.Filter.BinaryComparisonOperator;
import org.geotools.jackson.databind.filter.dto.Filter.BinarySpatialOperator;
import org.geotools.jackson.databind.filter.dto.Filter.BinarySpatialOperator.DistanceBufferOperator;
import org.geotools.jackson.databind.filter.dto.Filter.BinaryTemporalOperator;
import org.geotools.jackson.databind.filter.dto.Filter.Id.FeatureId;
import org.geotools.jackson.databind.filter.dto.Filter.MultiValuedFilter;
import org.geotools.jackson.databind.filter.dto.Filter.MultiValuedFilter.MatchAction;
import org.geotools.jackson.databind.filter.dto.Filter.PropertyIsNull;
import org.geotools.jackson.databind.filter.dto.SortBy;
import org.junit.Test;
import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Abstract test suite for {@link Filter} Data Transfer Objects or POJOS; to be used both for
 * testing serialization/deserialization and mapping to and from {@link org.opengis.filter.Filter}
 */
public abstract class FilterRoundtripTest {

    protected abstract <F extends Filter> F roundtripTest(F dto) throws Exception;

    protected abstract void roundtripTest(org.geotools.jackson.databind.filter.dto.SortBy dto)
            throws Exception;

    public @Test void include() throws Exception {
        Filter filter = Filter.INCLUDE;
        roundtripTest(filter);
    }

    public @Test void exclude() throws Exception {
        Filter filter = Filter.EXCLUDE;
        roundtripTest(filter);
    }

    public @Test void nativeFilter() throws Exception {
        Filter filter = new Filter.NativeFilter().setNative("select * from test_data;");
        roundtripTest(filter);
    }

    public @Test void idFilter_FeatureId_Simple() throws Exception {
        Set<FeatureId> identifiers = new HashSet<>();
        identifiers.add(new Filter.Id.FeatureId().setId("states.1"));
        identifiers.add(new Filter.Id.FeatureId().setId("states.2"));
        Filter filter = new Filter.Id().setIdentifiers(identifiers);
        roundtripTest(filter);
    }

    public @Test void idFilter_FeatureId_FeatureVersion() throws Exception {
        Set<FeatureId> identifiers = new HashSet<>();
        identifiers.add(new Filter.Id.FeatureId().setFeatureVersion("v1").setId("states.1"));
        identifiers.add(new Filter.Id.FeatureId().setFeatureVersion("v1.1").setId("states.2"));
        Filter filter = new Filter.Id().setIdentifiers(identifiers);
        roundtripTest(filter);
    }

    public @Test void idFilter_ResourceId_Date() throws Exception {
        Set<FeatureId> identifiers = new HashSet<>();
        identifiers.add(
                new Filter.Id.ResourceId()
                        .setStartTime(new Date())
                        .setEndTime(new Date())
                        // .setFeatureVersion("v1")
                        .setId("states.1"));
        identifiers.add(
                new Filter.Id.ResourceId()
                        .setStartTime(new Date())
                        .setEndTime(new Date())
                        // .setFeatureVersion("2")
                        .setId("states.2"));
        Filter filter = new Filter.Id().setIdentifiers(identifiers);
        roundtripTest(filter);
    }

    public @Test void notFilter() throws Exception {
        Set<FeatureId> identifiers = new HashSet<>();
        identifiers.add(new Filter.Id.FeatureId().setId("states.1"));
        identifiers.add(new Filter.Id.FeatureId().setId("states.2"));
        Filter inFilter = new Filter.Id().setIdentifiers(identifiers);
        Filter notInFilter = new Filter.Not().setFilter(inFilter);
        roundtripTest(notInFilter);
    }

    public @Test void propertyIsNul() throws Exception {
        Filter filter = isNullFilter();
        roundtripTest(filter);
    }

    public @Test void sortBy() throws Exception {
        SortBy dto = new SortBy(propertyName("some.property.name"), SortBy.SortOrder.ASCENDING);
        roundtripTest(dto);
        dto = new SortBy(propertyName("collprop[1]"), SortBy.SortOrder.DESCENDING);
        roundtripTest(dto);
    }

    private PropertyIsNull isNullFilter() {
        return new Filter.PropertyIsNull()
                .setExpression(new Expression.PropertyName().setPropertyName("name"));
    }

    public @Test void propertyIsNil() throws Exception {
        Filter filter =
                new Filter.PropertyIsNil()
                        .setExpression(new Expression.PropertyName().setPropertyName("name"))
                        .setNilReason("because");
        roundtripTest(filter);
    }

    public @Test void propertyIsLike() throws Exception {
        MultiValuedFilter filter =
                new Filter.PropertyIsLike()
                        .setExpression(propertyName("text"))
                        .setEscape("--")
                        .setLiteral("good thoughts")
                        .setMatchingCase(true)
                        .setSingleChar("?")
                        .setWildCard("*")
                        .setMatchAction(MatchAction.ONE);
        roundtripTest(filter);
    }

    public @Test void propertyIsBetween() throws Exception {
        MultiValuedFilter dto =
                new Filter.PropertyIsBetween()
                        .setExpression(propertyName("count"))
                        .setLowerBoundary(literal(1000))
                        .setUpperBoundary(literal(2000))
                        .setMatchAction(MatchAction.ANY);
        roundtripTest(dto);
    }

    public @Test void andFilter() throws Exception {
        List<Filter> children = Arrays.asList(Filter.INCLUDE, isNullFilter());
        Filter filter = new Filter.BinaryLogicOperator.And().setChildren(children);
        roundtripTest(filter);
    }

    public @Test void orFilter() throws Exception {
        List<Filter> children = Arrays.asList(Filter.INCLUDE, isNullFilter());
        Filter filter = new Filter.BinaryLogicOperator.Or().setChildren(children);
        roundtripTest(filter);
    }

    public @Test void binaryComparisonOperators() throws Exception {
        testBinaryComparisonOperator(Filter.BinaryComparisonOperator.PropertyIsEqualTo::new);
        testBinaryComparisonOperator(Filter.BinaryComparisonOperator.PropertyIsNotEqualTo::new);
        testBinaryComparisonOperator(Filter.BinaryComparisonOperator.PropertyIsLessThan::new);
        testBinaryComparisonOperator(
                Filter.BinaryComparisonOperator.PropertyIsLessThanOrEqualTo::new);
        testBinaryComparisonOperator(Filter.BinaryComparisonOperator.PropertyIsGreaterThan::new);
        testBinaryComparisonOperator(
                Filter.BinaryComparisonOperator.PropertyIsGreaterThanOrEqualTo::new);
    }

    private void testBinaryComparisonOperator(Supplier<BinaryComparisonOperator> factory)
            throws Exception {
        BinaryComparisonOperator filter = factory.get();
        filter.setExpression1(propertyName("the_geom"));
        filter.setExpression2(geometry());
        filter.setMatchAction(MatchAction.ONE);
        filter.setMatchingCase(true);
        roundtripTest(filter);
    }

    public @Test void binarySpatialOperators() throws Exception {
        testBinarySpatialOperator(Filter.BinarySpatialOperator.BBOX::new);
        testBinarySpatialOperator(Filter.BinarySpatialOperator.Contains::new);
        testBinarySpatialOperator(Filter.BinarySpatialOperator.Crosses::new);
        testBinarySpatialOperator(Filter.BinarySpatialOperator.Disjoint::new);
        testBinarySpatialOperator(Filter.BinarySpatialOperator.Equals::new);
        testBinarySpatialOperator(Filter.BinarySpatialOperator.Intersects::new);
        testBinarySpatialOperator(Filter.BinarySpatialOperator.Overlaps::new);
        testBinarySpatialOperator(Filter.BinarySpatialOperator.Touches::new);
        testBinarySpatialOperator(Filter.BinarySpatialOperator.Within::new);
    }

    public @Test void binaryTemporalOperators() throws Exception {
        testBinaryTemporalOperator(Filter.BinaryTemporalOperator.After::new);
        testBinaryTemporalOperator(Filter.BinaryTemporalOperator.AnyInteracts::new);
        testBinaryTemporalOperator(Filter.BinaryTemporalOperator.Before::new);
        testBinaryTemporalOperator(Filter.BinaryTemporalOperator.Begins::new);
        testBinaryTemporalOperator(Filter.BinaryTemporalOperator.BegunBy::new);
        testBinaryTemporalOperator(Filter.BinaryTemporalOperator.During::new);
        testBinaryTemporalOperator(Filter.BinaryTemporalOperator.EndedBy::new);
        testBinaryTemporalOperator(Filter.BinaryTemporalOperator.Ends::new);
        testBinaryTemporalOperator(Filter.BinaryTemporalOperator.Meets::new);
        testBinaryTemporalOperator(Filter.BinaryTemporalOperator.MetBy::new);
        testBinaryTemporalOperator(Filter.BinaryTemporalOperator.OverlappedBy::new);
        testBinaryTemporalOperator(Filter.BinaryTemporalOperator.TContains::new);
        testBinaryTemporalOperator(Filter.BinaryTemporalOperator.TEquals::new);
        testBinaryTemporalOperator(Filter.BinaryTemporalOperator.TOverlaps::new);
    }

    public @Test void distanceBufferOperators() throws Exception {
        testDistanceBufferOperator(Filter.BinarySpatialOperator.Beyond::new);
        testDistanceBufferOperator(Filter.BinarySpatialOperator.DWithin::new);
    }

    private void testBinarySpatialOperator(Supplier<BinarySpatialOperator> factory)
            throws Exception {
        BinarySpatialOperator filter = factory.get();
        filter.setExpression1(propertyName("the_geom"));
        filter.setExpression2(geometry());
        filter.setMatchAction(MatchAction.ONE);
        roundtripTest(filter);
    }

    private void testDistanceBufferOperator(Supplier<DistanceBufferOperator> factory)
            throws Exception {
        DistanceBufferOperator filter = factory.get();
        filter.setExpression1(propertyName("the_geom"));
        filter.setExpression2(geometry());
        filter.setMatchAction(MatchAction.ANY);
        filter.setDistance(1000d);
        filter.setDistanceUnits("m");
        roundtripTest(filter);
    }

    private void testBinaryTemporalOperator(Supplier<BinaryTemporalOperator> factory)
            throws Exception {
        BinaryTemporalOperator filter = factory.get();
        filter.setExpression1(propertyName("time"));
        filter.setExpression2(temporalLiteral());
        filter.setMatchAction(MatchAction.ALL);
        roundtripTest(filter);
    }

    private Expression temporalLiteral() {
        return literal(LocalDate.of(1977, 01, 17));
    }

    private Literal literal(Object literal) {
        return new Expression.Literal().setValue(literal);
    }

    private Expression geometry() {
        Geometry geometry = geom("POLYGON   ((0 0, 10 10, 20 0, 0 0),(1 1, 9 9, 19 1, 1 1))");
        return literal(geometry);
    }

    private Expression.PropertyName propertyName(String name) {
        return new Expression.PropertyName().setPropertyName(name);
    }

    private Geometry geom(String wkt) {
        try {
            CoordinateSequenceFactory csf = new PackedCoordinateSequenceFactory();
            GeometryFactory gf = new GeometryFactory(csf);
            WKTReader reader = new WKTReader(gf);
            // do not create 3-dim coords if only 2 are requested
            reader.setIsOldJtsCoordinateSyntaxAllowed(false);
            reader.setIsOldJtsMultiPointSyntaxAllowed(true);
            return reader.read(wkt);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
