/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.filter;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.geotools.jackson.databind.filter.dto.ExpressionDto;
import org.geotools.jackson.databind.filter.dto.FilterDto;
import org.geotools.jackson.databind.filter.dto.FilterDto.BinaryComparisonOperatorDto;
import org.geotools.jackson.databind.filter.dto.FilterDto.BinarySpatialOperatorDto;
import org.geotools.jackson.databind.filter.dto.FilterDto.BinarySpatialOperatorDto.DistanceBufferOperatorDto;
import org.geotools.jackson.databind.filter.dto.FilterDto.BinaryTemporalOperatorDto;
import org.geotools.jackson.databind.filter.dto.FilterDto.IdDto.FeatureId;
import org.geotools.jackson.databind.filter.dto.FilterDto.MultiValuedFilterDto;
import org.geotools.jackson.databind.filter.dto.FilterDto.MultiValuedFilterDto.MatchActionDto;
import org.geotools.jackson.databind.filter.dto.FilterDto.PropertyIsNullDto;
import org.geotools.jackson.databind.filter.dto.LiteralDto;
import org.geotools.jackson.databind.filter.dto.SortByDto;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

/**
 * Abstract test suite for {@link FilterDto} Data Transfer Objects or POJOS; to be used both for testing
 * serialization/deserialization and mapping to and from {@link org.geotools.api.filter.Filter}
 */
public abstract class FilterRoundtripTest {

    protected abstract <F extends FilterDto> F roundtripTest(F dto) throws Exception;

    protected abstract void roundtripTest(org.geotools.jackson.databind.filter.dto.SortByDto dto) throws Exception;

    @Test
    void include() throws Exception {
        FilterDto filter = FilterDto.INCLUDE;
        roundtripTest(filter);
    }

    @Test
    void exclude() throws Exception {
        FilterDto filter = FilterDto.EXCLUDE;
        roundtripTest(filter);
    }

    @Test
    void nativeFilter() throws Exception {
        FilterDto filter = new FilterDto.NativeFilterDto().setNative("select * from test_data;");
        roundtripTest(filter);
    }

    @Test
    void idFilter_FeatureId_Simple() throws Exception {
        Set<FeatureId> identifiers = new HashSet<>();
        identifiers.add(new FilterDto.IdDto.FeatureId().setId("states.1"));
        identifiers.add(new FilterDto.IdDto.FeatureId().setId("states.2"));
        FilterDto filter = new FilterDto.IdDto().setIdentifiers(identifiers);
        roundtripTest(filter);
    }

    @Test
    void idFilter_FeatureId_FeatureVersion() throws Exception {
        Set<FeatureId> identifiers = new HashSet<>();
        identifiers.add(new FilterDto.IdDto.FeatureId().setFeatureVersion("v1").setId("states.1"));
        identifiers.add(
                new FilterDto.IdDto.FeatureId().setFeatureVersion("v1.1").setId("states.2"));
        FilterDto filter = new FilterDto.IdDto().setIdentifiers(identifiers);
        roundtripTest(filter);
    }

    @Test
    void idFilter_ResourceId_Date() throws Exception {
        Set<FeatureId> identifiers = new HashSet<>();
        identifiers.add(new FilterDto.IdDto.ResourceId()
                .setStartTime(new Date())
                .setEndTime(new Date())
                // .setFeatureVersion("v1")
                .setId("states.1"));
        identifiers.add(new FilterDto.IdDto.ResourceId()
                .setStartTime(new Date())
                .setEndTime(new Date())
                // .setFeatureVersion("2")
                .setId("states.2"));
        FilterDto filter = new FilterDto.IdDto().setIdentifiers(identifiers);
        roundtripTest(filter);
    }

    @Test
    void notFilter() throws Exception {
        Set<FeatureId> identifiers = new HashSet<>();
        identifiers.add(new FilterDto.IdDto.FeatureId().setId("states.1"));
        identifiers.add(new FilterDto.IdDto.FeatureId().setId("states.2"));
        FilterDto inFilter = new FilterDto.IdDto().setIdentifiers(identifiers);
        FilterDto notInFilter = new FilterDto.NotDto().setFilter(inFilter);
        roundtripTest(notInFilter);
    }

