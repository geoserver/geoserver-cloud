/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.geotools.jackson.databind.filter.dto.Expression;
import org.geotools.jackson.databind.filter.dto.Expression.FunctionName;
import org.geotools.jackson.databind.filter.mapper.ExpressionMapper;
import org.geotools.jackson.databind.util.ObjectMapperUtil;
import org.junit.BeforeClass;
import org.mapstruct.factory.Mappers;
import org.opengis.filter.expression.Function;

/**
 * Test suite for {@link GeoToolsFilterModule} serialization and deserialization of {@link
 * org.opengis.filter.expression.Expression}s
 */
@Slf4j
public class GeoToolsFilterModuleExpressionsTest extends ExpressionRoundtripTest {
    private boolean debug = Boolean.valueOf(System.getProperty("debug", "false"));

    protected void print(String logmsg, Object... args) {
        if (debug) log.debug(logmsg, args);
    }

    private static ObjectMapper objectMapper;
    private static ExpressionMapper expressionMapper;

    public static @BeforeClass void beforeAll() {
        objectMapper = ObjectMapperUtil.newObjectMapper();
        expressionMapper = Mappers.getMapper(ExpressionMapper.class);
    }

    protected @Override <E extends Expression> E roundtripTest(E dto) throws Exception {
        final org.opengis.filter.expression.Expression expected = expressionMapper.map(dto);
        String serialized = objectMapper.writeValueAsString(expected);
        print("serialized: {}", serialized);
        org.opengis.filter.expression.Expression deserialized;
        deserialized =
                objectMapper.readValue(serialized, org.opengis.filter.expression.Expression.class);

        if (expected instanceof Function) {
            assertTrue(deserialized instanceof Function);
            Function f1 = (Function) expected;
            Function f2 = (Function) deserialized;
            assertEquals(f1.getName(), f2.getName());
            assertEquals(f1.getParameters(), f2.getParameters());
        } else {
            assertEquals(expected, deserialized);
        }
        return dto;
    }

    protected @Override FunctionName roundtripTest(FunctionName dto) throws Exception {
        org.opengis.filter.capability.FunctionName expected = expressionMapper.map(dto);
        String serialized = objectMapper.writeValueAsString(expected);
        print("serialized: {}", serialized);
        org.opengis.filter.capability.FunctionName deserialized =
                objectMapper.readValue(
                        serialized, org.opengis.filter.capability.FunctionName.class);
        assertEquals(dto.getName(), deserialized.getName());
        assertEquals(dto.getArgumentCount(), deserialized.getArgumentCount());
        assertEquals(dto.getArgumentNames(), deserialized.getArgumentNames());
        return dto;
    }
}
