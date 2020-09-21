/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.geotools.filter.FunctionFinder;
import org.geotools.geometry.jts.Geometries;
import org.geotools.jackson.databind.filter.dto.Expression;
import org.geotools.jackson.databind.filter.dto.Expression.Add;
import org.geotools.jackson.databind.filter.dto.Expression.BinaryExpression;
import org.geotools.jackson.databind.filter.dto.Expression.Divide;
import org.geotools.jackson.databind.filter.dto.Expression.Function;
import org.geotools.jackson.databind.filter.dto.Expression.Literal;
import org.geotools.jackson.databind.filter.dto.Expression.Multiply;
import org.geotools.jackson.databind.filter.dto.Expression.PropertyName;
import org.geotools.jackson.databind.filter.dto.Expression.Subtract;
import org.junit.Ignore;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.type.Name;
import org.opengis.filter.capability.FunctionName;
import org.opengis.parameter.Parameter;
import si.uom.SI;

/**
 * Abstract test suite for {@link Expression} Data Transfer Objects or POJOS; to be used both for
 * testing serialization/deserialization and mapping to and from {@link
 * org.opengis.filter.expression.Expression}
 */
@Slf4j
public abstract class ExpressionRoundtripTest {

    protected abstract <E extends Expression> E roundtripTest(E dto) throws Exception;

    public @Test void propertySimple() throws Exception {
        PropertyName dto = propertyName("states");
        roundtripTest(dto);
    }

    public @Test void propertyNamePrefixedNoNamespaceContext() throws Exception {
        PropertyName dto = propertyName("topp:states");
        roundtripTest(dto);
    }

    public @Test void propertyNameNamespaceContext() throws Exception {
        PropertyName dto = propertyName("topp:states");
        Map<String, String> context = new HashMap<>();
        // automatically added by NamespaceSupport
        context.put("xml", "http://www.w3.org/XML/1998/namespace");
        context.put("topp", "http://test.com");
        dto.setNamespaceContext(context);

        roundtripTest(dto);
    }

    public @Test void binaryExpressionAdd() throws Exception {
        BinaryExpression dto =
                new Add()
                        .setExpression1(propertyName("name"))
                        .setExpression2(literal(Long.MAX_VALUE));
        roundtripTest(dto);
    }

    public @Test void binaryExpressionSubtract() throws Exception {
        BinaryExpression dto =
                new Subtract().setExpression1(propertyName("name")).setExpression2(literal(1000));
        roundtripTest(dto);
    }

    public @Test void binaryExpressionDivide() throws Exception {
        BinaryExpression dto =
                new Divide().setExpression1(propertyName("name")).setExpression2(literal(1000));
        roundtripTest(dto);
    }

    public @Test void binaryExpressionMultiply() throws Exception {
        BinaryExpression dto =
                new Multiply().setExpression1(propertyName("name")).setExpression2(literal(1000));
        roundtripTest(dto);
    }

    public @Test void literalNull() throws Exception {
        roundtripTest(literal(null));
    }

    public @Test void literalInteger() throws Exception {
        roundtripTest(literal(Integer.MIN_VALUE));
    }

    public @Test void literalLong() throws Exception {
        roundtripTest(literal(Long.MAX_VALUE));
    }

    public @Test void literalDouble() throws Exception {
        roundtripTest(literal(0.5d));
    }

    public @Test void literalFloat() throws Exception {
        roundtripTest(literal(Float.MIN_VALUE));
    }

    public @Test void literalBigInteger() throws Exception {
        roundtripTest(
                literal(
                        BigInteger.valueOf(Long.MAX_VALUE)
                                .add(BigInteger.valueOf(Long.MAX_VALUE))));
        roundtripTest(
                literal(
                        BigInteger.valueOf(Long.MIN_VALUE)
                                .subtract(BigInteger.valueOf(Long.MAX_VALUE))));
    }

    @Ignore
    public @Test void literalBigDecimal() throws Exception {
        roundtripTest(
                literal(
                        BigDecimal.valueOf(Double.MAX_VALUE)
                                .add(BigDecimal.valueOf(Double.MAX_VALUE))));
    }

    public @Test void literalGeometry() throws Exception {
        roundtripTest(literal(samplePoint()));
    }

    public @Test void literalJavaUtilDate() throws Exception {
        roundtripTest(literal(new java.util.Date()));
    }

    public @Test void literalInstant() throws Exception {
        roundtripTest(literal(Instant.now()));
    }

    @Ignore("no jackson module can handle serialization/deserialization")
    public @Test void literalAwtColor() throws Exception {
        roundtripTest(literal(Color.GREEN));
    }

