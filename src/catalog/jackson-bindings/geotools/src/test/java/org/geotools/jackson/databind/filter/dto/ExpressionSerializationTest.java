/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.geotools.jackson.databind.filter.ExpressionRoundtripTest;
import org.geotools.jackson.databind.filter.dto.Expression.FunctionName;
import org.geotools.jackson.databind.util.ObjectMapperUtil;
import org.junit.jupiter.api.BeforeAll;

@Slf4j
public class ExpressionSerializationTest extends ExpressionRoundtripTest {
    private boolean debug = Boolean.valueOf(System.getProperty("debug", "false"));

    protected void print(String logmsg, Object... args) {
        if (debug) log.debug(logmsg, args);
    }

    private static ObjectMapper objectMapper;

    public static @BeforeAll void beforeAll() {
        objectMapper = ObjectMapperUtil.newObjectMapper();
    }

    @SuppressWarnings("unchecked")
    protected @Override <E extends Expression> E roundtripTest(E dto) throws Exception {
        String serialized = objectMapper.writeValueAsString(dto);
        print("serialized: {}", serialized);
        Expression deserialized = objectMapper.readValue(serialized, Expression.class);
        assertEquals(dto, deserialized);
        return (E) deserialized;
    }

    protected @Override FunctionName roundtripTest(FunctionName dto) throws Exception {
        String serialized = objectMapper.writeValueAsString(dto);
        print("serialized: {}", serialized);
        FunctionName deserialized =
                objectMapper.readValue(serialized, Expression.FunctionName.class);
        assertEquals(dto, deserialized);
        return deserialized;
    }
}
