/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.api.parameter.Parameter;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.filter.FunctionFinder;
import org.geotools.geometry.jts.Geometries;
import org.geotools.jackson.databind.filter.dto.Expression;
import org.geotools.jackson.databind.filter.dto.Expression.Add;
import org.geotools.jackson.databind.filter.dto.Expression.BinaryExpression;
import org.geotools.jackson.databind.filter.dto.Expression.Divide;
import org.geotools.jackson.databind.filter.dto.Expression.Function;
import org.geotools.jackson.databind.filter.dto.Expression.Multiply;
import org.geotools.jackson.databind.filter.dto.Expression.PropertyName;
import org.geotools.jackson.databind.filter.dto.Expression.Subtract;
import org.geotools.jackson.databind.filter.dto.Literal;
import org.geotools.referencing.CRS;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import si.uom.SI;

/**
 * Abstract test suite for {@link Expression} Data Transfer Objects or POJOS; to be used both for
 * testing serialization/deserialization and mapping to and from {@link
 * org.geotools.api.filter.expression.Expression}
 */
@Slf4j
public abstract class ExpressionRoundtripTest {

    protected void print(String logmsg, Object... args) {
        boolean debug = Boolean.getBoolean("debug");
        if (debug) log.info(logmsg, args);
    }

    protected abstract <E extends Expression> E roundtripTest(E dto) throws Exception;

    protected abstract Expression.FunctionName roundtripTest(Expression.FunctionName dto) throws Exception;

    @Test
    void propertySimple() throws Exception {
        PropertyName dto = propertyName("states");
        roundtripTest(dto);
    }

    @Test
    void propertyNamePrefixedNoNamespaceContext() throws Exception {
        PropertyName dto = propertyName("topp:states");
        roundtripTest(dto);
    }

    @Test
    void propertyNameNamespaceContext() throws Exception {
        PropertyName dto = propertyName("topp:states");
        Map<String, String> context = new HashMap<>();
        // automatically added by NamespaceSupport
        context.put("xml", "http://www.w3.org/XML/1998/namespace");
        context.put("topp", "http://test.com");
        dto.setNamespaceContext(context);

        roundtripTest(dto);
    }

    @Test
    void binaryExpressionAdd() throws Exception {
        BinaryExpression dto = new Add().setExpression1(propertyName("name")).setExpression2(literal(Long.MAX_VALUE));
        roundtripTest(dto);
    }

    @Test
    void binaryExpressionSubtract() throws Exception {
        BinaryExpression dto =
                new Subtract().setExpression1(propertyName("name")).setExpression2(literal(1000));
        roundtripTest(dto);
    }

    @Test
    void binaryExpressionDivide() throws Exception {
        BinaryExpression dto = new Divide().setExpression1(propertyName("name")).setExpression2(literal(1000));
        roundtripTest(dto);
    }

    @Test
    void binaryExpressionMultiply() throws Exception {
        BinaryExpression dto =
                new Multiply().setExpression1(propertyName("name")).setExpression2(literal(1000));
        roundtripTest(dto);
    }

    @Test
    void literalNull() throws Exception {
        roundtripTest(literal(null));
    }

    @Test
    void literalInteger() throws Exception {
        roundtripTest(literal(Integer.MIN_VALUE));
    }

    @Test
    void literalLong() throws Exception {
        roundtripTest(literal(Long.MAX_VALUE));
    }

    @Test
    void literalDouble() throws Exception {
        roundtripTest(literal(0.5d));
    }

    @Test
    void literalFloat() throws Exception {
        roundtripTest(literal(Float.MIN_VALUE));
    }

