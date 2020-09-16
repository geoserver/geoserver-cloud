package org.geotools.jackson.databind.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geotools.jackson.databind.filter.dto.Expression;
import org.geotools.jackson.databind.filter.mapper.ExpressionMapper;
import org.junit.Before;
import org.mapstruct.factory.Mappers;
import org.opengis.filter.expression.Function;

/**
 * Test suite for {@link GeoToolsFilterModule} serialization and deserialization of {@link
 * org.opengis.filter.expression.Expression}s
 */
public class GeoToolsFilterModuleExpressionsTest extends ExpressionRoundtripTest {

    private ObjectMapper objectMapper;
    private ExpressionMapper expressionMapper;

    public @Before void before() {
        objectMapper = new ObjectMapper();
        objectMapper.setDefaultPropertyInclusion(Include.NON_EMPTY);
        objectMapper.findAndRegisterModules();
        expressionMapper = Mappers.getMapper(ExpressionMapper.class);
    }

    protected @Override <E extends Expression> E roundtripTest(E dto) throws Exception {
        final org.opengis.filter.expression.Expression expected = expressionMapper.map(dto);
        String serialized = objectMapper.writeValueAsString(expected);
        System.err.println(serialized);
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
}
