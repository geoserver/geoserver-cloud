/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.filter;

import lombok.NonNull;
import lombok.Value;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.function.IsInstanceOf;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.PropertyIsLike;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Function;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.filter.LengthFunction;
import org.geotools.filter.LikeFilterImpl;
import org.geotools.filter.LiteralExpressionImpl;
import org.geotools.filter.function.FilterFunction_strConcat;
import org.geotools.filter.function.FilterFunction_strEndsWith;
import org.geotools.filter.function.FilterFunction_strEqualsIgnoreCase;
import org.geotools.filter.function.FilterFunction_strIndexOf;
import org.geotools.filter.function.FilterFunction_strLength;
import org.geotools.filter.function.FilterFunction_strStartsWith;
import org.geotools.filter.function.FilterFunction_strSubstring;
import org.geotools.filter.function.FilterFunction_strSubstringStart;
import org.geotools.filter.function.FilterFunction_strToLowerCase;
import org.geotools.filter.function.FilterFunction_strToUpperCase;
import org.geotools.filter.function.FilterFunction_strTrim;
import org.geotools.filter.function.math.FilterFunction_abs;
import org.geotools.filter.function.math.FilterFunction_abs_2;
import org.geotools.filter.function.math.FilterFunction_abs_3;
import org.geotools.filter.function.math.FilterFunction_abs_4;
import org.geotools.jdbc.PreparedFilterToSQL;
import org.geotools.util.Converters;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @since 1.4
 */
class PgsqlFilterToSQL extends PreparedFilterToSQL {

    public static @Value class Result {
        String whereClause;
        List<Object> literalValues;

        @SuppressWarnings("rawtypes")
        List<Class> literalTypes;
    }

    /**
     * @param dialect
     */
    public PgsqlFilterToSQL(Writer out) {
        super(out);
        setSqlNameEscape("\"");
        super.setCapabilities(PgsqlFilterCapabilities.capabilities());
    }

    public static Result evaluate(Filter filter) {
        StringWriter out = new StringWriter();
        var filterToPreparedStatement = new PgsqlFilterToSQL(out);
        filterToPreparedStatement.setSqlNameEscape("\"");
        filter.accept(filterToPreparedStatement, null);
        out.flush();

        String whereClause = out.toString();
        List<Object> literalValues = filterToPreparedStatement.getLiteralValues();
        @SuppressWarnings("rawtypes")
        List<Class> literalTypes = filterToPreparedStatement.getLiteralTypes();
        return new Result(whereClause, literalValues, literalTypes);
    }

