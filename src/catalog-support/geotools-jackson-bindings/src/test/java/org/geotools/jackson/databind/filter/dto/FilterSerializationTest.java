/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter.dto;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.geotools.jackson.databind.filter.FilterRoundtripTest;
import org.junit.Before;

public class FilterSerializationTest extends FilterRoundtripTest {

    private ObjectMapper objectMapper;

    public @Before void before() {
        objectMapper = new ObjectMapper();
        objectMapper.setDefaultPropertyInclusion(Include.NON_EMPTY);
        objectMapper.findAndRegisterModules();
    }

    @SuppressWarnings("unchecked")
    protected @Override <F extends Filter> F roundtripTest(F dto) throws Exception {
        String serialized = objectMapper.writeValueAsString(dto);
        System.err.println(serialized);
        Filter deserialized = objectMapper.readValue(serialized, Filter.class);
        assertEquals(dto, deserialized);
        return (F) deserialized;
    }

    protected @Override void roundtripTest(SortBy dto) throws Exception {
        String serialized = objectMapper.writeValueAsString(dto);
        System.err.println(serialized);
        SortBy deserialized = objectMapper.readValue(serialized, SortBy.class);
        assertEquals(dto, deserialized);
    }
}
