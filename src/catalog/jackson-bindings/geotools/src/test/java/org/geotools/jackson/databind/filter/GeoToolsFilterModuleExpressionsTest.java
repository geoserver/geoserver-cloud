/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.geotools.api.filter.expression.Function;
import org.geotools.api.filter.expression.Literal;
import org.geotools.jackson.databind.filter.dto.Expression;
import org.geotools.jackson.databind.filter.dto.Expression.FunctionName;
import org.geotools.jackson.databind.filter.mapper.ExpressionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.mapstruct.factory.Mappers;

/**
 * Test suite for {@link GeoToolsFilterModule} serialization and deserialization of {@link
 * org.geotools.api.filter.expression.Expression}s
 */
public abstract class GeoToolsFilterModuleExpressionsTest extends ExpressionRoundtripTest {

    private ObjectMapper objectMapper;
    private ExpressionMapper expressionMapper;

    public @BeforeEach void beforeEach() {
        objectMapper = newObjectMapper();
        expressionMapper = Mappers.getMapper(ExpressionMapper.class);
    }

    public @BeforeEach void beforeAll() {
        objectMapper = newObjectMapper();
    }

    protected abstract ObjectMapper newObjectMapper();

    protected @Override <E extends Expression> E roundtripTest(E dto) throws Exception {
        final org.geotools.api.filter.expression.Expression expected = expressionMapper.map(dto);
        String serialized = objectMapper.writeValueAsString(expected);
        print("serialized: {}", serialized);
        org.geotools.api.filter.expression.Expression deserialized;
        deserialized = objectMapper.readValue(serialized, org.geotools.api.filter.expression.Expression.class);

        if (expected instanceof Function f1) {
            assertThat(deserialized).isInstanceOf(Function.class);
            Function f2 = (Function) deserialized;
            assertEquals(f1.getName(), f2.getName());
            assertEquals(f1.getParameters(), f2.getParameters());
        } else if (expected instanceof Literal literal) {
            assertThat(deserialized).isInstanceOf(Literal.class);
            Object v1 = literal.getValue();
            Object v2 = ((Literal) deserialized).getValue();
            boolean valueEquals = org.geotools.jackson.databind.filter.dto.Literal.valueEquals(v1, v2);
            assertTrue(valueEquals);
        } else {
            assertEquals(expected, deserialized);
        }
        return dto;
    }

    protected @Override FunctionName roundtripTest(FunctionName dto) throws Exception {
        org.geotools.api.filter.capability.FunctionName expected = expressionMapper.map(dto);
        String serialized = objectMapper.writeValueAsString(expected);
        print("serialized: {}", serialized);
        org.geotools.api.filter.capability.FunctionName deserialized =
                objectMapper.readValue(serialized, org.geotools.api.filter.capability.FunctionName.class);
        assertEquals(dto.getName(), deserialized.getName());
        assertEquals(dto.getArgumentCount(), deserialized.getArgumentCount());
        assertEquals(dto.getArgumentNames(), deserialized.getArgumentNames());
        return dto;
    }
}