    @Test
    void literalBigInteger() throws Exception {
        roundtripTest(literal(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(Long.MAX_VALUE))));
        roundtripTest(literal(BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.valueOf(Long.MAX_VALUE))));
    }

    @Test
    void literalBigDecimal() throws Exception {
        roundtripTest(literal(BigDecimal.valueOf(Double.MAX_VALUE).add(BigDecimal.valueOf(Double.MAX_VALUE))));
    }

    @Test
    void literalGeometry() throws Exception {
        roundtripTest(literal(samplePoint()));
    }

    @Test
    void literalJavaUtilDate() throws Exception {
        roundtripTest(literal(new java.util.Date()));
    }

    @Test
    void literalInstant() throws Exception {
        // use a value with rounded-up nanos, some JVM implementations will not get the same exact
        // value after marshalling/unmarshalling
        Instant literal = Instant.ofEpochMilli(Instant.now().toEpochMilli()).plusNanos(1000);
        roundtripTest(literal(literal));
    }

    @Test
    void literalAwtColor() throws Exception {
        roundtripTest(literal(Color.GREEN));
    }

    @Test
    void literalEnum() throws Exception {
        roundtripTest(literal(Geometries.MULTILINESTRING));
    }

    @Disabled("no jackson module can handle serialization/deserialization")
    @Test
    void literalJavaxMeassureUnit() throws Exception {
        roundtripTest(literal(SI.ASTRONOMICAL_UNIT));
    }

    @Test
    void literalList() throws Exception {
        roundtripTest(literal(List.of(1, 2, 3, 4)));
        roundtripTest(literal(List.of(new Date(1), new Date(2), new Date(3))));
    }

    @Test
    void literalListEmpty() throws Exception {
        roundtripTest(literal(List.of()));
    }

    @Test
    void literalListMixedContent() throws Exception {
        List<Object> value = Arrays.asList(1, null, new java.util.Date(1), List.of(4, 5, 6));
        try {
            roundtripTest(literal(value));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void literalSetEmpty() throws Exception {
        roundtripTest(literal(Set.of()));
    }

    @Test
    void literalSet() throws Exception {
        roundtripTest(literal(Set.of(1, 2, 3, 4)));
        roundtripTest(literal(Set.of(new Date(1), new Date(2), new Date(3))));
    }

    @Test
    void literalSetMixedContent() throws Exception {
        Set<Object> value = Set.of(1, new java.util.Date(1), Set.of(4, 5, 6));
        roundtripTest(literal(value));
    }

    @Test
    void literalMapEmpty() throws Exception {
        roundtripTest(literal(Map.of()));
        roundtripTest(literal(Collections.emptyMap()));
        roundtripTest(literal(new HashMap<>()));
    }

    @Test
    void literalMapSingle() throws Exception {
        roundtripTest(literal(Map.of("k1", 1)));
    }

    @Test
    void literalMap() throws Exception {
        roundtripTest(literal(Map.of("k1", 1, "k2", 2, "k3", 3, "k4", 4)));
    }

    @Test
    void literalMapMixedContent() throws Exception {
        roundtripTest(literal(Map.of("k1", 1, "k2", 2L, "k3", 3F, "k4", 4D, "k5", "svalue")));
    }

    @Test
    void literalArrayEmpty() throws Exception {
        roundtripTest(literal(new byte[0]));
        roundtripTest(literal(new char[0]));
        roundtripTest(literal(new boolean[0]));
        roundtripTest(literal(new short[0]));
        roundtripTest(literal(new int[0]));
        roundtripTest(literal(new long[0]));
        roundtripTest(literal(new float[0]));
        roundtripTest(literal(new double[0]));
        roundtripTest(literal(new String[0]));
        roundtripTest(literal(new Date[0]));
    }

    @Test
    void literalArray() throws Exception {
        roundtripTest(literal(new byte[] {1}));
        roundtripTest(literal(new byte[] {0, 1, 2, 3}));

        roundtripTest(literal(new char[] {1}));
        roundtripTest(literal(new char[] {0, 1, 2, 3}));

        roundtripTest(literal(new boolean[] {false, true, false}));
        roundtripTest(literal(new short[] {Short.MIN_VALUE, 0, Short.MAX_VALUE}));
        roundtripTest(literal(new int[] {Integer.MIN_VALUE, 0, Integer.MAX_VALUE}));
        roundtripTest(literal(new long[] {Long.MIN_VALUE, 0L, Long.MAX_VALUE}));
        roundtripTest(literal(new float[] {Float.MIN_VALUE, 0f, Float.MAX_VALUE}));
        roundtripTest(literal(new double[] {Double.MIN_VALUE, 0d, Double.MAX_VALUE}));

        roundtripTest(literal(new String[] {"S1", null, "S2", null, "S3"}));
        roundtripTest(literal(new Date[] {new Date(1), new Date(2), null, new Date(3)}));
    }

    private Literal literal(Object value) {
        return new Literal().setValue(value);
    }

    private PropertyName propertyName(String name) {
        return new PropertyName().setPropertyName(name);
    }

    private Geometry samplePoint() {
        try {
            return new WKTReader().read("POINT(5.5 10.9)");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private Geometry sampleLineString() {
        try {
            return new WKTReader().read("LINESTRING(0 0, 0 1, 1 1, 1 0)");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void allAvailableFunctions() throws Exception {
        // build a list of ignored function names, due to inability to serialize/deserialize their
        // argument types
        Set<String> ignore = new HashSet<String>(Arrays.asList(
                "contrast",
                "darken",
                "desaturate",
                "grayscale",
                "lighten",
                "mix",
                "inArray",
                "relatePattern",
                "relate",
                "rescaleToPixels",
                "saturate",
                "shade",
                "spin",
                "tint",
                "Categorize"));

        FunctionFinder finder = new FunctionFinder(null);
        List<FunctionName> allFunctionDescriptions = finder.getAllFunctionDescriptions();
        for (FunctionName functionName : allFunctionDescriptions) {
            if (ignore.contains(functionName.getName())) {
                print("Ignoring function {}, can't represent its arguments in JSON", functionName.getName());
                continue;
            }
            testFunctionRoundtrip(functionName);
        }
    }

    @Test
    void allAvailableFunctionNames() throws Exception {
        FunctionFinder finder = new FunctionFinder(null);
        List<FunctionName> allFunctionDescriptions = finder.getAllFunctionDescriptions().stream()
                .sorted((f1, f2) -> f1.getName().compareTo(f2.getName()))
                .toList();
        for (FunctionName functionName : allFunctionDescriptions) {
            testFunctionNameRoundtrip(functionName);
        }
    }

    private void testFunctionRoundtrip(FunctionName functionDescriptor) throws Exception {
        print("Testing function {}", functionDescriptor);

        String name = functionDescriptor.getName();
        Name functionName = functionDescriptor.getFunctionName();
        int argumentCount = functionDescriptor.getArgumentCount();
        List<String> argumentNames = functionDescriptor.getArgumentNames();
        List<Parameter<?>> arguments = functionDescriptor.getArguments();
        Parameter<?> returnValueDescriptor = functionDescriptor.getReturn();
        print(
                "functionName: {}, argumentCount: {}, argumentNames: {}, ret: {}",
                functionName,
                argumentCount,
                argumentNames,
                returnValueDescriptor);

        assertNotNull(name);
        assertNotNull(functionName);
        assertNotNull(argumentNames);
        assertNotNull(arguments);
        assertNotNull(returnValueDescriptor);
        assertNull(functionName.getNamespaceURI(), "Unexpected non-null function name nsURI");
        assertEquals(name, functionName.getLocalPart());

        List<Expression> parameters = buildParameters(arguments);
        Function dto = new Function();
        dto.setName(name);
        dto.setParameters(parameters);

        roundtripTest(dto);
    }

    private void testFunctionNameRoundtrip(FunctionName functionName) throws Exception {
        String name = functionName.getName();
        int argumentCount = functionName.getArgumentCount();
        List<String> argumentNames = functionName.getArgumentNames();
        assertNotNull(name);
        assertNotNull(functionName);
        assertNotNull(argumentNames);

        Expression.FunctionName dto = new Expression.FunctionName();
        dto.setName(name);
        dto.setArgumentCount(argumentCount);
        dto.getArgumentNames().addAll(argumentNames);

        print("Testing FunctionName {}", dto);
        roundtripTest(dto);
    }

    private List<Expression> buildParameters(List<Parameter<?>> arguments) {
        List<Expression> parameters = new ArrayList<>();
        arguments.forEach(p -> parameters.addAll(buildSampleParameter(p)));
        return parameters;
    }

    private List<Expression> buildSampleParameter(Parameter<?> p) {
        int occurs = p.isRequired().booleanValue() ? p.getMinOccurs() : 1;
        return IntStream.range(0, occurs).mapToObj(i -> buildSampleParam(p)).toList();
    }

    private Expression buildSampleParam(Parameter<?> p) {
        Object val = p.getDefaultValue();
        if (val == null) {
            val = sampleValue(p.getType());
        }
        return literal(val);
    }

    /**
     * These are all the parameter types found empirically:
     *
     * <ul>
     *   <li>java.lang.Object[]
     *   <li>java.awt.Color
     *   <li>java.lang.Boolean
     *   <li>java.lang.Class
     *   <li>java.lang.Comparable
     *   <li>java.lang.Double
     *   <li>java.lang.Float
     *   <li>java.lang.Integer
     *   <li>java.lang.Long
     *   <li>java.lang.Number
     *   <li>java.lang.Object
     *   <li>java.lang.String
     *   <li>java.util.Collection
     *   <li>java.util.Date
     *   <li>java.util.List
     *   <li>java.util.Map
     *   <li>javax.measure.Unit
     *   <li>org.geotools.filter.function.Classifier
     *   <li>org.geotools.filter.function.color.AbstractHSLFunction.Method
     *   <li>org.geotools.styling.visitor.RescalingMode
     *   <li>org.locationtech.jts.geom.Geometry
     *   <li>org.locationtech.jts.geom.LineString
     * </ul>
     *
     * @param type
     * @return
     */
    private Object sampleValue(Class<?> type) {
        return switch (type.getCanonicalName()) {
            case "java.lang.Object[]" -> new Object[] {1, "hola"};
            case "java.awt.Color" -> Color.BLUE;
            case "java.lang.Boolean" -> Boolean.TRUE;
            case "java.lang.Class" -> MultiPolygon.class;
            case "java.lang.Comparable" -> "string is comparable";
            case "java.lang.Double" -> Double.valueOf(0.33);
            case "java.lang.Float" -> Float.valueOf(0.1f);
            case "java.lang.Integer" -> Integer.MIN_VALUE;
            case "java.lang.Long" -> Long.MAX_VALUE;
            case "java.lang.Number" -> Double.MAX_VALUE;
            case "java.lang.Object" -> "sample java.lang.Object";
            case "java.lang.String" -> "sample string";
            case "java.util.Collection" -> Collections.singleton("single set value");
            case "java.util.Date" -> new Date(60183226800000L);
            case "java.util.List" -> Collections.singletonList("single list value");
            case "java.util.Map" -> Collections.singletonMap("single map key", "single map value");
            case "javax.measure.Unit" -> SI.ASTRONOMICAL_UNIT;
            case "org.geotools.filter.function.Classifier" -> null;
            // sigh, this is a package-private enum, returning null
            case "org.geotools.filter.function.color.AbstractHSLFunction.Method" -> null;
            // another package-private enum
            case "org.geotools.styling.visitor.RescalingMode" -> null;
            case "org.locationtech.jts.geom.Geometry" -> samplePoint();
            case "org.locationtech.jts.geom.LineString" -> sampleLineString();
            case "org.geotools.api.referencing.crs.CoordinateReferenceSystem" -> sampleCrs();
            case "org.locationtech.jts.geom.Point" -> geom("POINT(1 1)");
            default ->
                throw new UnsupportedOperationException(
                        "Unexpected parameter type, add a sample value: '%s'".formatted(type.getCanonicalName()));
        };
    }

    @SneakyThrows(ParseException.class)
    private Geometry geom(String string) {
        return new WKTReader().read(string);
    }

    @SneakyThrows(FactoryException.class)
    private CoordinateReferenceSystem sampleCrs() {
        return CRS.decode("EPSG:4326");
    }
}
