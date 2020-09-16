package org.geotools.jackson.databind.filter.dto;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geotools.jackson.databind.filter.ExpressionRoundtripTest;
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
}
