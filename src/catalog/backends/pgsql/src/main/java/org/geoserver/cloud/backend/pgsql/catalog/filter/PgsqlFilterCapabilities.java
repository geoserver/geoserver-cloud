/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql.catalog.filter;

import lombok.experimental.UtilityClass;

import org.geotools.api.filter.ExcludeFilter;
import org.geotools.api.filter.Id;
import org.geotools.api.filter.IncludeFilter;
import org.geotools.api.filter.PropertyIsBetween;
import org.geotools.api.filter.PropertyIsLike;
import org.geotools.api.filter.PropertyIsNull;
import org.geotools.api.filter.expression.Add;
import org.geotools.api.filter.expression.Divide;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.Multiply;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.expression.Subtract;
import org.geotools.filter.FilterCapabilities;
import org.geotools.filter.LengthFunction;
import org.geotools.filter.function.DateDifferenceFunction;
import org.geotools.filter.function.FilterFunction_equalTo;
import org.geotools.filter.function.FilterFunction_strConcat;
import org.geotools.filter.function.FilterFunction_strEndsWith;
import org.geotools.filter.function.FilterFunction_strEqualsIgnoreCase;
import org.geotools.filter.function.FilterFunction_strIndexOf;
import org.geotools.filter.function.FilterFunction_strLength;
import org.geotools.filter.function.FilterFunction_strReplace;
import org.geotools.filter.function.FilterFunction_strStartsWith;
import org.geotools.filter.function.FilterFunction_strSubstring;
import org.geotools.filter.function.FilterFunction_strSubstringStart;
import org.geotools.filter.function.FilterFunction_strToLowerCase;
import org.geotools.filter.function.FilterFunction_strToUpperCase;
import org.geotools.filter.function.FilterFunction_strTrim;
import org.geotools.filter.function.FilterFunction_strTrim2;
import org.geotools.filter.function.InArrayFunction;
import org.geotools.filter.function.InFunction;
import org.geotools.filter.function.math.FilterFunction_abs;
import org.geotools.filter.function.math.FilterFunction_abs_2;
import org.geotools.filter.function.math.FilterFunction_abs_3;
import org.geotools.filter.function.math.FilterFunction_abs_4;
import org.geotools.filter.function.math.FilterFunction_ceil;
import org.geotools.filter.function.math.FilterFunction_floor;

/**
 * @since 1.4
 */
@UtilityClass
class PgsqlFilterCapabilities {

    private static final FilterCapabilities INSTANCE = createFilterCapabilities();

    public static FilterCapabilities capabilities() {
        return INSTANCE;
    }

    static FilterCapabilities createFilterCapabilities() {
        FilterCapabilities caps = new FilterCapabilities();

        // basic expressions
        caps.addType(Add.class);
        caps.addType(Subtract.class);
        caps.addType(Divide.class);
        caps.addType(Multiply.class);
        caps.addType(PropertyName.class);
        caps.addType(Literal.class);

        // basic filters
        caps.addAll(FilterCapabilities.LOGICAL_OPENGIS);
        caps.addAll(FilterCapabilities.SIMPLE_COMPARISONS_OPENGIS);
        caps.addType(PropertyIsNull.class);
        caps.addType(PropertyIsBetween.class);
        caps.addType(PropertyIsLike.class);
        caps.addType(Id.class);
        caps.addType(IncludeFilter.class);
        caps.addType(ExcludeFilter.class);

        // supported functions
        caps.addAll(InFunction.getInCapabilities());

        // add support for string functions
        caps.addType(FilterFunction_strConcat.class);
        caps.addType(FilterFunction_strEndsWith.class);
        caps.addType(FilterFunction_strStartsWith.class);
        caps.addType(FilterFunction_strEqualsIgnoreCase.class);
        caps.addType(FilterFunction_strIndexOf.class);
        caps.addType(FilterFunction_strLength.class);
        caps.addType(LengthFunction.class);
        caps.addType(FilterFunction_strToLowerCase.class);
        caps.addType(FilterFunction_strToUpperCase.class);
        caps.addType(FilterFunction_strReplace.class);
        caps.addType(FilterFunction_strSubstring.class);
        caps.addType(FilterFunction_strSubstringStart.class);
        caps.addType(FilterFunction_strTrim.class);
        caps.addType(FilterFunction_strTrim2.class);

        // add support for math functions
        caps.addType(FilterFunction_abs.class);
        caps.addType(FilterFunction_abs_2.class);
        caps.addType(FilterFunction_abs_3.class);
        caps.addType(FilterFunction_abs_4.class);
        caps.addType(FilterFunction_ceil.class);
        caps.addType(FilterFunction_floor.class);

        // time related functions
        caps.addType(DateDifferenceFunction.class);

        // array functions
        caps.addType(InArrayFunction.class);

        // compare functions
        caps.addType(FilterFunction_equalTo.class);

        return caps;
    }
}
