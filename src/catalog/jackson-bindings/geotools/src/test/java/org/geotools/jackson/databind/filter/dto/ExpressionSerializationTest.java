/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.filter.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.geotools.jackson.databind.filter.ExpressionRoundtripTest;
import org.geotools.jackson.databind.filter.dto.ExpressionDto.FunctionNameDto;
import org.geotools.jackson.databind.util.ObjectMapperUtil;
import org.junit.jupiter.api.BeforeAll;
import tools.jackson.databind.ObjectMapper;

class ExpressionSerializationTest extends ExpressionRoundtripTest {

    protected static ObjectMapper objectMapper;

    @BeforeAll
    static void setUpMapper() {
        objectMapper = ObjectMapperUtil.newObjectMapper();
    }

    @SuppressWarnings("unchecked")
    protected @Override <E extends ExpressionDto> E roundtripTest(E dto) throws Exception {
        String serialized = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dto);
        print("serialized: {}", serialized);
        ExpressionDto deserialized = objectMapper.readValue(serialized, ExpressionDto.class);
        assertEquals(dto, deserialized);
        return (E) deserialized;
    }

    protected @Override FunctionNameDto roundtripTest(FunctionNameDto dto) throws Exception {
        String serialized = objectMapper.writeValueAsString(dto);
        print("serialized: {}", serialized);
        FunctionNameDto deserialized = objectMapper.readValue(serialized, ExpressionDto.FunctionNameDto.class);
        assertEquals(dto, deserialized);
        return deserialized;
    }
}