    @Test
    void propertyIsNul() throws Exception {
        FilterDto filter = isNullFilter();
        roundtripTest(filter);
    }

    @Test
    void sortBy() throws Exception {
        SortByDto dto = new SortByDto(propertyName("some.property.name"), SortByDto.SortOrderDto.ASCENDING);
        roundtripTest(dto);
        dto = new SortByDto(propertyName("collprop[1]"), SortByDto.SortOrderDto.DESCENDING);
        roundtripTest(dto);
    }

    private PropertyIsNullDto isNullFilter() {
        return new FilterDto.PropertyIsNullDto()
                .setExpression(new ExpressionDto.PropertyNameDto().setPropertyName("name"));
    }

    @Test
    void propertyIsNil() throws Exception {
        FilterDto filter = new FilterDto.PropertyIsNilDto()
                .setExpression(new ExpressionDto.PropertyNameDto().setPropertyName("name"))
                .setNilReason("because");
        roundtripTest(filter);
    }

    @Test
    void propertyIsLike() throws Exception {
        MultiValuedFilterDto filter = new FilterDto.PropertyIsLikeDto()
                .setExpression(propertyName("text"))
                .setEscape("-")
                .setLiteral("good thoughts")
                .setMatchingCase(true)
                .setSingleChar("?")
                .setWildCard("*")
                .setMatchAction(MatchActionDto.ONE);
        roundtripTest(filter);
    }

    @Test
    void propertyIsBetween() throws Exception {
        MultiValuedFilterDto dto = new FilterDto.PropertyIsBetweenDto()
                .setExpression(propertyName("count"))
                .setLowerBoundary(literal(1000))
                .setUpperBoundary(literal(2000))
                .setMatchAction(MatchActionDto.ANY);
        roundtripTest(dto);
    }

    @Test
    void andFilter() throws Exception {
        List<FilterDto> children = Arrays.asList(FilterDto.INCLUDE, isNullFilter());
        FilterDto filter = new FilterDto.BinaryLogicOperatorDto.And().setChildren(children);
        roundtripTest(filter);
    }

    @Test
    void orFilter() throws Exception {
        List<FilterDto> children = Arrays.asList(FilterDto.INCLUDE, isNullFilter());
        FilterDto filter = new FilterDto.BinaryLogicOperatorDto.Or().setChildren(children);
        roundtripTest(filter);
    }

    @Test
    void binaryComparisonOperators() throws Exception {
        testBinaryComparisonOperator(FilterDto.BinaryComparisonOperatorDto.PropertyIsEqualToDto::new);
        testBinaryComparisonOperator(FilterDto.BinaryComparisonOperatorDto.PropertyIsNotEqualToDto::new);
        testBinaryComparisonOperator(FilterDto.BinaryComparisonOperatorDto.PropertyIsLessThanDto::new);
        testBinaryComparisonOperator(FilterDto.BinaryComparisonOperatorDto.PropertyIsLessThanOrEqualToDto::new);
        testBinaryComparisonOperator(FilterDto.BinaryComparisonOperatorDto.PropertyIsGreaterThanDto::new);
        testBinaryComparisonOperator(FilterDto.BinaryComparisonOperatorDto.PropertyIsGreaterThanOrEqualToDto::new);
    }

    private void testBinaryComparisonOperator(Supplier<BinaryComparisonOperatorDto> factory) throws Exception {
        BinaryComparisonOperatorDto filter = factory.get();
        filter.setExpression1(propertyName("the_geom"));
        filter.setExpression2(geometry());
        filter.setMatchAction(MatchActionDto.ONE);
        filter.setMatchingCase(true);
        roundtripTest(filter);
    }

    @Test
    void binarySpatialOperators() throws Exception {
        testBinarySpatialOperator(FilterDto.BinarySpatialOperatorDto.BBOXDto::new);
        testBinarySpatialOperator(FilterDto.BinarySpatialOperatorDto.ContainsDto::new);
        testBinarySpatialOperator(FilterDto.BinarySpatialOperatorDto.CrossesDto::new);
        testBinarySpatialOperator(FilterDto.BinarySpatialOperatorDto.DisjointDto::new);
        testBinarySpatialOperator(FilterDto.BinarySpatialOperatorDto.EqualsDto::new);
        testBinarySpatialOperator(FilterDto.BinarySpatialOperatorDto.IntersectsDto::new);
        testBinarySpatialOperator(FilterDto.BinarySpatialOperatorDto.OverlapsDto::new);
        testBinarySpatialOperator(FilterDto.BinarySpatialOperatorDto.TouchesDto::new);
        testBinarySpatialOperator(FilterDto.BinarySpatialOperatorDto.WithinDto::new);
    }

