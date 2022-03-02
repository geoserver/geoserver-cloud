/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter.dto;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.geotools.jackson.databind.filter.ExpressionRoundtripTest;
import org.geotools.jackson.databind.filter.dto.Expression.FunctionName;
import org.junit.Before;

public class ExpressionSerializationTest extends ExpressionRoundtripTest {

    private ObjectMapper objectMapper;

    public @Before void before() {
        objectMapper = new ObjectMapper();
        objectMapper.setDefaultPropertyInclusion(Include.NON_EMPTY);
        objectMapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        // should register our modules, but also all the other ones like JavaTimeModule
        objectMapper.findAndRegisterModules();
    }

    @SuppressWarnings("unchecked")
    protected @Override <E extends Expression> E roundtripTest(E dto) throws Exception {
        String serialized = objectMapper.writeValueAsString(dto);
        System.err.println(serialized);
        Expression deserialized = objectMapper.readValue(serialized, Expression.class);
        assertEquals(dto, deserialized);
        return (E) deserialized;
    }

    protected @Override FunctionName roundtripTest(FunctionName dto) throws Exception {
        String serialized = objectMapper.writeValueAsString(dto);
        System.err.println(serialized);
        FunctionName deserialized =
                objectMapper.readValue(serialized, Expression.FunctionName.class);
        assertEquals(dto, deserialized);
        return deserialized;
    }
}
