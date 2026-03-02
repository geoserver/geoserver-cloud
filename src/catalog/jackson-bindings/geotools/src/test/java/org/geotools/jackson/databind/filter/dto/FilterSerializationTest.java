/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.filter.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import lombok.extern.slf4j.Slf4j;
import org.geotools.jackson.databind.filter.FilterRoundtripTest;
import org.geotools.jackson.databind.util.ObjectMapperUtil;
import org.junit.jupiter.api.BeforeAll;
import tools.jackson.databind.ObjectMapper;

@Slf4j
class FilterSerializationTest extends FilterRoundtripTest {

    protected void print(String logmsg, Object... args) {
        boolean debug = Boolean.getBoolean("debug");
        if (debug) {
            log.debug(logmsg, args);
        }
    }

    private static ObjectMapper objectMapper;

    @BeforeAll
    static void beforeAll() {
        objectMapper = ObjectMapperUtil.newObjectMapper();
    }

    @SuppressWarnings("unchecked")
    protected @Override <F extends FilterDto> F roundtripTest(F dto) throws Exception {
        String serialized = objectMapper.writeValueAsString(dto);
        print("serialized: {}", serialized);
        FilterDto deserialized = objectMapper.readValue(serialized, FilterDto.class);
        assertEquals(dto, deserialized);
        return (F) deserialized;
    }

    protected @Override void roundtripTest(SortByDto dto) throws Exception {
        String serialized = objectMapper.writeValueAsString(dto);
        print("serialized: {}", serialized);
        SortByDto deserialized = objectMapper.readValue(serialized, SortByDto.class);
        assertEquals(dto, deserialized);
    }
}
