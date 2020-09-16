/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.geotools.jackson.databind.filter.ExpressionRoundtripTest;
import org.geotools.jackson.databind.filter.dto.Expression;
import org.junit.Before;
import org.mapstruct.factory.Mappers;

public class ExpressionMapperTest extends ExpressionRoundtripTest {

    private ExpressionMapper expressions;

    public @Before void before() {
        expressions = Mappers.getMapper(ExpressionMapper.class);
    }

    @SuppressWarnings("unchecked")
    protected @Override <E extends Expression> E roundtripTest(E dto) throws Exception {
        org.opengis.filter.expression.Expression expression = expressions.map(dto);
        assertNotNull(expression);

        Expression roundTripped = expressions.map(expression);
        assertEquals(dto, roundTripped);
        return (E) roundTripped;
    }
}