    @Override
    public Object visit(PropertyIsEqualTo filter, Object extraData) {
        Expression left = filter.getExpression1();
        Expression right = filter.getExpression2();

        PropertyName prop =
                Optional.of(left)
                        .filter(PropertyName.class::isInstance)
                        .or(() -> Optional.of(right).filter(PropertyName.class::isInstance))
                        .map(PropertyName.class::cast)
                        .orElse(null);
        if (isArray(prop)) {
            Expression value = right;
            if (right instanceof Literal literal) {
                String values =
                        asList(literal.getValue()).stream()
                                .map(o -> Converters.convert(o, String.class))
                                .map("'%s'"::formatted)
                                .collect(Collectors.joining(","));
                value = new LiteralExpressionImpl("ARRAY[%s]".formatted(values));
            }
            try {
                prop.accept(this, extraData);
                out.write(" && ");
                // value.accept(this, extraData);
                out.write((String) ((Literal) value).getValue());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            //            String arrayOperator = prop == left ? "<@" : "@>";
            //            super.visitBinaryComparisonOperator(filter, "&&");
            return extraData;
        }

        return super.visit(filter, extraData);
    }

    @SuppressWarnings("unchecked")
    private List<Object> asList(Object value) {
        if (value instanceof Collection) {
            // beware Stream.toList() does not support null values but Collectors.toList() does
            return ((Collection<Object>) value).stream().collect(Collectors.toList());
        }
        return List.of(value);
    }

    private boolean isArray(PropertyName prop) {
        if (null == prop) return false;
        String propertyName = prop.getPropertyName();
        return "styles.id".equals(propertyName) || "layers.id".equals(propertyName);
    }

    /**
     * Writes the SQL for the Like Filter. Assumes the current java implemented wildcards for the
     * Like Filter: . for multi and .? for single. And replaces them with the SQL % and _,
     * respectively.
     *
     * <p>Uses ILIKE if {@link PropertyIsLike#isMatchingCase()} is {@code false}
     *
     * @param filter the Like Filter to be visited.
     * @task REVISIT: Need to think through the escape char, so it works right when Java uses one,
     *     and escapes correctly with an '_'.
     */
    @Override
    public Object visit(PropertyIsLike filter, Object extraData) {
        char esc = filter.getEscape().charAt(0);
        char multi = filter.getWildCard().charAt(0);
        char single = filter.getSingleChar().charAt(0);
        boolean matchCase = filter.isMatchingCase();

        String literal = filter.getLiteral();
        Expression att = filter.getExpression();

        // JD: hack for date values, we append some additional padding to handle
        // the matching of time/timezone/etc...
        Class<?> attributeType = getExpressionType(att);
        // null check if returnType of expression is Object, null is returned
        // from getExpressionType
        if (attributeType != null && Date.class.isAssignableFrom(attributeType)) {
            literal += multi;
        }

        String pattern =
                LikeFilterImpl.convertToSQL92(esc, multi, single, matchCase, literal, false);

        try {
            att.accept(this, extraData);

            if (!matchCase) {
                out.write(" ILIKE ");
            } else {
                out.write(" LIKE ");
            }

            writeLiteral(pattern);
        } catch (java.io.IOException ioe) {
            throw new UncheckedIOException(IO_ERROR, ioe);
        }
        return extraData;
    }

    @Override
    public Object visit(Function function, Object extraData) throws RuntimeException {
        super.encodingFunction = true;
        boolean encoded;
        try {
            encoded = visitFunction(function);
        } catch (IOException e) {
            throw new UncheckedIOException(IO_ERROR, e);
        }
        super.encodingFunction = false;

        if (encoded) {
            return extraData;
        }
        return super.visit(function, extraData);
    }

    /**
     * Maps a function to its native db equivalent
     *
     * <p>
     *
     * @implNote copied from org.geotools.data.postgis.PostgisFilterToSQL
     */
    @Override
    protected String getFunctionName(Function function) {
        if (function instanceof FilterFunction_strLength || function instanceof LengthFunction) {
            return "char_length";
        } else if (function instanceof FilterFunction_strToLowerCase) {
            return "lower";
        } else if (function instanceof FilterFunction_strToUpperCase) {
            return "upper";
        } else if (function instanceof FilterFunction_abs
                || function instanceof FilterFunction_abs_2
                || function instanceof FilterFunction_abs_3
                || function instanceof FilterFunction_abs_4) {
            return "abs";
        }
        return super.getFunctionName(function);
    }

    /**
     * Performs custom visits for functions that cannot be encoded as <code>
     * functionName(p1, p2, ... pN).</code>
     *
     * <p>
     *
     * @implNote copied and adapted from org.geotools.data.postgis.FilterToSqlHelper
     */
    protected boolean visitFunction(Function function) throws IOException {
        if (function instanceof FilterFunction_strConcat) {
            Expression s1 = getParameter(function, 0, true);
            Expression s2 = getParameter(function, 1, true);
            out.write("(");
            s1.accept(this, String.class);
            out.write(" || ");
            s2.accept(this, String.class);
            out.write(")");
            return true;
        }
        if (function instanceof FilterFunction_strEndsWith) {
            Expression str = getParameter(function, 0, true);
            Expression end = getParameter(function, 1, true);

            out.write("(");
            str.accept(this, String.class);
            out.write(" LIKE ('%' || ");
            end.accept(this, String.class);
            out.write("))");
            return true;
        }
        if (function instanceof FilterFunction_strStartsWith) {
            Expression str = getParameter(function, 0, true);
            Expression start = getParameter(function, 1, true);

            out.write("(");
            str.accept(this, String.class);
            out.write(" LIKE (");
            start.accept(this, String.class);
            out.write(" || '%'))");
            return true;
        }
        if (function instanceof FilterFunction_strEqualsIgnoreCase) {
            Expression first = getParameter(function, 0, true);
            Expression second = getParameter(function, 1, true);

            out.write("(lower(");
            first.accept(this, String.class);
            out.write(") = lower(");
            second.accept(this, String.class);
            out.write("::text))");
            return true;
        }
        if (function instanceof FilterFunction_strIndexOf) {
            Expression first = getParameter(function, 0, true);
            Expression second = getParameter(function, 1, true);

            // would be a simple call, but strIndexOf returns zero based indices
            out.write("(strpos(");
            first.accept(this, String.class);
            out.write(", ");
            second.accept(this, String.class);
            out.write(") - 1)");
            return true;
        }
        if (function instanceof FilterFunction_strSubstring) {
            Expression string = getParameter(function, 0, true);
            Expression start = getParameter(function, 1, true);
            Expression end = getParameter(function, 2, true);

            // postgres does sub(string, start, count)... count instead of end, and 1 based indices
            out.write("substr(");
            string.accept(this, String.class);
            out.write(", ");
            start.accept(this, Integer.class);
            out.write(" + 1, (");
            end.accept(this, Integer.class);
            out.write(" - ");
            start.accept(this, Integer.class);
            out.write("))");
            return true;
        }
        if (function instanceof FilterFunction_strSubstringStart) {
            Expression string = getParameter(function, 0, true);
            Expression start = getParameter(function, 1, true);

            // postgres does sub(string, start, count)... count instead of end, and 1 based indices
            out.write("substr(");
            string.accept(this, String.class);
            out.write(", ");
            start.accept(this, Integer.class);
            out.write(" + 1)");
        }
        if (function instanceof FilterFunction_strTrim) {
            Expression string = getParameter(function, 0, true);

            out.write("trim(both ' ' from ");
            string.accept(this, String.class);
            out.write(")");
            return true;
        }
        if (function instanceof IsInstanceOf instanceOf) {
            Expression typeExpr = getParameter(instanceOf, 0, true);
            @SuppressWarnings("unchecked")
            Class<? extends CatalogInfo> type = typeExpr.evaluate(null, Class.class);

            String types = infoTypes(type);
            String f =
                    """
                    "@type" = ANY('{%s}')
                    """
                            .formatted(types);
            out.write(f);
            return true;
        }

        // function not supported
        return false;
    }

    protected @NonNull String infoTypes(Class<? extends CatalogInfo> clazz) {
        ClassMappings cm;
        if (clazz.isInterface()) cm = ClassMappings.fromInterface(clazz);
        else cm = ClassMappings.fromImpl(clazz);
        if (null == cm)
            throw new IllegalArgumentException(
                    "Unknown type for IsInstanceOf: " + clazz.getCanonicalName());

        return Stream.of(cm.concreteInterfaces())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(","));
    }
}