    public @Test void literalEnum() throws Exception {
        roundtripTest(literal(Geometries.MULTILINESTRING));
    }

    @Ignore("no jackson module can handle serialization/deserialization")
    public @Test void literalJavaxMeassureUnit() throws Exception {
        roundtripTest(literal(SI.ASTRONOMICAL_UNIT));
    }

    public @Test void literalList() throws Exception {
        List<Object> l1 = Arrays.asList(1, 2, 3, 4);
        roundtripTest(literal(l1));

        List<Object> l2 = Arrays.asList(new Date(1), new Date(2), new Date(3));
        roundtripTest(literal(l2));
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

    public @Test void allAvailableFunctions() throws Exception {
        // build a list of ignored function names, due to inability to serialize/deserialize their
        // argument types
        Set<String> ignore =
                new HashSet<String>(
                        Arrays.asList(
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
                log.info(
                        "Ignoring function {}, can't represent its arguments in JSON",
                        functionName.getName());
                continue;
            }
            testFunctionRoundtrip(functionName);
        }
    }

    private void testFunctionRoundtrip(FunctionName functionDescriptor) throws Exception {
        log.debug("Testing function {}", functionDescriptor);

        String name = functionDescriptor.getName();
        Name functionName = functionDescriptor.getFunctionName();
        int argumentCount = functionDescriptor.getArgumentCount();
        List<String> argumentNames = functionDescriptor.getArgumentNames();
        List<Parameter<?>> arguments = functionDescriptor.getArguments();
        Parameter<?> returnValueDescriptor = functionDescriptor.getReturn();
        log.debug(
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
        assertNull("Unexpected non-null function name nsURI", functionName.getNamespaceURI());
        assertEquals(name, functionName.getLocalPart());

        List<Expression> parameters = buildParameters(argumentCount, arguments);
        Function dto = new Function();
        dto.setName(name);
        dto.setParameters(parameters);

        roundtripTest(dto);
    }

    private List<Expression> buildParameters(int argumentCount, List<Parameter<?>> arguments) {
        List<Expression> parameters = new ArrayList<>();
        arguments.forEach(p -> parameters.addAll(buildSampleParameter(p)));
        return parameters;
    }

    private List<Expression> buildSampleParameter(Parameter<?> p) {
        int occurs = p.isRequired().booleanValue() ? p.getMinOccurs() : 1;
        return IntStream.range(0, occurs)
                .mapToObj(i -> buildSampleParam(i, p))
                .collect(Collectors.toList());
    }

    // static Set<String> allFunctionParamTypes = new TreeSet<>();

    private Expression buildSampleParam(int i, Parameter<?> p) {
        // allFunctionParamTypes.add(p.getType().getCanonicalName());
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
        switch (type.getCanonicalName()) {
            case "java.lang.Object[]":
                return new Object[] {1, "hola"};
            case "java.awt.Color":
                return Color.BLUE;
            case "java.lang.Boolean":
                return Boolean.TRUE;
            case "java.lang.Class":
                return MultiPolygon.class;
            case "java.lang.Comparable":
                return "string is comparable";
            case "java.lang.Double":
                return Double.valueOf(0.33);
            case "java.lang.Float":
                return Float.valueOf(0.1f);
            case "java.lang.Integer":
                return Integer.MIN_VALUE;
            case "java.lang.Long":
                return Long.MAX_VALUE;
            case "java.lang.Number":
                return Double.MAX_VALUE;
            case "java.lang.Object":
                return "sample java.lang.Object";
            case "java.lang.String":
                return "sample string";
            case "java.util.Collection":
                return Collections.singleton("single set value");
            case "java.util.Date":
                return new Date(60183226800000L);
            case "java.util.List":
                return Collections.singletonList("single list value");
            case "java.util.Map":
                return Collections.singletonMap("single map key", "single map value");
            case "javax.measure.Unit":
                return SI.ASTRONOMICAL_UNIT;
            case "org.geotools.filter.function.Classifier":
                return null;
                // sigh, this is a package-private enum, returning null
            case "org.geotools.filter.function.color.AbstractHSLFunction.Method":
                return null;
                // another package-private enum
            case "org.geotools.styling.visitor.RescalingMode":
                return null;
            case "org.locationtech.jts.geom.Geometry":
                return samplePoint();
            case "org.locationtech.jts.geom.LineString":
                return sampleLineString();
            default:
                throw new UnsupportedOperationException(
                        "Unexpected parameter type, add a sample value: '"
                                + type.getCanonicalName()
                                + "'");
        }
    }
}
