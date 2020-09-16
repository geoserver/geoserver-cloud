package org.geotools.jackson.databind.filter;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geotools.jackson.databind.filter.dto.Filter;
import org.geotools.jackson.databind.filter.mapper.FilterMapper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mapstruct.factory.Mappers;

/**
 * Test suite for {@link GeoToolsFilterModule} serialization and deserialization of {@link
 * org.opengis.filter.Filter}s
 */
public class GeoToolsFilterModuleFiltersTest extends FilterRoundtripTest {

    private ObjectMapper objectMapper;
    private FilterMapper filterMapper;

    public @Before void before() {
        objectMapper = new ObjectMapper();
        objectMapper.setDefaultPropertyInclusion(Include.NON_EMPTY);
        objectMapper.findAndRegisterModules();

        filterMapper = Mappers.getMapper(FilterMapper.class);
    }

    protected @Override <F extends Filter> F roundtripTest(F dto) throws Exception {
        final org.opengis.filter.Filter expected = filterMapper.map(dto);
        String serialized = objectMapper.writeValueAsString(expected);
        System.err.println(serialized);
        org.opengis.filter.Filter deserialized;
        deserialized = objectMapper.readValue(serialized, org.opengis.filter.Filter.class);
        assertEquals(expected, deserialized);
        return dto;
    }

    @Ignore("revisit, ResourceIdImpl equals issue")
    @Override
    public @Test void idFilter_ResourceId_Date() throws Exception {}
}
