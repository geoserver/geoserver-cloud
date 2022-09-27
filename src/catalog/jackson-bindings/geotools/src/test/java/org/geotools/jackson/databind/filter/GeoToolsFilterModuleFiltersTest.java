/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.geotools.jackson.databind.filter.dto.Filter;
import org.geotools.jackson.databind.filter.dto.SortBy;
import org.geotools.jackson.databind.filter.mapper.FilterMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/**
 * Test suite for {@link GeoToolsFilterModule} serialization and deserialization of {@link
 * org.opengis.filter.Filter}s
 */
@Slf4j
public abstract class GeoToolsFilterModuleFiltersTest extends FilterRoundtripTest {

    protected void print(String logmsg, Object... args) {
        boolean debug = Boolean.getBoolean("debug");
        if (debug) log.debug(logmsg, args);
    }

    private ObjectMapper objectMapper;
    private FilterMapper filterMapper;

    public @BeforeEach void beforeEach() {
        objectMapper = newObjectMapper();
        filterMapper = Mappers.getMapper(FilterMapper.class);
    }

    protected abstract ObjectMapper newObjectMapper();

    protected @Override <F extends Filter> F roundtripTest(F dto) throws Exception {
        final org.opengis.filter.Filter expected = filterMapper.map(dto);
        String serialized = objectMapper.writeValueAsString(expected);
        print("serialized: {}", serialized);
        org.opengis.filter.Filter deserialized;
        deserialized = objectMapper.readValue(serialized, org.opengis.filter.Filter.class);
        assertEquals(expected, deserialized);
        return dto;
    }

    protected @Override void roundtripTest(SortBy dto) throws Exception {
        final org.opengis.filter.sort.SortBy expected = filterMapper.map(dto);
        String serialized = objectMapper.writeValueAsString(expected);
        print("serialized: {}", serialized);
        org.opengis.filter.sort.SortBy deserialized;
        deserialized = objectMapper.readValue(serialized, org.opengis.filter.sort.SortBy.class);
        assertEquals(expected, deserialized);
    }

    @Disabled("revisit, ResourceIdImpl equals issue")
    @Override
    public @Test void idFilter_ResourceId_Date() throws Exception {}
}
