/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.geotools.jackson.databind.filter.FilterRoundtripTest;
import org.geotools.jackson.databind.util.ObjectMapperUtil;
import org.junit.jupiter.api.BeforeAll;

@Slf4j
class FilterSerializationTest extends FilterRoundtripTest {

    protected void print(String logmsg, Object... args) {
        boolean debug = Boolean.getBoolean("debug");
        if (debug) log.debug(logmsg, args);
    }

    private static ObjectMapper objectMapper;

    public static @BeforeAll void beforeAll() {
        objectMapper = ObjectMapperUtil.newObjectMapper();
    }

    @SuppressWarnings("unchecked")
    protected @Override <F extends Filter> F roundtripTest(F dto) throws Exception {
        String serialized = objectMapper.writeValueAsString(dto);
        print("serialized: {}", serialized);
        Filter deserialized = objectMapper.readValue(serialized, Filter.class);
        assertEquals(dto, deserialized);
        return (F) deserialized;
    }

    protected @Override void roundtripTest(SortBy dto) throws Exception {
        String serialized = objectMapper.writeValueAsString(dto);
        print("serialized: {}", serialized);
        SortBy deserialized = objectMapper.readValue(serialized, SortBy.class);
        assertEquals(dto, deserialized);
    }
}