    @Test
    void binaryTemporalOperators() throws Exception {
        testBinaryTemporalOperator(FilterDto.BinaryTemporalOperatorDto.AfterDto::new);
        testBinaryTemporalOperator(FilterDto.BinaryTemporalOperatorDto.AnyInteractsDto::new);
        testBinaryTemporalOperator(FilterDto.BinaryTemporalOperatorDto.BeforeDto::new);
        testBinaryTemporalOperator(FilterDto.BinaryTemporalOperatorDto.BeginsDto::new);
        testBinaryTemporalOperator(FilterDto.BinaryTemporalOperatorDto.BegunByDto::new);
        testBinaryTemporalOperator(FilterDto.BinaryTemporalOperatorDto.DuringDto::new);
        testBinaryTemporalOperator(FilterDto.BinaryTemporalOperatorDto.EndedByDto::new);
        testBinaryTemporalOperator(FilterDto.BinaryTemporalOperatorDto.EndsDto::new);
        testBinaryTemporalOperator(FilterDto.BinaryTemporalOperatorDto.MeetsDto::new);
        testBinaryTemporalOperator(FilterDto.BinaryTemporalOperatorDto.MetByDto::new);
        testBinaryTemporalOperator(FilterDto.BinaryTemporalOperatorDto.OverlappedByDto::new);
        testBinaryTemporalOperator(FilterDto.BinaryTemporalOperatorDto.TContainsDto::new);
        testBinaryTemporalOperator(FilterDto.BinaryTemporalOperatorDto.TEqualsDto::new);
        testBinaryTemporalOperator(FilterDto.BinaryTemporalOperatorDto.TOverlapsDto::new);
    }

    @Test
    void distanceBufferOperators() throws Exception {
        testDistanceBufferOperator(FilterDto.BinarySpatialOperatorDto.BeyondDto::new);
        testDistanceBufferOperator(FilterDto.BinarySpatialOperatorDto.DWithinDto::new);
    }

    private void testBinarySpatialOperator(Supplier<BinarySpatialOperatorDto> factory) throws Exception {
        BinarySpatialOperatorDto filter = factory.get();
        filter.setExpression1(propertyName("the_geom"));
        filter.setExpression2(geometry());
        filter.setMatchAction(MatchActionDto.ONE);
        roundtripTest(filter);
    }

    private void testDistanceBufferOperator(Supplier<DistanceBufferOperatorDto> factory) throws Exception {
        DistanceBufferOperatorDto filter = factory.get();
        filter.setExpression1(propertyName("the_geom"));
        filter.setExpression2(geometry());
        filter.setMatchAction(MatchActionDto.ANY);
        filter.setDistance(1000d);
        filter.setDistanceUnits("m");
        roundtripTest(filter);
    }

    private void testBinaryTemporalOperator(Supplier<BinaryTemporalOperatorDto> factory) throws Exception {
        BinaryTemporalOperatorDto filter = factory.get();
        filter.setExpression1(propertyName("time"));
        filter.setExpression2(temporalLiteral());
        filter.setMatchAction(MatchActionDto.ALL);
        roundtripTest(filter);
    }

    private ExpressionDto temporalLiteral() {
        return literal(LocalDate.of(1977, 01, 17));
    }

    private LiteralDto literal(Object literal) {
        return new LiteralDto().setValue(literal);
    }

    private ExpressionDto geometry() {
        Geometry geometry = geom("POLYGON   ((0 0, 10 10, 20 0, 0 0),(1 1, 9 9, 19 1, 1 1))");
        return literal(geometry);
    }

    private ExpressionDto.PropertyNameDto propertyName(String name) {
        return new ExpressionDto.PropertyNameDto().setPropertyName(name);
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
